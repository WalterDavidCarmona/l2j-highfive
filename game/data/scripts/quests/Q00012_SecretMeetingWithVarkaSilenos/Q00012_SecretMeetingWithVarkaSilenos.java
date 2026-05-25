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
package quests.Q00012_SecretMeetingWithVarkaSilenos;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * Secret Meeting With Varka Silenos (12)<br>
 * Original Jython script by Emperorc.
 * @author nonom
 */
public class Q00012_SecretMeetingWithVarkaSilenos extends Quest
{
	// NPCs
	private static final int CADMON = 31296;
	private static final int HELMUT = 31258;
	private static final int NARAN = 31378;
	
	// Item
	private static final int BOX = 7232;
	
	public Q00012_SecretMeetingWithVarkaSilenos()
	{
		super(12);
		addStartNpc(CADMON);
		addTalkId(CADMON, HELMUT, NARAN);
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
			case "31296-03.html":
			{
				qs.startQuest();
				break;
			}
			case "31258-02.html":
			{
				if (qs.isCond(1))
				{
					qs.setCond(2, true);
					giveItems(player, BOX, 1);
				}
				break;
			}
			case "31378-02.html":
			{
				if (qs.isCond(2) && hasQuestItems(player, BOX))
				{
					addExpAndSp(player, 233125, 18142);
					qs.exitQuest(false, true);
				}
				else
				{
					htmltext = "31378-03.html";
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
				if (npcId == CADMON)
				{
					htmltext = (player.getLevel() >= 74) ? "31296-01.htm" : "31296-02.html";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = qs.getCond();
				if ((npcId == CADMON) && (cond == 1))
				{
					htmltext = "31296-04.html";
				}
				else if (npcId == HELMUT)
				{
					if (cond == 1)
					{
						htmltext = "31258-01.html";
					}
					else if (cond == 2)
					{
						htmltext = "31258-03.html";
					}
				}
				else if ((npcId == NARAN) && (cond == 2))
				{
					htmltext = "31378-01.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
}
