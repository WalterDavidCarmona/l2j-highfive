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
package ai.others;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Blackdagger Wing AI.
 * @author Zoey76
 * @since 2.6.0.0
 */
public class BlackdaggerWing extends Script
{
	// NPCs
	private static final int BLACKDAGGER_WING = 25721;
	
	// Skills
	private static final SkillHolder POWER_STRIKE = new SkillHolder(6833, 1);
	private static final SkillHolder RANGE_MAGIC_ATTACK = new SkillHolder(6834, 1);
	
	// Variables
	private static final String MID_HP_FLAG = "MID_HP_FLAG";
	private static final String POWER_STRIKE_CAST_COUNT = "POWER_STRIKE_CAST_COUNT";
	
	// Timers
	private static final String DAMAGE_TIMER = "DAMAGE_TIMER";
	
	// Misc
	private static final int MAX_CHASE_DIST = 2500;
	private static final double MID_HP_PERCENTAGE = 0.50;
	
	public BlackdaggerWing()
	{
		addAttackId(BLACKDAGGER_WING);
		addSpellFinishedId(BLACKDAGGER_WING);
		addCreatureSeeId(BLACKDAGGER_WING);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (LocationUtil.calculateDistance(npc, npc.getSpawn(), false, false) > MAX_CHASE_DIST)
		{
			npc.teleToLocation(npc.getSpawn().getX(), npc.getSpawn().getY(), npc.getSpawn().getZ());
		}
		
		if ((npc.getCurrentHp() < (npc.getMaxHp() * MID_HP_PERCENTAGE)) && !npc.getVariables().getBoolean(MID_HP_FLAG, false))
		{
			npc.getVariables().set(MID_HP_FLAG, true);
			startQuestTimer(DAMAGE_TIMER, 10000, npc, attacker);
		}
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (npc.getVariables().getBoolean(MID_HP_FLAG, false))
		{
			final Creature mostHated = npc.asAttackable().getMostHated();
			if ((mostHated != null) && mostHated.isPlayer() && (mostHated != creature) && (getRandom(5) < 1))
			{
				addSkillCastDesire(npc, creature, RANGE_MAGIC_ATTACK, 99999);
			}
		}
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if (skill.getId() == POWER_STRIKE.getSkillId())
		{
			npc.getVariables().set(POWER_STRIKE_CAST_COUNT, npc.getVariables().getInt(POWER_STRIKE_CAST_COUNT) + 1);
			if (npc.getVariables().getInt(POWER_STRIKE_CAST_COUNT) > 3)
			{
				addSkillCastDesire(npc, player, RANGE_MAGIC_ATTACK, 99999);
				npc.getVariables().set(POWER_STRIKE_CAST_COUNT, 0);
			}
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (DAMAGE_TIMER.equals(event))
		{
			npc.getAI().setIntention(Intention.ATTACK);
			startQuestTimer(DAMAGE_TIMER, 30000, npc, player);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	public static void main(String[] args)
	{
		new BlackdaggerWing();
	}
}
