package com.mineclaw;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MineClawMessenger implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("mineclaw");

    @Override
    public void onInitialize() {
        LOGGER.info("MineClaw Messenger loaded!");
    }
}
