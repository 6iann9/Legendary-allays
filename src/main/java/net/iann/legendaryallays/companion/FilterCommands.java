package net.iann.legendaryallays.companion;

import net.iann.legendaryallays.command.LuteCommand;
import net.iann.legendaryallays.command.LuteSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class FilterCommands {
    private FilterCommands() {}

    public static void addOffhand(ServerPlayer player) {
        ItemStack stack = player.getOffhandItem();
        if (stack.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.legendaryallays.filter_empty_hand"), true);
            return;
        }
        CompanionRoster roster = CompanionRoster.get(player.server);
        ItemFilter filter = roster.filter(player.getUUID());
        if (!filter.add(stack)) {
            player.displayClientMessage(Component.translatable("message.legendaryallays.filter_duplicate",
                    Component.translatable(stack.getItem().getDescriptionId())), true);
            return;
        }
        roster.setDirty();
        refreshTargets(player);
        player.displayClientMessage(Component.translatable("message.legendaryallays.filter_added",
                Component.translatable(stack.getItem().getDescriptionId()), filter.size()), true);
        LuteSounds.play(player, LuteCommand.FILTER_ADD);
    }

    public static void clear(ServerPlayer player) {
        CompanionRoster roster = CompanionRoster.get(player.server);
        roster.filter(player.getUUID()).clear();
        roster.setDirty();
        refreshTargets(player);
        player.displayClientMessage(Component.translatable("message.legendaryallays.filter_cleared"), true);
        LuteSounds.play(player, LuteCommand.FILTER_CLEAR);
    }

    private static void refreshTargets(ServerPlayer player) {
        DropCollection.filterChanged(player);
        NearbyCollection.validate(player);
        // Already-carried items still finish their delivery.
    }
}
