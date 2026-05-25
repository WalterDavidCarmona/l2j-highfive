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
package quests.Q00174_SupplyCheck;

import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.NpcStringId;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * Supply Check (174)
 * @author malyelfik
 */
public class Q00174_SupplyCheck extends Quest
{
	// NPCs
	private static final int NIKA = 32167;
	private static final int BENIS = 32170;
	private static final int MARCELA = 32173;
	
	// Items
	private static final int WAREHOUSE_MANIFEST = 9792;
	private static final int GROCERY_STORE_MANIFEST = 9793;
	private static final int[] REWARD =
	{
		23, // Wooden Breastplate
		43, // Wooden Helmet
		49, // Gloves
		2386, // Wooden Gaiters
		37, // Leather Shoes
	};
	
	// Misc
	private static final int MIN_LEVEL = 2;
	private static final int GUIDE_MISSION = 41;
	
	public Q00174_SupplyCheck()
	{
		super(174);
		addStartNpc(MARCELA);
		addTalkId(MARCELA, BENIS, NIKA);
		registerQuestItems(WAREHOUSE_MANIFEST, GROCERY_STORE_MANIFEST);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		if (event.equalsIgnoreCase("32173-03.htm"))
		{
			qs.startQuest();
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
			case MARCELA:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() >= MIN_LEVEL) ? "32173-01.htm" : "32173-02.htm";
						break;
					}
					case State.STARTED:
					{
						switch (qs.getCond())
						{
							case 1:
							{
								htmltext = "32173-04.html";
								break;
							}
							case 2:
							{
								qs.setCond(3, true);
								takeItems(player, WAREHOUSE_MANIFEST, -1);
								htmltext = "32173-05.html";
								break;
							}
							case 3:
							{
								htmltext = "32173-06.html";
								break;
							}
							case 4:
							{
								for (int itemId : REWARD)
								{
									giveItems(player, itemId, 1);
								}
								
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
								
								addExpAndSp(player, 5672, 446);
								giveAdena(player, 2466, true);
								qs.exitQuest(false, true);
								htmltext = "32173-07.html";
								break;
							}
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
			case BENIS:
			{
				if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 1:
						{
							qs.setCond(2, true);
							giveItems(player, WAREHOUSE_MANIFEST, 1);
							htmltext = "32170-01.html";
							break;
						}
						case 2:
						{
							htmltext = "32170-02.html";
							break;
						}
						default:
						{
							htmltext = "32170-03.html";
							break;
						}
					}
				}
				break;
			}
			case NIKA:
			{
				if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 1:
						case 2:
						{
							htmltext = "32167-01.html";
							break;
						}
						case 3:
						{
							qs.setCond(4, true);
							giveItems(player, GROCERY_STORE_MANIFEST, 1);
							htmltext = "32167-02.html";
							break;
						}
						case 4:
						{
							htmltext = "32167-03.html";
							break;
						}
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
}
