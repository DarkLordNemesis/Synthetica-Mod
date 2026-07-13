package net.darklordnemesis.synthetica.essentia;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.darklordnemesis.synthetica.registry.ModRegistries;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class Aspect {

    /**
     * CODEC
     *
     * Codec.lazyInitialized() defers construction until first use.
     * This solves two problems at once:
     *
     *  1. REGISTRATION ORDER — ModRegistries.ASPECT may not be populated
     *     yet when this class is loaded. lazyInitialized() means the codec
     *     is built on first use, by which point all registries are ready.
     *
     *  2. RECURSION — AspectComposition holds Holder<Aspect>, so its codec
     *     needs a codec for Aspect to build from. By the time the lazy codec
     *     is first evaluated, the Aspect registry exists and
     *     holderByNameCodec() can safely be called on it.
     *     We do NOT need Codec.recursive() here because AspectComposition
     *     stores Holder<Aspect> (a registry reference by ResourceLocation),
     *     not a full inline Aspect — so there is no infinite nesting in the
     *     serialized data, only a cross-reference via the registry key.
     *
     * JSON format for a primal aspect:
     * { "color": 16724992 }
     *
     * JSON format for a compound aspect:
     * {
     *   "color": 9699539,
     *   "composition": {
     *     "first": "synthetica:ignis",
     *     "second": "synthetica:aqua"
     *   }
     * }
     */
    public static final Codec<Aspect> CODEC = Codec.lazyInitialized(() ->
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("color").forGetter(Aspect::getColor),
                    AspectComposition.CODEC.optionalFieldOf("composition").forGetter(Aspect::getComposition)
            ).apply(instance, Aspect::new))
    );

    private final int color;
    private final Optional<AspectComposition> aspectComposition;

    public Aspect(int color, Optional<AspectComposition> aspectComposition) {
        this.color = color;
        this.aspectComposition = aspectComposition;
    }

    public Component getDisplayName(Holder<Aspect> holder) {
        ResourceLocation id = holder.unwrapKey()
                .orElseThrow()
                .location();
        return Component.translatable("aspect." + id.getNamespace() + "." + id.getPath());
    }

    public boolean isCompound() { return aspectComposition.isPresent(); }
    public boolean isPrimal()   { return aspectComposition.isEmpty();   }

    public Optional<AspectComposition> getComposition() { return aspectComposition; }

    public int getColor() { return color; }

    public float getRed()   { return ((color >> 16) & 0xFF) / 255.0f; }
    public float getGreen() { return ((color >>  8) & 0xFF) / 255.0f; }
    public float getBlue()  { return ( color        & 0xFF) / 255.0f; }
    public float getAlpha() { return ((color >> 24) & 0xFF) / 255.0f; }

    // -------------------------------------------------------------------------
    // AspectComposition
    // -------------------------------------------------------------------------

    /**
     * AspectComposition
     *
     * Stores the two primal (or compound) aspects that combine to form
     * a compound aspect. Both components are stored as Holder<Aspect>
     * so they are serialized as registry keys ("synthetica:ignis") rather
     * than inlined objects — this avoids any recursive nesting in the JSON.
     *
     * The codec is also lazy for the same registration-order reason as above:
     * holderByNameCodec() on the Aspect registry must not be called until
     * the registry is fully populated.
     */
    public record AspectComposition(
            @NotNull Holder<Aspect> first,
            @NotNull Holder<Aspect> second
    ) {
        /**
         * CODEC for AspectComposition.
         *
         * holderByNameCodec() serializes each Holder<Aspect> as its
         * ResourceLocation string ("synthetica:ignis"), resolving the
         * actual Holder from the registry on deserialization.
         * This is safe because:
         *  - Lazy init ensures the registry exists when this codec runs
         *  - Holders are resolved by key, not by recursing into Aspect data
         */
        public static final Codec<AspectComposition> CODEC = Codec.lazyInitialized(() ->
                RecordCodecBuilder.create(instance -> instance.group(
                        ModRegistries.ASPECT
                                .holderByNameCodec()
                                .fieldOf("first")
                                .forGetter(AspectComposition::first),
                        ModRegistries.ASPECT
                                .holderByNameCodec()
                                .fieldOf("second")
                                .forGetter(AspectComposition::second)
                ).apply(instance, AspectComposition::new))
        );

        public Holder<Aspect> getComponent1() { return first; }
        public Holder<Aspect> getComponent2() { return second; }
    }
}