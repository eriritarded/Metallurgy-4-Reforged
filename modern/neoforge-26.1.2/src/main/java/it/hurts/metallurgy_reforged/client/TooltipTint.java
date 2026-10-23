package it.hurts.metallurgy_reforged.client;
public final class TooltipTint {
    private static final ThreadLocal<Integer> COLOR=ThreadLocal.withInitial(()->0xffffffff);
    public static int color() {return COLOR.get();}
    public static void color(int value) {COLOR.set(value);}
    public static void clear() {COLOR.remove();}
    private TooltipTint() {}
}
