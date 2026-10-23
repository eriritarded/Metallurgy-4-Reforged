package it.hurts.metallurgy_reforged.compat;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.config.ContentAvailability;
import it.hurts.metallurgy_reforged.effect.*;
import it.hurts.metallurgy_reforged.gadget.*;
import it.hurts.metallurgy_reforged.material.Materials;
import it.hurts.metallurgy_reforged.recipe.*;
import mezz.jei.api.ingredients.subtypes.*;
import mezz.jei.api.recipe.vanilla.IJeiShapedRecipeBuilder;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import java.util.*;
/** Serializable JEI presentation recipes, assembled by the real common rules. */
public final class JeiCrafting {
    private static final List<String> FORMS=List.of("sword","pickaxe","axe","shovel","hoe","helmet","chestplate","leggings","boots");
    private static boolean rule(String kind) {return !it.hurts.metallurgy_reforged.client.ProcessingClient.craftingRule(kind).isEmpty();}
    public static void subtypes(ISubtypeRegistration registration) {
        List<String> items=new ArrayList<>();items.add("ore_detector");for(String form:FORMS) items.add("tartarite_"+form);
        for(String id:items) registration.registerSubtypeInterpreter(Content.ITEM_MAP.get(id).get(),new ISubtypeInterpreter<ItemStack>() {
            @Override public Object getSubtypeData(ItemStack stack,UidContext context) {String value=subtype(stack);return value.isEmpty()?null:value;}
        });
    }
    private static String subtype(ItemStack stack) {
        if(GadgetLogic.is(stack,"ore_detector")) {int count=GadgetLogic.detectorMetals(stack).size();return count==0?"":"programmed_"+count;}
        try(State state=VersionBridge.stack(stack)) {return state.string("paragon");}
    }
    private static void define(IJeiShapedRecipeBuilder builder,char symbol,ItemStack stack) {builder.define(symbol,Ingredient.of(stack.getItem()),display(stack));}
    private static net.minecraft.world.item.crafting.display.SlotDisplay display(ItemStack stack) {return new net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(stack));}
    public static void recipes(IRecipeRegistration registration) {
        List<RecipeHolder<CraftingRecipe>> recipes=new ArrayList<>();
        var metals=Materials.ALL.stream().filter(m->m.oreLevel()>=0).toList();
        if(rule("detector") && metals.size()>=3) {
            for(int count=1;count<=3;count++) for(int offset=0;offset<metals.size();offset++) {
                var inputs=new ArrayList<ItemStack>();inputs.add(Content.stack("ore_detector"));
                for(int n=0;n<count;n++) inputs.add(Content.stack(metals.get((offset+n)%metals.size()).name()+"_ingot"));
                var programmed=DetectorCrafting.assemble(inputs);if(programmed.isEmpty()) continue;
                var builder=registration.getVanillaRecipeFactory().createShapedRecipeBuilder(CraftingBookCategory.MISC,display(programmed));
                define(builder,'D',inputs.get(0));define(builder,'I',inputs.get(1));builder.pattern("DI");
                if(count>1) {define(builder,'J',inputs.get(2));if(count==3) {define(builder,'K',inputs.get(3));builder.pattern("JK");}else builder.pattern("J ");}
                String name="detector_program_"+count+"_"+metals.get(offset).name();recipes.add(new RecipeHolder<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,Identifier.fromNamespaceAndPath("metallurgy","jei_examples/"+name)),builder.build()));
                var clean=DetectorCrafting.assemble(List.of(programmed));
                builder=registration.getVanillaRecipeFactory().createShapedRecipeBuilder(CraftingBookCategory.MISC,display(clean));define(builder,'D',programmed);builder.pattern("D");
                name="detector_clear_"+count+"_"+metals.get(offset).name();recipes.add(new RecipeHolder<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,Identifier.fromNamespaceAndPath("metallurgy","jei_examples/"+name)),builder.build()));
            }
            registration.addItemStackInfo(Content.stack("ore_detector"),Component.translatable("jei.metallurgy.detector_programming"));
        }
        if(rule("paragon")) for(var metal:Materials.ALL) {
            if(metal.name().equals("tartarite") || !metal.hasTools() && !metal.hasArmor() || !ContentAvailability.enabled("metallurgy:"+metal.name()+"_block")) continue;
            var block=Content.stack(metal.name()+"_block");
            for(String form:FORMS) {
                var equipment=Content.stack("tartarite_"+form);if(!ContentAvailability.enabled(VersionBridge.itemId(equipment))) continue;
                List<ItemStack> grid=List.of(ItemStack.EMPTY,block,ItemStack.EMPTY,block,equipment,block,ItemStack.EMPTY,block,ItemStack.EMPTY);
                var output=Paragon.assemble(grid,3,3);if(output.isEmpty()) continue;
                var builder=registration.getVanillaRecipeFactory().createShapedRecipeBuilder(CraftingBookCategory.MISC,display(output));
                define(builder,'B',block);define(builder,'E',equipment);builder.pattern(" B ").pattern("BEB").pattern(" B ");
                String name="paragon_"+form+"_"+metal.name();recipes.add(new RecipeHolder<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,Identifier.fromNamespaceAndPath("metallurgy","jei_examples/"+name)),builder.build()));
            }
        }
        registration.addRecipes(mezz.jei.api.constants.RecipeTypes.CRAFTING,recipes);
        if(rule("paragon")) registration.addItemStackInfo(FORMS.stream().map(f->Content.stack("tartarite_"+f)).toList(),Component.translatable("jei.metallurgy.paragon_absorption"));
    }
    private JeiCrafting() {}
}
