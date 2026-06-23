package net.darklordnemesis.synthetica.block.custom;

import net.darklordnemesis.synthetica.block.utils.PipeNetworkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class CableBlock extends Block {
    public CableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            PipeNetworkUtil.markConnectedPipesDirty(level, pos);
        }
    }
}