package net.iann.legendaryallays.client.wheel;

import net.minecraft.core.particles.ParticleTypes;
import net.iann.legendaryallays.command.LuteCommand;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Wheel entries: implemented server commands plus local particle placeholders. */
public enum WheelCommand {
    BIND("bind", Items.LEAD, ParticleTypes.HEART, 0xFF77DFBA),
    UNBIND("unbind", Items.SHEARS, ParticleTypes.CLOUD, 0xFFD1DAE7),
    FILTER_ADD("filter_add", Items.HOPPER, ParticleTypes.HAPPY_VILLAGER, 0xFFA4E86D),
    FILTER_CLEAR("filter_clear", Items.BUCKET, ParticleTypes.WITCH, 0xFFD49AF5),
    COLLECT_DROPPED("collect_dropped", Items.IRON_PICKAXE, ParticleTypes.FLAME, 0xFFFFAE67),
    COLLECT_NEARBY("collect_nearby", Items.COMPASS, ParticleTypes.END_ROD, 0xFF77D9EE),
    COLLECT_EVERYWHERE("collect_everywhere", Items.ENDER_PEARL, ParticleTypes.PORTAL, 0xFFB5A0FF),
    IDLE("idle", Items.BARRIER, ParticleTypes.SMOKE, 0xFFFF818A);

    public LuteCommand serverCommand() {
        return switch (this) {
            case BIND -> LuteCommand.BIND;
            case UNBIND -> LuteCommand.UNBIND;
            case COLLECT_DROPPED -> LuteCommand.COLLECT_DROPPED;
            case IDLE -> LuteCommand.IDLE;
            default -> null;
        };
    }

    public final String key;
    public final ItemStack icon;
    public final SimpleParticleType particle;
    public final int color;

    WheelCommand(String id, Item item, SimpleParticleType particle, int color) {
        this.key = "wheel.legendaryallays." + id;
        this.icon = new ItemStack(item);
        this.particle = particle;
        this.color = color;
    }
}
