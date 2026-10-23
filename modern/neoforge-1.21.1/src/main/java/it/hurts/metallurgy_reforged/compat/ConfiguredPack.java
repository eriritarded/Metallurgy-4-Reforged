package it.hurts.metallurgy_reforged.compat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import java.io.*;
import java.util.*;
public final class ConfiguredPack implements PackResources {
    private final PackResources delegate;private final String module;
    private static final String TARGET="neoforge-1.21.1";
    private static final String ALLOYS="recipe/compat/enderio/";
    private final Map<String,byte[]> editable;
    public ConfiguredPack(PackResources delegate,String module) {
        this.delegate=delegate;this.module=module;
        if(module.equals("enderio") && it.hurts.metallurgy_reforged.config.GeneralConfig.enableEnderIOAlloyConfig) {
            Map<String,byte[]> defaults=new LinkedHashMap<>();
            delegate.listResources(PackType.SERVER_DATA,"metallurgy",ALLOYS,(id,source)->{
                try(var stream=source.get()) {defaults.put(id.getPath(),stream.readAllBytes());}catch(IOException error) {throw new UncheckedIOException(error);}
            });
            editable=EnderIoFiles.load(TARGET,ALLOYS,defaults);
        } else editable=null;
    }
    private boolean managed(PackType type,ResourceLocation id) {return editable!=null && type==PackType.SERVER_DATA && id.getNamespace().equals("metallurgy") && id.getPath().startsWith(ALLOYS);}
    private IoSupplier<InputStream> configure(ResourceLocation id,IoSupplier<InputStream> source) {
        if(source==null || !DynamicPackData.accept(module,id.getPath())) return null;
        return ()->{try(var input=source.get()) {return new ByteArrayInputStream(DynamicPackData.transform(module,id.getPath(),input.readAllBytes()));}catch(RuntimeException exception) {throw new IOException("Invalid configured Metallurgy pack resource "+id,exception);}};
    }
    @Override public IoSupplier<InputStream> getRootResource(String... path) {return delegate.getRootResource(path);}
    @Override public IoSupplier<InputStream> getResource(PackType type,ResourceLocation id) {
        if(managed(type,id)) {byte[] bytes=editable.get(id.getPath());return bytes==null?null:()->new ByteArrayInputStream(bytes);}
        return configure(id,delegate.getResource(type,id));
    }
    @Override public void listResources(PackType type,String namespace,String path,ResourceOutput output) {
        delegate.listResources(type,namespace,path,(id,source)->{if(managed(type,id)) return;var configured=configure(id,source);if(configured!=null) output.accept(id,configured);});
        String prefix=path.isEmpty() || path.endsWith("/")?path:path+"/";
        if(editable!=null && type==PackType.SERVER_DATA && namespace.equals("metallurgy")) for(var entry:editable.entrySet()) if(entry.getKey().startsWith(prefix)) output.accept(ResourceLocation.fromNamespaceAndPath(namespace,entry.getKey()),()->new ByteArrayInputStream(entry.getValue()));
    }
    @Override public Set<String> getNamespaces(PackType type) {return delegate.getNamespaces(type);}
    @Override public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {return delegate.getMetadataSection(serializer);}
    @Override public String packId() {return delegate.packId();}
    @Override public net.minecraft.server.packs.PackLocationInfo location() {return delegate.location();}
    @Override public void close() {delegate.close();}
}
