package net.darklordnemesis.synthetica.essentia.value;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.darklordnemesis.synthetica.essentia.Aspect;
import net.darklordnemesis.synthetica.essentia.EssentiaStack;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.*;

/**
 * EssentiaValueMap
 *
 * LOADING PIPELINE:
 *  1. apply() — fires on the server during datapack reload (world load,
 *     /reload command). Reads every JSON file found under essentia_values/
 *     in any datapack, decodes them with EssentiaValueEntry.CODEC, and
 *     populates itemValues and tagValues.
 *  2. EssentiaValueProvider.get() — queries the maps at runtime. Item
 *     lookup is O(1). Tag lookup iterates registered tag keys (small set).
 *
 * PRIORITY:
 *  If both an item entry AND a tag entry match, the item entry wins.
 *  If multiple tag entries match (e.g. #logs and #planks both match oak),
 *  their values are MERGED — all stacks from all matching tags are combined.
 *  This mirrors how Thaumcraft handles compound aspect sources.
 *
 * KEY CONCEPTS DEMONSTRATED:
 *  - SimpleJsonResourceReloadListener — the correct base class for loading
 *    JSON data from datapacks. Handles file discovery and parsing for us;
 *    we only implement apply() to process the parsed JsonElements.
 *  - JsonOps.INSTANCE — the DynamicOps implementation for Gson JsonElements,
 *    used here because SimpleJsonResourceReloadListener gives us JsonElements.
 *    (For NBT we'd use NbtOps; for network we use RegistryFriendlyByteBuf.)
 *  - resultOrPartial() — logs codec errors without crashing. Essential for
 *    datapack loading where a malformed file should warn, not crash the server.
 *  - Singleton pattern — one instance registered as a resource listener,
 *    queried statically via EssentiaValueProvider.
 */
public class EssentiaValueMap extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // The folder name inside data/<namespace>/ where JSON files are found
    public static final String FOLDER = "essentia_values";

    // Singleton — registered in your mod's AddReloadListenersEvent handler
    public static final EssentiaValueMap INSTANCE = new EssentiaValueMap();

    // Item → EssentiaValue (specific item entries)
    private Map<Item, EssentiaValue> itemValues = new HashMap<>();

    // TagKey → EssentiaValue (tag entries)
    private Map<TagKey<Item>, EssentiaValue> tagValues = new HashMap<>();

    private EssentiaValueMap() {
        super(GSON, FOLDER);
    }

    // -------------------------------------------------------------------------
    // Resource loading
    // -------------------------------------------------------------------------

    /**
     * apply() — called on the server thread after all JSON files are parsed.
     *
     * @param object  map of ResourceLocation → JsonElement for every file found
     * @param manager the resource manager (not needed here)
     * @param profiler for performance tracking (not needed here)
     */
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object,
                         ResourceManager manager, ProfilerFiller profiler) {
        Map<Item, EssentiaValue> newItemValues = new HashMap<>();
        Map<TagKey<Item>, EssentiaValue> newTagValues = new HashMap<>();

        int loaded = 0, skipped = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            ResourceLocation fileId = entry.getKey();

            // Decode the JSON using our codec with JsonOps
            Optional<EssentiaValueEntry> result = EssentiaValueEntry.CODEC
                    .parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> LOGGER.warn(
                            "[Synthetica] Failed to parse essentia value '{}': {}", fileId, err));

            if (result.isEmpty()) { skipped++; continue; }

            EssentiaValueEntry valueEntry = result.get();

            if (!valueEntry.isValid()) {
                LOGGER.warn("[Synthetica] Skipping essentia value '{}': must have exactly one of " +
                        "'item' or 'tag', and at least one non-empty value stack.", fileId);
                skipped++;
                continue;
            }

            if (valueEntry.isItemEntry()) {
                // Resolve to an actual Item — skip if the item isn't registered
                valueEntry.resolveItem().ifPresentOrElse(
                        item -> {
                            // If multiple files target the same item, merge their stacks
                            newItemValues.merge(item, valueEntry.value(), EssentiaValueMap::mergeValues);
                        },
                        () -> LOGGER.warn("[Synthetica] Unknown item in essentia value '{}': {}",
                                fileId, valueEntry.item().orElse(null))
                );
            } else {
                // Tag entry — store by TagKey, resolved at query time
                valueEntry.resolveTag().ifPresent(tag ->
                        newTagValues.merge(tag, valueEntry.value(), EssentiaValueMap::mergeValues)
                );
            }

            loaded++;
        }

        // Swap in the new maps atomically — thread safe for concurrent reads
        this.itemValues = Collections.unmodifiableMap(newItemValues);
        this.tagValues  = Collections.unmodifiableMap(newTagValues);

        LOGGER.info("[Synthetica] Loaded {} essentia values ({} skipped).", loaded, skipped);
    }

    // -------------------------------------------------------------------------
    // Lookup
    // -------------------------------------------------------------------------

    /**
     * Returns the EssentiaValue for the given ItemStack.
     *
     * Priority: specific item entry > merged tag entries.
     * If only tags match, all matching tag values are merged together.
     * Returns EssentiaValue.EMPTY if nothing matches.
     */
    public EssentiaValue getFor(ItemStack stack) {
        if (stack.isEmpty()) return EssentiaValue.EMPTY;
        return getFor(stack.getItem());
    }

    public EssentiaValue getFor(Item item) {
        // 1. Specific item entry wins if present
        EssentiaValue specific = itemValues.get(item);
        if (specific != null) return specific;

        // 2. Collect all matching tag entries and merge them
        EssentiaValue merged = EssentiaValue.EMPTY;
        for (Map.Entry<TagKey<Item>, EssentiaValue> entry : tagValues.entrySet()) {
            if (item.builtInRegistryHolder().is(entry.getKey())) {
                merged = mergeValues(merged, entry.getValue());
            }
        }
        return merged;
    }

    /** True if this item has any registered essentia value. */
    public boolean hasValue(Item item) {
        return !getFor(item).isEmpty();
    }

    public boolean hasValue(ItemStack stack) {
        return !stack.isEmpty() && hasValue(stack.getItem());
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Merges two EssentiaValues by combining their stack lists.
     * Stacks with the same aspect are summed together rather than duplicated.
     */
    private static EssentiaValue mergeValues(EssentiaValue a, EssentiaValue b) {
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;

        // Use a map keyed by aspect Holder to accumulate amounts
        Map<Holder<Aspect>,
                Long> merged = new LinkedHashMap<>();

        for (EssentiaStack stack : a.stacks()) {
            if (!stack.isEmpty() && stack.getAspect() != null)
                merged.merge(stack.getAspect(), stack.getAmount(), Long::sum);
        }
        for (EssentiaStack stack : b.stacks()) {
            if (!stack.isEmpty() && stack.getAspect() != null)
                merged.merge(stack.getAspect(), stack.getAmount(), Long::sum);
        }

        List<EssentiaStack> resultStacks = new ArrayList<>();
        merged.forEach((aspect, amount) ->
                resultStacks.add(new EssentiaStack(aspect, amount)));

        return new EssentiaValue(resultStacks);
    }
}