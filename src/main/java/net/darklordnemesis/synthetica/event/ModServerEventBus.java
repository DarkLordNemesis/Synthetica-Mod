package net.darklordnemesis.synthetica.event;

import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.block.entity.EssentiaJarBlockEntity;
import net.darklordnemesis.synthetica.block.entity.ModBlockEntities;
import net.darklordnemesis.synthetica.capabilities.Capabilities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = Synthetica.MOD_ID)
public class ModServerEventBus {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EssentiaHandler.BLOCK,
                ModBlockEntities.ESSENTIA_JAR_BE.get(),
                EssentiaJarBlockEntity::getEssentiaHandler
        );
    }
}
