package mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.languagetool.JLanguageTool;
import org.languagetool.language.French;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.List;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow
    private TextFieldWidget chatField;

    private final JLanguageTool langTool = new JLanguageTool(new French());

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (chatField != null && chatField.getText() != null && !chatField.getText().isEmpty()) {
            String input = chatField.getText();

            try {
                List<RuleMatch> matches = langTool.check(input);

                for (RuleMatch match : matches) {
                    String message = match.getMessage();
                    String suggestion = String.join(", ", match.getSuggestedReplacements());

                    int yOffset = 12 + matches.indexOf(match) * 10;

                    context.drawText(
                            MinecraftClient.getInstance().textRenderer,
                            Text.literal("⚠ " + message + " → " + suggestion),
                            4,
                            yOffset,
                            0xFF5555,
                            false
                    );
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
