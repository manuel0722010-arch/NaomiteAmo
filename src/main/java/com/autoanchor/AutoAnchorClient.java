package com.autoanchor;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

public class AutoAnchorClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("autoanchor", "main"));

    private enum Step { IDLE, CHARGE, TOTEM, EXPLODE }

    private static KeyMapping toggleKey;

    private boolean enabled = false;
    private Step step = Step.IDLE;
    private int delay = 0;
    private int glowSlot = -1;
    private int totemSlot = -1;
    private BlockHitResult target;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.autoanchor.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                CATEGORY
        ));

        UseBlockCallback.EVENT.register(this::onUseBlock);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private static int findHotbar(Player player, Item item) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(item)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isChargedAnchor(Level level, BlockHitResult hit) {
        if (hit == null) return false;
        BlockState state = level.getBlockState(hit.getBlockPos());
        return state.is(Blocks.RESPAWN_ANCHOR) && state.getValue(RespawnAnchorBlock.CHARGE) > 0;
    }

    // Clic derecho en un ancla sin carga: inicia la secuencia y cancela el clic normal.
    private InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (!enabled || step != Step.IDLE || !level.isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level.dimension() == Level.NETHER) {
            return InteractionResult.PASS;
        }
        BlockState state = level.getBlockState(hit.getBlockPos());
        if (!state.is(Blocks.RESPAWN_ANCHOR) || state.getValue(RespawnAnchorBlock.CHARGE) != 0) {
            return InteractionResult.PASS;
        }
        glowSlot = findHotbar(player, Items.GLOWSTONE);
        totemSlot = findHotbar(player, Items.TOTEM_OF_UNDYING);
        if (glowSlot < 0 || totemSlot < 0) {
            return InteractionResult.PASS;
        }
        target = hit;
        step = Step.CHARGE;
        delay = 0;
        return InteractionResult.FAIL;
    }

    private void interact(Minecraft mc, LocalPlayer player) {
        mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, target);
        player.swing(InteractionHand.MAIN_HAND);
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

        if (!enabled || player == null || mc.level == null || mc.gameMode == null) {
            step = Step.IDLE;
            return;
        }

        if (delay > 0) {
            delay--;
            return;
        }

        switch (step) {
            case CHARGE -> {
                if (target == null || !mc.level.getBlockState(target.getBlockPos()).is(Blocks.RESPAWN_ANCHOR)) {
                    step = Step.IDLE;
                    return;
                }
                player.getInventory().setSelectedSlot(glowSlot);
                interact(mc, player);
                step = Step.TOTEM;
                delay = 1;
            }
            case TOTEM -> {
                player.getInventory().setSelectedSlot(totemSlot);
                step = Step.EXPLODE;
                delay = 1;
            }
            case EXPLODE -> {
                if (isChargedAnchor(mc.level, target)) {
                    interact(mc, player);
                }
                step = Step.IDLE;
                delay = 5;
            }
            case IDLE -> {
                // Respaldo: totem en mano + mirando un ancla cargada = explotarla.
                if (mc.screen != null || mc.level.dimension() == Level.NETHER) return;
                if (!player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)) return;
                if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
                if (!isChargedAnchor(mc.level, hit)) return;
                target = hit;
                interact(mc, player);
                delay = 5;
            }
        }
    }
}
