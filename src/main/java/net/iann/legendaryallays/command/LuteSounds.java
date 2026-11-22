package net.iann.legendaryallays.command;

import net.iann.legendaryallays.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Plays successful lute commands once, from the player, for nearby listeners. */
public final class LuteSounds {
    private LuteSounds() {}

    public static void play(ServerPlayer player, LuteCommand command) {
        SoundEvent sound = switch (command) {
            case BIND -> ModSounds.BIND.get();
            case UNBIND -> ModSounds.UNBIND.get();
            case COLLECT_DROPPED -> ModSounds.COLLECT_DROPPED.get();
            case COLLECT_NEARBY -> ModSounds.COLLECT_NEARBY.get();
            case COLLECT_EVERYWHERE -> ModSounds.COLLECT_EVERYWHERE.get();
            case IDLE -> ModSounds.IDLE.get();
            case FILTER_ADD -> ModSounds.FILTER_ADD.get();
            case FILTER_CLEAR -> ModSounds.FILTER_CLEAR.get();
        };
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
