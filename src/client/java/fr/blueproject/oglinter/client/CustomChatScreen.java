package fr.blueproject.oglinter.client;

import io.github.cottonmc.cotton.gui.client.LibGui;
import io.github.cottonmc.cotton.gui.widget.*;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.blueproject.oglinter.client.OglinterClient;

public class CustomChatScreen extends LibGui {
    private final WScrollPanel scrollPanel;
    private final WBox messageBox;
    private final WTextField chatInput;
    private final WBox suggestionBox;

    private final List<RuleMatch> lastMatches = new ArrayList<>();

    public CustomChatScreen() {
        super(new WPlainPanel());

        WPlainPanel root = (WPlainPanel) getRootPanel();
        root.setSize(320, 280);
        root.setInsets(Insets.ROOT_PANEL);

        messageBox = new WBox(WBox.VerticalAlignment.TOP);
        scrollPanel = new WScrollPanel(messageBox);
        scrollPanel.setSize(300, 180);
        scrollPanel.setScrollingHorizontally(false);

        chatInput = new WTextField();
        chatInput.setSize(300, 20);
        chatInput.setChangedListener(this::onTextChanged);
        chatInput.setMaxLength(256);

        suggestionBox = new WBox(WBox.VerticalAlignment.TOP);
        suggestionBox.setSize(300, 40);

        WButton sendButton = new WButton(Text.literal("Send"));
        sendButton.setOnClick(() -> sendMessage(chatInput.getText()));

        root.add(scrollPanel, 10, 10);
        root.add(chatInput, 10, 195);
        root.add(sendButton, 260, 195);
        root.add(suggestionBox, 10, 220);

        setRootPanel(root);
    }

    private void onTextChanged(String text) {
        lastMatches.clear();
        suggestionBox.clear();

        if (!OglinterClient.ENABLED || text.isEmpty() || text.startsWith("/") || text.startsWith(">")) return;

        try {
            lastMatches.addAll(OglinterClient.LANG_TOOL.check(text));

            for (RuleMatch match : lastMatches) {
                List<String> suggestions = match.getSuggestedReplacements();
                if (!suggestions.isEmpty()) {
                    String suggestion = suggestions.get(0);
                    WButton suggestionButton = new WButton(Text.literal("🔧 " + suggestion));
                    suggestionButton.setOnClick(() -> {
                        String newText = text.substring(0, match.getFromPos()) + suggestion + text.substring(match.getToPos());
                        chatInput.setText(newText);
                        onTextChanged(newText);
                    });
                    suggestionBox.add(suggestionButton);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String text) {
        if (text == null || text.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.getNetworkHandler().sendChatMessage(text);
            addMessageToList(client.player.getName().getString() + ": " + text);
        }

        chatInput.setText("");
        suggestionBox.clear();
    }

    public void addMessageToList(String message) {
        WLabel label = new WLabel(Text.literal(message));
        messageBox.add(label);
        scrollPanel.layout();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}