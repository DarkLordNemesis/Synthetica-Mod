package net.darklordnemesis.synthetica.item.custom;

import net.darklordnemesis.synthetica.item.custom.renderer.EssentiaJarItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class EssentiaJarBlockItem extends BlockItem {

    public EssentiaJarBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return EssentiaJarItemRenderer.INSTANCE;
            }
        });
    }
}