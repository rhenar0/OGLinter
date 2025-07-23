package fr.blueproject.oglinter.mixin.client;

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

import java.io.IOException;
import java.util.List;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow
    protected TextFieldWidget chatField;

    @Unique
    private List<RuleMatch> lastMatches = List.of();

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) throws IOException {
        if (!OglinterClient.ENABLED || chatField == null || chatField.getText().isEmpty() || chatField.getText().startsWith("/") || chatField.getText().startsWith(">")) return;

        String input = chatField.getText();
        lastMatches = OglinterClient.LANG_TOOL.check(input);

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
