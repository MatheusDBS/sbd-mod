package dev.skyblockdailies;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

final class Crafts {
	record Recipe(String out, double count, Map<String, Double> inputs, String type, long duration) {}

	record Result(Recipe r, double cost, double value, double profit) {}

	private static final String BAZAAR = "https://api.hypixel.net/v2/skyblock/bazaar"; // public endpoints, no API key needed
	private static final String LOWEST_BIN = "https://sky.coflnet.com/api/prices/neu";
	private static final long PRICE_TTL_MS = 5 * 60_000;
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	private static final String[] GRID = {"A1", "A2", "A3", "B1", "B2", "B3", "C1", "C2", "C3"};

	static volatile List<Recipe> recipes = List.of();
	static volatile Map<String, String> names = Map.of();
	private static volatile Map<String, String> usedByName = Map.of(); // lowercase name -> id, only items used in recipes
	static volatile Map<String, Double> buy = Map.of(), sell = Map.of();
	static volatile Map<String, Recipe> forgeByOut = Map.of();
	static volatile Map<String, Recipe> craftByOut = Map.of();
	static volatile Map<String, List<Recipe>> usedInIndex = Map.of();
	static volatile Map<String, Double> npcSell = Map.of();
	static volatile JsonObject essenceCosts = new JsonObject(); // NEU constants/essencecosts.json: star costs per item
	static volatile long pricesVersion; // bumped on every price update; tooltips use it as cache key
	static volatile Set<String> bazaarIds = Set.of();
	private static final String ITEMS = "https://api.hypixel.net/v2/resources/skyblock/items";
	private static long pricesAt;
	private static volatile boolean refreshing;


	static void loadRepoAsync() {
		CompletableFuture.runAsync(() -> {
			try {
				loadRepo();
			} catch (Exception e) {
				System.err.println("[SkyblockDailies] falha ao ler receitas: " + e);
			}
		});
	}

	private static Path repoDir() {
		Path game = FabricLoader.getInstance().getGameDir(), cfg = FabricLoader.getInstance().getConfigDir();
		return Stream.of(game.resolve("skyhanni/neurepo/items"), cfg.resolve("skyblocker/item-repo/items"), cfg.resolve("notenoughupdates/repo/items"))
				.filter(Files::isDirectory)
				.max(Comparator.comparingLong(p -> p.toFile().lastModified()))
				.orElse(null);
	}

	private static void loadRepo() throws IOException {
		Path dir = repoDir();
		if (dir == null) {
			System.out.println("[SkyblockDailies] NEU repo not found; /sbd bestcraft has no recipes");
			return;
		} // shortcut: relies on the repo SkyHanni/Skyblocker download; fetch the NEU zip if that stops being true
		loadRepoFrom(dir);
	}

	static void loadRepoFrom(Path dir) throws IOException {
		List<Recipe> rs = new ArrayList<>();
		Map<String, String> ns = new HashMap<>();
		try (Stream<Path> files = Files.list(dir)) {
			for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
				try {
					JsonObject d = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
					String id = d.get("internalname").getAsString();
					ns.put(id, d.has("displayname") ? d.get("displayname").getAsString().replaceAll("§.", "") : id);
					List<JsonObject> raw = new ArrayList<>();
					if (d.has("recipes")) d.getAsJsonArray("recipes").forEach(e -> raw.add(e.getAsJsonObject()));
					if (d.has("recipe")) raw.add(d.getAsJsonObject("recipe"));
					for (JsonObject r : raw) {
						Recipe p = parseRecipe(id, r);
						if (p != null && !p.inputs().isEmpty()) rs.add(p);
					}
				} catch (Exception ignored) {
				}
			}
		}
		Map<String, String> used = new HashMap<>();
		for (Recipe r : rs) for (String i : r.inputs().keySet()) used.putIfAbsent(ns.getOrDefault(i, i).toLowerCase(Locale.ROOT), i);
		Map<String, Recipe> forge = new HashMap<>(), craft = new HashMap<>();
		Map<String, List<Recipe>> usedIn = new HashMap<>();
		for (Recipe r : rs) {
			(r.type().equals("forge") ? forge : craft).putIfAbsent(r.out(), r);
			for (String i : r.inputs().keySet()) usedIn.computeIfAbsent(i, k -> new ArrayList<>()).add(r);
		}
		forgeByOut = forge;
		craftByOut = craft;
		Path ess = dir.getParent().resolve("constants/essencecosts.json");
		if (Files.exists(ess)) essenceCosts = JsonParser.parseString(Files.readString(ess)).getAsJsonObject();
		usedInIndex = usedIn;
		names = ns;
		usedByName = used;
		recipes = rs;
		System.out.println("[SkyblockDailies] " + rs.size() + " recipes loaded from " + dir);
	}

	static Map.Entry<String, Double> parseIng(String s) {
		int c = s.lastIndexOf(':');
		if (c > 0) {
			try {
				return Map.entry(s.substring(0, c), Double.parseDouble(s.substring(c + 1)));
			} catch (NumberFormatException ignored) {
			}
		}
		return Map.entry(s, 1.0); // some recipes omit ":1"
	}

	static Recipe parseRecipe(String out, JsonObject r) {
		String type = r.has("type") ? r.get("type").getAsString() : "crafting";
		List<String> parts = new ArrayList<>();
		if (type.equals("forge")) {
			for (JsonElement e : r.getAsJsonArray("inputs")) parts.add(e.getAsString());
		} else if (type.equals("crafting")) {
			for (String k : GRID) if (r.has(k) && !r.get(k).getAsString().isEmpty()) parts.add(r.get(k).getAsString());
		} else {
			return null; // npc_shop, drops, trades... aren't crafts
		}
		Map<String, Double> inputs = new LinkedHashMap<>();
		for (String p : parts) {
			var e = parseIng(p);
			inputs.merge(e.getKey(), e.getValue(), Double::sum);
		}
		double count = r.has("count") ? r.get("count").getAsDouble() : 1;
		return new Recipe(r.has("overrideOutputId") ? r.get("overrideOutputId").getAsString() : out,
				count > 0 ? count : 1, inputs, type, r.has("duration") ? r.get("duration").getAsLong() : 0);
	}


	// Non-blocking refresh for tooltips, which must never stall rendering.
	static void refreshPricesAsync() {
		if (refreshing || System.currentTimeMillis() - pricesAt < PRICE_TTL_MS) return;
		refreshing = true;
		CompletableFuture.runAsync(() -> {
			try {
				ensurePrices();
			} catch (Exception e) {
				System.err.println("[SkyblockDailies] price refresh failed: " + e);
			} finally {
				refreshing = false;
			}
		});
	}

	static void ensurePricesNow() throws IOException, InterruptedException {
		ensurePrices();
	}

	private static synchronized void ensurePrices() throws IOException, InterruptedException {
		if (System.currentTimeMillis() - pricesAt < PRICE_TTL_MS) return;
		JsonObject bins = get(LOWEST_BIN), bz = get(BAZAAR).getAsJsonObject("products");
		Map<String, Double> b = new HashMap<>(), s = new HashMap<>();
		bins.entrySet().forEach(e -> {
			b.put(e.getKey(), e.getValue().getAsDouble());
			s.put(e.getKey(), e.getValue().getAsDouble());
		});
		bz.entrySet().forEach(e -> {
			JsonObject q = e.getValue().getAsJsonObject().getAsJsonObject("quick_status");
			double sp = q.get("sellPrice").getAsDouble(), bp = q.get("buyPrice").getAsDouble();
			if (sp > 0 && bp > 0) { // a buy order costs ~sellPrice, a sell offer earns ~buyPrice
				b.put(e.getKey(), sp);
				s.put(e.getKey(), bp);
			}
		});
		b.put("SKYBLOCK_COIN", 1.0);
		if (npcSell.isEmpty()) { // NPC prices barely change: once per session
			Map<String, Double> npc = new HashMap<>();
			for (JsonElement e : get(ITEMS).getAsJsonArray("items")) {
				JsonObject o = e.getAsJsonObject();
				if (o.has("npc_sell_price")) npc.put(o.get("id").getAsString(), o.get("npc_sell_price").getAsDouble());
			}
			npcSell = npc;
		}
		bazaarIds = Set.copyOf(bz.keySet());
		buy = b;
		sell = s;
		pricesAt = System.currentTimeMillis();
		pricesVersion++;
	}

	private static JsonObject get(String url) throws IOException, InterruptedException {
		return JsonParser.parseString(fetch(url)).getAsJsonObject();
	}

	static String fetch(String url) throws IOException, InterruptedException {
		HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
				.header("User-Agent", "SkyblockDailies-mod").build();
		HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() != 200) throw new IOException(url + " returned " + res.statusCode());
		return res.body();
	}


	static List<Result> bestCrafts(String material, int top, Map<String, Double> buy, Map<String, Double> sell) {
		return bestCrafts(usedInIndex.isEmpty() ? recipes : usedInIndex.getOrDefault(material, List.of()), material, top, buy, sell);
	}

	static List<Result> bestCrafts(List<Recipe> candidates, String material, int top, Map<String, Double> buy, Map<String, Double> sell) {
		Set<String> seen = new HashSet<>();
		List<Result> out = new ArrayList<>();
		for (Recipe r : candidates) {
			if (!r.inputs().containsKey(material) || !sell.containsKey(r.out())) continue;
			if (!seen.add(r.out() + r.inputs())) continue;
			if (!buy.keySet().containsAll(r.inputs().keySet())) continue; // unpriced input (vanilla/NPC item)
			double cost = r.inputs().entrySet().stream().mapToDouble(e -> buy.get(e.getKey()) * e.getValue()).sum();
			double value = sell.get(r.out()) * r.count();
			out.add(new Result(r, cost, value, value - cost));
		}
		out.sort(Comparator.comparingDouble(Result::profit).reversed());
		return out.subList(0, Math.min(top, out.size()));
	}

	static List<String> search(String text, int limit) {
		String t = text.toLowerCase(Locale.ROOT);
		return usedByName.keySet().stream()
				.filter(n -> n.contains(t))
				.sorted(Comparator.comparing((String n) -> !n.startsWith(t)).thenComparingInt(String::length))
				.limit(limit)
				.map(n -> names.getOrDefault(usedByName.get(n), n))
				.toList();
	}

	static String fmt(double n) {
		double a = Math.abs(n);
		if (a >= 1e9) return String.format(Locale.ROOT, "%.2fB", n / 1e9);
		if (a >= 1e6) return String.format(Locale.ROOT, "%.2fM", n / 1e6);
		if (a >= 1e3) return String.format(Locale.ROOT, "%.2fk", n / 1e3);
		// small prices (e.g. NPC 4.8 coins) keep one decimal; whole numbers stay clean
		return a < 100 && n != Math.rint(n) ? String.format(Locale.ROOT, "%.1f", n) : String.format(Locale.ROOT, "%.0f", n);
	}

	static int command(FabricClientCommandSource src, String material) {
		if (recipes.isEmpty()) {
			src.sendError(Component.literal("Recipes still loading (or launch once with SkyHanni/Skyblocker so it downloads the NEU repo)."));
			return 0;
		}
		String key = material.toLowerCase(Locale.ROOT);
		String id = usedByName.containsKey(key) ? usedByName.get(key)
				: names.containsKey(material.toUpperCase(Locale.ROOT)) ? material.toUpperCase(Locale.ROOT)
				: search(material, 1).stream().map(n -> usedByName.get(n.toLowerCase(Locale.ROOT))).findFirst().orElse(null);
		if (id == null) {
			src.sendError(Component.literal("Couldn't find material \"" + material + "\"."));
			return 0;
		}
		return run(src, id);
	}

	static String heldItemId(FabricClientCommandSource src) {
		var player = src.getClient().player;
		return player == null ? null : skyblockId(player.getMainHandItem());
	}

	// Hypixel stores the SkyBlock id in custom_data.id
	static String skyblockId(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? null : data.copyTag().getString("id").filter(s -> !s.isEmpty()).orElse(null);
	}

	static int commandHeld(FabricClientCommandSource src) {
		String id = heldItemId(src);
		if (id == null) return -1; // nothing from SkyBlock in hand: caller prints usage
		if (recipes.isEmpty()) {
			src.sendError(Component.literal("Recipes still loading (or launch once with SkyHanni/Skyblocker so it downloads the NEU repo)."));
			return 0;
		}
		if (recipes.stream().noneMatch(r -> r.inputs().containsKey(id))) {
			src.sendError(Component.literal(names.getOrDefault(id, id) + " isn't used in any known craft."));
			return 0;
		}
		return run(src, id);
	}

	private static int run(FabricClientCommandSource src, String id) {
		src.sendFeedback(Component.literal("§7Fetching prices..."));
		CompletableFuture.runAsync(() -> {
			List<String> lines = new ArrayList<>();
			try {
				ensurePrices();
				List<Result> res = bestCrafts(id, 5, buy, sell);
				lines.add("§6§lCrafts using §f" + names.get(id));
				if (res.isEmpty()) lines.add("§7No craft with known prices uses this material.");
				int i = 1;
				for (Result r : res) {
					String count = r.r().count() > 1 ? " x" + fmt(r.r().count()) : "";
					String forge = r.r().type().equals("forge") ? " §7· forge " + fmt(r.r().duration() / 3600.0) + "h" : "";
					lines.add("§e" + i++ + ". §f" + names.getOrDefault(r.r().out(), r.r().out()) + count
							+ " §7profit " + (r.profit() >= 0 ? "§a" : "§c") + fmt(r.profit())
							+ " §8(sells " + fmt(r.value()) + " − cost " + fmt(r.cost()) + ")" + forge);
					StringBuilder ings = new StringBuilder("   §8");
					r.r().inputs().forEach((it, q) -> ings.append(fmt(q)).append("x ").append(names.getOrDefault(it, it)).append(", "));
					lines.add(ings.substring(0, ings.length() - 2));
				}
				lines.add("§8Bazaar: buy order/sell offer · AH: lowest BIN · no taxes");
			} catch (Exception e) {
				lines.add("§cFailed to fetch prices: " + e.getMessage());
			}
			src.getClient().execute(() -> lines.forEach(l -> src.sendFeedback(Component.literal(l))));
		});
		return 1;
	}
}
