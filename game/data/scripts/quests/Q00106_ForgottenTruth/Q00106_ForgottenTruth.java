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
package quests.Q00106_ForgottenTruth;

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
 * Forgotten Truth (106)
 * @author Janiko
 */
public class Q00106_ForgottenTruth extends Quest
{
	// NPCs
	private static final int THIFIELL = 30358;
	private static final int KARTA = 30133;
	
	// Monster
	private static final int TUMRAN_ORC_BRIGAND = 27070;
	
	// Items
	private static final int ONYX_TALISMAN1 = 984;
	private static final int ONYX_TALISMAN2 = 985;
	private static final int ANCIENT_SCROLL = 986;
	private static final int ANCIENT_CLAY_TABLET = 987;
	private static final int KARTAS_TRANSLATION = 988;
	
	// Rewards
	private static final int ELDRITCH_DAGGER = 989;
	private static final int ELDRITCH_STAFF = 2373;
	private static final ItemHolder[] REWARDS =
	{
		new ItemHolder(1060, 100), // Lesser Healing Potion
		new ItemHolder(4412, 10), // Echo Crystal - Theme of Battle
		new ItemHolder(4413, 10), // Echo Crystal - Theme of Love
		new ItemHolder(4414, 10), // Echo Crystal - Theme of Solitude
		new ItemHolder(4415, 10), // Echo Crystal - Theme of Feast
		new ItemHolder(4416, 10), // Echo Crystal - Theme of Celebration
	};
	private static final ItemHolder SPIRITSHOTS_NO_GRADE_FOR_ROOKIES = new ItemHolder(5790, 3000);
	private static final ItemHolder SOULSHOTS_NO_GRADE = new ItemHolder(1835, 1000);
	private static final ItemHolder SPIRITSHOTS_NO_GRADE = new ItemHolder(2509, 500);
	
	// Misc
	private static final int MIN_LEVEL = 10;
	private static final int GUIDE_MISSION = 41;
	
	public Q00106_ForgottenTruth()
	{
		super(106);
		addStartNpc(THIFIELL);
		addTalkId(THIFIELL, KARTA);
		addKillId(TUMRAN_ORC_BRIGAND);
		registerQuestItems(KARTAS_TRANSLATION, ONYX_TALISMAN1, ONYX_TALISMAN2, ANCIENT_SCROLL, ANCIENT_CLAY_TABLET);
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
			case "30358-04.htm":
			{
				htmltext = event;
				break;
			}
			case "30358-05.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					giveItems(player, ONYX_TALISMAN1, 1);
					htmltext = event;
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
		if ((qs != null) && qs.isCond(2) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, npc, killer, true) && (getRandom(100) < 20) && hasQuestItems(killer, ONYX_TALISMAN2))
		{
			if (!hasQuestItems(killer, ANCIENT_SCROLL))
			{
				giveItems(killer, ANCIENT_SCROLL, 1);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
			}
			else if (!hasQuestItems(killer, ANCIENT_CLAY_TABLET))
			{
				qs.setCond(3, true);
				giveItems(killer, ANCIENT_CLAY_TABLET, 1);
			}
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		
		switch (npc.getId())
		{
			case THIFIELL:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						if (talker.getRace() == Race.DARK_ELF)
						{
							htmltext = talker.getLevel() >= MIN_LEVEL ? "30358-03.htm" : "30358-02.htm";
						}
						else
						{
							htmltext = "30358-01.htm";
						}
						break;
					}
					case State.STARTED:
					{
						if (hasAtLeastOneQuestItem(talker, ONYX_TALISMAN1, ONYX_TALISMAN2) && !hasQuestItems(talker, KARTAS_TRANSLATION))
						{
							htmltext = "30358-06.html";
						}
						else if (qs.isCond(4) && hasQuestItems(talker, KARTAS_TRANSLATION))
						{
							if ((talker.getLevel() < 25) && talker.isMageClass())
							{
								giveItems(talker, SPIRITSHOTS_NO_GRADE_FOR_ROOKIES);
								playSound(talker, "tutorial_voice_027");
							}
							
							if (!talker.isMageClass())
							{
								giveItems(talker, SOULSHOTS_NO_GRADE);
							}
							else
							{
								giveItems(talker, SPIRITSHOTS_NO_GRADE);
							}
							
							for (ItemHolder reward : REWARDS)
							{
								giveItems(talker, reward);
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
							
							giveAdena(talker, 10266, true);
							addExpAndSp(talker, 24195, 2074);
							giveItems(talker, qs.getPlayer().isMageClass() ? ELDRITCH_STAFF : ELDRITCH_DAGGER, 1);
							qs.exitQuest(false, true);
							talker.sendPacket(new SocialAction(talker.getObjectId(), 3));
							htmltext = "30358-07.html";
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
			case KARTA:
			{
				if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 1:
						{
							if (hasQuestItems(talker, ONYX_TALISMAN1))
							{
								qs.setCond(2, true);
								takeItems(talker, ONYX_TALISMAN1, -1);
								giveItems(talker, ONYX_TALISMAN2, 1);
								htmltext = "30133-01.html";
							}
							break;
						}
						case 2:
						{
							if (hasQuestItems(talker, ONYX_TALISMAN2))
							{
								htmltext = "30133-02.html";
							}
							break;
						}
						case 3:
						{
							if (hasQuestItems(talker, ANCIENT_SCROLL, ANCIENT_CLAY_TABLET))
							{
								qs.setCond(4, true);
								takeItems(talker, -1, ANCIENT_SCROLL, ANCIENT_CLAY_TABLET, ONYX_TALISMAN2);
								giveItems(talker, KARTAS_TRANSLATION, 1);
								htmltext = "30133-03.html";
							}
							break;
						}
						case 4:
						{
							if (hasQuestItems(talker, KARTAS_TRANSLATION))
							{
								htmltext = "30133-04.html";
							}
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
