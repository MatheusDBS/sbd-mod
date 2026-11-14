package dev.skyblockdailies;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// /sbd sellcheck: is the held item's price high or low compared to the last 30 days (Coflnet daily averages)?
final class SellCheck {
	private static final String HISTORY = "https://sky.coflnet.com/api/item/price/%s/history/month";
	private static final int MIN_DAYS = 5;

	// Oldest -> newest (Coflnet returns auction and bazaar items in different orders).
	// field: "avg" for Bazaar items; "min" for auction items, because "now" is the lowest BIN and
	// the daily average mixes in upgraded copies (a 3-star Hyperion looked "cheap" against it).
	static List<Double> daily(String json, String field) {
		List<JsonObject> days = new ArrayList<>();
		for (JsonElement e : JsonParser.parseString(json).getAsJsonArray()) {
			JsonObject o = e.getAsJsonObject();
			if (o.has(field) && o.has("time") && o.get(field).getAsDouble() > 0) days.add(o);
		}
		days.sort(Comparator.comparing(o -> o.get("time").getAsString()));
		List<Double> values = days.stream().map(o -> o.get(field).getAsDouble()).toList();
		// drop days far off the median: "lowest sale" includes 10-coin coop transfers and mistyped BINs
		List<Double> sorted = values.stream().sorted().toList();
		double median = sorted.isEmpty() ? 0 : sorted.get(sorted.size() / 2);
		return values.stream().filter(v -> v >= median * 0.2 && v <= median * 5).toList();
	}

	static List<String> lines(String name, List<Double> avgs, double now, boolean bazaar) {
		List<String> out = new ArrayList<>();
		out.add("§6§lSell or hold: §f" + name + " §8(" + (bazaar ? "Bazaar, daily average" : "AH, lowest sale per day") + ")");
		if (avgs.size() < MIN_DAYS) {
			out.add("§8Not enough price history (" + avgs.size() + " days).");
			return out;
		}
		double mean = avgs.stream().mapToDouble(d -> d).average().orElse(0);
		double min = avgs.stream().mapToDouble(d -> d).min().orElse(0), max = avgs.stream().mapToDouble(d -> d).max().orElse(0);
		double pct = avgs.stream().filter(d -> d <= now).count() / (double) avgs.size();
		out.add("§7Now: §6" + Crafts.fmt(now) + " §8| §730d mean: §6" + Crafts.fmt(mean) + " §8| §7range: §6" + Crafts.fmt(min) + "§7-§6" + Crafts.fmt(max));

		if (avgs.size() >= 14) {
			int n = avgs.size();
			double last = avgs.subList(n - 7, n).stream().mapToDouble(d -> d).average().orElse(0);
			double prev = avgs.subList(n - 14, n - 7).stream().mapToDouble(d -> d).average().orElse(0);
			double change = prev > 0 ? (last - prev) / prev * 100 : 0;
			out.add("§77-day trend: " + (change >= 0 ? "§a▲ +" : "§c▼ ") + Math.round(change) + "%");
		}

		String verdict = pct >= 0.7 ? "§aPrice is high for this month → good time to sell"
				: pct <= 0.3 ? "§cPrice is low for this month → better to hold"
				: "§ePrice is around average → no strong signal";
		out.add(verdict + " §8(higher than " + Math.round(pct * 100) + "% of days)");
		out.add("§8Based on Coflnet price history (last 30 days) · not a prediction");
		return out;
	}

	static int command(FabricClientCommandSource src) {
		var player = src.getClient().player;
		String id = player == null ? null : Crafts.skyblockId(player.getMainHandItem());
		if (id == null) {
			src.sendError(Component.literal("Hold a SkyBlock item and run /sbd sellcheck."));
			return 0;
		}
		src.sendFeedback(Component.literal("§7Fetching price history..."));
		CompletableFuture.runAsync(() -> {
			List<String> lines;
			try {
				Crafts.ensurePricesNow();
				boolean bazaar = Crafts.bazaarIds.contains(id);
				List<Double> days = daily(Crafts.fetch(String.format(HISTORY, id)), bazaar ? "avg" : "min");
				double now = Crafts.sell.getOrDefault(id, days.isEmpty() ? 0 : days.get(days.size() - 1));
				lines = lines(Crafts.names.getOrDefault(id, id), days, now, bazaar);
			} catch (Exception e) {
				lines = List.of("§cFailed to fetch price history: " + e.getMessage());
			}
			List<String> done = lines;
			src.getClient().execute(() -> done.forEach(l -> src.sendFeedback(Component.literal(l))));
		});
		return 1;
	}
}
