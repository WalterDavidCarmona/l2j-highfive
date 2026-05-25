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
package quests.Q00011_SecretMeetingWithKetraOrcs;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * Secret Meeting With Ketra Orcs (11)<br>
 * Original Jython script by Emperorc.
 * @author nonom
 */
public class Q00011_SecretMeetingWithKetraOrcs extends Quest
{
	// NPCs
	private static final int CADMON = 31296;
	private static final int LEON = 31256;
	private static final int WAHKAN = 31371;
	
	// Item
	private static final int BOX = 7231;
	
	public Q00011_SecretMeetingWithKetraOrcs()
	{
		super(11);
		addStartNpc(CADMON);
		addTalkId(CADMON, LEON, WAHKAN);
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
			case "31256-02.html":
			{
				if (qs.isCond(1))
				{
					qs.setCond(2, true);
					giveItems(player, BOX, 1);
				}
				break;
			}
			case "31371-02.html":
			{
				if (qs.isCond(2) && hasQuestItems(player, BOX))
				{
					addExpAndSp(player, 233125, 18142);
					qs.exitQuest(false, true);
				}
				else
				{
					htmltext = "31371-03.html";
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
				if ((npcId == CADMON) && qs.isCond(1))
				{
					htmltext = "31296-04.html";
				}
				else if (npcId == LEON)
				{
					if (qs.isCond(1))
					{
						htmltext = "31256-01.html";
					}
					else if (qs.isCond(2))
					{
						htmltext = "31256-03.html";
					}
				}
				else if ((npcId == WAHKAN) && qs.isCond(2))
				{
					htmltext = "31371-01.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
}
