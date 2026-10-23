package it.hurts.metallurgy_reforged.gadget;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
public final class IceShieldBlock extends GlassBlock {
    public static final IntegerProperty AGE=IntegerProperty.create("age",0,3);
    public IceShieldBlock(Properties properties) { super(properties);registerDefaultState(stateDefinition.any().setValue(AGE,0)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {builder.add(AGE);}
    @Override public PushReaction getPistonPushReaction(BlockState state) {return PushReaction.NORMAL;}
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(state.getValue(AGE)==0 && ShieldLogic.holdsDome(level,pos)) {level.scheduleTick(pos,this,10+random.nextInt(11));return;}
        int age=state.getValue(AGE);
        if(age<3) {level.setBlock(pos,state.setValue(AGE,age+1),2);level.scheduleTick(pos,this,10+random.nextInt(11));}
        else {level.removeBlock(pos,false);level.playSound(null,pos,net.minecraft.sounds.SoundEvents.GLASS_BREAK,net.minecraft.sounds.SoundSource.BLOCKS,0.15F,1.25F);}
    }
}
