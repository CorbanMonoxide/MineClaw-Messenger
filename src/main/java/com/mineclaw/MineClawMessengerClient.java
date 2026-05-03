package com.mineclaw;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.LinkedList;
import java.util.List;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.Gson;

public class MineClawMessengerClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("mineclaw");
    private static final String WEBHOOK_URL = "http://100.74.68.45:9876/nav-message";
    private static final String RESPONSE_URL = "http://100.74.68.45:9876/nav-responses";
    private static final Gson GSON = new Gson();
    private static String playerName = "MinecraftPlayer";
    private static List<String> messageHistory = new LinkedList<>();
    private static final int MAX_HISTORY = 50;

    @Override
    public void onInitializeClient() {
        LOGGER.info("===== MineClaw Messenger Client INITIALIZING =====");
        LOGGER.info("Webhook URL: " + WEBHOOK_URL);
        LOGGER.info("Response URL: " + RESPONSE_URL);
        
        // Register commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // Main /nav command
            dispatcher.register(
                ClientCommands.literal("nav")
                    .then(ClientCommands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> handleNavCommand(ctx))
                    )
            );
            
            // /nav-status command
            dispatcher.register(
                ClientCommands.literal("nav-status")
                    .executes(ctx -> handleStatusCommand(ctx))
            );
            
            // /nav-history command
            dispatcher.register(
                ClientCommands.literal("nav-history")
                    .executes(ctx -> handleHistoryCommand(ctx))
            );
            
            // /nav-settings command
            dispatcher.register(
                ClientCommands.literal("nav-settings")
                    .then(ClientCommands.argument("setting", StringArgumentType.word())
                        .then(ClientCommands.argument("value", StringArgumentType.greedyString())
                            .executes(ctx -> handleSettingsCommand(ctx))
                        )
                    )
            );
            
            LOGGER.info("[MineClaw] Commands registered successfully");
        });

        // Start response poller timer
        Timer timer = new Timer("MineClaw-Poller", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                pollForResponses();
            }
        }, 1000, 2000);
        
        LOGGER.info("===== MineClaw Messenger Client READY =====");
    }

    private int handleNavCommand(CommandContext<?> ctx) {
        String message = StringArgumentType.getString(ctx, "message");
        playerName = "MinecraftPlayer";
        
        LOGGER.info("[MineClaw Command] /nav received: " + message);
        
        // Send in background thread
        new Thread(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("message", message);
                payload.addProperty("sender", playerName);
                
                String jsonPayload = GSON.toJson(payload);
                LOGGER.info("[MineClaw Send] Payload: " + jsonPayload);

                URL url = new URL(WEBHOOK_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    addToHistory("→ " + message);
                    LOGGER.info("[MineClaw] Message sent successfully (HTTP " + responseCode + ")");
                } else {
                    LOGGER.error("[MineClaw Send] Failed - HTTP " + responseCode);
                }
                
                conn.disconnect();

            } catch (Exception e) {
                LOGGER.error("[MineClaw Send] EXCEPTION: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        return 1;
    }

    private int handleStatusCommand(CommandContext<?> ctx) {
        LOGGER.info("[MineClaw] Status - Webhook: " + WEBHOOK_URL);
        LOGGER.info("[MineClaw] Status - Player: " + playerName);
        LOGGER.info("[MineClaw] Status - Messages: " + messageHistory.size());
        return 1;
    }

    private int handleHistoryCommand(CommandContext<?> ctx) {
        if (messageHistory.isEmpty()) {
            LOGGER.info("[MineClaw] No message history");
        } else {
            LOGGER.info("[MineClaw] Message History:");
            for (String msg : messageHistory) {
                LOGGER.info("  " + msg);
            }
        }
        return 1;
    }

    private int handleSettingsCommand(CommandContext<?> ctx) {
        String setting = StringArgumentType.getString(ctx, "setting");
        String value = StringArgumentType.getString(ctx, "value");
        
        if ("player".equalsIgnoreCase(setting)) {
            playerName = value;
            LOGGER.info("[MineClaw] Player name set to: " + playerName);
        } else {
            LOGGER.info("[MineClaw] Unknown setting: " + setting);
        }
        
        return 1;
    }

    private static void addToHistory(String message) {
        messageHistory.add(message);
        if (messageHistory.size() > MAX_HISTORY) {
            messageHistory.remove(0);
        }
    }

    private static void pollForResponses() {
        new Thread(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("sender", playerName);

                HttpURLConnection conn = (HttpURLConnection) new URL(RESPONSE_URL).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(GSON.toJson(payload).getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    InputStream is = conn.getInputStream();
                    Scanner scanner = new Scanner(is).useDelimiter("\\A");
                    String body = scanner.hasNext() ? scanner.next() : "";
                    is.close();
                    
                    JsonObject response = JsonParser.parseString(body).getAsJsonObject();
                    JsonArray responses = response.getAsJsonArray("responses");
                    
                    if (responses != null && responses.size() > 0) {
                        for (int i = 0; i < responses.size(); i++) {
                            JsonObject resp = responses.get(i).getAsJsonObject();
                            String respText = resp.get("response").getAsString();
                            addToHistory("← " + respText);
                            LOGGER.info("[MineClaw Response] " + respText);
                        }
                    }
                }
                conn.disconnect();

            } catch (Exception e) {
                // Silently ignore poll errors
            }
        }).start();
    }
}
