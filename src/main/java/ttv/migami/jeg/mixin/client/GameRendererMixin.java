package ttv.migami.jeg.mixin.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.init.ModEffects;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", ordinal = 0, shift = At.Shift.AFTER))
    public void updateCameraAndRender(float partialTicks, long nanoTime, boolean renderWorldIn, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null)
        {
            return;
        }

        MobEffectInstance effect = player.getEffect(ModEffects.BLINDED.get());
        if (effect != null)
        {
            // Render white screen-filling overlay at full alpha effect when duration is above threshold
            // When below threshold, fade to full transparency as duration approaches 0
            float percent = Math.min((effect.getDuration() / (float) Config.SERVER.alphaFadeThreshold.get()), 1);
            Window window = minecraft.getWindow();
            GuiGraphics pGuiGraphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
            pGuiGraphics.fill(0, 0, window.getScreenWidth(), window.getScreenHeight(), ((int) (percent * Config.SERVER.alphaOverlay.get() + 0.5) << 24) | 16777215);
        }

        MobEffectInstance smokeEffect = player.getEffect(ModEffects.SMOKED.get());

        if (smokeEffect != null)
        {
            float percent = Math.min((smokeEffect.getDuration() / (float) Config.SERVER.alphaFadeThreshold.get()), 1);
            Window window = Minecraft.getInstance().getWindow();
            GuiGraphics pGuiGraphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
            pGuiGraphics.fill(0, 0, window.getScreenWidth(), window.getScreenHeight(), ((int) (percent * 248 + 0.5) << 24) | 8156784);
        }
    }
}