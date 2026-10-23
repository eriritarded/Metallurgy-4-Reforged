package it.hurts.metallurgy_reforged.compat.spartan;

import com.oblivioussp.spartanweaponry.api.*;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import java.util.*;
import java.util.function.*;

/** API12 owns weapon behavior, balance traits, projectiles and reloadable stats. */
public final class SpartanIntegration {
    public record Entry(String id,Materials.Definition metal,Supplier<Item> item) {}
    public static final List<Entry> ENTRIES=new ArrayList<>();
    private static final Map<String,WeaponMaterial> MATERIALS=new LinkedHashMap<>();
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,"metallurgy");
    private static WeaponMaterial material(Materials.Definition m) {
        return MATERIALS.computeIfAbsent(m.name(),name->new WeaponMaterial(name,"metallurgy",m.color(),m.color(),
            (int)m.tool("durability",1),m.tool("efficiency",0),m.tool("damage",0),(int)m.tool("enchantability",0),
            TagKey.create(Registries.ITEM,new ResourceLocation("forge","ingots/"+name)),
            TagKey.create(WeaponTraits.REGISTRY_KEY,new ResourceLocation("metallurgy","materials/"+name))).setUseCustomDisplayName());
    }
    public static void register(IEventBus bus) {
        SpartanWeaponryAPI.assertAPIVersion("metallurgy",12);
        Map<String,Function<WeaponMaterial,Item>> factories=new LinkedHashMap<>();
        factories.put("dagger",SpartanWeaponryAPI::createDagger);factories.put("longsword",SpartanWeaponryAPI::createLongsword);
        factories.put("katana",SpartanWeaponryAPI::createKatana);factories.put("saber",SpartanWeaponryAPI::createSaber);
        factories.put("rapier",SpartanWeaponryAPI::createRapier);factories.put("greatsword",SpartanWeaponryAPI::createGreatsword);
        factories.put("hammer",SpartanWeaponryAPI::createBattleHammer);factories.put("warhammer",SpartanWeaponryAPI::createWarhammer);
        factories.put("spear",SpartanWeaponryAPI::createSpear);factories.put("halberd",SpartanWeaponryAPI::createHalberd);
        factories.put("pike",SpartanWeaponryAPI::createPike);factories.put("lance",SpartanWeaponryAPI::createLance);
        factories.put("throwing_knife",SpartanWeaponryAPI::createThrowingKnife);factories.put("throwing_axe",SpartanWeaponryAPI::createTomahawk);
        factories.put("javelin",SpartanWeaponryAPI::createJavelin);factories.put("boomerang",SpartanWeaponryAPI::createBoomerang);
        factories.put("battleaxe",SpartanWeaponryAPI::createBattleaxe);factories.put("mace",SpartanWeaponryAPI::createFlangedMace);
        factories.put("glaive",SpartanWeaponryAPI::createGlaive);factories.put("staff",SpartanWeaponryAPI::createQuarterstaff);
        factories.put("parrying_dagger",SpartanWeaponryAPI::createParryingDagger);factories.put("longbow",SpartanWeaponryAPI::createLongbow);
        factories.put("crossbow",SpartanWeaponryAPI::createHeavyCrossbow);
        for(var metal:Materials.ALL) if(metal.hasTools()) for(var factory:factories.entrySet()) {
            String id=factory.getKey()+"_"+metal.name();var item=ITEMS.register(id,()->factory.getValue().apply(material(metal)));
            ENTRIES.add(new Entry(id,metal,item));
        }
        ITEMS.register(bus);
        Content.TABS.register("spartanweaponry",()->CreativeModeTab.builder().title(Component.literal("Metallurgy: Spartan Weaponry"))
            .icon(()->new ItemStack(ENTRIES.get(0).item().get())).displayItems((parameters,output)->ENTRIES.forEach(entry->output.accept(entry.item().get()))).build());
        if(net.minecraftforge.fml.loading.FMLEnvironment.dist==net.minecraftforge.api.distmarker.Dist.CLIENT) SpartanClient.register(bus);
    }
    private SpartanIntegration() {}
}
