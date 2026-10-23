package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import it.hurts.metallurgy_reforged.machine.*;
import it.hurts.metallurgy_reforged.visual.SoundCue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import java.util.*;
public final class MachineAmbience {
    private static final Map<BlockPos,MachineSound> SOUNDS=new HashMap<>();
    public static void cue(SoundCue cue) {
        var mc=Minecraft.getInstance();if(!cue.valid() || mc.level==null || !VersionBridge.dimensionId(mc.level).equals(cue.dimension())) return;
        var previous=SOUNDS.remove(cue.pos());if(previous!=null) mc.getSoundManager().stop(previous);
        if(cue.kind()<0) return;
        var sound=new MachineSound(mc.level,cue.pos(),cue.kind());if(!sound.validBlock()) return;
        SOUNDS.put(cue.pos(),sound);mc.getSoundManager().play(sound);
    }
    public static void tick() {
        var mc=Minecraft.getInstance();var iterator=SOUNDS.entrySet().iterator();
        while(iterator.hasNext()) {
            var sound=iterator.next().getValue();
            if(mc.level!=sound.level || !sound.validBlock() || sound.age>20 && !mc.getSoundManager().isActive(sound)) {mc.getSoundManager().stop(sound);iterator.remove();}
        }
    }
    private static final class MachineSound extends AbstractTickableSoundInstance {
        private final ClientLevel level;private final BlockPos pos;private final int kind;private int age;
        MachineSound(ClientLevel level,BlockPos pos,int kind) {
            super(Content.SOUND_MAP.get(switch(kind) {case 0->"crusher_ambience";case 1->"alloyer_ambience";default->"sublimation_chamber_ambience";}).get(),SoundSource.BLOCKS,RandomSource.create());
            this.level=level;this.pos=pos.immutable();this.kind=kind;x=pos.getX()+0.5;y=pos.getY()+0.5;z=pos.getZ()+0.5;volume=1;pitch=1;looping=false;
        }
        boolean validBlock() {
            if(!level.hasChunkAt(pos)) return false;var state=level.getBlockState(pos);
            if(kind==2) return state.is(Content.BLOCK_MAP.get("sublimation_chamber").get()) && state.getValue(ChamberBlock.LIT);
            return state.is(Content.BLOCK_MAP.get(kind==0?"crusher":"alloyer").get()) && state.getValue(MachineBlock.BURNING);
        }
        @Override public void tick() {age++;if(Minecraft.getInstance().level!=level || !validBlock()) stop();}
    }
    private MachineAmbience() {}
}
