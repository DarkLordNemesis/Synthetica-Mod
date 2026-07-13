package net.darklordnemesis.synthetica.block.custom;

import com.mojang.serialization.MapCodec;
import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.block.entity.GravitationalAnomalyBlockEntity;
import net.darklordnemesis.synthetica.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class GravitationalAnomalyBlock extends BaseEntityBlock {
    private static final MapCodec<GravitationalAnomalyBlock> CODEC = simpleCodec(GravitationalAnomalyBlock::new);

    public GravitationalAnomalyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GravitationalAnomalyBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.GRAVITATIONAL_ANOMALY_BE.get(), (level1, blockPos, blockState1, blockEntity) -> blockEntity.tick(level1, blockPos, blockState1, blockEntity));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof GravitationalAnomalyBlockEntity be) {
            System.out.println("GravitationalAnomaly with mass " + be.getMass() + " and radius " + be.getRadius() + " removed");
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
