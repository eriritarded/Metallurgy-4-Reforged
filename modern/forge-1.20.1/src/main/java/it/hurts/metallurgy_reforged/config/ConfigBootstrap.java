package it.hurts.metallurgy_reforged.config;
public final class ConfigBootstrap {
    public static void initialize() {ConfigFiles.initialize(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("metallurgy_reforged"));}
    private ConfigBootstrap() {}
}
