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
package ai.areas.ForgeOfTheGods;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * Tar Beetle AI
 * @author nonom, malyelfik
 */
public class TarBeetle extends Script
{
	// NPC
	private static final int TAR_BEETLE = 18804;
	
	// Skills
	private static final int TAR_SPITE = 6142;
	private static final SkillHolder[] SKILLS =
	{
		new SkillHolder(TAR_SPITE, 1),
		new SkillHolder(TAR_SPITE, 2),
		new SkillHolder(TAR_SPITE, 3)
	};
	
	private static final TarBeetleSpawn spawn = new TarBeetleSpawn();
	
	private TarBeetle()
	{
		addAggroRangeEnterId(TAR_BEETLE);
		addSpellFinishedId(TAR_BEETLE);
	}
	
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (npc.getScriptValue() > 0)
		{
			final BuffInfo info = player.getEffectList().getBuffInfoBySkillId(TAR_SPITE);
			final int level = (info != null) ? info.getSkill().getAbnormalLevel() : 0;
			if (level < 3)
			{
				final Skill skill = SKILLS[level].getSkill();
				if (!npc.isSkillDisabled(skill))
				{
					npc.setTarget(player);
					npc.doCast(skill);
				}
			}
		}
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if ((skill != null) && (skill.getId() == TAR_SPITE))
		{
			final int val = npc.getScriptValue() - 1;
			if ((val <= 0) || (SKILLS[0].getSkill().getMpConsume() > npc.getCurrentMp()))
			{
				spawn.removeBeetle(npc);
			}
			else
			{
				npc.setScriptValue(val);
			}
		}
	}
	
	@Override
	public void unload()
	{
		spawn.unload();
		super.unload();
	}
	
	public static void main(String[] args)
	{
		new TarBeetle();
	}
}
