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
package quests.Q00603_DaimonTheWhiteEyedPart1;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * Daimon the White-Eyed - Part 1 (603)
 * @author Mobius
 */
public class Q00603_DaimonTheWhiteEyedPart1 extends Quest
{
	// NPC
	private static final int EYE_OF_ARGOS = 31683;
	private static final int TABLET_1 = 31548;
	private static final int TABLET_2 = 31549;
	private static final int TABLET_3 = 31550;
	private static final int TABLET_4 = 31551;
	private static final int TABLET_5 = 31552;
	
	// Items
	private static final int SPIRIT_OF_DARKNESS = 7190;
	private static final int BROKEN_CRYSTAL = 7191;
	
	// Monsters
	private static final Map<Integer, Double> MONSTER_CHANCES = new HashMap<>();
	static
	{
		MONSTER_CHANCES.put(21297, 0.5); // Canyon Bandersnatch Slave
		MONSTER_CHANCES.put(21299, 0.519); // Buffalo Slave
		MONSTER_CHANCES.put(21304, 0.673); // Grendel Slave
	}
	
	// Reward
	private static final int UNFINISHED_CRYSTAL = 7192;
	
	// Misc
	private static final int MIN_LEVEL = 73;
	
	public Q00603_DaimonTheWhiteEyedPart1()
	{
		super(603);
		addStartNpc(EYE_OF_ARGOS);
		addTalkId(EYE_OF_ARGOS, TABLET_1, TABLET_2, TABLET_3, TABLET_4, TABLET_5);
		addKillId(MONSTER_CHANCES.keySet());
		registerQuestItems(SPIRIT_OF_DARKNESS, BROKEN_CRYSTAL);
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
			case "31683-03.htm":
			{
				if (qs.isCreated())
				{
					qs.set("tablet_" + TABLET_1, 0);
					qs.set("tablet_" + TABLET_2, 0);
					qs.set("tablet_" + TABLET_3, 0);
					qs.set("tablet_" + TABLET_4, 0);
					qs.set("tablet_" + TABLET_5, 0);
					qs.startQuest();
					htmltext = event;
				}
				break;
			}
			case "31548-02.html":
			case "31549-02.html":
			case "31550-02.html":
			case "31551-02.html":
			case "31552-02.html":
			{
				if (qs.getCond() < 6)
				{
					giveItems(player, BROKEN_CRYSTAL, 1);
					qs.set("TABLET_" + npc.getId(), 1);
					qs.setCond(qs.getCond() + 1, true);
					htmltext = event;
				}
				break;
			}
			case "31683-06.html":
			{
				if (qs.isCond(6) && (getQuestItemsCount(player, BROKEN_CRYSTAL) >= 5))
				{
					takeItems(player, BROKEN_CRYSTAL, -1);
					qs.setCond(7, true);
					htmltext = event;
				}
				break;
			}
			case "31683-10.html":
			{
				if (qs.isCond(8))
				{
					if (getQuestItemsCount(player, SPIRIT_OF_DARKNESS) >= 200)
					{
						takeItems(player, SPIRIT_OF_DARKNESS, -1);
						giveItems(player, UNFINISHED_CRYSTAL, 1);
						qs.exitQuest(true, true);
						htmltext = event;
					}
					else
					{
						htmltext = "31683-11.html";
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if (npc.getId() == EYE_OF_ARGOS)
				{
					htmltext = ((talker.getLevel() < MIN_LEVEL) ? "31683-02.html" : "31683-01.htm");
				}
				break;
			}
			case State.STARTED:
			{
				if (npc.getId() == EYE_OF_ARGOS)
				{
					switch (qs.getCond())
					{
						case 1:
						case 2:
						case 3:
						case 4:
						case 5:
						{
							htmltext = "31683-04.html";
							break;
						}
						case 6:
						{
							htmltext = "31683-05.html";
							break;
						}
						case 7:
						{
							htmltext = "31683-07.html";
							break;
						}
						case 8:
						{
							htmltext = "31683-08.html";
							break;
						}
					}
				}
				else if (qs.getInt("TABLET_" + npc.getId()) == 0)
				{
					htmltext = npc.getId() + "-01.html";
				}
				else
				{
					htmltext = npc.getId() + "-03.html";
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(talker);
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, 7, 3, npc);
		if (qs != null)
		{
			if (giveItemRandomly(qs.getPlayer(), npc, SPIRIT_OF_DARKNESS, 1, 200, MONSTER_CHANCES.get(npc.getId()), true))
			{
				qs.setCond(8, true);
			}
		}
	}
}
