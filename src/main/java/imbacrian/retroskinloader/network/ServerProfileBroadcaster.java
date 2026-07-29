package imbacrian.retroskinloader.network;

import net.minecraft.core.net.packet.PacketCustomPayload;
import net.minecraft.server.entity.player.PlayerServer;
import net.minecraft.server.net.PlayerList;

import imbacrian.retroskinloader.skin.SkinProfile;

public final class ServerProfileBroadcaster {

	private ServerProfileBroadcaster() {
	}

	/** Sends a player's profile to a SINGLE specific connection. */
	public static void sendTo(PlayerServer recipient, int entityId, String username, SkinProfile profile) {
		byte[] payload = RslProfilePayload.encodeSet(entityId, username, profile);
		recipient.playerNetServerHandler.sendPacket(new PacketCustomPayload(RslProfilePayload.CHANNEL, payload));
	}

	/** Notifies ALL connected players about a player's profile (e.g. when logging in). */
	public static void broadcast(PlayerList playerList, int entityId, String username, SkinProfile profile) {
		byte[] payload = RslProfilePayload.encodeSet(entityId, username, profile);
		playerList.sendPacketToAllPlayers(new PacketCustomPayload(RslProfilePayload.CHANNEL, payload));
	}
}
