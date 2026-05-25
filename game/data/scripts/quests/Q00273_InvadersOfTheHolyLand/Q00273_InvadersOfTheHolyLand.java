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
package quests.Q00273_InvadersOfTheHolyLand;

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
 * Invaders of the Holy Land (273)
 * @author xban1x
 */
public class Q00273_InvadersOfTheHolyLand extends Quest
{
	// NPC
	private static final int VARKEES = 30566;
	
	// Monsters
	private static final Map<Integer, Integer> MONSTERS = new HashMap<>();
	static
	{
		MONSTERS.put(20311, 90); // Rakeclaw Imp
		MONSTERS.put(20312, 87); // Rakeclaw Imp Hunter
		MONSTERS.put(20313, 77); // Rakeclaw Imp Chieftain
	}
	
	// Items
	private static final int BLACK_SOULSTONE = 1475;
	private static final int RED_SOULSTONE = 1476;
	private static final ItemHolder SOULSHOTS_NO_GRADE_FOR_ROOKIES = new ItemHolder(5789, 6000);
	
	// Misc
	private static final int MIN_LEVEL = 6;
	private static final int GUIDE_MISSION = 41;
	
	public Q00273_InvadersOfTheHolyLand()
	{
		super(273);
		addStartNpc(VARKEES);
		addTalkId(VARKEES);
		addKillId(MONSTERS.keySet());
		registerQuestItems(BLACK_SOULSTONE, RED_SOULSTONE);
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
				case "30566-04.htm":
				{
					qs.startQuest();
					htmltext = event;
					break;
				}
				case "30566-08.html":
				{
					qs.exitQuest(true, true);
					htmltext = event;
					break;
				}
				case "30566-09.html":
				{
					htmltext = event;
					break;
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
			if (getRandom(100) <= MONSTERS.get(npc.getId()))
			{
				giveItems(killer, BLACK_SOULSTONE, 1);
			}
			else
			{
				giveItems(killer, RED_SOULSTONE, 1);
			}
			
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
				htmltext = (player.getRace() == Race.ORC) ? (player.getLevel() >= MIN_LEVEL) ? "30566-03.htm" : "30566-02.htm" : "30566-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (hasAtLeastOneQuestItem(player, BLACK_SOULSTONE, RED_SOULSTONE))
				{
					final long black = getQuestItemsCount(player, BLACK_SOULSTONE);
					final long red = getQuestItemsCount(player, RED_SOULSTONE);
					giveAdena(player, (red * 10) + (black * 3) + ((red > 0) ? (((red + black) >= 10) ? 1800 : 0) : ((black >= 10) ? 1500 : 0)), true);
					takeItems(player, -1, BLACK_SOULSTONE, RED_SOULSTONE);
					
					if ((player.getLevel() < 25) && (getOneTimeQuestFlag(player, 57) == 0))
					{
						giveItems(player, SOULSHOTS_NO_GRADE_FOR_ROOKIES);
						playSound(player, "tutorial_voice_026");
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
					
					htmltext = (red > 0) ? "30566-07.html" : "30566-06.html";
				}
				else
				{
					htmltext = "30566-05.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
}
