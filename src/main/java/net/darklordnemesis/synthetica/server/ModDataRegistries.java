package net.darklordnemesis.synthetica.server;

import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.sheath.SheathTransform;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@EventBusSubscriber(modid = Synthetica.MOD_ID)
public class ModDataRegistries {

    // 1. Create the Registry Key
    public static final ResourceKey<Registry<SheathTransform>> SHEATH_POSITIONS_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Synthetica.MOD_ID, "sheath_position"));

    @SubscribeEvent
    public static void onNewDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        // 2. Register it!
        // The first CODEC tells the game how to read the JSON files from datapacks.
        // The second CODEC tells the game how to sync the data over the network to clients.
        event.dataPackRegistry(SHEATH_POSITIONS_KEY, SheathTransform.CODEC, SheathTransform.CODEC);
    }
}