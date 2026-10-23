package it.hurts.metallurgy_reforged.compat;
import it.hurts.metallurgy_reforged.config.GeneralConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;
public final class OptionalPacks {
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {bus.addListener(OptionalPacks::packs);}
    private static void packs(AddPackFindersEvent event) {
        if(event.getPackType()!=PackType.SERVER_DATA) return;
        pack(event,"core_tiers");
        if(ModList.get().isLoaded("enderio")) pack(event,"enderio");
        if(GeneralConfig.tiCIntegration && ModList.get().isLoaded("tconstruct")) pack(event,"tconstruct");
        if(GeneralConfig.silentGemsIntegration && ModList.get().isLoaded("silentgear")) pack(event,"silentgear");
        if(GeneralConfig.tetraIntegration && ModList.get().isLoaded("tetra")) pack(event,"tetra");
        if(GeneralConfig.projectEIntegration && ModList.get().isLoaded("projecte")) pack(event,"projecte");
    }
    private static void pack(AddPackFindersEvent event,String dependency) {
        var folder=ModList.get().getModFileById("metallurgy").getFile().findResource("resourcepacks",dependency);
        event.addRepositorySource(consumer->{
            var pack=Pack.readMetaAndCreate("metallurgy/"+dependency,Component.literal("Metallurgy: "+dependency),true,id->new ConfiguredPack(new PathPackResources(id,folder,false),dependency),PackType.SERVER_DATA,Pack.Position.BOTTOM,PackSource.BUILT_IN);
            if(pack==null) throw new IllegalStateException("Invalid bundled Metallurgy data pack: "+dependency);
            consumer.accept(pack);
        });
    }
    private OptionalPacks() {}
}
