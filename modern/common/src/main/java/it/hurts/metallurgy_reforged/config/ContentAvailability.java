package it.hurts.metallurgy_reforged.config;

/** Keep registry IDs stable for saves while disabling acquisition of a content category. */
public final class ContentAvailability {
    public static boolean enabled(String id) {
        if(!id.startsWith("metallurgy:")) return true;
        String path=id.substring(11);var blocks=RegistrationConfig.categoryBlocks;var items=RegistrationConfig.categoryItems;
        if(path.endsWith("_nugget")) return items.enableMetalNuggets;
        if(path.endsWith("_dust") && !java.util.Set.of("gold_dust","iron_dust","sulfur_dust","thermite_dust").contains(path)) return items.enableMetalDusts;
        if(path.endsWith("_sword")) return items.enableMetalSwords;
        if(path.endsWith("_pickaxe")) return items.enableMetalPickaxes;
        if(path.endsWith("_axe")) return items.enableMetalAxes;
        if(path.endsWith("_shovel")) return items.enableMetalShovels;
        if(path.endsWith("_hoe")) return items.enableMetalHoes;
        if(path.endsWith("_helmet") || path.endsWith("_chestplate") || path.endsWith("_leggings") || path.endsWith("_boots")) return items.enableMetalArmorSets;
        if(path.endsWith("_engraved_block")) return blocks.enableEngravedMetalBlocks;
        if(path.endsWith("_large_bricks")) return blocks.enableLargeBricksMetalBlocks;
        if(path.endsWith("_bricks")) return blocks.enableBricksMetalBlocks;
        if(path.endsWith("_crystals")) return blocks.enableCrystalMetalBlocks;
        if(path.endsWith("_hazard_block")) return blocks.enableHazardMetalBlocks;
        if(path.endsWith("_reinforced_glass")) return blocks.enableReinforcedGlassBlocks;
        if(path.endsWith("_block") && !java.util.Set.of("road_block","striped_road_block","bitumen_block","charcoal_block","sulfur_block","bimetal_structure_block").contains(path)) return blocks.enableRawMetalBlocks;
        return true;
    }
    private ContentAvailability() {}
}
