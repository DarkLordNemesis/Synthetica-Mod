package net.darklordnemesis.synthetica.block.entity;

import net.darklordnemesis.synthetica.block.ModBlocks;
import net.darklordnemesis.synthetica.block.utils.BundleDefinition;
import net.darklordnemesis.synthetica.block.utils.BundleTransferHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class PipeBlockEntity extends BlockEntity {
    // Hardcoded for now — later drive this from a GUI/NBT
    private static final BundleDefinition BUNDLE = BundleDefinition.of(
            new ItemStack(Items.REDSTONE, 1),
            new ItemStack(Items.IRON_INGOT, 4)
    );

    private static final int TRANSFER_INTERVAL = 1; // ticks between attempts
    private int tickCounter = 0;


    public PipeBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.PIPE_BE.get(), pos, blockState);
    }


    public static void tick(Level level, BlockPos pos, BlockState state, PipeBlockEntity blockEntity) {
        if (level.isClientSide) return;
        if (++blockEntity.tickCounter < TRANSFER_INTERVAL) return;
        blockEntity.tickCounter = 0;

        // Grab handlers on south (source) and north (destination)
        IItemHandler source = getAdjacentHandler(level, pos, Direction.SOUTH);

        if (source == null) return;
        if (isEmpty(source)) return;

        Set<IItemHandler> outputs = blockEntity.getOutputHandlers(source);
        if (outputs.isEmpty()) return;
        for (IItemHandler output : outputs) {
            BundleTransferHelper.tryTransferBundle(source, output, BUNDLE, 4);
        }
    }

    private static IItemHandler getAdjacentHandler(Level level, BlockPos pos, Direction dir) {
        if (level == null) return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(dir), dir.getOpposite());
    }

    private Set<IItemHandler> getOutputHandlers(IItemHandler source) {
        Set<IItemHandler> outputs = new HashSet<>();
        traverse(this.getBlockPos(), pos -> {
            for (Direction dir : Direction.values()) {
                IItemHandler handler = getAdjacentHandler(level, pos, dir);
                if (handler != null && handler != source) outputs.add(handler);
            }
        });
        return outputs;
    }

    private void traverse(BlockPos pos, Consumer<BlockPos> consumer) {
        Set<BlockPos> visited = new HashSet<>();
        visited.add(pos);
        consumer.accept(pos);
        traverse(pos, consumer, visited);

    }

    private void traverse(BlockPos pos, Consumer<BlockPos> consumer, Set<BlockPos> visited) {
        for (Direction dir : Direction.values()) {
            BlockPos next = pos.relative(dir);
            if (visited.contains(next)) continue;
            visited.add(next);
            assert level != null;
            if (level.getBlockState(next) == ModBlocks.BISMUTH_BLOCK.get().defaultBlockState()) {
                consumer.accept(next);
                traverse(next, consumer, visited);
            }
        }
    }

    private static boolean isEmpty(IItemHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }


}
