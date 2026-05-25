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
package quests.Q00296_TarantulasSpiderSilk;

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
 * Tarantula's Spider Silk (296)
 * @author xban1x
 */
public class Q00296_TarantulasSpiderSilk extends Quest
{
	// NPCs
	private static final int TRADER_MION = 30519;
	private static final int DEFENDER_NATHAN = 30548;
	
	// Monsters
	private static final int[] MONSTERS =
	{
		20394,
		20403,
		20508,
	};
	
	// Items
	private static final int TARANTULA_SPIDER_SILK = 1493;
	private static final int TARANTULA_SPINNERETTE = 1494;
	
	// Misc
	private static final int MIN_LEVEL = 15;
	private static final int GUIDE_MISSION = 41;
	
	public Q00296_TarantulasSpiderSilk()
	{
		super(296);
		addStartNpc(TRADER_MION);
		addTalkId(TRADER_MION, DEFENDER_NATHAN);
		addKillId(MONSTERS);
		registerQuestItems(TARANTULA_SPIDER_SILK, TARANTULA_SPINNERETTE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		String html = null;
		if (qs == null)
		{
			return null;
		}
		
		switch (event)
		{
			case "30519-03.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					html = event;
				}
				break;
			}
			case "30519-06.html":
			{
				if (qs.isStarted())
				{
					qs.exitQuest(true, true);
					html = event;
				}
				break;
			}
			case "30519-07.html":
			{
				if (qs.isStarted())
				{
					html = event;
				}
				break;
			}
			case "30548-03.html":
			{
				if (qs.isStarted())
				{
					if (hasQuestItems(player, TARANTULA_SPINNERETTE))
					{
						giveItems(player, TARANTULA_SPIDER_SILK, (15 + getRandom(9)) * getQuestItemsCount(player, TARANTULA_SPINNERETTE));
						takeItems(player, TARANTULA_SPINNERETTE, -1);
						html = event;
					}
					else
					{
						html = "30548-02.html";
					}
				}
				break;
			}
		}
		
		return html;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, npc, killer, true))
		{
			final int chance = getRandom(100);
			if (chance > 95)
			{
				giveItemRandomly(killer, npc, TARANTULA_SPINNERETTE, 1, 0, 1, true);
			}
			else if (chance > 45)
			{
				giveItemRandomly(killer, npc, TARANTULA_SPIDER_SILK, 1, 0, 1, true);
			}
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String html = getNoQuestMsg(talker);
		if (qs.isCreated() && (npc.getId() == TRADER_MION))
		{
			html = (talker.getLevel() >= MIN_LEVEL ? "30519-02.htm" : "30519-01.htm");
		}
		else if (qs.isStarted())
		{
			if (npc.getId() == TRADER_MION)
			{
				final long silk = getQuestItemsCount(talker, TARANTULA_SPIDER_SILK);
				if (silk >= 1)
				{
					giveAdena(talker, (silk * 30) + (silk >= 10 ? 2000 : 0), true);
					takeItems(talker, TARANTULA_SPIDER_SILK, -1);
					
					// Newbie Guide.
					final Quest newbieGuide = ScriptManager.getInstance().getScript(NewbieGuide.class.getSimpleName());
					if (newbieGuide != null)
					{
						final QuestState newbieGuideQs = newbieGuide.getQuestState(talker, true);
						if (!haveNRMemo(newbieGuideQs, GUIDE_MISSION))
						{
							setNRMemo(newbieGuideQs, GUIDE_MISSION);
							setNRMemoState(newbieGuideQs, GUIDE_MISSION, 100000);
							showOnScreenMsg(talker, NpcStringId.LAST_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
						}
						else if (((getNRMemoState(newbieGuideQs, GUIDE_MISSION) % 100000000) / 10000000) != 1)
						{
							setNRMemo(newbieGuideQs, GUIDE_MISSION);
							setNRMemoState(newbieGuideQs, GUIDE_MISSION, getNRMemoState(newbieGuideQs, GUIDE_MISSION) + 10000000);
							showOnScreenMsg(talker, NpcStringId.LAST_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
						}
						
						newbieGuideQs.setState(State.COMPLETED);
					}
					
					html = "30519-05.html";
				}
				else
				{
					html = "30519-04.html";
				}
			}
			else if (npc.getId() == DEFENDER_NATHAN)
			{
				html = "30548-01.html";
			}
		}
		
		return html;
	}
}
