package com.mineclaw;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonObject;
import com.google.gson.Gson;

public class ChatHook {
    private static final String WEBHOOK_URL = "http://100.74.68.45:8765/nav-message";
    private static final Gson GSON = new Gson();

    public static void register() {
        ClientCommandManager.register(
            ClientCommandManager.literal("@nav")
                .then(ClientCommandManager.argument("message", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                    .executes(ChatHook::handleCommand)
                )
        );
    }

    private static int handleCommand(CommandContext<FabricClientCommandSource> ctx) {
        String message = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "message");
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.player == null) {
            return 0;
        }

        new Thread(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("message", message);
                payload.addProperty("sender", client.player.getName().getString());

                HttpURLConnection conn = (HttpURLConnection) new URL(WEBHOOK_URL).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(GSON.toJson(payload).getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    client.player.sendMessage(Text.literal("§6[Nav]§r Message sent"), false);
                } else {
                    client.player.sendMessage(Text.literal("§c[Nav]§r Error: " + responseCode), false);
                }
                conn.disconnect();

            } catch (Exception e) {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§c[Nav]§r Connection failed: " + e.getMessage()), false);
                }
                MineClawMessenger.LOGGER.error("Failed to send message", e);
            }
        }).start();

        return 1;
    }
}
