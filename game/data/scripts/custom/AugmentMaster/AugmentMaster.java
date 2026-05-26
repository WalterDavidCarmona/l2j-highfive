/*
 * Copyright (c) 2013 L2jMobius
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package custom.AugmentMaster;

import java.io.FileInputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.options.Augmentation;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Augment Master NPC - Allows players to choose and apply weapon augmentations.
 * Configuration: game/config/Custom/AugmentMaster.ini
 * @author Custom
 */
public class AugmentMaster extends Script
{
	// ── Configuration (loaded from AugmentMaster.ini) ───────────────────────────
	private static final Properties CONFIG = new Properties();
	private static final boolean ENABLED;
	private static final int NPC_ID;
	private static final int ITEM_ID;
	private static final long ITEM_COUNT;
	private static final int REMOVE_ITEM_ID;
	private static final long REMOVE_ITEM_COUNT;
	private static final int[] AVAILABLE_OPTIONS;

	static
	{
		try
		{
			CONFIG.load(new FileInputStream("./config/Custom/AugmentMaster.ini"));
		}
		catch (Exception e)
		{
			LOGGER.warning("AugmentMaster: Could not read config file, using defaults.");
		}

		ENABLED = Boolean.parseBoolean(CONFIG.getProperty("Enabled", "true"));
		NPC_ID = Integer.parseInt(CONFIG.getProperty("NpcId", "1004000").trim());
		ITEM_ID = Integer.parseInt(CONFIG.getProperty("ItemId", "57").trim());
		ITEM_COUNT = Long.parseLong(CONFIG.getProperty("ItemCount", "1000000").trim());
		REMOVE_ITEM_ID = Integer.parseInt(CONFIG.getProperty("RemoveItemId", "57").trim());
		REMOVE_ITEM_COUNT = Long.parseLong(CONFIG.getProperty("RemoveItemCount", "0").trim());

		final String raw = CONFIG.getProperty("AugmentOptionIds",
			// Active Skills
			"16200,16201,16202,16203,16204,16205,16206,16207,16208,16209,16210,16211," +
			"16212,16213,16214,16215,16216,16217,16218,16219,16220," +
			"16221,16222,16223,16224,16225,16226,16227,16228,16229,16230," +
			"16231,16232,16235,16287,16288,16289,16290,16291,16293,16294," +
			"16295,16296,16297,16298,16299,16300,16301,16302,16303,16304," +
			// Chance: On Attack
			"16238,16239,16240,16241,16242,16243,16305,16306,16307,16308,16309,16310," +
			// Chance: On Critical
			"16244,16245,16246,16247,16248,16249,16311,16312,16313,16314,16315,16316," +
			// Chance: On Magic Attack
			"16273,16274,16275,16276,16277,16278,16325,16326,16327,16328,16329,16330," +
			// Chance: On Defense
			"16250,16251,16252,16253,16254,16255,16256,16257,16258,16259,16260,16261," +
			"16262,16263,16264,16265,16266,16267,16268,16269,16270,16271,16272," +
			"16317,16318,16319,16320,16321,16322,16323,16324," +
			// Passive Skills
			"16279,16280,16281,16282,16283,16284,16285,16286," +
			"16331,16332,16333,16334,16335,16336,16337,16338,16339,16340," +
			// Stats
			"16377,16378,16379,16380");

		final String[] tokens = raw.split(",");
		AVAILABLE_OPTIONS = new int[tokens.length];
		for (int i = 0; i < tokens.length; i++)
		{
			AVAILABLE_OPTIONS[i] = Integer.parseInt(tokens[i].trim());
		}
	}

	// ── Augment Type Enum ────────────────────────────────────────────────────────
	private enum AugmentType
	{
		ACTIVE("Active Skills", "A0C4FF"),
		CHANCE_ATTACK("Chance: On Attack", "FFD966"),
		CHANCE_CRITICAL("Chance: On Critical", "FF9966"),
		CHANCE_MAGIC("Chance: On Magic Attack", "FF66CC"),
		CHANCE_DEFENSE("Chance: On Defense", "A8D5A2"),
		PASSIVE("Passive Skills", "D4A0FF"),
		STATS("Stat Bonuses", "CCCCCC");

		final String displayName;
		final String color;

		AugmentType(String displayName, String color)
		{
			this.displayName = displayName;
			this.color = color;
		}
	}

	// ── Augment Info Record ──────────────────────────────────────────────────────
	private static class AugmentInfo
	{
		final int optionId;
		final String name;
		final String description;
		final AugmentType type;

		AugmentInfo(int optionId, String name, String description, AugmentType type)
		{
			this.optionId = optionId;
			this.name = name;
			this.description = description;
			this.type = type;
		}
	}

	// ── Augment Catalog ──────────────────────────────────────────────────────────
	private static final Map<Integer, AugmentInfo> AUGMENT_CATALOG = new LinkedHashMap<>();

	static
	{
		// ══════════════════════════════════════════════════════════════════
		// Active Skills
		// ══════════════════════════════════════════════════════════════════
		reg(16200, "Blessed Soul", "Temporarily increases Maximum MP.", AugmentType.ACTIVE);
		reg(16201, "Magic Barrier", "Temporarily increases Magic Defense.", AugmentType.ACTIVE);
		reg(16202, "Mana Burn", "Burns up your enemy's MP.", AugmentType.ACTIVE);
		reg(16203, "Mana Gain", "Increases the recovery rate when MP is being recovered by recharge.", AugmentType.ACTIVE);
		reg(16204, "Recharge", "Restores the target's MP.", AugmentType.ACTIVE);
		reg(16205, "Aura Flare", "Inflicts a non-attribute magic attack.", AugmentType.ACTIVE);
		reg(16206, "Might", "Temporarily increases Physical Attack.", AugmentType.ACTIVE);
		reg(16207, "Paralyze", "Inflicts paralysis on the target.", AugmentType.ACTIVE);
		reg(16208, "Shield", "Temporarily increases Physical Defense.", AugmentType.ACTIVE);
		reg(16209, "Poison", "Inflicts poison on the target.", AugmentType.ACTIVE);
		reg(16210, "Duel Weakness", "Temporarily decreases the PvP opponent's P. Atk.", AugmentType.ACTIVE);
		reg(16211, "Duel Might", "Temporarily increases PvP Physical Attack.", AugmentType.ACTIVE);
		reg(16212, "Recall", "Teleports to the nearest town.", AugmentType.ACTIVE);
		reg(16213, "Resurrection", "Resurrects a fallen target.", AugmentType.ACTIVE);
		reg(16214, "Stone (AoE)", "Emits an area of effect earth attack.", AugmentType.ACTIVE);
		reg(16215, "Prominence (AoE)", "Emits an area of effect fire attack.", AugmentType.ACTIVE);
		reg(16216, "Solar Flare (AoE)", "Emits an area of effect holy magic attack.", AugmentType.ACTIVE);
		reg(16217, "Aura Flare (AoE)", "Emits elemental damage over an area.", AugmentType.ACTIVE);
		reg(16218, "Shadow Flare (AoE)", "Emits an area of effect dark attack.", AugmentType.ACTIVE);
		reg(16219, "Hydro Blast (AoE)", "Emits an area of effect water attack.", AugmentType.ACTIVE);
		reg(16220, "Hurricane (AoE)", "Emits an area of effect wind attack.", AugmentType.ACTIVE);
		reg(16221, "Sleep", "Inflicts sleep on the target.", AugmentType.ACTIVE);
		reg(16222, "Slow", "Decreases the target's moving speed.", AugmentType.ACTIVE);
		reg(16223, "Stun", "Inflicts stun on the target.", AugmentType.ACTIVE);
		reg(16224, "Stone", "Inflicts an earth attack.", AugmentType.ACTIVE);
		reg(16225, "Prominence", "Inflicts a fire attack.", AugmentType.ACTIVE);
		reg(16226, "Solar Flare", "Inflicts a holy magic attack.", AugmentType.ACTIVE);
		reg(16227, "Aura Flare (Single)", "Emits elemental damage on a single target.", AugmentType.ACTIVE);
		reg(16228, "Shadow Flare", "Inflicts a dark attack.", AugmentType.ACTIVE);
		reg(16229, "Hydro Blast", "Inflicts a water attack.", AugmentType.ACTIVE);
		reg(16230, "Hurricane", "Inflicts a wind attack.", AugmentType.ACTIVE);
		reg(16231, "Trick", "Cancels your enemy's target.", AugmentType.ACTIVE);
		reg(16232, "Medusa", "Inflicts petrification on the target.", AugmentType.ACTIVE);
		reg(16235, "Vampiric Touch", "Absorbs HP from your target.", AugmentType.ACTIVE);
		reg(16287, "Refresh", "Temporarily decreases all skill reuse time.", AugmentType.ACTIVE);
		reg(16288, "Clarity", "Reduces MP consumption of all skills for a fixed time.", AugmentType.ACTIVE);
		reg(16289, "Focus", "Temporarily increases the probability of critical attack.", AugmentType.ACTIVE);
		reg(16290, "Reflect Damage", "Reflects a portion of received physical damage for a fixed time.", AugmentType.ACTIVE);
		reg(16291, "Doom", "Blocks all of the target's physical/magic skills.", AugmentType.ACTIVE);
		reg(16293, "Celestial Shield", "Becomes invincible momentarily.", AugmentType.ACTIVE);
		reg(16294, "Wild Magic", "Increases the Critical Rate of magic for a fixed time.", AugmentType.ACTIVE);
		reg(16295, "Party Recall", "Teleports party members to a town.", AugmentType.ACTIVE);
		reg(16296, "Silence", "Blocks the target's magic skills.", AugmentType.ACTIVE);
		reg(16297, "Skill Refresh", "Temporarily decreases skill reuse time.", AugmentType.ACTIVE);
		reg(16298, "Skill Clarity", "Reduces MP consumption of a skill for a fixed time.", AugmentType.ACTIVE);
		reg(16299, "Music Refresh", "Temporarily decreases the reuse time of song/dance skills.", AugmentType.ACTIVE);
		reg(16300, "Music Clarity", "Reduces MP consumption of songs and dances for a fixed duration.", AugmentType.ACTIVE);
		reg(16301, "Spell Refresh", "Temporarily decreases magic reuse time.", AugmentType.ACTIVE);
		reg(16302, "Spell Clarity", "Reduces MP consumption of all magic for a fixed duration.", AugmentType.ACTIVE);
		reg(16303, "Stealth", "Avoids monster preemptive attacks. Attack abilities are reduced.", AugmentType.ACTIVE);
		reg(16304, "Vampiric Rage", "Restores HP proportional to physical damage dealt.", AugmentType.ACTIVE);

		// ══════════════════════════════════════════════════════════════════
		// Chance: Physical Attack
		// ══════════════════════════════════════════════════════════════════
		reg(16238, "Winter on Attack", "Chance to decrease the target's Atk. Spd.", AugmentType.CHANCE_ATTACK);
		reg(16239, "Bleed on Attack", "Chance to inflict bleed on the target.", AugmentType.CHANCE_ATTACK);
		reg(16240, "Fear on Attack", "Chance to cause the target to flee.", AugmentType.CHANCE_ATTACK);
		reg(16241, "Hold on Attack", "Chance to inflict hold on the target.", AugmentType.CHANCE_ATTACK);
		reg(16242, "Poison on Attack", "Chance to inflict poison on the target.", AugmentType.CHANCE_ATTACK);
		reg(16243, "Medusa on Attack", "Chance to inflict petrification on the target.", AugmentType.CHANCE_ATTACK);
		reg(16305, "Doom on Attack", "Chance to block all skills of the target.", AugmentType.CHANCE_ATTACK);
		reg(16306, "Mana Burn on Attack", "Chance to burn the target's MP.", AugmentType.CHANCE_ATTACK);
		reg(16307, "Paralyze on Attack", "Chance to inflict paralysis on the target.", AugmentType.CHANCE_ATTACK);
		reg(16308, "Silence on Attack", "Chance to block the target's magic skills.", AugmentType.CHANCE_ATTACK);
		reg(16309, "Sleep on Attack", "Chance to inflict sleep on the target.", AugmentType.CHANCE_ATTACK);
		reg(16310, "Stun on Attack", "Chance to inflict stun on the target.", AugmentType.CHANCE_ATTACK);

		// ══════════════════════════════════════════════════════════════════
		// Chance: Critical Hit
		// ══════════════════════════════════════════════════════════════════
		reg(16244, "Winter on Critical", "Chance to decrease the target's Atk. Spd. on critical.", AugmentType.CHANCE_CRITICAL);
		reg(16245, "Bleed on Critical", "Chance to inflict bleed on critical.", AugmentType.CHANCE_CRITICAL);
		reg(16246, "Fear on Critical", "Chance to cause the target to flee on critical.", AugmentType.CHANCE_CRITICAL);
		reg(16247, "Hold on Critical", "Chance to inflict hold on critical.", AugmentType.CHANCE_CRITICAL);
		reg(16248, "Poison on Critical", "Chance to inflict poison on critical.", AugmentType.CHANCE_CRITICAL);
		reg(16249, "Medusa on Critical", "Chance to inflict petrification on critical.", AugmentType.CHANCE_CRITICAL);
		reg(16311, "Doom on Critical", "Chance to block all skills of the target on critical.", AugmentType.CHANCE_CRITICAL);
		reg(16312, "Mana Burn on Critical", "Chance to burn the target's MP on critical.", AugmentType.CHANCE_CRITICAL);
		reg(16313, "Paralyze on Critical", "Chance to inflict paralysis on critical.", AugmentType.CHANCE_CRITICAL);
		reg(16314, "Silence on Critical", "Chance to block the target's magic skills on critical.", AugmentType.CHANCE_CRITICAL);
		reg(16315, "Sleep on Critical", "Chance to inflict sleep on critical.", AugmentType.CHANCE_CRITICAL);
		reg(16316, "Stun on Critical", "Chance to inflict stun on critical.", AugmentType.CHANCE_CRITICAL);

		// ══════════════════════════════════════════════════════════════════
		// Chance: On Magic Attack
		// ══════════════════════════════════════════════════════════════════
		reg(16273, "Winter on Magic", "Chance to decrease the target's Atk. Spd. on magic attack.", AugmentType.CHANCE_MAGIC);
		reg(16274, "Bleed on Magic", "Chance to inflict bleed on magic attack.", AugmentType.CHANCE_MAGIC);
		reg(16275, "Fear on Magic", "Chance to cause the target to flee on magic attack.", AugmentType.CHANCE_MAGIC);
		reg(16276, "Hold on Magic", "Chance to inflict hold on magic attack.", AugmentType.CHANCE_MAGIC);
		reg(16277, "Poison on Magic", "Chance to inflict poison on magic attack.", AugmentType.CHANCE_MAGIC);
		reg(16278, "Medusa on Magic", "Chance to inflict petrification on magic attack.", AugmentType.CHANCE_MAGIC);
		reg(16325, "Doom on Magic", "Chance to block all skills of the target on magic attack.", AugmentType.CHANCE_MAGIC);
		reg(16326, "Mana Burn on Magic", "Chance to burn the target's MP on magic attack.", AugmentType.CHANCE_MAGIC);
		reg(16327, "Paralyze on Magic", "Chance to inflict paralysis on magic attack.", AugmentType.CHANCE_MAGIC);
		reg(16328, "Silence on Magic", "Chance to block the target's magic skills on magic attack.", AugmentType.CHANCE_MAGIC);
		reg(16329, "Sleep on Magic", "Chance to inflict sleep on magic attack.", AugmentType.CHANCE_MAGIC);
		reg(16330, "Stun on Magic", "Chance to inflict stun on magic attack.", AugmentType.CHANCE_MAGIC);

		// ══════════════════════════════════════════════════════════════════
		// Chance: On Defense (When Receiving Damage)
		// ══════════════════════════════════════════════════════════════════
		reg(16250, "Winter on Defense", "Chance to decrease the attacker's Atk. Spd.", AugmentType.CHANCE_DEFENSE);
		reg(16251, "Agility on Defense", "Chance to increase Evasion when receiving damage.", AugmentType.CHANCE_DEFENSE);
		reg(16252, "Bleed on Defense", "Chance to inflict bleed on the attacker.", AugmentType.CHANCE_DEFENSE);
		reg(16253, "Ritual on Defense", "Chance to restore CP when receiving damage.", AugmentType.CHANCE_DEFENSE);
		reg(16254, "Focus on Defense", "Chance to increase Critical Rate when receiving damage.", AugmentType.CHANCE_DEFENSE);
		reg(16255, "Charm on Defense", "Chance to decrease the attacker's aggression.", AugmentType.CHANCE_DEFENSE);
		reg(16256, "Guidance on Defense", "Chance to increase Accuracy when receiving damage.", AugmentType.CHANCE_DEFENSE);
		reg(16257, "Hold on Defense", "Chance to inflict hold on the attacker.", AugmentType.CHANCE_DEFENSE);
		reg(16258, "Prayer on Defense", "Chance to increase HP recovery effect when receiving damage.", AugmentType.CHANCE_DEFENSE);
		reg(16259, "Heal on Defense", "Chance to restore HP when receiving damage.", AugmentType.CHANCE_DEFENSE);
		reg(16260, "Empower on Defense", "Chance to increase M. Atk. when receiving damage.", AugmentType.CHANCE_DEFENSE);
		reg(16261, "Wild Magic on Defense", "Chance to increase Magic Critical Rate when receiving damage.", AugmentType.CHANCE_DEFENSE);
		reg(16262, "Cheer on Defense", "Chance to increase Max CP when receiving damage.", AugmentType.CHANCE_DEFENSE);
		reg(16263, "Blessed Body on Defense", "Chance to increase Max HP when receiving damage.", AugmentType.CHANCE_DEFENSE);
		reg(16264, "Blessed Soul on Defense", "Chance to increase Max MP when receiving damage.", AugmentType.CHANCE_DEFENSE);
		reg(16265, "Magic Barrier on Defense", "Chance to increase M. Def. when receiving damage.", AugmentType.CHANCE_DEFENSE);
		reg(16266, "Might on Defense", "Chance to increase P. Atk. when receiving damage.", AugmentType.CHANCE_DEFENSE);
		reg(16267, "Shield on Defense", "Chance to increase P. Def. when receiving damage.", AugmentType.CHANCE_DEFENSE);
		reg(16268, "Poison on Defense", "Chance to inflict poison on the attacker.", AugmentType.CHANCE_DEFENSE);
		reg(16269, "Duel Weakness on Defense", "Chance to decrease the attacker's PvP P. Atk.", AugmentType.CHANCE_DEFENSE);
		reg(16270, "Duel Might on Defense", "Chance to increase PvP P. Atk. when receiving damage.", AugmentType.CHANCE_DEFENSE);
		reg(16271, "Sleep on Defense", "Chance to inflict sleep on the attacker.", AugmentType.CHANCE_DEFENSE);
		reg(16272, "Slow on Defense", "Chance to decrease the attacker's moving speed.", AugmentType.CHANCE_DEFENSE);
		reg(16317, "Doom on Defense", "Chance to block all skills of the attacker.", AugmentType.CHANCE_DEFENSE);
		reg(16318, "Fear on Defense", "Chance to cause the attacker to flee.", AugmentType.CHANCE_DEFENSE);
		reg(16319, "Mana Gain on Defense", "Chance to increase MP recovery effect when receiving damage.", AugmentType.CHANCE_DEFENSE);
		reg(16320, "Recharge on Defense", "Chance to restore MP when receiving damage.", AugmentType.CHANCE_DEFENSE);
		reg(16321, "Paralyze on Defense", "Chance to inflict paralysis on the attacker.", AugmentType.CHANCE_DEFENSE);
		reg(16322, "Silence on Defense", "Chance to silence the attacker.", AugmentType.CHANCE_DEFENSE);
		reg(16323, "Stun on Defense", "Chance to stun the attacker.", AugmentType.CHANCE_DEFENSE);
		reg(16324, "Medusa on Defense", "Chance to inflict petrification on the attacker.", AugmentType.CHANCE_DEFENSE);

		// ══════════════════════════════════════════════════════════════════
		// Passive Skills
		// ══════════════════════════════════════════════════════════════════
		reg(16279, "Heal Empower", "Increases the power of HP recovery magic.", AugmentType.PASSIVE);
		reg(16280, "Prayer", "Increases HP recovery magic effect.", AugmentType.PASSIVE);
		reg(16281, "Empower", "Increases M. Atk. when the weapon is equipped.", AugmentType.PASSIVE);
		reg(16282, "Magic Barrier", "Increases M. Def. when the weapon is equipped.", AugmentType.PASSIVE);
		reg(16283, "Might", "Increases P. Atk. when the weapon is equipped.", AugmentType.PASSIVE);
		reg(16284, "Shield", "Increases P. Def. when the weapon is equipped.", AugmentType.PASSIVE);
		reg(16285, "Duel Might", "Increases PvP P. Atk. when the weapon is equipped.", AugmentType.PASSIVE);
		reg(16286, "Weight Limit", "Increases the weight limit 2 times.", AugmentType.PASSIVE);
		reg(16331, "Clarity", "Reduces the MP consumption of all skills.", AugmentType.PASSIVE);
		reg(16332, "Agility", "Increases Evasion when the weapon is equipped.", AugmentType.PASSIVE);
		reg(16333, "Focus", "Increases Critical Rate when the weapon is equipped.", AugmentType.PASSIVE);
		reg(16334, "Reflect Damage", "Reflects a portion of received physical damage back to the attacker.", AugmentType.PASSIVE);
		reg(16335, "Guidance", "Increases Accuracy when the weapon is equipped.", AugmentType.PASSIVE);
		reg(16336, "Wild Magic", "Increases Magic Critical Rate when the weapon is equipped.", AugmentType.PASSIVE);
		reg(16337, "Mana Gain", "Increases MP recovery from recharge when the weapon is equipped.", AugmentType.PASSIVE);
		reg(16338, "Skill Clarity", "Decreases skill MP consumption when the weapon is equipped.", AugmentType.PASSIVE);
		reg(16339, "Music Clarity", "Reduces MP consumption of song/dance skills.", AugmentType.PASSIVE);
		reg(16340, "Spell Clarity", "Reduces MP consumption of magic.", AugmentType.PASSIVE);

		// ══════════════════════════════════════════════════════════════════
		// Stat Bonuses
		// ══════════════════════════════════════════════════════════════════
		reg(16377, "STR +1", "Permanently increases Strength by 1.", AugmentType.STATS);
		reg(16378, "CON +1", "Permanently increases Constitution by 1.", AugmentType.STATS);
		reg(16379, "INT +1", "Permanently increases Intelligence by 1.", AugmentType.STATS);
		reg(16380, "MEN +1", "Permanently increases Mentality by 1.", AugmentType.STATS);
	}

	private static void reg(int id, String name, String desc, AugmentType type)
	{
		AUGMENT_CATALOG.put(id, new AugmentInfo(id, name, desc, type));
	}

	// ── Constructor ──────────────────────────────────────────────────────────────
	private AugmentMaster()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}

	// ── onFirstTalk ──────────────────────────────────────────────────────────────
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		// IMPORTANT: must return the filename — returning null causes the engine
		// to show the default "Quest" dialog on top of any manually sent packet.
		return "1004000.htm";
	}

	// ── onEvent ──────────────────────────────────────────────────────────────────
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!ENABLED)
		{
			return null;
		}

		// Main page — return filename so the engine loads the static HTML
		if (event.equals("main"))
		{
			return "1004000.htm";
		}

		// Category pages
		if (event.startsWith("cat_"))
		{
			final String typeKey = event.substring(4);
			sendCategoryHtml(npc, player, typeKey);
			return null;
		}

		// Apply augmentation
		if (event.startsWith("apply_"))
		{
			try
			{
				final int optionId = Integer.parseInt(event.substring(6));
				applyAugment(npc, player, optionId);
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("AugmentMaster: Invalid option ID in event: " + event);
			}
			return null;
		}

		// Remove augmentation
		if (event.equals("remove"))
		{
			sendRemoveConfirmHtml(npc, player);
			return null;
		}

		if (event.equals("remove_confirm"))
		{
			removeAugment(npc, player);
			return null;
		}

		return null;
	}

	// ── Send Category HTML ───────────────────────────────────────────────────────
	private void sendCategoryHtml(Npc npc, Player player, String typeKey)
	{
		AugmentType targetType;
		try
		{
			targetType = AugmentType.valueOf(typeKey);
		}
		catch (IllegalArgumentException e)
		{
			sendHtml(npc, player, "1004000.htm");
			return;
		}

		// Collect augments that belong to this category AND are in the configured list
		final List<AugmentInfo> list = new ArrayList<>();
		for (final int optionId : AVAILABLE_OPTIONS)
		{
			final AugmentInfo info = AUGMENT_CATALOG.get(optionId);
			if (info != null && info.type == targetType)
			{
				list.add(info);
			}
		}

		final String itemName = getItemName(ITEM_ID);
		final String costText = (ITEM_COUNT > 0) ? formatNumber(ITEM_COUNT) + " " + itemName : "Free";

		final StringBuilder html = new StringBuilder();
		html.append("<html><body>");
		html.append("Augment Master:<br>");
		html.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
		html.append("<center><font color=\"").append(targetType.color).append("\">").append(targetType.displayName).append("</font></center>");
		html.append("<font color=\"A2A0A2\">Cost per augment: </font><font color=\"FF9900\">").append(costText).append("</font><br>");
		html.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");

		if (list.isEmpty())
		{
			html.append("<br><center><font color=\"A2A0A2\">No augments available in this category.</font></center><br>");
		}
		else
		{
			html.append("<table width=270 cellpadding=3 cellspacing=0 border=0>");

			for (final AugmentInfo info : list)
			{
				html.append("<tr>");
				html.append("<td width=175>");
				html.append("<font color=\"").append(targetType.color).append("\">").append(info.name).append("</font><br1>");
				html.append("<font color=\"A2A0A2\">").append(info.description).append("</font>");
				html.append("</td>");
				html.append("<td width=90 align=center>");
				html.append("<button value=\"Apply\" action=\"bypass -h Script AugmentMaster apply_").append(info.optionId).append("\" width=85 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
				html.append("</td>");
				html.append("</tr>");

				// Separator between rows
				html.append("<tr><td colspan=2 height=4></td></tr>");
			}

			html.append("</table>");
		}

		html.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
		html.append("<center><a action=\"bypass -h Script AugmentMaster main\">Back to Menu</a></center>");
		html.append("</body></html>");

		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setHtml(html.toString());
		player.sendPacket(msg);
	}

	// ── Send Remove Confirm HTML ─────────────────────────────────────────────────
	private void sendRemoveConfirmHtml(Npc npc, Player player)
	{
		final Item weapon = player.getActiveWeaponInstance();

		if (weapon == null)
		{
			sendHtml(npc, player, "1004000-noweapon.htm");
			return;
		}

		if (!weapon.isAugmented())
		{
			sendHtml(npc, player, "1004000-noaugment.htm");
			return;
		}

		final StringBuilder html = new StringBuilder();
		html.append("<html><body>");
		html.append("Augment Master:<br>");
		html.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");

		if (REMOVE_ITEM_COUNT > 0)
		{
			final String removeItemName = getItemName(REMOVE_ITEM_ID);
			html.append("Removing an augment from your weapon requires:<br>");
			html.append("<font color=\"FF9900\">").append(formatNumber(REMOVE_ITEM_COUNT)).append(" ").append(removeItemName).append("</font><br>");
		}
		else
		{
			html.append("I can remove the augment from your<br1>");
			html.append("weapon at <font color=\"00CC00\">no cost</font>.<br>");
		}

		html.append("<br>");
		html.append("Your weapon: <font color=\"LEVEL\">").append(weapon.getName()).append("</font><br>");
		html.append("Do you want to proceed?<br><br>");
		html.append("<table width=270 cellpadding=2><tr>");
		html.append("<td><button value=\"Confirm Remove\" action=\"bypass -h Script AugmentMaster remove_confirm\" width=130 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		html.append("<td><button value=\"Cancel\" action=\"bypass -h Script AugmentMaster main\" width=130 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		html.append("</tr></table>");
		html.append("</body></html>");

		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setHtml(html.toString());
		player.sendPacket(msg);
	}

	// ── Apply Augment Logic ──────────────────────────────────────────────────────
	private void applyAugment(Npc npc, Player player, int optionId)
	{
		// Validate option exists in catalog
		final AugmentInfo info = AUGMENT_CATALOG.get(optionId);
		if (info == null)
		{
			player.sendMessage("Augment Master: Unknown augment option.");
			sendHtml(npc, player, "1004000.htm");
			return;
		}

		// Check weapon is equipped
		final Item weapon = player.getActiveWeaponInstance();
		if (weapon == null)
		{
			sendHtml(npc, player, "1004000-noweapon.htm");
			return;
		}

		// Check that this option was configured as available
		boolean optionAllowed = false;
		for (final int allowed : AVAILABLE_OPTIONS)
		{
			if (allowed == optionId)
			{
				optionAllowed = true;
				break;
			}
		}

		if (!optionAllowed)
		{
			player.sendMessage("Augment Master: That augmentation is not available.");
			sendHtml(npc, player, "1004000.htm");
			return;
		}

		// Check item cost
		if (ITEM_COUNT > 0)
		{
			if (getQuestItemsCount(player, ITEM_ID) < ITEM_COUNT)
			{
				sendHtml(npc, player, "1004000-noitem.htm");
				return;
			}
			takeItems(player, ITEM_ID, ITEM_COUNT);
		}

		// Remove existing augment if any
		if (weapon.isAugmented())
		{
			weapon.removeAugmentation();
		}

		// Generate augmentationId like the blacksmith does:
		// stat12 (lower 16 bits) = stat bonus + determines glow/name (Sacredium, etc.)
		// stat34 (upper 16 bits) = the chosen skill option
		// Constants matching AugmentationData
		final int STAT_BLOCKSIZE = 3640;
		final int STAT_SUBBLOCKSIZE = 91;
		final int lifeStoneLevel = 9; // max level = best stats
		// Top-grade glow: glowModifier = ((lifeStoneGrade + resultColor) / 2) * (10 * STAT_SUBBLOCKSIZE)
		// lifeStoneGrade=3 (TOP), resultColor=3 (RED) -> ((3+3)/2) * 910 = 2730
		final int glowModifier = 3 * (10 * STAT_SUBBLOCKSIZE);
		final int blockOffset = Rnd.get(0, 1) * STAT_BLOCKSIZE;
		final int levelOffset = lifeStoneLevel * STAT_SUBBLOCKSIZE;
		final int offset = levelOffset + blockOffset + glowModifier + 1;
		final int stat12 = Rnd.get(offset, (offset + STAT_SUBBLOCKSIZE) - 1);
		final int stat34 = optionId;
		final int augmentationId = (stat34 << 16) + stat12;
		final Augmentation newAugmentation = new Augmentation(augmentationId);
		weapon.setAugmentation(newAugmentation);
		player.sendItemList(false);

		// Success HTML
		final StringBuilder html = new StringBuilder();
		html.append("<html><body>");
		html.append("Augment Master:<br>");
		html.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
		html.append("<font color=\"00CC00\">Augmentation applied successfully!</font><br><br>");
		html.append("Weapon: <font color=\"LEVEL\">").append(weapon.getName()).append("</font><br>");
		html.append("Augment: <font color=\"").append(info.type.color).append("\">").append(info.name).append("</font><br1>");
		html.append("<font color=\"A2A0A2\">").append(info.description).append("</font><br>");
		html.append("<br>");
		html.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
		html.append("<center><a action=\"bypass -h Script AugmentMaster main\">Back to Menu</a></center>");
		html.append("</body></html>");

		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setHtml(html.toString());
		player.sendPacket(msg);
	}

	// ── Remove Augment Logic ─────────────────────────────────────────────────────
	private void removeAugment(Npc npc, Player player)
	{
		final Item weapon = player.getActiveWeaponInstance();
		if (weapon == null)
		{
			sendHtml(npc, player, "1004000-noweapon.htm");
			return;
		}

		if (!weapon.isAugmented())
		{
			sendHtml(npc, player, "1004000-noaugment.htm");
			return;
		}

		// Check removal cost
		if (REMOVE_ITEM_COUNT > 0)
		{
			if (getQuestItemsCount(player, REMOVE_ITEM_ID) < REMOVE_ITEM_COUNT)
			{
				sendHtml(npc, player, "1004000-noitem.htm");
				return;
			}
			takeItems(player, REMOVE_ITEM_ID, REMOVE_ITEM_COUNT);
		}

		weapon.removeAugmentation();
		player.sendItemList(false);

		sendHtml(npc, player, "1004000-removed.htm");
	}

	// ── Utility: Send Static HTML File ───────────────────────────────────────────
	private void sendHtml(Npc npc, Player player, String filename)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setHtml(getHtm(player, filename));
		player.sendPacket(msg);
	}

	// ── Utility: Get Item Name ────────────────────────────────────────────────────
	private static String getItemName(int itemId)
	{
		try
		{
			final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
			return (template != null) ? template.getName() : ("Item #" + itemId);
		}
		catch (Exception e)
		{
			return "Item #" + itemId;
		}
	}

	// ── Utility: Format Large Numbers ────────────────────────────────────────────
	private static String formatNumber(long value)
	{
		return NumberFormat.getNumberInstance(Locale.US).format(value);
	}

	// ── Entry Point ──────────────────────────────────────────────────────────────
	public static void main(String[] args)
	{
		new AugmentMaster();
	}
}
