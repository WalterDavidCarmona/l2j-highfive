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
package quests.Q00028_ChestCaughtWithABaitOfIcyAir;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

import quests.Q00051_OFullesSpecialBait.Q00051_OFullesSpecialBait;

/**
 * Chest Caught With A Bait Of Icy Air (28)<br>
 * Original Jython script by Skeleton.
 * @author nonom
 */
public class Q00028_ChestCaughtWithABaitOfIcyAir extends Quest
{
	// NPCs
	private static final int OFULLE = 31572;
	private static final int KIKI = 31442;
	
	// Items
	private static final int YELLOW_TREASURE_BOX = 6503;
	private static final int KIKIS_LETTER = 7626;
	private static final int ELVEN_RING = 881;
	
	public Q00028_ChestCaughtWithABaitOfIcyAir()
	{
		super(28);
		addStartNpc(OFULLE);
		addTalkId(OFULLE, KIKI);
		registerQuestItems(KIKIS_LETTER);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "31572-04.htm":
			{
				st.startQuest();
				break;
			}
			case "31572-08.htm":
			{
				if (st.isCond(1) && hasQuestItems(player, YELLOW_TREASURE_BOX))
				{
					giveItems(player, KIKIS_LETTER, 1);
					takeItems(player, YELLOW_TREASURE_BOX, -1);
					st.setCond(2, true);
					htmltext = "31572-07.htm";
				}
				break;
			}
			case "31442-03.htm":
			{
				if (st.isCond(2) && hasQuestItems(player, KIKIS_LETTER))
				{
					giveItems(player, ELVEN_RING, 1);
					st.exitQuest(false, true);
					htmltext = "31442-02.htm";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		final int npcId = npc.getId();
		
		switch (st.getState())
		{
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
			case State.CREATED:
			{
				final QuestState qs = player.getQuestState(Q00051_OFullesSpecialBait.class.getSimpleName());
				if (npcId == OFULLE)
				{
					htmltext = "31572-02.htm";
					if (qs != null)
					{
						htmltext = ((player.getLevel() >= 36) && qs.isCompleted()) ? "31572-01.htm" : htmltext;
					}
				}
				break;
			}
			case State.STARTED:
			{
				switch (npcId)
				{
					case OFULLE:
					{
						switch (st.getCond())
						{
							case 1:
							{
								htmltext = "31572-06.htm";
								if (hasQuestItems(player, YELLOW_TREASURE_BOX))
								{
									htmltext = "31572-05.htm";
								}
								break;
							}
							case 2:
							{
								htmltext = "31572-09.htm";
								break;
							}
						}
						break;
					}
					case KIKI:
					{
						if (st.isCond(2))
						{
							htmltext = "31442-01.htm";
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
