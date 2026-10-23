"""Native26.1.2 adapter, using the inspected NeoForge26.1.2.122 sources."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'neoforge-1.21.1/src/main/java'
DEST = ROOT / 'neoforge-26.1.2/src/main/java'
PACKAGE = 'it/hurts/metallurgy_reforged/'


def main():
    for path in SOURCE.rglob('*.java'):
        relative = path.relative_to(SOURCE)
        name = relative.as_posix().removeprefix(PACKAGE)
        if name in ['inventory/MetalChestItem.java', 'client/ChestItemRenderer.java','client/GadgetProperties.java','client/GadgetRenderTypes.java','client/GadgetOverlay.java','client/KnucklesModel.java','client/KnucklesRenderer.java','client/GauntletHandRenderer.java','recipe/ProcessingRecipe.java']:
            continue
        t = path.read_text(encoding='utf-8').replace('ResourceLocation', 'Identifier')
        if name == 'Metallurgy.java':
            t = t.replace('Content.register(bus);', 'Content.register(bus);\n        if(net.neoforged.fml.ModList.get().isLoaded("hephaestus_tools"))\n            it.hurts.metallurgy_reforged.compat.hephaestus.HephaestusTraits.register(bus);')
        if name == 'content/Content.java':
            continue
        if name == 'compat/JeiCrafting.java':
            start=t.index('    private static boolean rule(');end=t.index('    public static void subtypes(',start)
            t=t[:start]+'    private static boolean rule(String kind) {return !it.hurts.metallurgy_reforged.client.ProcessingClient.craftingRule(kind).isEmpty();}\n'+t[end:]
            t=re.sub(r'^            @Override public String getLegacyStringSubtypeInfo[^\n]*\n','',t,flags=re.M)
            t=t.replace('builder.define(symbol,Ingredient.of(stack))','builder.define(symbol,Ingredient.of(stack.getItem()),display(stack))')
            t=t.replace('    public static void recipes(', '    private static net.minecraft.world.item.crafting.display.SlotDisplay display(ItemStack stack) {return new net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(stack));}\n    public static void recipes(')
            t=t.replace('CraftingBookCategory.MISC,List.of(programmed)', 'CraftingBookCategory.MISC,display(programmed)').replace('CraftingBookCategory.MISC,List.of(clean)', 'CraftingBookCategory.MISC,display(clean)').replace('CraftingBookCategory.MISC,List.of(output)', 'CraftingBookCategory.MISC,display(output)')
            t=t.replace('new RecipeHolder<>(Identifier.fromNamespaceAndPath("metallurgy","jei_examples/"+name),builder.build())','new RecipeHolder<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,Identifier.fromNamespaceAndPath("metallurgy","jei_examples/"+name)),builder.build())')
        elif name == 'compat/ConfiguredPack.java':
            t=t.replace('MetadataSectionSerializer','MetadataSectionType').replace('"neoforge-1.21.1"','"neoforge-26.1.2"')
        elif name == 'compat/JeiPlugin.java':
            t=t.replace('Arrays.stream(d.ingredients().get(slot).getItems())', 'd.ingredients().get(slot).items().map(holder->new ItemStack(holder.value()))')
            t=t.replace('GuiGraphics','GuiGraphicsExtractor').replace('graphics.drawString(', 'graphics.text(')
            t=t.replace('BuiltInRegistries.MOB_EFFECT.get(', 'BuiltInRegistries.MOB_EFFECT.getValue(')
            t=t.replace('getRegistryName(View recipe)', 'getIdentifier(View recipe)')
        elif name == 'machine/GenericCrushing.java':
            t=t.replace('input.getTags()', 'input.typeHolder().tags()').replace('output.getTags()', 'output.typeHolder().tags()')
        elif name == 'client/GadgetTooltips.java':
            t=t.replace('component.copyTag()', 'component.copyTagWithoutId()')
            t=t.replace('var data=BlockItem.getBlockEntityData(stack);if(data==null) return;', 'var component=stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);if(component==null) return;var data=component.copyTagWithoutId();')
            t=t.replace('data.getString("effect")', 'data.getStringOr("effect","")').replace('data.getInt("active_time")','data.getIntOr("active_time",0)').replace('BuiltInRegistries.MOB_EFFECT.get(', 'BuiltInRegistries.MOB_EFFECT.getValue(')
        elif name == 'client/DetectorDisplay.java':
            t=t.replace('BuiltInRegistries.CREATIVE_MODE_TAB.get(', 'BuiltInRegistries.CREATIVE_MODE_TAB.getValue(')
        elif name == 'client/ChamberParticles.java':
            t=t.replace('BuiltInRegistries.MOB_EFFECT.get(', 'BuiltInRegistries.MOB_EFFECT.getValue(')
        elif name == 'gadget/PotashFertilizerItem.java':
            t=t.replace('return InteractionResult.sidedSuccess(world.isClientSide);', 'return InteractionResult.SUCCESS;')
            t=t.replace('server.sendParticles(net.minecraft.core.particles.ParticleTypes.DRIPPING_LAVA,','server.sendParticles(net.minecraft.core.particles.ParticleTypes.DRIPPING_LAVA,false,false,')
        elif name == 'advancement/Advancements.java':
            t=t.replace('net.minecraft.advancements.critereon.', 'net.minecraft.advancements.criterion.')
        elif name == 'mixin/CorpseRenderMixin.java':
            t='''package it.hurts.metallurgy_reforged.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.metallurgy_reforged.client.CorpseRenderData;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntityRenderer.class)
public abstract class CorpseRenderMixin {
    @Unique private final java.util.Map<LivingEntityRenderState,Boolean> metallurgy$corpses=new java.util.WeakHashMap<>();
    @Shadow protected abstract int getModelTint(LivingEntityRenderState state);
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",at=@At("TAIL"))
    private void metallurgy$extract(LivingEntity entity,LivingEntityRenderState state,float partial,CallbackInfo callback) {metallurgy$corpses.put(state,CorpseRenderData.active(entity));}
    @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At("HEAD"))
    private void metallurgy$shake(LivingEntityRenderState state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera,CallbackInfo callback) {if(Boolean.TRUE.equals(metallurgy$corpses.get(state))) pose.translate(CorpseRenderData.jitter(),0,CorpseRenderData.jitter());}
    @Redirect(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getModelTint(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)I"))
    private int metallurgy$tint(LivingEntityRenderer<?,?,?> renderer,LivingEntityRenderState state) {int original=getModelTint(state);return Boolean.TRUE.equals(metallurgy$corpses.get(state))?CorpseRenderData.tint(original):original;}
}
'''
        elif name == 'client/OreParticle.java':
            t=t.replace('extends TextureSheetParticle','extends SingleQuadParticle').replace('super(level,x,y,z,0,0,0)', 'super(level,x,y,z,0,0,0,frame(0))')
            # AtlasManager indexes logical atlas IDs; TextureAtlas.LOCATION_BLOCKS is a GPU texture ID.
            t=t.replace('Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(', 'Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).getSprite(')
            t=t.replace('public ParticleRenderType getRenderType() {return ParticleRenderType.TERRAIN_SHEET;}', 'protected SingleQuadParticle.Layer getLayer() {return SingleQuadParticle.Layer.OPAQUE_TERRAIN;}')
        elif name == 'client/OreParticles.java':
            t=t.replace('.isSolidRender(world,pos.relative(direction))', '.isSolidRender()')
        elif name == 'client/ItemVisuals.java':
            t=t.replace('Screen.hasShiftDown()', 'com.mojang.blaze3d.platform.InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow(),org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)').replace('Screen.hasControlDown()', 'com.mojang.blaze3d.platform.InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow(),org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL)')
            start=t.index('    @SubscribeEvent public static void color(');end=t.index('    private ItemVisuals()',start)
            t=t[:start]+'''    @SubscribeEvent public static void color(RenderTooltipEvent.Texture event) {
        var material=MaterialItems.material(event.getItemStack());TooltipTint.color(0xffffffff);
        if(material!=null && GeneralConfig.metalColoredTooltipBorders) {TooltipTint.color(0xff000000|material.color());event.setTexture(net.minecraft.resources.Identifier.fromNamespaceAndPath("metallurgy","metal"));}
    }
'''+t[end:]
        elif name == 'client/ChamberRenderer.java':
            t='''package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.machine.ChamberEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
public final class ChamberRenderer implements BlockEntityRenderer<ChamberEntity,ChamberRenderer.State> {
    private final ItemModelResolver resolver;
    public ChamberRenderer(BlockEntityRendererProvider.Context context) {resolver=context.itemModelResolver();}
    public static final class State extends BlockEntityRenderState {final ItemStackRenderState item=new ItemStackRenderState();}
    @Override public State createRenderState() {return new State();}
    @Override public void extractRenderState(ChamberEntity chamber,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay breaking) {BlockEntityRenderer.super.extractRenderState(chamber,state,partial,camera,breaking);resolver.updateForTopItem(state.item,chamber.getItem(0),ItemDisplayContext.GROUND,chamber.getLevel(),null,0);}
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {if(state.item.isEmpty()) return;pose.pushPose();pose.translate(0.5,0.25,0.5);pose.scale(1.7F,1.7F,1.7F);state.item.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
}
'''
        elif name == 'client/HudRenderer.java':
            t=t.replace('BuiltInRegistries.MOB_EFFECT.get(', 'BuiltInRegistries.MOB_EFFECT.getValue(')
            t=re.sub(r'\bGuiGraphics\b','GuiGraphicsExtractor',t).replace('graphics.drawString(', 'graphics.text(').replace('graphics.renderItem(', 'graphics.item(')
            t=t.replace('graphics.blit(texture,x,y,u,v,width,height,textureWidth,textureHeight)', 'graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,texture,x,y,u,v,width,height,textureWidth,textureHeight)')
            start=t.index('    private static void effect(');end=t.index('    private static void blit(',start)
            t=t[:start]+'''    private static void effect(GuiGraphicsExtractor graphics,String name,int x,int y) {var key=it.hurts.metallurgy_reforged.effect.VersionBridge.effectKey(name);graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,Identifier.fromNamespaceAndPath(key.getNamespace(),"mob_effect/"+key.getPath()),x,y,18,18);}
'''+t[end:]
        elif name == 'world/RetrogenEvents.java':
            t=t.replace('.serialize(Codec.BOOL)', '.serialize(Codec.BOOL.fieldOf("generated"))').replace('chunk.setUnsaved(true)', 'chunk.markUnsaved()')
        elif name == 'fluid/MetalBucketItem.java':
            t=t.replace('net.minecraft.world.entity.player.Player','net.minecraft.world.entity.LivingEntity').replace('Player player','LivingEntity player')
        elif name == 'gadget/GadgetItem.java':
            start=t.index('    @Override public void initializeClient(');end=t.index('    @Override public InteractionResultHolder',start);t=t[:start]+t[end:]
            t=t.replace('onEntitySwing(ItemStack stack,LivingEntity entity)', 'onEntitySwing(ItemStack stack,LivingEntity entity,InteractionHand hand)')
            t=t.replace('InteractionResultHolder<ItemStack> use(', 'InteractionResult use(')
            t=t.replace('InteractionResultHolder.success(player.getItemInHand(hand))','InteractionResult.SUCCESS').replace('InteractionResultHolder.pass(player.getItemInHand(hand))','InteractionResult.PASS')
            t=t.replace('boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker) { GadgetLogic.hit(stack,target,attacker);return true; }','void hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker) { GadgetLogic.hit(stack,target,attacker); }')
            t=t.replace('    @Override public int getEnchantmentValue() { return id.equals("brass_knuckles") || id.equals("rubracium_gauntlet")?1:0; }\n','')
        elif name == 'gadget/GadgetBridge.java':
            t=t.replace('player.displayClientMessage(Component.translatable(key),true)', 'player.sendOverlayMessage(Component.translatable(key))')
            t=t.replace('player.displayClientMessage(message,true)', 'player.sendOverlayMessage(message)')
            t=t.replace('getCompound("metallurgy_effects")','getCompoundOrEmpty("metallurgy_effects")')
            t=t.replace('stack.getTags()', 'stack.typeHolder().tags()')
            t=t.replace('player.displayClientMessage(Component.translatable("effect.metallurgy.punch_effect_tired"),true)', 'player.sendOverlayMessage(Component.translatable("effect.metallurgy.punch_effect_tired"))')
            t=t.replace('new ClickEvent(ClickEvent.Action.OPEN_URL,url)', 'new ClickEvent.OpenUrl(java.net.URI.create(url))')
            t=t.replace('new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(', 'new net.neoforged.neoforge.event.level.block.BreakBlockEvent(')
            t=t.replace('net.minecraft.world.item.UseAnim.','net.minecraft.world.item.ItemUseAnimation.')
            t=t.replace('level.sendParticles(player,net.minecraft.core.particles.ParticleTypes.EXPLOSION,true,','level.sendParticles(player,net.minecraft.core.particles.ParticleTypes.EXPLOSION,true,false,')
        elif name == 'gadget/MetallurgyShieldItem.java':
            t=t.replace('super(properties);this.id=id;', 'super(properties.delayedComponent(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS,MetallurgyShieldItem::blockingComponent).component(net.minecraft.core.component.DataComponents.USE_EFFECTS,id.endsWith("_buckler")?new net.minecraft.world.item.component.UseEffects(true,true,1):net.minecraft.world.item.component.UseEffects.DEFAULT));this.id=id;')
            t=t.replace('    private final String id;', '''    private final String id;
    private static net.minecraft.world.item.component.BlocksAttacks blockingComponent(net.minecraft.core.HolderLookup.Provider registries) {
        return new net.minecraft.world.item.component.BlocksAttacks(0.25F,1F,
                java.util.List.of(new net.minecraft.world.item.component.BlocksAttacks.DamageReduction(90F,java.util.Optional.empty(),0F,1F)),
                new net.minecraft.world.item.component.BlocksAttacks.ItemDamageFunction(3F,1F,1F),
                java.util.Optional.of(registries.getOrThrow(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)),
                java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK),java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BREAK));
    }''')
            t=t.replace('InteractionResultHolder<ItemStack> use(', 'InteractionResult use(').replace('InteractionResultHolder.consume(stack)', 'InteractionResult.CONSUME')
            t=t.replace('void releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining) { ShieldLogic.end(entity,stack,remaining); }','boolean releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining) { ShieldLogic.end(entity,stack,remaining);return true; }')
            t=t.replace('    @Override public int getEnchantmentValue() { return switch(id) {case "lemurite_shield"->17;case "ceruclase_shield"->18;case "zinc_buckler"->8;case "vulcanite_buckler"->20;default->22;}; }\n','')
            t=t.replace('    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack ingredient) { return false; }\n','')
        elif name in ['gadget/RoadBlock.java','gadget/PhosphorusLampBlock.java']:
            t=t.replace('DirectionProperty FACING','EnumProperty<Direction> FACING')
            t=t.replace('updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos) {level.scheduleTick(pos,this,1);return state;}','updateShape(BlockState state,LevelReader level,net.minecraft.world.level.ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos neighborPos,BlockState neighbor,RandomSource random) {ticks.scheduleTick(pos,this,1);return state;}')
        elif name == 'gadget/LighterItem.java':
            t=t.replace('var state=level.getBlockState(clicked);state.onCaughtFire(level,clicked,context.getClickedFace(),player);\n                level.removeBlock(clicked,false);GadgetBridge.damage', 'var state=level.getBlockState(clicked);if(!state.onCaughtFire(level,clicked,context.getClickedFace(),player)) return InteractionResult.FAIL;\n                level.removeBlock(clicked,false);GadgetBridge.damage')
        elif name == 'client/GadgetVisuals.java':
            t=t.replace('hidden(RenderPlayerEvent.Pre event) {if(ShieldLogic.invisible(event.getEntity())) event.setCanceled(true);}', 'hidden(RenderPlayerEvent.Pre<?> event) {var level=Minecraft.getInstance().level;if(level!=null && level.getEntity(event.getRenderState().id) instanceof net.minecraft.world.entity.LivingEntity entity && ShieldLogic.invisible(entity)) event.setCanceled(true);}')
            start=t.index('    @SubscribeEvent public static void movement(');end=t.index('    @SubscribeEvent public static void attack',start)
            t=t[:start]+'''    @SubscribeEvent public static void movement(MovementInputUpdateEvent event) {it.hurts.metallurgy_reforged.gadget.RoadMovement.input(event.getEntity(),event.getInput().getMoveVector().lengthSquared()>0);}
'''+t[end:]
            t=t.replace('    @SubscribeEvent public static void attack(InputEvent event) {', '''    // InputEvent is an abstract grouping type and cannot be registered with the event bus.
    // Mouse Post and Key run after native key mappings have received the physical input.
    @SubscribeEvent public static void attackMouse(InputEvent.MouseButton.Post event) { attack(); }
    @SubscribeEvent public static void attackKey(InputEvent.Key event) { attack(); }
    private static void attack() {''')
        elif name == 'fluid/FluidContent.java':
            t=t.replace('BlockBehaviour.Properties.of()', 'Content.blockProperties(definition.block())')
            t=t.replace('new Item.Properties()', 'Content.properties(id+"_bucket")')
            t=t.replace('.noCollission()', '.noCollision()')
            t=t.replace('level.dimensionType().ultraWarm()', 'level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES,pos)')
        elif name == 'fluid/MetallurgyLiquidBlock.java':
            t=t.replace('BlockPos pos,Entity entity)', 'BlockPos pos,Entity entity,net.minecraft.world.entity.InsideBlockEffectApplier effects,boolean precise)')
        elif name == 'client/FluidRendering.java':
            t=t.replace(',bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD','')
            t=t.replace('    private FluidRendering() {}','''    @net.neoforged.bus.api.SubscribeEvent
    public static void models(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) {
        for(var entry:it.hurts.metallurgy_reforged.fluid.FluidContent.ENTRIES.values()) {
            var still=new net.minecraft.client.resources.model.sprite.Material(Identifier.fromNamespaceAndPath("metallurgy","blocks/molten_metal_still"));
            var flow=new net.minecraft.client.resources.model.sprite.Material(Identifier.fromNamespaceAndPath("metallurgy","blocks/molten_metal_flow"));
            event.register(new net.minecraft.client.renderer.block.FluidModel.Unbaked(still,flow,null,net.neoforged.neoforge.client.fluid.FluidTintSources.constant(entry.definition.color())),entry.still,entry.flowing);
        }
    }
    private FluidRendering() {}''')
        elif name == 'effect/ToolBridge.java':
            t=t.replace('BlockEvent.BreakEvent(', 'net.neoforged.neoforge.event.level.block.BreakBlockEvent(')
            t=t.replace('stack.getTags()', 'stack.typeHolder().tags()').replace('state.getTags()', 'state.typeHolder().tags()')
            t=t.replace('instanceof BushBlock','instanceof VegetationBlock')
        elif name == 'mixin/BlockLootMixin.java':
            t=t.replace('Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;', 'Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;')
            t=t.replace('Entity breaker,ItemStack tool)', 'Entity breaker,net.minecraft.world.item.ItemInstance instance)')
            t=t.replace('        return ToolEffects.drops', '        ItemStack tool=instance instanceof ItemStack stack?stack:ItemStack.EMPTY;\n        return ToolEffects.drops')
        elif name == 'entity/PierKnight.java':
            t=t.replace('doHurtTarget(Entity target)', 'doHurtTarget(ServerLevel level,Entity target)').replace('knight.moveTo(', 'knight.snapTo(')
            start=t.index('    @Override public void addAdditionalSaveData(');end=t.index('    private static final class OwnerTarget',start)
            t=t[:start]+'''    @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);if(owner!=null) output.putString("OwnerUUID",owner.toString());output.putInt("PierThickness",thickness());output.putInt("PierLifespan",remaining);output.putBoolean("WidePutin",wide());output.putInt("VanishTime",vanish());
    }
    @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);try { owner=UUID.fromString(input.getStringOr("OwnerUUID","")); }catch(IllegalArgumentException ignored) { owner=null; }
        thickness(input.getIntOr("PierThickness",1));remaining=input.getIntOr("PierLifespan",20);entityData.set(WIDE,input.getBooleanOr("WidePutin",false));entityData.set(VANISH,Math.max(0,Math.min(40,input.getIntOr("VanishTime",0))));
    }
'''+t[end:]
        elif name == 'entity/VulcanitePrimedTnt.java':
            start=t.index('    @Override protected void addAdditionalSaveData(');end=t.index('    @Override public float getBlockExplosionResistance(',start)
            t=t[:start]+'''    @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {super.addAdditionalSaveData(output);output.putInt("tnt_type",0);if(ownerId!=null) output.putString("metallurgy_owner",ownerId.toString());}
    @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {super.readAdditionalSaveData(input);try {ownerId=UUID.fromString(input.getStringOr("metallurgy_owner",""));}catch(IllegalArgumentException ignored) {ownerId=null;}}
'''+t[end:]
            t=t.replace('getEntityDamageAmount(Explosion explosion,Entity entity)', 'getEntityDamageAmount(Explosion explosion,Entity entity,float exposure)').replace('super.getEntityDamageAmount(explosion,entity)', 'super.getEntityDamageAmount(explosion,entity,exposure)')
            t=t.replace('        var calculator=', '        if(!(level() instanceof ServerLevel server) || !server.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES)) return;\n        var calculator=')
        elif name == 'gadget/VulcaniteTntBlock.java':
            t=t.replace('wasExploded(Level level,','wasExploded(ServerLevel level,')
            t=t.replace('void onCaughtFire(', 'boolean onCaughtFire(').replace('if(!(level instanceof ServerLevel)) return;', 'if(!(level instanceof ServerLevel server) || !server.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES)) return false;',1)
            t=t.replace('level.gameEvent(owner,GameEvent.PRIME_FUSE,pos);', 'level.gameEvent(owner,GameEvent.PRIME_FUSE,pos);return true;')
            t=t.replace('if(!(level instanceof ServerLevel)) return;', 'if(!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES)) return;')
        elif name == 'client/PierKnightRenderer.java':
            t='''package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.entity.PierKnight;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
public final class PierKnightRenderer extends HumanoidMobRenderer<PierKnight,AvatarRenderState,PlayerModel> {
    private static final Identifier SKIN=Identifier.fromNamespaceAndPath("metallurgy","textures/models/pierknight_skin.png");
    private static final class KnightState extends AvatarRenderState { boolean wide;float fade; }
    public PierKnightRenderer(EntityRendererProvider.Context context) { super(context,new PlayerModel(context.bakeLayer(ModelLayers.PLAYER),false),0.5F); }
    @Override public AvatarRenderState createRenderState() { return new KnightState(); }
    @Override public void extractRenderState(PierKnight knight,AvatarRenderState state,float partial) {
        super.extractRenderState(knight,state,partial);var own=(KnightState)state;own.wide=knight.wide();own.fade=Math.max(0,Math.min(1,1-(partial+(knight.deathTime>0?knight.deathTime:knight.vanish()))/40F));
    }
    @Override public Identifier getTextureLocation(AvatarRenderState state) { return SKIN; }
    @Override protected int getModelTint(AvatarRenderState state) { return ((int)(((KnightState)state).fade*255)<<24)|0xffffff; }
    @Override protected float getFlipDegrees() { return 0; }
    @Override protected void scale(AvatarRenderState state,PoseStack pose) { if(((KnightState)state).wide) pose.scale(3,1,1); }
}
'''
        elif name == 'mixin/LivingAccess.java':
            t=t.replace('metallurgy$loot(DamageSource source,boolean playerKilled)', 'metallurgy$loot(net.minecraft.server.level.ServerLevel level,DamageSource source,boolean playerKilled)')
        elif name == 'network/ArmorNetwork.java':
            t=t.replace('PacketDistributor.sendToServer(', 'net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(')
        elif name == 'client/ArmorInput.java':
            t=t.replace('@EventBusSubscriber(modid="metallurgy",value=Dist.CLIENT)', '@EventBusSubscriber(modid="metallurgy",value=Dist.CLIENT)')
        elif name == 'client/ArmorKeys.java':
            t=t.replace(',bus=EventBusSubscriber.Bus.MOD','')
            t=t.replace('    public static final KeyMapping UP=', '    private static final KeyMapping.Category CATEGORY=new KeyMapping.Category(Identifier.fromNamespaceAndPath("metallurgy","armor"));\n    public static final KeyMapping UP=')
            t=t.replace('import net.minecraft.client.KeyMapping;', 'import net.minecraft.client.KeyMapping;\nimport net.minecraft.resources.Identifier;')
            t=t.replace('"key.categories.metallurgy"','CATEGORY').replace('{ event.register(UP);event.register(DOWN); }','{ event.registerCategory(CATEGORY);event.register(UP);event.register(DOWN); }')
        elif name == 'Metallurgy.java':
            t = t.replace('import net.neoforged.neoforge.items.wrapper.InvWrapper;', 'import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;')
            t = t.replace('        it.hurts.metallurgy_reforged.compat.ExternalHeat.register(event);', '''        it.hurts.metallurgy_reforged.compat.ExternalHeat.register(event);
        // NeoForge's fallback only covers exact BucketItem instances, not our placement-gated subclass.
        // FluidContentsTint and native fluid transfer both read the contained fluid through this capability.
        for (var entry:it.hurts.metallurgy_reforged.fluid.FluidContent.ENTRIES.values()) {
            event.registerItem(Capabilities.Fluid.ITEM,
                (stack,access)->new net.neoforged.neoforge.transfer.fluid.BucketResourceHandler(access),entry.bucket.get());
        }''')
            t = t.replace('Capabilities.ItemHandler.BLOCK', 'Capabilities.Item.BLOCK').replace('new InvWrapper(chest)', 'VanillaContainerWrapper.of(chest)')
            t = t.replace('side==null?new InvWrapper(machine):new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(machine,side)', 'new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(machine,side)')
        elif name == 'inventory/MetalChestBlock.java':
            t = t.replace('DirectionProperty FACING', 'EnumProperty<Direction> FACING')
            t = t.replace('RenderShape.ENTITYBLOCK_ANIMATED', 'RenderShape.MODEL')
            t = t.replace('getAnalogOutputSignal(BlockState state, Level level, BlockPos pos)', 'getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction)')
            start = t.index('    @Override public void onRemove(')
            end = t.index('    @Override public boolean hasAnalogOutputSignal', start)
            t = t[:start] + '''    @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel level,BlockPos pos,boolean moved) {
        // The native BlockEntity.preRemoveSideEffects already drops Container
        // contents. Dropping here as well would duplicate the inventory.
        Containers.updateNeighboursAfterDestroy(state,level,pos);
    }
''' + t[end:]
            t = t.replace('InteractionResult.sidedSuccess(level.isClientSide)', 'InteractionResult.SUCCESS')
            t = t.replace('level.isClientSide', 'level.isClientSide()')
        elif name == 'inventory/MetalChestEntity.java':
            t = t.replace('level.random.', 'level.getRandom().')
            t = t.replace('extends RandomizableContainerBlockEntity {', 'extends RandomizableContainerBlockEntity implements net.minecraft.world.level.block.entity.LidBlockEntity {')
            t = t.replace('protected boolean isOwnContainer(', 'public boolean isOwnContainer(')
            start = t.index('    @Override protected void loadAdditional(')
            end = t.index('    @Override public void startOpen', start)
            t = t[:start] + '''    @Override protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);items=NonNullList.withSize(getContainerSize(),ItemStack.EMPTY);
        if(!tryLoadLootTable(input)) ContainerHelper.loadAllItems(input,items);
    }
    @Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);if(!trySaveLootTable(output)) ContainerHelper.saveAllItems(output,items);
    }
''' + t[end:]
            t = t.replace('startOpen(Player player) { if(!isRemoved() && !player.isSpectator()) openers.incrementOpeners(player,level,worldPosition,getBlockState()); }', 'startOpen(net.minecraft.world.entity.ContainerUser user) { if(!isRemoved() && !user.getLivingEntity().isSpectator()) openers.incrementOpeners(user.getLivingEntity(),level,worldPosition,getBlockState(),user.getContainerInteractionRange()); }')
            t = t.replace('stopOpen(Player player) { if(!isRemoved() && !player.isSpectator()) openers.decrementOpeners(player,level,worldPosition,getBlockState()); }', 'stopOpen(net.minecraft.world.entity.ContainerUser user) { if(!isRemoved() && !user.getLivingEntity().isSpectator()) openers.decrementOpeners(user.getLivingEntity(),level,worldPosition,getBlockState()); }')
            t = t.replace('public float openness(float partial)', '@Override public float getOpenNess(float partial)')
        elif name == 'effect/EffectEvents.java':
            t = t.replace('.level().isClientSide)', '.level().isClientSide())')
            t = t.replace('weapon.getItem() instanceof SwordItem || weapon.getItem() instanceof AxeItem', 'id.endsWith("_sword") || id.endsWith("_axe")')
            t = t.replace('.getString("paragon")', '.getStringOr("paragon","")')
            t=t.replace('net.neoforged.neoforge.event.level.BlockEvent.BreakEvent', 'net.neoforged.neoforge.event.level.block.BreakBlockEvent')
        elif name == 'effect/VersionBridge.java':
            t=t.replace('type.natural(),type.ultraWarm()', 'type.hasSkyLight() && type.skybox()==net.minecraft.world.level.dimension.DimensionType.Skybox.OVERWORLD,type.attributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES,net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES.defaultValue())')
            t=t.replace('level.dimension().location()', 'level.dimension().identifier()')
            t=t.replace('.metallurgy$loot(source,true)', '.metallurgy$loot(level,source,true)')
            t=t.replace('level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT)', 'level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS)')
            t=t.replace('net.minecraft.world.item.UseAnim.DRINK','net.minecraft.world.item.ItemUseAnimation.DRINK')
            t=t.replace('BuiltInRegistries.BLOCK.get(id)', 'BuiltInRegistries.BLOCK.getValue(id)').replace('key.location()', 'key.identifier()')
            t=t.replace('new net.minecraft.world.item.component.Unbreakable(true)', 'net.minecraft.util.Unit.INSTANCE')
            t=t.replace('return player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);', 'return player.level() instanceof net.minecraft.server.level.ServerLevel level && level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY);')
            t = t.replace('entity.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD)', 'entity.getType().builtInRegistryHolder().is(net.minecraft.tags.EntityTypeTags.UNDEAD)')
            t = t.replace('entity.level().isDay()', 'entity.level().isBrightOutside()')
            t = t.replace('level.getRecipeManager()', 'level.recipeAccess()')
            t = t.replace('recipe.value().assemble(inventory,level.registryAccess())', 'recipe.value().assemble(inventory)')
            t = t.replace('!net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(player.level(),pos,player.level().getBlockState(pos),player)).isCanceled()', '!net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(player.level(),server.gameMode.getGameModeForPlayer(),server,pos,player.level().getBlockState(pos)).isCanceled()')
            t = t.replace('import net.minecraft.world.item.ArmorItem;', 'import net.minecraft.core.component.DataComponents;')
            t = t.replace('stack.getItem() instanceof ArmorItem', 'stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).modifiers().stream().anyMatch(entry->entry.attribute().equals(Attributes.ARMOR))')
            t = t.replace('return target.hurt(source,amount);', 'return target.level() instanceof net.minecraft.server.level.ServerLevel level && target.hurtServer(level,source,amount);')
            t = t.replace('root.getCompound("metallurgy_effects")', 'root.getCompoundOrEmpty("metallurgy_effects")')
            t = t.replace('BuiltInRegistries.MOB_EFFECT.getHolder(', 'BuiltInRegistries.MOB_EFFECT.get(')
            t = t.replace('BuiltInRegistries.ATTRIBUTE.getHolder(', 'BuiltInRegistries.ATTRIBUTE.get(')
            t = t.replace('"generic."+attribute', 'attribute')
            t = t.replace('entity.setSecondsOnFire(seconds)', 'entity.igniteForSeconds(seconds)')
            t = t.replace('entity.spawnAtLocation(stack,entity.getBbHeight()/2)', 'entity.spawnAtLocation((net.minecraft.server.level.ServerLevel)entity.level(),stack,entity.getBbHeight()/2)')
            t = t.replace('getCooldownPercent(stack.getItem(),0)', 'getCooldownPercent(stack,0)').replace('addCooldown(stack.getItem(),ticks)', 'addCooldown(stack,ticks)')
            t = t.replace('new Vector3f((rgb>>16&255)/255F,(rgb>>8&255)/255F,(rgb&255)/255F)', 'rgb')
            t = t.replace('BuiltInRegistries.SOUND_EVENT.get(', 'BuiltInRegistries.SOUND_EVENT.getValue(')
            start=t.index('    public static void chargeInventory(');end=t.index('    public static void privateSound(',start)
            t=t[:start]+'''    public static void chargeInventory(Player player,float energy) {
        var targets=new java.util.ArrayList<net.neoforged.neoforge.transfer.energy.EnergyHandler>();
        for(int i=0;i<36;i++) {
            var access=net.neoforged.neoforge.transfer.access.ItemAccess.forPlayerSlot(player,i);
            var storage=access.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.ITEM);
            if(storage!=null) try(var transaction=net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                if(storage.insert(1,transaction)>0) targets.add(storage);
            }
        }
        if(targets.isEmpty()) return;
        int each=Math.max(0,(int)Math.ceil(energy/targets.size()));
        try(var transaction=net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            for(var storage:targets) storage.insert(each,transaction);transaction.commit();
        }
    }
'''+t[end:]
            for old,new in [('getString','getStringOr'),('getInt','getIntOr'),('getLong','getLongOr'),('getFloat','getFloatOr'),('getDouble','getDoubleOr'),('getBoolean','getBooleanOr')]:
                default='""' if old=='getString' else 'false' if old=='getBoolean' else '0'
                t=t.replace('tag.'+old+'(key)', 'tag.'+new+'(key,'+default+')')
        elif name == 'effect/ItemAttributes.java':
            t=t.replace('BuiltInRegistries.ATTRIBUTE.getHolder(', 'BuiltInRegistries.ATTRIBUTE.get(').replace('"generic."+entry.attribute()', 'entry.attribute()')
        elif name in ['machine/MachineEntity.java','machine/ChamberEntity.java']:
            t=t.replace('level.isClientSide', 'level.isClientSide()')
            t=t.replace('loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries)', 'loadAdditional(net.minecraft.world.level.storage.ValueInput tag)')
            t=t.replace('saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries)', 'saveAdditional(net.minecraft.world.level.storage.ValueOutput tag)')
            t=t.replace('super.loadAdditional(tag,registries);','super.loadAdditional(tag);').replace('super.saveAdditional(tag,registries);','super.saveAdditional(tag);')
            t=t.replace('ContainerHelper.loadAllItems(tag,items,registries)', 'ContainerHelper.loadAllItems(tag,items)').replace('ContainerHelper.saveAllItems(tag,items,registries)', 'ContainerHelper.saveAllItems(tag,items)')
            t=re.sub(r'tag.getInt\("([a-z_]+)"\)',r'tag.getIntOr("\1",0)',t)
            t=re.sub(r'tag.getString\("([a-z_]+)"\)',r'tag.getStringOr("\1","")',t)
            if name.endswith('ChamberEntity.java'):
                at=t.rfind('}')
                t=t[:at]+'''    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state) {
        if(level instanceof ServerLevel) { ChamberBridge.drop(this);clearEffects(); }
    }
'''+t[at:]
        elif name == 'machine/ChamberBlock.java':
            t=t.replace('DirectionProperty FACING','EnumProperty<Direction> FACING')
            start=t.index('    @Override public void onRemove(');t=t[:start]+'}\n'
        elif name == 'machine/ChamberBridge.java':
            t=t.replace('BlockItem.setBlockEntityData(stack,Content.CHAMBER_ENTITY.get(),data)', 'stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.TypedEntityData.of(Content.CHAMBER_ENTITY.get(),data))')
        elif name == 'machine/MachineBlock.java':
            t=t.replace('DirectionProperty FACING','EnumProperty<Direction> FACING')
            t=t.replace('InteractionResult.sidedSuccess(level.isClientSide)', 'InteractionResult.SUCCESS').replace('level.isClientSide', 'level.isClientSide()')
            start=t.index('    @Override public void onRemove(');end=t.index('    @Override public boolean hasAnalogOutputSignal',start)
            t=t[:start]+'''    @Override protected void affectNeighborsAfterRemoval(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,boolean moved) {
        Containers.updateNeighboursAfterDestroy(state,level,pos);
    }
'''+t[end:]
            t=t.replace('getAnalogOutputSignal(BlockState state,Level level,BlockPos pos)', 'getAnalogOutputSignal(BlockState state,Level level,BlockPos pos,Direction direction)')
        elif name == 'machine/MachineBridge.java':
            t=t.replace('stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING)', 'level==null?0:stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING,level.fuelValues())')
            t=t.replace('stack.hasCraftingRemainingItem()?stack.getCraftingRemainingItem():ItemStack.EMPTY', 'stack.getCraftingRemainder()==null?ItemStack.EMPTY:stack.getCraftingRemainder().create()')
            t=t.replace('.level().isClientSide)', '.level().isClientSide())')
            t=t.replace('output.onCraftedBy(player.level(),player,count)', 'output.onCraftedBy(player,count)').replace('firePlayerSmeltedEvent(player,output)', 'firePlayerSmeltedEvent(player,output,count)')
        elif name in ['recipe/ParagonRecipe.java','recipe/DetectorRecipe.java']:
            t=t.replace('public DetectorRecipe(CraftingBookCategory category) { super(category); }', 'public DetectorRecipe() {}')
            t=t.replace('public ParagonRecipe(CraftingBookCategory category) { super(category); }', 'public ParagonRecipe() {}')
            t=t.replace('import net.minecraft.core.RegistryAccess;', '')
            t=t.replace('assemble(CraftingInput input,net.minecraft.core.HolderLookup.Provider registries)', 'assemble(CraftingInput input)')
            t=re.sub(r'^    @Override public boolean canCraftInDimensions[^\n]*\n','',t,flags=re.M)
        elif name == 'client/MachineScreen.java':
            t=t.replace('import net.minecraft.client.gui.GuiGraphics;', 'import net.minecraft.client.gui.GuiGraphicsExtractor;\nimport net.minecraft.client.renderer.RenderPipelines;')
            t=t.replace('super(menu,inventory,title);','super(menu,inventory,title,176,166);')
            t=t.replace('protected void renderLabels(GuiGraphics graphics,int x,int y)', 'protected void extractLabels(GuiGraphicsExtractor graphics,int x,int y)').replace('graphics.drawString(font,title,10,-22,color,false)', 'graphics.text(font,title,10,-22,0xff000000|color,false)')
            start=t.index('    @Override public void render(');end=t.index('\n',start)+1
            t=t[:start]+t[end:]
            t=t.replace('protected void renderBg(GuiGraphics graphics,float partial,int x,int y)', 'public void extractBackground(GuiGraphicsExtractor graphics,int x,int y,float partial)')
            t=t.replace('        graphics.blit(texture,leftPos,', '        super.extractBackground(graphics,x,y,partial);\n        graphics.blit(texture,leftPos,',1)
            t=re.sub(r'graphics.blit\(texture,([^;]+)\);',r'graphics.blit(RenderPipelines.GUI_TEXTURED,texture,\1,256,256);',t)
        elif name == 'client/ClientSetup.java':
            t = t.replace(',bus=EventBusSubscriber.Bus.MOD', '')
        elif name == 'client/ChestRenderer.java':
            t = '''package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.inventory.MetalChestEntity;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
/** Native model submission also resets lid pose between distinct chest states. */
public final class ChestRenderer extends net.minecraft.client.renderer.blockentity.ChestRenderer<MetalChestEntity> {
    public ChestRenderer(BlockEntityRendererProvider.Context context) { super(context); }
    @Override protected SpriteId getCustomSprite(MetalChestEntity chest,ChestRenderState state) {
        String id=BuiltInRegistries.BLOCK.getKey(chest.getBlockState().getBlock()).getPath();
        return Sheets.CHEST_MAPPER.apply(Identifier.fromNamespaceAndPath("metallurgy",id));
    }
}
'''
        elif name == 'client/ChestScreen.java':
            t = '''package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.inventory.MetalChestMenu;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
public final class ChestScreen extends AbstractContainerScreen<MetalChestMenu> {
    private final String metal;
    private final Identifier texture;
    private final int textureHeight;
    public ChestScreen(MetalChestMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title,menu.columns*18+14,menu.rows*18+116);
        metal=switch(menu.capacity) {case 54->"brass";case 72->"silver";case 81->"electrum";case 90->"rubracium";case 108->"platinum";default->throw new IllegalArgumentException("Chest tier");};
        texture=Identifier.fromNamespaceAndPath("metallurgychests","textures/gui/"+metal+"_chest.png");
        textureHeight=imageHeight>256?imageHeight:256;
        titleLabelX=10;titleLabelY=9;
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics,int x,int y,float partial) {
        super.extractBackground(graphics,x,y,partial);
        graphics.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos,topPos,0,0,imageWidth,imageHeight,256,textureHeight);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics,int x,int y) {
        graphics.text(font,title,titleLabelX,titleLabelY,0xff000000|Materials.BY_NAME.get(metal).color(),false);
    }
}
'''
        t=re.sub(r'\.level\(\)\.isClientSide\b(?!\()', '.level().isClientSide()',t)
        out = DEST / relative
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(t, encoding='utf-8')
    metadata = ROOT / 'neoforge-26.1.2/src/main/resources/META-INF/neoforge.mods.toml'
    t = metadata.read_text(encoding='utf-8')
    t = '\n'.join(line for line in t.splitlines() if not line.startswith(('modLoader=', 'loaderVersion='))) + '\n'
    metadata.write_text(t, encoding='utf-8')


if __name__ == '__main__':
    main()
    from generate_gauntlet_tools import apply
    apply('neoforge-26.1.2')
    from generate_processing_adapters import generate26
    generate26()
    from generate_optional_pack_adapters import generate
    generate('neoforge-26.1.2')
