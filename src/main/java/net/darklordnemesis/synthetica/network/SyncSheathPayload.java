package net.darklordnemesis.synthetica.network;

import io.netty.buffer.ByteBuf;
import net.darklordnemesis.synthetica.Synthetica;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

public record SyncSheathPayload(UUID playerId, List<KatanaSyncInfo> katanas) implements CustomPacketPayload {
    public static final Type<SyncSheathPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Synthetica.MOD_ID, "sync_sheath"));

    public static final StreamCodec<ByteBuf, SyncSheathPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SyncSheathPayload::playerId,
            // Apply the list codec to our custom object codec
            KatanaSyncInfo.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncSheathPayload::katanas,
            SyncSheathPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}