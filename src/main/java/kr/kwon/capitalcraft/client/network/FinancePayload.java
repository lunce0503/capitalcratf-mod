package kr.kwon.capitalcraft.client.network;

import java.nio.charset.StandardCharsets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FinancePayload(String json) implements CustomPacketPayload {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath("capitalcraft", "main");
    public static final Type<FinancePayload> TYPE = new Type<>(CHANNEL);
    public static final StreamCodec<RegistryFriendlyByteBuf, FinancePayload> CODEC = new StreamCodec<>() {
        @Override
        public FinancePayload decode(RegistryFriendlyByteBuf buffer) {
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return new FinancePayload(new String(bytes, StandardCharsets.UTF_8));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FinancePayload payload) {
            buffer.writeBytes(payload.json().getBytes(StandardCharsets.UTF_8));
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
