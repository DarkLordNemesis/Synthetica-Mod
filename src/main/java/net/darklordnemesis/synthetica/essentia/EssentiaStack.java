package net.darklordnemesis.synthetica.essentia;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.darklordnemesis.synthetica.registry.ModRegistries;
import net.darklordnemesis.synthetica.util.Codecs;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class EssentiaStack {


    /**
     * Serializes a non-empty EssentiaStack to/from JSON or NBT.
     *
     * Codec.lazyInitialized() defers construction until first use so the
     * Aspect registry is guaranteed to be populated by then.
     *
     * holderByNameCodec() serializes the Holder<Aspect> as its
     * ResourceLocation string ("synthetica:ignis"), which is human-readable
     * in NBT editors and robust to registry ID reordering.
     *
     * Codecs.POSITIVE_LONG validates that amount is >= 1 at the codec
     * level, so malformed data (amount=0) is rejected before it reaches
     * the constructor — mirrors how ItemStack handles this.
     *
     * JSON format:
     * {
     *   "aspect": "synthetica:ignis",
     *   "amount": 32
     * }
     */
    public static final Codec<EssentiaStack> CODEC = Codec.lazyInitialized(
            () -> RecordCodecBuilder.create(instance -> instance.group(
                    ModRegistries.ASPECT
                            .holderByNameCodec()
                            .fieldOf("aspect")
                            .forGetter(EssentiaStack::getAspect),
                    Codecs.POSITIVE_LONG
                            .fieldOf("amount")
                            .forGetter(EssentiaStack::getAmount)
            ).apply(instance, EssentiaStack::new))
    );

    /**
     * OPTIONAL_CODEC — like CODEC but tolerates an absent or empty stack.
     *
     * Decoding: a missing field or EMPTY stack deserializes to EssentiaStack.EMPTY
     *   rather than throwing a codec error.
     * Encoding: EMPTY stacks serialize to nothing (field is omitted entirely).
     *
     * Use this for any field that may legitimately be absent —
     * e.g. jar contents, recipe outputs, loot table entries.
     *
     * xmap() converts between Optional<EssentiaStack> (what optionalFieldOf
     * produces) and EssentiaStack (what callers expect):
     *   Optional.empty()     → EssentiaStack.EMPTY
     *   Optional.of(stack)   → stack
     */
    public static final Codec<EssentiaStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC)
            .xmap(optional -> optional.orElse(EssentiaStack.EMPTY), stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack));

    /**
     * STREAM_CODEC — serializes EssentiaStack over the network.
     *
     * Uses RegistryFriendlyByteBuf so it can carry the registry context
     * needed to resolve Holders on the receiving end.
     *
     * holderRegistry() sends the Holder<Aspect> as its synced integer
     * registry ID — far more compact than a ResourceLocation string.
     * This works because ModRegistries.ASPECT was built with .sync(true).
     *
     * VAR_LONG writes the amount as a variable-length integer — small
     * numbers (like 1–64) only use 1–2 bytes instead of always 8.
     *
     * The constructor lambda on the receiving end returns EMPTY if amount
     * is 0, keeping the invariant that only EMPTY has amount=0.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, EssentiaStack> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.holderRegistry(ModRegistries.ASPECT_REGISTRY_KEY),
                    EssentiaStack::getAspect,
                    ByteBufCodecs.VAR_LONG,
                    EssentiaStack::getAmount,
                    (aspect, amount) -> amount <= 0 ? EssentiaStack.EMPTY : new EssentiaStack(aspect, amount)
            );




    public static final EssentiaStack EMPTY = new EssentiaStack(null);

    @Nullable
    private final Holder<Aspect> aspect;
    private long amount;

    public EssentiaStack(Holder<Aspect> aspect, long amount) {
        this.aspect = aspect;
        this.amount = amount;
    }

    private EssentiaStack(@Nullable Void unused) {
        this.aspect = null;
        this.amount = 0;
    }

    public boolean isEmpty() {
        return this == EMPTY || this.amount <= 0;
    }

    @Nullable
    public Holder<Aspect> getAspect() {
        return this.aspect;
    }

    public long getAmount() {
        return this.isEmpty() ? 0 : this.amount;
    }

    public void setAmount(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.amount = amount;
    }

    public void grow(long amount) {
        this.setAmount(this.getAmount() + amount);
    }

    public void shrink(long amount) {
        this.setAmount(this.getAmount() - amount);
    }

    public EssentiaStack copy() {
        if (this.isEmpty()) {
            return EMPTY;
        }
        return new EssentiaStack(this.aspect, this.amount);
    }

    public EssentiaStack copyWithAmount(long amount) {
        if (this.isEmpty()) {
            return EMPTY;
        }
        return new EssentiaStack(this.aspect, amount);
    }

    /**
     * Splits off a stack of the given amount of this stack and reduces this stack by the amount.
     *
     * @param amount The amount to split
     * @return The split stack
     */
    public EssentiaStack split(long amount) {
        long i = Math.min(amount, this.amount);
        EssentiaStack stack = this.copyWithAmount(i);
        this.shrink(i);
        return stack;
    }


    public boolean is(Holder<Aspect> aspect) {
        return Objects.equals(this.getAspect(), aspect);
    }

    public boolean is(HolderSet<Aspect> set) {
        return set.contains(this.aspect);
    }

    public boolean isSameAspect(EssentiaStack other) {
        return this.is(other.getAspect());
    }

    /**
     * Checks if two EssentiaStacks are the same. This checks the aspect and amount.
     *
     * @param first First EssentiaStack
     * @param second Second EssentiaStack
     * @return {@code true} if the two stacks are the same
     */
    public static boolean matches(EssentiaStack first, EssentiaStack second) {
        if (first == second) {
            return true;
        } else {
            return isSameAspect(first, second) && isSameAmount(first, second);
        }
    }

    /**
     * Checks if two EssentiaStacks have the same aspect.
     *
     * @param first First EssentiaStack
     * @param second Second EssentiaStack
     * @return {@code true} if the two stacks have the same aspect
     */
    public static boolean isSameAspect(EssentiaStack first, EssentiaStack second) {
        return first.isSameAspect(second);
    }

    /**
     * Checks if two EssentiaStacks have the same amount.
     *
     * @param first First EssentiaStack
     * @param second Second EssentiaStack
     * @return {@code true} if the two stacks have the same amount
     */
    public static boolean isSameAmount(EssentiaStack first, EssentiaStack second) {
        return first.getAmount() == second.getAmount();
    }

    /**
     * Returns a component that describes the stack.
     *
     * @return translatable component of {@code essentia.stack.empty} if empty else {@code essentia.stack.full} with aspect and amount
     */
    public Component getDisplayName() {
        if (this.isEmpty()) {
            return Component.translatable("essentia.stack.empty");
        }
        return Component.translatable("essentia.stack.full", this.getAspect().value().getDisplayName(this.getAspect()), this.getAmount());
    }
}
