package com.barby.ignshistoryplus;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IgnsHistoryPlus implements ModInitializer {

    public static final String MOD_ID = "ignshistory_plus";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("IGNSHistory+ mod loaded (client features initialize separately).");
    }
}
