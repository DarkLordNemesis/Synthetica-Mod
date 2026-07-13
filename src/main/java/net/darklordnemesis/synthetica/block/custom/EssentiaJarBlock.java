package net.darklordnemesis.synthetica.block.custom;

import com.mojang.serialization.MapCodec;
import net.darklordnemesis.synthetica.block.ModBlocks;
import net.darklordnemesis.synthetica.block.entity.EssentiaJarBlockEntity;
import net.darklordnemesis.synthetica.component.EssentiaContents;
import net.darklordnemesis.synthetica.component.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * EssentiaJarBlock
 *
 * The block class for the essentia jar. Key responsibilities:
 *  - Creating the BlockEntity (newBlockEntity)
 *  - Dropping the jar item WITH its essentia contents when broken
 *  - Preserving essentia data when the block is picked up (middle-click)
 *
 * WHY playerWillDestroy() INSTEAD OF A LOOT TABLE?
 *  Loot tables can drop items with BE data via the "copy_components"
 *  function, but that requires a DataComponent registered on the item.
 *  Since we are serializing directly to NBT for now, it is simpler and
 *  more reliable to handle the drop manually here and return an empty
 *  list from getDrops() so the loot table doesn't also fire.
 */
public class EssentiaJarBlock extends BaseEntityBlock {
    private static final MapCodec<? extends BaseEntityBlock> CODEC = simpleCodec(EssentiaJarBlock::new);

    private static final VoxelShape JAR_BODY = Block.box(3.0, 0.0, 3.0, 13.0, 12.0, 13.0); // From element 1
    private static final VoxelShape JAR_NECK = Block.box(5.0, 12.0, 5.0, 11.0, 14.0, 11.0); // From element 2[cite: 3]

    // Combine both boxes into a single outline shape
    private static final VoxelShape SHAPE = Shapes.or(JAR_BODY, JAR_NECK);

    public EssentiaJarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    // -------------------------------------------------------------------------
    // BlockEntity wiring
    // -------------------------------------------------------------------------

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssentiaJarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        // No tick needed for passive storage — add one later for animations/drain
        return null;
    }

    @Override
    protected VoxelShape getVisualShape(BlockState p_309057_, BlockGetter p_308936_, BlockPos p_308956_, CollisionContext p_309006_) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity be) {
            return be.interact(level, player);
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // ENTITYBLOCK_ANIMATED routes rendering through the BlockEntityRenderer
        // instead of a baked model — required for the fluid layer we'll add later.
        return RenderShape.MODEL;
    }

    // -------------------------------------------------------------------------
    // Breaking — preserve essentia in the dropped item
    // -------------------------------------------------------------------------

    /**
     * playerWillDestroy — fires on the SERVER just before the block is removed.
     * At this point the BlockEntity is still alive, so we can read its contents.
     * <p>
     * We save the BlockEntity's NBT directly onto the dropped ItemStack using
     * ItemStack.applyComponents / set, but the simplest approach compatible with
     * how our BE saves is to copy the saved CompoundTag into the item's custom
     * data component using DataComponents.BLOCK_ENTITY_DATA.
     * <p>
     * We then spawn the item manually so it appears at the block's centre.
     * The harvest check (isCorrectToolForDrops / canHarvestBlock) is respected
     * via the super call — we only spawn the drop if the super would have too.
     *
     * @return
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            if (level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity jarBE) {
                // Build the item drop with its essentia data attached
                ItemStack drop = createDropWithContents(jarBE, level.registryAccess()
                        instanceof net.minecraft.core.HolderLookup.Provider prov ? prov : null);

                // Spawn the item at the centre of the block
                Vec3 centre = Vec3.atCenterOf(pos);
                net.minecraft.world.entity.item.ItemEntity itemEntity =
                        new net.minecraft.world.entity.item.ItemEntity(
                                level, centre.x, centre.y, centre.z, drop);
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
        return state;
    }

    /**
     * getDrops — return empty list so the normal loot table doesn't also
     * fire and drop a second (empty) jar alongside our content-filled one.
     *
     * If the block was broken by an explosion or other non-player means,
     * playerWillDestroy doesn't fire. In that case we fall through to here
     * and drop a plain empty jar via the loot table if one is defined,
     * or nothing if not. Adjust to your preference.
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // Check if there's a block entity in the loot context
        LootParams params = builder.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
        net.minecraft.world.level.block.entity.BlockEntity be =
                params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (be instanceof EssentiaJarBlockEntity jarBE) {
            // Non-player break (explosion, piston, etc.) — still preserve contents
            HolderLookupProviderHolder holder = new HolderLookupProviderHolder(params);
            return List.of(createDropWithContents(jarBE, holder.get()));
        }

        // Fallback: plain jar item
        return List.of(new ItemStack(ModBlocks.ESSENTIA_JAR.get()));
    }

    /**
     * getCloneItemStack — called when the player middle-clicks the block in
     * creative mode. Returns a jar item with the current essentia data so
     * creative players can copy a filled jar.
     */
    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level,
                                       BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity jarBE
                && level instanceof net.minecraft.core.HolderLookup.Provider prov) {
            return createDropWithContents(jarBE, prov);
        }
        return super.getCloneItemStack(level, pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable net.minecraft.world.entity.LivingEntity placer,
                            ItemStack stack) {
        if (!level.isClientSide
                && stack.has(ModDataComponents.ESSENTIA_CONTENTS.get())
                && level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity jarBE) {
            jarBE.readFromItem(stack);
        }
    }


    // -------------------------------------------------------------------------
    // Helper: build an ItemStack carrying the jar's essentia NBT
    // -------------------------------------------------------------------------

    /**
     * Creates the dropped/picked-up ItemStack and attaches the BlockEntity's
     * saved NBT to it via DataComponents.BLOCK_ENTITY_DATA.
     *
     * DataComponents.BLOCK_ENTITY_DATA is vanilla's standard component for
     * carrying BlockEntity NBT on an ItemStack — used by banners, shulker
     * boxes, etc. When EssentiaJarBlock.setPlacedBy or the block's onPlace
     * restores the BlockEntity, it reads this component automatically via
     * the standard BaseEntityBlock machinery.
     *
     * If registries is null (shouldn't happen in normal play) we fall back
     * to a plain empty jar.
     */
    private ItemStack createDropWithContents(EssentiaJarBlockEntity jarBE,
                                             @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(ModBlocks.ESSENTIA_JAR.get());

        if (jarBE.isEmpty()) {
            return stack; // empty jar — no component needed
        }

        // Store the essentia as a proper DataComponent instead of BLOCK_ENTITY_DATA.
        // EssentiaContents.STREAM_CODEC uses RegistryFriendlyByteBuf, so the
        // Holder<Aspect> is resolved correctly when this ItemStack crosses the network.
        stack.set(ModDataComponents.ESSENTIA_CONTENTS.get(),
                new EssentiaContents(jarBE.getEssentia()));

        return stack;
    }


    /**
     * Small helper to extract the HolderLookup.Provider from LootParams,
     * which doesn't expose it directly.
     */
    private static class HolderLookupProviderHolder {
        private final net.minecraft.core.HolderLookup.Provider provider;

        HolderLookupProviderHolder(LootParams params) {
            // LootParams carries a Level reference internally; the Level itself
            // implements HolderLookup.Provider in 1.21.
            this.provider = params.getLevel().registryAccess();
        }

        net.minecraft.core.HolderLookup.Provider get() {
            return provider;
        }
    }
}