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
package quests.Q00281_HeadForTheHills;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.NpcStringId;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * Head for the Hills! (281)
 * @author xban1x
 */
public class Q00281_HeadForTheHills extends Quest
{
	// NPC
	private static final int MERCELA = 32173;
	
	// Item
	private static final int CLAWS = 9796;
	
	// Monsters
	private static final Map<Integer, Integer> MONSTERS = new HashMap<>();
	
	// Rewards
	private static final int[] REWARDS =
	{
		115, // Earring of Wisdom
		876, // Ring of Anguish
		907, // Necklace of Anguish
		22, // Leather Shirt
		428, // Feriotic Tunic
		1100, // Cotton Tunic
		29, // Leather Pants
		463, // Feriotic Stockings
		1103, // Cotton Stockings
		736, // Scroll of Escape
	};
	private static final ItemHolder SOULSHOTS_NO_GRADE_FOR_ROOKIES = new ItemHolder(5789, 6000);
	private static final ItemHolder SPIRITSHOTS_NO_GRADE_FOR_ROOKIES = new ItemHolder(5790, 3000);
	static
	{
		MONSTERS.put(22234, 390); // Green Goblin
		MONSTERS.put(22235, 450); // Mountain Werewolf
		MONSTERS.put(22236, 650); // Muertos Archer
		MONSTERS.put(22237, 720); // Mountain Fungus
		MONSTERS.put(22238, 920); // Mountain Werewolf Chief
		MONSTERS.put(22239, 990); // Muertos Guard
	}
	
	// Misc
	private static final int MIN_LEVEL = 6;
	private static final int GUIDE_MISSION = 41;
	
	public Q00281_HeadForTheHills()
	{
		super(281);
		addStartNpc(MERCELA);
		addTalkId(MERCELA);
		addKillId(MONSTERS.keySet());
		registerQuestItems(CLAWS);
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
			case "32173-03.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "32173-06.html":
			{
				if (hasQuestItems(player, CLAWS))
				{
					final long claws = getQuestItemsCount(player, CLAWS);
					giveAdena(player, ((claws * 23) + (claws >= 10 ? 400 : 0)), true);
					takeItems(player, CLAWS, -1);
					
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
					
					htmltext = event;
				}
				else
				{
					htmltext = "32173-07.html";
				}
				break;
			}
			case "32173-08.html":
			{
				htmltext = event;
				break;
			}
			case "32173-09.html":
			{
				qs.exitQuest(true, true);
				htmltext = event;
				break;
			}
			case "32173-11.html":
			{
				if (getQuestItemsCount(player, CLAWS) >= 50)
				{
					if (getRandom(1000) <= 360)
					{
						giveItems(player, REWARDS[getRandom(9)], 1);
					}
					else
					{
						giveItems(player, REWARDS[9], 1);
					}
					
					takeItems(player, CLAWS, 50);
					
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
					
					htmltext = event;
				}
				else
				{
					htmltext = "32173-10.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && (getRandom(1000) <= MONSTERS.get(npc.getId())))
		{
			giveItems(killer, CLAWS, 1);
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
				htmltext = (player.getLevel() >= MIN_LEVEL) ? "32173-01.htm" : "32173-02.htm";
				break;
			}
			case State.STARTED:
			{
				htmltext = hasQuestItems(player, CLAWS) ? "32173-05.html" : "32173-04.html";
				break;
			}
		}
		
		return htmltext;
	}
}
