package dev.skyblockdailies;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogicTest {
	private static Instant ny(String localDateTime) {
		return ZonedDateTime.parse(localDateTime + "-05:00[America/New_York]").toInstant();
	}

	@Test
	void resets() {
		// ticked 23:30 NY -> back at the next midnight NY
		assertEquals(ny("2026-11-14T00:00:00"), SkyblockDailies.nextReset("ET?", ny("2026-11-13T23:30:00")));
		// ticked exactly at midnight -> back the next midnight
		assertEquals(ny("2026-11-14T00:00:00"), SkyblockDailies.nextReset("ET", ny("2026-11-13T00:00:00")));
		assertEquals(ny("2026-11-13T20:00:00"), SkyblockDailies.nextReset("ET20", ny("2026-11-13T10:00:00")));
		assertEquals(ny("2026-11-14T20:00:00"), SkyblockDailies.nextReset("ET20", ny("2026-11-13T21:00:00")));
		// midnight UTC = 19:00 NY (EST)
		assertEquals(ny("2026-11-13T19:00:00"), SkyblockDailies.nextReset("UTC", ny("2026-11-13T10:00:00")));
		assertEquals(ny("2026-11-13T11:15:00"), SkyblockDailies.nextReset("CD1.25", ny("2026-11-13T10:00:00")));
	}

	@Test
	void bestCrafts() {
		// same case as the Discord bot's test_skyblock.py
		Crafts.usedInIndex = Map.of(); // another test may have filled the index
		Crafts.recipes = Stream.of(
				Crafts.parseRecipe("ART", JsonParser.parseString("{\"A1\":\"SORROW:16\",\"A2\":\"PLASMA:16\",\"C3\":\"SORROW:16\",\"B2\":\"RING:1\"}").getAsJsonObject()),
				Crafts.parseRecipe("CHEAP", JsonParser.parseString("{\"type\":\"forge\",\"inputs\":[\"SORROW:1\",\"SKYBLOCK_COIN:100\"],\"count\":2}").getAsJsonObject()),
				Crafts.parseRecipe("UNKNOWN", JsonParser.parseString("{\"A1\":\"SORROW:1\",\"A2\":\"STICK:1\"}").getAsJsonObject())
		).toList();
		Map<String, Double> buy = Map.of("SORROW", 1000.0, "PLASMA", 500.0, "RING", 10_000.0, "SKYBLOCK_COIN", 1.0);
		Map<String, Double> sell = Map.of("ART", 100_000.0, "CHEAP", 200.0, "UNKNOWN", 1e9);
		List<Crafts.Result> res = Crafts.bestCrafts("SORROW", 5, buy, sell);
		assertEquals(List.of("ART", "CHEAP"), res.stream().map(r -> r.r().out()).toList()); // STICK has no price
		assertEquals(100_000 - (32 * 1000 + 16 * 500 + 10_000), res.get(0).profit());
		assertEquals(2 * 200 - 1100, res.get(1).profit());
	}

	private static int ticksUntilFire(boolean in, int max) {
		for (int i = 1; i <= max; i++) if (Reminder.step(in)) return i;
		return -1;
	}

	@Test
	void reminder() {
		ticksUntilFire(false, Reminder.AWAY_SECONDS);
		assertEquals(Reminder.DELAY_SECONDS, ticksUntilFire(true, 5000));
		assertEquals(-1, ticksUntilFire(true, 5000)); // only once
		ticksUntilFire(false, 5); // server switch: no scoreboard for 5s
		assertEquals(-1, ticksUntilFire(true, 5000));
		ticksUntilFire(false, 90); // 1m30 in the main lobby
		assertEquals(-1, ticksUntilFire(true, 5000));
		ticksUntilFire(false, Reminder.AWAY_SECONDS); // 5 min away -> fires again
		assertEquals(Reminder.DELAY_SECONDS, ticksUntilFire(true, 5000));
	}

	private static List<String> ids(String msg) {
		return AutoMark.matches(msg).stream().map(AutoMark.Rule::taskId).toList();
	}

	@Test
	void autoMark() {
		for (AutoMark.Rule r : AutoMark.RULES)
			assertEquals(true, Tasks.ALL.stream().anyMatch(t -> t.id().equals(r.taskId())), r.taskId());
		assertEquals(List.of("Fetchur"), ids("[NPC] Fetchur: thanks thats probably what i needed"));
		assertEquals(List.of("Faction Quests (5)"), ids("You completed your Barbarian quest! Visit the Town Board to claim the rewards,"));
		assertEquals(List.of("Mini-bosses (5)"), ids("You gained 20 Reputation for killing the miniboss!"));
		assertEquals(List.of("Heavy Pearls (Matriarch)"), ids("Find a way to reach the top of the stomach!"));
		assertEquals(List.of("Experimentation Table"), ids("You claimed the Superpairs rewards!"));
		assertEquals(List.of(), ids("You claimed the Chronomatron rewards!"));
		assertEquals(List.of("5 runs com bônus de XP"), ids("Team Score: 302 (S+)"));
		assertEquals(List.of("Atender todos os Visitors"), ids("OFFER ACCEPTED with Duke (UNCOMMON)"));
		assertEquals(List.of("Free Trial (1 Tree Gift)", "250 Tree Gifts"), ids("TREE GIFT"));
		assertEquals(List.of("Starlyn Contests (3 bônus)"), ids("[NPC] Starlyn: You earned a total of 1,234 points!"));
		assertEquals(List.of(), ids("[NPC] Fetchur: its a yellow stained glass"));
	}

	@Test
	void areas() {
		assertEquals("Garden", Area.sectionOf("Garden"));
		assertEquals("Crimson Isle", Area.sectionOf("Kuudra's Hollow"));
		assertEquals("Dwarven Mines", Area.sectionOf("Glacite Mineshafts"));
		assertEquals("Dungeons", Area.sectionOf("Catacombs"));
		assertEquals("Galatea", Area.sectionOf("Moonglade Marsh"));
		assertEquals("Hub", Area.sectionOf("Hub"));
		assertEquals(null, Area.sectionOf("The Park"));
	}

	@Test
	void forgeTooltip() {
		Crafts.Recipe r = Crafts.parseRecipe("REFINED_MITHRIL", JsonParser.parseString(
				"{\"type\":\"forge\",\"inputs\":[\"ENCHANTED_MITHRIL:160\"],\"count\":1,\"duration\":21600}").getAsJsonObject());
		List<String> lines = Forge.lines(r, Map.of("ENCHANTED_MITHRIL", 500.0), Map.of("REFINED_MITHRIL", 110_000.0),
				Map.of("ENCHANTED_MITHRIL", "Enchanted Mithril"));
		// cost 80k, sells 110k, profit 30k over 6h = 5k/h
		assertEquals(true, lines.contains("§7Cost: §680.00k §8| §7Sells: §6110.00k"), lines.toString());
		assertEquals(true, lines.contains("§7Profit: §a30.00k §8(§a5.00k/h§8)"), lines.toString());
		assertEquals(true, lines.contains("§8 - §7160x Enchanted Mithril §8(80.00k)"), lines.toString());
		// unknown material price -> no profit line, "?" cost
		List<String> unknown = Forge.lines(r, Map.of("X", 1.0), Map.of("REFINED_MITHRIL", 110_000.0), Map.of());
		assertEquals(true, unknown.contains("§7Cost: §6? §8| §7Sells: §6110.00k"), unknown.toString());
	}

	private static Crafts.Recipe recipe(String out, String json) {
		return Crafts.parseRecipe(out, JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	void tooltipLines() {
		Map<String, Double> buy = Map.of("COAL", 2.0, "ENCHANTED_COAL", 400.0, "ENCHANTED_MITHRIL", 500.0);
		Map<String, Double> sell = Map.of("ENCHANTED_COAL", 380.0, "REFINED_MITHRIL", 110_000.0);
		// 160 coal (320) vs buying for 400
		Crafts.Recipe ench = recipe("ENCHANTED_COAL", "{\"A1\":\"COAL:32\",\"A2\":\"COAL:32\",\"A3\":\"COAL:32\",\"B1\":\"COAL:32\",\"B2\":\"COAL:32\"}");
		assertEquals("§6Craft: §7cost §6320 §8| §7buy §6400 §8→ §acraft saves 80", Tooltips.craftLine(ench, buy, sell));
		assertEquals("§6Sell: §7NPC §64.8 §8| §7market §6380 §8→ §amarket",
				Tooltips.marketLine("ENCHANTED_COAL", sell, Map.of("ENCHANTED_COAL", 4.8)));
		assertEquals(null, Tooltips.marketLine("ENCHANTED_COAL", sell, Map.of()));
		Crafts.Recipe minion = recipe("COAL_GENERATOR_1", "{\"A1\":\"COAL:10\",\"A2\":\"COAL:10\",\"B2\":\"WOOD_PICKAXE:1\"}");
		assertEquals("COAL", Tooltips.mainIngredient(minion));
		Crafts.craftByOut = Map.of("COAL_GENERATOR_1", minion);
		// 15s/action, 2 actions per coal -> 2880/day
		assertEquals("§6Minion: §7~2.88k Coal/day §8≈ §65.76k coins/day §8(base)",
				Tooltips.minionLine("COAL_GENERATOR_5", 15, Map.of(), Map.of("COAL", 2.0), Map.of("COAL", "Coal")));
		assertEquals(null, Tooltips.minionLine("COAL", 15, Map.of(), Map.of(), Map.of()));
		Crafts.usedInIndex = Map.of("COAL", List.of(ench));
		assertEquals("§6Used in §f1 §6recipe §8| §7best: §fEnchanted Coal §a+60",
				Tooltips.usedInLine("COAL", buy, sell, Map.of("ENCHANTED_COAL", "Enchanted Coal")));
	}

	@Test
	void bestForge() {
		Crafts.Recipe refined = recipe("REFINED_MITHRIL", "{\"type\":\"forge\",\"inputs\":[\"ENCHANTED_MITHRIL:160\"],\"duration\":21600}");
		Crafts.Recipe fast = recipe("FAST", "{\"type\":\"forge\",\"inputs\":[\"ENCHANTED_MITHRIL:1\"],\"duration\":3600}");
		Crafts.Recipe unknown = recipe("NOPE", "{\"type\":\"forge\",\"inputs\":[\"MISSING:1\"],\"duration\":60}");
		Map<String, Double> buy = Map.of("ENCHANTED_MITHRIL", 500.0);
		Map<String, Double> sell = Map.of("REFINED_MITHRIL", 110_000.0, "FAST", 10_500.0, "NOPE", 1e9);
		List<Forge.Ranked> res = Forge.best(List.of(refined, fast, unknown), 5, buy, sell);
		// FAST: 10k/h beats REFINED: 30k over 6h = 5k/h; NOPE skipped (unknown input price)
		assertEquals(List.of("FAST", "REFINED_MITHRIL"), res.stream().map(r -> r.r().out()).toList());
		assertEquals(5000.0, res.get(1).perHour(), 1e-9);
	}

	@Test
	void goalParsing() {
		Map<String, Double> m = new java.util.HashMap<>();
		Goal.putNumber(m, "Purse: 1,234,567 (+5)");
		Goal.putNumber(m, " Mithril: 12.5k");
		Goal.putNumber(m, "Bits: 3,000");
		Goal.putNumber(m, "www.hypixel.net");
		assertEquals(Map.of("purse", 1_234_567.0, "mithril", 12_500.0, "bits", 3000.0), m);
		assertEquals(10_000_000.0, Goal.parseAmount("10M"));
		assertEquals(2500.0, Goal.parseAmount("2.5k"));
		assertEquals(1000.0, Goal.parseAmount("1,000"));
		assertEquals(-1.0, Goal.parseAmount("abc"));
	}

	@Test
	void upgradeCosts() {
		// real NEU entry for Hyperion
		var hyp = JsonParser.parseString("{\"type\":\"Wither\",\"1\":150,\"2\":300,\"3\":500,\"4\":900,\"5\":1500,"
				+ "\"items\":{\"4\":[\"SKYBLOCK_COIN:10000\"],\"5\":[\"SKYBLOCK_COIN:25000\"]}}").getAsJsonObject();
		Map<String, Double> buy = Map.of("ESSENCE_WITHER", 3000.0, "SKYBLOCK_COIN", 1.0, "ENCHANTMENT_SHARPNESS_7", 5e6);
		assertEquals(900 * 3000.0 + 10_000, Upgrade.starCost(hyp, 4, buy));
		assertEquals(5, Upgrade.maxStars(hyp));
		var essences = new com.google.gson.JsonObject();
		essences.add("HYPERION", hyp);
		List<String> lines = Upgrade.lines("HYPERION", 3, Map.of("sharpness", 6, "smite", 7), buy, Map.of("HYPERION", "Hyperion"), essences);
		assertEquals(true, lines.contains("§7Next star (4): §62.71M §8(900 Wither Essence)"), lines.toString());
		assertEquals(true, lines.contains("§7To max (5): §67.24M"), lines.toString()); // 2.71M + 1500*3000+25k
		assertEquals(true, lines.contains("§8 - §7Sharpness 6 → 7: §65.00M"), lines.toString());
		assertEquals(false, lines.toString().contains("Smite"), lines.toString()); // no smite 8 book
	}

	@Test
	void sellCheck() {
		// Coflnet bazaar items come newest-first; must be sorted
		String json = "[{\"avg\":30,\"min\":3,\"time\":\"2026-11-03\"},{\"avg\":10,\"min\":1,\"time\":\"2026-11-01\"},{\"avg\":20,\"min\":2,\"time\":\"2026-11-02\"}]";
		assertEquals(List.of(10.0, 20.0, 30.0), SellCheck.daily(json, "avg"));
		assertEquals(List.of(1.0, 2.0, 3.0), SellCheck.daily(json, "min"));
		// a 10-coin "lowest sale" among ~500M days is dropped
		String outlier = "[{\"min\":500e6,\"time\":\"1\"},{\"min\":10,\"time\":\"2\"},{\"min\":480e6,\"time\":\"3\"},{\"min\":510e6,\"time\":\"4\"}]";
		assertEquals(List.of(500e6, 480e6, 510e6), SellCheck.daily(outlier, "min"));
		List<Double> month = java.util.stream.IntStream.rangeClosed(1, 30).mapToObj(i -> (double) i).toList();
		assertEquals(true, SellCheck.lines("X", month, 29, true).stream().anyMatch(l -> l.contains("good time to sell")));
		assertEquals(true, SellCheck.lines("X", month, 3, false).stream().anyMatch(l -> l.contains("better to hold")));
		assertEquals(true, SellCheck.lines("X", List.of(1.0, 2.0), 3, true).stream().anyMatch(l -> l.contains("Not enough")));
	}

	@Test
	void gabagool() {
		assertEquals(new Gabagool.Result(1, 0, 301, 1216, 76, 2, 6), Gabagool.calc(36));
		assertEquals(new Gabagool.Result(2, 3, 602, 2416, 151, 4, 12), Gabagool.calc(75));
	}
}
