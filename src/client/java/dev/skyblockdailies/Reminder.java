package dev.skyblockdailies;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Aviso na tela ~15s depois de entrar no SkyBlock, se houver dailies pendentes. */
final class Reminder {
	static final int DELAY_SECONDS = 15;
	// Only re-arms after 5 min outside SkyBlock. Not on connection JOIN: every Hypixel lobby/island switch
	// re-joins through the proxy, which made the notice show on every switch.
	static final int AWAY_SECONDS = 5 * 60;

	private static boolean armed = true;
	private static int inSeconds, outSeconds;

	/** Called once per second, after Area.update. */
	static void tick(Minecraft mc) {
		if (step(Area.inSkyblock)) show(mc);
	}

	/** Advances one second; true when the notice should show now. */
	static boolean step(boolean in) {
		if (!in) {
			if (++outSeconds >= AWAY_SECONDS) {
				armed = true;
				inSeconds = 0;
			}
			return false;
		}
		outSeconds = 0;
		if (armed && ++inSeconds >= DELAY_SECONDS) {
			armed = false;
			return true;
		}
		return false;
	}


	private static void show(Minecraft mc) {
		List<Tasks.Task> daily = SkyblockDailies.visible().stream().filter(SkyblockDailies::isDaily).toList();
		long pending = daily.stream().filter(t -> !SkyblockDailies.isDone(t)).count();
		if (pending == 0) return;
		var hud = mc.gui.hud;
		hud.setTimes(10, 100, 20); // entra 0,5s, fica 5s, sai 1s
		hud.setTitle(Component.literal("§6§lDo your dailies!"));
		hud.setSubtitle(Component.literal("§e" + pending + " pending §7• press §fK §7or §f/sbd dailies"));
		hud.getChat().addClientSystemMessage(Component.literal(
				"§6[Dailies] §eYou have §f" + pending + "§e of " + daily.size() + " tasks pending. §7Press §fK§7 or type §f/sbd dailies§7."));
	}
}
