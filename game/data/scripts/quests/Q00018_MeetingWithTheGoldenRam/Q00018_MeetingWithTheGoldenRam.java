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
package quests.Q00018_MeetingWithTheGoldenRam;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * Meeting With The Golden Ram (18)<br>
 * Original jython script by disKret.
 * @author nonom
 */
public class Q00018_MeetingWithTheGoldenRam extends Quest
{
	// NPCs
	private static final int DONAL = 31314;
	private static final int DAISY = 31315;
	private static final int ABERCROMBIE = 31555;
	
	// Item
	private static final int BOX = 7245;
	
	public Q00018_MeetingWithTheGoldenRam()
	{
		super(18);
		addStartNpc(DONAL);
		addTalkId(DONAL, DAISY, ABERCROMBIE);
		registerQuestItems(BOX);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "31314-03.html":
			{
				if (player.getLevel() >= 66)
				{
					qs.startQuest();
				}
				else
				{
					htmltext = "31314-02.html";
				}
				break;
			}
			case "31315-02.html":
			{
				qs.setCond(2, true);
				giveItems(player, BOX, 1);
				break;
			}
			case "31555-02.html":
			{
				if (hasQuestItems(player, BOX))
				{
					giveAdena(player, 40000, true);
					addExpAndSp(player, 126668, 11731);
					qs.exitQuest(false, true);
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		final int npcId = npc.getId();
		
		switch (qs.getState())
		{
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
			case State.CREATED:
			{
				if (npcId == DONAL)
				{
					htmltext = "31314-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (npcId == DONAL)
				{
					htmltext = "31314-04.html";
				}
				else if (npcId == DAISY)
				{
					htmltext = (qs.getCond() < 2) ? "31315-01.html" : "31315-03.html";
				}
				else if ((npcId == ABERCROMBIE) && qs.isCond(2) && hasQuestItems(player, BOX))
				{
					htmltext = "31555-01.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
}
