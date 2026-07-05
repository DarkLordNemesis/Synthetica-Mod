package net.darklordnemesis.synthetica.client;

import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.item.ModItems;
import net.darklordnemesis.synthetica.network.CycleSheathPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Synthetica.MOD_ID, value = Dist.CLIENT)
public class ClientInputHandler {

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        // Check if holding Katana (main hand or offhand)
        boolean holdingKatana = player.getMainHandItem().is(ModItems.KATANA.get()) || player.getOffhandItem().is(ModItems.KATANA.get());

        // We require the player to hold ALT to change positions so we don't break hotbar scrolling
        if (holdingKatana && Screen.hasAltDown()) {

            // event.getScrollDeltaY() returns positive for scrolling up, negative for down
            int direction = event.getScrollDeltaY() > 0 ? 1 : -1;

            // Send the packet to the server
            PacketDistributor.sendToServer(new CycleSheathPayload(direction));

            // Cancel the event so the hotbar slot doesn't change
            event.setCanceled(true);
        }
    }
}