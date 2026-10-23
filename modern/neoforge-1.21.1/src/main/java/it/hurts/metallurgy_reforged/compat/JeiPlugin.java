package it.hurts.metallurgy_reforged.compat;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.machine.*;
import it.hurts.metallurgy_reforged.recipe.ProcessingAccess;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.*;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.transfer.*;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import java.util.*;
/** Only JEI discovers this class; the base mod never links it on either side. */
@mezz.jei.api.JeiPlugin
public final class JeiPlugin implements IModPlugin {
    public record View(String id,String source,String kind,List<List<ItemStack>> inputs,ItemStack output,String effect,int duration,float xp) {}
    private static final Map<String,RecipeType<View>> TYPES=Map.of("crusher",RecipeType.create("metallurgy","crusher",View.class),"alloyer",RecipeType.create("metallurgy","alloyer",View.class),"chamber",RecipeType.create("metallurgy","sublimation_chamber",View.class));
    @Override public ResourceLocation getPluginUid() {return ResourceLocation.fromNamespaceAndPath("metallurgy","native_recipes");}
    @Override public void registerItemSubtypes(ISubtypeRegistration registration) {JeiCrafting.subtypes(registration);}
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {for(String kind:List.of("crusher","alloyer","chamber")) registration.addRecipeCategories(new Category(kind,registration.getJeiHelpers().getGuiHelper()));}
    public static List<View> views() {
        var result=new ArrayList<View>();var level=Minecraft.getInstance().level;if(level==null) return result;
        for(var entry:ProcessingAccess.entries(level)) {
            var d=entry.definition();
            if(d.kind().equals("generic_crusher")) {
                for(var item:BuiltInRegistries.ITEM) {var input=new ItemStack(item);var output=GenericCrushing.find(input);if(output!=null) result.add(new View(entry.id()+"/"+BuiltInRegistries.ITEM.getKey(item).toString().replace(':','/'),entry.id(),"crusher",List.of(List.of(input)),output.output(),"",0,output.xp()));}
                continue;
            }
            var inputs=new ArrayList<List<ItemStack>>();
            for(int slot=0;slot<d.ingredients().size();slot++) {int amount=d.amounts().get(slot);inputs.add(Arrays.stream(d.ingredients().get(slot).getItems()).filter(s->!s.isEmpty()).map(s->s.copyWithCount(amount)).toList());}
            if(inputs.stream().anyMatch(List::isEmpty)) continue;
            result.add(new View(entry.id(),entry.id(),d.kind(),List.copyOf(inputs),d.output(),d.effect(),d.duration(),d.xp()));
        }
        result.sort(Comparator.comparing(View::id));return result;
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        JeiCrafting.recipes(registration);
        var views=views();for(var kind:TYPES.keySet()) registration.addRecipes(TYPES.get(kind),views.stream().filter(v->v.kind.equals(kind)).toList());
        for(String id:List.of("tar_ore","tar","bitumen")) registration.addItemStackInfo(Content.stack(id),Component.translatable("description.jei_compat.tar_processing"));
        registration.addItemStackInfo(Content.stack("lemurite_shield"),Component.translatable("description.jei_compat.invisibility_shield"));
        registration.addItemStackInfo(Content.stack("thermite_dust"),Component.translatable("description.jei_compat.thermite"));
        fluidInfo(registration,registration.getJeiHelpers().getPlatformFluidHelper());
        registration.addItemStackInfo(List.of(Content.stack("krik_helmet"),Content.stack("krik_chestplate"),Content.stack("krik_leggings"),Content.stack("krik_boots")),Component.translatable("description.jei_compat.krik_armor"));
    }
    private static <T> void fluidInfo(IRecipeRegistration registration,mezz.jei.api.helpers.IPlatformFluidHelper<T> helper) {
        var fluid=it.hurts.metallurgy_reforged.fluid.FluidContent.ENTRIES.get("molten_thermite").still.get();
        registration.addIngredientInfo(helper.create(fluid.builtInRegistryHolder(),1000),helper.getFluidIngredientType(),Component.translatable("description.jei_compat.thermite"));
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {for(String kind:TYPES.keySet()) registration.addRecipeCatalyst(Content.stack(kind.equals("chamber")?"sublimation_chamber":kind),TYPES.get(kind));}
    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        for(String kind:List.of("crusher","alloyer")) registration.addRecipeTransferHandler(new IRecipeTransferInfo<MachineMenu,View>() {
            @Override public Class<MachineMenu> getContainerClass() {return MachineMenu.class;}
            @Override public Optional<MenuType<MachineMenu>> getMenuType() {return Optional.of(Content.MACHINE_MENU.get());}
            @Override public RecipeType<View> getRecipeType() {return TYPES.get(kind);}
            @Override public boolean canHandle(MachineMenu menu,View recipe) {return menu.alloy==kind.equals("alloyer");}
            @Override public IRecipeTransferError getHandlingError(MachineMenu menu,View recipe) {return registration.getTransferHelper().createUserErrorWithTooltip(Component.translatable("container."+kind));}
            @Override public List<Slot> getRecipeSlots(MachineMenu menu,View recipe) {return List.copyOf(menu.slots.subList(0,menu.alloy?2:1));}
            @Override public List<Slot> getInventorySlots(MachineMenu menu,View recipe) {int start=menu.machine.getContainerSize();return List.copyOf(menu.slots.subList(start,start+36));}
        });
    }
    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(it.hurts.metallurgy_reforged.client.MachineScreen.class,new mezz.jei.api.gui.handlers.IGuiContainerHandler<it.hurts.metallurgy_reforged.client.MachineScreen>() {
            @Override public Collection<mezz.jei.api.gui.handlers.IGuiClickableArea> getGuiClickableAreas(it.hurts.metallurgy_reforged.client.MachineScreen screen,double x,double y) {
                boolean alloy=screen.getMenu().alloy;return List.of(mezz.jei.api.gui.handlers.IGuiClickableArea.createBasic(alloy?40:93,32,7,33,TYPES.get(alloy?"alloyer":"crusher")));
            }
        });
    }
    private static final class Category implements IRecipeCategory<View> {
        private final String kind;private final IDrawable background,icon;private final IDrawableAnimated flame,bar,flow;
        Category(String kind,IGuiHelper helper) {
            this.kind=kind;boolean chamber=kind.equals("chamber");var texture=ResourceLocation.fromNamespaceAndPath("metallurgy","textures/gui/"+(chamber?"sublimation_chamber_jei":kind)+".png");
            background=helper.createDrawable(texture,chamber?4:0,chamber?4:0,chamber?168:176,chamber?74:108);
            icon=helper.createDrawableItemStack(Content.stack(chamber?"sublimation_chamber":kind));
            flame=helper.createAnimatedDrawable(helper.createDrawable(texture,176,chamber?0:61,chamber?14:17,chamber?14:17),chamber?150:200,IDrawableAnimated.StartDirection.TOP,true);
            bar=chamber?null:helper.createAnimatedDrawable(helper.createDrawable(texture,176,0,7,33),140,IDrawableAnimated.StartDirection.BOTTOM,false);
            flow=kind.equals("alloyer")?helper.createAnimatedDrawable(helper.createDrawable(texture,176,81,10,25),80,IDrawableAnimated.StartDirection.TOP,false):null;
        }
        @Override public RecipeType<View> getRecipeType() {return TYPES.get(kind);}
        @Override public Component getTitle() {return Component.translatable("container."+(kind.equals("chamber")?"chamber":kind));}
        @Override public int getWidth() {return kind.equals("chamber")?168:176;}
        @Override public int getHeight() {return kind.equals("chamber")?74:108;}
        @Override public IDrawable getIcon() {return icon;}
        @Override public ResourceLocation getRegistryName(View recipe) {return ResourceLocation.tryParse(recipe.id);}
        @Override public List<ResourceLocation> getSourceRecipeIds(View recipe) {return List.of(ResourceLocation.tryParse(recipe.source));}
        @Override public void setRecipe(IRecipeLayoutBuilder builder,View recipe,IFocusGroup focuses) {
            if(kind.equals("alloyer")) {builder.addInputSlot(102,24).addItemStacks(recipe.inputs.get(0));builder.addInputSlot(123,24).addItemStacks(recipe.inputs.get(1));builder.addOutputSlot(57,74).addItemStack(recipe.output);}
            else if(kind.equals("crusher")) {builder.addInputSlot(61,28).addItemStacks(recipe.inputs.get(0));builder.addOutputSlot(67,68).addItemStack(recipe.output);}
            else builder.addInputSlot(43,27).addItemStacks(recipe.inputs.get(0));
        }
        @Override public void draw(View recipe,IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY) {
            background.draw(graphics,0,0);
            if(kind.equals("chamber")) {
                flame.draw(graphics,75,49);var key=VersionBridge.effectKey(recipe.effect);var effect=BuiltInRegistries.MOB_EFFECT.get(key);
                var font=Minecraft.getInstance().font;String name=effect==null?recipe.effect:Component.translatable(effect.getDescriptionId()).getString();
                graphics.drawString(font,font.plainSubstrByWidth(name,70),92,20,0xff404040,false);
                graphics.drawString(font,(recipe.duration/20)+" s",92,35,0xff404040,false);
            } else {flame.draw(graphics,kind.equals("alloyer")?110:128,61);bar.draw(graphics,kind.equals("alloyer")?40:93,33);if(flow!=null) flow.draw(graphics,52,32);}
        }
        @Override public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tooltip,View recipe,IRecipeSlotsView slots,double x,double y) {for(var line:tooltipLines(recipe)) tooltip.add(line);}
        private List<Component> tooltipLines(View recipe) {
            if(kind.equals("chamber")) {var effect=BuiltInRegistries.MOB_EFFECT.get(VersionBridge.effectKey(recipe.effect));return List.of(effect==null?Component.literal(recipe.effect):Component.translatable(effect.getDescriptionId()),Component.literal(recipe.duration/20+" s"));}
            return List.of(Component.literal("7 s · "+recipe.xp+" XP / item"));
        }
    }
}
