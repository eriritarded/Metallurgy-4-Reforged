package it.hurts.metallurgy_reforged.compat;
import net.minecraft.world.entity.player.Player;
import java.util.function.Predicate;
/** Avoids an optional TConstruct class in the vanilla menu mixin's linkage. */
public final class OptionalArmorGuard {
    private static Predicate<Player> locked=player->false;
    public static void register(Predicate<Player> predicate) {locked=predicate;}
    public static boolean locked(Player player) {return locked.test(player);}
    private OptionalArmorGuard() {}
}
