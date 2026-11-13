package dev.skyblockdailies;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.Duration;
import java.util.List;

public class DailiesScreen extends Screen {
	private static final int TOP = 34, ROW = 22, BOTTOM = 32, SIDE_W = 120, GAP = 8;
	// lembra seção e página entre aberturas
	private static String section = Tasks.SECTIONS.get(0);
	private static int page;
	private static boolean edit; // modo editar: clicar esconde/mostra a tarefa

	public DailiesScreen() {
		super(Component.literal("Skyblock Dailies"));
	}

	private static List<Tasks.Task> tasksOf(String sec) {
		return (edit ? SkyblockDailies.allTasks() : SkyblockDailies.visible()).stream().filter(t -> t.section().equals(sec)).toList();
	}

	private static int doneIn(List<Tasks.Task> tasks) {
		return (int) tasks.stream().filter(SkyblockDailies::isDone).count();
	}

	@Override
	protected void init() {
		int panelW = Math.min(width - 20, 560);
		int left = (width - panelW) / 2;

		// Barra lateral: uma aba por lugar, com progresso
		List<String> secs = Tasks.SECTIONS;
		int tabH = Math.min(ROW, (height - TOP - BOTTOM) / secs.size());
		for (int i = 0; i < secs.size(); i++) {
			String sec = secs.get(i);
			List<Tasks.Task> ts = tasksOf(sec);
			int d = doneIn(ts);
			String mark = sec.equals(section) ? "§e» " : "";
			String count = d == ts.size() ? "§a✔" : "§7" + d + "/" + ts.size();
			addRenderableWidget(Button.builder(Component.literal(mark + "§f" + sec + " " + count), b -> {
						section = sec;
						page = 0;
						rebuildWidgets();
					})
					.bounds(left, TOP + i * tabH, SIDE_W, tabH - 2).build());
		}

		// Painel: tarefas da seção escolhida
		List<Tasks.Task> tasks = tasksOf(section);
		int x0 = left + SIDE_W + GAP, areaW = panelW - SIDE_W - GAP;
		int cols = areaW >= 400 ? 2 : 1;
		int colW = (areaW - (cols - 1) * GAP) / cols;
		int rows = Math.max(1, (height - TOP - BOTTOM) / ROW);
		int perPage = rows * cols;
		int pages = Math.max(1, (tasks.size() + perPage - 1) / perPage);
		page = Math.min(page, pages - 1);
		int start = page * perPage;
		for (int i = start; i < Math.min(tasks.size(), start + perPage); i++) {
			Tasks.Task t = tasks.get(i);
			int idx = i - start;
			int x = x0 + (idx / rows) * (colW + GAP);
			int y = TOP + (idx % rows) * ROW;
			Duration rem = SkyblockDailies.remaining(t);
			int need = AutoMark.need(t), prog = SkyblockDailies.progressOf(t);
			String label = edit
					? (SkyblockDailies.isHidden(t) ? "§8[hidden] §7" : "§a[shown] §f") + t.name()
					: rem != null
					? "§a✔ §7" + t.name() + " §8(back in " + SkyblockDailies.fmt(rem) + ")"
					: "§c✘ §f" + t.name() + (prog > 0 ? " §e[" + prog + "/" + need + "]" : "");
			addRenderableWidget(Button.builder(Component.literal(label), b -> {
						if (edit) SkyblockDailies.toggleHidden(t);
						else SkyblockDailies.toggle(t);
						rebuildWidgets();
					})
					.bounds(x, y, edit && SkyblockDailies.isCustom(t) ? colW - 22 : colW, 20)
					.tooltip(Tooltip.create(Component.literal(t.tip() + "\n§eResets: §f" + SkyblockDailies.describeReset(t.reset()))))
					.build());
			if (edit && SkyblockDailies.isCustom(t)) { // delete button for custom tasks
				addRenderableWidget(Button.builder(Component.literal("§c✕"), b -> {
							SkyblockDailies.removeCustom(t);
							rebuildWidgets();
						})
						.tooltip(Tooltip.create(Component.literal("Delete this custom task")))
						.bounds(x + colW - 20, y, 20, 20).build());
			}
		}

		int by = height - 26;
		Button prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); })
				.bounds(left, by, 20, 20).build());
		prev.active = page > 0;
		Button next = addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); })
				.bounds(left + 24, by, 20, 20).build());
		next.active = page < pages - 1;
		prev.visible = next.visible = pages > 1;
		int bx = left + panelW;
		addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
				.bounds(bx -= 60, by, 60, 20).build());
		if (edit) {
			addRenderableWidget(Button.builder(Component.literal("§a+ Task"), b -> minecraft.gui.setScreen(new AddTaskScreen(this, section)))
					.tooltip(Tooltip.create(Component.literal("Create your own daily or timer.")))
					.bounds(bx -= 104, by, 100, 20).build());
		} else {
			addRenderableWidget(Button.builder(Component.literal("Clear section"), b -> {
						SkyblockDailies.clear(tasks.stream().map(Tasks.Task::id).toList());
						rebuildWidgets();
					})
					.bounds(bx -= 104, by, 100, 20).build());
		}
		addRenderableWidget(Button.builder(Component.literal("HUD: " + (SkyblockDailies.hudOn ? "§aon" : "§coff") + " §7✎"),
						b -> minecraft.gui.setScreen(new HudEditScreen(this)))
				.tooltip(Tooltip.create(Component.literal("Move, resize, recolor or turn off the island HUD.")))
				.bounds(bx -= 94, by, 90, 20).build());
		addRenderableWidget(Button.builder(Component.literal(edit ? "§eDone" : "Edit"), b -> {
					edit = !edit;
					rebuildWidgets();
				})
				.tooltip(Tooltip.create(Component.literal("Edit mode: click a task to hide or show it, add or delete your own tasks.")))
				.bounds(bx - 74, by, 70, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		List<Tasks.Task> daily = SkyblockDailies.visible().stream().filter(SkyblockDailies::isDaily).toList();
		int total = daily.size(), doneN = doneIn(daily);
		Duration left = SkyblockDailies.untilDailyReset();
		String color = doneN == total ? "§a" : "§e";
		g.centeredText(font, "§6§lSkyblock Dailies  " + color + doneN + "/" + total + "  §7» §f" + section,
				width / 2, 8, 0xFFFFFFFF);
		g.centeredText(font, edit ? "§eEdit mode: click to hide/show tasks"
						: "§7Daily reset in " + left.toHours() + "h " + left.toMinutesPart() + "min  •  hover for details",
				width / 2, 20, 0xFFFFFFFF);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
