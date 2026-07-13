package net.darklordnemesis.synthetica.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.darklordnemesis.synthetica.block.entity.GravitationalAnomalyBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.data.ModelData;

public class GravitationalAnomalyRenderer implements BlockEntityRenderer<GravitationalAnomalyBlockEntity> {

    public GravitationalAnomalyRenderer(BlockEntityRendererProvider.Context context) {
        // Context holds structural geometry helpers if needed
    }

    @Override
    public void render(GravitationalAnomalyBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        // 1. Center the pivot point inside the 1x1x1 block space
        poseStack.translate(0.5, 0.5, 0.5);

        // 2. Dynamic Scale Calculation
        // A base size that swells out using a cube-root function so it grows smoothly, not linearly
        float scale = (float) Math.cbrt(blockEntity.getMass() / 10.0);
        poseStack.scale(scale, scale, scale);

        // 3. Make it spin slowly over time for a cosmic distortion effect
        float rotationTime = (Minecraft.getInstance().level.getGameTime() + partialTick) * 0.8F;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationTime));
        poseStack.mulPose(Axis.XP.rotationDegrees(rotationTime * 0.5F));

        // 4. Render a dramatic model (We will steal the ender dragon/end portal texture style, or fallback to an obsidian orb)
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(Blocks.BLACK_CONCRETE.defaultBlockState());

        // Render block model centered at its updated matrix scale
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                poseStack.last(),
                bufferSource.getBuffer(RenderType.translucent()),
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                model, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay,
                ModelData.EMPTY,
                RenderType.translucent()
        );

        poseStack.popPose();
    }
}