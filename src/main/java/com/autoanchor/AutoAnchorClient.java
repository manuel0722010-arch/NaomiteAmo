package com.autoanchor;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

/**
 * Auto Anchor (cliente).
 *
 * Cuando esta activado, tienes un totem en la mano principal y estas mirando
 * un ancla de reaparicion con carga (>0) fuera del Nether, interactua con el
 * ancla para que explote. Empieza DESACTIVADO; se activa con la tecla G
 * (configurable en Controles).
 */
public class AutoAnchorClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("autoanchor", "main"));

    private static KeyMapping toggleKey;

    private boolean enabled = false;
    private int cooldown = 0;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.autoanchor.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft mc) {
        LocalPlayer player = mc.player;

        while (toggleKey.consumeClick()) {
            enabled = !enabled;
            if (player != null) {
                player.displayClientMessage(
                        Component.literal("Auto Anchor: " + (enabled ? "ON" : "OFF")), true);
            }
        }

        if (!enabled || player == null || mc.level == null || mc.gameMode == null || mc.screen != null) {
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        // En el Nether el ancla funciona como punto de reaparicion y no explota.
        if (mc.level.dimension() == Level.NETHER) {
            return;
        }

        if (!player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)) {
            return;
        }

        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockState state = mc.level.getBlockState(hit.getBlockPos());
        if (!state.is(Blocks.RESPAWN_ANCHOR) || state.getValue(RespawnAnchorBlock.CHARGE) <= 0) {
            return;
        }

        mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.swing(InteractionHand.MAIN_HAND);
        cooldown = 5;
    }
}
