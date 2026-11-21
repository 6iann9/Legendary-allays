package net.iann.legendaryallays.network;

import net.iann.legendaryallays.LegendaryAllays;
import net.iann.legendaryallays.command.LuteCommand;
import net.iann.legendaryallays.companion.CompanionManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The server determines targets, ownership and release positions, never the client. */
public record LuteCommandPayload(LuteCommand command) implements CustomPacketPayload {
    public static final Type<LuteCommandPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LegendaryAllays.MOD_ID, "lute_command"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LuteCommandPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeEnum(payload.command()), buffer -> new LuteCommandPayload(buffer.readEnum(LuteCommand.class)));
    @Override public Type<LuteCommandPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToServer(TYPE, CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) CompanionManager.command(player, payload.command());
        });
    }
}
