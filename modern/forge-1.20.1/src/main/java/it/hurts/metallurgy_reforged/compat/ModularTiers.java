package it.hurts.metallurgy_reforged.compat;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/** Forge's sorted tiers are shared by Tetra and Silent Gear. No optional API
 * classes are referenced, including on a dedicated server without either mod. */
public final class ModularTiers {
    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        boolean tetra = ModList.get().isLoaded("tetra");
        if (!tetra && !ModList.get().isLoaded("silentgear") && !ModList.get().isLoaded("tconstruct")) return;
        Object previous = Tiers.NETHERITE;
        for (int level = 5; level <= 7; level++) {
            ResourceLocation id = new ResourceLocation("metallurgy", "level_" + level);
            var tier = new ForgeTier(level, 1, 1, 0, 0,
                TagKey.create(Registries.BLOCK, new ResourceLocation("metallurgy", "needs_level_" + level)),
                () -> Ingredient.EMPTY);
            // Tetra's special forge hammer must remain above craftable tiers.
            List<Object> before = tetra ? List.of(new ResourceLocation("tetra", "maxed_forge_hammer")) : List.of();
            TierSortingRegistry.registerTier(tier, id, List.of(previous), before);
            previous = id;
        }
    }

    public static boolean isModularPickaxe(ItemStack stack, BlockState state) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !List.of("tetra","silentgear","tconstruct").contains(id.getNamespace())) return false;
        // The assembled item owns its current tool actions and material tier.
        // Merely holding any item from either mod does not grant ore access.
        return stack.canPerformAction(ToolActions.PICKAXE_DIG) && stack.isCorrectToolForDrops(state);
    }

    private ModularTiers() {}
}
