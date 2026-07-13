package net.darklordnemesis.synthetica.event;

import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.essentia.value.EssentiaValueMap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@EventBusSubscriber(modid = Synthetica.MOD_ID)
public class ForgeEventBus {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(EssentiaValueMap.INSTANCE);
    }


}
