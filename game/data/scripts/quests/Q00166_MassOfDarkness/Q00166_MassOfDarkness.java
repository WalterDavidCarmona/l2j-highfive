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
package quests.Q00166_MassOfDarkness;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.NpcStringId;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * Mass of Darkness (166)
 * @author xban1x
 */
public class Q00166_MassOfDarkness extends Quest
{
	// NPCs
	private static final int UNDRIAS = 30130;
	private static final int IRIA = 30135;
	private static final int DORANKUS = 30139;
	private static final int TRUDY = 30143;
	
	// Items
	private static final int UNDRIAS_LETTER = 1088;
	private static final int CEREMONIAL_DAGGER = 1089;
	private static final int DREVIANT_WINE = 1090;
	private static final int GARMIELS_SCRIPTURE = 1091;
	
	// Misc
	private static final Map<Integer, Integer> NPC_ITEMS = new HashMap<>();
	static
	{
		NPC_ITEMS.put(IRIA, CEREMONIAL_DAGGER);
		NPC_ITEMS.put(DORANKUS, DREVIANT_WINE);
		NPC_ITEMS.put(TRUDY, GARMIELS_SCRIPTURE);
	}
	private static final int MIN_LEVEL = 2;
	private static final int GUIDE_MISSION = 41;
	
	public Q00166_MassOfDarkness()
	{
		super(166);
		addStartNpc(UNDRIAS);
		addTalkId(UNDRIAS, IRIA, DORANKUS, TRUDY);
		registerQuestItems(UNDRIAS_LETTER, CEREMONIAL_DAGGER, DREVIANT_WINE, GARMIELS_SCRIPTURE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && event.equals("30130-03.htm"))
		{
			qs.startQuest();
			giveItems(player, UNDRIAS_LETTER, 1);
			return event;
		}
		
		return null;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case UNDRIAS:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getRace() == Race.DARK_ELF) ? (player.getLevel() >= MIN_LEVEL) ? "30130-02.htm" : "30130-01.htm" : "30130-00.htm";
						break;
					}
					case State.STARTED:
					{
						if (qs.isCond(2) && hasQuestItems(player, UNDRIAS_LETTER, CEREMONIAL_DAGGER, DREVIANT_WINE, GARMIELS_SCRIPTURE))
						{
							// Newbie Guide.
							final Quest newbieGuide = ScriptManager.getInstance().getScript(NewbieGuide.class.getSimpleName());
							if (newbieGuide != null)
							{
								final QuestState newbieGuideQs = newbieGuide.getQuestState(player, true);
								if (!haveNRMemo(newbieGuideQs, GUIDE_MISSION))
								{
									setNRMemo(newbieGuideQs, GUIDE_MISSION);
									setNRMemoState(newbieGuideQs, GUIDE_MISSION, 1);
									showOnScreenMsg(player, NpcStringId.DELIVERY_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
								}
								else if ((getNRMemoState(newbieGuideQs, GUIDE_MISSION) % 10) != 1)
								{
									setNRMemo(newbieGuideQs, GUIDE_MISSION);
									setNRMemoState(newbieGuideQs, GUIDE_MISSION, getNRMemoState(newbieGuideQs, GUIDE_MISSION) + 1);
									showOnScreenMsg(player, NpcStringId.DELIVERY_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
								}
							}
							
							addExpAndSp(player, 5672, 466);
							giveAdena(player, 2966, true);
							qs.exitQuest(false, true);
							htmltext = "30130-05.html";
						}
						else
						{
							htmltext = "30130-04.html";
						}
						break;
					}
					case State.COMPLETED:
					{
						htmltext = getAlreadyCompletedMsg(player);
						break;
					}
				}
				break;
			}
			case IRIA:
			case DORANKUS:
			case TRUDY:
			{
				if (qs.isStarted())
				{
					final int npcId = npc.getId();
					final int itemId = NPC_ITEMS.get(npcId);
					if (qs.isCond(1) && !hasQuestItems(player, itemId))
					{
						giveItems(player, itemId, 1);
						if (hasQuestItems(player, CEREMONIAL_DAGGER, DREVIANT_WINE, GARMIELS_SCRIPTURE))
						{
							qs.setCond(2, true);
						}
						
						htmltext = npcId + "-01.html";
					}
					else
					{
						htmltext = npcId + "-02.html";
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
}
