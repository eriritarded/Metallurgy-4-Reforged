package it.hurts.metallurgy_reforged.fluid;

import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.effect.State;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class MetallurgyLiquidBlock extends LiquidBlock {
    private final boolean tar;
    public MetallurgyLiquidBlock(FluidContent.Entry entry,BlockBehaviour.Properties properties) {super(entry.still,properties);tar=entry.definition.tar();}
    @Override public boolean isRandomlyTicking(BlockState state) {return tar && state.getValue(LEVEL)==0;}
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(!tar || state.getValue(LEVEL)!=0) return;
        for(Direction direction:Direction.values()) {
            BlockPos next=pos.relative(direction);if(!level.hasChunkAt(next)) return;
            BlockState other=level.getBlockState(next);if(other.getBlock()==this && other.getValue(LEVEL)==0) return;
        }
        level.setBlockAndUpdate(pos,Content.BLOCK_MAP.get("tar_ore").get().defaultBlockState());
    }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if(tar) entity.setDeltaMovement(entity.getDeltaMovement().scale(0.25));
        else if(!VersionBridge.client(entity) && !entity.fireImmune()) {
            try(State data=VersionBridge.entity(entity)) {
                long now=level.getGameTime();
                if(!data.has("molten_contact_at") || data.longValue("molten_contact_at")!=now) {
                    data.longValue("molten_contact_at",now);VersionBridge.burn(entity,15);entity.lavaHurt();
                }
            }
        }
    }
}
