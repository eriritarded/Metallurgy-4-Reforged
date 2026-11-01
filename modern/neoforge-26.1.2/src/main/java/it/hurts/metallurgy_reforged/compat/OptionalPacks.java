package it.hurts.metallurgy_reforged.compat;
import it.hurts.metallurgy_reforged.config.GeneralConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.fml.ModList;
import java.util.Optional;
public final class OptionalPacks {
    public static void register(net.neoforged.bus.api.IEventBus bus) {bus.addListener(OptionalPacks::packs);}
    private static void packs(AddPackFindersEvent event) {
        if(event.getPackType()!=PackType.SERVER_DATA) return;
        pack(event,"core_tiers");
        if(ModList.get().isLoaded("enderio")) pack(event,"enderio");
        if(GeneralConfig.silentGemsIntegration && ModList.get().isLoaded("silentgear")) pack(event,"silentgear");
        if(GeneralConfig.tiCIntegration && ModList.get().isLoaded("hephaestus")) pack(event,"hephaestus");
        // Above Thaumaturge, because it replaces some of Thaumaturge's own recipes.
        if(GeneralConfig.thaumaturgeIntegration && ModList.get().isLoaded("thaumaturge")) pack(event,"thaumaturge",Pack.Position.TOP);

    }
    private static void pack(AddPackFindersEvent event,String dependency) {pack(event,dependency,Pack.Position.BOTTOM);}
    private static void pack(AddPackFindersEvent event,String dependency,Pack.Position position) {
        var info=ModList.get().getModContainerById("metallurgy").orElseThrow().getModInfo();
        Pack.ResourcesSupplier original=new net.neoforged.neoforge.resource.JarContentsPackResources.JarContentsResourcesSupplier(info.getOwningFile().getFile().getContents(),"resourcepacks/"+dependency);
        var resources=new Pack.ResourcesSupplier() {
            public PackResources openPrimary(PackLocationInfo location) {return new ConfiguredPack(original.openPrimary(location),dependency);}
            public PackResources openFull(PackLocationInfo location,Pack.Metadata metadata) {return new ConfiguredPack(original.openFull(location,metadata),dependency);}
        };
        // Configured resources differ from the jar bytes; never advertise an
        // unchanged KnownPack identity to skip synchronizing this server data.
        var pack=Pack.readMetaAndCreate(new PackLocationInfo("metallurgy/"+dependency,Component.literal("Metallurgy: "+dependency),PackSource.BUILT_IN,Optional.empty()),resources,PackType.SERVER_DATA,new PackSelectionConfig(true,position,false));
        if(pack==null) throw new IllegalStateException("Invalid bundled Metallurgy pack: "+dependency);
        event.addRepositorySource(consumer->consumer.accept(pack));
    }
    private OptionalPacks() {}
}
