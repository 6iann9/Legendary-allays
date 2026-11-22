package net.iann.legendaryallays.command;

/** Commands implemented by the server. Keep client-only placeholders out of this list. */
public enum LuteCommand {
    BIND,
    UNBIND,
    COLLECT_DROPPED,
    IDLE,
    COLLECT_NEARBY,
    COLLECT_EVERYWHERE,
    FILTER_ADD,
    FILTER_CLEAR
}
