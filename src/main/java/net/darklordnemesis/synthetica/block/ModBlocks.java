package net.darklordnemesis.synthetica.block;

import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.block.custom.*;
import net.darklordnemesis.synthetica.item.ModItems;
import net.darklordnemesis.synthetica.item.custom.EssentiaJarBlockItem;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Synthetica.MOD_ID);

    //register blocks
    public static final DeferredBlock<Block> BISMUTH_BLOCK = registerBlockWithItem("bismuth_block",
            () -> new Block(BlockBehaviour.Properties.of().strength(2f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));

    public static final DeferredBlock<Block> BISMUTH_ORE = registerBlockWithItem("bismuth_ore",
            () -> new DropExperienceBlock(UniformInt.of(1, 10), BlockBehaviour.Properties.of().strength(2f).requiresCorrectToolForDrops().sound(SoundType.STONE)));

    public static final DeferredBlock<Block> PEDESTAL = registerBlockWithItem("pedestal",
            () -> new PedestalBlock(BlockBehaviour.Properties.of().noOcclusion()));

    public static final DeferredBlock<Block> PIPE = registerBlockWithItem("pipe",
            () -> new PipeBlock(BlockBehaviour.Properties.of().noOcclusion()));

    public static final DeferredBlock<Block> CABLE = registerBlockWithItem("cable",
            () -> new CableBlock(BlockBehaviour.Properties.of().noOcclusion()));

    public static final DeferredBlock<Block> GRAVITATIONAL_ANOMALY = registerBlockWithItem("gravitational_anomaly",
            () -> new GravitationalAnomalyBlock(BlockBehaviour.Properties.of().noOcclusion()));

    public static final DeferredBlock<Block> ESSENTIA_JAR = registerBlockWithCustomItem("essentia_jar",
            () -> new EssentiaJarBlock(BlockBehaviour.Properties.of().noOcclusion()),
            () -> new EssentiaJarBlockItem(ModBlocks.ESSENTIA_JAR.get(), new Item.Properties())
    );
    //#####

    private static <T extends Block> DeferredBlock<T> registerBlockWithItem(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }

    private static <T extends Block> DeferredBlock<T> registerBlockWithCustomItem(String name, Supplier<T> block, Supplier<Item> item) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, item);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}