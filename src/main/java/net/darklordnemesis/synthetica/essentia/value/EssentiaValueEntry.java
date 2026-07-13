package net.darklordnemesis.synthetica.essentia.value;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Optional;

/**
 * EssentiaValueEntry
 *
 * Represents one JSON file entry — either an item or a tag, plus the
 * EssentiaValue (list of stacks) it provides.
 *
 * We use Optional for both item and tag so the codec can handle either
 * field being absent. Exactly one of them must be present — validated
 * in isValid() so bad JSON entries are skipped with a log warning rather
 * than crashing the game.
 *
 * KEY CONCEPTS DEMONSTRATED:
 *  - ResourceLocation.CODEC — serializes a ResourceLocation to/from a
 *    string like "minecraft:coal". Used for both item and tag fields.
 *  - Codec.optionalField() — field is allowed to be absent in JSON,
 *    producing Optional.empty() instead of a codec error.
 *  - TagKey.create() — wraps a ResourceLocation into a typed tag key.
 *    Tags are always resolved lazily (at world load), not here.
 *
 * JSON formats:
 *
 * Item entry:
 * { "item": "minecraft:coal", "values": [...] }
 *
 * Tag entry:
 * { "tag": "minecraft:logs", "values": [...] }
 */
public record EssentiaValueEntry(
        Optional<ResourceLocation> item,
        Optional<ResourceLocation> tag,
        EssentiaValue value
) {

    public static final Codec<EssentiaValueEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.optionalFieldOf("item")
                            .forGetter(EssentiaValueEntry::item),
                    ResourceLocation.CODEC.optionalFieldOf("tag")
                            .forGetter(EssentiaValueEntry::tag),
                    EssentiaValue.CODEC.fieldOf("values")
                            .forGetter(EssentiaValueEntry::value)
            ).apply(instance, EssentiaValueEntry::new)
    );

    /**
     * Returns true if exactly one of item or tag is present and the value
     * is not empty. Entries failing this check are skipped during loading.
     */
    public boolean isValid() {
        boolean hasItem = item.isPresent();
        boolean hasTag  = tag.isPresent();
        return (hasItem ^ hasTag) && !value.isEmpty(); // XOR: exactly one must be set
    }

    /** True if this entry targets a specific item (not a tag). */
    public boolean isItemEntry() { return item.isPresent(); }

    /** True if this entry targets an item tag. */
    public boolean isTagEntry() { return tag.isPresent(); }

    /**
     * Resolves the item ResourceLocation to an actual Item.
     * Returns empty if the item isn't registered (e.g. from a missing mod).
     */
    public Optional<Item> resolveItem() {
        return item.flatMap(BuiltInRegistries.ITEM::getOptional);
    }

    /**
     * Resolves the tag ResourceLocation to a TagKey<Item>.
     * TagKey is just a typed wrapper around the RL — actual tag contents
     * are resolved later by the tag system when needed.
     */
    public Optional<TagKey<Item>> resolveTag() {
        return tag.map(rl -> TagKey.create(Registries.ITEM, rl));
    }
}