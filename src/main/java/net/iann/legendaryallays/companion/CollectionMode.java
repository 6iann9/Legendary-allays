package net.iann.legendaryallays.companion;

public enum CollectionMode {
    DROPPED(true, false),
    NEARBY(false, true),
    EVERYWHERE(true, true);

    public final boolean dropped;
    public final boolean nearby;

    CollectionMode(boolean dropped, boolean nearby) {
        this.dropped = dropped;
        this.nearby = nearby;
    }
}
