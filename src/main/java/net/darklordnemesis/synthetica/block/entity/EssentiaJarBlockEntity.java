package net.darklordnemesis.synthetica.block.entity;

import com.mojang.serialization.DynamicOps;
import net.darklordnemesis.synthetica.component.EssentiaContents;
import net.darklordnemesis.synthetica.component.ModDataComponents;
import net.darklordnemesis.synthetica.essentia.EssentiaStack;
import net.darklordnemesis.synthetica.essentia.IEssentiaHandler;
import net.darklordnemesis.synthetica.essentia.SingleEssentiaTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EssentiaJarBlockEntity
 *
 * Stores a SingleEssentiaTank with 64 capacity. Handles:
 *  - NBT save/load using EssentiaStack.CODEC via NbtOps (no manual field writes)
 *  - Client sync via getUpdateTag / getUpdatePacket / onDataPacket
 *  - Notifying the level to save when contents change via onContentsChanged()
 *
 * KEY CONCEPTS DEMONSTRATED:
 *  - Using an existing Codec with NbtOps to serialize directly to/from Tag,
 *    so save/load logic is always in sync with the codec — no duplication.
 *  - registries.createSerializationContext(NbtOps.INSTANCE) — produces a
 *    DynamicOps<Tag> that is registry-aware, required for Holder serialization.
 *  - The two-method client sync pattern:
 *      getUpdateTag()    → initial chunk send (full data)
 *      getUpdatePacket() → per-change delta (also full data for simplicity)
 *      onDataPacket()    → client applies the received tag
 *  - Overriding onContentsChanged() inside an anonymous subclass of
 *    SingleEssentiaTank so the tank can tell the BE to save and sync
 *    without the tank needing a reference to the BE itself.
 */
public class EssentiaJarBlockEntity extends BlockEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(EssentiaJarBlockEntity.class);
    private static final String NBT_KEY = "essentia";
    public static final long CAPACITY = 64;

    /**
     * Anonymous subclass of SingleEssentiaTank that overrides onContentsChanged()
     * to call back into the BlockEntity. This is the cleanest way to wire the
     * tank's change notification to the BE without circular references or listeners.
     */
    private final SingleEssentiaTank tank = new SingleEssentiaTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    public EssentiaJarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ESSENTIA_JAR_BE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Public accessors
    // -------------------------------------------------------------------------

    public IEssentiaHandler getEssentiaHandler(Direction side) {
        if (side == Direction.UP) {
            return tank;
        }
        return null;
    }

    /** Convenience: the current contents as an EssentiaStack (copy). */
    public EssentiaStack getEssentia() {
        return tank.getEssentia();
    }

    public boolean isEmpty() {
        return tank.isEmpty();
    }

    public long getAmount() {
        return tank.getEssentiaAmount();
    }

    public long getCapacity() {
        return CAPACITY;
    }

    /** Fill fraction 0.0–1.0, used by the renderer later. */
    public float getFillFraction() {
        return Math.min(1, (float) tank.getEssentiaAmount() / CAPACITY);
    }

    public void readFromItem(ItemStack stack) {
        EssentiaContents contents = stack.get(ModDataComponents.ESSENTIA_CONTENTS.get());
        if (contents != null && !contents.isEmpty()) {
            tank.setEssentia(contents.stack());
            setChanged();
        }
    }

    public InteractionResult interact(Level level, Player player) {
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                tank.clear();
            } else {
                player.displayClientMessage(getEssentia().getDisplayName(), true);
            }
        }
        return InteractionResult.SUCCESS;
    }


    // -------------------------------------------------------------------------
    // NBT — save and load
    // -------------------------------------------------------------------------

    /**
     * saveAdditional — writes our essentia data into the chunk's NBT.
     *
     * We use EssentiaStack.CODEC with NbtOps rather than manually writing
     * fields. This guarantees save and load always use the same format, and
     * means adding fields to EssentiaStack.CODEC automatically updates saves.
     *
     * registries.createSerializationContext(NbtOps.INSTANCE) produces a
     * registry-aware DynamicOps<Tag>. This is required because the codec
     * serializes Holder<Aspect> which needs registry context to resolve
     * ResourceLocations to Holder objects and back.
     *
     * If the tank is empty we skip writing entirely — omitting the key is
     * cleaner than storing an empty marker, and OPTIONAL_CODEC on load
     * handles absence gracefully.
     */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        EssentiaStack stored = tank.getEssentia();
        if (!stored.isEmpty()) {
            DynamicOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
            EssentiaStack.CODEC.encodeStart(ops, stored)
                    .resultOrPartial(err -> LOGGER.error("Failed to save EssentiaStack: {}", err))
                    .ifPresent(encoded -> tag.put(NBT_KEY, encoded));
        }
    }

    /**
     * loadAdditional — reads essentia data back from NBT on chunk load.
     *
     * We use OPTIONAL_CODEC so a missing key (empty jar) simply produces
     * EssentiaStack.EMPTY rather than a codec error. If decoding fails
     * (e.g. corrupted data) we log and leave the tank empty.
     *
     * After decoding we set the tank's essentia directly via setEssentia(),
     * bypassing fill() so we don't accidentally reject a valid stored value
     * due to the aspect-lock check on a non-empty tank.
     */
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // Reset first so a load always starts from a clean state
        tank.setEssentia(EssentiaStack.EMPTY);

        if (tag.contains(NBT_KEY)) {
            DynamicOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
            EssentiaStack.CODEC.parse(ops, tag.get(NBT_KEY))
                    .resultOrPartial(err -> LOGGER.error("Failed to load EssentiaStack: {}", err))
                    .ifPresent(tank::setEssentia);
        }
    }

    // -------------------------------------------------------------------------
    // Client sync — two-method pattern
    // -------------------------------------------------------------------------

    /**
     * getUpdateTag — called server-side for both the initial chunk send
     * and block update packets. We reuse saveWithoutMetadata so the client
     * always receives the full current state.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    /**
     * getUpdatePacket — the packet sent when level.sendBlockUpdated() fires
     * (triggered from onContentsChanged above). The packet payload is built
     * from getUpdateTag() automatically.
     */
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * onDataPacket — called CLIENT-SIDE when the update packet arrives.
     * We apply the tag (which runs loadAdditional) and then mark the
     * block for a render refresh.
     */
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt,
                             HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, registries);
        }
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
        }
    }
}