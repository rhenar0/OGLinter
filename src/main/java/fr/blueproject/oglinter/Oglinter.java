package fr.blueproject.oglinter;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Oglinter implements ModInitializer {
    public static final String MOD_ID = "OGLinter";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing OGLinter");
    }
}
