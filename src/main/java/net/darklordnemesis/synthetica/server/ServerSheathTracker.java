package net.darklordnemesis.synthetica.server;

import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.component.ModDataComponents;
import net.darklordnemesis.synthetica.network.KatanaSyncInfo;
import net.darklordnemesis.synthetica.network.SyncSheathPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Synthetica.MOD_ID)
public class ServerSheathTracker {

    private static final Map<UUID, List<KatanaSyncInfo>> LAST_STATES = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        List<KatanaSyncInfo> currentKatanas = buildKatanaList(player);
        List<KatanaSyncInfo> oldKatanas = LAST_STATES.getOrDefault(player.getUUID(), List.of());

        // Records automatically check deep equality, so this accurately detects any changes
        // in position, drawn state, or number of katanas.
        if (!currentKatanas.equals(oldKatanas)) {
            LAST_STATES.put(player.getUUID(), currentKatanas);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new SyncSheathPayload(player.getUUID(), currentKatanas));
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player targetPlayer && event.getEntity() instanceof ServerPlayer tracker) {
            List<KatanaSyncInfo> katanas = LAST_STATES.getOrDefault(targetPlayer.getUUID(), List.of());
            if (!katanas.isEmpty()) {
                PacketDistributor.sendToPlayer(tracker, new SyncSheathPayload(targetPlayer.getUUID(), katanas));
            }
        }
    }

    private static List<KatanaSyncInfo> buildKatanaList(Player player) {
        List<KatanaSyncInfo> list = new ArrayList<>();
        ResourceLocation defaultPos = ResourceLocation.fromNamespaceAndPath(Synthetica.MOD_ID, "hip_left");

        for (ItemStack stack : player.getInventory().items) {
            // Use the Tag instead of a hardcoded item!
            if (stack.is(net.darklordnemesis.synthetica.tag.ModTags.HAS_SHEATH)) {
                boolean isDrawn = stack == player.getMainHandItem();
                ResourceLocation pos = stack.getOrDefault(ModDataComponents.SHEATH_POSITION.get(), defaultPos);
                ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());

                list.add(new KatanaSyncInfo(itemId, pos, isDrawn));
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(net.darklordnemesis.synthetica.tag.ModTags.HAS_SHEATH)) {
                boolean isDrawn = stack == player.getOffhandItem();
                ResourceLocation pos = stack.getOrDefault(ModDataComponents.SHEATH_POSITION.get(), defaultPos);
                ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());

                list.add(new KatanaSyncInfo(itemId, pos, isDrawn));
            }
        }
        return list;
    }
}