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
package quests.Q00038_DragonFangs;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;

/**
 * Dragon Fangs (38)
 * @author Janiko
 */
public class Q00038_DragonFangs extends Quest
{
	// NPCs
	private static final int IRIS = 30034;
	private static final int MAGISTER_ROHMER = 30344;
	private static final int GUARD_LUIS = 30386;
	
	// Monsters
	private static final int LIZARDMAN_SENTINEL = 21100;
	private static final int LIZARDMAN_SHAMAN = 21101;
	private static final int LIZARDMAN_LEADER = 20356;
	private static final int LIZARDMAN_SUB_LEADER = 20357;
	
	// Items
	private static final ItemHolder FEATHER = new ItemHolder(7173, 100);
	private static final int TOTEM_TOOTH_1ST = 7174;
	private static final ItemHolder TOTEM_TOOTH_2ND = new ItemHolder(7175, 50);
	private static final int LETTER_1ST = 7176;
	private static final int LETTER_2ND = 7177;
	
	// Rewards
	private static final int BONE_HELMET = 45;
	private static final int LEATHER_GAUNTLET = 605;
	private static final int ASPIS = 627;
	private static final int BLUE_BUCKSKIN_BOOTS = 1123;
	
	// Misc
	private static final int MIN_LEVEL = 19;
	
	public Q00038_DragonFangs()
	{
		super(38);
		addStartNpc(GUARD_LUIS);
		addTalkId(GUARD_LUIS, IRIS, MAGISTER_ROHMER);
		addKillId(LIZARDMAN_SENTINEL, LIZARDMAN_SHAMAN, LIZARDMAN_LEADER, LIZARDMAN_SUB_LEADER);
		registerQuestItems(FEATHER.getId(), TOTEM_TOOTH_1ST, TOTEM_TOOTH_2ND.getId(), LETTER_1ST, LETTER_2ND);
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
			case "30386-03.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					htmltext = event;
				}
				break;
			}
			case "30386-06.html":
			{
				if (qs.isCond(2))
				{
					if (hasItem(player, FEATHER))
					{
						qs.setCond(3, true);
						takeItem(player, FEATHER);
						giveItems(player, TOTEM_TOOTH_1ST, 1);
						htmltext = event;
					}
					else
					{
						htmltext = "30386-07.html";
					}
				}
				break;
			}
			case "30034-02.html":
			{
				if (qs.isCond(3))
				{
					if (hasQuestItems(player, TOTEM_TOOTH_1ST))
					{
						qs.setCond(4, true);
						takeItems(player, TOTEM_TOOTH_1ST, 1);
						giveItems(player, LETTER_1ST, 1);
						htmltext = event;
					}
					else
					{
						htmltext = "30034-03.html";
					}
				}
				break;
			}
			case "30034-06.html":
			{
				if (qs.isCond(5))
				{
					if (hasQuestItems(player, LETTER_2ND))
					{
						qs.setCond(6, true);
						takeItems(player, LETTER_2ND, 1);
						htmltext = event;
					}
					else
					{
						htmltext = "30034-07.html";
					}
				}
				break;
			}
			case "30034-10.html":
			{
				if (qs.isCond(7))
				{
					if (hasItem(player, TOTEM_TOOTH_2ND))
					{
						addExpAndSp(player, 435117, 23977);
						final int chance = getRandom(1000);
						if (chance < 250)
						{
							rewardItems(player, BONE_HELMET, 1);
							giveAdena(player, 5200, true);
						}
						else if (chance < 500)
						{
							rewardItems(player, ASPIS, 1);
							giveAdena(player, 1500, true);
						}
						else if (chance < 750)
						{
							rewardItems(player, BLUE_BUCKSKIN_BOOTS, 1);
							giveAdena(player, 3200, true);
						}
						else if (chance < 1000)
						{
							rewardItems(player, LEATHER_GAUNTLET, 1);
							giveAdena(player, 3200, true);
						}
						
						qs.exitQuest(false, true);
						htmltext = event;
					}
					else
					{
						htmltext = "30034-11.html";
					}
				}
				break;
			}
			case "30344-02.html":
			{
				if (qs.isCond(4))
				{
					if (hasQuestItems(player, LETTER_1ST))
					{
						qs.setCond(5, true);
						takeItems(player, LETTER_1ST, 1);
						giveItems(player, LETTER_2ND, 1);
						htmltext = event;
					}
					else
					{
						htmltext = "30344-03.html";
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
		switch (npc.getId())
		{
			case IRIS:
			{
				switch (qs.getCond())
				{
					case 3:
					{
						htmltext = "30034-01.html";
						break;
					}
					case 4:
					{
						htmltext = "30034-04.html";
						break;
					}
					case 5:
					{
						htmltext = "30034-05.html";
						break;
					}
					case 6:
					{
						htmltext = "30034-09.html";
						break;
					}
					case 7:
					{
						if (hasItem(talker, TOTEM_TOOTH_2ND))
						{
							htmltext = "30034-08.html";
						}
						break;
					}
				}
				break;
			}
			case MAGISTER_ROHMER:
			{
				if (qs.isCond(4))
				{
					htmltext = "30344-01.html";
				}
				else if (qs.isCond(5))
				{
					htmltext = "30344-04.html";
				}
				break;
			}
			case GUARD_LUIS:
			{
				if (qs.isCreated())
				{
					htmltext = (talker.getLevel() >= MIN_LEVEL) ? "30386-01.htm" : "30386-02.htm";
				}
				else if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 1:
						{
							htmltext = "30386-05.html";
							break;
						}
						case 2:
						{
							if (hasItem(talker, FEATHER))
							{
								htmltext = "30386-04.html";
							}
							break;
						}
						case 3:
						{
							htmltext = "30386-08.html";
							break;
						}
					}
				}
				else if (qs.isCompleted())
				{
					htmltext = getAlreadyCompletedMsg(talker);
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		switch (npc.getId())
		{
			case LIZARDMAN_SUB_LEADER:
			case LIZARDMAN_SENTINEL:
			{
				final QuestState qs = getRandomPartyMemberState(killer, 1, 3, npc);
				if ((qs != null) && giveItemRandomly(qs.getPlayer(), npc, FEATHER.getId(), 1, FEATHER.getCount(), 1, true))
				{
					qs.setCond(2);
				}
				break;
			}
			case LIZARDMAN_LEADER:
			case LIZARDMAN_SHAMAN:
			{
				final QuestState qs = getRandomPartyMemberState(killer, 6, 3, npc);
				if ((qs != null) && giveItemRandomly(qs.getPlayer(), npc, TOTEM_TOOTH_2ND.getId(), 1, TOTEM_TOOTH_2ND.getCount(), 0.5, true))
				{
					qs.setCond(7);
				}
				break;
			}
		}
	}
}
