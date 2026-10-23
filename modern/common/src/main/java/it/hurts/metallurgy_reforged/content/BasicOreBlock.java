package it.hurts.metallurgy_reforged.content;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Explosion;
/** Source BlockOre explosion eligibility also applies to its nonmetal ores. */
public final class BasicOreBlock extends Block {
    public BasicOreBlock(Properties properties) {super(properties);}
    @Override public boolean dropFromExplosion(Explosion explosion) {return java.util.concurrent.ThreadLocalRandom.current().nextInt(4)>0;}
}
