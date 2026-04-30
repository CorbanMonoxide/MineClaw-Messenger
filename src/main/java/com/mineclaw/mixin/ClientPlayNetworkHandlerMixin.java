package com.mineclaw.mixin;

import com.mineclaw.NavMessenger;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    private static final NavMessenger messenger = new NavMessenger();

    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onChatMessage(ChatMessageS2CPacket packet, CallbackInfo ci) {
        String messageText = packet.content().getString();
        if (messageText.startsWith("@nav")) {
            messenger.sendMessage(messageText);
        }
    }
}
