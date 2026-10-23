package it.hurts.metallurgy_reforged.recipe;
import it.hurts.metallurgy_reforged.machine.ProcessingDefinition;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;
public final class ProcessingRecipe implements Recipe<ProcessingInput> {
    public final ProcessingDefinition definition;
    public ProcessingRecipe(ProcessingDefinition definition) {this.definition=definition;}
    @Override public boolean matches(ProcessingInput input,Level level) {return (definition.kind().equals(input.kind()) || definition.kind().equals("generic_crusher") && input.kind().equals("crusher")) && definition.orientation(input.getItem(0),input.getItem(1),true)!=0;}
    @Override public ItemStack assemble(ProcessingInput input,HolderLookup.Provider registries) {var result=definition.result(input.first(),input.second());return result==null?ItemStack.EMPTY:result.output();}
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) {return definition.output();}
    @Override public boolean canCraftInDimensions(int width,int height) {return true;}
    @Override public boolean isSpecial() {return true;}
    @Override public NonNullList<Ingredient> getIngredients() {var list=NonNullList.<Ingredient>create();list.addAll(definition.ingredients());return list;}
    @Override public RecipeSerializer<ProcessingRecipe> getSerializer() {return ProcessingRegistration.SERIALIZER.get();}
    @Override public RecipeType<ProcessingRecipe> getType() {return ProcessingRegistration.TYPE.get();}
    public static final class Serializer implements RecipeSerializer<ProcessingRecipe> {
        private static final MapCodec<ProcessingRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Codec.STRING.fieldOf("kind").forGetter((ProcessingRecipe r)->r.definition.kind()),
            Ingredient.CODEC_NONEMPTY.listOf().optionalFieldOf("ingredients",List.of()).forGetter(r->r.definition.ingredients()),
            Codec.intRange(1,64).listOf().optionalFieldOf("amounts",List.of()).forGetter(r->r.definition.amounts()),
            ItemStack.CODEC.optionalFieldOf("result",ItemStack.EMPTY).forGetter(r->r.definition.output()),
            Codec.floatRange(0,1000).optionalFieldOf("experience",0F).forGetter(r->r.definition.xp()),
            Codec.STRING.optionalFieldOf("effect","").forGetter(r->r.definition.effect()),
            Codec.intRange(1,1728000).optionalFieldOf("duration",24000).forGetter(r->r.definition.duration()),
            Codec.intRange(0,255).optionalFieldOf("amplifier",0).forGetter(r->r.definition.amplifier())
        ).apply(i,(kind,ingredients,amounts,result,xp,effect,duration,amplifier)->new ProcessingRecipe(new ProcessingDefinition(kind,ingredients,amounts.isEmpty()?Collections.nCopies(ingredients.size(),1):amounts,result,xp,effect,duration,amplifier))));
        private static final StreamCodec<RegistryFriendlyByteBuf,ProcessingRecipe> STREAM=new StreamCodec<>() {
            @Override public ProcessingRecipe decode(RegistryFriendlyByteBuf b) {
                String kind=b.readUtf(16);int count=b.readVarInt();if(count<0 || count>2) throw new IllegalArgumentException("Invalid processing input count");
                var ingredients=new ArrayList<Ingredient>();var amounts=new ArrayList<Integer>();for(int n=0;n<count;n++) {ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(b));amounts.add(b.readVarInt());}
                return new ProcessingRecipe(new ProcessingDefinition(kind,ingredients,amounts,ItemStack.OPTIONAL_STREAM_CODEC.decode(b),b.readFloat(),b.readUtf(256),b.readVarInt(),b.readVarInt()));
            }
            @Override public void encode(RegistryFriendlyByteBuf b,ProcessingRecipe r) {
                var d=r.definition;b.writeUtf(d.kind(),16);b.writeVarInt(d.ingredients().size());for(int n=0;n<d.ingredients().size();n++) {Ingredient.CONTENTS_STREAM_CODEC.encode(b,d.ingredients().get(n));b.writeVarInt(d.amounts().get(n));}
                ItemStack.OPTIONAL_STREAM_CODEC.encode(b,d.output());b.writeFloat(d.xp());b.writeUtf(d.effect(),256);b.writeVarInt(d.duration());b.writeVarInt(d.amplifier());
            }
        };
        @Override public MapCodec<ProcessingRecipe> codec() {return CODEC;}
        @Override public StreamCodec<RegistryFriendlyByteBuf,ProcessingRecipe> streamCodec() {return STREAM;}
    }
}
