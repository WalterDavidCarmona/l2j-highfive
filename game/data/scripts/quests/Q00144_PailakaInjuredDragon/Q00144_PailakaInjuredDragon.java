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
package quests.Q00144_PailakaInjuredDragon;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.LocationUtil;

import instances.PailakaInjuredDragon.PailakaInjuredDragon;

/**
 * Pailaka - Injured Dragon (144)
 * @author Zoey76, Mobius
 */
public class Q00144_PailakaInjuredDragon extends Quest
{
	// NPCs
	private static final int KETRA_ORC_SHAMAN = 32499;
	private static final int KETRA_ORC_SUPPORTER_1 = 32502;
	private static final int KETRA_ORC_INTELLIGENCE_OFFICER = 32509;
	private static final int KETRA_ORC_SUPPORTER2 = 32512;
	
	// Monster
	private static final int LATANA = 18660;
	
	// Items
	private static final int PAILAKA_INSTANT_SHIELD = 13032;
	private static final int QUICK_HEALING_POTION = 13033;
	private static final int SPEAR_OF_SILENOS = 13052;
	private static final int ENHANCED_SPEAR_OF_SILENOS = 13053;
	private static final int COMPLETE_SPEAR_OF_SILENOS = 13054;
	private static final int PAILAKA_SOULSHOT_GRADE_A = 13055;
	private static final int WEAPON_UPGRADE_STAGE_1 = 13056;
	private static final int WEAPON_UPGRADE_STAGE_2 = 13057;
	private static final int SILENOS_HAIR_ACCESSORY = 13058;
	private static final int PAILAKA_SHIRT = 13296;
	private static final int SCROLL_OF_ESCAPE = 736;
	
	// Skills
	private static final SkillHolder PAILAKA_REWARD_ENERGY_REPLENISHING = new SkillHolder(5774, 2);
	private static final SkillHolder[] BUFFS =
	{
		new SkillHolder(1086, 2), // Haste Lv2
		new SkillHolder(1204, 2), // Wind Walk Lv2
		new SkillHolder(1059, 3), // Empower Lv3
		new SkillHolder(1085, 3), // Acumen Lv3
		new SkillHolder(1078, 6), // Concentration Lv6
		new SkillHolder(1068, 3), // Might Lv3
		new SkillHolder(1240, 3), // Guidance Lv3
		new SkillHolder(1077, 3), // Focus Lv3
		new SkillHolder(1242, 3), // Death Whisper Lv3
		new SkillHolder(1062, 2), // Berserker Spirit Lv2
		new SkillHolder(1268, 4), // Vampiric Rage Lv4
		new SkillHolder(1045, 6), // Blessed Body Lv6
	};
	
	// Misc
	private static final int MIN_LEVEL = 73;
	private static final int MAX_LEVEL = 77;
	private static final int MAX_BUFFS = 5;
	private static final int XP_REWARD = 28000000;
	private static final int SP_REWARD = 2850000;
	
	public Q00144_PailakaInjuredDragon()
	{
		super(144);
		addStartNpc(KETRA_ORC_SHAMAN);
		addFirstTalkId(KETRA_ORC_INTELLIGENCE_OFFICER, KETRA_ORC_SUPPORTER2);
		addTalkId(KETRA_ORC_SHAMAN, KETRA_ORC_SUPPORTER_1, KETRA_ORC_INTELLIGENCE_OFFICER, KETRA_ORC_SUPPORTER2);
		addKillId(LATANA);
		registerQuestItems(PAILAKA_INSTANT_SHIELD, QUICK_HEALING_POTION, SPEAR_OF_SILENOS, ENHANCED_SPEAR_OF_SILENOS, COMPLETE_SPEAR_OF_SILENOS, PAILAKA_SOULSHOT_GRADE_A, WEAPON_UPGRADE_STAGE_1, WEAPON_UPGRADE_STAGE_2, SILENOS_HAIR_ACCESSORY);
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
			case "start":
			{
				if (qs.isCreated() && (player.getLevel() >= MIN_LEVEL) && (player.getLevel() <= MAX_LEVEL))
				{
					qs.startQuest();
					qs.setMemoState(1);
				}
				
				htmltext = "32499-07.html";
				break;
			}
			case "1":
			{
				switch (npc.getId())
				{
					// Ask to hear more
					case KETRA_ORC_SHAMAN:
					{
						if (player.getLevel() < MIN_LEVEL)
						{
							htmltext = "32499-03.htm";
						}
						else if (player.getLevel() > MAX_LEVEL)
						{
							htmltext = "32499-04z.htm";
						}
						else
						{
							htmltext = "32499-04.htm";
						}
						break;
					}
					// Enhance the weapon
					case KETRA_ORC_INTELLIGENCE_OFFICER:
					{
						if (!qs.isStarted())
						{
							break;
						}
						
						if (hasQuestItems(player, SPEAR_OF_SILENOS))
						{
							if (hasQuestItems(player, WEAPON_UPGRADE_STAGE_1))
							{
								takeItems(player, SPEAR_OF_SILENOS, -1);
								takeItems(player, WEAPON_UPGRADE_STAGE_1, -1);
								giveItems(player, ENHANCED_SPEAR_OF_SILENOS, 1);
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								htmltext = "32509-02.html";
							}
							else
							{
								htmltext = "32509-05.html";
							}
						}
						else if (hasQuestItems(player, ENHANCED_SPEAR_OF_SILENOS))
						{
							if (hasQuestItems(player, WEAPON_UPGRADE_STAGE_2))
							{
								takeItems(player, ENHANCED_SPEAR_OF_SILENOS, -1);
								takeItems(player, WEAPON_UPGRADE_STAGE_2, -1);
								giveItems(player, COMPLETE_SPEAR_OF_SILENOS, 1);
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								htmltext = "32509-03.html";
							}
							else
							{
								htmltext = "32509-04.html";
							}
						}
						else if (hasQuestItems(player, COMPLETE_SPEAR_OF_SILENOS))
						{
							htmltext = "32509-06.html";
						}
						else if (!hasQuestItems(player, SPEAR_OF_SILENOS))
						{
							htmltext = "32509-01a.html";
						}
						break;
					}
					// Return the Spear
					case KETRA_ORC_SUPPORTER2:
					{
						if (!qs.isCompleted() && qs.isCond(4))
						{
							giveItems(player, PAILAKA_SHIRT, 1);
							giveItems(player, SCROLL_OF_ESCAPE, 1);
							addExpAndSp(player, XP_REWARD, SP_REWARD);
							npc.setTarget(player);
							npc.doCast(PAILAKA_REWARD_ENERGY_REPLENISHING.getSkill());
							qs.exitQuest(false, true);
							
							final Instance instance = InstanceManager.getInstance().getInstance(npc.getInstanceId());
							if (instance != null)
							{
								instance.setDuration(GeneralConfig.INSTANCE_FINISH_TIME);
								instance.setEmptyDestroyTime(0);
							}
							
							htmltext = "32512-02z.html";
						}
						else
						{
							htmltext = "32512-03.html";
						}
						break;
					}
				}
				break;
			}
			// Ask about this strange darkness
			case "3249905":
			{
				htmltext = "32499-05.htm";
				break;
			}
			case "2":
			{
				switch (npc.getId())
				{
					// Keep listening
					case KETRA_ORC_SHAMAN:
					{
						if (qs.isCreated() && (player.getLevel() >= MIN_LEVEL))
						{
							htmltext = "32499-06.htm";
						}
						break;
					}
					// I understand
					case KETRA_ORC_SUPPORTER_1:
					{
						if (qs.isCond(2))
						{
							qs.setCond(3, true);
							qs.setMemoState(3);
							giveItems(player, SPEAR_OF_SILENOS, 1);
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							htmltext = "32502-05.html";
						}
						break;
					}
					// Receive enhancement magic
					case KETRA_ORC_INTELLIGENCE_OFFICER:
					{
						if (countBuffs(player) >= MAX_BUFFS)
						{
							htmltext = "32509-96.html";
						}
						else
						{
							htmltext = "32509-99.html";
						}
						break;
					}
					case KETRA_ORC_SUPPORTER2:
					{
						if (qs.isCompleted())
						{
							htmltext = "32512-03.html";
						}
						break;
					}
				}
				break;
			}
			// Come back later
			case "3249908":
			{
				htmltext = "32499-08a.html";
				break;
			}
			// Go now / Enter Pailaka
			case "3":
			{
				if (qs.isStarted())
				{
					final Quest instance = ScriptManager.getInstance().getScript(PailakaInjuredDragon.class.getSimpleName());
					instance.onEvent("enter", npc, player);
					
					if (qs.isCond(1))
					{
						qs.setCond(2, true);
						htmltext = "32499-09.html";
					}
					else
					{
						htmltext = "32499-11.html";
					}
				}
				break;
			}
			// Keep listening
			case "3250202":
			{
				htmltext = "32502-02.html";
				break;
			}
			// Ask if he wants to rescue the dragon
			case "3250203":
			{
				htmltext = "32502-03.html";
				break;
			}
			// Ask what can be done
			case "3250204":
			{
				htmltext = "32502-04.html";
				break;
			}
			// Ask about the enemy
			case "3250207":
			{
				htmltext = "32502-07.html";
				break;
			}
			// Please select the next spell you wish to receive!
			case "-1":
			case "-2":
			case "-3":
			case "-4":
			case "-5":
			case "-6":
			case "-7":
			case "-8":
			case "-9":
			case "-10":
			case "-11":
			case "-12":
			{
				final int buffCount = countBuffs(player);
				if (buffCount < MAX_BUFFS)
				{
					if (!npc.isCastingNow())
					{
						npc.getVariables().set("i_ai0", npc.getVariables().getInt("i_ai0", 0) + 1);
						npc.setTarget(player);
						npc.doCast(BUFFS[-(Integer.valueOf(event) + 1)].getSkill());
						
						if (buffCount == (MAX_BUFFS - 1))
						{
							htmltext = "32509-97.html";
						}
						else
						{
							htmltext = "32509-98.html";
						}
					}
					else
					{
						htmltext = "32509-98.html";
					}
				}
				else
				{
					htmltext = "32509-96.html";
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
		switch (npc.getId())
		{
			case KETRA_ORC_SHAMAN:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = "32499-01.htm";
						break;
					}
					case State.STARTED:
					{
						htmltext = qs.isCond(1) ? "32499-08.html" : "32499-10.html";
						break;
					}
					case State.COMPLETED:
					{
						htmltext = "32499-02.html";
						break;
					}
				}
				break;
			}
			case KETRA_ORC_SUPPORTER_1:
			{
				htmltext = qs.isCond(2) ? "32502-01.html" : "32502-06.html";
				break;
			}
			case KETRA_ORC_SUPPORTER2:
			{
				if (qs.isCompleted())
				{
					htmltext = "32512-02.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.getId() == KETRA_ORC_SUPPORTER2)
		{
			final QuestState qs = getQuestState(player, false);
			if ((qs != null) && qs.isCompleted())
			{
				return "32512-03.html";
			}
		}
		
		return npc.getId() + ".html";
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isCond(3) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, npc, killer, false))
		{
			qs.setCond(4, true);
		}
	}
	
	private int countBuffs(Player player)
	{
		int count = 0;
		for (SkillHolder skillHolder : BUFFS)
		{
			if (player.isAffectedBySkill(skillHolder.getSkillId()))
			{
				count++;
			}
		}
		
		return count;
	}
}
