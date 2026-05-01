package com.mineclaw;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NavMessenger {
    private static final String WEBHOOK_URL = "http://localhost:8765/nav-message";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public void openChatScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.setScreen(new ChatScreen("@nav "));
        }
    }

    public void sendMessage(String message) {
        if (!message.startsWith("@nav")) {
            return;
        }

        String content = message.substring(4).trim();
        if (content.isEmpty()) {
            return;
        }

        Thread sendThread = new Thread(() -> {
            try {
                String jsonPayload = String.format(
                    "{\"message\": \"%s\", \"sender\": \"minecraft\"}",
                    escapeJson(content)
                );

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(WEBHOOK_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    MinecraftClient.getInstance().player.sendMessage(
                        net.minecraft.text.Text.of("§a✓ Message sent to Nav"),
                        false
                    );
                } else {
                    MinecraftClient.getInstance().player.sendMessage(
                        net.minecraft.text.Text.of("§c✗ Failed to send message"),
                        false
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        sendThread.setDaemon(true);
        sendThread.start();
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }
}
