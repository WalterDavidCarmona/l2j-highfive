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
package quests.Q10282_ToTheSeedOfAnnihilation;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * To the Seed of Destruction (10269)<br>
 * Original Jython script by Gnacik 2010-08-13 Based on Freya PTS.
 * @author nonom
 */
public class Q10282_ToTheSeedOfAnnihilation extends Quest
{
	// NPCs
	private static final int KBALDIR = 32733;
	private static final int KLEMIS = 32734;
	
	// Item
	private static final int SOA_ORDERS = 15512;
	
	public Q10282_ToTheSeedOfAnnihilation()
	{
		super(10282);
		addStartNpc(KBALDIR);
		addTalkId(KBALDIR, KLEMIS);
		registerQuestItems(SOA_ORDERS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "32733-07.htm":
			{
				qs.startQuest();
				giveItems(player, SOA_ORDERS, 1);
				break;
			}
			case "32734-02.htm":
			{
				giveAdena(player, 212182, true);
				addExpAndSp(player, 1148480, 99110);
				qs.exitQuest(false);
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
				if (npcId == KBALDIR)
				{
					htmltext = "32733-09.htm";
				}
				else if (npcId == KLEMIS)
				{
					htmltext = "32734-03.htm";
				}
				break;
			}
			case State.CREATED:
			{
				htmltext = (player.getLevel() < 84) ? "32733-00.htm" : "32733-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(1))
				{
					if (npcId == KBALDIR)
					{
						htmltext = "32733-08.htm";
					}
					else if (npcId == KLEMIS)
					{
						htmltext = "32734-01.htm";
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
}
