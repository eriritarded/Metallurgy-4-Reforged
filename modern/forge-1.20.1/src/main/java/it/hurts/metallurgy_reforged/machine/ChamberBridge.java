package it.hurts.metallurgy_reforged.machine;
import it.hurts.metallurgy_reforged.content.Content;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.sounds.SoundSource;
public final class ChamberBridge {
    public static void drop(ChamberEntity chamber) {
        ItemStack stack=Content.stack("sublimation_chamber");
        if(!chamber.isEmpty() || !chamber.effect.isEmpty()) {
            var data=chamber.saveWithoutMetadata();data.remove("affected_players");
            BlockItem.setBlockEntityData(stack,Content.CHAMBER_ENTITY.get(),data);
        }
        Block.popResource(chamber.getLevel(),chamber.getBlockPos(),stack);
    }
    public static void sound(ChamberEntity chamber,String suffix) {
        var sound=Content.SOUND_MAP.get("sublimation_chamber_"+suffix);
        if(sound!=null) chamber.getLevel().playSound(null,chamber.getBlockPos(),sound.get(),SoundSource.BLOCKS,1,1);
    }
    private ChamberBridge() {}
}
