import ast, json, sys
src = open(__import__("os").path.join(__import__("os").path.dirname(__file__), "make_sheet.py"), encoding="utf-8").read()
node = next(n for n in ast.parse(src).body if isinstance(n, ast.Assign) and n.targets[0].id == "DAILY")
daily = ast.literal_eval(node.value)

ORDER = ["Geral", "Private Island", "Hub", "Garden", "Dwarven Mines", "Crimson Isle", "Dungeons", "Galatea", "Rift"]
BY_CAT = {"Enchanting": "Private Island", "Mining": "Dwarven Mines", "Foraging": "Galatea", "Hunting": "Galatea"}
BY_NAME = {
    "Coletar minions": "Private Island",
    "Checar Forge": "Dwarven Mines",
    "Checar ordens do Bazaar / AH": "Hub",
    "Kat - pet upgrade": "Hub",
    "Checar buffs (Booster Cookie / God Potion / Century Cake)": "Hub",
}
# Regras de reset (wiki): ET = 00:00 Nova York, UTC = 00:00 UTC, ET20 = 20:00 NY, CD<h> = cooldown após marcar.
# Sufixo "?" = horário não confirmado na wiki (presumido 00:00 NY, o horário do servidor).
RESET = {
    "Fetchur": "ET",
    "Bachelor's Roses (5)": "UTC",
    "Heavy Pearls (Matriarch)": "ET20",
    "Experimentation Table": "CD24",
    "Pomtair - Enchanted Bone Meal": "CD24",
    "Fossil Muncher": "CD24",
    "Greenhouse: Rosewater Flask": "CD2",
    "Ubik - Split or Steal (Tit for Tat)": "CD2",
    "Greenhouse: colher e replantar": "CD4",
    "Greenhouse: regar (Hydrocan)": "CD4",
    "Encher Composter (Organic Matter + Fuel)": "CD1.5",
    "Atender todos os Visitors": "CD1.25",
    "Esvaziar Pest Traps + recolocar isca": "CD0.5",
    "Jacob's Contest": "CD1",
}
reset = lambda n: RESET.get(n, "ET?")
assert set(RESET) <= {n for _, n, *_ in daily}, set(RESET) - {n for _, n, *_ in daily}
section = lambda c, n: BY_NAME.get(n, BY_CAT.get(c, c))
assert all(section(c, n) in ORDER for c, n, *_ in daily), [n for c, n, *_ in daily if section(c, n) not in ORDER]
assert len({n for _, n, *_ in daily}) == len(daily), "ids duplicados"
daily.sort(key=lambda t: ORDER.index(section(t[0], t[1])))  # sort é estável: mantém a ordem da planilha dentro da seção

from en_tasks import DAILY_EN, TIMER_EN  # texto em inglês; o nome em português continua como id (não perde o progresso salvo)
assert set(DAILY_EN) == {n for _, n, *_ in daily}, set(DAILY_EN) ^ {n for _, n, *_ in daily}
ORDER[0] = "General"
section_en = lambda c, n: "General" if section(c, n) == "Geral" else section(c, n)

q = lambda s: json.dumps(s, ensure_ascii=False)  # string JSON == literal Java para estes caracteres
rows = []
for c, n, *_ in daily:
    en, where, what, reward = DAILY_EN[n]
    rows.append(f'\t\t\tnew Task({q(n)}, {q(section_en(c, n))}, {q(en)}, {q(f"§eWhere: §f{where}\n§eWhat to do: §f{what}\n§eReward: §f{reward}")}, {q(reset(n))})')

# Timers da planilha que ainda não são tarefas viram a seção "Timers" (cooldown, avisa quando fica pronto)
tnode = next(n for n in ast.parse(src).body if isinstance(n, ast.Assign) and n.targets[0].id == "TIMERS")
TIMER_SKIP = {  # já cobertos por tarefas diárias ou pelo alarme da Dark Auction
    "Garden Visitors (fila de 5 cheia)", "Pest Traps cheias", "Jacob's Contest", "Composter sem Organic Matter",
    "Greenhouse Growth Stage", "Rosewater Flasks", "Tit for Tat (Ubik)", "Dark Auction (~min 55 de cada hora)",
}
timers = [t for t in ast.literal_eval(tnode.value) if t[0] not in TIMER_SKIP]
assert len(timers) == len(ast.literal_eval(tnode.value)) - len(TIMER_SKIP), "TIMER_SKIP com nome errado"
ORDER.append("Timers")
assert set(TIMER_EN) == {n for n, *_ in timers}, set(TIMER_EN) ^ {n for n, *_ in timers}
rows += [f'\t\t\tnew Task({q(n)}, "Timers", {q(TIMER_EN[n][0])}, {q(f"§eWhere: §f{TIMER_EN[n][1]}\n§eClick when you use it; you get a notice when it is ready again.")}, {q(f"CD{h:g}")})'
         for n, o, h in timers]
out = f"""package dev.skyblockdailies;

import java.util.List;

/** Generated from the Skyblock_Diarias.xlsx spreadsheet data (gen_tasks.py). */
final class Tasks {{
	record Task(String id, String section, String name, String tip, String reset) {{}}

	static final List<String> SECTIONS = List.of({", ".join(q(s) for s in ORDER)});

	static final List<Task> ALL = List.of(
{",\n".join(rows)}
	);
}}
"""
open(sys.argv[1], "w", encoding="utf-8").write(out)
from collections import Counter
print(Counter(section(c, n) for c, n, *_ in daily))
