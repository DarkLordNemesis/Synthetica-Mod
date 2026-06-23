package net.darklordnemesis.synthetica.block.entity;

import net.darklordnemesis.synthetica.block.ModBlocks;
import net.darklordnemesis.synthetica.block.utils.BundleDefinition;
import net.darklordnemesis.synthetica.block.utils.BundleTransferHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PipeBlockEntity extends BlockEntity {

    private static final BundleDefinition BUNDLE = BundleDefinition.of(
            new ItemStack(Items.REDSTONE, 1),
            new ItemStack(Items.IRON_INGOT, 4)
    );

    private static final int TRANSFER_INTERVAL = 1;
    private static final int REBUILD_INTERVAL = 10; // minimum number of ticks between rebuilds
    private int tickCounter = 0;
    private int rebuildTickCounter = 0;

    // Cached capability lookups for each connected output. Self-refreshing.
    private final List<BlockCapabilityCache<IItemHandler, Direction>> outputCaches = new ArrayList<>();
    private boolean networkDirty = true; // true on creation/load -> builds cache on first tick

    public PipeBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.PIPE_BE.get(), pos, blockState);
    }

    /** Call whenever the connected cable network topology may have changed. */
    public void markNetworkDirty() {
        this.networkDirty = true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PipeBlockEntity be) {
        if (level.isClientSide) return;
        be.rebuildTickCounter = Math.max(0, be.rebuildTickCounter - 1);
        if (++be.tickCounter < TRANSFER_INTERVAL) return;
        be.tickCounter = 0;

        if (be.networkDirty) {
            be.rebuildOutputCache((ServerLevel) level);
            be.networkDirty = false;
        }

        IItemHandler source = getAdjacentHandler(level, pos, Direction.SOUTH);
        if (source == null || isEmpty(source)) return;

        for (BlockCapabilityCache<IItemHandler, Direction> cache : be.outputCaches) {
            IItemHandler output = cache.getCapability();
            if (output == null) continue; // capability disappeared; rebuild will drop it next dirty pass
            BundleTransferHelper.tryTransferBundle(source, output, BUNDLE, 4);
        }
    }

    /**
     * Iterative BFS over the connected cable network. Rebuilds the list of
     * cached output capability lookups from scratch.
     */
    private void rebuildOutputCache(ServerLevel level) {
        rebuildTickCounter = REBUILD_INTERVAL;
        outputCaches.clear();

        // Never register the pipe's own source position as an output —
        // prevents looping items back into the inventory they came from.
        BlockPos sourcePos = getBlockPos().relative(Direction.SOUTH);

        Set<BlockPos> visitedCables = new HashSet<>();
        Set<BlockPos> seenOutputs = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        BlockPos start = getBlockPos();
        visitedCables.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.relative(dir);

                if (level.getBlockState(neighborPos).is(ModBlocks.CABLE.get())) {
                    if (visitedCables.add(neighborPos)) {
                        queue.add(neighborPos);
                    }
                    continue;
                }

                if (neighborPos.equals(sourcePos)) continue;
                if (!seenOutputs.add(neighborPos)) continue; // already cached this block

                Direction sideTowardsCable = dir.getOpposite();

                // Cheap probe first — don't bother creating a tracked cache
                // for positions that don't expose a handler at all.
                IItemHandler probe = level.getCapability(
                        Capabilities.ItemHandler.BLOCK, neighborPos, sideTowardsCable);
                if (probe == null) continue;

                BlockCapabilityCache<IItemHandler, Direction> cache = BlockCapabilityCache.create(
                        Capabilities.ItemHandler.BLOCK,
                        level,
                        neighborPos,
                        sideTowardsCable,
                        () -> !this.isRemoved(),   // validator: stop tracking once this pipe is gone
                        this::markNetworkDirty     // if this capability changes, re-evaluate topology
                );

                outputCaches.add(cache);
            }
        }
    }

    private static IItemHandler getAdjacentHandler(Level level, BlockPos pos, Direction dir) {
        if (level == null) return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(dir), dir.getOpposite());
    }

    private static boolean isEmpty(IItemHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }
}