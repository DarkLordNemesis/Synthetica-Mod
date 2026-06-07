package net.darklordnemesis.synthetica.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.darklordnemesis.synthetica.block.entity.PedestalBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public class PedestalBlockEntityRenderer implements BlockEntityRenderer<PedestalBlockEntity> {
    public PedestalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PedestalBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        ItemStack itemStack = blockEntity.inventory.getStackInSlot(0);

        poseStack.pushPose();
        poseStack.translate(0.5, 1.15, 0.5);
        poseStack.scale(1f, 1f, 1f);

        // Turn the item on the pedestal to face player (camera Pos)
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

        Vec3 cameraPos = camera.getPosition();
        Vec3 pedestalPos = Vec3.atCenterOf(blockEntity.getBlockPos());

        // Calculate the angle between the camera and the pedestal
        double dx = cameraPos.x - pedestalPos.x;
        double dz = cameraPos.z - pedestalPos.z;

        // Convert the angle to degrees
        float yaw = (float) Math.toDegrees(Math.atan2(dx, dz));

        // apply rotation to the item
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw + 180));

        itemRenderer.renderStatic(itemStack, ItemDisplayContext.FIXED, getLightLevel(blockEntity.getLevel(), blockEntity.getBlockPos()),
                OverlayTexture.NO_OVERLAY, poseStack, bufferSource, blockEntity.getLevel(), 1);

        poseStack.popPose();


    }

    private int getLightLevel(Level level, BlockPos blockPos) {
        int blockLight = level.getBrightness(LightLayer.BLOCK, blockPos);
        int skyLight = level.getBrightness(LightLayer.SKY, blockPos);
        return LightTexture.pack(skyLight, blockLight);
    }
}
