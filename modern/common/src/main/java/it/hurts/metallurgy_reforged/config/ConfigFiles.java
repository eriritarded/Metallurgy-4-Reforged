package it.hurts.metallurgy_reforged.config;
import com.google.gson.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Restart-scoped JSON config, loaded before content registration. Existing user files are never reset. */
public final class ConfigFiles {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final List<Class<?>> ROOTS=java.util.stream.Stream.of(GeneralConfig.class,RegistrationConfig.class,EffectsRosterConfig.class,EffectTweaksConfig.class,GadgetsConfig.class,WorldGenerationConfig.class).flatMap(type->type.isAnnotationPresent(Config.class)?java.util.stream.Stream.of(type):Arrays.stream(type.getDeclaredClasses()).filter(child->child.isAnnotationPresent(Config.class))).toList();
    private static Map<String,Boolean> effects=Map.of();
    private static Path directory;
    public static Path folder() {if(directory==null) throw new IllegalStateException("Metallurgy config is not initialized");return directory;}
    public static synchronized void initialize(Path folder) {
        if(directory!=null) return;
        try {
            Files.createDirectories(folder);
            Map<String,JsonObject> documents=new LinkedHashMap<>();List<Runnable> assignments=new ArrayList<>();
            for(Class<?> type:ROOTS) {
                Config metadata=type.getAnnotation(Config.class);String name=Path.of(metadata.name()).getFileName().toString();
                JsonObject root=documents.get(name);
                if(root==null) {
                    Path file=folder.resolve(name+".json");root=Files.exists(file)?read(file).getAsJsonObject():new JsonObject();documents.put(name,root);
                }
                JsonObject values=root;
                if(!metadata.category().isEmpty()) {
                    if(!root.has(metadata.category())) root.add(metadata.category(),new JsonObject());
                    values=root.getAsJsonObject(metadata.category());
                }
                collect(type,null,values,name,assignments);
            }
            // No partially applied values if a later file contains an invalid setting.
            assignments.forEach(Runnable::run);
            for(var entry:documents.entrySet()) {
                Path file=folder.resolve(entry.getKey()+".json");
                if(!Files.exists(file)) try(Writer writer=Files.newBufferedWriter(file,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW)) {GSON.toJson(entry.getValue(),writer);}
            }
            Map<String,Boolean> roster=new HashMap<>();
            for(Field field:EffectsRosterConfig.class.getFields()) if(field.getType()==boolean.class) {
                String[] parts=field.getName().split("Effect",2);
                String metal=parts[0].replaceAll("([a-z])([A-Z])","$1_$2").toLowerCase(Locale.ROOT);
                roster.put(metal+":"+parts[1].toLowerCase(Locale.ROOT),field.getBoolean(null));
            }
            effects=Map.copyOf(roster);directory=folder;
        } catch(IOException|ReflectiveOperationException|RuntimeException error) {throw new IllegalStateException("Invalid Metallurgy configuration in "+folder+": "+error.getMessage(),error);}
    }
    private static JsonElement read(Path path) throws IOException {
        if(Files.size(path)>4*1024*1024) throw new IOException(path+" exceeds4 MiB");
        try(Reader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)) {return JsonParser.parseReader(reader);}
    }
    private static void collect(Class<?> type,Object owner,JsonObject values,String location,List<Runnable> assignments) throws IllegalAccessException {
        for(Field field:type.getFields()) {
            if(Modifier.isFinal(field.getModifiers()) || field.isAnnotationPresent(Config.Ignore.class) || !Modifier.isStatic(field.getModifiers()) && owner==null) continue;
            Object receiver=Modifier.isStatic(field.getModifiers())?null:owner;Object fallback=field.get(receiver);String name=field.getName();
            if(field.getType().getPackageName().equals(ConfigFiles.class.getPackageName())) {
                if(!values.has(name)) values.add(name,new JsonObject());collect(field.getType(),fallback,values.getAsJsonObject(name),location+"."+name,assignments);continue;
            }
            if(!values.has(name)) values.add(name,GSON.toJsonTree(fallback));
            JsonElement input=values.get(name);Object decoded=GSON.fromJson(input,field.getGenericType());
            validate(field,input,decoded,fallback,location+"."+name);
            assignments.add(()-> {try {field.set(receiver,decoded);}catch(IllegalAccessException error) {throw new IllegalStateException(error);}});
        }
        for(Class<?> nested:type.getDeclaredClasses()) {
            boolean hasStatic=Arrays.stream(nested.getFields()).anyMatch(field->Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers()));
            if(hasStatic) {String name=nested.getSimpleName();if(!values.has(name)) values.add(name,new JsonObject());collect(nested,null,values.getAsJsonObject(name),location+"."+name,assignments);}
        }
    }
    private static void validate(Field field,JsonElement input,Object value,Object fallback,String location) {
        if(value==null) throw new IllegalArgumentException(location+" cannot be null");
        if(field.getType()==boolean.class && (!input.isJsonPrimitive() || !input.getAsJsonPrimitive().isBoolean())) throw new IllegalArgumentException(location+" must be a boolean");
        if(value instanceof Number number) {
            if(!input.isJsonPrimitive() || !input.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException(location+" must be a number");
            double actual=number.doubleValue();if(!Double.isFinite(actual)) throw new IllegalArgumentException(location+" must be finite");
            if(field.getType()==int.class && input.getAsBigDecimal().compareTo(java.math.BigDecimal.valueOf(number.intValue()))!=0) throw new IllegalArgumentException(location+" must be an integer");
            Config.RangeInt integers=field.getAnnotation(Config.RangeInt.class);Config.RangeDouble decimals=field.getAnnotation(Config.RangeDouble.class);
            // Some original literal defaults contradict their own range annotation.
            // Keep those literal defaults; reject newly supplied out-of-range values.
            if(!Objects.equals(value,fallback) && (integers!=null && (actual<integers.min() || actual>integers.max()) || decimals!=null && (actual<decimals.min() || actual>decimals.max()))) throw new IllegalArgumentException(location+" outside its original configured range");
            if(field.getName().equals("lanternLightLevel") && (actual<0 || actual>15)) throw new IllegalArgumentException(location+" must be0..15");
            if(field.getName().equals("gauntletMaxDamage") && actual<1) throw new IllegalArgumentException(location+" must be positive");
            if((field.getName().equals("gauntletHungerModifier") && actual>Integer.MAX_VALUE/2.0) || Math.abs(actual)>Float.MAX_VALUE) throw new IllegalArgumentException(location+" exceeds native numeric limits");
        }
        if(field.getType().isArray()) {
            if(!input.isJsonArray() || Array.getLength(value)>4096) throw new IllegalArgumentException(location+" must be an array with at most4096 entries");
            if(field.getType().getComponentType()==String.class) for(JsonElement element:input.getAsJsonArray())
                if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString() || element.getAsString().length()>256) throw new IllegalArgumentException(location+" needs non-null strings of at most256 characters");
            if(field.getName().equals("oreDimensionOverrides")) {
                Set<String> seen=new HashSet<>();
                for(JsonElement entry:input.getAsJsonArray()) {
                    String text=entry.getAsString();
                    if(!text.matches("[a-z0-9_.-]+:[a-z0-9/._-]+=(overworld|nether|end|none)") || !seen.add(text.substring(0,text.lastIndexOf('=')))) throw new IllegalArgumentException(location+" needs unique dimension IDs followed by=overworld/nether/end/none");
                }
            }
        }
    }
    public static boolean effect(String metal,String category) {return effects.getOrDefault(metal+":"+(category.equals("sword")?"weapon":category),true);}
    public static Object oreCategory(String metal) {
        StringBuilder name=new StringBuilder();boolean upper=false;for(char c:metal.toCharArray()) {if(c=='_') upper=true;else {name.append(upper?Character.toUpperCase(c):c);upper=false;}}
        if(metal.equals("ceruclase")) name=new StringBuilder("cerucalse");
        try {return WorldGenerationConfig.class.getField(name.toString()).get(null);}catch(ReflectiveOperationException error) {throw new IllegalArgumentException("No original ore config for "+metal,error);}
    }
    public static int oreInt(String metal,String key,int fallback) {try {return oreCategory(metal).getClass().getField(key).getInt(oreCategory(metal));}catch(ReflectiveOperationException error) {return fallback;}}
    public static List<String> oreBiomes(String metal) {try {return Arrays.stream((String[])oreCategory(metal).getClass().getField("biomes").get(oreCategory(metal))).map(ConfigFiles::biomeAlias).distinct().toList();}catch(ReflectiveOperationException error) {return List.of();}}
    private static String biomeAlias(String id) {return switch(id) {case "minecraft:beaches"->"minecraft:beach";case "minecraft:stone_beach"->"minecraft:stony_shore";case "minecraft:cold_beach"->"minecraft:snowy_beach";case "minecraft:jungle_hills"->"minecraft:jungle";case "minecraft:jungle_edge"->"minecraft:sparse_jungle";default->id;};}
    public static int rarity(int original) {return switch(original) {case 10->WorldGenerationConfig.rarity.commonRarity;case 7->WorldGenerationConfig.rarity.uncommonRarity;case 5->WorldGenerationConfig.rarity.rareRarity;case 3->WorldGenerationConfig.rarity.veryRareRarity;case 1->WorldGenerationConfig.rarity.ultraRareRarity;default->original;};}
    public static JsonArray materialStats(JsonArray defaults) throws IOException {
        if(!GeneralConfig.enableCustomMaterialStatsConfig || directory==null) return defaults;
        Path file=directory.resolve("materials.json");
        if(!Files.exists(file)) try(Writer writer=Files.newBufferedWriter(file,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW)) {GSON.toJson(defaults,writer);}
        JsonArray requested=read(file).getAsJsonArray();Map<String,JsonObject> known=new LinkedHashMap<>();
        for(JsonElement value:defaults) {
            JsonObject entry=value.getAsJsonObject().deepCopy();
            // JsonMaterialHelper accepts these optional extra attributes even if the
            // particular material's original JSON did not declare them.
            if(entry.has("tool_stats")) for(String key:List.of("attack_damage","attack_speed","max_health","movement_speed","reach_distance"))
                if(!entry.getAsJsonObject("tool_stats").has(key)) entry.getAsJsonObject("tool_stats").addProperty(key,0);
            if(entry.has("armor_stats")) for(String key:List.of("max_health","movement_speed","knockback_resistance"))
                if(!entry.getAsJsonObject("armor_stats").has(key)) entry.getAsJsonObject("armor_stats").addProperty(key,0);
            known.put(entry.get("name").getAsString(),entry);
        }
        Set<String> seen=new HashSet<>();
        for(JsonElement value:requested) {
            JsonObject entry=value.getAsJsonObject();String name=entry.get("name").getAsString();JsonObject original=known.get(name);
            if(original==null || !seen.add(name)) throw new IllegalArgumentException("Unknown/duplicate custom material "+name);
            boolean hadOre=original.get("ore_harvest_level").getAsInt()>=0;
            merge(original,entry);
            if(hadOre!=(original.get("ore_harvest_level").getAsInt()>=0)) throw new IllegalArgumentException(name+" cannot add/remove an ore registry ID through a stat override");
            validateMaterial(original,name);
        }
        JsonArray result=new JsonArray();known.values().forEach(result::add);return result;
    }
    private static void validateMaterial(JsonObject material,String name) {
        String color=material.get("color").getAsString();if(!color.matches("[0-9a-fA-F]{1,6}")) throw new IllegalArgumentException(name+" needs an RGB hexadecimal color");
        for(String key:List.of("hardness","blast_resistance","ore_harvest_level")) numeric(material.get(key),name+"."+key,key.equals("ore_harvest_level"));
        if(material.get("ore_harvest_level").getAsInt()<-1 || material.get("ore_harvest_level").getAsInt()>7) throw new IllegalArgumentException(name+" ore tier must be-1..7");
        for(String category:List.of("armor_stats","tool_stats")) if(material.has(category)) {
            JsonObject stats=material.getAsJsonObject(category);
            for(var entry:stats.entrySet()) {
                if(entry.getKey().equals("damage_reduction")) {
                    if(!entry.getValue().isJsonArray() || entry.getValue().getAsJsonArray().size()!=4) throw new IllegalArgumentException(name+" needs four armor defense values");
                    for(JsonElement defense:entry.getValue().getAsJsonArray()) {numeric(defense,name+" defense",true);if(defense.getAsInt()<0) throw new IllegalArgumentException(name+" defense cannot be negative");}
                } else {
                    String key=entry.getKey();boolean integer=List.of("durability","enchantability","harvest_level").contains(key);
                    numeric(entry.getValue(),name+"."+category+"."+key,integer);double amount=entry.getValue().getAsDouble();
                    if(key.equals("durability") && (amount<1 || amount>Integer.MAX_VALUE/16)) throw new IllegalArgumentException(name+" durability exceeds native limits");
                    if(key.equals("harvest_level") && (amount<0 || amount>7)) throw new IllegalArgumentException(name+" harvest tier must be0..7");
                    if(List.of("enchantability","efficiency","toughness").contains(key) && amount<0) throw new IllegalArgumentException(name+" "+key+" cannot be negative");
                }
            }
        }
    }
    private static void numeric(JsonElement value,String location,boolean integer) {
        if(!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber() || !Double.isFinite(value.getAsDouble()) || Math.abs(value.getAsDouble())>Float.MAX_VALUE) throw new IllegalArgumentException(location+" must be a finite native number");
        if(integer && (value.getAsBigDecimal().compareTo(java.math.BigDecimal.valueOf(value.getAsLong()))!=0 || value.getAsDouble()<Integer.MIN_VALUE || value.getAsDouble()>Integer.MAX_VALUE)) throw new IllegalArgumentException(location+" must be a native integer");
    }
    private static void merge(JsonObject into,JsonObject changes) {
        for(var entry:changes.entrySet()) {
            if(!into.has(entry.getKey())) throw new IllegalArgumentException("Unknown material stat "+entry.getKey());
            if(entry.getValue().isJsonObject()) merge(into.getAsJsonObject(entry.getKey()),entry.getValue().getAsJsonObject());
            else {if(entry.getValue().isJsonNull()) throw new IllegalArgumentException("Null material stat "+entry.getKey());into.add(entry.getKey(),entry.getValue().deepCopy());}
        }
    }
    private ConfigFiles() {}
}
