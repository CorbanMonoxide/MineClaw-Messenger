package com.mineclaw;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MineClawMessenger implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("mineclaw");

    @Override
    public void onInitialize() {
        LOGGER.info("MineClaw Messenger initializing...");
        ChatHook.register();
    }
}
