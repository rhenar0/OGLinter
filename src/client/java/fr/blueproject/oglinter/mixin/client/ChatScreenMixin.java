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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.List;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow
    private TextFieldWidget chatField;

    private List<RuleMatch> lastMatches = List.of();

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) throws IOException {
        if (!OglinterClient.ENABLED || chatField == null || chatField.getText().isEmpty()) return;
        String input = chatField.getText();
        lastMatches = OglinterClient.LANG_TOOL.check(input);
        drawUnderlines(context, input, lastMatches);
    }

    private void drawUnderlines(DrawContext context, String input, List<RuleMatch> matches) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        for (RuleMatch match : matches) {
            int start = match.getFromPos();
            int end = match.getToPos();

            if (start >= input.length() || end > input.length()) continue;

            String before = input.substring(0, start);
            String word = input.substring(start, end);

            TextFieldWidgetAccessor accessor = (TextFieldWidgetAccessor)(Object) chatField;
            int scrollOffset = accessor.getFirstCharacterIndex();

            String visiblePrefix = input.substring(0, Math.min(scrollOffset, input.length()));
            int scrollX = textRenderer.getWidth(visiblePrefix);

            int xStart = chatField.getX() + textRenderer.getWidth(before) - scrollX;
            int xEnd = xStart + textRenderer.getWidth(word);

            int yUnderline = chatField.getY() + chatField.getHeight() - 4;

            context.fill(xStart, yUnderline, xEnd, yUnderline + 1, 0xFFFF5555);

            List<String> suggestions = match.getSuggestedReplacements();
            if (!suggestions.isEmpty()) {
                String suggestion = suggestions.get(0);
                int suggestionWidth = textRenderer.getWidth(suggestion);
                int xSuggestion = xStart + (xEnd - xStart - suggestionWidth) / 2;
                int ySuggestion = yUnderline - 12;

                int paddingX = 4;
                int paddingY = 3;

                int boxLeft = xSuggestion - paddingX;
                int boxRight = xSuggestion + suggestionWidth + paddingX;
                int boxBottom = yUnderline - 10;
                int boxTop = boxBottom - textRenderer.fontHeight - 2 * paddingY;

                context.fill(boxLeft, boxTop, boxRight, boxBottom, 0xFF2E2E2E);
                context.drawBorder(boxLeft, boxTop, boxRight - boxLeft, boxBottom - boxTop, 0xFF5A5A5A);

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