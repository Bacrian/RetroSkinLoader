package imbacrian.retroskinloader.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.core.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import imbacrian.retroskinloader.RetroSkinLoader;

@Mixin(targets = "net.minecraft.core.util.helper.GetSkinUrlThread")
public abstract class GetSkinUrlThreadMixin {
    @Shadow private Player player;

    @Inject(method = "getSkinObject", at = @At("HEAD"), cancellable = true)
    private void getSkinObject(String username, CallbackInfoReturnable<String> cir) {
        String profileJson = RetroSkinLoader.loadSessionProfile(username);
        if (profileJson != null) {
            cir.setReturnValue(profileJson);
        }
    }

    @Inject(method = "run", at = @At("TAIL"))
    private void rsl$restoreServerProfile(CallbackInfo ci) {
        if (this.player != null) {
            RetroSkinLoader.applyServerProfile(this.player);
            // Force texture reload by clearing the downloaded texture cache for this player
            if (Minecraft.getMinecraft() != null) {
                if (this.player.skinURL != null) {
                    Minecraft.getMinecraft().textureManager.downloadedTextures.remove(this.player.skinURL);
                    RetroSkinLoader.LOGGER.info("Cleared texture cache for skin: {}", this.player.skinURL);
                }
                if (this.player.capeURL != null) {
                    Minecraft.getMinecraft().textureManager.downloadedTextures.remove(this.player.capeURL);
                    RetroSkinLoader.LOGGER.info("Cleared texture cache for cape: {}", this.player.capeURL);
                }
            }
        }
    }
}
