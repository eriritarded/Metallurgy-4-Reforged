package it.hurts.metallurgy_reforged.gadget;
import it.hurts.metallurgy_reforged.entity.VulcanitePrimedTnt;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
public final class VulcaniteTntBlock extends TntBlock {
    private static final com.mojang.serialization.MapCodec<TntBlock> CODEC=simpleCodec(VulcaniteTntBlock::new);
    @Override public com.mojang.serialization.MapCodec<TntBlock> codec() {return CODEC;}
    public VulcaniteTntBlock(Properties properties) {super(properties);}
    @Override public boolean onCaughtFire(BlockState state,Level level,BlockPos pos,Direction face,LivingEntity owner) {
        if(!(level instanceof ServerLevel server) || !server.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES)) return false;
        var entity=VulcanitePrimedTnt.create(level,pos,owner);level.addFreshEntity(entity);
        level.playSound(null,pos,SoundEvents.TNT_PRIMED,SoundSource.BLOCKS,1,1);level.gameEvent(owner,GameEvent.PRIME_FUSE,pos);return true;
    }
    @Override public void wasExploded(ServerLevel level,BlockPos pos,Explosion explosion) {
        if(!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES)) return;
        var entity=VulcanitePrimedTnt.create(level,pos,explosion.getIndirectSourceEntity());
        int fuse=entity.getFuse();entity.setFuse(level.getRandom().nextInt(fuse/4)+fuse/8);level.addFreshEntity(entity);
    }
}
