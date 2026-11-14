package dev.skyblockdailies;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;

import java.util.Locale;

final class Area {
	static boolean inSkyblock;
	static String area = "";
	static String section;

	static void update(Minecraft mc) {
		inSkyblock = inSkyblock(mc);
		area = inSkyblock ? tabArea(mc) : "";
		section = sectionOf(area);
	}

	// Hypixel puts "SKYBLOCK" in the sidebar title (also "SKYBLOCK CO-OP" / "SKYBLOCK GUEST").
	static boolean inSkyblock(Minecraft mc) {
		if (mc.level == null) return false;
		Objective obj = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
		return obj != null && strip(obj.getDisplayName().getString()).toUpperCase(Locale.ROOT).contains("SKYBLOCK");
	}

	private static String tabArea(Minecraft mc) {
		if (mc.getConnection() == null) return "";
		for (PlayerInfo p : mc.getConnection().getListedOnlinePlayers()) {
			Component name = p.getTabListDisplayName();
			if (name == null) continue;
			String s = strip(name.getString()).trim();
			if (s.startsWith("Area:") || s.startsWith("Dungeon:")) return s.substring(s.indexOf(':') + 1).trim();
		}
		return "";
	}

	static String sectionOf(String area) {
		String a = area.toLowerCase(Locale.ROOT);
		if (a.isEmpty()) return null;
		if (a.contains("garden")) return "Garden";
		if (a.contains("private island")) return "Private Island";
		if (a.contains("crimson") || a.contains("kuudra")) return "Crimson Isle";
		if (a.contains("dwarven") || a.contains("glacite") || a.contains("mineshaft") || a.contains("crystal hollows")) return "Dwarven Mines";
		if (a.contains("dungeon") || a.contains("catacombs")) return "Dungeons";
		if (a.contains("galatea") || a.contains("moonglade")) return "Galatea";
		if (a.contains("rift")) return "Rift";
		if (a.equals("hub")) return "Hub";
		return null;
	}

	static String strip(String s) {
		return s.replaceAll("§.", "");
	}
}
