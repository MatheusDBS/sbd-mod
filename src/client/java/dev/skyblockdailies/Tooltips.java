package dev.skyblockdailies;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Cached per item until prices change, so hovering is a map lookup per frame.
final class Tooltips {
	private static final Pattern ACTION_TIME = Pattern.compile("Time Between Actions: ([\\d.]+)s");
	private static final Pattern MINION = Pattern.compile("^(.+_(GENERATOR|MINION))_\\d+$");
	private static final Map<String, List<String>> cache = new HashMap<>();
	private static long cacheVersion = -1;

	static void append(ItemStack stack, List<Component> tooltip) {
		String id = Crafts.skyblockId(stack);
		if (id == null || Crafts.recipes.isEmpty()) return;
		Crafts.refreshPricesAsync();
		if (cacheVersion != Crafts.pricesVersion) {
			cache.clear();
			cacheVersion = Crafts.pricesVersion;
		}
		List<String> lines = cache.get(id);
		if (lines == null) {
			lines = build(id, actionSeconds(tooltip));
			cache.put(id, lines);
		}
		lines.forEach(l -> tooltip.add(Component.literal(l)));
	}

	private static double actionSeconds(List<Component> tooltip) {
		for (Component c : tooltip) {
			Matcher m = ACTION_TIME.matcher(Area.strip(c.getString()));
			if (m.find()) return Double.parseDouble(m.group(1));
		}
		return 0;
	}

	static List<String> build(String id, double minionActionSeconds) {
		Map<String, Double> buy = Crafts.buy, sell = Crafts.sell, npc = Crafts.npcSell;
		Map<String, String> names = Crafts.names;
		List<String> out = new ArrayList<>();
		if (buy.isEmpty()) return out; // prices not loaded yet; next price update clears the cache

		Crafts.Recipe forge = Crafts.forgeByOut.get(id);
		if (forge != null) out.addAll(Forge.lines(forge, buy, sell, names));

		String craft = craftLine(Crafts.craftByOut.get(id), buy, sell);
		String market = marketLine(id, sell, npc);
		String used = usedInLine(id, buy, sell, names);
		String minion = minionLine(id, minionActionSeconds, sell, npc, names);
		if (craft != null || market != null || used != null || minion != null) {
			if (out.isEmpty()) out.add("");
			for (String l : new String[]{craft, market, used, minion}) if (l != null) out.add(l);
		}
		return out;
	}

	static String craftLine(Crafts.Recipe r, Map<String, Double> buy, Map<String, Double> sell) {
		if (r == null) return null;
		Double price = buy.get(r.out());
		double cost = 0;
		for (var e : r.inputs().entrySet()) {
			Double p = buy.get(e.getKey());
			if (p == null) return null; // vanilla/NPC ingredient without price
			cost += p * e.getValue();
		}
		double unitCost = cost / r.count();
		if (price == null) return "§6Craft: §7cost §6" + Crafts.fmt(unitCost) + " §8(no market price)";
		String verdict = unitCost < price ? "§acraft saves " + Crafts.fmt(price - unitCost) : "§ebuy saves " + Crafts.fmt(unitCost - price);
		return "§6Craft: §7cost §6" + Crafts.fmt(unitCost) + " §8| §7buy §6" + Crafts.fmt(price) + " §8→ " + verdict;
	}

	static String marketLine(String id, Map<String, Double> sell, Map<String, Double> npc) {
		Double n = npc.get(id), m = sell.get(id);
		if (n == null || n <= 0) return null;
		if (m == null) return "§6Sell: §7NPC §6" + Crafts.fmt(n);
		String best = n >= m ? "§aNPC" : "§amarket";
		return "§6Sell: §7NPC §6" + Crafts.fmt(n) + " §8| §7market §6" + Crafts.fmt(m) + " §8→ " + best;
	}

	static String usedInLine(String id, Map<String, Double> buy, Map<String, Double> sell, Map<String, String> names) {
		List<Crafts.Recipe> rs = Crafts.usedInIndex.get(id);
		if (rs == null || rs.isEmpty()) return null;
		List<Crafts.Result> best = Crafts.bestCrafts(rs, id, 1, buy, sell);
		String head = "§6Used in §f" + rs.size() + " §6recipe" + (rs.size() == 1 ? "" : "s");
		if (best.isEmpty()) return head;
		Crafts.Result b = best.get(0);
		String c = b.profit() >= 0 ? "§a+" : "§c";
		return head + " §8| §7best: §f" + names.getOrDefault(b.r().out(), b.r().out()) + " " + c + Crafts.fmt(b.profit());
	}

	// Base output only: 2 actions per resource (place + break / spawn + kill),
	// resource = main ingredient of the tier I recipe. No fuel, upgrades or extra drops.
	static String minionLine(String id, double actionSeconds, Map<String, Double> sell, Map<String, Double> npc, Map<String, String> names) {
		Matcher m = MINION.matcher(id);
		if (!m.matches() || actionSeconds <= 0) return null;
		String resource = mainIngredient(Crafts.craftByOut.get(m.group(1) + "_1"));
		if (resource == null) return null;
		double perDay = 86400 / (actionSeconds * 2);
		double price = Math.max(sell.getOrDefault(resource, 0.0), npc.getOrDefault(resource, 0.0));
		return "§6Minion: §7~" + Crafts.fmt(perDay) + " " + names.getOrDefault(resource, resource) + "/day"
				+ (price > 0 ? " §8≈ §6" + Crafts.fmt(perDay * price) + " coins/day" : "") + " §8(base)";
	}

	static String mainIngredient(Crafts.Recipe r) {
		if (r == null) return null;
		return r.inputs().entrySet().stream()
				.filter(e -> !e.getKey().matches(".*(PICKAXE|_AXE|SHOVEL|HOE|SWORD|BOW|FISHING_ROD|SHEARS)$"))
				.max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey).orElse(null);
	}
}
