package it.hurts.metallurgy_reforged.effect;

import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Active1.4.1 TOOL/PICKAXE/HOE callbacks, adapted to actual native drops.
 * Auxiliary destruction is bounded and cannot recursively schedule abilities. */
public final class ToolEffects {
    private record Mining(BlockPos origin,BlockState state,ItemStack stack,int step,long at) {}
    private static final Map<Player,Mining> CARMOT=new WeakHashMap<>();
    private static final Map<Player,ItemStack> CELENEGIL=new WeakHashMap<>();
    private static final ThreadLocal<Integer> AUXILIARY=ThreadLocal.withInitial(()->0);
    public static boolean auxiliary(BooleanSupplier action) {
        int previous=AUXILIARY.get();AUXILIARY.set(previous+1);
        try { return action.getAsBoolean(); } finally { if(previous==0) AUXILIARY.remove();else AUXILIARY.set(previous); }
    }
    private static boolean tool(ItemStack stack) { return EquipmentEffects.accepts(stack,EquipmentEffects.Category.TOOL) && it.hurts.metallurgy_reforged.config.ConfigFiles.effect(EquipmentEffects.material(stack),"tool"); }
    private static String metal(ItemStack stack) {
        String metal=EquipmentEffects.material(stack),form=EquipmentEffects.form(stack);
        String category=form.equals("hoe")?"hoe":form.equals("pickaxe") && Set.of("angmallen","mithril","shadow_iron","oureclase","damascus_steel").contains(metal)?"pickaxe":"tool";
        return it.hurts.metallurgy_reforged.config.ConfigFiles.effect(metal,category)?metal:"";
    }
    public static float speed(Player player,BlockState block,BlockPos pos,float original,float current) {
        ItemStack held=player.getMainHandItem();if(!tool(held)) return current;
        String metal=metal(held);
        boolean harvest=pos!=null && ToolBridge.canHarvest(player,pos,block);
        if(pos!=null && player.level() instanceof ServerLevel server) {
            if(metal.equals("astral_silver") && player.level().dimension()!=Level.OVERWORLD && held.getDestroySpeed(block)>1) for(int i=0;i<2;i++) it.hurts.metallurgy_reforged.visual.EffectParticles.block(server,pos,metal,1,1,true,server.getRandom().nextInt(6),Vec3.ZERO);
            else if(metal.equals("quicksilver") && harvest) it.hurts.metallurgy_reforged.visual.EffectParticles.block(server,pos,metal,1,1,true,5,Vec3.ZERO);
            else if(metal.equals("sanguinite") && VersionBridge.cooldown(player,held)>0) it.hurts.metallurgy_reforged.visual.EffectParticles.block(server,pos,metal,1,2,true,4,Vec3.ZERO,new Vec3(.25,.25,.25));
        }
        return switch(metal) {
            case "astral_silver" -> player.level().dimension()!=Level.OVERWORLD && held.getDestroySpeed(block)>1?original*2:current;
            case "deep_iron" -> player.isUnderWater()?original*3:current;
            case "electrum" -> {try(State state=VersionBridge.stack(held)) {yield state.flag("voltage_control")?original*4:current;}}
            case "quicksilver" -> harvest?original*block.getDestroySpeed(player.level(),pos)/2:current;
            case "ceruclase" -> {if(harvest && matchingTool(held,block)) {VersionBridge.award(player,"challenges/low_temperatures");yield 100;}yield current;}
            case "sanguinite" -> {
                boolean cooling=VersionBridge.cooldown(player,held)>0;
                if(!cooling) try(State state=VersionBridge.stack(held)) {state.remove("harvest_boost");}
                yield cooling?original*1.75F:current;
            }
            default -> current;
        };
    }
    private static boolean matchingTool(ItemStack held,BlockState state) {
        return switch(EquipmentEffects.form(held)) {
            case "pickaxe" -> state.is(BlockTags.MINEABLE_WITH_PICKAXE);
            case "axe" -> state.is(BlockTags.MINEABLE_WITH_AXE);
            case "shovel" -> state.is(BlockTags.MINEABLE_WITH_SHOVEL);
            default -> false;
        };
    }
    /** True cancels an overlapping Carmot primary break; XP returned separately. */
    public static boolean breaking(Player player,BlockPos pos,BlockState state) {
        if(!(player.level() instanceof ServerLevel level) || AUXILIARY.get()>0) return false;
        ItemStack held=player.getMainHandItem();if(!tool(held)) return false;
        String metal=metal(held);
        if(metal.equals("carmot") && ToolBridge.canHarvest(player,pos,state) && matchingTool(held,state)) {
            if(CARMOT.containsKey(player)) return true;
            CARMOT.put(player,new Mining(pos.immutable(),state,held,1,level.getGameTime()));
            VersionBridge.cooldown(player,held,(6-Math.max(0,ToolBridge.required(state)-2))*11);
        } else if(metal.equals("electrum")) {
            try(State data=VersionBridge.stack(held)) { if(data.flag("voltage_control")) {held.setDamageValue(held.getDamageValue()+4);VersionBridge.award(player,"challenges/overclocked_tools");} }
        } else if(metal.equals("kalendrite") && player.getHealth()<player.getMaxHealth()) {
            held.setDamageValue(held.getDamageValue()+Math.round((player.getMaxHealth()-player.getHealth())/2));player.heal(1);
            it.hurts.metallurgy_reforged.visual.EffectParticles.entity(player,metal,16,2.5F,true,5);
        } else if(metal.equals("celenegil")) {
            try(State data=VersionBridge.entity(player)) {
                int count=data.integer("tool.celenegil.broken");
                if(count>=5) {
                    VersionBridge.buff(player,"haste",30,count>=60?2:count>=15?1:0);
                    data.integer("tool.celenegil.damage",held.getDamageValue());CELENEGIL.put(player,held);
                } else VersionBridge.sound(player,"entity.egg.throw",0.75F,0.75F+(count+1)/5F);
                data.integer("tool.celenegil.broken",count+1);data.flag("tool.celenegil.inactive",false);
            }
        } else if(metal.equals("vulcanite") && level.getRandom().nextFloat()<0.4F+0.2F*ToolBridge.fortune(player,held)) {
            List<BlockPos> adjacent=new ArrayList<>();
            for(Direction direction:Direction.values()) if(level.hasChunkAt(pos.relative(direction)) && !level.isEmptyBlock(pos.relative(direction))) adjacent.add(pos.relative(direction));
            if(!adjacent.isEmpty()) {
                BlockPos extra=adjacent.get(level.getRandom().nextInt(adjacent.size()));ToolBridge.harvest(player,extra,false);
                it.hurts.metallurgy_reforged.visual.EffectParticles.block(level,extra,metal,10,1.5F,true,5,Vec3.ZERO);VersionBridge.sound(player,"entity.firework_rocket.blast",2,1.5F);
            }
        }
        return false;
    }
    public static int experience(Player player,BlockState block,int original) {
        return EquipmentEffects.accepts(player.getMainHandItem(),EquipmentEffects.Category.PICKAXE)
            && metal(player.getMainHandItem()).equals("damascus_steel") && ore(block) && Math.random()<0.5
            ?original+ToolBridge.required(block)+1:original;
    }
    private static boolean ore(BlockState block) {
        String id=VersionBridge.blockId(block);
        return id.startsWith("metallurgy:") && id.endsWith("_ore") || ToolBridge.oreBlock(block);
    }
    private static boolean ownOre(BlockState block) {String id=VersionBridge.blockId(block);return id.startsWith("metallurgy:") && id.endsWith("_ore");}
    public static List<ItemStack> drops(ServerLevel level,BlockPos pos,BlockState block,BlockEntity blockEntity,Entity breaker,ItemStack used,List<ItemStack> original) {
        if(AUXILIARY.get()>0 || !(breaker instanceof Player player) || used.isEmpty()) return original;
        if(used.getItem()!=player.getMainHandItem().getItem()) return original;
        ItemStack held=player.getMainHandItem(); // Native harvest passes a pre-wear copy of the tool.
        boolean tool=tool(used),pick=EquipmentEffects.accepts(used,EquipmentEffects.Category.PICKAXE),hoe=EquipmentEffects.accepts(used,EquipmentEffects.Category.HOE);
        if(!tool && !hoe) return original;
        List<ItemStack> drops=new ArrayList<>();for(ItemStack stack:original) drops.add(stack.copy());
        String metal=metal(used);int fortune=ToolBridge.fortune(player,used);
        if(tool && metal.equals("sanguinite") && VersionBridge.cooldown(player,held)>0) VersionBridge.award(player,"challenges/overclocked_tools");
        if(pick && (metal.equals("angmallen") && ownOre(block) || metal.equals("shadow_iron") && VersionBridge.blockId(block).equals("metallurgy:shadow_iron_ore"))) {
            if(level.getRandom().nextInt(10)<3+fortune*3) {
                int required=ToolBridge.required(block);
                List<Materials.Definition> ores=Materials.ALL.stream().filter(m->m.oreLevel()>=0 && !VersionBridge.blockId(block).equals("metallurgy:"+m.name()+"_ore")
                    && m.oreLevel()>=required-(metal.equals("angmallen")?1:0) && m.oreLevel()<=required+1).toList();
                if(!ores.isEmpty()) {
                    ItemStack result=Content.stack(ores.get(level.getRandom().nextInt(ores.size())).name()+"_ore");
                    if(metal.equals("angmallen")) { lock(result);VersionBridge.damageStack(player,held,1); }
                    drops.clear();drops.add(result);VersionBridge.sound(player,"block.enchantment_table.use",1,1);
                    if(metal.equals("angmallen")) it.hurts.metallurgy_reforged.visual.EffectParticles.block(level,pos,metal,20,1.3F,true,5,new Vec3(0,.2,0));
                }
            }
        } else if(pick && metal.equals("mithril") && ownOre(block) && !drops.isEmpty()) {
            ItemStack first=drops.get(0);if(first.getItem() instanceof BlockItem && VersionBridge.itemId(first).endsWith("_ore")) lock(first);
            if(Math.random()<=(fortune+1)/4F) {VersionBridge.damageStack(player,held,2);first.grow(1);VersionBridge.sound(player,"block.note_block.harp",1,1);}
        } else if(pick && metal.equals("oureclase")) {
            List<ItemStack> dusts=new ArrayList<>();Iterator<ItemStack> dropIterator=drops.iterator();
            while(dropIterator.hasNext()) {
                ItemStack drop=dropIterator.next();String found=null;
                if(!VersionBridge.itemId(drop).endsWith("_ore") && !ToolBridge.oreStack(drop)) return drops;
                for(var m:Materials.ALL) if(VersionBridge.itemId(drop).equals("metallurgy:"+m.name()+"_ore") || VersionBridge.itemTag(drop,"ores/"+m.name())) {found=m.name();break;}
                if(found!=null) {dropIterator.remove();dusts.add(Content.stack(found+"_dust"));}
            }
            drops.addAll(dusts);if(!dusts.isEmpty()) {VersionBridge.modSound(player,"crusher_impact",0.8F,1.2F);it.hurts.metallurgy_reforged.visual.EffectParticles.block(level,pos,metal,5,3,false,5,Vec3.ZERO);}
        } else if(tool && metal.equals("ignatius") && Math.random()<(fortune+1)/4F) {
            for(int i=0;i<drops.size();i++) {ItemStack smelt=VersionBridge.smelt(level,drops.get(i));if(!smelt.isEmpty()) {smelt.setCount(drops.get(i).getCount());drops.set(i,smelt);}}
            it.hurts.metallurgy_reforged.visual.EffectParticles.block(level,pos,metal,10,2,true,3,new Vec3(0,.02,0),new Vec3(.03125,0,.03125));
        } else if(tool && metal.equals("midasium") && drops.stream().noneMatch(d->d.getItem() instanceof BlockItem || d.getItem()==block.getBlock().asItem()) && Math.random()<=(fortune+1)*0.3F) {
            List<ItemStack> compact=new ArrayList<>();
            for(ItemStack drop:drops) {ItemStack match=compact.stream().filter(d->ToolBridge.same(d,drop)).findFirst().orElse(null);if(match==null) compact.add(drop);else match.grow(drop.getCount());}
            drops=compact;
            for(int i=0;i<drops.size();i++) if(Math.random()<=0.03+fortune*0.05) drops.set(i,new ItemStack(Items.GOLD_INGOT,drops.get(i).getCount()));else drops.get(i).grow(fortune==0?2:1);
            it.hurts.metallurgy_reforged.visual.EffectParticles.block(level,pos,metal,drops.size(),1,true,5,Vec3.ZERO,new Vec3(.05,.05,.05));
        } else if(hoe && metal.equals("midasium") && block.getBlock() instanceof CropBlock && Math.random()<=0.35F+fortune*0.15F) {
            held.setDamageValue(held.getDamageValue()+1);
            for(int i=0;i<drops.size();i++) {ItemStack old=drops.get(i);drops.set(i,new ItemStack(old.getItem()==block.getBlock().asItem()?Items.GOLD_NUGGET:Items.GOLD_INGOT,old.getCount()));}
            it.hurts.metallurgy_reforged.visual.EffectParticles.block(level,pos,metal,10,2,true,3,Vec3.ZERO,new Vec3(.075,.075,.075));VersionBridge.sound(player,"block.note_block.chime",1,1);
        } else if(hoe && metal.equals("prometheum")) {
            boolean changed=false;
            if(block.getBlock() instanceof CropBlock || block.is(Blocks.MELON)) {
                // Preserve the original precedence: (count + random < .5) ?1:2.
                for(ItemStack drop:drops) if(Math.random()<0.5) {drop.setCount(drop.getCount()+Math.random()<0.5?1:2);changed=true;it.hurts.metallurgy_reforged.visual.EffectParticles.block(level,pos,metal,20,1,true,6,Vec3.ZERO);}
            } else if(block.is(BlockTags.LEAVES) || ToolBridge.grass(block)) {
                for(int i=0;i<4;i++) {List<ItemStack> extra=Block.getDrops(block,level,pos,blockEntity,player,used);for(ItemStack drop:extra) {drops.add(drop);changed=true;}}
            }
            if(changed) VersionBridge.damageStack(player,held,1);
        }
        if(tool && metal.equals("desichalkos")) drops.removeIf(stack->player.getInventory().add(stack));
        return drops;
    }
    private static void lock(ItemStack stack) {try(State state=VersionBridge.stack(stack)) {state.flag("locked",true);}}
    public static boolean rightClick(Player player,InteractionHand hand) {
        if(!(player.level() instanceof ServerLevel level) || hand!=InteractionHand.MAIN_HAND) return false;
        ItemStack held=player.getMainHandItem();String metal=metal(held);
        if(EquipmentEffects.accepts(held,EquipmentEffects.Category.HOE) && metal.equals("atlarus")) {
            if(VersionBridge.cooldown(player,held)>0) return false;
            try(State state=VersionBridge.stack(held)) {
                if(state.has("hoe.range")) return false;
                state.integer("hoe.range",0);state.longValue("hoe.origin",player.blockPosition().asLong());
            }
            VersionBridge.cooldown(player,held,30);return true;
        }
        if(!tool(held)) return false;
        if(metal.equals("electrum")) {
            try(State state=VersionBridge.stack(held)) {boolean active=!state.flag("voltage_control");state.flag("voltage_control",active);state.integer("harvest_boost",active?1:0);}
            return true;
        }
        if(VersionBridge.cooldown(player,held)>0) return false;
        if(metal.equals("krik")) {
            Vec3 look=player.getLookAngle();player.setDeltaMovement(player.getDeltaMovement().add(look.x*1.8,look.y*1.2,look.z*1.8));VersionBridge.velocity(player);
            VersionBridge.damageStack(player,held,2);VersionBridge.cooldown(player,held,player.onGround()?120:48);VersionBridge.sound(player,"entity.ghast.shoot",1.25F,0.65F);return true;
        }
        if(metal.equals("sanguinite")) {
            VersionBridge.hit(player,player.damageSources().wither(),6);held.setDamageValue(Math.max(0,held.getDamageValue()-6));
            try(State state=VersionBridge.stack(held)) {state.integer("harvest_boost",1);}
            VersionBridge.cooldown(player,held,320);return true;
        }
        if(metal.equals("inolashite")) {tunnel(player,level,held);return true;}
        return false;
    }
    private static void tunnel(Player player,ServerLevel level,ItemStack held) {
        Vec3 eye=player.getEyePosition(),look=player.getLookAngle();BlockPos blocked=null;Set<BlockPos> visited=new LinkedHashSet<>();
        outer: for(int sample=0;sample<=48;sample++) for(int lane=0;lane<(Math.abs(player.getXRot())<60?2:1);lane++) {
            BlockPos pos=BlockPos.containing(eye.add(look.scale(sample/8D)).add(0,-lane,0));
            if(!visited.add(pos)) continue;
            if(!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) {blocked=pos;break outer;}
            if(!level.isEmptyBlock(pos) && !ToolBridge.harvest(player,pos,true)) {blocked=pos;break outer;}
            if(held.isEmpty()) {blocked=pos;break outer;}
        }
        Direction pointing=Math.abs(look.x)>Math.abs(look.y) && Math.abs(look.x)>Math.abs(look.z)?(look.x>0?Direction.EAST:Direction.WEST)
            :Math.abs(look.y)>Math.abs(look.z)?(look.y>0?Direction.UP:Direction.DOWN):(look.z>0?Direction.SOUTH:Direction.NORTH);
        Direction face=pointing.getOpposite();
        Vec3 target=blocked==null?player.position().add(look.scale(6)).add(0,1,0):Vec3.atBottomCenterOf(blocked.relative(face));
        if(level.hasChunkAt(BlockPos.containing(target)) && level.getWorldBorder().isWithinBounds(BlockPos.containing(target))
            && level.noCollision(player,player.getBoundingBox().move(target.subtract(player.position())))) {
            Vec3 start=player.position();var random=level.getRandom();
            for(int i=0;i<128;i++) {Vec3 at=start.lerp(target,i/127D).add((random.nextDouble()-.5)*player.getBbWidth()*2,random.nextDouble()*player.getBbHeight(),(random.nextDouble()-.5)*player.getBbWidth()*2);level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,at.x,at.y,at.z,0,(random.nextFloat()-.5)*.2,(random.nextFloat()-.5)*.2,(random.nextFloat()-.5)*.2,1);}
            player.teleportTo(target.x,target.y,target.z);player.invulnerableTime=20;
        }
        VersionBridge.cooldown(player,held,200);VersionBridge.sound(player,"entity.enderman.teleport",1,1);
    }
    public static boolean crop(Player player,InteractionHand hand,BlockPos pos) {
        return hand==InteractionHand.MAIN_HAND && EquipmentEffects.accepts(player.getMainHandItem(),EquipmentEffects.Category.HOE)
            && metal(player.getMainHandItem()).equals("midasium") && player.level().getBlockState(pos).getBlock() instanceof CropBlock && ToolBridge.harvestCrop(player,pos);
    }
    public static void equipped(ItemStack to) {if(EquipmentEffects.form(to).equals("hoe") && metal(to).equals("atlarus")) try(State state=VersionBridge.stack(to)) {state.remove("hoe.range");}}
    public static void tick(Player player) {
        if(!(player.level() instanceof ServerLevel level)) return;
        ItemStack held=player.getMainHandItem();Mining mining=CARMOT.get(player);long now=level.getGameTime();
        if(mining!=null && now>=mining.at+11) {
            int step=mining.step,max=3-Math.max(0,ToolBridge.required(mining.state)-2);
            if(step>max || mining.stack.isEmpty() || held!=mining.stack) CARMOT.remove(player);
            else {
                for(int x=-step-1;x<step+1;x++) for(int y=-step-1;y<step+1;y++) for(int z=-step-1;z<step+1;z++)
                    if(Math.ceil(Math.sqrt(x*x+y*y+z*z))==step) {BlockPos pos=mining.origin.offset(x,y,z);if(level.hasChunkAt(pos) && level.getBlockState(pos).getBlock()==mining.state.getBlock()) ToolBridge.harvest(player,pos,true);}
                if(step>=3) CARMOT.remove(player);else CARMOT.put(player,new Mining(mining.origin,mining.state,mining.stack,step+1,now));
                VersionBridge.sound(player,"entity.blaze.hurt",1.5F,(8-step)/6F);
            }
        }
        try(State data=VersionBridge.entity(player)) {
            if(data.integer("tool.celenegil.broken")!=0 && now>=data.longValue("tool.celenegil.at")+10) {
                int step=data.integer("tool.celenegil.step");
                if(step==1) data.flag("tool.celenegil.inactive",true);
                if(step==3 && data.flag("tool.celenegil.inactive")) {
                    ItemStack previous=CELENEGIL.remove(player);if(previous!=null && !previous.isEmpty()) previous.setDamageValue(data.integer("tool.celenegil.damage"));
                    data.integer("tool.celenegil.broken",0);VersionBridge.sound(player,"block.redstone_torch.burnout",0.75F,0.75F);
                }
                data.integer("tool.celenegil.step",step>=3?0:step+1);data.longValue("tool.celenegil.at",now);
            }
        }
        if(EquipmentEffects.form(held).equals("hoe") && metal(held).equals("atlarus") && now%4==0) {
            try(State state=VersionBridge.stack(held)) {
                if(!state.has("hoe.range")) return;
                int range=state.integer("hoe.range"),changed=0;BlockPos origin=BlockPos.of(state.longValue("hoe.origin"));
                for(int x=-range;x<=range;x++) for(int y=-5;y<=5;y++) for(int z=-range;z<=range;z++) if(Math.abs(x)==range || Math.abs(z)==range) {
                    BlockPos pos=origin.offset(x,y,z);if(level.hasChunkAt(pos) && ToolBridge.plant(level.getBlockState(pos)) && ToolBridge.harvest(player,pos,false)) changed++;
                }
                for(int x=-range;x<=range;x++) for(int z=-range;z<=range;z++) if(Math.abs(x)==range || Math.abs(z)==range) {
                    int y=0;for(int search=0;search<5;search++) if(level.isEmptyBlock(origin.offset(x,search,z))) {y=search;break;}
                    var pos=origin.offset(x,y,z);if(!level.hasChunkAt(pos)) continue;
                    for(int i=0;i<5;i++) level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,pos.getX()+level.getRandom().nextDouble(),pos.getY(),pos.getZ()+level.getRandom().nextDouble(),0,Math.max(-1,Math.min(1,x))*.1,.7,Math.max(-1,Math.min(1,z))*.1,1);
                }
                if(changed>0) VersionBridge.damageStack(player,held,Math.round(changed*0.5F));
                if(range<5) state.integer("hoe.range",range+1);else state.remove("hoe.range");
                VersionBridge.sound(player,"entity.blaze.shoot",1.4F,1-range/5F*0.3F);
            }
        }
    }
    public static void cleanup(Player player) {
        CARMOT.remove(player);CELENEGIL.remove(player);
        try(State data=VersionBridge.entity(player)) {for(String field:List.of("broken","damage","step","at","inactive")) data.remove("tool.celenegil."+field);}
        try(State data=VersionBridge.stack(player.getMainHandItem())) {data.remove("hoe.range");}
    }
    private ToolEffects() {}
}
