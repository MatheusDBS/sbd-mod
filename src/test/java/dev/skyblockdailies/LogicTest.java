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
		// diária: marcada 23:30 NY -> volta 00:00 NY do dia seguinte
		assertEquals(ny("2026-11-14T00:00:00"), SkyblockDailies.nextReset("ET?", ny("2026-11-13T23:30:00")));
		// marcada exatamente à meia-noite -> só volta na próxima meia-noite
		assertEquals(ny("2026-11-14T00:00:00"), SkyblockDailies.nextReset("ET", ny("2026-11-13T00:00:00")));
		// Matriarch: 20:00 NY
		assertEquals(ny("2026-11-13T20:00:00"), SkyblockDailies.nextReset("ET20", ny("2026-11-13T10:00:00")));
		assertEquals(ny("2026-11-14T20:00:00"), SkyblockDailies.nextReset("ET20", ny("2026-11-13T21:00:00")));
		// UTC: 00:00 UTC = 19:00 NY (EST)
		assertEquals(ny("2026-11-13T19:00:00"), SkyblockDailies.nextReset("UTC", ny("2026-11-13T10:00:00")));
		// cooldown
		assertEquals(ny("2026-11-13T11:15:00"), SkyblockDailies.nextReset("CD1.25", ny("2026-11-13T10:00:00")));
	}

	@Test
	void bestCrafts() {
		// mesmo caso do test_skyblock.py do bot
		Crafts.usedInIndex = Map.of(); // sem índice: varre a lista inteira (outro teste pode ter preenchido)
		Crafts.recipes = Stream.of(
				Crafts.parseRecipe("ART", JsonParser.parseString("{\"A1\":\"SORROW:16\",\"A2\":\"PLASMA:16\",\"C3\":\"SORROW:16\",\"B2\":\"RING:1\"}").getAsJsonObject()),
				Crafts.parseRecipe("CHEAP", JsonParser.parseString("{\"type\":\"forge\",\"inputs\":[\"SORROW:1\",\"SKYBLOCK_COIN:100\"],\"count\":2}").getAsJsonObject()),
				Crafts.parseRecipe("UNKNOWN", JsonParser.parseString("{\"A1\":\"SORROW:1\",\"A2\":\"STICK:1\"}").getAsJsonObject())
		).toList();
		Map<String, Double> buy = Map.of("SORROW", 1000.0, "PLASMA", 500.0, "RING", 10_000.0, "SKYBLOCK_COIN", 1.0);
		Map<String, Double> sell = Map.of("ART", 100_000.0, "CHEAP", 200.0, "UNKNOWN", 1e9);
		List<Crafts.Result> res = Crafts.bestCrafts("SORROW", 5, buy, sell);
		assertEquals(List.of("ART", "CHEAP"), res.stream().map(r -> r.r().out()).toList()); // UNKNOWN: STICK sem preço
		assertEquals(100_000 - (32 * 1000 + 16 * 500 + 10_000), res.get(0).profit());
		assertEquals(2 * 200 - 1100, res.get(1).profit());
	}

	private static int ticksUntilFire(boolean in, int max) {
		for (int i = 1; i <= max; i++) if (Reminder.step(in)) return i;
		return -1;
	}

	@Test
	void reminder() {
		Reminder.onJoin();
		assertEquals(Reminder.DELAY_SECONDS, ticksUntilFire(true, 5000)); // avisa aos 15s
		assertEquals(-1, ticksUntilFire(true, 5000)); // só uma vez
		ticksUntilFire(false, 5); // warp: scoreboard some por 5s
		assertEquals(-1, ticksUntilFire(true, 5000)); // não repete
		ticksUntilFire(false, Reminder.AWAY_SECONDS); // ficou 1 min fora do SkyBlock
		assertEquals(Reminder.DELAY_SECONDS, ticksUntilFire(true, 5000)); // voltou: avisa de novo
	}

	private static List<String> ids(String msg) {
		return AutoMark.matches(msg).stream().map(AutoMark.Rule::taskId).toList();
	}

	@Test
	void autoMark() {
		// every rule points to a real task
		for (AutoMark.Rule r : AutoMark.RULES)
			assertEquals(true, Tasks.ALL.stream().anyMatch(t -> t.id().equals(r.taskId())), r.taskId());
		// sample messages (color codes already stripped)
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
		// 30: 160 coal (320) vs buying for 400 -> crafting saves 80
		Crafts.Recipe ench = recipe("ENCHANTED_COAL", "{\"A1\":\"COAL:32\",\"A2\":\"COAL:32\",\"A3\":\"COAL:32\",\"B1\":\"COAL:32\",\"B2\":\"COAL:32\"}");
		assertEquals("§6Craft: §7cost §6320 §8| §7buy §6400 §8→ §acraft saves 80", Tooltips.craftLine(ench, buy, sell));
		// 31: NPC pays 4.8 vs market 380
		assertEquals("§6Sell: §7NPC §64.8 §8| §7market §6380 §8→ §amarket",
				Tooltips.marketLine("ENCHANTED_COAL", sell, Map.of("ENCHANTED_COAL", 4.8)));
		assertEquals(null, Tooltips.marketLine("ENCHANTED_COAL", sell, Map.of()));
		// 33: tier I recipe main ingredient, ignoring the tool
		Crafts.Recipe minion = recipe("COAL_GENERATOR_1", "{\"A1\":\"COAL:10\",\"A2\":\"COAL:10\",\"B2\":\"WOOD_PICKAXE:1\"}");
		assertEquals("COAL", Tooltips.mainIngredient(minion));
		Crafts.craftByOut = Map.of("COAL_GENERATOR_1", minion);
		// 15s per action -> 2 actions per coal -> 2880/day; best price = max(market, npc)
		assertEquals("§6Minion: §7~2.88k Coal/day §8≈ §65.76k coins/day §8(base)",
				Tooltips.minionLine("COAL_GENERATOR_5", 15, Map.of(), Map.of("COAL", 2.0), Map.of("COAL", "Coal")));
		assertEquals(null, Tooltips.minionLine("COAL", 15, Map.of(), Map.of(), Map.of()));
		// 32: used-in uses the index
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
	void gabagool() {
		assertEquals(new Gabagool.Result(1, 0, 301, 1216, 76, 2, 6), Gabagool.calc(36));
		assertEquals(new Gabagool.Result(2, 3, 602, 2416, 151, 4, 12), Gabagool.calc(75));
	}
}
