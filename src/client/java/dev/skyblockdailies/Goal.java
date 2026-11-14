package dev.skyblockdailies;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Session goal: "/sbd goal 10M purse" tracks how much a sidebar/tab number grew since the goal was set.
// Not saved on purpose: it's a per-session goal.
final class Goal {
	private static final Pattern LINE = Pattern.compile("^\\s*([A-Za-z][A-Za-z' ]*?):\\s*([\\d,]+(?:\\.\\d+)?)\\s*([kmb])?\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern AMOUNT = Pattern.compile("^([\\d.,]+)\\s*([kmb])?$", Pattern.CASE_INSENSITIVE);
	private static final int BAR = 10;

	// The sidebar rounds the bank ("Bank: 4B"), so a 10M deposit is invisible there; bank goals count chat messages instead.
	private static final Pattern BANK_MOVE = Pattern.compile("^You have (deposited|withdrawn) ([\\d.,]+[kmb]?) coins!", Pattern.CASE_INSENSITIVE);
	private static final Pattern BANK_INTEREST = Pattern.compile("(?:received|earned) ([\\d.,]+[kmb]?) coins as interest", Pattern.CASE_INSENSITIVE);

	static String label; // lowercase, as found in Area.numbers
	static double start, target;
	private static double bankDelta;
	private static boolean reached;

	static void onChat(String plain) {
		if (!"bank".equals(label)) return;
		Matcher m = BANK_MOVE.matcher(plain);
		if (m.find()) {
			double v = parseAmount(m.group(2));
			if (v > 0) bankDelta += m.group(1).equalsIgnoreCase("deposited") ? v : -v;
			return;
		}
		m = BANK_INTEREST.matcher(plain);
		if (m.find() && parseAmount(m.group(1)) > 0) bankDelta += parseAmount(m.group(1));
	}

	/** Parses "Purse: 1,234,567 (+5)" / "Mithril: 12.5k" into the map. */
	static void putNumber(Map<String, Double> out, String line) {
		Matcher m = LINE.matcher(line);
		if (!m.find()) return;
		double v = Double.parseDouble(m.group(2).replace(",", "")) * mult(m.group(3));
		out.putIfAbsent(m.group(1).trim().toLowerCase(Locale.ROOT), v);
	}

	/** "10M", "2.5k", "1,000" -> number; -1 if invalid. */
	static double parseAmount(String s) {
		Matcher m = AMOUNT.matcher(s.trim());
		if (!m.matches()) return -1;
		try {
			double v = Double.parseDouble(m.group(1).replace(",", "")) * mult(m.group(2));
			return v > 0 ? v : -1;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static double mult(String suffix) {
		if (suffix == null) return 1;
		return switch (suffix.toLowerCase(Locale.ROOT)) {
			case "k" -> 1e3;
			case "m" -> 1e6;
			default -> 1e9;
		};
	}

	/** Progress so far, or NaN if the number isn't visible right now (e.g. powder only shows in the mines). */
	static double progress() {
		if ("bank".equals(label)) return bankDelta;
		Double now = Area.numbers.get(label);
		return now == null ? Double.NaN : now - start;
	}

	static String hudLine() {
		if (label == null) return null;
		double p = progress();
		if (Double.isNaN(p)) return "§6Goal §7» §f" + label + " §8(not visible here)";
		double frac = Math.max(0, Math.min(1, p / target));
		int filled = (int) Math.round(frac * BAR);
		return "§6Goal §7» §f" + label + " §a" + Crafts.fmt(p) + "§7/§f" + Crafts.fmt(target)
				+ " §a" + "|".repeat(filled) + "§8" + "|".repeat(BAR - filled) + " §7" + Math.round(frac * 100) + "%";
	}

	/** Called once per second: shows a title once when the goal is reached. */
	static void tick(Minecraft mc) {
		if (label == null || reached || !(progress() >= target)) return;
		reached = true;
		mc.gui.hud.setTimes(10, 80, 20);
		mc.gui.hud.setTitle(Component.literal("§a§lGoal reached!"));
		mc.gui.hud.setSubtitle(Component.literal("§f+" + Crafts.fmt(progress()) + " " + label));
		SkyblockDailies.say(mc, "§6[Goal] §aReached: §f+" + Crafts.fmt(progress()) + " " + label);
	}

	static int set(FabricClientCommandSource src, String amount, String name) {
		double t = parseAmount(amount);
		if (t < 0) {
			src.sendError(Component.literal("Invalid amount \"" + amount + "\". Use e.g. 10M, 250k, 5000."));
			return 0;
		}
		String key = name.trim().toLowerCase(Locale.ROOT);
		Double now = Area.numbers.get(key);
		if (now == null) {
			src.sendError(Component.literal("Couldn't find \"" + name + ":\" on your sidebar or tab list right now. Visible: "
					+ (Area.numbers.isEmpty() ? "nothing (are you in SkyBlock?)" : String.join(", ", Area.numbers.keySet()))));
			return 0;
		}
		label = key;
		start = now;
		target = t;
		bankDelta = 0;
		reached = false;
		src.sendFeedback(Component.literal("§6[Goal] §aSet: §f+" + Crafts.fmt(t) + " " + key + " §7(starting at " + Crafts.fmt(now) + ")"));
		return 1;
	}

	static int status(FabricClientCommandSource src) {
		if (label == null) {
			src.sendFeedback(Component.literal("§eUsage: §f/sbd goal <amount> <name>  §7e.g. /sbd goal 10M purse, /sbd goal 100k mithril"));
			src.sendFeedback(Component.literal("§7Works with any \"Name: number\" on your sidebar or tab list. §f/sbd goal clear §7removes it."));
			return 1;
		}
		src.sendFeedback(Component.literal(hudLine()));
		return 1;
	}

	static int clear(FabricClientCommandSource src) {
		label = null;
		src.sendFeedback(Component.literal("§6[Goal] §7Cleared."));
		return 1;
	}
}
