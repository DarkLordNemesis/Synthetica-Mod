package net.darklordnemesis.synthetica.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.client.SheathStateManager;
import net.darklordnemesis.synthetica.item.ModItems;
import net.darklordnemesis.synthetica.network.KatanaSyncInfo;
import net.darklordnemesis.synthetica.component.ModDataComponents;
import net.darklordnemesis.synthetica.server.ModDataRegistries;
import net.darklordnemesis.synthetica.tag.ModTags;
import net.darklordnemesis.synthetica.renderer.model.EmptySheathModel;
import net.darklordnemesis.synthetica.renderer.model.SheathModel;
import net.darklordnemesis.synthetica.sheath.SheathTransform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SheathRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    // Helper record to group visual assets together
    private record SheathVisuals<T extends LivingEntity>(EntityModel<T> sheathed, EntityModel<T> drawn, ResourceLocation texture) {}

    // The Client-Side Registry Map
    private final Map<Item, SheathVisuals<T>> visualsRegistry = new HashMap<>();

    public SheathRenderLayer(RenderLayerParent<T, M> renderer, EntityModelSet modelSet) {
        super(renderer);

        // 1. REGISTER KATANA VISUALS
        visualsRegistry.put(ModItems.KATANA.get(), new SheathVisuals<>(
                new SheathModel<>(modelSet.bakeLayer(ModRenderLayers.SHEATH_LAYER)),
                new EmptySheathModel<>(modelSet.bakeLayer(ModRenderLayers.EMPTY_SHEATH_LAYER)),
                ResourceLocation.fromNamespaceAndPath(Synthetica.MOD_ID, "textures/models/entity/sheath.png")
        ));

        // 2. Add future weapons here! Example:
        // visualsRegistry.put(ModItems.GREATSWORD.get(), new SheathVisuals<>(
        //         new GreatswordSheathModel<>(modelSet.bakeLayer(ModRenderLayers.GREATSWORD_LAYER)),
        //         new EmptyGreatswordSheathModel<>(modelSet.bakeLayer(ModRenderLayers.EMPTY_GREATSWORD_LAYER)),
        //         ResourceLocation.fromNamespaceAndPath(Synthetica.MOD_ID, "textures/models/entity/greatsword_sheath.png")
        // ));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T livingEntity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(livingEntity instanceof Player player)) return;

        List<KatanaSyncInfo> katanasToRender = getKatanasToRender(player);
        if (katanasToRender.isEmpty()) return;

        Registry<SheathTransform> registry = Minecraft.getInstance().level.registryAccess().registryOrThrow(ModDataRegistries.SHEATH_POSITIONS_KEY);

        for (KatanaSyncInfo info : katanasToRender) {
            ResourceLocation positionId = info.positionId();
            if (positionId == null) continue;

            SheathTransform transform = registry.get(positionId);
            if (transform == null) continue;

            // Look up the specific sword item and its visual data
            Item weaponItem = BuiltInRegistries.ITEM.get(info.itemId());
            SheathVisuals<T> visualData = visualsRegistry.get(weaponItem);

            // If we haven't registered a model for this sword type, skip it
            if (visualData == null) continue;

            // Use the item-specific models and texture
            EntityModel<T> modelToRender = info.isDrawn() ? visualData.drawn() : visualData.sheathed();
            VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(visualData.texture()));

            poseStack.pushPose();

            if (this.getParentModel() instanceof HumanoidModel<?> humanoidModel) {
                humanoidModel.body.translateAndRotate(poseStack);
            }

            poseStack.translate(transform.translation().x(), transform.translation().y(), transform.translation().z());
            poseStack.mulPose(Axis.XP.rotationDegrees(transform.rotation().x()));
            poseStack.mulPose(Axis.YP.rotationDegrees(transform.rotation().y()));
            poseStack.mulPose(Axis.ZP.rotationDegrees(transform.rotation().z()));
            poseStack.scale(transform.scale().x(), transform.scale().y(), transform.scale().z());

            modelToRender.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    private List<KatanaSyncInfo> getKatanasToRender(Player player) {
        List<KatanaSyncInfo> renderInfos = new ArrayList<>();
        ResourceLocation defaultPos = ResourceLocation.fromNamespaceAndPath(Synthetica.MOD_ID, "hip_left");

        if (player == Minecraft.getInstance().player) {
            for (ItemStack stack : player.getInventory().items) {
                if (stack.is(ModTags.HAS_SHEATH)) {
                    boolean isDrawn = stack == player.getMainHandItem();
                    ResourceLocation pos = stack.getOrDefault(ModDataComponents.SHEATH_POSITION.get(), defaultPos);
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    renderInfos.add(new KatanaSyncInfo(itemId, pos, isDrawn));
                }
            }
            for (ItemStack stack : player.getInventory().offhand) {
                if (stack.is(ModTags.HAS_SHEATH)) {
                    boolean isDrawn = stack == player.getOffhandItem();
                    ResourceLocation pos = stack.getOrDefault(ModDataComponents.SHEATH_POSITION.get(), defaultPos);
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    renderInfos.add(new KatanaSyncInfo(itemId, pos, isDrawn));
                }
            }
        } else {
            renderInfos.addAll(SheathStateManager.getKatanas(player.getUUID()));
        }
        return renderInfos;
    }
}