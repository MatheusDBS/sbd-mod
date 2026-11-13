package dev.skyblockdailies;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Drag the HUD to move it, scroll to resize, buttons for color and on/off. */
public class HudEditScreen extends Screen {
	private static final List<String> SAMPLE = List.of(
			"§6Dailies §7» §fGarden §7(3)", "§7• §fServe all Visitors", "§7• §fJacob's Contest", "§7• §fSpend Copper");
	private final Screen parent;
	private boolean dragging;
	private double dragDx, dragDy;

	public HudEditScreen(Screen parent) {
		super(Component.literal("Edit HUD"));
		this.parent = parent;
	}

	private List<String> preview() {
		List<String> real = Area.section == null ? null : DailiesHud.lines(Area.section);
		return real != null ? real : SAMPLE;
	}

	@Override
	protected void init() {
		int by = height - 26, cx = width / 2;
		addRenderableWidget(Button.builder(Component.literal("HUD: " + (SkyblockDailies.hudOn ? "§aon" : "§coff")), b -> {
			SkyblockDailies.hudOn = !SkyblockDailies.hudOn;
			SkyblockDailies.save();
			rebuildWidgets();
		}).bounds(cx - 190, by, 80, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Color: " + DailiesHud.COLOR_NAMES[DailiesHud.color]), b -> {
			DailiesHud.color = (DailiesHud.color + 1) % DailiesHud.COLORS.length;
			SkyblockDailies.save();
			rebuildWidgets();
		}).bounds(cx - 106, by, 90, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
			DailiesHud.x = 0.005f;
			DailiesHud.y = 0.4f;
			DailiesHud.scale = 1f;
			DailiesHud.color = 0;
			SkyblockDailies.save();
			rebuildWidgets();
		}).bounds(cx - 12, by, 60, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(cx + 52, by, 80, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		DailiesHud.draw(g, preview());
		g.fill(DailiesHud.boxX - 1, DailiesHud.boxY - 1, DailiesHud.boxX + DailiesHud.boxW + 1, DailiesHud.boxY, 0xFFFFFF55);
		g.centeredText(font, "§6§lEdit HUD", width / 2, 8, 0xFFFFFFFF);
		g.centeredText(font, "§7Drag the box to move it  •  scroll to resize (§f" + Math.round(DailiesHud.scale * 100) + "%§7)",
				width / 2, 20, 0xFFFFFFFF);
	}

	private boolean overBox(double mx, double my) {
		return mx >= DailiesHud.boxX && mx <= DailiesHud.boxX + DailiesHud.boxW
				&& my >= DailiesHud.boxY && my <= DailiesHud.boxY + DailiesHud.boxH;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
		if (super.mouseClicked(e, doubleClick)) return true;
		if (e.button() == 0 && overBox(e.x(), e.y())) {
			dragging = true;
			dragDx = e.x() - DailiesHud.boxX;
			dragDy = e.y() - DailiesHud.boxY;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		if (!dragging) return super.mouseDragged(e, dx, dy);
		DailiesHud.x = (float) ((e.x() - dragDx) / width);
		DailiesHud.y = (float) ((e.y() - dragDy) / height);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent e) {
		if (dragging) {
			dragging = false;
			SkyblockDailies.save();
			return true;
		}
		return super.mouseReleased(e);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		DailiesHud.scale = Math.max(0.5f, Math.min(3f, Math.round((DailiesHud.scale + (float) sy * 0.1f) * 10) / 10f));
		SkyblockDailies.save();
		return true;
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
