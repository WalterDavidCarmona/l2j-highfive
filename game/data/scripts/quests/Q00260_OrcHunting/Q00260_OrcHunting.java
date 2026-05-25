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
package quests.Q00260_OrcHunting;

import java.util.HashMap;
import java.util.Map;

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
 * Orc Hunting (260)
 * @author xban1x
 */
public class Q00260_OrcHunting extends Quest
{
	// NPC
	private static final int RAYEN = 30221;
	
	// Items
	private static final int ORC_AMULET = 1114;
	private static final int ORC_NECKLACE = 1115;
	private static final ItemHolder SPIRITSHOTS_NO_GRADE_FOR_ROOKIES = new ItemHolder(5790, 3000);
	private static final ItemHolder SOULSHOTS_NO_GRADE_FOR_ROOKIES = new ItemHolder(5789, 6000);
	
	// Monsters
	private static final Map<Integer, Integer> MONSTERS = new HashMap<>();
	static
	{
		MONSTERS.put(20468, ORC_AMULET); // Kaboo Orc
		MONSTERS.put(20469, ORC_AMULET); // Kaboo Orc Archer
		MONSTERS.put(20470, ORC_AMULET); // Kaboo Orc Grunt
		MONSTERS.put(20471, ORC_NECKLACE); // Kaboo Orc Fighter
		MONSTERS.put(20472, ORC_NECKLACE); // Kaboo Orc Fighter Leader
		MONSTERS.put(20473, ORC_NECKLACE); // Kaboo Orc Fighter Lieutenant
	}
	
	// Misc
	private static final int MIN_LEVEL = 6;
	private static final int GUIDE_MISSION = 41;
	
	public Q00260_OrcHunting()
	{
		super(260);
		addStartNpc(RAYEN);
		addTalkId(RAYEN);
		addKillId(MONSTERS.keySet());
		registerQuestItems(ORC_AMULET, ORC_NECKLACE);
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
			case "30221-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "30221-07.html":
			{
				qs.exitQuest(true, true);
				htmltext = event;
				break;
			}
			case "30221-08.html":
			{
				htmltext = event;
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && (getRandom(10) > 4))
		{
			giveItems(killer, MONSTERS.get(npc.getId()), 1);
			playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
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
				htmltext = (player.getRace() == Race.ELF) ? (player.getLevel() >= MIN_LEVEL) ? "30221-03.htm" : "30221-02.html" : "30221-01.html";
				break;
			}
			case State.STARTED:
			{
				if (hasAtLeastOneQuestItem(player, getRegisteredItemIds()))
				{
					final long amulets = getQuestItemsCount(player, ORC_AMULET);
					final long necklaces = getQuestItemsCount(player, ORC_NECKLACE);
					giveAdena(player, ((amulets * 12) + (necklaces * 30) + ((amulets + necklaces) >= 10 ? 1000 : 0)), true);
					takeItems(player, -1, getRegisteredItemIds());
					
					if ((player.getLevel() < 25) && (getOneTimeQuestFlag(player, 57) == 0))
					{
						if (player.isMageClass())
						{
							giveItems(player, SPIRITSHOTS_NO_GRADE_FOR_ROOKIES);
							playSound(player, "tutorial_voice_027");
						}
						else
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
					
					htmltext = "30221-06.html";
				}
				else
				{
					htmltext = "30221-05.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
}
