package it.hurts.metallurgy_reforged.compat;
import it.hurts.metallurgy_reforged.machine.MachineEntity;
import net.minecraftforge.fml.ModList;
import java.lang.reflect.*;
/** Optional native IE capability; no IE class is resolved when absent.
 * The proxy implements the actual loaded API interface, not a bundled copy. */
public final class ExternalHeat {
    private static final String API="blusunrize.immersiveengineering.api.tool.ExternalHeaterHandler";
    private static final class Binding {
        static final Class<?> TYPE;
        static final Object CAPABILITY;
        static final Field COST,SPEED;
        static {
            try {var owner=Class.forName(API);TYPE=Class.forName(API+"$IExternalHeatable");CAPABILITY=owner.getField("CAPABILITY").get(null);COST=owner.getField("defaultFurnaceEnergyCost");SPEED=owner.getField("defaultFurnaceSpeedupCost");}
            catch(ReflectiveOperationException exception) {throw new IllegalStateException("Unsupported Immersive Engineering external heater API",exception);}
        }
    }
    public static Object capability() {return ModList.get().isLoaded("immersiveengineering")?Binding.CAPABILITY:null;}
    public static Object wrap(MachineEntity machine) {
        if(capability()==null) return null;
        return Proxy.newProxyInstance(Binding.TYPE.getClassLoader(),new Class<?>[]{Binding.TYPE},(proxy,method,args)->switch(method.getName()) {
            case "doHeatTick"->machine.externalHeat((Integer)args[0],(Boolean)args[1],Binding.COST.getInt(null),Binding.SPEED.getInt(null));
            case "hashCode"->System.identityHashCode(proxy);case "equals"->proxy==args[0];case "toString"->"Metallurgy external heater at "+machine.getBlockPos();
            default->throw new UnsupportedOperationException("Unsupported external heater method: "+method);
        });
    }
    private ExternalHeat() {}
}
