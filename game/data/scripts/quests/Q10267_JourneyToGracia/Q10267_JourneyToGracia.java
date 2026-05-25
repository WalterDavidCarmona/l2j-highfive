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
package quests.Q10267_JourneyToGracia;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * Journey To Gracia (10267)<br>
 * Original Jython script by Kerberos.
 * @author nonom
 */
public class Q10267_JourneyToGracia extends Quest
{
	// NPCs
	private static final int ORVEN = 30857;
	private static final int KEUCEREUS = 32548;
	private static final int PAPIKU = 32564;
	
	// Item
	private static final int LETTER = 13810;
	
	public Q10267_JourneyToGracia()
	{
		super(10267);
		addStartNpc(ORVEN);
		addTalkId(ORVEN, KEUCEREUS, PAPIKU);
		registerQuestItems(LETTER);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return getNoQuestMsg(player);
		}
		
		switch (event)
		{
			case "30857-06.html":
			{
				qs.startQuest();
				giveItems(player, LETTER, 1);
				break;
			}
			case "32564-02.html":
			{
				qs.setCond(2, true);
				break;
			}
			case "32548-02.html":
			{
				giveAdena(player, 92500, true);
				addExpAndSp(player, 75480, 7570);
				qs.exitQuest(false, true);
				break;
			}
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
			case ORVEN:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() < 75) ? "30857-00.html" : "30857-01.htm";
						break;
					}
					case State.STARTED:
					{
						htmltext = "30857-07.html";
						break;
					}
					case State.COMPLETED:
					{
						htmltext = "30857-0a.html";
						break;
					}
				}
				break;
			}
			case PAPIKU:
			{
				if (qs.isStarted())
				{
					htmltext = qs.isCond(1) ? "32564-01.html" : "32564-03.html";
				}
				break;
			}
			case KEUCEREUS:
			{
				if (qs.isStarted() && qs.isCond(2))
				{
					htmltext = "32548-01.html";
				}
				else if (qs.isCompleted())
				{
					htmltext = "32548-03.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
}
