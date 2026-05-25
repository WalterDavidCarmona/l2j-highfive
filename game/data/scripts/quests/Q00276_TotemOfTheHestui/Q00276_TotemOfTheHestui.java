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
package quests.Q00276_TotemOfTheHestui;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.util.LocationUtil;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * Totem of the Hestui (276)
 * @author xban1x
 */
public class Q00276_TotemOfTheHestui extends Quest
{
	// NPC
	private static final int TANAPI = 30571;
	
	// Items
	private static final int KASHA_PARASITE = 1480;
	private static final int KASHA_CRYSTAL = 1481;
	
	// Monsters
	private static final int KASHA_BEAR = 20479;
	private static final int KASHA_BEAR_TOTEM = 27044;
	
	// Rewards
	private static final int[] REWARDS =
	{
		29,
		1500,
	};
	
	// Misc
	private static final List<ItemHolder> SPAWN_CHANCES = new ArrayList<>();
	static
	{
		SPAWN_CHANCES.add(new ItemHolder(79, 100));
		SPAWN_CHANCES.add(new ItemHolder(69, 20));
		SPAWN_CHANCES.add(new ItemHolder(59, 15));
		SPAWN_CHANCES.add(new ItemHolder(49, 10));
		SPAWN_CHANCES.add(new ItemHolder(39, 2));
	}
	private static final int MIN_LEVEL = 15;
	private static final int GUIDE_MISSION = 41;
	
	public Q00276_TotemOfTheHestui()
	{
		super(276);
		addStartNpc(TANAPI);
		addTalkId(TANAPI);
		addKillId(KASHA_BEAR, KASHA_BEAR_TOTEM);
		registerQuestItems(KASHA_PARASITE, KASHA_CRYSTAL);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && event.equals("30571-03.htm"))
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
		if ((qs != null) && qs.isCond(1) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, killer, npc, true))
		{
			switch (npc.getId())
			{
				case KASHA_BEAR:
				{
					final long chance1 = getQuestItemsCount(killer, KASHA_PARASITE);
					final int chance2 = getRandom(100);
					boolean chance3 = true;
					for (ItemHolder spawnChance : SPAWN_CHANCES)
					{
						if ((chance1 >= spawnChance.getId()) && (chance2 <= spawnChance.getCount()))
						{
							addSpawn(KASHA_BEAR_TOTEM, killer);
							takeItems(killer, KASHA_PARASITE, -1);
							chance3 = false;
							break;
						}
					}
					
					if (chance3)
					{
						giveItemRandomly(killer, KASHA_PARASITE, 1, 0, 1, true);
					}
					break;
				}
				case KASHA_BEAR_TOTEM:
				{
					if (giveItemRandomly(killer, KASHA_CRYSTAL, 1, 1, 1, true))
					{
						qs.setCond(2);
					}
					break;
				}
			}
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
				htmltext = (player.getRace() == Race.ORC) ? (player.getLevel() >= MIN_LEVEL) ? "30571-02.htm" : "30571-01.htm" : "30571-00.htm";
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "30571-04.html";
						break;
					}
					case 2:
					{
						if (hasQuestItems(player, KASHA_CRYSTAL))
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
							
							for (int reward : REWARDS)
							{
								rewardItems(player, reward, 1);
							}
							
							qs.exitQuest(true, true);
							htmltext = "30571-05.html";
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
