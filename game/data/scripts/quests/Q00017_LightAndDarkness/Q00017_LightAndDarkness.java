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
package quests.Q00017_LightAndDarkness;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

import quests.Q00015_SweetWhispers.Q00015_SweetWhispers;

/**
 * Light And Darkness (17)<br>
 * Original jython script by disKret, Skeleton & DrLecter.
 * @author nonom
 */
public class Q00017_LightAndDarkness extends Quest
{
	// NPCs
	private static final int HIERARCH = 31517;
	private static final int SAINT_ALTAR_1 = 31508;
	private static final int SAINT_ALTAR_2 = 31509;
	private static final int SAINT_ALTAR_3 = 31510;
	private static final int SAINT_ALTAR_4 = 31511;
	
	// Item
	private static final int BLOOD_OF_SAINT = 7168;
	
	public Q00017_LightAndDarkness()
	{
		super(17);
		addStartNpc(HIERARCH);
		addTalkId(HIERARCH, SAINT_ALTAR_1, SAINT_ALTAR_2, SAINT_ALTAR_3, SAINT_ALTAR_4);
		registerQuestItems(BLOOD_OF_SAINT);
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
			case "31517-02.html":
			{
				if (player.getLevel() >= 61)
				{
					qs.startQuest();
					giveItems(player, BLOOD_OF_SAINT, 4);
				}
				else
				{
					htmltext = "31517-02a.html";
				}
				break;
			}
			case "31508-02.html":
			case "31509-02.html":
			case "31510-02.html":
			case "31511-02.html":
			{
				final int cond = qs.getCond();
				final int npcId = Integer.parseInt(event.replace("-02.html", ""));
				if ((cond == (npcId - 31507)) && hasQuestItems(player, BLOOD_OF_SAINT))
				{
					htmltext = npcId + "-01.html";
					takeItems(player, BLOOD_OF_SAINT, 1);
					qs.setCond(cond + 1, true);
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
		switch (qs.getState())
		{
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
			case State.CREATED:
			{
				final QuestState qs2 = player.getQuestState(Q00015_SweetWhispers.class.getSimpleName());
				htmltext = ((qs2 != null) && qs2.isCompleted()) ? "31517-00.htm" : "31517-06.html";
				break;
			}
			case State.STARTED:
			{
				final long blood = getQuestItemsCount(player, BLOOD_OF_SAINT);
				final int npcId = npc.getId();
				switch (npcId)
				{
					case HIERARCH:
					{
						if (qs.getCond() < 5)
						{
							htmltext = (blood >= 5) ? "31517-05.html" : "31517-04.html";
						}
						else
						{
							addExpAndSp(player, 697040, 54887);
							qs.exitQuest(false, true);
							htmltext = "31517-03.html";
						}
						break;
					}
					case SAINT_ALTAR_1:
					case SAINT_ALTAR_2:
					case SAINT_ALTAR_3:
					case SAINT_ALTAR_4:
					{
						if ((npcId - 31507) == qs.getCond())
						{
							htmltext = npcId + ((blood > 0) ? "-00.html" : "-02.html");
						}
						else if (qs.getCond() > (npcId - 31507))
						{
							htmltext = npcId + "-03.html";
						}
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
}
