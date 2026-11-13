package dev.skyblockdailies;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Forge lines for the item tooltip (forge time, material cost, sell value, profit per forge hour) and /bestforge. */
final class Forge {
	private static final int MAX_MATERIALS = 8;

	record Ranked(Crafts.Recipe r, double cost, double value, double profit, double perHour) {}

	/** Forge recipes with all prices known, sorted by profit per forge hour. */
	static List<Ranked> best(java.util.Collection<Crafts.Recipe> forges, int top, Map<String, Double> buy, Map<String, Double> sell) {
		List<Ranked> out = new ArrayList<>();
		for (Crafts.Recipe r : forges) {
			Double unit = sell.get(r.out());
			if (unit == null || !buy.keySet().containsAll(r.inputs().keySet())) continue;
			double cost = r.inputs().entrySet().stream().mapToDouble(e -> buy.get(e.getKey()) * e.getValue()).sum();
			double value = unit * r.count(), profit = value - cost;
			out.add(new Ranked(r, cost, value, profit, profit / (Math.max(60, r.duration()) / 3600.0)));
		}
		out.sort(java.util.Comparator.comparingDouble(Ranked::perHour).reversed());
		return out.subList(0, Math.min(top, out.size()));
	}

	static int command(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource src) {
		if (Crafts.forgeByOut.isEmpty()) {
			src.sendError(net.minecraft.network.chat.Component.literal("Recipes still loading (or launch once with SkyHanni/Skyblocker so it downloads the NEU repo)."));
			return 0;
		}
		src.sendFeedback(net.minecraft.network.chat.Component.literal("§7Fetching prices..."));
		java.util.concurrent.CompletableFuture.runAsync(() -> {
			List<String> lines = new ArrayList<>();
			try {
				Crafts.ensurePricesNow();
				List<Ranked> res = best(Crafts.forgeByOut.values(), 10, Crafts.buy, Crafts.sell);
				lines.add("§6§lBest forge items §7(profit per forge hour)");
				int i = 1;
				for (Ranked x : res) {
					lines.add("§e" + i++ + ". §f" + Crafts.names.getOrDefault(x.r().out(), x.r().out())
							+ " §a" + Crafts.fmt(x.perHour()) + "/h §8(profit " + Crafts.fmt(x.profit()) + " in "
							+ SkyblockDailies.fmt(Duration.ofSeconds(Math.max(60, x.r().duration()))) + ", cost " + Crafts.fmt(x.cost()) + ")");
				}
				lines.add("§8Ignores HotM/quest requirements · no taxes · hover an item for its materials");
			} catch (Exception e) {
				lines.add("§cFailed to fetch prices: " + e.getMessage());
			}
			src.getClient().execute(() -> lines.forEach(l -> src.sendFeedback(net.minecraft.network.chat.Component.literal(l))));
		});
		return 1;
	}

	/** Tooltip lines; "?" where a price is unknown (prices still loading or item not sold anywhere). */
	static List<String> lines(Crafts.Recipe r, Map<String, Double> buy, Map<String, Double> sell, Map<String, String> names) {
		List<String> out = new ArrayList<>();
		out.add("");
		out.add("§6§lForge §7(" + SkyblockDailies.fmt(Duration.ofSeconds(Math.max(60, r.duration()))) + ")");

		boolean known = true;
		double cost = 0;
		List<String> mats = new ArrayList<>();
		for (var e : r.inputs().entrySet()) {
			Double p = buy.get(e.getKey());
			if (p == null) known = false;
			else cost += p * e.getValue();
			mats.add("§8 - §7" + Crafts.fmt(e.getValue()) + "x " + names.getOrDefault(e.getKey(), e.getKey())
					+ (p == null ? " §8(?)" : " §8(" + Crafts.fmt(p * e.getValue()) + ")"));
		}
		Double unit = sell.get(r.out());
		double value = unit == null ? 0 : unit * r.count();

		out.add("§7Cost: §6" + (known ? Crafts.fmt(cost) : "?") + " §8| §7Sells: §6" + (unit == null ? "?" : Crafts.fmt(value)));
		if (known && unit != null) {
			double profit = value - cost;
			double hours = Math.max(60, r.duration()) / 3600.0;
			String c = profit >= 0 ? "§a" : "§c";
			out.add("§7Profit: " + c + Crafts.fmt(profit) + " §8(" + c + Crafts.fmt(profit / hours) + "/h§8)");
		} else if (buy.isEmpty()) {
			out.add("§8Loading prices...");
		}
		out.add("§7Materials:");
		out.addAll(mats.subList(0, Math.min(MAX_MATERIALS, mats.size())));
		if (mats.size() > MAX_MATERIALS) out.add("§8 ...+" + (mats.size() - MAX_MATERIALS));
		return out;
	}
}
