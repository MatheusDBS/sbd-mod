package dev.skyblockdailies;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Small list with the pending dailies of the current island. Position, size and color are configurable. */
final class DailiesHud {
	private static final int MAX_LINES = 6, PAD = 2;
	// background color presets (ARGB) and their names
	static final int[] COLORS = {0x80000000, 0xC0000000, 0x80102040, 0x80301040, 0x00000000};
	static final String[] COLOR_NAMES = {"Dark", "Darker", "Blue", "Purple", "None"};

	// config (saved in prefs): top-left corner as a fraction of the screen, scale and color preset
	static float x = 0.005f, y = 0.4f, scale = 1f;
	static int color;

	// lines rebuilt once per second (not every frame)
	private static List<String> current;

	// last drawn box in screen pixels (used by the editor to drag it)
	static int boxX, boxY, boxW, boxH;

	static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (current != null && mc.gui.screen() == null) draw(g, current);
	}

	/** Called once per second. */
	static void update() {
		current = SkyblockDailies.hudOn && Area.inSkyblock && Area.section != null ? lines(Area.section) : null;
	}

	/** HUD text for a section, or null if nothing is pending there. */
	static List<String> lines(String section) {
		List<Tasks.Task> pending = SkyblockDailies.visible().stream()
				.filter(t -> t.section().equals(section) && !SkyblockDailies.isDone(t))
				.toList();
		if (pending.isEmpty()) return null;
		List<String> out = new ArrayList<>();
		out.add("§6Dailies §7» §f" + section + " §7(" + pending.size() + ")");
		for (Tasks.Task t : pending.subList(0, Math.min(MAX_LINES, pending.size()))) {
			int p = SkyblockDailies.progressOf(t);
			out.add("§7• §f" + t.name() + (p > 0 ? " §8[" + p + "/" + AutoMark.need(t) + "]" : ""));
		}
		if (pending.size() > MAX_LINES) out.add("§8+" + (pending.size() - MAX_LINES) + " (press K)");
		return out;
	}

	static void draw(GuiGraphicsExtractor g, List<String> lines) {
		Minecraft mc = Minecraft.getInstance();
		var font = mc.font;
		int lh = font.lineHeight + 1;
		int w = 0;
		for (String l : lines) w = Math.max(w, font.width(l));
		int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
		boxW = Math.round((w + PAD * 2) * scale);
		boxH = Math.round((lines.size() * lh + PAD * 2) * scale);
		// keep the whole box on screen
		boxX = Math.max(0, Math.min(sw - boxW, Math.round(x * sw)));
		boxY = Math.max(0, Math.min(sh - boxH, Math.round(y * sh)));

		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(boxX, boxY);
		pose.scale(scale, scale);
		if (COLORS[color] != 0) g.fill(0, 0, w + PAD * 2, lines.size() * lh + PAD * 2, COLORS[color]);
		for (int i = 0; i < lines.size(); i++) g.text(font, lines.get(i), PAD, PAD + i * lh, 0xFFFFFFFF);
		pose.popMatrix();
	}
}
