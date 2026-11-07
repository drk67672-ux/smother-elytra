package com.elytraswing;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ElytraSwingClient implements ClientModInitializer {
    private static KeyBinding boostKey;
    private int delayTicks = 0;
    private int cooldown = 0;

    @Override
    public void onInitializeClient() {
        delayTicks = loadDelay();

        boostKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.elytraswing.boost",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.elytraswing"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (cooldown > 0) cooldown--;

            ClientPlayerEntity player = client.player;
            if (player == null || !player.isFallFlying()) return;

            if (boostKey.isPressed() && cooldown <= 0) {
                if (boost(client, player)) {
                    cooldown = delayTicks;
                }
            }
        });
    }

    private boolean boost(MinecraftClient client, ClientPlayerEntity player) {
        if (client.interactionManager == null) return false;

        if (player.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) {
            client.interactionManager.interactItem(player, Hand.OFF_HAND);
            return true;
        }

        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(Items.FIREWORK_ROCKET)) {
                int previous = inv.selectedSlot;
                inv.selectedSlot = i;
                client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                inv.selectedSlot = previous;
                return true;
            }
        }
        return false;
    }

    // Config: .minecraft/config/elytraswing.properties -> delay=0..10
    private int loadDelay() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("elytraswing.properties");
        Properties props = new Properties();
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    props.load(r);
                }
            } else {
                props.setProperty("delay", "0");
                try (Writer w = Files.newBufferedWriter(file)) {
                    props.store(w, "Delay in ticks between uses (0-10)");
                }
            }
            int value = Integer.parseInt(props.getProperty("delay", "0").trim());
            return Math.max(0, Math.min(10, value));
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }
}
