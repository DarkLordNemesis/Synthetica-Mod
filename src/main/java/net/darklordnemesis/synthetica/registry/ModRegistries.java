package net.darklordnemesis.synthetica.registry;

import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.essentia.Aspect;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * ModRegistries
 *
 * Creates and holds the custom Registry objects for this mod.
 *
 * KEY CONCEPTS DEMONSTRATED:
 *  - ResourceKey<Registry<T>> — the unique key identifying a registry itself.
 *    Just as items live in Registry<Item>, aspects live in Registry<Aspect>.
 *    The ResourceKey is how the game refers to the registry by name.
 *
 *  - RegistryBuilder — NeoForge's builder for creating a new Registry.
 *    .sync(true) means the registry contents are sent to clients on join,
 *    so client-side code (like rendering) can safely read aspects.
 *
 *  - WHY A CUSTOM REGISTRY vs a simple Map/Enum:
 *    A proper Registry gives you:
 *      • Integer IDs for network sync (efficient)
 *      • ResourceLocation keys for JSON/NBT (human-readable)
 *      • Tag support (group aspects by behaviour)
 *      • Other mods can register their own aspects at startup
 *      • DeferredRegister works the same as items/blocks
 *
 * SETUP: Call ModRegistries.ASPECT in your mod constructor BEFORE
 * ModAspects registers anything — the registry must exist first.
 * NeoForge handles ordering automatically when you use NewRegistryEvent.
 */

@EventBusSubscriber(modid = Synthetica.MOD_ID)
public class ModRegistries {

    /**
     * ASPECT_REGISTRY_KEY
     *
     * The ResourceKey that names our custom registry: "synthetica:aspect"
     * This is what you'd use to look up the registry from RegistryAccess,
     * and what shows up in /data commands if you query it.
     */
    public static final ResourceKey<Registry<Aspect>> ASPECT_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    ResourceLocation.fromNamespaceAndPath(Synthetica.MOD_ID, "aspect")
            );

    /**
     * ASPECT
     *
     * The actual Registry instance. Built by NeoForge during NewRegistryEvent.
     * You register this in your mod event bus setup — see the snippet at the
     * bottom of this file.
     *
     * .sync(true) — aspect data is synced to clients on login so the
     * render layer and GUI code can safely read aspect colours/names.
     */
    public static Registry<Aspect> ASPECT;

    /**
     * Call this from your mod constructor or FMLCommonSetupEvent to
     * register the creation of the custom registry with NeoForge:
     *
     *   MOD_EVENT_BUS.addListener(ModRegistries::onNewRegistry);
     *
     * Or with the annotation approach shown in the snippet below.
     */
    @SubscribeEvent
    public static void onNewRegistry(net.neoforged.neoforge.registries.NewRegistryEvent event) {
        ASPECT = event.create(
                new RegistryBuilder<>(ASPECT_REGISTRY_KEY)
                        .sync(true)   // send registry contents to clients on join
        );
    }
}