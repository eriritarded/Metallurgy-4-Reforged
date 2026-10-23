package it.hurts.metallurgy_reforged.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.hurts.metallurgy_reforged.gadget.GadgetLogic;
import it.hurts.metallurgy_reforged.gadget.ShieldLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/** Native player skin and the unchanged gauntlet mesh share one moving arm pose. */
@EventBusSubscriber(modid = "metallurgy", value = Dist.CLIENT)
public final class GauntletHandRenderer {
    @SubscribeEvent
    public static void render(RenderHandEvent event) {
        if (!GadgetLogic.is(event.getItemStack(), "rubracium_gauntlet")) return;
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) return;

        HumanoidArm hand = event.getHand() == InteractionHand.MAIN_HAND
                ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean left = hand == HumanoidArm.LEFT;
        var renderer = minecraft.getEntityRenderDispatcher().getPlayerRenderer(player);
        var arm = left ? renderer.getModel().leftArm : renderer.getModel().rightArm;
        var skin = player.getSkin();
        var pose = event.getPoseStack();
        pose.pushPose();
        try {
            applyArmPose(pose, left, event.getEquipProgress(), event.getSwingProgress());
            // Match AvatarRenderer's native reset/roll even when invisibility or
            // another RenderArmEvent subscriber suppresses the skin submission.
            arm.resetPose();
            arm.zRot = left ? -0.1F : 0.1F;
            if (!player.isInvisible() && !ShieldLogic.invisible(player)) {
                var texture = skin.body().texturePath();
                boolean sleeve = player.isModelPartShown(left
                        ? PlayerModelPart.LEFT_SLEEVE : PlayerModelPart.RIGHT_SLEEVE);
                if (left) renderer.renderLeftHand(pose, event.getSubmitNodeCollector(),
                        event.getPackedLight(), texture, sleeve, player);
                else renderer.renderRightHand(pose, event.getSubmitNodeCollector(),
                        event.getPackedLight(), texture, sleeve, player);
            }

            // Attach in the same arm-local frame as the third-person hand layer.
            // The existing third-person JSON already clears the skin and sleeve.
            arm.translateAndRotate(pose);
            if (skin.model() == PlayerModelType.SLIM) {
                pose.translate((left ? -0.5F : 0.5F) / 16.0F, 0, 0);
            }
            pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.translate((left ? -1.0F : 1.0F) / 16.0F, 2.0F / 16.0F, -10.0F / 16.0F);
            minecraft.gameRenderer.itemInHandRenderer.renderItem(player, event.getItemStack(),
                    left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    pose, event.getSubmitNodeCollector(), event.getPackedLight());
            // Replace the ordinary held-item draw; it would omit the skin and
            // apply a second, independent first-person transform to the metal.
            event.setCanceled(true);
        } finally {
            pose.popPose();
        }
    }

    /** ItemInHandRenderer.renderPlayerArm constants from the exact 26.1.2 client. */
    private static void applyArmPose(PoseStack pose, boolean left, float equip, float swing) {
        float sign = left ? -1.0F : 1.0F;
        float root = Mth.sqrt(swing);
        float pi = (float) Math.PI;
        pose.translate(sign * (0.64000005F - 0.3F * Mth.sin(root * pi)),
                -0.6F - 0.6F * equip + 0.4F * Mth.sin(root * 2.0F * pi),
                -0.71999997F - 0.4F * Mth.sin(swing * pi));
        pose.mulPose(Axis.YP.rotationDegrees(sign * 45.0F));
        pose.mulPose(Axis.YP.rotationDegrees(sign * Mth.sin(root * pi) * 70.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(sign * Mth.sin(swing * swing * pi) * -20.0F));
        pose.translate(sign * -1.0F, 3.6F, 3.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(sign * 120.0F));
        pose.mulPose(Axis.XP.rotationDegrees(200.0F));
        pose.mulPose(Axis.YP.rotationDegrees(sign * -135.0F));
        pose.translate(sign * 5.6F, 0, 0);
    }

    private GauntletHandRenderer() {}
}
