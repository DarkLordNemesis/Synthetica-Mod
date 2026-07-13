package net.darklordnemesis.synthetica.essentia;

import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.registry.ModRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * ModAspects
 *
 * Registers the six primal aspects into the custom Aspect registry.
 *
 * KEY CONCEPTS DEMONSTRATED:
 *  - DeferredRegister with a custom registry key — identical pattern to
 *    items/blocks but pointing at ModRegistries.ASPECT_REGISTRY_KEY.
 *  - DeferredHolder<Aspect, Aspect> — the holder type for our registry.
 *    The double <Aspect, Aspect> is because the registry holds Aspect objects
 *    and DeferredHolder<RegistryType, ObjectType> needs both.
 *  - ResourceKey.create() — creates the per-entry key used to look up
 *    a specific aspect in the registry (e.g. "synthetica:ignis").
 *
 * THE SIX PRIMALS (from Thaumcraft lore):
 *  Ignis  — Fire    — passion, energy, destruction
 *  Aqua   — Water   — flow, adaptability, life
 *  Terra  — Earth   — stability, growth, nature
 *  Aer    — Air     — freedom, speed, thought
 *  Ordo   — Order   — structure, purity, law
 *  Perditio— Entropy — chaos, decay, void
 *
 * COLOURS: stored as packed ARGB ints (0xAARRGGBB).
 * Alpha 0xFF = fully opaque. Colours chosen to match Thaumcraft's aesthetic.
 */
public class ModAspects {

    /**
     * DeferredRegister targeting our custom Aspect registry.
     * Works exactly like DeferredRegister.create(Registries.ITEM, MOD_ID)
     * but for our own registry type.
     */
    public static final DeferredRegister<Aspect> ASPECTS =
            DeferredRegister.create(ModRegistries.ASPECT_REGISTRY_KEY, Synthetica.MOD_ID);

    // -------------------------------------------------------------------------
    // The six primal aspects
    // -------------------------------------------------------------------------

    /** Ignis — Fire — deep orange-red */
    public static final DeferredHolder<Aspect, Aspect> IGNIS = registerPrimal(
            "ignis",
            0xFFDD4400
    );

    /** Aqua — Water — clear blue */
    public static final DeferredHolder<Aspect, Aspect> AQUA = registerPrimal(
            "aqua",
            0xFF2288DD
    );

    /** Terra — Earth — rich green */
    public static final DeferredHolder<Aspect, Aspect> TERRA = registerPrimal(
            "terra",
            0xFF336B2F
    );

    /** Aer — Air — pale sky blue */
    public static final DeferredHolder<Aspect, Aspect> AER = registerPrimal(
            "aer",
            0xFFF5E98E
    );

    /** Ordo — Order — clean silver-white */
    public static final DeferredHolder<Aspect, Aspect> ORDO = registerPrimal(
            "ordo",
            0xFFDDDDFF
    );

    /** Perditio — Entropy — dark void purple */
    public static final DeferredHolder<Aspect, Aspect> PERDITIO = registerPrimal(
            "perditio",
            0xFF141414
    );

    // Tier 1 Compound Aspects //

    /** Lux — Light — white */
    public static final DeferredHolder<Aspect, Aspect> LUX = registerCompound(
            "lux",
            0xFFDDDDFF,
            new Aspect.AspectComposition(AER, IGNIS)
    );

    // -------------------------------------------------------------------------
    // Registration helper
    // -------------------------------------------------------------------------

    /**
     * Registers a new Aspect with a given registry name, translation key, and colour.
     *
     * @param name           Registry path, e.g. "ignis" → "synthetica:ignis"
     * @param color          Packed ARGB colour (0xAARRGGBB)
     */
    private static DeferredHolder<Aspect, Aspect> registerPrimal(String name, int color) {
        return ASPECTS.register(name, () -> new Aspect(color, Optional.empty()));
    }

    private static DeferredHolder<Aspect, Aspect> registerCompound(String name, int color, @NotNull Aspect.AspectComposition components) {
        return ASPECTS.register(name, () -> new Aspect(color, Optional.of(components)));
    }

    // -------------------------------------------------------------------------
    // Lookup helpers
    // -------------------------------------------------------------------------

    /**
     * Gets the ResourceKey for a specific aspect by path.
     * Useful when you need to reference an aspect from NBT or network code:
     *
     *   ResourceKey<Aspect> key = ModAspects.key("ignis");
     *   Aspect aspect = registryAccess.registryOrThrow(ModRegistries.ASPECT_REGISTRY_KEY).get(key);
     */
    public static ResourceKey<Aspect> key(String path) {
        return ResourceKey.create(
                ModRegistries.ASPECT_REGISTRY_KEY,
                ResourceLocation.fromNamespaceAndPath(Synthetica.MOD_ID, path)
        );
    }

    public static void registerPrimal(IEventBus modEventBus) {
        ASPECTS.register(modEventBus);
    }
}