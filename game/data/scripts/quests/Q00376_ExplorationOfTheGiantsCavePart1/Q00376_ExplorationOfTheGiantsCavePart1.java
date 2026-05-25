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
package quests.Q00376_ExplorationOfTheGiantsCavePart1;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;

/**
 * Exploration of the Giants' Cave Part 1 (376)<br>
 * Original Jython script by Gnacik.
 * @author nonom
 */
public class Q00376_ExplorationOfTheGiantsCavePart1 extends Quest
{
	// NPC
	private static final int SOBLING = 31147;
	
	// Items
	private static final int ANCIENT_PARCHMENT = 14841;
	private static final int BOOK1 = 14836;
	private static final int BOOK2 = 14837;
	private static final int BOOK3 = 14838;
	private static final int BOOK4 = 14839;
	private static final int BOOK5 = 14840;
	
	// Mobs
	private static final Map<Integer, Double> MOBS = new HashMap<>();
	static
	{
		MOBS.put(22670, 0.314); // const_lord
		MOBS.put(22671, 0.302); // const_gaurdian
		MOBS.put(22672, 0.300); // const_seer
		MOBS.put(22673, 0.258); // hirokai
		MOBS.put(22674, 0.248); // imagro
		MOBS.put(22675, 0.264); // palite
		MOBS.put(22676, 0.258); // hamrit
		MOBS.put(22677, 0.266); // kranout
	}
	
	public Q00376_ExplorationOfTheGiantsCavePart1()
	{
		super(376);
		addStartNpc(SOBLING);
		addTalkId(SOBLING);
		addKillId(MOBS.keySet());
		registerQuestItems(ANCIENT_PARCHMENT);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "31147-02.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "31147-04.html":
			case "31147-cont.html":
			{
				htmltext = event;
				break;
			}
			case "31147-quit.html":
			{
				qs.exitQuest(true, true);
				htmltext = event;
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(player, -1, 3, npc);
		if (qs != null)
		{
			giveItemRandomly(qs.getPlayer(), npc, ANCIENT_PARCHMENT, 1, 0, MOBS.get(npc.getId()), true);
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs.isCreated())
		{
			htmltext = (player.getLevel() >= 79) ? "31147-01.htm" : "31147-00.html";
		}
		else if (qs.isStarted())
		{
			htmltext = hasQuestItems(player, BOOK1, BOOK2, BOOK3, BOOK4, BOOK5) ? "31147-03.html" : "31147-02a.html";
		}
		
		return htmltext;
	}
}
