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
package quests.Q00377_ExplorationOfTheGiantsCavePart2;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;

/**
 * Exploration of the Giants' Cave Part 2 (377)<br>
 * Original Jython script by Gnacik.
 * @author nonom
 */
public class Q00377_ExplorationOfTheGiantsCavePart2 extends Quest
{
	// NPC
	private static final int SOBLING = 31147;
	
	// Items
	private static final int TITAN_ANCIENT_BOOK = 14847;
	private static final int BOOK1 = 14842;
	private static final int BOOK2 = 14843;
	private static final int BOOK3 = 14844;
	private static final int BOOK4 = 14845;
	private static final int BOOK5 = 14846;
	
	// Mobs
	private static final Map<Integer, Integer> MOBS1 = new HashMap<>();
	private static final Map<Integer, Double> MOBS2 = new HashMap<>();
	static
	{
		MOBS1.put(22660, 366); // lesser_giant_re
		MOBS1.put(22661, 424); // lesser_giant_soldier_re
		MOBS1.put(22662, 304); // lesser_giant_shooter_re
		MOBS1.put(22663, 304); // lesser_giant_scout_re
		MOBS1.put(22664, 354); // lesser_giant_mage_re
		MOBS1.put(22665, 324); // lesser_giant_elder_re
		MOBS2.put(22666, 0.276); // barif_re
		MOBS2.put(22667, 0.284); // barif_pet_re
		MOBS2.put(22668, 0.240); // gamlin_re
		MOBS2.put(22669, 0.240); // leogul_re
	}
	
	public Q00377_ExplorationOfTheGiantsCavePart2()
	{
		super(377);
		addStartNpc(SOBLING);
		addTalkId(SOBLING);
		addKillId(MOBS1.keySet());
		addKillId(MOBS2.keySet());
		registerQuestItems(TITAN_ANCIENT_BOOK);
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
			final int npcId = npc.getId();
			if (MOBS1.containsKey(npcId))
			{
				giveItemRandomly(qs.getPlayer(), npc, TITAN_ANCIENT_BOOK, (getRandom(1000) < MOBS1.get(npcId)) ? 3 : 2, 0, 1, true);
			}
			else
			{
				giveItemRandomly(qs.getPlayer(), npc, TITAN_ANCIENT_BOOK, 1, 0, MOBS2.get(npcId), true);
			}
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
