package dev.skyblockdailies;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SkyblockDailies implements ClientModInitializer {
	// Hypixel server time
	static final ZoneId NY = ZoneId.of("America/New_York");

	private static final Map<String, Long> done = new HashMap<>(); // id -> when it was ticked (epoch ms)
	private static final Set<String> hidden = new HashSet<>();
	static final List<Tasks.Task> custom = new ArrayList<>();
	private static final Map<String, long[]> progress = new HashMap<>(); // id -> {NY epochDay, count}
	static boolean hudOn = true;
	private static boolean openNextTick;
	private static int tickCount;

	@Override
	public void onInitializeClient() {
		load();
		KeyMapping key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.skyblockdailies.open", InputConstants.Type.KEYSYM, InputConstants.KEY_K,
				KeyMapping.Category.register(Identifier.fromNamespaceAndPath("skyblockdailies", "main"))));

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> {
			// Everything lives under /sbd so it can't clash with Hypixel or other mods.
			// Every node has .executes(): otherwise Fabric forwards an incomplete command to the server ("Unknown command").
			dispatcher.register(ClientCommands.literal("sbd")
					.executes(c -> help(c.getSource()))
					.then(ClientCommands.literal("dailies").executes(c -> {
						openNextTick = true; // the chat screen closes after the command, so open next tick
						return 1;
					}))
					.then(ClientCommands.literal("bestcraft")
							.executes(c -> {
								int r = Crafts.commandHeld(c.getSource());
								return r >= 0 ? r : usage(c.getSource(), "/sbd bestcraft <material>  §7or hold the item and just type /sbd bestcraft");
							})
							.then(ClientCommands.argument("material", StringArgumentType.greedyString())
									.suggests((c, b) -> {
										Crafts.search(b.getRemaining(), 25).forEach(b::suggest);
										return b.buildFuture();
									})
									.executes(c -> Crafts.command(c.getSource(), StringArgumentType.getString(c, "material")))))
					.then(ClientCommands.literal("bestforge").executes(c -> Forge.command(c.getSource())))
					.then(ClientCommands.literal("gabagool")
							.executes(c -> usage(c.getSource(), "/sbd gabagool <amount of Very Crude>  §7e.g. /sbd gabagool 360"))
							.then(ClientCommands.argument("very_crude", IntegerArgumentType.integer(0))
									.executes(c -> Gabagool.command(c.getSource(), IntegerArgumentType.getInteger(c, "very_crude")))))
					.then(ClientCommands.literal("goal")
							.executes(c -> Goal.status(c.getSource()))
							.then(ClientCommands.literal("clear").executes(c -> Goal.clear(c.getSource())))
							.then(ClientCommands.argument("amount", StringArgumentType.word())
									.executes(c -> Goal.status(c.getSource()))
									.then(ClientCommands.argument("name", StringArgumentType.greedyString())
											.suggests((c, b) -> {
												Area.numbers.keySet().forEach(b::suggest);
												return b.buildFuture();
											})
											.executes(c -> Goal.set(c.getSource(), StringArgumentType.getString(c, "amount"), StringArgumentType.getString(c, "name"))))))
					.then(ClientCommands.literal("upgrade").executes(c -> Upgrade.command(c.getSource())))
					.then(ClientCommands.literal("sellcheck").executes(c -> SellCheck.command(c.getSource())))
					.then(ClientCommands.literal("help").executes(c -> help(c.getSource()))));
		});

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (++tickCount % 20 == 0) { // once per second: everything that reads the scoreboard/tab list
				Area.update(mc);
				Reminder.tick(mc);
				Goal.tick(mc);
				DailiesHud.update();
				notifyReady(mc);
			}
			while (key.consumeClick()) openNextTick = true;
			if (openNextTick) {
				openNextTick = false;
				mc.gui.setScreen(new DailiesScreen());
			}
		});
		ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
			if (!overlay) AutoMark.onChat(msg.getString());
		});
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("skyblockdailies", "hud"), DailiesHud::render);
		ItemTooltipCallback.EVENT.register((stack, ctx, flag, lines) -> Tooltips.append(stack, lines));
		Crafts.loadRepoAsync();
	}

	private static int help(FabricClientCommandSource src) {
		for (String l : new String[]{
				"§6§lSkyblock Dailies §7commands:",
				"§f/sbd help §7- this list",
				"§f/sbd dailies §7- open the checklist (or press K)",
				"§f/sbd bestcraft [material] §7- most profitable crafts (no material = item in hand)",
				"§f/sbd bestforge §7- best Forge items by profit per hour",
				"§f/sbd gabagool <amount> §7- Very Crude → Hypergolic calculator",
				"§f/sbd goal <amount> <name> §7- session goal, e.g. 10M purse (shown on the HUD)",
				"§f/sbd upgrade §7- cost of the next stars and enchant levels of the held item",
				"§f/sbd sellcheck §7- is the held item's price high or low vs the last 30 days"})
			src.sendFeedback(Component.literal(l));
		return 1;
	}

	private static int usage(FabricClientCommandSource src, String text) {
		src.sendFeedback(Component.literal("§eUsage: §f" + text));
		return 1;
	}

	static void say(Minecraft mc, String text) {
		if (mc.player != null) mc.gui.hud.getChat().addClientSystemMessage(Component.literal(text));
	}


	static Instant nextReset(String rule, Instant marked) {
		String r = rule.replace("?", "");
		if (r.startsWith("CD")) return marked.plusSeconds(Math.round(Double.parseDouble(r.substring(2)) * 3600));
		ZoneId zone = r.equals("UTC") ? ZoneOffset.UTC : NY;
		LocalTime at = r.equals("ET20") ? LocalTime.of(20, 0) : LocalTime.MIDNIGHT;
		ZonedDateTime m = marked.atZone(zone);
		ZonedDateTime next = m.toLocalDate().atTime(at).atZone(zone);
		return (next.isAfter(m) ? next : next.plusDays(1)).toInstant();
	}

	static String describeReset(String rule) {
		String r = rule.replace("?", "");
		if (r.startsWith("CD")) {
			return "cooldown of " + fmt(Duration.ofSeconds(Math.round(Double.parseDouble(r.substring(2)) * 3600))) + " after you tick it";
		}
		String local = nextReset(rule, Instant.now()).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"));
		String src = switch (r) {
			case "UTC" -> "00:00 UTC";
			case "ET20" -> "8:00 PM New York";
			default -> "midnight New York";
		};
		return "every day at " + local + " (your time) = " + src + (rule.endsWith("?") ? " §8(assumed)" : "");
	}

	static String fmt(Duration d) {
		long h = d.toHours(), m = d.toMinutesPart();
		return h > 0 ? h + "h" + (m > 0 ? String.format("%02d", m) : "") : Math.max(1, m) + "min";
	}

	static Duration untilDailyReset() {
		return Duration.between(Instant.now(), nextReset("ET", Instant.now()));
	}


	// Timers don't count toward daily progress or the join reminder
	static boolean isDaily(Tasks.Task t) {
		return !t.section().equals("Timers");
	}

	static boolean isHidden(Tasks.Task t) {
		return hidden.contains(t.id());
	}

	static void toggleHidden(Tasks.Task t) {
		if (!hidden.remove(t.id())) hidden.add(t.id());
		save();
	}

	static List<Tasks.Task> allTasks() {
		if (custom.isEmpty()) return Tasks.ALL;
		List<Tasks.Task> all = new ArrayList<>(Tasks.ALL);
		all.addAll(custom);
		return all;
	}

	static boolean isCustom(Tasks.Task t) {
		return t.id().startsWith("custom:");
	}

	static void addCustom(String name, String section, String reset) {
		String clean = name.replace('\t', ' ').trim();
		custom.add(new Tasks.Task("custom:" + System.currentTimeMillis(), section, clean, "§eCustom task", reset));
		save();
	}

	static void removeCustom(Tasks.Task t) {
		custom.removeIf(c -> c.id().equals(t.id()));
		done.remove(t.id());
		hidden.remove(t.id());
		save();
	}

	static List<Tasks.Task> visible() {
		return allTasks().stream().filter(t -> !isHidden(t)).toList();
	}

	// null when the task isn't done
	static Duration remaining(Tasks.Task t) {
		Long m = done.get(t.id());
		if (m == null) return null;
		Duration d = Duration.between(Instant.now(), nextReset(t.reset(), Instant.ofEpochMilli(m)));
		return d.isNegative() || d.isZero() ? null : d;
	}

	static boolean isDone(Tasks.Task t) {
		return remaining(t) != null;
	}

	static void toggle(Tasks.Task t) {
		if (isDone(t)) done.remove(t.id());
		else done.put(t.id(), System.currentTimeMillis());
		progress.remove(t.id());
		save();
	}

	static void markDone(Tasks.Task t) {
		if (isDone(t)) return;
		done.put(t.id(), System.currentTimeMillis());
		save();
	}

	static void clear(List<String> ids) {
		ids.forEach(id -> {
			done.remove(id);
			progress.remove(id);
		});
		save();
	}

	// today's count (NY day) for multi-part tasks like 5 quests
	static int progressOf(Tasks.Task t) {
		long[] p = progress.get(t.id());
		return p != null && p[0] == LocalDate.now(NY).toEpochDay() ? (int) p[1] : 0;
	}

	static void addProgress(Tasks.Task t, int need) {
		if (isDone(t)) return;
		int n = progressOf(t) + 1;
		progress.put(t.id(), new long[]{LocalDate.now(NY).toEpochDay(), n});
		if (n >= need) {
			progress.remove(t.id());
			done.put(t.id(), System.currentTimeMillis());
		}
		save();
	}

	private static void notifyReady(Minecraft mc) {
		if (mc.player == null) return;
		List<String> ready = new ArrayList<>();
		for (Tasks.Task t : allTasks()) {
			if (!t.reset().startsWith("CD") || !done.containsKey(t.id()) || isDone(t)) continue;
			done.remove(t.id());
			if (!isHidden(t)) ready.add(t.name());
		}
		if (ready.isEmpty()) return;
		save();
		String names = String.join(", ", ready);
		mc.gui.hud.setOverlayMessage(Component.literal("§a✔ Ready again: §f" + names), false);
		say(mc, "§6[Dailies] §aReady again: §f" + names);
	}


	private static Path file(String name) {
		return FabricLoader.getInstance().getConfigDir().resolve(name);
	}

	// skyblockdailies.txt: "id<TAB>epochMillis" per ticked task.
	// skyblockdailies-prefs.txt: "hide<TAB>id", "hud<TAB>off", "p<TAB>id<TAB>day<TAB>count", "hudcfg...", "custom...".
	// Unknown or broken lines are skipped.
	private static void load() {
		try {
			if (Files.exists(file("skyblockdailies.txt"))) {
				for (String line : Files.readAllLines(file("skyblockdailies.txt"))) {
					String[] f = line.split("\t");
					if (f.length == 2) {
						try {
							done.put(f[0], Long.parseLong(f[1].trim()));
						} catch (NumberFormatException ignored) {
						}
					}
				}
			}
			if (Files.exists(file("skyblockdailies-prefs.txt"))) {
				for (String line : Files.readAllLines(file("skyblockdailies-prefs.txt"))) {
					String[] f = line.split("\t");
					try {
						if (f.length == 2 && f[0].equals("hide")) hidden.add(f[1]);
						else if (f.length == 2 && f[0].equals("hud")) hudOn = !f[1].equals("off");
						else if (f.length == 5 && f[0].equals("custom")) custom.add(new Tasks.Task(f[1], f[2], f[3], "§eCustom task", f[4]));
						else if (f.length == 5 && f[0].equals("hudcfg")) {
							DailiesHud.x = Float.parseFloat(f[1]);
							DailiesHud.y = Float.parseFloat(f[2]);
							DailiesHud.scale = Float.parseFloat(f[3]);
							DailiesHud.color = Math.floorMod(Integer.parseInt(f[4]), DailiesHud.COLORS.length);
						}
						else if (f.length == 4 && f[0].equals("p")) progress.put(f[1], new long[]{Long.parseLong(f[2]), Long.parseLong(f[3])});
					} catch (NumberFormatException ignored) {
					}
				}
			}
		} catch (IOException e) {
			System.err.println("[SkyblockDailies] falha ao ler: " + e);
		}
	}

	static void save() {
		List<String> d = new ArrayList<>(), p = new ArrayList<>();
		done.forEach((id, ms) -> d.add(id + "\t" + ms));
		hidden.forEach(id -> p.add("hide\t" + id));
		if (!hudOn) p.add("hud\toff");
		progress.forEach((id, v) -> p.add("p\t" + id + "\t" + v[0] + "\t" + v[1]));
		custom.forEach(t -> p.add("custom\t" + t.id() + "\t" + t.section() + "\t" + t.name() + "\t" + t.reset()));
		p.add("hudcfg\t" + DailiesHud.x + "\t" + DailiesHud.y + "\t" + DailiesHud.scale + "\t" + DailiesHud.color);
		try {
			Files.write(file("skyblockdailies.txt"), d);
			Files.write(file("skyblockdailies-prefs.txt"), p);
		} catch (IOException e) {
			System.err.println("[SkyblockDailies] falha ao salvar: " + e);
		}
	}
}
