"""Native recipe interfaces/codecs. Kept explicit at the two API boundaries."""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PACKAGE='it/hurts/metallurgy_reforged/recipe'

def write(target,name,text):
    path=ROOT/target/'src/main/java'/PACKAGE/name
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(text,encoding='utf-8')

def generate121():
    target='neoforge-1.21.1'
    write(target,'ProcessingInput.java','''package it.hurts.metallurgy_reforged.recipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
public record ProcessingInput(String kind,ItemStack first,ItemStack second) implements RecipeInput {
    @Override public ItemStack getItem(int slot) {return switch(slot) {case 0->first;case 1->second;default->throw new IndexOutOfBoundsException(slot);};}
    @Override public int size() {return 2;}
}
''')
    write(target,'ProcessingRecipe.java','''package it.hurts.metallurgy_reforged.recipe;
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
''')
    path=ROOT/target/'src/main/java'/PACKAGE/'ProcessingAccess.java'
    t=path.read_text(encoding='utf-8').replace('recipe.getId().toString(),recipe.definition','recipe.id().toString(),recipe.value().definition')
    path.write_text(t,encoding='utf-8')

def generate26():
    target='neoforge-26.1.2'
    path=ROOT/target/'src/main/java/it/hurts/metallurgy_reforged/Metallurgy.java'
    t=path.read_text(encoding='utf-8').replace('        Content.register(bus);','        Content.register(bus);\n        bus.addListener(it.hurts.metallurgy_reforged.network.ProcessingNetwork::register);')
    path.write_text(t,encoding='utf-8')
    path=ROOT/target/'src/main/java/it/hurts/metallurgy_reforged/client/ProcessingClient.java'
    t=path.read_text(encoding='utf-8')
    start=t.index('    @SubscribeEvent public static void received(');end=t.index('    private ProcessingClient()',start)
    t=t[:start]+'''    private static long generation;
    private static int expected,parts,bytes;
    private static java.util.Map<String,String> crafting=java.util.Map.of(),pendingCrafting=java.util.Map.of();
    public static String craftingRule(String kind) {return crafting.getOrDefault(kind,"");}
    private static final java.util.ArrayList<ProcessingAccess.Entry> pending=new java.util.ArrayList<>();
    private static void clearPending() {generation=0;expected=0;parts=0;bytes=0;pending.clear();pendingCrafting=java.util.Map.of();}
    public static void part(it.hurts.metallurgy_reforged.network.ProcessingNetwork.Part part) {
        if(part.index()==0) {clearPending();generation=part.generation();parts=part.total();pendingCrafting=part.crafting();}
        if(generation!=part.generation() || expected!=part.index() || parts!=part.total() || !pendingCrafting.equals(part.crafting())) {clearPending();return;}
        bytes+=part.bytes();pending.addAll(part.entries());expected++;
        if(bytes>it.hurts.metallurgy_reforged.network.ProcessingNetwork.MAX_TOTAL || pending.size()>it.hurts.metallurgy_reforged.network.ProcessingNetwork.MAX_RECIPES) {clearPending();throw new IllegalArgumentException("Oversized processing snapshot");}
        if(expected==parts) {
            var ids=new java.util.HashSet<String>();for(var entry:pending) if(!ids.add(entry.id())) {clearPending();throw new IllegalArgumentException("Duplicate processing recipe ID");}
            recipes=pending.stream().sorted(java.util.Comparator.comparing(ProcessingAccess.Entry::id)).toList();crafting=pendingCrafting;clearPending();
        }
    }
    @SubscribeEvent public static void disconnected(ClientPlayerNetworkEvent.LoggingOut event) {recipes=List.of();crafting=java.util.Map.of();clearPending();ProcessingAccess.invalidate();}
'''+t[end:]
    path.write_text(t,encoding='utf-8')
    path=ROOT/target/'src/main/java'/PACKAGE/'ProcessingEvents.java'
    t=path.read_text(encoding='utf-8').replace('ProcessingAccess.invalidate();}', 'ProcessingAccess.invalidate();it.hurts.metallurgy_reforged.network.ProcessingNetwork.sync(event);}',1)
    path.write_text(t,encoding='utf-8')
    # ProcessingRecipe and machine/ProcessingDefinition are maintained native 26 adapters.
    # Their ItemStackTemplate codecs must not be regenerated from the older ItemStack API.
    path=ROOT/target/'src/main/java'/PACKAGE/'ProcessingRegistration.java'
    t=path.read_text(encoding='utf-8').replace('ProcessingRecipe.Serializer::new','ProcessingRecipe.Serializer::create')
    path.write_text(t,encoding='utf-8')
    path=ROOT/target/'src/main/java'/PACKAGE/'ProcessingAccess.java'
    t=path.read_text(encoding='utf-8').replace('server.getRecipeManager()','server.recipeAccess()').replace('manager.getAllRecipesFor(ProcessingRegistration.TYPE.get())','manager.recipeMap().byType(ProcessingRegistration.TYPE.get())').replace('recipe.id().toString()','recipe.id().identifier().toString()')
    path.write_text(t,encoding='utf-8')
