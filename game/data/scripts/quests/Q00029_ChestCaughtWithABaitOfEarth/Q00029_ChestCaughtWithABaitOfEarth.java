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
package quests.Q00029_ChestCaughtWithABaitOfEarth;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

import quests.Q00052_WilliesSpecialBait.Q00052_WilliesSpecialBait;

/**
 * Chest Caught With A Bait Of Earth (29)<br>
 * Original Jython script by Skeleton.
 * @author nonom
 */
public class Q00029_ChestCaughtWithABaitOfEarth extends Quest
{
	// NPCs
	private static final int WILLIE = 31574;
	private static final int ANABEL = 30909;
	
	// Items
	private static final int PURPLE_TREASURE_BOX = 6507;
	private static final int SMALL_GLASS_BOX = 7627;
	private static final int PLATED_LEATHER_GLOVES = 2455;
	
	public Q00029_ChestCaughtWithABaitOfEarth()
	{
		super(29);
		addStartNpc(WILLIE);
		addTalkId(WILLIE, ANABEL);
		registerQuestItems(SMALL_GLASS_BOX);
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
			case "31574-04.htm":
			{
				st.startQuest();
				break;
			}
			case "31574-08.htm":
			{
				if (st.isCond(1) && hasQuestItems(player, PURPLE_TREASURE_BOX))
				{
					giveItems(player, SMALL_GLASS_BOX, 1);
					takeItems(player, PURPLE_TREASURE_BOX, -1);
					st.setCond(2, true);
					htmltext = "31574-07.htm";
				}
				break;
			}
			case "30909-03.htm":
			{
				if (st.isCond(2) && hasQuestItems(player, SMALL_GLASS_BOX))
				{
					giveItems(player, PLATED_LEATHER_GLOVES, 1);
					st.exitQuest(false, true);
					htmltext = "30909-02.htm";
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
				final QuestState qs = player.getQuestState(Q00052_WilliesSpecialBait.class.getSimpleName());
				if (npcId == WILLIE)
				{
					htmltext = "31574-02.htm";
					if (qs != null)
					{
						htmltext = ((player.getLevel() >= 48) && qs.isCompleted()) ? "31574-01.htm" : htmltext;
					}
				}
				break;
			}
			case State.STARTED:
			{
				switch (npcId)
				{
					case WILLIE:
					{
						switch (st.getCond())
						{
							case 1:
							{
								htmltext = "31574-06.htm";
								if (hasQuestItems(player, PURPLE_TREASURE_BOX))
								{
									htmltext = "31574-05.htm";
								}
								break;
							}
							case 2:
							{
								htmltext = "31574-09.htm";
								break;
							}
						}
						break;
					}
					case ANABEL:
					{
						if (st.isCond(2))
						{
							htmltext = "30909-01.htm";
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
