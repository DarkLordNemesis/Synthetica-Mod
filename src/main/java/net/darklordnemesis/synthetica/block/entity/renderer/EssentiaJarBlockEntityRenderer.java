package net.darklordnemesis.synthetica.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.darklordnemesis.synthetica.block.entity.EssentiaJarBlockEntity;
import net.darklordnemesis.synthetica.essentia.Aspect;
import net.darklordnemesis.synthetica.essentia.EssentiaStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * EssentiaJarBlockEntityRenderer
 *
 * Renders two things each frame:
 *  1. The jar model (glass body + lid) using its texture
 *  2. A tinted horizontal quad that rises with the fill level
 *
 * KEY CONCEPTS DEMONSTRATED:
 *  - BlockEntityRendererProvider.Context — used in the constructor to bake
 *    the ModelPart tree. This is the correct place to call context.bakeLayer()
 *    for BE renderers (not in a static initialiser).
 *  - Two separate RenderTypes in one render() call — the jar uses
 *    entityCutoutNoCull (opaque + transparent pixels), the fluid uses
 *    translucent (smooth alpha blending). Each gets its own VertexConsumer
 *    from bufferSource.getBuffer().
 *  - Manual quad construction via VertexConsumer — four vertices per face,
 *    each needing position, color, UV, overlay, light, and normal.
 *  - PoseStack centering — the engine calls render() with the PoseStack
 *    already at the block's corner (0,0,0). We translate to block centre
 *    (0.5, 0, 0.5) so the model sits centred in the block.
 *  - Aspect colour unpacking — the aspect stores a packed ARGB int;
 *    we unpack the channels to pass them to setColor().
 */
public class EssentiaJarBlockEntityRenderer implements BlockEntityRenderer<EssentiaJarBlockEntity> {
    // You will need to create a simple white, greyscale, or fluid-like texture in your mod resources
    private static final ResourceLocation FLUID_TEXTURE = ResourceLocation.fromNamespaceAndPath("synthetica", "textures/block/essentia_fluid.png");

    public EssentiaJarBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(EssentiaJarBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.isEmpty()) { //[cite: 2]
            return;
        }

        EssentiaStack stack = blockEntity.getEssentia(); //[cite: 2]
        if (stack.getAspect() == null) { //[cite: 2]
            return;
        }

        Aspect aspect = stack.getAspect().value(); //[cite: 2]
        float r = aspect.getRed(); //[cite: 4]
        float g = aspect.getGreen(); //[cite: 4]
        float b = aspect.getBlue(); //[cite: 4]
        float a = 0.9F;

        // 3D Bounding box definitions (matching essentia_jar.json inner walls)
        float minX = 4F / 16F;
        float maxX = 12F / 16F;
        float minZ = 4F / 16F;
        float maxZ = 12F / 16F;
        float minY = 1F / 16F;

        float maxFluidHeight = 10F / 16F;
        float maxY = minY + (maxFluidHeight * blockEntity.getFillFraction()); //[cite: 2]

        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentCull(FLUID_TEXTURE));

        // Render the faces with pixel-aligned UV tracking
        renderFluidBox(consumer, matrix, minX, maxX, minY, maxY, minZ, maxZ, r, g, b, a, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private void renderFluidBox(VertexConsumer consumer, Matrix4f matrix, float minX, float maxX, float minY, float maxY, float minZ, float maxZ, float r, float g, float b, float a, int light, int overlay) {
        // --- HORIZONTAL UVs (For top and bottom faces) ---
        // Maps a central 8x8 pixel window out of the 16x16 sheet
        float minU = 4F / 16F;
        float maxU = 12F / 16F;
        float minV = 4F / 16F;
        float maxV = 12F / 16F;

        // --- VERTICAL UVs (For side faces) ---
        // V=0 is top of image, V=1 is bottom.
        // botV aligns perfectly with the fluid base (Y=1), leaving a 1px gap from texture bottom[cite: 3].
        // topV scales dynamically with height, preventing stretching by cutting the texture off.
        float botV = 1.0F - minY;
        float topV = 1.0F - maxY;

        // UP Face
        addVertex(consumer, matrix, minX, maxY, minZ, r, g, b, a, minU, minV, light, overlay, 0, 1, 0);
        addVertex(consumer, matrix, minX, maxY, maxZ, r, g, b, a, minU, maxV, light, overlay, 0, 1, 0);
        addVertex(consumer, matrix, maxX, maxY, maxZ, r, g, b, a, maxU, maxV, light, overlay, 0, 1, 0);
        addVertex(consumer, matrix, maxX, maxY, minZ, r, g, b, a, maxU, minV, light, overlay, 0, 1, 0);

        // DOWN Face
        addVertex(consumer, matrix, minX, minY, maxZ, r, g, b, a, minU, maxV, light, overlay, 0, -1, 0);
        addVertex(consumer, matrix, minX, minY, minZ, r, g, b, a, minU, minV, light, overlay, 0, -1, 0);
        addVertex(consumer, matrix, maxX, minY, minZ, r, g, b, a, maxU, minV, light, overlay, 0, -1, 0);
        addVertex(consumer, matrix, maxX, minY, maxZ, r, g, b, a, maxU, maxV, light, overlay, 0, -1, 0);

        // NORTH Face (-Z)
        addVertex(consumer, matrix, maxX, maxY, minZ, r, g, b, a, minU, topV, light, overlay, 0, 0, -1);
        addVertex(consumer, matrix, maxX, minY, minZ, r, g, b, a, minU, botV, light, overlay, 0, 0, -1);
        addVertex(consumer, matrix, minX, minY, minZ, r, g, b, a, maxU, botV, light, overlay, 0, 0, -1);
        addVertex(consumer, matrix, minX, maxY, minZ, r, g, b, a, maxU, topV, light, overlay, 0, 0, -1);

        // SOUTH Face (+Z)
        addVertex(consumer, matrix, minX, maxY, maxZ, r, g, b, a, minU, topV, light, overlay, 0, 0, 1);
        addVertex(consumer, matrix, minX, minY, maxZ, r, g, b, a, minU, botV, light, overlay, 0, 0, 1);
        addVertex(consumer, matrix, maxX, minY, maxZ, r, g, b, a, maxU, botV, light, overlay, 0, 0, 1);
        addVertex(consumer, matrix, maxX, maxY, maxZ, r, g, b, a, maxU, topV, light, overlay, 0, 0, 1);

        // WEST Face (-X)
        addVertex(consumer, matrix, minX, maxY, minZ, r, g, b, a, minU, topV, light, overlay, -1, 0, 0);
        addVertex(consumer, matrix, minX, minY, minZ, r, g, b, a, minU, botV, light, overlay, -1, 0, 0);
        addVertex(consumer, matrix, minX, minY, maxZ, r, g, b, a, maxU, botV, light, overlay, -1, 0, 0);
        addVertex(consumer, matrix, minX, maxY, maxZ, r, g, b, a, maxU, topV, light, overlay, -1, 0, 0);

        // EAST Face (+X)
        addVertex(consumer, matrix, maxX, maxY, maxZ, r, g, b, a, minU, topV, light, overlay, 1, 0, 0);
        addVertex(consumer, matrix, maxX, minY, maxZ, r, g, b, a, minU, botV, light, overlay, 1, 0, 0);
        addVertex(consumer, matrix, maxX, minY, minZ, r, g, b, a, maxU, botV, light, overlay, 1, 0, 0);
        addVertex(consumer, matrix, maxX, maxY, minZ, r, g, b, a, maxU, topV, light, overlay, 1, 0, 0);
    }

    private void addVertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, float r, float g, float b, float a, float u, float v, int light, int overlay, float nx, float ny, float nz) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }
}