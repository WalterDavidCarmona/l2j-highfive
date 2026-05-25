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
package quests.Q00293_TheHiddenVeins;

import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.NpcStringId;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * The Hidden Veins (293)
 * @author xban1x
 */
public class Q00293_TheHiddenVeins extends Quest
{
	// NPCs
	private static final int FILAUR = 30535;
	private static final int CHICHIRIN = 30539;
	
	// Monsters
	private static final int[] MONSTERS =
	{
		20446,
		20447,
		20448,
	};
	
	// Items
	private static final int CHRYSOLITE_ORE = 1488;
	private static final int TORN_MAP_FRAGMENT = 1489;
	private static final int HIDDEN_ORE_MAP = 1490;
	private static final ItemHolder SOULSHOTS_NO_GRADE_FOR_ROOKIES = new ItemHolder(5789, 6000);
	
	// Misc
	private static final int MIN_LEVEL = 6;
	private static final int REQUIRED_TORN_MAP_FRAGMENT = 4;
	private static final int GUIDE_MISSION = 41;
	
	public Q00293_TheHiddenVeins()
	{
		super(293);
		addStartNpc(FILAUR);
		addTalkId(FILAUR, CHICHIRIN);
		addKillId(MONSTERS);
		registerQuestItems(CHRYSOLITE_ORE, TORN_MAP_FRAGMENT, HIDDEN_ORE_MAP);
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
			case "30535-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "30535-07.html":
			{
				qs.exitQuest(true, true);
				htmltext = event;
				break;
			}
			case "30535-08.html":
			{
				htmltext = event;
				break;
			}
			case "30539-03.html":
			{
				if (getQuestItemsCount(player, TORN_MAP_FRAGMENT) >= REQUIRED_TORN_MAP_FRAGMENT)
				{
					giveItems(player, HIDDEN_ORE_MAP, 1);
					takeItems(player, TORN_MAP_FRAGMENT, REQUIRED_TORN_MAP_FRAGMENT);
					htmltext = event;
				}
				else
				{
					htmltext = "30539-02.html";
				}
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if (qs != null)
		{
			final int chance = getRandom(100);
			if (chance > 50)
			{
				giveItems(killer, CHRYSOLITE_ORE, 1);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else if (chance < 5)
			{
				giveItems(killer, TORN_MAP_FRAGMENT, 1);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case FILAUR:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getRace() == Race.DWARF) ? (player.getLevel() >= MIN_LEVEL) ? "30535-03.htm" : "30535-02.htm" : "30535-01.htm";
						break;
					}
					case State.STARTED:
					{
						if (hasAtLeastOneQuestItem(player, CHRYSOLITE_ORE, HIDDEN_ORE_MAP))
						{
							final long ores = getQuestItemsCount(player, CHRYSOLITE_ORE);
							final long maps = getQuestItemsCount(player, HIDDEN_ORE_MAP);
							giveAdena(player, (ores * 5) + (maps * 500) + (((ores + maps) >= 10) ? 2000 : 0), true);
							takeItems(player, -1, CHRYSOLITE_ORE, HIDDEN_ORE_MAP);
							
							if ((player.getLevel() < 25) && (getOneTimeQuestFlag(player, 57) == 0))
							{
								if (!player.isMageClass())
								{
									giveItems(player, SOULSHOTS_NO_GRADE_FOR_ROOKIES);
									playSound(player, "tutorial_voice_026");
								}
								
								setOneTimeQuestFlag(player, 57, 1);
							}
							
							// Newbie Guide.
							final Quest newbieGuide = ScriptManager.getInstance().getScript(NewbieGuide.class.getSimpleName());
							if (newbieGuide != null)
							{
								final QuestState newbieGuideQs = newbieGuide.getQuestState(player, true);
								if (!haveNRMemo(newbieGuideQs, GUIDE_MISSION))
								{
									setNRMemo(newbieGuideQs, GUIDE_MISSION);
									setNRMemoState(newbieGuideQs, GUIDE_MISSION, 1000);
									showOnScreenMsg(player, NpcStringId.ACQUISITION_OF_SOULSHOT_FOR_BEGINNERS_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
								}
								else if (((getNRMemoState(newbieGuideQs, GUIDE_MISSION) % 10000) / 1000) != 1)
								{
									setNRMemo(newbieGuideQs, GUIDE_MISSION);
									setNRMemoState(newbieGuideQs, GUIDE_MISSION, getNRMemoState(newbieGuideQs, GUIDE_MISSION) + 1000);
									showOnScreenMsg(player, NpcStringId.ACQUISITION_OF_SOULSHOT_FOR_BEGINNERS_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
								}
							}
							
							htmltext = (ores > 0) ? (maps > 0) ? "30535-10.html" : "30535-06.html" : "30535-09.html";
						}
						else
						{
							htmltext = "30535-05.html";
						}
						break;
					}
				}
				break;
			}
			case CHICHIRIN:
			{
				htmltext = "30539-01.html";
				break;
			}
		}
		
		return htmltext;
	}
}
