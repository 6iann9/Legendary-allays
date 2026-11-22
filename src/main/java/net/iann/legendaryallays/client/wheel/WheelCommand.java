package net.iann.legendaryallays.client.wheel;

import net.iann.legendaryallays.command.LuteCommand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Wheel labels, placeholder icons and their server commands. */
public enum WheelCommand {
    COLLECTION("collection", Items.CHEST, 0xFFFFD577),
    BIND("bind", Items.LEAD, 0xFF77DFBA),
    UNBIND("unbind", Items.SHEARS, 0xFFD1DAE7),
    FILTER_ADD("filter_add", Items.HOPPER, 0xFFA4E86D),
    FILTER_CLEAR("filter_clear", Items.BUCKET, 0xFFD49AF5),
    COLLECT_DROPPED("collect_dropped", Items.IRON_PICKAXE, 0xFFFFAE67),
    COLLECT_NEARBY("collect_nearby", Items.COMPASS, 0xFF77D9EE),
    COLLECT_EVERYWHERE("collect_everywhere", Items.ENDER_PEARL, 0xFFB5A0FF),
    IDLE("idle", Items.BARRIER, 0xFFFF818A);

    public LuteCommand serverCommand() {
        return switch (this) {
            case COLLECTION -> throw new IllegalStateException("Collection opens a local submenu");
            case BIND -> LuteCommand.BIND;
            case UNBIND -> LuteCommand.UNBIND;
            case COLLECT_DROPPED -> LuteCommand.COLLECT_DROPPED;
            case IDLE -> LuteCommand.IDLE;
            case COLLECT_NEARBY -> LuteCommand.COLLECT_NEARBY;
            case COLLECT_EVERYWHERE -> LuteCommand.COLLECT_EVERYWHERE;
            case FILTER_ADD -> LuteCommand.FILTER_ADD;
            case FILTER_CLEAR -> LuteCommand.FILTER_CLEAR;
        };
    }

    public final String key;
    public final ItemStack icon;
    public final int color;

    WheelCommand(String id, Item item, int color) {
        this.key = "wheel.legendaryallays." + id;
        this.icon = new ItemStack(item);
        this.color = color;
    }
}
