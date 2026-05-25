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
package quests.Q00137_TempleChampionPart1;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;

/**
 * Temple Champion - 1 (137)
 * @author nonom
 */
public class Q00137_TempleChampionPart1 extends Quest
{
	// NPCs
	private static final int SYLVAIN = 30070;
	private static final int[] MOBS =
	{
		20083, // Granite Golem
		20144, // Hangman Tree
		20199, // Amber Basilisk
		20200, // Strain
		20201, // Ghoul
		20202, // Dead Seeker
	};
	
	// Items
	private static final int FRAGMENT = 10340;
	private static final int EXECUTOR = 10334;
	private static final int MISSIONARY = 10339;
	
	public Q00137_TempleChampionPart1()
	{
		super(137);
		addStartNpc(SYLVAIN);
		addTalkId(SYLVAIN);
		addKillId(MOBS);
		registerQuestItems(FRAGMENT);
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
			case "30070-02.htm":
			{
				qs.startQuest();
				break;
			}
			case "30070-05.html":
			{
				qs.set("talk", "1");
				break;
			}
			case "30070-06.html":
			{
				qs.set("talk", "2");
				break;
			}
			case "30070-08.html":
			{
				qs.unset("talk");
				qs.setCond(2, true);
				break;
			}
			case "30070-16.html":
			{
				if (qs.isCond(3) && hasQuestItems(player, EXECUTOR) && hasQuestItems(player, MISSIONARY))
				{
					takeItems(player, EXECUTOR, -1);
					takeItems(player, MISSIONARY, -1);
					giveAdena(player, 69146, true);
					if (player.getLevel() < 41)
					{
						addExpAndSp(player, 219975, 13047);
					}
					
					qs.exitQuest(false, true);
				}
				break;
			}
		}
		
		return event;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isStarted() && qs.isCond(2) && (getQuestItemsCount(player, FRAGMENT) < 30))
		{
			giveItems(player, FRAGMENT, 1);
			if (getQuestItemsCount(player, FRAGMENT) >= 30)
			{
				qs.setCond(3, true);
			}
			else
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs.isCompleted())
		{
			return getAlreadyCompletedMsg(player);
		}
		
		switch (qs.getCond())
		{
			case 1:
			{
				switch (qs.getInt("talk"))
				{
					case 1:
					{
						htmltext = "30070-05.html";
						break;
					}
					case 2:
					{
						htmltext = "30070-06.html";
						break;
					}
					default:
					{
						htmltext = "30070-03.html";
						break;
					}
				}
				break;
			}
			case 2:
			{
				htmltext = "30070-08.html";
				break;
			}
			case 3:
			{
				if (qs.getInt("talk") == 1)
				{
					htmltext = "30070-10.html";
				}
				else if (getQuestItemsCount(player, FRAGMENT) >= 30)
				{
					qs.set("talk", "1");
					htmltext = "30070-09.html";
					takeItems(player, FRAGMENT, -1);
				}
				break;
			}
			default:
			{
				htmltext = ((player.getLevel() >= 35) && hasQuestItems(player, EXECUTOR, MISSIONARY)) ? "30070-01.htm" : "30070-00.html";
				break;
			}
		}
		
		return htmltext;
	}
}
