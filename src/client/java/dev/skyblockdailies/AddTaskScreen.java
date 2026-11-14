package dev.skyblockdailies;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AddTaskScreen extends Screen {
	private static final String[][] RESETS = {
			{"Daily (midnight New York)", "ET"}, {"Every 1h", "CD1"}, {"Every 2h", "CD2"}, {"Every 4h", "CD4"},
			{"Every 8h", "CD8"}, {"Every 12h", "CD12"}, {"Every 24h", "CD24"}, {"Every 2 days", "CD48"}, {"Weekly", "CD168"}};

	private final Screen parent;
	private int section, reset;
	private EditBox name;
	private String error = "";

	public AddTaskScreen(Screen parent, String currentSection) {
		super(Component.literal("New task"));
		this.parent = parent;
		this.section = Math.max(0, Tasks.SECTIONS.indexOf(currentSection));
	}

	@Override
	protected void init() {
		int cx = width / 2, y = height / 2 - 50;
		String typed = name == null ? "" : name.getValue();
		name = new EditBox(font, cx - 120, y, 240, 20, Component.literal("Task name"));
		name.setMaxLength(48);
		name.setHint(Component.literal("§7Task name, e.g. Buy Booster Cookie"));
		name.setValue(typed);
		addRenderableWidget(name);
		setInitialFocus(name);

		addRenderableWidget(Button.builder(Component.literal("Section: §e" + Tasks.SECTIONS.get(section)), b -> {
			section = (section + 1) % Tasks.SECTIONS.size();
			rebuildWidgets();
		}).bounds(cx - 120, y + 26, 240, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Resets: §e" + RESETS[reset][0]), b -> {
			reset = (reset + 1) % RESETS.length;
			rebuildWidgets();
		}).bounds(cx - 120, y + 50, 240, 20).build());

		addRenderableWidget(Button.builder(Component.literal("§aSave"), b -> save()).bounds(cx - 120, y + 84, 116, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(cx + 4, y + 84, 116, 20).build());
	}

	private void save() {
		if (name.getValue().isBlank()) {
			error = "§cType a name first.";
			return;
		}
		SkyblockDailies.addCustom(name.getValue(), Tasks.SECTIONS.get(section), RESETS[reset][1]);
		onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.centeredText(font, "§6§lNew task", width / 2, height / 2 - 72, 0xFFFFFFFF);
		if (!error.isEmpty()) g.centeredText(font, error, width / 2, height / 2 + 60, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
