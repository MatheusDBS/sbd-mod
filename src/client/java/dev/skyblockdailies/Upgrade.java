package dev.skyblockdailies;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

// /sbd upgrade: coins needed for the next essence stars and the next enchant levels of the held item.
final class Upgrade {
	/** Cost of one star level, or NaN if some price is unknown. */
	static double starCost(JsonObject e, int level, Map<String, Double> buy) {
		if (!e.has(String.valueOf(level))) return Double.NaN;
		Double ess = buy.get("ESSENCE_" + e.get("type").getAsString().toUpperCase(Locale.ROOT));
		if (ess == null) return Double.NaN;
		double cost = e.get(String.valueOf(level)).getAsDouble() * ess;
		JsonObject items = e.has("items") ? e.getAsJsonObject("items") : new JsonObject();
		if (items.has(String.valueOf(level))) {
			for (JsonElement it : items.getAsJsonArray(String.valueOf(level))) {
				var ing = Crafts.parseIng(it.getAsString());
				Double p = buy.get(ing.getKey());
				if (p == null) return Double.NaN;
				cost += p * ing.getValue();
			}
		}
		return cost;
	}

	static int maxStars(JsonObject e) {
		int max = 0;
		for (String k : e.keySet()) if (k.matches("\\d+")) max = Math.max(max, Integer.parseInt(k));
		return max;
	}

	static List<String> lines(String id, int stars, Map<String, Integer> enchants, Map<String, Double> buy, Map<String, String> names, JsonObject essences) {
		List<String> out = new ArrayList<>();
		out.add("§6§lUpgrade: §f" + names.getOrDefault(id, id) + (stars > 0 ? " §6" + "✪".repeat(stars) : ""));

		if (essences.has(id)) {
			JsonObject e = essences.getAsJsonObject(id);
			int max = maxStars(e);
			String type = e.get("type").getAsString();
			if (stars >= max) {
				out.add("§7Stars: §amaxed §8(" + max + ")");
			} else {
				double next = starCost(e, stars + 1, buy);
				out.add("§7Next star (" + (stars + 1) + "): §6" + (Double.isNaN(next) ? "?" : Crafts.fmt(next))
						+ " §8(" + e.get(String.valueOf(stars + 1)).getAsInt() + " " + type + " Essence)");
				double total = 0;
				for (int lv = stars + 1; lv <= max; lv++) total += starCost(e, lv, buy);
				out.add("§7To max (" + max + "): §6" + (Double.isNaN(total) ? "?" : Crafts.fmt(total)));
			}
		} else {
			out.add("§8No essence stars for this item.");
		}

		List<String> books = new ArrayList<>();
		for (var en : new TreeMap<>(enchants).entrySet()) {
			String book = "ENCHANTMENT_" + en.getKey().toUpperCase(Locale.ROOT) + "_" + (en.getValue() + 1);
			Double p = buy.get(book);
			if (p != null) books.add("§8 - §7" + pretty(en.getKey()) + " " + en.getValue() + " → " + (en.getValue() + 1) + ": §6" + Crafts.fmt(p));
		}
		if (!books.isEmpty()) {
			out.add("§7Next enchant levels (book price):");
			out.addAll(books);
		} else if (!enchants.isEmpty()) {
			out.add("§8No next-level enchant books on the Bazaar.");
		}
		out.add("§8Master stars and reforges not included · no taxes");
		return out;
	}

	private static String pretty(String enchant) {
		StringBuilder sb = new StringBuilder();
		for (String w : enchant.split("_")) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
		return sb.toString().trim();
	}

	static int command(FabricClientCommandSource src) {
		var player = src.getClient().player;
		ItemStack stack = player == null ? ItemStack.EMPTY : player.getMainHandItem();
		String id = Crafts.skyblockId(stack);
		if (id == null) {
			src.sendError(Component.literal("Hold a SkyBlock item and run /sbd upgrade."));
			return 0;
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
		int stars = tag.getInt("upgrade_level").or(() -> tag.getInt("dungeon_item_level")).orElse(0);
		Map<String, Integer> enchants = new TreeMap<>();
		CompoundTag ench = tag.getCompoundOrEmpty("enchantments");
		for (String k : ench.keySet()) enchants.put(k, ench.getIntOr(k, 0));

		src.sendFeedback(Component.literal("§7Fetching prices..."));
		CompletableFuture.runAsync(() -> {
			List<String> lines;
			try {
				Crafts.ensurePricesNow();
				lines = lines(id, stars, enchants, Crafts.buy, Crafts.names, Crafts.essenceCosts);
			} catch (Exception e) {
				lines = List.of("§cFailed to fetch prices: " + e.getMessage());
			}
			List<String> done = lines;
			src.getClient().execute(() -> done.forEach(l -> src.sendFeedback(Component.literal(l))));
		});
		return 1;
	}
}
