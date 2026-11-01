package it.hurts.metallurgy_reforged.config;
/** Restart-scoped switches for native optional resource conditions. */
public final class IntegrationFlags {
    public static boolean enabled(String integration) {return switch(integration) {
        case "tetra"->GeneralConfig.tetraIntegration;case "silentgear"->GeneralConfig.silentGemsIntegration;
        case "tconstruct"->GeneralConfig.tiCIntegration;case "conarm"->GeneralConfig.conarmIntegration;
        case "industrialforegoing"->GeneralConfig.inForegoingIntegration;case "projecte"->GeneralConfig.projectEIntegration;
        case "spartanweaponry"->GeneralConfig.spartanWeaponryIntegration;
        case "thaumaturge"->GeneralConfig.thaumaturgeIntegration;
        case "thaumaturge_unify"->GeneralConfig.thaumaturgeIntegration&&GeneralConfig.thaumaturgeUnifyMetals;
        case "mekanism","enderio","thermal_expansion","chisel"->true;default->false;
    };}
    private IntegrationFlags() {}
}
