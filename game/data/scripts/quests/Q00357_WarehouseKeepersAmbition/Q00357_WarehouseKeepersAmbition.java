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
package quests.Q00357_WarehouseKeepersAmbition;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;

/**
 * Warehouse Keeper's Ambition (357)
 * @author Janiko, Mobius
 */
public class Q00357_WarehouseKeepersAmbition extends Quest
{
	// NPC
	private static final int SILVA = 30686;
	
	// Item
	private static final int JADE_CRYSTAL = 5867;
	
	// Monsters
	private static final Map<Integer, Double> DROP_DATA = new HashMap<>();
	static
	{
		DROP_DATA.put(20594, 0.577); // Forest Runner
		DROP_DATA.put(20595, 0.6); // Fline Elder
		DROP_DATA.put(20596, 0.638); // Liele Elder
		DROP_DATA.put(20597, 0.062); // Valley Treant Elder
	}
	
	// Misc
	private static final int MIN_LEVEL = 47;
	
	public Q00357_WarehouseKeepersAmbition()
	{
		super(357);
		addStartNpc(SILVA);
		addTalkId(SILVA);
		addKillId(DROP_DATA.keySet());
		registerQuestItems(JADE_CRYSTAL);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs != null)
		{
			switch (event)
			{
				case "30686-01.htm":
				case "30686-03.htm":
				case "30686-04.htm":
				case "30686-10.html":
				{
					htmltext = event;
					break;
				}
				case "30686-05.htm":
				{
					if (qs.isCreated())
					{
						qs.startQuest();
						htmltext = event;
					}
					break;
				}
				case "30686-09.html":
				{
					final long crystalCount = getQuestItemsCount(player, JADE_CRYSTAL);
					if (crystalCount > 0)
					{
						long adenaReward = crystalCount * 425;
						if (crystalCount < 100)
						{
							adenaReward += 13500;
							htmltext = "30686-08.html";
						}
						else
						{
							adenaReward += 40500;
							htmltext = event;
						}
						
						giveAdena(player, adenaReward, true);
						takeItems(player, JADE_CRYSTAL, -1);
					}
					break;
				}
				case "30686-11.html":
				{
					final long crystalCount = getQuestItemsCount(player, JADE_CRYSTAL);
					if (crystalCount > 0)
					{
						giveAdena(player, (crystalCount * 425) + ((crystalCount >= 100) ? 40500 : 0), true);
						takeItems(player, JADE_CRYSTAL, -1);
					}
					
					qs.exitQuest(true, true);
					htmltext = event;
					break;
				}
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		if (qs.isCreated())
		{
			htmltext = ((talker.getLevel() < MIN_LEVEL) ? "30686-01.html" : "30686-02.htm");
		}
		else if (qs.isStarted())
		{
			htmltext = (hasQuestItems(talker, JADE_CRYSTAL)) ? "30686-07.html" : "30686-06.html";
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, -1, 3, npc);
		if (qs != null)
		{
			giveItemRandomly(qs.getPlayer(), npc, JADE_CRYSTAL, 1, 0, DROP_DATA.get(npc.getId()), true);
		}
	}
}
