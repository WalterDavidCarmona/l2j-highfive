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
package quests.Q00347_GoGetTheCalculator;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * Go Get the Calculator (347)
 * @author Mobius
 */
public class Q00347_GoGetTheCalculator extends Quest
{
	// NPCs
	private static final int BRUNON = 30526;
	private static final int SILVERA = 30527;
	private static final int SPIRON = 30532;
	private static final int BALANKI = 30533;
	
	// Items
	private static final int STOLEN_CALCULATOR = 4285;
	private static final int GEMSTONE = 4286;
	
	// Monster
	private static final int GEMSTONE_BEAST = 20540;
	
	// Reward
	private static final int CALCULATOR = 4393;
	private static final int ADENA = 1500;
	
	// Misc
	private static final int MIN_LEVEL = 12;
	
	public Q00347_GoGetTheCalculator()
	{
		super(347);
		addStartNpc(BRUNON);
		addTalkId(BRUNON, SILVERA, SPIRON, BALANKI);
		addKillId(GEMSTONE_BEAST);
		registerQuestItems(STOLEN_CALCULATOR, GEMSTONE);
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
			case "30526-03.htm":
			case "30526-04.htm":
			case "30526-05.htm":
			case "30526-06.htm":
			case "30526-07.htm":
			case "30532-03.html":
			case "30532-04.html":
			{
				htmltext = event;
				break;
			}
			case "30526-08.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					htmltext = event;
				}
				break;
			}
			case "30526-10.html":
			{
				if (qs.isCond(6))
				{
					takeItems(player, STOLEN_CALCULATOR, -1);
					rewardItems(player, CALCULATOR, 1);
					qs.exitQuest(true, true);
					htmltext = event;
				}
				else
				{
					htmltext = "30526-09.html";
				}
				break;
			}
			case "30526-11.html":
			{
				if (qs.isCond(6))
				{
					takeItems(player, STOLEN_CALCULATOR, -1);
					giveAdena(player, ADENA, true);
					qs.exitQuest(true, true);
					htmltext = event;
				}
				break;
			}
			case "30532-02.html":
			{
				if (qs.isCond(1))
				{
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
			case "30533-02.html":
			{
				if ((qs.isCond(2)) && (player.getAdena() > 100))
				{
					takeItems(player, Inventory.ADENA_ID, 100);
					qs.setCond(3, true);
					htmltext = event;
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
				if (npc.getId() == BRUNON)
				{
					htmltext = (talker.getLevel() >= MIN_LEVEL) ? "30526-01.htm" : "30526-02.html";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case BRUNON:
					{
						if (hasQuestItems(talker, CALCULATOR))
						{
							qs.setCond(6);
						}
						
						switch (qs.getCond())
						{
							case 1:
							case 2:
							{
								htmltext = "30526-13.html";
								break;
							}
							case 3:
							case 4:
							{
								htmltext = "30526-14.html";
								break;
							}
							case 5:
							{
								htmltext = "30526-15.html";
								break;
							}
							case 6:
							{
								htmltext = "30526-09.html";
								break;
							}
						}
						break;
					}
					case SPIRON:
					{
						htmltext = qs.isCond(1) ? "30532-01.html" : "30532-05.html";
						break;
					}
					case BALANKI:
					{
						if (qs.isCond(2))
						{
							htmltext = "30533-01.html";
						}
						else if (qs.getCond() > 2)
						{
							htmltext = "30533-04.html";
						}
						else
						{
							htmltext = "30533-03.html";
						}
						break;
					}
					case SILVERA:
					{
						switch (qs.getCond())
						{
							case 1:
							case 2:
							{
								htmltext = "30527-01.html";
								break;
							}
							case 3:
							{
								qs.setCond(4, true);
								htmltext = "30527-02.html";
								break;
							}
							case 4:
							{
								htmltext = "30527-04.html";
								break;
							}
							case 5:
							{
								takeItems(talker, GEMSTONE, -1);
								giveItems(talker, STOLEN_CALCULATOR, 1);
								qs.setCond(6, true);
								htmltext = "30527-03.html";
								break;
							}
							case 6:
							{
								htmltext = "30527-05.html";
								break;
							}
						}
						break;
					}
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
		final QuestState qs = getRandomPartyMemberState(killer, 4, 3, npc);
		if ((qs != null) && giveItemRandomly(qs.getPlayer(), npc, GEMSTONE, 1, 10, 0.4, true))
		{
			qs.setCond(5);
		}
	}
}
