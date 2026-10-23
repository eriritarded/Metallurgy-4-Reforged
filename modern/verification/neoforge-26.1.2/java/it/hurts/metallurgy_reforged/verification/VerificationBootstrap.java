package it.hurts.metallurgy_reforged.verification;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
@EventBusSubscriber(modid="metallurgy")
public final class VerificationBootstrap {
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION,helper->{
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","materialstatsandregistrations"),CoreGameTests::materialStatsAndRegistrations);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","chestpersistenceautomationandshiftclick"),CoreGameTests::chestPersistenceAutomationAndShiftClick);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","brokenchestdropsinventoryonce"),CoreGameTests::brokenChestDropsInventoryOnce);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","divinegraceownershipregression"),CoreGameTests::divineGraceOwnershipRegression);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","immediateweaponandtruedamage"),CoreGameTests::immediateWeaponAndTrueDamage);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","delayedmithrilandowneddashcleanup"),CoreGameTests::delayedMithrilAndOwnedDashCleanup);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","equipmentextraattributes"),CoreGameTests::equipmentExtraAttributes);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","machineprocessingfuelandsidedautomation"),CoreGameTests::machineProcessingFuelAndSidedAutomation);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","enhancedfuelandloadedcraftingrecipes"),CoreGameTests::enhancedFuelAndLoadedCraftingRecipes);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","nativeparagonrecipepreservesequipment"),CoreGameTests::nativeParagonRecipePreservesEquipment);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","haderothrebirthandarcaneequipment"),CoreGameTests::haderothRebirthAndArcaneEquipment);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","soulbounddropscloneandplatinumunbreakable"),CoreGameTests::soulboundDropsCloneAndPlatinumUnbreakable);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","adamantinenativedeathprotection"),CoreGameTests::adamantineNativeDeathProtection);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","platinumpurificationandvyroxeresstacks"),CoreGameTests::platinumPurificationAndVyroxeresStacks);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","kalendriteregendelay"),CoreGameTests::kalendriteRegenDelay);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","sublimationaurapauseandstatefulblockitem"),CoreGameTests::sublimationAuraPauseAndStatefulBlockItem);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","sublimationrecipebatchboundaries"),CoreGameTests::sublimationRecipeBatchBoundaries);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","atlarusincomingprojectilewind"),CoreGameTests::atlarusIncomingProjectileWind);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","ceruclaseauraopposingentitycategory"),CoreGameTests::ceruclaseAuraOpposingEntityCategory);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","celenegilsixthhitpurge"),CoreGameTests::celenegilSixthHitPurge);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","desichalkoschargeandconsumption"),CoreGameTests::desichalkosChargeAndConsumption);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","ignatiusfireandcustomwaterdamage"),CoreGameTests::ignatiusFireAndCustomWaterDamage);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","prometheumcrouchgrowthcycle"),CoreGameTests::prometheumCrouchGrowthCycle);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","amordrineserverbudgetandintentvalidation"),CoreGameTests::amordrineServerBudgetAndIntentValidation);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","krikinventoryheightboundandfallprotection"),CoreGameTests::krikInventoryHeightBoundAndFallProtection);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","inolashitedelayedrecallandcleanup"),CoreGameTests::inolashiteDelayedRecallAndCleanup);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","quicksilvernativeliquidcollisionandcleanup"),CoreGameTests::quicksilverNativeLiquidCollisionAndCleanup);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","etheriumpausedclockandownedphysicscleanup"),CoreGameTests::etheriumPausedClockAndOwnedPhysicsCleanup);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","sanguiniteoriginalmobrevivesonlyonce"),CoreGameTests::sanguiniteOriginalMobRevivesOnlyOnce);
            helper.register(Identifier.fromNamespaceAndPath("metallurgy","damascusknightidentitydamageandownercleanup"),CoreGameTests::damascusKnightIdentityDamageAndOwnerCleanup);
        });
    }
}
