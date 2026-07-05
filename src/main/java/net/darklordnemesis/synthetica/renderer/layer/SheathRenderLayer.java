package net.darklordnemesis.synthetica.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.client.SheathStateManager;
import net.darklordnemesis.synthetica.item.ModItems;
import net.darklordnemesis.synthetica.datacomponent.ModDataComponents;
import net.darklordnemesis.synthetica.network.KatanaSyncInfo;
import net.darklordnemesis.synthetica.server.ModDataRegistries;
import net.darklordnemesis.synthetica.renderer.model.EmptySheathModel;
import net.darklordnemesis.synthetica.renderer.model.SheathModel;
import net.darklordnemesis.synthetica.sheath.SheathTransform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SheathRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Synthetica.MOD_ID, "textures/models/entity/sheath.png");

    private final SheathModel<T> sheathModel;
    private final EmptySheathModel<T> emptySheathModel;

    public SheathRenderLayer(RenderLayerParent<T, M> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.sheathModel = new SheathModel<>(modelSet.bakeLayer(ModRenderLayers.SHEATH_LAYER));
        this.emptySheathModel = new EmptySheathModel<>(modelSet.bakeLayer(ModRenderLayers.EMPTY_SHEATH_LAYER));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T livingEntity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        if (!(livingEntity instanceof Player player)) {
            return;
        }

        // 1. Get all katanas that need rendering
        List<KatanaSyncInfo> katanasToRender = getKatanasToRender(player);

        if (katanasToRender.isEmpty()) {
            return;
        }

        // 2. Fetch the synchronized Datapack Registry from the client world
        Registry<SheathTransform> registry = Minecraft.getInstance().level.registryAccess().registryOrThrow(ModDataRegistries.SHEATH_POSITIONS_KEY);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        // 3. Render every katana found in the inventory
        for (KatanaSyncInfo info : katanasToRender) {
            ResourceLocation positionId = info.positionId();
            if (positionId == null) continue;

            SheathTransform transform = registry.get(positionId);
            if (transform == null) continue; // Safety check in case the JSON was deleted

            // Pick which model to render:
            //   holding  → empty sheath (sword is drawn)
            //   not held → full sheath  (sword is sheathed)
            EntityModel<T> modelToRender = info.isDrawn() ? emptySheathModel : sheathModel;

            poseStack.pushPose();

            // apply body rotation
            if (this.getParentModel() instanceof net.minecraft.client.model.HumanoidModel<?> humanoidModel) {
                // This inherits the exact translation and rotation of the body (sneaking, swimming, flying)
                humanoidModel.body.translateAndRotate(poseStack);
            }

            // Apply JSON transformations
            poseStack.translate(transform.translation().x(), transform.translation().y(), transform.translation().z());

            poseStack.mulPose(Axis.XP.rotationDegrees(transform.rotation().x()));
            poseStack.mulPose(Axis.YP.rotationDegrees(transform.rotation().y()));
            poseStack.mulPose(Axis.ZP.rotationDegrees(transform.rotation().z()));

            poseStack.scale(transform.scale().x(), transform.scale().y(), transform.scale().z());

            modelToRender.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

            poseStack.popPose();
        }
    }

    /**
     * Gathers all katanas on the player and determines if they are drawn or sheathed.
     */
    private List<KatanaSyncInfo> getKatanasToRender(Player player) {
        List<KatanaSyncInfo> renderInfos = new ArrayList<>();
        ResourceLocation defaultPos = ResourceLocation.fromNamespaceAndPath(Synthetica.MOD_ID, "hip_left");

        if (player == Minecraft.getInstance().player) {

            // Check main inventory
            for (ItemStack stack : player.getInventory().items) {
                if (stack.is(ModItems.KATANA.get())) {
                    boolean isDrawn = stack == player.getMainHandItem();
                    ResourceLocation pos = stack.getOrDefault(ModDataComponents.SHEATH_POSITION.get(), defaultPos);
                    renderInfos.add(new KatanaSyncInfo(pos, isDrawn));
                }
            }

            // Check offhand specifically
            for (ItemStack stack : player.getInventory().offhand) {
                if (stack.is(ModItems.KATANA.get())) {
                    boolean isDrawn = stack == player.getOffhandItem();
                    ResourceLocation pos = stack.getOrDefault(ModDataComponents.SHEATH_POSITION.get(), defaultPos);
                    renderInfos.add(new KatanaSyncInfo(pos, isDrawn));
                }
            }
        } else {
            renderInfos.addAll(SheathStateManager.getKatanas(player.getUUID()));
        }

        return renderInfos;
    }
}