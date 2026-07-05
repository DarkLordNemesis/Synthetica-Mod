package net.darklordnemesis.synthetica.network;

import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.client.SheathStateManager;
import net.darklordnemesis.synthetica.datacomponent.ModDataComponents;
import net.darklordnemesis.synthetica.item.ModItems;
import net.darklordnemesis.synthetica.server.ModDataRegistries;
import net.darklordnemesis.synthetica.sheath.SheathTransform;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Synthetica.MOD_ID) // Mod Bus
public class ModNetworking {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Synthetica.MOD_ID);

        registrar.playToClient(
                SyncSheathPayload.TYPE,
                SyncSheathPayload.STREAM_CODEC,
                ModNetworking::handleSheathSync
        );

        registrar.playToServer(
                CycleSheathPayload.TYPE,
                CycleSheathPayload.STREAM_CODEC,
                ModNetworking::handleSheathCycle
        );
    }

    private static void handleSheathSync(final SyncSheathPayload payload, final IPayloadContext context) {
        // Ensure this runs on the main client thread
        context.enqueueWork(() -> {
            SheathStateManager.setKatanas(payload.playerId(), payload.katanas());
        });
    }

    // Inside your ModNetworking class

    private static void handleSheathCycle(final CycleSheathPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // Since this is playToServer, context.player() is the ServerPlayer
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) return;

            net.minecraft.world.item.ItemStack heldItem = player.getMainHandItem();
            if (!heldItem.is(ModItems.KATANA.get())) {
                heldItem = player.getOffhandItem();
            }

            // If they aren't actually holding it, ignore the packet (anti-cheat)
            if (!heldItem.is(ModItems.KATANA.get())) return;

            // 1. Get the Synced Registry from the server level
            net.minecraft.core.Registry<SheathTransform> registry = player.level().registryAccess().registryOrThrow(ModDataRegistries.SHEATH_POSITIONS_KEY);

            // 2. Filter down to ALL positions that this specific sword is allowed to use
            net.minecraft.world.item.ItemStack finalHeldItem = heldItem;
            java.util.List<ResourceLocation> validPositions = registry.keySet().stream()
                    .filter(id -> {
                        SheathTransform transform = registry.get(id);
                        return transform != null && transform.isValidFor(finalHeldItem); // Your helper method from the Codec class
                    })
                    .toList();

            if (validPositions.isEmpty()) return; // No valid positions defined in JSONs

            // 3. Find current position (or default to the first one)
            ResourceLocation currentPos = heldItem.getOrDefault(ModDataComponents.SHEATH_POSITION.get(), validPositions.get(0));

            // 4. Calculate the new index based on scroll direction
            int currentIndex = validPositions.indexOf(currentPos);
            if (currentIndex == -1) currentIndex = 0; // Fallback if current isn't valid anymore

            int nextIndex = (currentIndex + payload.direction()) % validPositions.size();
            if (nextIndex < 0) nextIndex += validPositions.size(); // Handle negative wrapping

            // 5. Update the Data Component!
            // (Because we set networkSynchronized(), this automatically updates the client's item!)
            heldItem.set(ModDataComponents.SHEATH_POSITION.get(), validPositions.get(nextIndex));
        });
    }
}