package fr.blueproject.oglinter.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.languagetool.JLanguageTool;
import org.languagetool.language.French;

import java.util.Set;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class OglinterClient implements ClientModInitializer {
    public static final JLanguageTool LANG_TOOL = new JLanguageTool(new French());
    public static final String MOD_ID = "OGLinterClient";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static boolean ENABLED = true;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing OGLinter client");
        IgnoreListManager.load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("og")
                            .then(literal("toggle")
                                    .executes(context -> {
                                        OglinterClient.toggle();
                                        context.getSource().sendFeedback(Text.literal(
                                                "OGLinter " + (OglinterClient.ENABLED ? "activé" : "désactivé")
                                        ));
                                        return 1;
                                    }))
                            .then(literal("dico")
                                    .then(literal("add")
                                            .then(argument("mot", word())
                                                    .executes(context -> {
                                                        String word = getString(context, "mot");
                                                        boolean added = IgnoreListManager.addWord(word);
                                                        context.getSource().sendFeedback(Text.literal(
                                                                added ? "§aMot ajouté à la liste ignorée." : "§eCe mot est déjà ignoré."
                                                        ));
                                                        return 1;
                                                    })))
                                    .then(literal("remove")
                                            .then(argument("mot", word())
                                                    .executes(context -> {
                                                        String word = getString(context, "mot");
                                                        boolean removed = IgnoreListManager.removeWord(word);
                                                        context.getSource().sendFeedback(Text.literal(
                                                                removed ? "§cMot retiré de la liste ignorée." : "§eCe mot n'était pas ignoré."
                                                        ));
                                                        return 1;
                                                    })))
                                    .then(literal("list")
                                            .executes(context -> {
                                                Set<String> ignored = IgnoreListManager.getIgnoreWords();
                                                if (ignored.isEmpty()) {
                                                    context.getSource().sendFeedback(Text.literal("§7Aucun mot ignoré."));
                                                } else {
                                                    context.getSource().sendFeedback(Text.literal("§6Mots ignorés :"));
                                                    for (String word : ignored) {
                                                        context.getSource().sendFeedback(Text.literal(" - " + word));
                                                    }
                                                }
                                                return 1;
                                            }))
                            )
            );
        });
    }

    public static void toggle() {
        ENABLED = !ENABLED;
        LOGGER.info("OGLinter {}", ENABLED ? "activé" : "désactivé");
    }
}
