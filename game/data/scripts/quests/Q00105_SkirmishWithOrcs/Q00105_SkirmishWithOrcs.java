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
package quests.Q00105_SkirmishWithOrcs;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.util.LocationUtil;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * Skimirish with Orcs (105)
 * @author Janiko
 */
public class Q00105_SkirmishWithOrcs extends Quest
{
	// NPC
	private static final int KENDNELL = 30218;
	
	// Items
	private static final int KENDELLS_1ST_ORDER = 1836;
	private static final int KENDELLS_2ND_ORDER = 1837;
	private static final int KENDELLS_3RD_ORDER = 1838;
	private static final int KENDELLS_4TH_ORDER = 1839;
	private static final int KENDELLS_5TH_ORDER = 1840;
	private static final int KENDELLS_6TH_ORDER = 1841;
	private static final int KENDELLS_7TH_ORDER = 1842;
	private static final int KENDELLS_8TH_ORDER = 1843;
	private static final int KABOO_CHIEFS_1ST_TORQUE = 1844;
	private static final int KABOO_CHIEFS_2ST_TORQUE = 1845;
	private static final Map<Integer, Integer> MONSTER_DROP = new HashMap<>();
	static
	{
		MONSTER_DROP.put(27059, KENDELLS_1ST_ORDER); // Uoph (Kaboo Chief)
		MONSTER_DROP.put(27060, KENDELLS_2ND_ORDER); // Kracha (Kaboo Chief)
		MONSTER_DROP.put(27061, KENDELLS_3RD_ORDER); // Batoh (Kaboo Chief)
		MONSTER_DROP.put(27062, KENDELLS_4TH_ORDER); // Tanukia (Kaboo Chief)
		MONSTER_DROP.put(27064, KENDELLS_5TH_ORDER); // Turel (Kaboo Chief)
		MONSTER_DROP.put(27065, KENDELLS_6TH_ORDER); // Roko (Kaboo Chief)
		MONSTER_DROP.put(27067, KENDELLS_7TH_ORDER); // Kamut (Kaboo Chief)
		MONSTER_DROP.put(27068, KENDELLS_8TH_ORDER); // Murtika (Kaboo Chief)
	}
	private static final int[] KENDNELLS_ORDERS =
	{
		KENDELLS_1ST_ORDER,
		KENDELLS_2ND_ORDER,
		KENDELLS_3RD_ORDER,
		KENDELLS_4TH_ORDER,
		KENDELLS_5TH_ORDER,
		KENDELLS_6TH_ORDER,
		KENDELLS_7TH_ORDER,
		KENDELLS_8TH_ORDER
	};
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
	private static final ItemHolder SOULSHOTS_NO_GRADE_FOR_ROOKIES = new ItemHolder(5789, 7000);
	private static final ItemHolder SOULSHOTS_NO_GRADE = new ItemHolder(1835, 1000);
	private static final ItemHolder SPIRITSHOTS_NO_GRADE = new ItemHolder(2509, 500);
	private static final ItemHolder RED_SUNSET_SWORD = new ItemHolder(981, 1);
	private static final ItemHolder RED_SUNSET_STAFF = new ItemHolder(754, 1);
	
	// Misc
	private static final int MIN_LEVEL = 10;
	private static final int GUIDE_MISSION = 41;
	
	public Q00105_SkirmishWithOrcs()
	{
		super(105);
		addStartNpc(KENDNELL);
		addTalkId(KENDNELL);
		addKillId(MONSTER_DROP.keySet());
		registerQuestItems(KENDNELLS_ORDERS);
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
			case "30218-04.html":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					giveItems(player, KENDNELLS_ORDERS[getRandom(0, 3)], 1);
					htmltext = event;
				}
				break;
			}
			case "30218-05.html":
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
		if ((qs != null) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, npc, killer, true))
		{
			switch (npc.getId())
			{
				case 27059:
				case 27060:
				case 27061:
				case 27062:
				{
					if (qs.isCond(1) && hasQuestItems(killer, MONSTER_DROP.get(npc.getId())))
					{
						giveItems(killer, KABOO_CHIEFS_1ST_TORQUE, 1);
						qs.setCond(2, true);
					}
					break;
				}
				case 27064:
				case 27065:
				case 27067:
				case 27068:
				{
					if (qs.isCond(3) && hasQuestItems(killer, MONSTER_DROP.get(npc.getId())))
					{
						giveItems(killer, KABOO_CHIEFS_2ST_TORQUE, 1);
						qs.setCond(4, true);
					}
					break;
				}
			}
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if (talker.getRace() == Race.ELF)
				{
					htmltext = (talker.getLevel() >= MIN_LEVEL) ? "30218-03.htm" : "30218-02.htm";
				}
				else
				{
					htmltext = "30218-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (hasAtLeastOneQuestItem(talker, KENDELLS_1ST_ORDER, KENDELLS_2ND_ORDER, KENDELLS_3RD_ORDER, KENDELLS_4TH_ORDER))
				{
					htmltext = "30218-06.html";
				}
				
				if (qs.isCond(2) && hasQuestItems(talker, KABOO_CHIEFS_1ST_TORQUE))
				{
					for (int i = 0; i < 4; i++)
					{
						takeItems(talker, KENDNELLS_ORDERS[i], -1);
					}
					
					takeItems(talker, KABOO_CHIEFS_1ST_TORQUE, 1);
					giveItems(talker, KENDNELLS_ORDERS[getRandom(4, 7)], 1);
					qs.setCond(3, true);
					htmltext = "30218-07.html";
				}
				
				if (hasAtLeastOneQuestItem(talker, KENDELLS_5TH_ORDER, KENDELLS_6TH_ORDER, KENDELLS_7TH_ORDER, KENDELLS_8TH_ORDER))
				{
					htmltext = "30218-08.html";
				}
				
				if (qs.isCond(4) && hasQuestItems(talker, KABOO_CHIEFS_2ST_TORQUE))
				{
					for (ItemHolder reward : REWARDS)
					{
						giveItems(talker, reward);
					}
					
					if (!talker.isMageClass())
					{
						giveItems(talker, SOULSHOTS_NO_GRADE);
					}
					else
					{
						giveItems(talker, SPIRITSHOTS_NO_GRADE);
					}
					
					if (!talker.isMageClass() && !qs.isCompleted())
					{
						giveItems(talker, RED_SUNSET_SWORD);
					}
					else if (!qs.isCompleted())
					{
						giveItems(talker, RED_SUNSET_STAFF);
					}
					
					if (talker.getLevel() < 25)
					{
						if (talker.isMageClass())
						{
							giveItems(talker, SPIRITSHOTS_NO_GRADE_FOR_ROOKIES);
							playSound(talker, "tutorial_voice_027");
						}
						else
						{
							giveItems(talker, SOULSHOTS_NO_GRADE_FOR_ROOKIES);
							playSound(talker, "tutorial_voice_026");
						}
					}
					
					talker.sendPacket(new SocialAction(talker.getObjectId(), 3));
					
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
					
					giveAdena(talker, 17599, true);
					addExpAndSp(talker, 41478, 3555);
					qs.exitQuest(false, true);
					htmltext = "30218-09.html";
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(talker);
				break;
			}
		}
		
		return htmltext;
	}
}
