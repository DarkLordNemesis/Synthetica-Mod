package net.darklordnemesis.synthetica.renderer.layer;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.item.ModItems;
import net.darklordnemesis.synthetica.renderer.model.EmptySheathModel;
import net.darklordnemesis.synthetica.renderer.model.SheathModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SheathRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Synthetica.MOD_ID, "textures/models/entity/sheath.png");

    private final double[][] sheathPositions = new double[][] {
            {0.272, 0.84, 0.15},
            {-0.272, 0.84, 0.15}
    };

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

        // Check if the katana is anywhere in the player's inventory
        boolean hasKatanaInInventory = hasKatanaInInventory(player);

        // If the player doesn't have the katana at all, render nothing
        if (!hasKatanaInInventory) {
            return;
        }

        // Check specifically if the katana is in the main hand right now
        boolean isHoldingKatana = player.getMainHandItem().is(ModItems.KATANA.get()) || player.getOffhandItem().is(ModItems.KATANA.get());

        // Pick which model to render:
        //   holding  → empty sheath (sword is drawn)
        //   not held → full sheath  (sword is sheathed)
        EntityModel<T> modelToRender = isHoldingKatana ? emptySheathModel : sheathModel;


        for (double[] position : sheathPositions) {
            poseStack.pushPose();

            // x: -0.265 for right
            poseStack.translate(position[0], position[1], position[2]);
            poseStack.scale(0.5f, 0.5f, 0.5f);
            poseStack.mulPose(Axis.XP.rotationDegrees(65));

            poseStack.mulPose(Axis.ZP.rotationDegrees(180));

            VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

            modelToRender.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

            poseStack.popPose();
        }

    }

    private boolean hasKatanaInInventory(Player player) {
        if (player.getOffhandItem().is(ModItems.KATANA.get())) return true;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.KATANA.get())) {
                return true;
            }
        }
        return false;
    }

}