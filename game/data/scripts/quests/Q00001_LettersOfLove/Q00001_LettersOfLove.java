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
package quests.Q00001_LettersOfLove;

import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.NpcStringId;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * Letters of Love (1)
 * @author Zoey76
 */
public class Q00001_LettersOfLove extends Quest
{
	// NPCs
	private static final int DARIN = 30048;
	private static final int ROXXY = 30006;
	private static final int BAULRO = 30033;
	
	// Items
	private static final int DARINS_LETTER = 687;
	private static final int ROXXYS_KERCHIEF = 688;
	private static final int DARINS_RECEIPT = 1079;
	private static final int BAULROS_POTION = 1080;
	private static final int NECKLACE_OF_KNOWLEDGE = 906;
	
	// Misc
	private static final int MIN_LEVEL = 2;
	private static final int GUIDE_MISSION = 41;
	
	public Q00001_LettersOfLove()
	{
		super(1);
		addStartNpc(DARIN);
		addTalkId(DARIN, ROXXY, BAULRO);
		registerQuestItems(DARINS_LETTER, ROXXYS_KERCHIEF, DARINS_RECEIPT, BAULROS_POTION);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "30048-03.html":
			case "30048-04.html":
			case "30048-05.html":
			{
				htmltext = event;
				break;
			}
			case "30048-06.htm":
			{
				if (player.getLevel() >= MIN_LEVEL)
				{
					qs.startQuest();
					giveItems(player, DARINS_LETTER, 1);
					htmltext = event;
				}
				break;
			}
		}
		
		return htmltext;
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
				htmltext = (player.getLevel() < MIN_LEVEL) ? "30048-01.html" : "30048-02.html";
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						switch (npc.getId())
						{
							case DARIN:
							{
								htmltext = "30048-07.html";
								break;
							}
							case ROXXY:
							{
								if (hasQuestItems(player, DARINS_LETTER) && !hasQuestItems(player, ROXXYS_KERCHIEF))
								{
									takeItems(player, DARINS_LETTER, -1);
									giveItems(player, ROXXYS_KERCHIEF, 1);
									qs.setCond(2, true);
									htmltext = "30006-01.html";
								}
								break;
							}
						}
						break;
					}
					case 2:
					{
						switch (npc.getId())
						{
							case DARIN:
							{
								if (hasQuestItems(player, ROXXYS_KERCHIEF))
								{
									takeItems(player, ROXXYS_KERCHIEF, -1);
									giveItems(player, DARINS_RECEIPT, 1);
									qs.setCond(3, true);
									htmltext = "30048-08.html";
								}
								break;
							}
							case ROXXY:
							{
								if (hasQuestItems(player, ROXXYS_KERCHIEF))
								{
									htmltext = "30006-02.html";
								}
								break;
							}
						}
						break;
					}
					case 3:
					{
						switch (npc.getId())
						{
							case DARIN:
							{
								if (hasQuestItems(player, DARINS_RECEIPT) || !hasQuestItems(player, BAULROS_POTION))
								{
									htmltext = "30048-09.html";
								}
								break;
							}
							case ROXXY:
							{
								if (hasQuestItems(player, DARINS_RECEIPT) || hasQuestItems(player, BAULROS_POTION))
								{
									htmltext = "30006-03.html";
								}
								break;
							}
							case BAULRO:
							{
								if (hasQuestItems(player, DARINS_RECEIPT))
								{
									takeItems(player, DARINS_RECEIPT, -1);
									giveItems(player, BAULROS_POTION, 1);
									qs.setCond(4, true);
									htmltext = "30033-01.html";
								}
								else if (hasQuestItems(player, BAULROS_POTION))
								{
									htmltext = "30033-02.html";
								}
								break;
							}
						}
						break;
					}
					case 4:
					{
						switch (npc.getId())
						{
							case DARIN:
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
								
								giveItems(player, NECKLACE_OF_KNOWLEDGE, 1);
								addExpAndSp(player, 5672, 446);
								giveAdena(player, 2466, false);
								qs.exitQuest(false, true);
								htmltext = "30048-10.html";
								break;
							}
							case BAULRO:
							{
								if (hasQuestItems(player, BAULROS_POTION))
								{
									htmltext = "30033-02.html";
								}
								break;
							}
							case ROXXY:
							{
								if (hasQuestItems(player, BAULROS_POTION))
								{
									htmltext = "30006-03.html";
								}
								break;
							}
						}
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
		
		return htmltext;
	}
}
