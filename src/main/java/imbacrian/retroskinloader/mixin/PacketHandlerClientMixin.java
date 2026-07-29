package imbacrian.retroskinloader.mixin;

import imbacrian.retroskinloader.RetroSkinLoader;
import imbacrian.retroskinloader.network.RslProfilePayload;
import imbacrian.retroskinloader.network.RslProfilePayload.ProfileUpdate;
import imbacrian.retroskinloader.skin.ServerProfileProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.net.packet.PacketCustomPayload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.net.handler.PacketHandlerClient")
public abstract class PacketHandlerClientMixin {
    @Shadow @Final private Minecraft mc;

    @Inject(method = "handleCustomPayload", at = @At("HEAD"))
    private void rsl$handleProfilePacket(PacketCustomPayload packet, CallbackInfo ci) {
        if (!RslProfilePayload.CHANNEL.equals(packet.channel)) {
            return;
        }
        try {
            ProfileUpdate update = RslProfilePayload.decode(packet.data);
            if (update.isClear()) {
                ServerProfileProvider.clear(update.getUsername());
                refreshPlayer(update.getEntityId());
                RetroSkinLoader.LOGGER.info("Cleared server skin profile for {}", update.getUsername());
                return;
            }
            ServerProfileProvider.update(update.getUsername(), update.getProfile());
            applyProfile(update);
            RetroSkinLoader.LOGGER.info("Applied server skin profile for {}", update.getUsername());
        } catch (Exception exception) {
            RetroSkinLoader.LOGGER.warn("Ignoring invalid RSL:Profile packet", exception);
        }
    }

    private void applyProfile(ProfileUpdate update) {
        Player player = findPlayer(update.getEntityId(), update.getUsername());
        if (player == null) {
            return;
        }
        player.skinURL = update.getProfile().getSkinUrl();
        player.capeURL = update.getProfile().getCapeUrl();
        player.slimModel = "slim".equals(update.getProfile().getModel());
    }

    private void refreshPlayer(int entityId) {
        Player player = findPlayer(entityId, null);
        if (player != null) {
            new net.minecraft.core.util.helper.GetSkinUrlThread(player);
        }
    }

    private Player findPlayer(int entityId, String expectedUsername) {
        Player player = this.mc.thePlayer != null && this.mc.thePlayer.id == entityId ? this.mc.thePlayer : null;
        if (player == null && this.mc.currentWorld != null) {
            Entity entity = this.mc.currentWorld.getEntityByID(entityId);
            if (entity instanceof Player) {
                player = (Player) entity;
            }
        }
        if (player != null && expectedUsername != null && !expectedUsername.equalsIgnoreCase(player.username)) {
            RetroSkinLoader.LOGGER.warn("Ignoring RSL profile whose username does not match entity {}", entityId);
            return null;
        }
        return player;
    }
}
