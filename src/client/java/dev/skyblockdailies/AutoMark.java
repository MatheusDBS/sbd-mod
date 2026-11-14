package dev.skyblockdailies;

import java.util.List;
import java.util.regex.Pattern;

// Matched against chat with color codes stripped. Hypixel can change these messages at any time.
final class AutoMark {
	record Rule(String taskId, Pattern pattern, int need) {}

	private static Rule rule(String taskId, String regex, int need) {
		return new Rule(taskId, Pattern.compile(regex), need);
	}

	static final List<Rule> RULES = List.of(
			rule("Fetchur", "^\\[NPC] Fetchur: (thanks thats probably what i needed|come back another time, maybe tmrw)", 1),
			rule("Faction Quests (5)", "^You completed your \\w+ quest! Visit the Town Board", 5),
			// only sent when the kill gives daily reputation, so it counts each miniboss once
			rule("Mini-bosses (5)", "You gained 20 Reputation for killing the miniboss!", 5),
			rule("Heavy Pearls (Matriarch)", "^Find a way to reach the top of the stomach!", 1),
			rule("Experimentation Table", "^You claimed the Superpairs rewards!", 1), // Superpairs is the last experiment
			rule("5 runs com bônus de XP", "^\\s*Team Score: \\d+ \\(", 5),
			rule("Greenhouse: Rosewater Flask", "which grew your Greenhouse crops by", 1),
			rule("Atender todos os Visitors", "^OFFER ACCEPTED with ", 1),
			rule("Free Trial (1 Tree Gift)", "^\\s*TREE GIFT$", 1),
			rule("250 Tree Gifts", "^\\s*TREE GIFT$", 250),
			rule("Starlyn Contests (3 bônus)", "^\\[NPC] .+: You earned a total of [\\d,]+ points!", 3)
	);

	static int need(Tasks.Task t) {
		return RULES.stream().filter(r -> r.taskId().equals(t.id())).mapToInt(Rule::need).findFirst().orElse(1);
	}

	static List<Rule> matches(String plain) {
		return RULES.stream().filter(r -> r.pattern().matcher(plain).find()).toList();
	}

	static void onChat(String text) {
		for (Rule r : matches(Area.strip(text).trim())) {
			Tasks.ALL.stream().filter(t -> t.id().equals(r.taskId())).findFirst()
					.ifPresent(t -> SkyblockDailies.addProgress(t, r.need()));
		}
	}
}
