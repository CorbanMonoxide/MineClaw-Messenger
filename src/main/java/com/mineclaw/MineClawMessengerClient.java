package com.mineclaw;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class MineClawMessengerClient implements ClientModInitializer {
    public static final String MOD_ID = "mineclaw-messenger";
    
    private static KeyBinding sendNavMessageKey;
    private static final NavMessenger messenger = new NavMessenger();

    @Override
    public void onInitializeClient() {
        sendNavMessageKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.mineclaw.send_nav_message",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "category.mineclaw.keys"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (sendNavMessageKey.wasPressed()) {
                if (client.player != null) {
                    messenger.openChatScreen();
                }
            }
        });
    }
}
