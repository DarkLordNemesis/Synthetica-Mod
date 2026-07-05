package net.darklordnemesis.synthetica.network;

import io.netty.buffer.ByteBuf;
import net.darklordnemesis.synthetica.Synthetica;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// direction will be 1 (up) or -1 (down)
public record CycleSheathPayload(int direction) implements CustomPacketPayload {
    public static final Type<CycleSheathPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Synthetica.MOD_ID, "cycle_sheath"));

    public static final StreamCodec<ByteBuf, CycleSheathPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, CycleSheathPayload::direction,
            CycleSheathPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}