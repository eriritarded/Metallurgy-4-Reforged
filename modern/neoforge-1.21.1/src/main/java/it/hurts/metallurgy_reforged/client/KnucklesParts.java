package it.hurts.metallurgy_reforged.client;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
/** Source1.4.1 arm UVs, dimensions and pivots. Native ModelPart uses1/16 scale. */
public final class KnucklesParts {
    public final ModelPart arm,sleeve,knuckles;
    public KnucklesParts(boolean left,boolean slim) {
        MeshDefinition skin=new MeshDefinition();var root=skin.getRoot();int width=slim?3:4;
        root.addOrReplaceChild("arm",CubeListBuilder.create().texOffs(left?32:40,left?48:16).mirror(!left).addBox(0,0,0,width,12,4),PartPose.offset(0,-11,1));
        root.addOrReplaceChild("sleeve",CubeListBuilder.create().texOffs(left?48:40,left?48:32).addBox(0,0,0,width,12,4,new CubeDeformation(0.25F)),PartPose.offset(0,-11,1));
        var baked=LayerDefinition.create(skin,64,64).bakeRoot();arm=baked.getChild("arm");sleeve=baked.getChild("sleeve");
        MeshDefinition metal=new MeshDefinition();metal.getRoot().addOrReplaceChild("knuckles",CubeListBuilder.create().texOffs(0,0).addBox(0,0,0,1,2,5),PartPose.offset(1,0,0.5F));
        knuckles=LayerDefinition.create(metal,16,16).bakeRoot().getChild("knuckles");
    }
}
