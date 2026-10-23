package it.hurts.metallurgy_reforged.recipe;
import it.hurts.metallurgy_reforged.machine.ProcessingDefinition;
import com.google.gson.*;
import net.minecraft.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;
public final class ProcessingRecipe implements Recipe<ProcessingInput> {
    private final ResourceLocation id;
    public final ProcessingDefinition definition;
    public ProcessingRecipe(ResourceLocation id,ProcessingDefinition definition) {this.id=id;this.definition=definition;}
    @Override public boolean matches(ProcessingInput input,Level level) {return (definition.kind().equals(input.kind()) || definition.kind().equals("generic_crusher") && input.kind().equals("crusher")) && definition.orientation(input.getItem(0),input.getItem(1),true)!=0;}
    @Override public ItemStack assemble(ProcessingInput input,RegistryAccess registries) {var result=definition.result(input.getItem(0),input.getItem(1));return result==null?ItemStack.EMPTY:result.output();}
    @Override public ItemStack getResultItem(RegistryAccess registries) {return definition.output();}
    @Override public boolean canCraftInDimensions(int width,int height) {return true;}
    @Override public ResourceLocation getId() {return id;}
    @Override public boolean isSpecial() {return true;}
    @Override public NonNullList<Ingredient> getIngredients() {var list=NonNullList.<Ingredient>create();list.addAll(definition.ingredients());return list;}
    @Override public RecipeSerializer<ProcessingRecipe> getSerializer() {return ProcessingRegistration.SERIALIZER.get();}
    @Override public RecipeType<ProcessingRecipe> getType() {return ProcessingRegistration.TYPE.get();}
    public static final class Serializer implements RecipeSerializer<ProcessingRecipe> {
        @Override public ProcessingRecipe fromJson(ResourceLocation id,JsonObject json) {
            var inputs=new ArrayList<Ingredient>();for(var value:GsonHelper.getAsJsonArray(json,"ingredients")) inputs.add(Ingredient.fromJson(value));
            var amounts=new ArrayList<Integer>();if(json.has("amounts")) for(var value:json.getAsJsonArray("amounts")) amounts.add(value.getAsInt());else for(var ignored:inputs) amounts.add(1);
            ItemStack result=json.has("result")?net.minecraftforge.common.crafting.CraftingHelper.getItemStack(json.getAsJsonObject("result"),true):ItemStack.EMPTY;
            return new ProcessingRecipe(id,new ProcessingDefinition(GsonHelper.getAsString(json,"kind"),inputs,amounts,result,GsonHelper.getAsFloat(json,"experience",0),GsonHelper.getAsString(json,"effect",""),GsonHelper.getAsInt(json,"duration",24000),GsonHelper.getAsInt(json,"amplifier",0)));
        }
        @Override public ProcessingRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer) {
            String kind=buffer.readUtf(16);int count=buffer.readVarInt();if(count<0 || count>2) throw new IllegalArgumentException("Invalid processing input count");
            var ingredients=new ArrayList<Ingredient>();var amounts=new ArrayList<Integer>();for(int i=0;i<count;i++) {ingredients.add(Ingredient.fromNetwork(buffer));amounts.add(buffer.readVarInt());}
            return new ProcessingRecipe(id,new ProcessingDefinition(kind,ingredients,amounts,buffer.readItem(),buffer.readFloat(),buffer.readUtf(256),buffer.readVarInt(),buffer.readVarInt()));
        }
        @Override public void toNetwork(FriendlyByteBuf buffer,ProcessingRecipe recipe) {
            var d=recipe.definition;buffer.writeUtf(d.kind(),16);buffer.writeVarInt(d.ingredients().size());for(int i=0;i<d.ingredients().size();i++) {d.ingredients().get(i).toNetwork(buffer);buffer.writeVarInt(d.amounts().get(i));}
            buffer.writeItem(d.output());buffer.writeFloat(d.xp());buffer.writeUtf(d.effect(),256);buffer.writeVarInt(d.duration());buffer.writeVarInt(d.amplifier());
        }
    }
}
