package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import net.minecraft.world.entity.LivingEntity;
/** Source100/25/25 corpse tint, independent of global graphics color state. */
public final class CorpseRenderData {
    public static boolean active(LivingEntity entity) {try(var state=VersionBridge.entity(entity)) {return state.integer("corpse_state")>=1;}}
    public static int tint(int original) {return original&0xff000000 | ((original>>16&255)*100/255)<<16 | ((original>>8&255)*25/255)<<8 | (original&255)*25/255;}
    public static double jitter() {return java.util.concurrent.ThreadLocalRandom.current().nextGaussian()*.01;}
    private CorpseRenderData() {}
}
