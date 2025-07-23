package fr.blueproject.oglinter.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.languagetool.JLanguageTool;
import org.languagetool.language.French;

public class OglinterClient implements ClientModInitializer {
    public static final JLanguageTool LANG_TOOL = new JLanguageTool(new French());
    public static final String MOD_ID = "OGLinterClient";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static boolean ENABLED = true;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing OGLinter client");
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("og")
                            .then(ClientCommandManager.literal("toggle")
                                    .executes(context -> {
                                        OglinterClient.toggle();
                                        context.getSource().sendFeedback(Text.literal(
                                                "OGLinter " + (OglinterClient.ENABLED ? "activé " : "désactivé ")
                                        ));
                                        return 1;
                                    })
                            )
            );
        });
    }

    public static void toggle() {
        ENABLED = !ENABLED;
        LOGGER.info("OGLinter {}", ENABLED ? "activé" : "désactivé");
    }
}
