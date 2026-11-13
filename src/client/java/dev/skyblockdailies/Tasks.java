package dev.skyblockdailies;

import java.util.List;

/** Generated from the Skyblock_Diarias.xlsx spreadsheet data (gen_tasks.py). */
final class Tasks {
	record Task(String id, String section, String name, String tip, String reset) {}

	static final List<String> SECTIONS = List.of("General", "Private Island", "Hub", "Garden", "Dwarven Mines", "Crimson Isle", "Dungeons", "Galatea", "Rift", "Timers");

	static final List<Task> ALL = List.of(
			new Task("Daily Reward do Hypixel", "General", "Hypixel Daily Reward", "§eWhere: §fMain lobby (Delivery Man)\n§eWhat to do: §fClaim the daily reward / keep the streak\n§eReward: §fTokens / network XP", "ET?"),
			new Task("Checar Calendar (eventos)", "General", "Check the Calendar (events)", "§eWhere: §f/calendar\n§eWhat to do: §fLook for Hoppity's Hunt, Spooky, Jerry, Dark Auction, mayor, etc.\n§eReward: §fEvent rewards", "ET?"),
			new Task("Coletar minions", "Private Island", "Collect minions", "§eWhere: §fPrivate Island\n§eWhat to do: §fEmpty your minions and sell/store\n§eReward: §fCoins / collections", "ET?"),
			new Task("Experimentation Table", "Private Island", "Experimentation Table", "§eWhere: §fPrivate Island (Experimentation Table)\n§eWhat to do: §fDo Superpairs, Chronomatron and Ultrasequencer\n§eReward: §fEnchanted books, XP, rare items", "CD24"),
			new Task("Checar ordens do Bazaar / AH", "Hub", "Check Bazaar / AH orders", "§eWhere: §fBazaar / Auction House\n§eWhat to do: §fClaim orders and auctions, relist\n§eReward: §fCoins", "ET?"),
			new Task("Checar buffs (Booster Cookie / God Potion / Century Cake)", "Hub", "Check buffs (Booster Cookie / God Potion / Century Cake)", "§eWhere: §fElizabeth (Community Center)\n§eWhat to do: §fRenew if expired; spend Bits (God Potion, Kismet, Matriarch's Perfume)\n§eReward: §fBits and active buffs", "ET?"),
			new Task("Kat - pet upgrade", "Hub", "Kat - pet upgrade", "§eWhere: §fHub (Kat)\n§eWhat to do: §fCollect the finished pet and send the next one\n§eReward: §fHigher rarity pet", "ET?"),
			new Task("Matar pests acumuladas", "Garden", "Kill accumulated pests", "§eWhere: §fGarden (all plots)\n§eWhat to do: §fOn login: vacuum the pests (1/h spawn offline, max 8). 4+ pests cost Farming Fortune\n§eReward: §f1,000 coins + 1,500 Farming XP + Bits each", "ET?"),
			new Task("Esvaziar Pest Traps + recolocar isca", "Garden", "Empty Pest Traps + rebait", "§eWhere: §fGarden (up to 3 traps)\n§eWhat to do: §fKill trapped pests and add bait (Compost, Dung, Cheese, Honey Jar, Plant Matter, Jelly, Moondew)\n§eReward: §fChosen pests / Field Mouse chance", "CD0.5"),
			new Task("Atender todos os Visitors", "Garden", "Serve all Visitors", "§eWhere: §fGarden (Barn)\n§eWhat to do: §fGive requested items; decline bad offers. Queue max 5 (10 with upgrade)\n§eReward: §fCopper, Bits, Farming XP, rare chips", "CD1.25"),
			new Task("Greenhouse: colher e replantar", "Garden", "Greenhouse: harvest and replant", "§eWhere: §fGarden (Greenhouse, Garden VII)\n§eWhat to do: §fHarvest mature plants, replant, set up mutations\n§eReward: §fCrops, Ethereal Vine, +320 Farming XP per harvest", "CD4"),
			new Task("Greenhouse: regar (Hydrocan)", "Garden", "Greenhouse: water (Hydrocan)", "§eWhere: §fGarden (Greenhouse)\n§eWhat to do: §fKeep water above 0 (-20 per cycle; stalls at -100)\n§eReward: §fGrowth keeps going", "CD4"),
			new Task("Greenhouse: Rosewater Flask", "Garden", "Greenhouse: Rosewater Flask", "§eWhere: §fGarden (Greenhouse)\n§eWhat to do: §fUse a flask on each Greenhouse plot (2h cooldown)\n§eReward: §fPlot buff", "CD2"),
			new Task("Encher Composter (Organic Matter + Fuel)", "Garden", "Fill Composter (Organic Matter + Fuel)", "§eWhere: §fGarden (Composter)\n§eWhat to do: §fAdd crops/enchanted (OM) and Biofuel/Oil (fuel). Collect Compost\n§eReward: §fCompost (plots, Greenhouse slots, sell)", "CD1.5"),
			new Task("Bachelor's Roses (5)", "Garden", "Bachelor's Roses (5)", "§eWhere: §fGarden\n§eWhat to do: §fDyes to Vincent, Farming XP on a Legendary visitor, Supreme Voter's Badge to the mayor\n§eReward: §fRoses (needed for Special visitors)", "UTC"),
			new Task("Jacob's Contest", "Garden", "Jacob's Contest", "§eWhere: §fGarden (Jacob)\n§eWhat to do: §f1 contest per real hour (lasts 20 min). Pre-spawn pests and plant the contest crop in the Greenhouse\n§eReward: §fMedals, Jacob's Tickets, Bits", "CD1"),
			new Task("Gastar Copper", "Garden", "Spend Copper", "§eWhere: §fGarden (Desk / SkyMart)\n§eWhat to do: §fCrop Upgrades, Visitor Upgrades, Composter Upgrades\n§eReward: §fFortune, shorter cooldowns, more compost", "ET?"),
			new Task("Trocar pests com Phillip (opcional)", "Garden", "Trade pests with Phillip (optional)", "§eWhere: §fGarden (Pesthunter Phillip)\n§eWhat to do: §fTrade pests for a buff before farming\n§eReward: §f+5 Farming Fortune for 30 min per pest (max 40)", "ET?"),
			new Task("Checar Forge", "Dwarven Mines", "Check the Forge", "§eWhere: §fDwarven Mines (Forge)\n§eWhat to do: §fClaim finished items and start new ones\n§eReward: §fForged items", "ET?"),
			new Task("Fetchur", "Dwarven Mines", "Fetchur", "§eWhere: §fDwarven Mines\n§eWhat to do: §fBring him the item of the day\n§eReward: §fMithril Powder", "ET"),
			new Task("Fossil Muncher", "Dwarven Mines", "Fossil Muncher", "§eWhere: §fGlacite Tunnels\n§eWhat to do: §fUse it (~24h cooldown)\n§eReward: §fFossil reward", "CD24"),
			new Task("Faction Quests (5)", "Crimson Isle", "Faction Quests (5)", "§eWhere: §fCrimson Isle (quest board)\n§eWhat to do: §fComplete the 5 daily quests\n§eReward: §f+30 to 125 Reputation each", "ET?"),
			new Task("Mini-bosses (5)", "Crimson Isle", "Mini-bosses (5)", "§eWhere: §fCrimson Isle\n§eWhat to do: §fAshfang, Barbarian Duke X, Bladesoul, Magma Boss, Mage Outlaw\n§eReward: §f+20 Reputation each", "ET?"),
			new Task("Kuudra (1 run por tier)", "Crimson Isle", "Kuudra (1 run per tier)", "§eWhere: §fCrimson Isle (Kuudra)\n§eWhat to do: §fBasic/Hot/Burning/Fiery/Infernal - 1 of each\n§eReward: §f+10 to 50 Reputation", "ET?"),
			new Task("Training Session (Kaus ou Rollim)", "Crimson Isle", "Training Session (Kaus or Rollim)", "§eWhere: §fCrimson Isle\n§eWhat to do: §fDo your faction's training\n§eReward: §f+25 Reputation per run", "ET?"),
			new Task("Heavy Pearls (Matriarch)", "Crimson Isle", "Heavy Pearls (Matriarch)", "§eWhere: §fCrimson Isle (Matriarch)\n§eWhat to do: §fCollect the pearls\n§eReward: §fHeavy Pearls", "ET20"),
			new Task("Pomtair - Enchanted Bone Meal", "Crimson Isle", "Pomtair - Enchanted Bone Meal", "§eWhere: §fCrimson Isle\n§eWhat to do: §fHand it in (24h cooldown)\n§eReward: §f+50 Barbarian Rep.", "CD24"),
			new Task("5 runs com bônus de XP", "Dungeons", "5 dungeon runs (XP bonus)", "§eWhere: §fCatacombs\n§eWhat to do: §fComplete 5 dungeon runs\n§eReward: §f+40% Dungeoneering XP", "ET?"),
			new Task("Daily Wishes (3)", "Galatea", "Daily Wishes (3)", "§eWhere: §fGalatea\n§eWhat to do: §fOnce each: Fig, Mangrove, Helix Log\n§eReward: §fBonus", "ET?"),
			new Task("Free Trial (1 Tree Gift)", "Galatea", "Free Trial (1 Tree Gift)", "§eWhere: §fGalatea\n§eWhat to do: §fComplete your first Tree Gift of the day\n§eReward: §fBasic Safari Ticket", "ET?"),
			new Task("250 Tree Gifts", "Galatea", "250 Tree Gifts", "§eWhere: §fGalatea\n§eWhat to do: §fComplete the daily Tree Gifts (up to 250)\n§eReward: §fTree Gift rewards", "ET?"),
			new Task("Starlyn Contests (3 bônus)", "Galatea", "Starlyn Contests (3 bonuses)", "§eWhere: §fGalatea\n§eWhat to do: §fDo 3 contests\n§eReward: §fDaily bonus", "ET?"),
			new Task("Kiara", "Galatea", "Kiara", "§eWhere: §fGalatea\n§eWhat to do: §fHand in the requested shards\n§eReward: §f~7.3M to 8.6M coins", "ET?"),
			new Task("Sanger", "Galatea", "Sanger", "§eWhere: §fGalatea\n§eWhat to do: §fHand in the requested shards\n§eReward: §f~13.25M coins", "ET?"),
			new Task("Ubik - Split or Steal (Tit for Tat)", "Rift", "Ubik - Split or Steal (Tit for Tat)", "§eWhere: §fRift (Ubik's Cube)\n§eWhat to do: §fPlay Split or Steal (2h cooldown)\n§eReward: §fMotes (1k to 5k)", "CD2"),
			new Task("Ubik - Three Options", "Rift", "Ubik - Three Options", "§eWhere: §fRift (Ubik)\n§eWhat to do: §fPick 1 of 3 boxes (once per Rift visit)\n§eReward: §fIQ Point / Enigma Soul", "ET?"),
			new Task("Honeyhives", "Timers", "Honeyhives", "§eWhere: §fGalatea\n§eClick when you use it; you get a notice when it is ready again.", "CD1"),
			new Task("Huntraps", "Timers", "Huntraps", "§eWhere: §fGalatea\n§eClick when you use it; you get a notice when it is ready again.", "CD2"),
			new Task("Honey Lathering (Pot of Honeycomb)", "Timers", "Honey Lathering (Pot of Honeycomb)", "§eWhere: §fGalatea (Honey, Torrhus Canyon)\n§eClick when you use it; you get a notice when it is ready again.", "CD1"),
			new Task("Booster Cookie", "Timers", "Booster Cookie", "§eWhere: §fElizabeth\n§eClick when you use it; you get a notice when it is ready again.", "CD96"),
			new Task("Aranya (semanal, Chyme)", "Timers", "Aranya (weekly, Chyme)", "§eWhere: §fCrimson Isle\n§eClick when you use it; you get a notice when it is ready again.", "CD168"),
			new Task("Time Tower", "Timers", "Time Tower", "§eWhere: §fChocolate Factory\n§eClick when you use it; you get a notice when it is ready again.", "CD8"),
			new Task("Hungry Hiker", "Timers", "Hungry Hiker", "§eWhere: §f-\n§eClick when you use it; you get a notice when it is ready again.", "CD12"),
			new Task("Enchanted Clock", "Timers", "Enchanted Clock", "§eWhere: §f-\n§eClick when you use it; you get a notice when it is ready again.", "CD48"),
			new Task("Century Cakes", "Timers", "Century Cakes", "§eWhere: §f-\n§eClick when you use it; you get a notice when it is ready again.", "CD48"),
			new Task("Sirih's Sulphur", "Timers", "Sirih's Sulphur", "§eWhere: §fCrimson Isle\n§eClick when you use it; you get a notice when it is ready again.", "CD48"),
			new Task("Pablo's Flower", "Timers", "Pablo's Flower", "§eWhere: §fCrimson Isle\n§eClick when you use it; you get a notice when it is ready again.", "CD48"),
			new Task("Avorius's Cup of Blood", "Timers", "Avorius's Cup of Blood", "§eWhere: §fCrimson Isle\n§eClick when you use it; you get a notice when it is ready again.", "CD72"),
			new Task("Turd's Toy", "Timers", "Turd's Toy", "§eWhere: §fCrimson Isle\n§eClick when you use it; you get a notice when it is ready again.", "CD120")
	);
}
