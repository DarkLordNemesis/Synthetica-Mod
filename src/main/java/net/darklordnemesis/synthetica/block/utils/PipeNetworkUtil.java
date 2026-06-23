package net.darklordnemesis.synthetica.block.utils;

import net.darklordnemesis.synthetica.block.ModBlocks;
import net.darklordnemesis.synthetica.block.entity.PipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class PipeNetworkUtil {

    // Safety cap so a single block update on a huge network can't cause a lag spike
    private static final int MAX_VISITED = 4096;

    /**
     * BFS over the connected cable network starting at startCablePos.
     * Marks every PipeBlockEntity touching the network as dirty so it
     * rebuilds its output cache next tick.
     *
     * Call this from a cable block's neighborChanged.
     */
    public static void markConnectedPipesDirty(Level level, BlockPos startCablePos) {
        if (level.isClientSide) return;

        Set<BlockPos> visitedCables = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(startCablePos);
        visitedCables.add(startCablePos);

        int visitedCount = 0;
        while (!queue.isEmpty() && visitedCount++ < MAX_VISITED) {
            BlockPos current = queue.poll();

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.relative(dir);

                if (level.getBlockState(neighborPos).is(ModBlocks.CABLE.get())) {
                    if (visitedCables.add(neighborPos)) {
                        queue.add(neighborPos);
                    }
                } else if (level.getBlockEntity(neighborPos) instanceof PipeBlockEntity pipe) {
                    pipe.markNetworkDirty();
                }
            }
        }
    }
}