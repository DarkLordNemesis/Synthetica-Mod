package net.darklordnemesis.synthetica.event;

import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.item.ModItems;
import net.darklordnemesis.synthetica.item.armor.AbstractArmorItem;
import net.darklordnemesis.synthetica.item.armor.client.ArmorClientExtension;
import net.darklordnemesis.synthetica.item.armor.client.model.FrozenBlazeArmorModel;
import net.darklordnemesis.synthetica.item.armor.client.provider.ArmorModelProvider;
import net.darklordnemesis.synthetica.item.armor.client.provider.SimpleModelProvider;
import net.darklordnemesis.synthetica.renderer.layer.ModRenderLayers;
import net.darklordnemesis.synthetica.renderer.layer.SheathRenderLayer;
import net.darklordnemesis.synthetica.renderer.model.EmptySheathModel;
import net.darklordnemesis.synthetica.renderer.model.SheathModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Map;

@EventBusSubscriber(modid = Synthetica.MOD_ID, value = Dist.CLIENT)
public class ModClientEventbusEvents {

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        registerArmorExtension(ModItems.FROZEN_BLAZE_ARMOR, event, new SimpleModelProvider(FrozenBlazeArmorModel::createBodyLayer, FrozenBlazeArmorModel::new));
    }

    @SuppressWarnings("unchecked")
    private static <T extends AbstractArmorItem> void registerArmorExtension(Map<ArmorItem.Type, DeferredItem<T>> map, RegisterClientExtensionsEvent event, ArmorModelProvider provider) {
        event.registerItem(new ArmorClientExtension(provider), map.values().toArray(DeferredItem[]::new));
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModRenderLayers.SHEATH_LAYER, SheathModel::createBodyLayer);
        event.registerLayerDefinition(ModRenderLayers.EMPTY_SHEATH_LAYER, EmptySheathModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        EntityModelSet models = event.getEntityModels();
        for (PlayerSkin.Model skin : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(skin);
            if (renderer instanceof LivingEntityRenderer<? extends Player, ?> livingRender) {
                // noinspection unchecked
                ((LivingEntityRenderer<Player, PlayerModel<Player>>) livingRender)
                        .addLayer(new SheathRenderLayer<>(
                                (LivingEntityRenderer<Player, PlayerModel<Player>>) livingRender, models)
                        );
            }
        }
    }
}