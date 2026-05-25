/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ai.areas.DragonValley;

import java.util.EnumMap;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * Dragon Valley AI.
 * @author St3eT
 */
public class DragonValley extends Script
{
	// NPC
	private static final int NECROMANCER_OF_THE_VALLEY = 22858;
	private static final int EXPLODING_ORC_GHOST = 22818;
	private static final int WRATHFUL_ORC_GHOST = 22819;
	private static final int DRAKOS_ASSASSIN = 22823;
	private static final int[] SUMMON_NPC =
	{
		22822, // Drakos Warrior
		22824, // Drakos Guardian
		22862, // Drakos Hunter
	};
	private static final int[] SPAWN_ANIMATION =
	{
		22826, // Scorpion Bones
		22823, // Drakos Assassin
		22828, // Parasitic Leech
	};
	private static final int[] SPOIL_REACT_MONSTER =
	{
		22822, // Drakos Warrior
		22823, // Drakos Assassin
		22824, // Drakos Guardian
		22825, // Giant Scorpion Bones
		22826, // Scorpion Bones
		22827, // Batwing Drake
		22828, // Parasitic Leech
		22829, // Emerald Drake
		22830, // Gem Dragon
		22831, // Dragon Tracker of the Valley
		22832, // Dragon Scout of the Valley
		22833, // Sand Drake Tracker
		22834, // Dust Dragon Tracker
		22860, // Hungry Parasitic Leech
		22861, // Hard Scorpion Bones
		22862, // Drakos Hunter
	};
	
	// Items
	private static final int GREATER_HERB_OF_MANA = 8604;
	private static final int SUPERIOR_HERB_OF_MANA = 8605;
	
	// Skills
	private static final SkillHolder SELF_DESTRUCTION = new SkillHolder(6850, 1);
	private static final SkillHolder MORALE_BOOST1 = new SkillHolder(6885, 1);
	private static final SkillHolder MORALE_BOOST2 = new SkillHolder(6885, 2);
	private static final SkillHolder MORALE_BOOST3 = new SkillHolder(6885, 3);
	
	// Misc
	private static final int MIN_DISTANCE = 1500;
	private static final int MIN_MEMBERS = 3;
	private static final int MIN_LEVEL = 80;
	private static final int CLASS_LEVEL = 3;
	private static final EnumMap<PlayerClass, Double> CLASS_POINTS = new EnumMap<>(PlayerClass.class);
	static
	{
		CLASS_POINTS.put(PlayerClass.ADVENTURER, 0.2);
		CLASS_POINTS.put(PlayerClass.ARCANA_LORD, 1.5);
		CLASS_POINTS.put(PlayerClass.ARCHMAGE, 0.3);
		CLASS_POINTS.put(PlayerClass.CARDINAL, -0.6);
		CLASS_POINTS.put(PlayerClass.DOMINATOR, 0.2);
		CLASS_POINTS.put(PlayerClass.DOOMBRINGER, 0.2);
		CLASS_POINTS.put(PlayerClass.DOOMCRYER, 0.1);
		CLASS_POINTS.put(PlayerClass.DREADNOUGHT, 0.7);
		CLASS_POINTS.put(PlayerClass.DUELIST, 0.2);
		CLASS_POINTS.put(PlayerClass.ELEMENTAL_MASTER, 1.4);
		CLASS_POINTS.put(PlayerClass.EVA_SAINT, -0.6);
		CLASS_POINTS.put(PlayerClass.EVA_TEMPLAR, 0.8);
		CLASS_POINTS.put(PlayerClass.FEMALE_SOUL_HOUND, 0.4);
		CLASS_POINTS.put(PlayerClass.FORTUNE_SEEKER, 0.9);
		CLASS_POINTS.put(PlayerClass.GHOST_HUNTER, 0.2);
		CLASS_POINTS.put(PlayerClass.GHOST_SENTINEL, 0.2);
		CLASS_POINTS.put(PlayerClass.GRAND_KHAVATARI, 0.2);
		CLASS_POINTS.put(PlayerClass.HELL_KNIGHT, 0.6);
		CLASS_POINTS.put(PlayerClass.HIEROPHANT, 0.0);
		CLASS_POINTS.put(PlayerClass.JUDICATOR, 0.1);
		CLASS_POINTS.put(PlayerClass.MOONLIGHT_SENTINEL, 0.2);
		CLASS_POINTS.put(PlayerClass.MAESTRO, 0.7);
		CLASS_POINTS.put(PlayerClass.MALE_SOUL_HOUND, 0.4);
		CLASS_POINTS.put(PlayerClass.MYSTIC_MUSE, 0.3);
		CLASS_POINTS.put(PlayerClass.PHOENIX_KNIGHT, 0.6);
		CLASS_POINTS.put(PlayerClass.SAGITTARIUS, 0.2);
		CLASS_POINTS.put(PlayerClass.SHILLIEN_SAINT, -0.6);
		CLASS_POINTS.put(PlayerClass.SHILLIEN_TEMPLAR, 0.8);
		CLASS_POINTS.put(PlayerClass.SOULTAKER, 0.3);
		CLASS_POINTS.put(PlayerClass.SPECTRAL_DANCER, 0.4);
		CLASS_POINTS.put(PlayerClass.SPECTRAL_MASTER, 1.4);
		CLASS_POINTS.put(PlayerClass.STORM_SCREAMER, 0.3);
		CLASS_POINTS.put(PlayerClass.SWORD_MUSE, 0.4);
		CLASS_POINTS.put(PlayerClass.TITAN, 0.3);
		CLASS_POINTS.put(PlayerClass.TRICKSTER, 0.5);
		CLASS_POINTS.put(PlayerClass.WIND_RIDER, 0.2);
	}
	
	private DragonValley()
	{
		addAttackId(NECROMANCER_OF_THE_VALLEY);
		addAttackId(SUMMON_NPC);
		addKillId(NECROMANCER_OF_THE_VALLEY);
		addKillId(SPOIL_REACT_MONSTER);
		addSpawnId(EXPLODING_ORC_GHOST, NECROMANCER_OF_THE_VALLEY);
		addSpawnId(SPOIL_REACT_MONSTER);
		addSpellFinishedId(EXPLODING_ORC_GHOST);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("SELF_DESTRUCTION") && (npc != null) && !npc.isDead())
		{
			final Playable playable = npc.getVariables().getObject("playable", Playable.class);
			if ((playable != null) && (npc.calculateDistance3D(playable) < 250))
			{
				npc.disableCoreAI(true);
				npc.doCast(SELF_DESTRUCTION.getSkill());
			}
			else if (playable != null)
			{
				startQuestTimer("SELF_DESTRUCTION", 3000, npc, null);
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.getId() == NECROMANCER_OF_THE_VALLEY)
		{
			spawnGhost(npc, attacker, isSummon, 1);
		}
		else
		{
			if ((npc.getCurrentHp() < (npc.getMaxHp() / 2)) && (getRandom(100) < 5) && npc.isScriptValue(0))
			{
				npc.setScriptValue(1);
				final int rnd = getRandom(3, 5);
				for (int i = 0; i < rnd; i++)
				{
					final Playable playable = isSummon ? attacker.getSummon() : attacker;
					final Npc minion = addSpawn(DRAKOS_ASSASSIN, npc.getX(), npc.getY(), npc.getZ() + 20, npc.getHeading(), true, 0, true);
					addAttackDesire(minion, playable);
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == NECROMANCER_OF_THE_VALLEY)
		{
			spawnGhost(npc, killer, isSummon, 20);
		}
		else if (npc.asAttackable().isSpoiled())
		{
			npc.dropItem(killer, getRandom(GREATER_HERB_OF_MANA, SUPERIOR_HERB_OF_MANA), 1);
			manageMoraleBoost(killer, npc);
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.asAttackable().setOnKillDelay(0);
		if (npc.getId() == EXPLODING_ORC_GHOST)
		{
			startQuestTimer("SELF_DESTRUCTION", 3000, npc, null);
		}
		else if (ArrayUtil.contains(SPAWN_ANIMATION, npc.getId()))
		{
			npc.setShowSummonAnimation(true);
		}
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if (skill == SELF_DESTRUCTION.getSkill())
		{
			npc.doDie(player);
		}
	}
	
	private void manageMoraleBoost(Player player, Npc npc)
	{
		double points = 0;
		int moraleBoostLv = 0;
		final Party party = player.getParty();
		if ((party != null) && (party.getMemberCount() >= MIN_MEMBERS) && (npc != null))
		{
			for (Player member : party.getMembers())
			{
				if ((member.getLevel() >= MIN_LEVEL) && (member.getPlayerClass().level() >= CLASS_LEVEL) && (npc.calculateDistance3D(member) < MIN_DISTANCE))
				{
					points += CLASS_POINTS.get(member.getPlayerClass());
				}
			}
			
			if (points >= 3)
			{
				moraleBoostLv = 3;
			}
			else if (points >= 2)
			{
				moraleBoostLv = 2;
			}
			else if (points >= 1)
			{
				moraleBoostLv = 1;
			}
			
			for (Player member : party.getMembers())
			{
				if (npc.calculateDistance3D(member) < MIN_DISTANCE)
				{
					switch (moraleBoostLv)
					{
						case 1:
						{
							MORALE_BOOST1.getSkill().applyEffects(member, member);
							break;
						}
						case 2:
						{
							MORALE_BOOST2.getSkill().applyEffects(member, member);
							break;
						}
						case 3:
						{
							MORALE_BOOST3.getSkill().applyEffects(member, member);
							break;
						}
					}
				}
			}
		}
	}
	
	private void spawnGhost(Npc npc, Player player, boolean isSummon, int chance)
	{
		if ((npc.getScriptValue() < 2) && (getRandom(100) < chance))
		{
			int val = npc.getScriptValue();
			final Playable attacker = isSummon ? player.getSummon() : player;
			final Npc ghost1 = addSpawn(EXPLODING_ORC_GHOST, npc.getX(), npc.getY(), npc.getZ() + 20, npc.getHeading(), false, 0, true);
			ghost1.getVariables().set("playable", attacker);
			addAttackDesire(ghost1, attacker);
			val++;
			if ((val < 2) && (getRandomBoolean()))
			{
				final Npc ghost2 = addSpawn(WRATHFUL_ORC_GHOST, npc.getX(), npc.getY(), npc.getZ() + 20, npc.getHeading(), false, 0, false);
				addAttackDesire(ghost2, attacker);
				val++;
			}
			
			npc.setScriptValue(val);
		}
	}
	
	public static void main(String[] args)
	{
		new DragonValley();
	}
}
