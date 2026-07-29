package imbacrian.retroskinloader.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentTypeString;
import com.mojang.brigadier.builder.ArgumentBuilderLiteral;
import com.mojang.brigadier.builder.ArgumentBuilderRequired;

import imbacrian.retroskinloader.skin.ElyBySkinProvider;
import imbacrian.retroskinloader.skin.MojangSkinProvider;
import imbacrian.retroskinloader.skin.ServerProfileProvider;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.net.command.CommandManager;
import net.minecraft.core.net.command.CommandSource;
import net.minecraft.core.net.packet.PacketCustomPayload;
import net.minecraft.server.entity.player.PlayerServer;

import imbacrian.retroskinloader.network.RslProfilePayload;
import imbacrian.retroskinloader.skin.PlayerSkinChoices;
import imbacrian.retroskinloader.skin.SkinIdRegistry;
import imbacrian.retroskinloader.skin.SkinProfile;

public final class CommandRsl implements CommandManager.CommandRegistry {

	@Override
	public void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(
			ArgumentBuilderLiteral.<CommandSource>literal("rsl").then(
				ArgumentBuilderLiteral.<CommandSource>literal("skin")
					.then(ArgumentBuilderLiteral.<CommandSource>literal("set")
						.then(ArgumentBuilderRequired.<CommandSource, String>argument("id", ArgumentTypeString.word())
							.executes(ctx -> executeSet(ctx.getSource(), ArgumentTypeString.getString(ctx, "id")))))
					.then(ArgumentBuilderLiteral.<CommandSource>literal("clear")
						.executes(ctx -> executeClear(ctx.getSource())))
					.then(ArgumentBuilderLiteral.<CommandSource>literal("list")
						.executes(ctx -> executeList(ctx.getSource())))
					.then(ArgumentBuilderLiteral.<CommandSource>literal("admin").requires(CommandSource::hasAdmin)
						.then(ArgumentBuilderLiteral.<CommandSource>literal("add")
							.then(ArgumentBuilderRequired.<CommandSource, String>argument("id", ArgumentTypeString.word())
								.then(ArgumentBuilderRequired.<CommandSource, String>argument("skinUrl", ArgumentTypeString.string())

									// add id skinUrl
									.executes(ctx -> executeAdminAdd(
										ctx.getSource(),
										ArgumentTypeString.getString(ctx, "id"),
										ArgumentTypeString.getString(ctx, "skinUrl"),
										null,
										null))

									// add id skinUrl <optional> (model or capeUrl)
									.then(ArgumentBuilderRequired.<CommandSource, String>argument("optional", ArgumentTypeString.string())

										.executes(ctx -> {
											String optional = ArgumentTypeString.getString(ctx, "optional");

											String capeUrl = null;
											String model = null;

											if ("default".equals(optional) || "slim".equals(optional)) {
												model = optional;
											} else {
												capeUrl = optional;
											}

											return executeAdminAdd(
												ctx.getSource(),
												ArgumentTypeString.getString(ctx, "id"),
												ArgumentTypeString.getString(ctx, "skinUrl"),
												capeUrl,
												model);
										})

										// add id skinUrl capeUrl model
										.then(ArgumentBuilderRequired.<CommandSource, String>argument("model", ArgumentTypeString.word())

											.executes(ctx -> executeAdminAdd(
												ctx.getSource(),
												ArgumentTypeString.getString(ctx, "id"),
												ArgumentTypeString.getString(ctx, "skinUrl"),
												ArgumentTypeString.getString(ctx, "optional"),
												ArgumentTypeString.getString(ctx, "model")
											))
										)
									)
								)
							)
						)
						.then(ArgumentBuilderLiteral.<CommandSource>literal("remove")
							.then(ArgumentBuilderRequired.<CommandSource, String>argument("id", ArgumentTypeString.word())
								.executes(ctx -> executeAdminRemove(ctx.getSource(), ArgumentTypeString.getString(ctx, "id")))))
						.then(ArgumentBuilderLiteral.<CommandSource>literal("adduser")
							.then(ArgumentBuilderRequired.<CommandSource, String>argument("id", ArgumentTypeString.word())
								.then(ArgumentBuilderRequired.<CommandSource, String>argument("username", ArgumentTypeString.word())
									.executes(ctx -> executeAdminAddUser(ctx.getSource(),
										ArgumentTypeString.getString(ctx, "id"),
										ArgumentTypeString.getString(ctx, "username")))))))
			)
		);
	}

	private int executeSet(CommandSource source, String id) {
		SkinProfile profile = SkinIdRegistry.resolve(id);
		if (profile == null) {
			source.sendMessage("§eSkin with id '" + id + "' doesn't exist. Use /rsl skin list.");
			return 0;
		}
		Player player = source.getSender();
		PlayerSkinChoices.set(player.username, id);
		ServerProfileProvider.update(player.username, profile);

		// Reinvoking GetSkinUrlThread so it runs the full pipeline
		// (parsing + whatever that registers the texutre for the renderer)
		// not just field change.
		new net.minecraft.core.util.helper.GetSkinUrlThread(player);

		byte[] payload = RslProfilePayload.encodeSet(player.id, player.username, profile);
		source.sendPacketToAllPlayers(() -> new PacketCustomPayload(RslProfilePayload.CHANNEL, payload));
		source.sendMessage("§4Skin changed to '" + id + "'.");
		return 1;
	}

	private int executeClear(CommandSource source) {
		Player player = source.getSender();
		PlayerSkinChoices.clear(player.username);
		ServerProfileProvider.clear(player.username);

		// Guaranteed visible reset, whatever happens with the backup
		// resolution (that can perfectly fail smh).
		player.skinURL = null;
		player.capeURL = null;
		player.slimModel = false;

		// Reinvoking the actual GetSkinUrlThread to reset changes
		new net.minecraft.core.util.helper.GetSkinUrlThread(player);

		byte[] payload = RslProfilePayload.encodeClear(player.id, player.username);
		source.sendPacketToAllPlayers(() -> new PacketCustomPayload(RslProfilePayload.CHANNEL, payload));
		source.sendMessage("§4Went back to your original skin.");
		return 1;
	}

	private int executeList(CommandSource source) {
		source.sendMessage("Available skins: " + String.join(", ", SkinIdRegistry.availableIds()));
		return 1;
	}
	// I KNOW BOOLEAN METHOD CALLS ARE INVERTED STFU INTELLIJ 🥀
	private static boolean isValidUrl(String value) {
		try {
			java.net.URI uri = new java.net.URI(value);
			return uri.getHost() != null && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
		} catch (Exception exception) {
			return false;
		}
	}

	private int executeAdminAdd(CommandSource source, String id, String skinUrl, String capeUrl, String model) {
		if (!isValidUrl(skinUrl) || (capeUrl != null && !isValidUrl(capeUrl))) {
			source.sendMessage("§eThat's not a valid URL http(s). If you want to add via username, use /rsl skin admin adduser.");
			return 0;
		}

		String skinModel = (model != null && (model.equals("slim") || model.equals("default"))) ? model : "default";
		SkinIdRegistry.put(id, new SkinProfile(skinUrl, capeUrl, skinModel));

		source.sendMessage("§4Added skin '" + id + "' to the skin pool");
		return 1;
	}

	private int executeAdminRemove(CommandSource source, String id) {
		boolean removed = SkinIdRegistry.remove(id);
		source.sendMessage(removed
			? "§4Skin '" + id + "' deleted from pool."
			: "§eThere was no skin with id '" + id + "'.");
		return 1;
	}

	private int executeAdminAddUser(CommandSource source, String id, String username) {
		SkinProfile profile = new MojangSkinProvider().load(username);
		String sourceName = "Mojang";

		if (profile == null) {
			profile = new ElyBySkinProvider(null).load(username);
			sourceName = "Ely.by";
		}

		if (profile == null) {
			source.sendMessage("§eCouldn't find texture for '" + username + "' on Mojang nor Ely.by.");
			return 0;
		}
		SkinIdRegistry.put(id, profile);
		source.sendMessage("§4Added '" + id + "' solved from " + sourceName + " (" + username + ") to pool.");
		return 1;
	}
}
