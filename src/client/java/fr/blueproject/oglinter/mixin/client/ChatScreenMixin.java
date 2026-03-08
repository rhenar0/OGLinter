package fr.blueproject.oglinter.mixin.client;

import fr.blueproject.oglinter.client.IgnoreListManager;
import fr.blueproject.oglinter.client.OglinterClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.languagetool.rules.RuleMatch;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow
    protected TextFieldWidget chatField;

    @Unique
    private CompletableFuture<List<RuleMatch>> lintFuture;

    @Unique
    private String lintInput = "";

    @Unique
    private String lastCheckedInput = "";

    @Unique
    private List<RuleMatch> lastMatches = List.of();

    @Unique
    private long lastInputChangeTime = 0;

    @Unique
    private static final long DEBOUNCE_MS = 200;

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!OglinterClient.ENABLED || chatField == null) return;

        String input = chatField.getText();
        if (input.isEmpty() || input.startsWith("/") || input.startsWith(">")) {
            lastMatches = List.of();
            return;
        }

        if (!input.equals(lintInput)) {
            lintInput = input;
            lastInputChangeTime = System.currentTimeMillis();
        }

        long timeSinceLastChange = System.currentTimeMillis() - lastInputChangeTime;
        boolean shouldCheck = timeSinceLastChange >= DEBOUNCE_MS && !input.equals(lastCheckedInput);

        switch (OglinterClient.CURRENT_MODE) {
            case REAL_TIME -> {
                if (shouldCheck && (lintFuture == null || lintFuture.isDone())) {
                    final String snapshot = input;
                    lastCheckedInput = snapshot;

                    lintFuture = OglinterClient.checkAsync(snapshot).thenApply(matches -> matches.stream()
                            .filter(match -> {
                                int from = match.getFromPos();
                                int to = match.getToPos();
                                if (from < 0 || to > snapshot.length() || from >= to) return false;
                                String word = snapshot.substring(from, to);
                                return !IgnoreListManager.shouldIgnore(word);
                            })
                            .toList());
                }

                if (lintFuture != null && lintFuture.isDone()) {
                    lastMatches = lintFuture.getNow(lastMatches);
                }
            }

            case AFTER_WORD -> {
                if (!input.endsWith(" ")) {
                    drawUnderlines(context, input, lastMatches, mouseX, mouseY);
                    return;
                }

                if (shouldCheck && (lintFuture == null || lintFuture.isDone())) {
                    final String snapshot = input;
                    lastCheckedInput = snapshot;

                    lintFuture = OglinterClient.checkAsync(snapshot).thenApply(matches -> matches.stream()
                            .filter(match -> {
                                int from = match.getFromPos();
                                int to = match.getToPos();
                                if (from < 0 || to > snapshot.length() || from >= to) return false;
                                String word = snapshot.substring(from, to);
                                return !IgnoreListManager.shouldIgnore(word);
                            })
                            .toList());
                }

                if (lintFuture != null && lintFuture.isDone()) {
                    lastMatches = lintFuture.getNow(lastMatches);
                }
            }
        }

        drawUnderlines(context, input, lastMatches, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!OglinterClient.ENABLED || button != 0 || lastMatches == null || chatField == null) return;

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        String input = chatField.getText();

        for (RuleMatch match : lastMatches) {
            int start = match.getFromPos();
            int end = match.getToPos();
            if (start >= input.length() || end > input.length()) continue;

            String before = input.substring(0, start);
            String word = input.substring(start, end);
            TextFieldWidgetAccessor accessor = (TextFieldWidgetAccessor) chatField;
            int scrollOffset = accessor.getFirstCharacterIndex();
            String visiblePrefix = input.substring(0, Math.min(scrollOffset, input.length()));
            int scrollX = textRenderer.getWidth(visiblePrefix);
            int xStart = chatField.getX() + textRenderer.getWidth(before) - scrollX;
            int xEnd = xStart + textRenderer.getWidth(word);
            int yUnderline = chatField.getY() + chatField.getHeight() - 4;

            List<String> suggestions = match.getSuggestedReplacements();
            for (int i = 0; i < Math.min(3, suggestions.size()); i++) {
                String suggestion = suggestions.get(i);
                int suggestionWidth = textRenderer.getWidth(suggestion);
                int xSuggestion = xStart + (xEnd - xStart - suggestionWidth) / 2;
                int ySuggestion = yUnderline - 24 - i * 14;

                int paddingX = 4;
                int paddingY = 3;
                int boxLeft = xSuggestion - paddingX;
                int boxRight = xSuggestion + suggestionWidth + paddingX;
                int boxBottom = ySuggestion + textRenderer.fontHeight + paddingY;
                int boxTop = ySuggestion - paddingY;

                if (mouseX >= boxLeft && mouseX <= boxRight && mouseY >= boxTop && mouseY <= boxBottom) {
                    String newText = input.substring(0, start) + suggestion + input.substring(end);
                    chatField.setText(newText);
                    cir.cancel();
                    return;
                }
            }
        }
    }


    @Unique
    private void drawUnderlines(DrawContext context, String input, List<RuleMatch> matches, int mouseX, int mouseY) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        for (RuleMatch match : matches) {
            int start = match.getFromPos();
            int end = match.getToPos();

            if (start >= input.length() || end > input.length()) continue;

            String before = input.substring(0, start);
            String word = input.substring(start, end);

            TextFieldWidgetAccessor accessor = (TextFieldWidgetAccessor) chatField;
            int scrollOffset = accessor.getFirstCharacterIndex();
            String visiblePrefix = input.substring(0, Math.min(scrollOffset, input.length()));
            int scrollX = textRenderer.getWidth(visiblePrefix);

            int xStart = chatField.getX() + textRenderer.getWidth(before) - scrollX;
            int xEnd = xStart + textRenderer.getWidth(word);
            int yUnderline = chatField.getY() + chatField.getHeight() - 4;

            context.fill(xStart, yUnderline, xEnd, yUnderline + 1, 0xFFFF5555);

            List<String> suggestions = match.getSuggestedReplacements();
            for (int i = 0; i < Math.min(3, suggestions.size()); i++) {
                String suggestion = suggestions.get(i);
                int suggestionWidth = textRenderer.getWidth(suggestion);
                int xSuggestion = xStart + (xEnd - xStart - suggestionWidth) / 2;
                int ySuggestion = yUnderline - 24 - i * 14;

                int paddingX = 4;
                int paddingY = 3;
                int boxLeft = xSuggestion - paddingX;
                int boxRight = xSuggestion + suggestionWidth + paddingX;
                int boxBottom = ySuggestion + textRenderer.fontHeight + paddingY;
                int boxTop = ySuggestion - paddingY;

                boolean hovered = mouseX >= boxLeft && mouseX <= boxRight && mouseY >= boxTop && mouseY <= boxBottom;
                int backgroundColor = hovered ? 0xFF3C3C3C : 0xFF2E2E2E;
                int borderColor = hovered ? 0xFFAAAAAA : 0xFF5A5A5A;

                context.fill(boxLeft, boxTop, boxRight, boxBottom, backgroundColor);
                context.drawBorder(boxLeft, boxTop, boxRight - boxLeft, boxBottom - boxTop, borderColor);

                context.drawText(
                        textRenderer,
                        Text.literal(suggestion),
                        xSuggestion,
                        boxTop + paddingY,
                        0xFFD7D7D7,
                        false
                );

            }
        }
    }
}
