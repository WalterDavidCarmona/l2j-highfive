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
package quests.Q10268_ToTheSeedOfInfinity;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * To the Seed of Infinity (10268)<br>
 * Original Jython script by Kerberos.
 * @author nonom
 */
public class Q10268_ToTheSeedOfInfinity extends Quest
{
	// NPCs
	private static final int KEUCEREUS = 32548;
	private static final int TEPIOS = 32603;
	
	// Item
	private static final int INTRODUCTION = 13811;
	
	public Q10268_ToTheSeedOfInfinity()
	{
		super(10268);
		addStartNpc(KEUCEREUS);
		addTalkId(KEUCEREUS, TEPIOS);
		registerQuestItems(INTRODUCTION);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return getNoQuestMsg(player);
		}
		
		if (event.equals("32548-05.html"))
		{
			qs.startQuest();
			giveItems(player, INTRODUCTION, 1);
		}
		
		return event;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case KEUCEREUS:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() < 75) ? "32548-00.html" : "32548-01.htm";
						break;
					}
					case State.STARTED:
					{
						htmltext = "32548-06.html";
						break;
					}
					case State.COMPLETED:
					{
						htmltext = "32548-0a.html";
						break;
					}
				}
				break;
			}
			case TEPIOS:
			{
				switch (qs.getState())
				{
					case State.STARTED:
					{
						htmltext = "32530-01.html";
						giveAdena(player, 16671, true);
						addExpAndSp(player, 100640, 10098);
						qs.exitQuest(false, true);
						break;
					}
					case State.COMPLETED:
					{
						htmltext = "32530-02.html";
						break;
					}
					default:
					{
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
}
