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
package ai.areas.Gracia.AI.NPC.AbyssGaze;

import org.l2jmobius.gameserver.managers.SeedOfInfinityManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.Script;

public class AbyssGaze extends Script
{
	// NPC
	private static final int ABYSS_GATEKEEPER = 32539;
	
	public AbyssGaze()
	{
		addStartNpc(ABYSS_GATEKEEPER);
		addFirstTalkId(ABYSS_GATEKEEPER);
		addTalkId(ABYSS_GATEKEEPER);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState qs = player.getQuestState(getName());
		if (qs == null)
		{
			qs = newQuestState(player);
		}
		
		if (event.equals("request_permission"))
		{
			if ((SeedOfInfinityManager.getCurrentStage() == 2) || (SeedOfInfinityManager.getCurrentStage() == 5))
			{
				htmltext = "32539-2.htm";
			}
			else if ((SeedOfInfinityManager.getCurrentStage() == 3) && SeedOfInfinityManager.isSeedOpen())
			{
				htmltext = "32539-3.htm";
			}
			else
			{
				htmltext = "32539-1.htm";
			}
		}
		else if (event.equals("enter_seed") && (SeedOfInfinityManager.getCurrentStage() == 3))
		{
			return null;
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		QuestState qs = player.getQuestState(getName());
		if (qs == null)
		{
			qs = newQuestState(player);
		}
		
		return "32539.htm";
	}
}
