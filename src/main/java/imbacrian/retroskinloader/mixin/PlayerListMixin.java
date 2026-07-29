package imbacrian.retroskinloader.mixin;

import imbacrian.retroskinloader.network.ServerProfileBroadcaster;
import imbacrian.retroskinloader.skin.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.entity.player.PlayerServer;
import net.minecraft.server.net.PlayerList;

@Mixin(net.minecraft.server.net.PlayerList.class)
public abstract class PlayerListMixin {

	@Inject(method = "playerLoggedIn", at = @At("TAIL"))
	private void rsl$onPlayerLoggedIn(PlayerServer player, CallbackInfo ci) {
		for (PlayerServer other : ((PlayerList) (Object) this).playerEntities) {
			String otherId = PlayerSkinChoices.get(other.username);
			if (otherId == null) continue;
			SkinProfile otherProfile = SkinIdRegistry.resolve(otherId);
			if (otherProfile == null) continue;
			ServerProfileBroadcaster.sendTo(player, other.id, other.username, otherProfile); // tell the player about the skin
			if (other != player) {
				String selfId = PlayerSkinChoices.get(player.username);
				SkinProfile selfProfile = selfId == null ? null : SkinIdRegistry.resolve(selfId);
				if (selfProfile != null) ServerProfileBroadcaster.sendTo(other, player.id, player.username, selfProfile);
			}
		}
	}
}
