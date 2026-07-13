package net.darklordnemesis.synthetica.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.darklordnemesis.synthetica.essentia.EssentiaStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * EssentiaContents
 *
 * A DataComponent record that carries essentia data on an ItemStack.
 *
 * WHY NOT BLOCK_ENTITY_DATA?
 *  CustomData (BLOCK_ENTITY_DATA) stores raw NBT and decodes it with plain
 *  NbtOps — no registry context. When the ItemStack crosses the network,
 *  vanilla decodes each DataComponent using its registered StreamCodec.
 *  CustomData's StreamCodec doesn't resolve registry holders, so any NBT
 *  that contains a registry-keyed value (like our Holder<Aspect>) throws
 *  "Missing id for entity" because the holder can't be resolved.
 *
 *  A proper DataComponent has its OWN StreamCodec which runs through
 *  RegistryFriendlyByteBuf — giving it full registry context. That's
 *  exactly what EssentiaStack.STREAM_CODEC already provides.
 *
 * KEY CONCEPTS DEMONSTRATED:
 *  - DataComponent as a record — records are ideal because they are
 *    immutable and equals()/hashCode() are generated automatically,
 *    which the component system relies on for change detection.
 *  - Reusing EssentiaStack.CODEC and STREAM_CODEC — the component
 *    doesn't need its own serialization logic, it just delegates.
 *  - EMPTY sentinel — mirrors the EssentiaStack.EMPTY pattern so
 *    callers always have a safe non-null default to work with.
 */
public record EssentiaContents(EssentiaStack stack) {

    /** Sentinel value for an empty jar item — no aspect, no amount. */
    public static final EssentiaContents EMPTY = new EssentiaContents(EssentiaStack.EMPTY);

    /**
     * CODEC — used when the component is saved to disk (item in a chest,
     * player inventory NBT, etc.). Delegates to EssentiaStack.CODEC which
     * uses holderByNameCodec() → serializes as a ResourceLocation string,
     * safe for NBT where registry context is available on load.
     */
    public static final Codec<EssentiaContents> CODEC =
            EssentiaStack.CODEC.xmap(EssentiaContents::new, EssentiaContents::stack);

    /**
     * STREAM_CODEC — used when the component crosses the network inside an
     * ItemStack packet. Delegates to EssentiaStack.STREAM_CODEC which uses
     * ByteBufCodecs.holderRegistry() — sends the aspect as a synced integer
     * ID through RegistryFriendlyByteBuf. This is what fixes the crash:
     * the holder is resolved correctly on the receiving end.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, EssentiaContents> STREAM_CODEC =
            EssentiaStack.STREAM_CODEC.map(EssentiaContents::new, EssentiaContents::stack);

    public boolean isEmpty() {
        return stack.isEmpty();
    }
}