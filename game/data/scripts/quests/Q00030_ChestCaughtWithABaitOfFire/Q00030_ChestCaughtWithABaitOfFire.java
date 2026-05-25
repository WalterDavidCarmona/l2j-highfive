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
package quests.Q00030_ChestCaughtWithABaitOfFire;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

import quests.Q00053_LinnaeusSpecialBait.Q00053_LinnaeusSpecialBait;

/**
 * Chest Caught With A Bait Of Fire (30)<br>
 * Original Jython script by Ethernaly.
 * @author nonom
 */
public class Q00030_ChestCaughtWithABaitOfFire extends Quest
{
	// NPCs
	private static final int LINNAEUS = 31577;
	private static final int RUKAL = 30629;
	
	// Items
	private static final int RED_TREASURE_BOX = 6511;
	private static final int RUKAL_MUSICAL = 7628;
	private static final int PROTECTION_NECKLACE = 916;
	
	public Q00030_ChestCaughtWithABaitOfFire()
	{
		super(30);
		addStartNpc(LINNAEUS);
		addTalkId(LINNAEUS, RUKAL);
		registerQuestItems(RUKAL_MUSICAL);
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
			case "31577-02.htm":
			{
				st.startQuest();
				break;
			}
			case "31577-04a.htm":
			{
				if (st.isCond(1) && hasQuestItems(player, RED_TREASURE_BOX))
				{
					giveItems(player, RUKAL_MUSICAL, 1);
					takeItems(player, RED_TREASURE_BOX, -1);
					st.setCond(2, true);
					htmltext = "31577-04.htm";
				}
				break;
			}
			case "30629-02.htm":
			{
				if (st.isCond(2) && hasQuestItems(player, RUKAL_MUSICAL))
				{
					giveItems(player, PROTECTION_NECKLACE, 1);
					st.exitQuest(false, true);
					htmltext = "30629-03.htm";
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
				final QuestState qs = player.getQuestState(Q00053_LinnaeusSpecialBait.class.getSimpleName());
				if (npcId == LINNAEUS)
				{
					htmltext = "31577-00.htm";
					if (qs != null)
					{
						htmltext = ((player.getLevel() >= 61) && qs.isCompleted()) ? "31577-01.htm" : htmltext;
					}
				}
				break;
			}
			case State.STARTED:
			{
				switch (npcId)
				{
					case LINNAEUS:
					{
						switch (st.getCond())
						{
							case 1:
							{
								htmltext = "31577-03a.htm";
								if (hasQuestItems(player, RED_TREASURE_BOX))
								{
									htmltext = "31577-03.htm";
								}
								break;
							}
							case 2:
							{
								htmltext = "31577-05.htm";
								break;
							}
						}
						break;
					}
					case RUKAL:
					{
						if (st.isCond(2))
						{
							htmltext = "30629-01.htm";
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
