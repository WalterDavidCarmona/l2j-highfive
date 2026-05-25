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
package quests.Q00103_SpiritOfCraftsman;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.util.LocationUtil;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * Spirit of Craftsman (103)
 * @author Janiko
 */
public class Q00103_SpiritOfCraftsman extends Quest
{
	// NPCs
	private static final int BLACKSMITH_KAROYD = 30307;
	private static final int CECON = 30132;
	private static final int HARNE = 30144;
	
	// Items
	private static final int KAROYDS_LETTER = 968;
	private static final int CECKTINONS_VOUCHER1 = 969;
	private static final int CECKTINONS_VOUCHER2 = 970;
	private static final int SOUL_CATCHER = 971;
	private static final int PRESERVE_OIL = 972;
	private static final int ZOMBIE_HEAD = 973;
	private static final int STEELBENDERS_HEAD = 974;
	private static final int BONE_FRAGMENT = 1107;
	
	// Monsters
	private static final int MARSH_ZOMBIE = 20015;
	private static final int DOOM_SOLDIER = 20455;
	private static final int SKELETON_HUNTER = 20517;
	private static final int SKELETON_HUNTER_ARCHER = 20518;
	
	// Rewards
	private static final int BLOODSABER = 975;
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
	private static final ItemHolder SOULSHOTS_NO_GRADE = new ItemHolder(1835, 1000);
	private static final ItemHolder SPIRITSHOTS_NO_GRADE = new ItemHolder(2509, 500);
	
	// Misc
	private static final int MIN_LEVEL = 10;
	private static final int GUIDE_MISSION = 41;
	
	public Q00103_SpiritOfCraftsman()
	{
		super(103);
		addStartNpc(BLACKSMITH_KAROYD);
		addTalkId(BLACKSMITH_KAROYD, CECON, HARNE);
		addKillId(MARSH_ZOMBIE, DOOM_SOLDIER, SKELETON_HUNTER, SKELETON_HUNTER_ARCHER);
		registerQuestItems(KAROYDS_LETTER, CECKTINONS_VOUCHER1, CECKTINONS_VOUCHER2, SOUL_CATCHER, PRESERVE_OIL, ZOMBIE_HEAD, STEELBENDERS_HEAD, BONE_FRAGMENT);
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
			case "30307-04.htm":
			{
				htmltext = event;
				break;
			}
			case "30307-05.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					giveItems(player, KAROYDS_LETTER, 1);
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
			case BLACKSMITH_KAROYD:
			{
				if (qs.isCreated())
				{
					if (talker.getRace() != Race.DARK_ELF)
					{
						htmltext = "30307-01.htm";
					}
					else if (talker.getLevel() < MIN_LEVEL)
					{
						htmltext = "30307-02.htm";
					}
					else
					{
						htmltext = "30307-03.htm";
					}
				}
				else if (qs.isStarted())
				{
					if (hasAtLeastOneQuestItem(talker, KAROYDS_LETTER, CECKTINONS_VOUCHER1, CECKTINONS_VOUCHER2))
					{
						htmltext = "30307-06.html";
					}
					else if (hasQuestItems(talker, STEELBENDERS_HEAD))
					{
						if ((talker.getLevel() < 25) && !talker.isMageClass())
						{
							giveItems(talker, SOULSHOTS_NO_GRADE_FOR_ROOKIES);
							playSound(talker, "tutorial_voice_026");
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
							rewardItems(talker, reward);
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
						
						addExpAndSp(talker, 46663, 3999);
						giveAdena(talker, 19799, true);
						rewardItems(talker, BLOODSABER, 1);
						qs.exitQuest(false, true);
						talker.sendPacket(new SocialAction(talker.getObjectId(), 3));
						htmltext = "30307-07.html";
					}
				}
				else if (qs.isCompleted())
				{
					htmltext = getAlreadyCompletedMsg(talker);
				}
				break;
			}
			case CECON:
			{
				if (qs.isStarted())
				{
					if (hasQuestItems(talker, KAROYDS_LETTER))
					{
						qs.setCond(2, true);
						takeItems(talker, KAROYDS_LETTER, 1);
						giveItems(talker, CECKTINONS_VOUCHER1, 1);
						htmltext = "30132-01.html";
					}
					else if (hasAtLeastOneQuestItem(talker, CECKTINONS_VOUCHER1, CECKTINONS_VOUCHER2))
					{
						htmltext = "30132-02.html";
					}
					else if (hasQuestItems(talker, SOUL_CATCHER))
					{
						qs.setCond(6, true);
						takeItems(talker, SOUL_CATCHER, 1);
						giveItems(talker, PRESERVE_OIL, 1);
						htmltext = "30132-03.html";
					}
					else if (hasQuestItems(talker, PRESERVE_OIL) && !hasQuestItems(talker, ZOMBIE_HEAD, STEELBENDERS_HEAD))
					{
						htmltext = "30132-04.html";
					}
					else if (hasQuestItems(talker, ZOMBIE_HEAD))
					{
						qs.setCond(8, true);
						takeItems(talker, ZOMBIE_HEAD, 1);
						giveItems(talker, STEELBENDERS_HEAD, 1);
						htmltext = "30132-05.html";
					}
					else if (hasQuestItems(talker, STEELBENDERS_HEAD))
					{
						htmltext = "30132-06.html";
					}
				}
				break;
			}
			case HARNE:
			{
				if (qs.isStarted())
				{
					if (hasQuestItems(talker, CECKTINONS_VOUCHER1))
					{
						qs.setCond(3, true);
						takeItems(talker, CECKTINONS_VOUCHER1, 1);
						giveItems(talker, CECKTINONS_VOUCHER2, 1);
						htmltext = "30144-01.html";
					}
					else if (hasQuestItems(talker, CECKTINONS_VOUCHER2))
					{
						if (getQuestItemsCount(talker, BONE_FRAGMENT) >= 10)
						{
							qs.setCond(5, true);
							takeItems(talker, CECKTINONS_VOUCHER2, 1);
							takeItems(talker, BONE_FRAGMENT, 10);
							giveItems(talker, SOUL_CATCHER, 1);
							htmltext = "30144-03.html";
						}
						else
						{
							htmltext = "30144-02.html";
						}
					}
					else if (hasQuestItems(talker, SOUL_CATCHER))
					{
						htmltext = "30144-04.html";
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, -1, 3, npc);
		if (qs == null)
		{
			return;
		}
		
		switch (npc.getId())
		{
			case MARSH_ZOMBIE:
			{
				if (hasQuestItems(killer, PRESERVE_OIL) && (getRandom(10) < 5) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, npc, killer, true))
				{
					giveItems(killer, ZOMBIE_HEAD, 1);
					takeItems(killer, PRESERVE_OIL, -1);
					qs.setCond(7, true);
				}
				break;
			}
			case DOOM_SOLDIER:
			case SKELETON_HUNTER:
			case SKELETON_HUNTER_ARCHER:
			{
				if (hasQuestItems(killer, CECKTINONS_VOUCHER2) && giveItemRandomly(qs.getPlayer(), npc, BONE_FRAGMENT, 1, 10, 1, true))
				{
					qs.setCond(4, true);
				}
				break;
			}
		}
	}
}
