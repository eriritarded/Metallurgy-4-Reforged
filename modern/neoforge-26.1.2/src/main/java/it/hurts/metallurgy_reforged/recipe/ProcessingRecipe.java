package it.hurts.metallurgy_reforged.recipe;
import it.hurts.metallurgy_reforged.machine.ProcessingDefinition;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;
public final class ProcessingRecipe implements Recipe<ProcessingInput> {
    public final ProcessingDefinition definition;
    public ProcessingRecipe(ProcessingDefinition definition) {this.definition=definition;}
    @Override public boolean matches(ProcessingInput input,Level level) {return (definition.kind().equals(input.kind()) || definition.kind().equals("generic_crusher") && input.kind().equals("crusher")) && definition.orientation(input.getItem(0),input.getItem(1),true)!=0;}
    @Override public ItemStack assemble(ProcessingInput input) {var result=definition.result(input.first(),input.second());return result==null?ItemStack.EMPTY:result.output();}
    @Override public boolean isSpecial() {return true;}
    @Override public boolean showNotification() {return false;}
    @Override public String group() {return "";}
    @Override public PlacementInfo placementInfo() {return PlacementInfo.NOT_PLACEABLE;}
    @Override public RecipeBookCategory recipeBookCategory() {return RecipeBookCategories.FURNACE_MISC;}
    @Override public RecipeSerializer<ProcessingRecipe> getSerializer() {return ProcessingRegistration.SERIALIZER.get();}
    @Override public RecipeType<ProcessingRecipe> getType() {return ProcessingRegistration.TYPE.get();}
    public static final class Serializer {
        private record RecipeData(String kind,List<Ingredient> ingredients,List<Integer> amounts,Optional<ItemStackTemplate> result,float xp,String effect,int duration,int amplifier) {}
        private static final MapCodec<RecipeData> DATA_CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Codec.STRING.fieldOf("kind").forGetter(RecipeData::kind),
            Ingredient.CODEC.listOf().optionalFieldOf("ingredients",List.of()).forGetter(RecipeData::ingredients),
            Codec.intRange(1,64).listOf().optionalFieldOf("amounts",List.of()).forGetter(RecipeData::amounts),
            ItemStackTemplate.CODEC.optionalFieldOf("result").forGetter(RecipeData::result),
            Codec.floatRange(0,1000).optionalFieldOf("experience",0F).forGetter(RecipeData::xp),
            Codec.STRING.optionalFieldOf("effect","").forGetter(RecipeData::effect),
            Codec.intRange(1,1728000).optionalFieldOf("duration",24000).forGetter(RecipeData::duration),
            Codec.intRange(0,255).optionalFieldOf("amplifier",0).forGetter(RecipeData::amplifier)
        ).apply(i,RecipeData::new));
        private static final MapCodec<ProcessingRecipe> CODEC=DATA_CODEC.flatXmap(data->{
            try {
                return DataResult.success(new ProcessingRecipe(new ProcessingDefinition(data.kind(),data.ingredients(),data.amounts().isEmpty()?Collections.nCopies(data.ingredients().size(),1):data.amounts(),data.result(),data.xp(),data.effect(),data.duration(),data.amplifier())));
            } catch(IllegalArgumentException exception) {
                return DataResult.error(()->"Invalid Metallurgy processing recipe: "+exception.getMessage());
            }
        },recipe->{
            var d=recipe.definition;
            return DataResult.success(new RecipeData(d.kind(),d.ingredients(),d.amounts(),d.outputTemplate(),d.xp(),d.effect(),d.duration(),d.amplifier()));
        });
        private static final StreamCodec<RegistryFriendlyByteBuf,ProcessingRecipe> STREAM=new StreamCodec<>() {
            @Override public ProcessingRecipe decode(RegistryFriendlyByteBuf b) {
                String kind=b.readUtf(16);int count=b.readVarInt();if(count<0 || count>2) throw new IllegalArgumentException("Invalid processing input count");
                var ingredients=new ArrayList<Ingredient>();var amounts=new ArrayList<Integer>();for(int n=0;n<count;n++) {ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(b));amounts.add(b.readVarInt());}
                Optional<ItemStackTemplate> result=b.readBoolean()?Optional.of(ItemStackTemplate.STREAM_CODEC.decode(b)):Optional.empty();
                return new ProcessingRecipe(new ProcessingDefinition(kind,ingredients,amounts,result,b.readFloat(),b.readUtf(256),b.readVarInt(),b.readVarInt()));
            }
            @Override public void encode(RegistryFriendlyByteBuf b,ProcessingRecipe r) {
                var d=r.definition;b.writeUtf(d.kind(),16);b.writeVarInt(d.ingredients().size());for(int n=0;n<d.ingredients().size();n++) {Ingredient.CONTENTS_STREAM_CODEC.encode(b,d.ingredients().get(n));b.writeVarInt(d.amounts().get(n));}
                b.writeBoolean(d.outputTemplate().isPresent());d.outputTemplate().ifPresent(result->ItemStackTemplate.STREAM_CODEC.encode(b,result));b.writeFloat(d.xp());b.writeUtf(d.effect(),256);b.writeVarInt(d.duration());b.writeVarInt(d.amplifier());
            }
        };
        public static RecipeSerializer<ProcessingRecipe> create() {return new RecipeSerializer<>(CODEC,STREAM);}
    }
}
