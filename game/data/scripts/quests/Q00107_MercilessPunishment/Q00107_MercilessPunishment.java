/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package quests.Q00107_MercilessPunishment;

import org.l2jmobius.gameserver.config.PlayerConfig;
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
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.util.LocationUtil;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * Merciless Punishment (107)
 * @author Janiko
 */
public class Q00107_MercilessPunishment extends Quest
{
	// Npc
	private static final int URUTU_CHIEF_HATOS = 30568;
	private static final int CENTURION_PARUGON = 30580;
	
	// Items
	private static final int HATOSS_ORDER_1 = 1553;
	private static final int HATOSS_ORDER_2 = 1554;
	private static final int HATOSS_ORDER_3 = 1555;
	private static final int LETTER_TO_DARK_ELF = 1556;
	private static final int LETTER_TO_HUMAN = 1557;
	private static final int LETTER_TO_ELF = 1558;
	
	// Monster
	private static final int BARANKA_MESSENGER = 27041;
	
	// Rewards
	private static final int BUTCHER = 1510;
	private static final ItemHolder[] REWARDS =
	{
		new ItemHolder(1060, 100), // Lesser Healing Potion
		new ItemHolder(4412, 10), // Echo Crystal - Theme of Battle
		new ItemHolder(4413, 10), // Echo Crystal - Theme of Love
		new ItemHolder(4414, 10), // Echo Crystal - Theme of Solitude
		new ItemHolder(4415, 10), // Echo Crystal - Theme of Feast
		new ItemHolder(4416, 10), // Echo Crystal - Theme of Celebration
	};
	private static final ItemHolder SOULSHOTS_NO_GRADE_FOR_ROOKIES = new ItemHolder(5789, 7000);
	
	// Misc
	private static final int MIN_LEVEL = 10;
	private static final int GUIDE_MISSION = 41;
	
	public Q00107_MercilessPunishment()
	{
		super(107);
		addStartNpc(URUTU_CHIEF_HATOS);
		addTalkId(URUTU_CHIEF_HATOS, CENTURION_PARUGON);
		addKillId(BARANKA_MESSENGER);
		registerQuestItems(HATOSS_ORDER_1, HATOSS_ORDER_2, HATOSS_ORDER_3, LETTER_TO_DARK_ELF, LETTER_TO_HUMAN, LETTER_TO_ELF);
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
			case "30568-04.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					giveItems(player, HATOSS_ORDER_1, 1);
					htmltext = event;
				}
				break;
			}
			case "30568-07.html":
			{
				giveAdena(player, 200, true);
				playSound(player, QuestSound.ITEMSOUND_QUEST_GIVEUP);
				qs.exitQuest(true);
				htmltext = event;
				break;
			}
			case "30568-08.html":
			{
				if (qs.isCond(3) && hasQuestItems(player, HATOSS_ORDER_1))
				{
					qs.setCond(4);
					takeItems(player, HATOSS_ORDER_1, -1);
					giveItems(player, HATOSS_ORDER_2, 1);
					htmltext = event;
				}
				break;
			}
			case "30568-10.html":
			{
				if (qs.isCond(5) && hasQuestItems(player, HATOSS_ORDER_2))
				{
					qs.setCond(6);
					takeItems(player, HATOSS_ORDER_2, -1);
					giveItems(player, HATOSS_ORDER_3, 1);
					htmltext = event;
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		switch (npc.getId())
		{
			case URUTU_CHIEF_HATOS:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						if (talker.getRace() != Race.ORC)
						{
							htmltext = "30568-01.htm";
						}
						else if (talker.getLevel() < MIN_LEVEL)
						{
							htmltext = "30568-02.htm";
						}
						else
						{
							htmltext = "30568-03.htm";
						}
						break;
					}
					case State.STARTED:
					{
						switch (qs.getCond())
						{
							case 1:
							case 2:
							{
								if (hasQuestItems(talker, HATOSS_ORDER_1))
								{
									htmltext = "30568-05.html";
								}
								break;
							}
							case 3:
							{
								if (hasQuestItems(talker, HATOSS_ORDER_1, LETTER_TO_HUMAN))
								{
									htmltext = "30568-06.html";
								}
								break;
							}
							case 4:
							{
								if (hasQuestItems(talker, HATOSS_ORDER_2, LETTER_TO_HUMAN))
								{
									htmltext = "30568-08.html";
								}
								break;
							}
							case 5:
							{
								if (hasQuestItems(talker, HATOSS_ORDER_2, LETTER_TO_HUMAN, LETTER_TO_DARK_ELF))
								{
									htmltext = "30568-09.html";
								}
								break;
							}
							case 6:
							{
								if (hasQuestItems(talker, HATOSS_ORDER_3, LETTER_TO_HUMAN, LETTER_TO_DARK_ELF))
								{
									htmltext = "30568-10.html";
								}
								break;
							}
							case 7:
							{
								if (hasQuestItems(talker, HATOSS_ORDER_3, LETTER_TO_HUMAN, LETTER_TO_DARK_ELF, LETTER_TO_ELF))
								{
									if (talker.getLevel() < 25)
									{
										giveItems(talker, SOULSHOTS_NO_GRADE_FOR_ROOKIES);
										playSound(talker, "tutorial_voice_026");
									}
									
									// Newbie Guide.
									final Quest newbieGuide = ScriptManager.getInstance().getScript(NewbieGuide.class.getSimpleName());
									if (newbieGuide != null)
									{
										final QuestState newbieGuideQs = newbieGuide.getQuestState(talker, true);
										if (!haveNRMemo(newbieGuideQs, GUIDE_MISSION))
										{
											setNRMemo(newbieGuideQs, GUIDE_MISSION);
											setNRMemoState(newbieGuideQs, GUIDE_MISSION, 100000);
											
											showOnScreenMsg(talker, NpcStringId.ACQUISITION_OF_RACE_SPECIFIC_WEAPON_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
										}
										else if (((getNRMemoState(newbieGuideQs, GUIDE_MISSION) % 1000000) / 100000) != 1)
										{
											setNRMemo(newbieGuideQs, GUIDE_MISSION);
											setNRMemoState(newbieGuideQs, GUIDE_MISSION, getNRMemoState(newbieGuideQs, GUIDE_MISSION) + 100000);
											showOnScreenMsg(talker, NpcStringId.ACQUISITION_OF_RACE_SPECIFIC_WEAPON_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
										}
									}
									
									addExpAndSp(talker, 34565, 2962);
									giveAdena(talker, 14666, true);
									for (ItemHolder reward : REWARDS)
									{
										giveItems(talker, reward);
									}
									
									giveItems(talker, BUTCHER, 1);
									qs.exitQuest(false, true);
									talker.sendPacket(new SocialAction(talker.getObjectId(), 3));
									htmltext = "30568-11.html";
								}
								break;
							}
						}
						break;
					}
					case State.COMPLETED:
					{
						htmltext = getAlreadyCompletedMsg(talker);
						break;
					}
				}
				break;
			}
			case CENTURION_PARUGON:
			{
				if (qs.isStarted() && qs.isCond(1) && hasQuestItems(talker, HATOSS_ORDER_1))
				{
					qs.setCond(2, true);
					htmltext = "30580-01.html";
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
		if ((qs != null) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, npc, killer, true))
		{
			switch (qs.getCond())
			{
				case 2:
				{
					if (hasQuestItems(killer, HATOSS_ORDER_1))
					{
						giveItems(killer, LETTER_TO_HUMAN, 1);
						qs.setCond(3, true);
					}
					break;
				}
				case 4:
				{
					if (hasQuestItems(killer, HATOSS_ORDER_2))
					{
						giveItems(killer, LETTER_TO_DARK_ELF, 1);
						qs.setCond(5, true);
					}
					break;
				}
				case 6:
				{
					if (hasQuestItems(killer, HATOSS_ORDER_3))
					{
						giveItems(killer, LETTER_TO_ELF, 1);
						qs.setCond(7, true);
					}
					break;
				}
			}
		}
	}
}
