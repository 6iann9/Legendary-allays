package net.iann.legendaryallays.client.wheel;

/** Screen-space geometry shared by drawing and hit testing. */
public final class WheelLayout {
    public static final double INNER = 28;
    public static final double OUTER = 96;

    private WheelLayout() {}

    public static double scale(int width, int height) {
        return Math.min(1.0, Math.min(width / 280.0, height / 280.0));
    }

    public static double step(int count) {
        return Math.PI * 2 / count;
    }

    public static int selection(double x, double y, int count) {
        double step = step(count);
        if (x * x + y * y < INNER * INNER) return -1;
        double angle = Math.atan2(y, x) + Math.PI / 2 + step / 2;
        angle = (angle + Math.PI * 2) % (Math.PI * 2);
        return Math.min(count - 1, (int) (angle / step));
    }
}
