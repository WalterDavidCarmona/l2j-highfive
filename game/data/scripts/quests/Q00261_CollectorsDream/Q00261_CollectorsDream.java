/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package quests.Q00261_CollectorsDream;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.util.LocationUtil;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * Collector's Dream (261)
 * @author xban1x
 */
public class Q00261_CollectorsDream extends Quest
{
	// NPC
	private static final int ALSHUPES = 30222;
	
	// Monsters
	private static final int[] MONSTERS =
	{
		20308, // Hook Spider
		20460, // Crimson Spider
		20466, // Pincer Spider
	};
	
	// Item
	private static final int SPIDER_LEG = 1087;
	
	// Misc
	private static final int MIN_LEVEL = 15;
	private static final int MAX_LEG_COUNT = 8;
	private static final int GUIDE_MISSION = 41;
	
	public Q00261_CollectorsDream()
	{
		super(261);
		addStartNpc(ALSHUPES);
		addTalkId(ALSHUPES);
		addKillId(MONSTERS);
		registerQuestItems(SPIDER_LEG);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && event.equals("30222-03.htm"))
		{
			qs.startQuest();
			return event;
		}
		
		return null;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isCond(1) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, npc, killer, true) && giveItemRandomly(killer, SPIDER_LEG, 1, MAX_LEG_COUNT, 1, true))
		{
			qs.setCond(2);
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = (player.getLevel() >= MIN_LEVEL) ? "30222-02.htm" : "30222-01.htm";
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "30222-04.html";
						break;
					}
					case 2:
					{
						if (getQuestItemsCount(player, SPIDER_LEG) >= MAX_LEG_COUNT)
						{
							// Newbie Guide.
							final Quest newbieGuide = ScriptManager.getInstance().getScript(NewbieGuide.class.getSimpleName());
							if (newbieGuide != null)
							{
								final QuestState newbieGuideQs = newbieGuide.getQuestState(player, true);
								if (!haveNRMemo(newbieGuideQs, GUIDE_MISSION))
								{
									setNRMemo(newbieGuideQs, GUIDE_MISSION);
									setNRMemoState(newbieGuideQs, GUIDE_MISSION, 100000);
									showOnScreenMsg(player, NpcStringId.LAST_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
								}
								else if (((getNRMemoState(newbieGuideQs, GUIDE_MISSION) % 100000000) / 10000000) != 1)
								{
									setNRMemo(newbieGuideQs, GUIDE_MISSION);
									setNRMemoState(newbieGuideQs, GUIDE_MISSION, getNRMemoState(newbieGuideQs, GUIDE_MISSION) + 10000000);
									showOnScreenMsg(player, NpcStringId.LAST_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
								}
								
								newbieGuideQs.setState(State.COMPLETED);
							}
							
							giveAdena(player, 1000, true);
							addExpAndSp(player, 2000, 0);
							qs.exitQuest(true, true);
							htmltext = "30222-05.html";
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
