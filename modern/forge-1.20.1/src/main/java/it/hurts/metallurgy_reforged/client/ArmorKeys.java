package it.hurts.metallurgy_reforged.client;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
@Mod.EventBusSubscriber(modid="metallurgy",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ArmorKeys {
    public static final KeyMapping UP=new KeyMapping("key.metallurgy.krik_up",org.lwjgl.glfw.GLFW.GLFW_KEY_UP,"key.categories.metallurgy");
    public static final KeyMapping DOWN=new KeyMapping("key.metallurgy.krik_down",org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN,"key.categories.metallurgy");
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(UP);event.register(DOWN); }
    private ArmorKeys() {}
}
