package it.hurts.metallurgy_reforged.gadget;
import it.hurts.metallurgy_reforged.effect.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.BlockSnapshot;

public final class GadgetBridge {
    public static boolean marked(Entity entity,String key) {return entity.getPersistentData().getCompound("metallurgy_effects").contains(key);}
    public static String ingotMetal(ItemStack stack) {
        String id=VersionBridge.itemId(stack);
        if(id.startsWith("metallurgy:") && id.endsWith("_ingot")) return id.substring(11,id.length()-6);
        return stack.getTags().map(tag->tag.location().getPath()).filter(path->path.startsWith("ingots/")).map(path->path.substring(7)).filter(it.hurts.metallurgy_reforged.material.Materials.BY_NAME::containsKey).findFirst().orElse("");
    }
    public static boolean metal(ItemStack stack) {
        String id=VersionBridge.itemId(stack);
        if(id.startsWith("metallurgy:")) for(String name:it.hurts.metallurgy_reforged.material.Materials.BY_NAME.keySet()) if(id.startsWith("metallurgy:"+name+"_")) return true;
        return stack.getTags().anyMatch(tag->{String path=tag.location().getPath();return path.equals("ingots") || path.equals("dusts") || path.equals("ores") || path.startsWith("ingots/") || path.startsWith("dusts/") || path.startsWith("ores/");});
    }
    public static void damage(LivingEntity entity,ItemStack stack,int amount,EquipmentSlot slot) { stack.hurtAndBreak(amount,entity,slot); }
    public static void detectorMessage(Player player,String key) { player.displayClientMessage(Component.translatable(key),true); }
    public static void magnetMessage(Player player,int mode) {
        Component message=Component.translatable(switch(mode) {
            case 1 -> "message.metallurgy.magnet_metals";
            case 2 -> "message.metallurgy.magnet_all";
            default -> "message.metallurgy.magnet_off";
        });
        player.displayClientMessage(message,true);
    }
    public static void tired(Player player) { player.displayClientMessage(Component.translatable("effect.metallurgy.punch_effect_tired"),true); }
    public static boolean editable(Player player,BlockPos pos,Direction face,ItemStack stack) {
        return player.mayBuild() && player.level().hasChunkAt(pos) && player.level().isInWorldBounds(pos) && player.level().getWorldBorder().isWithinBounds(pos)
            && player.level().mayInteract(player,pos) && player.mayUseItemAt(pos,face,stack);
    }
    public static boolean place(Player player,BlockPos pos,Direction face,ItemStack stack,BlockState state) {
        if(!(player.level() instanceof ServerLevel level) || !editable(player,pos,face,stack)) return false;
        BlockSnapshot snapshot=BlockSnapshot.create(level.dimension(),level,pos);
        if(!level.setBlock(pos,state,3)) return false;
        if(net.neoforged.neoforge.event.EventHooks.onBlockPlace(player,snapshot,face)) { snapshot.restore(3);return false; }
        return true;
    }
    public static boolean punchBlock(Player player,BlockPos pos) {
        if(!editable(player,pos,Direction.UP,player.getMainHandItem())) return false;
        var state=player.level().getBlockState(pos);
        if(state.isAir() || !state.getFluidState().isEmpty() || state.getDestroySpeed(player.level(),pos)<0 || ToolBridge.required(state)>=5) return false;
        return ToolEffects.auxiliary(()-> {
            var event=new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(player.level(),pos,state,player);
            return !net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled() && player.level().destroyBlock(pos,true,player);
        });
    }
    public static void particles(LivingEntity entity,boolean cloud) {
        if(entity.level() instanceof ServerLevel level) level.sendParticles(cloud?net.minecraft.core.particles.ParticleTypes.CLOUD:net.minecraft.core.particles.ParticleTypes.CRIT,
            entity.getX(),entity.getY()+entity.getBbHeight()*0.75,entity.getZ(),20,entity.getBbWidth()*0.75,entity.getBbHeight()*0.75,entity.getBbWidth()*0.75,0);
    }
    public static void shieldParticle(LivingEntity entity) {
        if(entity instanceof net.minecraft.server.level.ServerPlayer player && entity.level() instanceof ServerLevel level)
            level.sendParticles(player,net.minecraft.core.particles.ParticleTypes.EXPLOSION,true,entity.getX(),entity.getY()+1,entity.getZ(),1,0,0,0,0);
    }
    public static boolean eatingOtherHand(Player player,ItemStack shield) {
        var other=player.getMainHandItem()==shield?player.getOffhandItem():player.getMainHandItem();
        return other.getUseAnimation()==net.minecraft.world.item.UseAnim.EAT || other.getUseAnimation()==net.minecraft.world.item.UseAnim.DRINK;
    }
    private GadgetBridge() {}
}
