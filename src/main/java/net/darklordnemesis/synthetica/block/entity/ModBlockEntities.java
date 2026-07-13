package net.darklordnemesis.synthetica.block.entity;

import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Synthetica.MOD_ID);

    public static final Supplier<BlockEntityType<PedestalBlockEntity>> PEDESTAL_BE = BLOCK_ENTITIES.register("pedestal_be", () -> BlockEntityType.Builder.of(
            PedestalBlockEntity::new, ModBlocks.PEDESTAL.get()).build(null));

    public static final Supplier<BlockEntityType<PipeBlockEntity>> PIPE_BE = BLOCK_ENTITIES.register("pipe_be", () -> BlockEntityType.Builder.of(
            PipeBlockEntity::new, ModBlocks.PIPE.get()).build(null));

    public static final Supplier<BlockEntityType<GravitationalAnomalyBlockEntity>> GRAVITATIONAL_ANOMALY_BE = BLOCK_ENTITIES.register("gravitational_anomaly_be", () -> BlockEntityType.Builder.of(
            GravitationalAnomalyBlockEntity::new, ModBlocks.GRAVITATIONAL_ANOMALY.get()).build(null));

    public static final Supplier<BlockEntityType<EssentiaJarBlockEntity>> ESSENTIA_JAR_BE = BLOCK_ENTITIES.register("essentia_jar_be", () -> BlockEntityType.Builder.of(
            EssentiaJarBlockEntity::new, ModBlocks.ESSENTIA_JAR.get()).build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
