"""Exact native built-in pack factories, preserving loader jar resources on26."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def generate(target):
    latest=target.endswith('26.1.2');key='Identifier' if latest else 'ResourceLocation'
    supplier='new net.neoforged.neoforge.resource.JarContentsPackResources.JarContentsResourcesSupplier(info.getOwningFile().getFile().getContents(),"resourcepacks/"+dependency)' if latest else 'BuiltInPackSource.fromName(location->new PathPackResources(location,info.getOwningFile().getFile().findResource("resourcepacks",dependency)))'
    text='''package it.hurts.metallurgy_reforged.compat;
import it.hurts.metallurgy_reforged.config.GeneralConfig;
import net.minecraft.resources.ResourceLocation;
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
HEPHAESTUS
PROJECTE
    }
    private static void pack(AddPackFindersEvent event,String dependency) {
        var info=ModList.get().getModContainerById("metallurgy").orElseThrow().getModInfo();
        Pack.ResourcesSupplier original=SUPPLIER;
        var resources=new Pack.ResourcesSupplier() {
            public PackResources openPrimary(PackLocationInfo location) {return new ConfiguredPack(original.openPrimary(location),dependency);}
            public PackResources openFull(PackLocationInfo location,Pack.Metadata metadata) {return new ConfiguredPack(original.openFull(location,metadata),dependency);}
        };
        // Configured resources differ from the jar bytes; never advertise an
        // unchanged KnownPack identity to skip synchronizing this server data.
        var pack=Pack.readMetaAndCreate(new PackLocationInfo("metallurgy/"+dependency,Component.literal("Metallurgy: "+dependency),PackSource.BUILT_IN,Optional.empty()),resources,PackType.SERVER_DATA,new PackSelectionConfig(true,Pack.Position.BOTTOM,false));
        if(pack==null) throw new IllegalStateException("Invalid bundled Metallurgy pack: "+dependency);
        event.addRepositorySource(consumer->consumer.accept(pack));
    }
    private OptionalPacks() {}
}
'''.replace('SUPPLIER',supplier).replace('PROJECTE','' if latest else '        if(GeneralConfig.projectEIntegration && ModList.get().isLoaded("projecte")) pack(event,"projecte");').replace('HEPHAESTUS\n','        if(GeneralConfig.tiCIntegration && ModList.get().isLoaded("hephaestus")) pack(event,"hephaestus");\n' if latest else '').replace('ResourceLocation',key)
    path=ROOT/target/'src/main/java/it/hurts/metallurgy_reforged/compat/OptionalPacks.java';path.write_text(text,encoding='utf-8')

