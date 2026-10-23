package it.hurts.metallurgy_reforged.client;

import it.hurts.metallurgy_reforged.gadget.GadgetLogic;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import java.util.*;

/** Client-only observations of already loaded chunks; never requests server ore data. */
public final class DetectorDisplay {
    public record Ore(BlockPos pos,int color) {}
    private static List<Ore> ores=List.of();
    private static final boolean[] lights=new boolean[3];
    private static long ticks;
    private static double previous,previousUsed;
    private static int colorIndex;
    private static net.minecraft.client.multiplayer.ClientLevel gogglesLevel;
    private static List<BlockPos> redstone=List.of();
    private static Set<Block> redstoneBlocks=Set.of();
    private static Object redstoneRegistry,redstoneFeatures;
    private static boolean redstonePermissions;
    private static long gogglesTicks,nextRedstoneScan,nextPaletteRefresh;
    public static void tick() {
        var mc=Minecraft.getInstance();var player=mc.player;
        Arrays.fill(lights,false);ores=List.of();
        tickGoggles(mc);
        if(player==null || mc.level==null || mc.isPaused()) return;
        ticks++;
        double factor=(Math.sin(Math.toRadians(ticks)*6)+1)*0.5;
        double used=factor-previous>=0?factor:1-factor;
        if(previousUsed-used>0.3) colorIndex++;
        previous=factor;previousUsed=used;
        GadgetLogic.detectorColorClock(colorIndex,used);
        ItemStack detector=ItemStack.EMPTY;
        // Preserve the source's offhand preference when both hands hold one.
        for(var hand:InteractionHand.values()) if(GadgetLogic.is(player.getItemInHand(hand),"ore_detector")) detector=player.getItemInHand(hand);
        var metals=GadgetLogic.detectorMetals(detector);
        if(metals.isEmpty() || VersionBridge.cooldown(player,detector)<=0.2F) return;
        var found=new ArrayList<Ore>();BlockPos origin=player.blockPosition();
        for(BlockPos pos:BlockPos.betweenClosed(origin.offset(-8,-8,-8),origin.offset(8,8,8))) {
            if(!mc.level.hasChunkAt(pos)) continue;
            var state=mc.level.getBlockState(pos);String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            boolean added=false;
            for(int i=0;i<metals.size();i++) {
                String metal=metals.get(i);
                if(id.equals("metallurgy:"+metal+"_ore") || VersionBridge.itemTag(new ItemStack(state.getBlock()),"ores/"+metal)) {
                    lights[i]=true;if(!added) {found.add(new Ore(pos.immutable(),Materials.BY_NAME.get(metal).color()));added=true;}
                }
            }
        }
        ores=List.copyOf(found);
        if(mc.level.getGameTime()%10==5) for(int i=0;i<3;i++) if(lights[i])
            player.playSound(it.hurts.metallurgy_reforged.content.Content.SOUND_MAP.get("metal_detector_beep_"+(i+1)).get(),1,1);
    }
    public static List<Ore> ores() {return ores;}
    public static int tint(ItemStack stack,int index) {
        var metals=GadgetLogic.detectorMetals(stack);
        if(index<0 || index>=metals.size()) return -1;
        int rgb=Materials.BY_NAME.get(metals.get(index)).color();
        var mc=Minecraft.getInstance();
        if(mc.player!=null && (mc.player.getMainHandItem()==stack || mc.player.getOffhandItem()==stack) && lights[index] && mc.level!=null && mc.level.getGameTime()%10>4) rgb^=0xffffff;
        return 0xff000000|rgb;
    }
    private static boolean gogglesActive(Minecraft mc) {
        return mc.screen==null && !mc.isPaused() && mc.level!=null && mc.player!=null
            && GadgetLogic.is(mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),"etherium_goggles");
    }
    private static void tickGoggles(Minecraft mc) {
        if(gogglesLevel!=mc.level) {
            gogglesLevel=mc.level;redstone=List.of();redstoneBlocks=Set.of();
            redstoneRegistry=null;redstoneFeatures=null;
            gogglesTicks=0;nextRedstoneScan=0;nextPaletteRefresh=0;
        }
        // Inventory/menu previews must never trigger world discovery or model work.
        if(!gogglesActive(mc)) {redstone=List.of();nextRedstoneScan=0;return;}
        gogglesTicks++;
        if(gogglesTicks<nextRedstoneScan) return;
        nextRedstoneScan=gogglesTicks+5;
        var player=mc.player;var level=mc.level;
        var features=level.enabledFeatures();var registry=level.registryAccess();
        boolean permissions=player.canUseGameMasterBlocks();
        if(registry!=redstoneRegistry || !Objects.equals(features,redstoneFeatures) || permissions!=redstonePermissions) {
            // Bootstrap only when the native tab context changes, never from rendering.
            CreativeModeTabs.tryRebuildTabContents(features,permissions,registry);
            redstoneRegistry=registry;redstoneFeatures=features;redstonePermissions=permissions;
            nextPaletteRefresh=0;
        }
        if(gogglesTicks>=nextPaletteRefresh) {
            var tab=BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.REDSTONE_BLOCKS);
            Set<Block> blocks=new HashSet<>();
            if(tab!=null) for(var stack:tab.getSearchTabDisplayItems()) if(stack.getItem() instanceof BlockItem item) blocks.add(item.getBlock());
            redstoneBlocks=Set.copyOf(blocks);nextPaletteRefresh=gogglesTicks+20;
        }
        if(redstoneBlocks.isEmpty()) {redstone=List.of();return;}
        var result=new ArrayList<BlockPos>();BlockPos origin=BlockPos.containing(player.getEyePosition());
        for(BlockPos pos:BlockPos.betweenClosed(origin.offset(-8,-8,-8),origin.offset(8,8,8)))
            if(level.hasChunkAt(pos) && redstoneBlocks.contains(level.getBlockState(pos).getBlock())) result.add(pos.immutable());
        result.sort(Comparator.comparingDouble((BlockPos pos)->pos.distSqr(origin)).reversed());
        redstone=List.copyOf(result);
    }
    /** Render callbacks only consume the cached scan; they never discover blocks. */
    public static List<BlockPos> redstone() {return gogglesActive(Minecraft.getInstance())?redstone:List.of();}
    public static boolean redstoneBlock(Block block) {return redstoneBlocks.contains(block);}
    private DetectorDisplay() {}
}
