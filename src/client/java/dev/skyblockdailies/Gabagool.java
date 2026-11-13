package dev.skyblockdailies;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

/**
 * /sbd gabagool: Very Crude Gabagool -> Hypergolic Gabagool (porte do gabagool.py).
 * 1 Very Crude + 8 Sulphuric Coal -> 8 Fuel; 24 Fuel + 1 SC -> 1 Heavy; 12 Heavy + 1 SC -> 1 Hypergolic;
 * 16 Ench. Coal + 1 Ench. Sulphur -> 4 Sulphuric Coal; Hypergolic + 2 Inferno Fuel Block + 6 Distillate -> 1 Minion Fuel.
 */
final class Gabagool {
	record Result(long hypergolic, long leftover, long sulphuricCoal, long enchCoal, long enchSulphur, long fuelBlock, long distillate) {}

	static Result calc(long veryCrude) {
		long hyper = veryCrude / 36; // 1 Hypergolic = 12 Heavy = 288 Fuel = 36 Very Crude
		long sc = hyper * (36 * 8 + 12 + 1); // 301 Sulphuric Coal por Hypergolic
		long crafts = (sc + 3) / 4; // arredonda pra cima: cada craft faz 4 Sulphuric Coal
		return new Result(hyper, veryCrude - hyper * 36, sc, crafts * 16, crafts, hyper * 2, hyper * 6);
	}

	static int command(FabricClientCommandSource src, int veryCrude) {
		Result r = calc(veryCrude);
		String[] lines = {
				"§6§lGabagool: §f" + veryCrude + " Very Crude §7→ §d" + r.hypergolic() + " Hypergolic §7(" + r.leftover() + " left over, use multiples of 36)",
				"§7Sulphuric Coal: §f" + r.sulphuricCoal(),
				"§7Enchanted Coal: §f" + r.enchCoal() + " §8(" + r.enchCoal() * 160 + " Coal)",
				"§7Enchanted Sulphur: §f" + r.enchSulphur() + " §8(" + r.enchSulphur() * 160 + " Sulphur)",
				"§7For " + r.hypergolic() + " Hypergolic Inferno Minion Fuel: §f" + r.fuelBlock() + " Inferno Fuel Block§7, §f" + r.distillate() + " Gabagool Distillate",
		};
		for (String l : lines) src.sendFeedback(Component.literal(l));
		return 1;
	}
}
