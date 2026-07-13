package net.darklordnemesis.synthetica.item.custom.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.darklordnemesis.synthetica.block.ModBlocks;
import net.darklordnemesis.synthetica.block.entity.EssentiaJarBlockEntity;
import net.darklordnemesis.synthetica.essentia.EssentiaStack;
import net.darklordnemesis.synthetica.essentia.SingleEssentiaTank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class EssentiaJarItemRenderer extends BlockEntityWithoutLevelRenderer {

    public static final EssentiaJarItemRenderer INSTANCE = new EssentiaJarItemRenderer();

    private final EssentiaJarBlockEntity dummyBE = new EssentiaJarBlockEntity(
            BlockPos.ZERO,
            ModBlocks.ESSENTIA_JAR.get().defaultBlockState()
    );

    public EssentiaJarItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // FIX: Explicitly clear out the dummy block entity's tank before evaluating the current stack
        if (this.dummyBE.getEssentiaHandler(Direction.UP) instanceof SingleEssentiaTank tank) {
            tank.setEssentia(EssentiaStack.EMPTY);
        }

        // Render the base empty jar model
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                ModBlocks.ESSENTIA_JAR.get().defaultBlockState(),
                poseStack, bufferSource, packedLight, packedOverlay,
                net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null
        );

        // Inject data components from the current item stack
        this.dummyBE.readFromItem(stack);

        // Delegate to fluid renderer if not empty
        Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(
                this.dummyBE, poseStack, bufferSource, packedLight, packedOverlay
        );
    }
}