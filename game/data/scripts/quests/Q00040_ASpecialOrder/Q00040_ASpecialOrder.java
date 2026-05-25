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
package quests.Q00040_ASpecialOrder;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * A Special Order (40)
 * @author Janiko
 */
public class Q00040_ASpecialOrder extends Quest
{
	// NPCs
	private static final int HELVETIA = 30081;
	private static final int OFULLE = 31572;
	private static final int GESTO = 30511;
	
	// Items
	private static final int ORANGE_SWIFT_FISH = 6450;
	private static final int ORANGE_UGLY_FISH = 6451;
	private static final int ORANGE_WIDE_FISH = 6452;
	private static final int GOLDEN_COBOL = 5079;
	private static final int BUR_COBOL = 5082;
	private static final int GREAT_COBOL = 5084;
	private static final int WONDROUS_CUBIC = 10632;
	private static final int BOX_OF_FISH = 12764;
	private static final int BOX_OF_SEED = 12765;
	
	// Misc
	private static final int MIN_LEVEL = 40;
	
	public Q00040_ASpecialOrder()
	{
		super(40);
		addStartNpc(HELVETIA);
		addTalkId(HELVETIA, OFULLE, GESTO);
		registerQuestItems(BOX_OF_FISH, BOX_OF_SEED);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "accept":
			{
				qs.setState(State.STARTED);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ACCEPT);
				if (getRandomBoolean())
				{
					qs.setCond(2);
					htmltext = "30081-03.html";
				}
				else
				{
					qs.setCond(5);
					htmltext = "30081-04.html";
				}
				break;
			}
			case "30081-07.html":
			{
				if (qs.isCond(4) && hasQuestItems(player, BOX_OF_FISH))
				{
					rewardItems(player, WONDROUS_CUBIC, 1);
					qs.exitQuest(false, true);
					htmltext = event;
				}
				break;
			}
			case "30081-10.html":
			{
				if (qs.isCond(7) && hasQuestItems(player, BOX_OF_SEED))
				{
					rewardItems(player, WONDROUS_CUBIC, 1);
					qs.exitQuest(false, true);
					htmltext = event;
				}
				break;
			}
			case "31572-02.html":
			case "30511-02.html":
			{
				htmltext = event;
				break;
			}
			case "31572-03.html":
			{
				if (qs.isCond(2))
				{
					qs.setCond(3, true);
					htmltext = event;
				}
				break;
			}
			case "30511-03.html":
			{
				if (qs.isCond(5))
				{
					qs.setCond(6, true);
					htmltext = event;
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
		switch (npc.getId())
		{
			case HELVETIA:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() >= MIN_LEVEL) ? "30081-01.htm" : "30081-02.htm";
						break;
					}
					case State.STARTED:
					{
						switch (qs.getCond())
						{
							case 2:
							case 3:
							{
								htmltext = "30081-05.html";
								break;
							}
							case 4:
							{
								if (hasQuestItems(player, BOX_OF_FISH))
								{
									htmltext = "30081-06.html";
								}
								break;
							}
							case 5:
							case 6:
							{
								htmltext = "30081-08.html";
								break;
							}
							case 7:
							{
								if (hasQuestItems(player, BOX_OF_SEED))
								{
									htmltext = "30081-09.html";
								}
								break;
							}
						}
						break;
					}
					case State.COMPLETED:
					{
						htmltext = getAlreadyCompletedMsg(player);
						break;
					}
				}
				break;
			}
			case OFULLE:
			{
				switch (qs.getCond())
				{
					case 2:
					{
						htmltext = "31572-01.html";
						break;
					}
					case 3:
					{
						if ((getQuestItemsCount(player, ORANGE_SWIFT_FISH) >= 10) && (getQuestItemsCount(player, ORANGE_UGLY_FISH) >= 10) && (getQuestItemsCount(player, ORANGE_WIDE_FISH) >= 10))
						{
							qs.setCond(4, true);
							giveItems(player, BOX_OF_FISH, 1);
							takeItems(player, 10, ORANGE_SWIFT_FISH, ORANGE_UGLY_FISH, ORANGE_WIDE_FISH);
							htmltext = "31572-05.html";
						}
						else
						{
							htmltext = "31572-04.html";
						}
						break;
					}
					case 4:
					{
						htmltext = "31572-06.html";
						break;
					}
				}
				break;
			}
			case GESTO:
			{
				switch (qs.getCond())
				{
					case 5:
					{
						htmltext = "30511-01.html";
						break;
					}
					case 6:
					{
						if ((getQuestItemsCount(player, GOLDEN_COBOL) >= 40) && (getQuestItemsCount(player, BUR_COBOL) >= 40) && (getQuestItemsCount(player, GREAT_COBOL) >= 40))
						{
							qs.setCond(7, true);
							giveItems(player, BOX_OF_SEED, 1);
							takeItems(player, 40, GOLDEN_COBOL, BUR_COBOL, GREAT_COBOL);
							htmltext = "30511-05.html";
						}
						else
						{
							htmltext = "30511-04.html";
						}
						break;
					}
					case 7:
					{
						htmltext = "30511-06.html";
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
}
