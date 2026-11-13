from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment
from openpyxl.formatting.rule import FormulaRule

OUT = __import__("os").path.join(__import__("os").path.dirname(__file__), "Skyblock_Diarias.xlsx")
HDR = PatternFill("solid", fgColor="1F2937")
GREEN = PatternFill("solid", fgColor="C6EFCE")
YELLOW = PatternFill("solid", fgColor="FFEB9C")
WHITE_B = Font(bold=True, color="FFFFFF")

# (categoria, tarefa, onde/NPC, o que fazer, recompensa)
DAILY = [
    ("Geral", "Daily Reward do Hypixel", "Lobby principal (Delivery Man)", "Pegar o reward diário / streak", "Tokens / XP de rede"),
    ("Geral", "Coletar minions", "Private Island", "Esvaziar minions e vender/guardar", "Coins / coleções"),
    ("Geral", "Checar Forge", "Dwarven Mines (Forge)", "Coletar itens prontos e iniciar novos", "Itens forjados"),
    ("Geral", "Checar ordens do Bazaar / AH", "Bazaar / Auction House", "Coletar ordens e leilões, relistar", "Coins"),
    ("Enchanting", "Experimentation Table", "Private Island (Experimentation Table)", "Fazer Superpairs, Chronomatron e Ultrasequencer", "Enchanted books, XP, itens raros"),
    ("Mining", "Fetchur", "Dwarven Mines", "Entregar o item pedido do dia", "Mithril Powder"),
    ("Mining", "Fossil Muncher", "Glacite Tunnels", "Usar (cooldown ~24h)", "Recompensa de fóssil"),
    ("Crimson Isle", "Faction Quests (5)", "Crimson Isle (quadro de quests)", "Completar as 5 quests diárias", "+30 a 125 Reputação cada"),
    ("Crimson Isle", "Mini-bosses (5)", "Crimson Isle", "Ashfang, Barbarian Duke X, Bladesoul, Magma Boss, Mage Outlaw", "+20 Reputação cada"),
    ("Crimson Isle", "Kuudra (1 run por tier)", "Crimson Isle (Kuudra)", "Basic/Hot/Burning/Fiery/Infernal - 1 de cada", "+10 a 50 Reputação"),
    ("Crimson Isle", "Training Session (Kaus ou Rollim)", "Crimson Isle", "Fazer o treino da sua facção", "+25 Reputação por run"),
    ("Crimson Isle", "Heavy Pearls (Matriarch)", "Crimson Isle (Matriarch)", "Pegar as 5 pérolas (cooldown 24h)", "Heavy Pearls"),
    ("Crimson Isle", "Pomtair - Enchanted Bone Meal", "Crimson Isle", "Entregar (cooldown 24h)", "+50 Rep. Barbarian"),
    ("Dungeons", "5 runs com bônus de XP", "Catacombs", "Completar 5 dungeons", "+40% Dungeoneering XP"),
    ("Garden", "Matar pests acumuladas", "Garden (todos os plots)", "Ao logar: vacuum nas pests (spawnam 1/h offline, máx 8). Com 4+ pests você perde Farming Fortune", "1.000 coins + 1.500 Farming XP + Bits cada"),
    ("Garden", "Esvaziar Pest Traps + recolocar isca", "Garden (até 3 traps)", "Matar as pests das traps e pôr bait (Compost, Dung, Cheese, Honey Jar, Plant Matter, Jelly, Moondew)", "Pests escolhidas / chance de Field Mouse"),
    ("Garden", "Atender todos os Visitors", "Garden (Barn)", "Entregar os itens pedidos; recusar ofertas ruins. Fila máx 5 (até 10 com upgrade)", "Copper, Bits, Farming XP, chips raros"),
    ("Garden", "Greenhouse: colher e replantar", "Garden (Greenhouse, Garden VII)", "Colher plantas maduras, replantar, montar mutações", "Crops, Ethereal Vine, +320 Farming XP por colheita"),
    ("Garden", "Greenhouse: regar (Hydrocan)", "Garden (Greenhouse)", "Manter água acima de 0 (perde 20 por ciclo; em -100 trava)", "Crescimento não para"),
    ("Garden", "Greenhouse: Rosewater Flask", "Garden (Greenhouse)", "Usar flask em cada plot do Greenhouse (cooldown 2h)", "Buff no plot"),
    ("Garden", "Encher Composter (Organic Matter + Fuel)", "Garden (Composter)", "Colocar crops/enchanted (OM) e Biofuel/Oil (fuel). Coletar Compost", "Compost (plots, slots do Greenhouse, vender)"),
    ("Garden", "Bachelor's Roses (5)", "Garden", "Dyes p/ Vincent, Farming XP em visitor Legendary, Supreme Voter's Badge p/ prefeito", "Roses (necessárias p/ visitors Special)"),
    ("Garden", "Jacob's Contest", "Garden (Jacob)", "1 contest por hora real (dura 20 min). Pré-spawnar pests e plantar o crop do contest no Greenhouse", "Medalhas, Jacob's Tickets, Bits"),
    ("Garden", "Gastar Copper", "Garden (Desk / SkyMart)", "Crop Upgrades, Visitor Upgrades, Composter Upgrades", "Fortune, menos cooldown, mais compost"),
    ("Garden", "Trocar pests com Phillip (opcional)", "Garden (Pesthunter Phillip)", "Trocar pests por buff antes de farmar", "+5 Farming Fortune por 30 min por pest (máx 40)"),
    ("Foraging", "Daily Wishes (3)", "Galatea", "1x cada: Fig, Mangrove, Helix Log", "Bônus"),
    ("Foraging", "Free Trial (1 Tree Gift)", "Galatea", "Completar o primeiro Tree Gift do dia", "Basic Safari Ticket"),
    ("Foraging", "250 Tree Gifts", "Galatea", "Completar os Tree Gifts diários (até 250)", "Recompensas de Tree Gift"),
    ("Rift", "Ubik - Split or Steal (Tit for Tat)", "Rift (Ubik's Cube)", "Jogar Split or Steal (cooldown 2h)", "Motes (1k a 5k)"),
    ("Rift", "Ubik - Three Options", "Rift (Ubik)", "Escolher 1 de 3 caixas (1x por visita ao Rift)", "IQ Point / Enigma Soul"),
    ("Geral", "Checar buffs (Booster Cookie / God Potion / Century Cake)", "Elizabeth (Community Center)", "Renovar se acabou; gastar Bits (God Potion, Kismet, Perfume da Matriarch)", "Bits e buffs ativos"),
    ("Geral", "Checar Calendar (eventos)", "/calendar", "Ver se tem Hoppity's Hunt, Spooky, Jerry, Dark Auction, mayor etc.", "Recompensas de evento"),
    ("Geral", "Kat - pet upgrade", "Hub (Kat)", "Coletar pet pronto e mandar o próximo", "Pet de raridade maior"),
    ("Foraging", "Starlyn Contests (3 bônus)", "Galatea", "Fazer 3 contests", "Bônus diário"),
    ("Hunting", "Kiara", "Galatea", "Entregar shards pedidos", "~7,3M a 8,6M coins"),
    ("Hunting", "Sanger", "Galatea", "Entregar shards pedidos", "~13,25M coins"),
]

# (tarefa, onde, cooldown em horas)
TIMERS = [
    ("Garden Visitors (fila de 5 cheia)", "Garden", 1.25),  # 5 visitors x 15 min base
    ("Pest Traps cheias", "Garden", 0.5),
    ("Jacob's Contest", "Garden", 1),
    ("Composter sem Organic Matter", "Garden", 1.5),  # base: 40k OM / 4k por compost x 10 min; maxado ~5h
    ("Greenhouse Growth Stage", "Garden", 4),  # base 4h, mín ~1h45 com upgrades
    ("Rosewater Flasks", "Garden (Greenhouse)", 2),
    ("Honeyhives", "Galatea", 1),
    ("Huntraps", "Galatea", 2),
    ("Honey Lathering (Pot of Honeycomb)", "Galatea (Honey, Torrhus Canyon)", 1),  # wiki: 15 min a 60h, varia com Beekeeper
    ("Dark Auction (~min 55 de cada hora)", "Wilderness (Sirius) - precisa 400k na bolsa", 1),
    ("Booster Cookie", "Elizabeth", 96),
    ("Aranya (semanal, Chyme)", "Crimson Isle", 168),
    ("Tit for Tat (Ubik)", "Rift", 2),
    ("Time Tower", "Chocolate Factory", 8),
    ("Hungry Hiker", "-", 12),
    ("Enchanted Clock", "-", 48),
    ("Century Cakes", "-", 48),
    ("Sirih's Sulphur", "Crimson Isle", 48),
    ("Pablo's Flower", "Crimson Isle", 48),
    ("Avorius's Cup of Blood", "Crimson Isle", 72),
    ("Turd's Toy", "Crimson Isle", 120),
]


def header(ws, row, cols):
    for i, h in enumerate(cols, 1):
        c = ws.cell(row, i, h)
        c.fill, c.font = HDR, WHITE_B


wb = Workbook()
ws = wb.active
ws.title = "Diárias"
n = len(DAILY)
first, last = 5, 4 + n
ws["A1"] = "SKYBLOCK - CHECKLIST DIÁRIO"; ws["A1"].font = Font(bold=True, size=14)
ws["A2"] = "Hoje:"; ws["B2"] = "=TODAY()"; ws["B2"].number_format = "dd/mm/yyyy"
ws["C2"] = f'=COUNTIF(A{first}:A{last},"✅")&" / {n} feitas"'; ws["C2"].font = Font(bold=True)
ws["D2"] = f"=COUNTIF(A{first}:A{last},\"✅\")/{n}"; ws["D2"].number_format = "0%"
ws["A3"] = "Como usar: na coluna G aperte Ctrl+; (data de hoje). Amanhã volta a ⬜ sozinho."
ws["A3"].font = Font(italic=True, color="666666")
header(ws, 4, ["Feito", "Categoria", "Tarefa", "Onde / NPC", "O que fazer", "Recompensa", "Data feito (Ctrl+;)"])
for r, row in enumerate(DAILY, first):
    ws.cell(r, 1, f'=IF(H{r}=TODAY(),"✅","⬜")').alignment = Alignment(horizontal="center")
    for i, v in enumerate(row, 2):
        ws.cell(r, i, v)
    ws.cell(r, 7).number_format = "dd/mm/yyyy"
    # H (oculta): data real, ou texto "d/m", "d/m/aa", "d/m/aaaa" lido como dia/mês
    g, s1 = f"G{r}", f'FIND("/",G{r})'
    s2 = f'FIND("/",G{r},{s1}+1)'
    y = f"IFERROR(VALUE(MID({g},{s2}+1,4)),YEAR(TODAY()))"
    ws.cell(r, 8, f'=IF({g}="","",IF(ISNUMBER({g}),INT({g}),IFERROR(DATE(IF({y}<100,{y}+2000,{y}),'
                  f'VALUE(MID({g},{s1}+1,IFERROR({s2},99)-{s1}-1)),VALUE(LEFT({g},{s1}-1))),"")))')
    ws.cell(r, 8).number_format = "dd/mm/yyyy"
ws.column_dimensions["H"].hidden = True
ws.conditional_formatting.add(f"A{first}:G{last}", FormulaRule(formula=[f"$H{first}=TODAY()"], fill=GREEN))
for col, w in zip("ABCDEFG", [7, 15, 38, 34, 55, 28, 18]):
    ws.column_dimensions[col].width = w
ws.freeze_panes = "A5"

# Timers: date+time of last use -> when it's ready again
wt = wb.create_sheet("Timers")
m = len(TIMERS)
tf, tl = 4, 3 + m
wt["A1"] = "TIMERS / COOLDOWNS"; wt["A1"].font = Font(bold=True, size=14)
wt["A2"] = "Na coluna E aperte Ctrl+; espaço Ctrl+Shift+; (data e hora). Status vira PRONTO quando acabar o cooldown."
wt["A2"].font = Font(italic=True, color="666666")
header(wt, 3, ["Status", "Tarefa", "Onde", "Cooldown (h)", "Última vez (data hora)", "Pronto em"])
for r, (t, onde, h) in enumerate(TIMERS, tf):
    wt.cell(r, 1, f'=IF(E{r}="","PRONTO",IF(NOW()>=F{r},"PRONTO","aguardando"))')
    wt.cell(r, 2, t); wt.cell(r, 3, onde); wt.cell(r, 4, h)
    wt.cell(r, 5).number_format = "dd/mm/yyyy hh:mm"
    wt.cell(r, 6, f'=IF(E{r}="","",E{r}+D{r}/24)').number_format = "dd/mm hh:mm"
wt.conditional_formatting.add(f"A{tf}:F{tl}", FormulaRule(formula=[f'$A{tf}="PRONTO"'], fill=GREEN))
wt.conditional_formatting.add(f"A{tf}:F{tl}", FormulaRule(formula=[f'$A{tf}="aguardando"'], fill=YELLOW))
for col, w in zip("ABCDEF", [12, 28, 22, 13, 22, 14]):
    wt.column_dimensions[col].width = w
wt.freeze_panes = "A4"

GUIDE = [
    ("Pests", "Liberam no Garden V. Spawn: 0,2% por crop quebrado, cooldown base 5 min (mín ~1m10s com equipamentos Pesthunter + Finnegan). Pest Repellent = 2x, MAX = 4x.", "Só morrem com Vacuum. Com 4+ pests perde 5% FF, até -75% com 8. Offline: 1 pest/h."),
    ("Pest Traps", "Comprar com Pesthunter Phillip (50 pests). Até 3 no Garden, 1 pest a cada ~15 min.", "Isca: Compost→Mosquito/Earthworm, Plant Matter→Locust/Slug, Dung→Beetle/Fly, Honey Jar→Moth/Cricket, Cheese→Rat/Mite, Jelly→Dragonfly/Firefly/Mantis, Moondew→Lunar Moth."),
    ("Visitors", "Cooldown base 15 min, fila 5 (até 10 com Max Occupancy). Better Advertising tira 1 min por tier (6 tiers).", "Matar pest reduz 30s do cooldown. Special visitors (Spaceman, Taylor) só vêm com Bachelor's Rose. Recompensas raras: Hypercharge/Quickdraw Chip, Flowering Bouquet."),
    ("Greenhouse", "Libera no Garden VII (visitor Carpenter dá o Blueprint). Cada estágio cresce em 4h (mín ~1h45). Cresce offline.", "Água: -20 por ciclo, use Hydrocan. Efeitos passam p/ plantas vizinhas (não diagonal). Plantas decaem (vida base 3 dias). Ethereal Vine libera mais slots e Greenhouses #2/#3."),
    ("Composter", "1 Compost a cada 10 min, gasta 4.000 Organic Matter + 2.000 Fuel. Cap base 40k OM (~1h40 sozinho).", "Melhor OM: Condensed Fermento, Fermento, Helianthus, Enchanted Hay Bale. Fuel: Biofuel. Itens não saem depois de colocados. Upgrades: Speed, Multi Drop, Fuel Cap, OM Cap, Cost Reduction."),
    ("Jacob's Contest", "A cada 1h real, dura 20 min, 3 crops aleatórios. Precisa Farming 10 e ≥100 do crop.", "Medalhas: Bronze top 60%, Silver 30%, Gold 10%, Platinum 5%, Diamond 2%. Gold sobe o cap de Farming (até 60)."),
    ("Copper / Desk", "Copper vem dos visitors (≥2 por oferta).", "Gastar em Crop Upgrades (+5 Crop Fortune por tier), Visitor Upgrades e Composter Upgrades."),
    ("Plots", "24 plots, cada um dá +3 Farming Fortune. Custam Compost / Compost Bundles.", "Limpar o plot depois de liberar. Presets na Desk."),
]
wg = wb.create_sheet("Guia Garden")
wg["A1"] = "GUIA DO GARDEN (wiki, nov/2026)"; wg["A1"].font = Font(bold=True, size=14)
header(wg, 3, ["Sistema", "Como funciona", "Dicas"])
for r, row in enumerate(GUIDE, 4):
    for i, v in enumerate(row, 1):
        wg.cell(r, i, v).alignment = Alignment(wrap_text=True, vertical="top")
    wg.cell(r, 1).font = Font(bold=True)
for col, w in zip("ABC", [16, 70, 90]):
    wg.column_dimensions[col].width = w

wb.save(OUT)
print("ok", OUT, n, m)
