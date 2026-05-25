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
package ai.others.NewbieGuide;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;

import quests.Q00255_Tutorial.Q00255_Tutorial;

/**
 * Class handle all newbie guide tasks
 * @author Zealar, Mobius
 */
public final class NewbieGuide extends Script
{
	// Suffix
	private static final String SUFFIX_FIGHTER_5_LEVEL = "-f05.htm";
	private static final String SUFFIX_FIGHTER_10_LEVEL = "-f10.htm";
	private static final String SUFFIX_FIGHTER_15_LEVEL = "-f15.htm";
	private static final String SUFFIX_FIGHTER_20_LEVEL = "-f20.htm";
	private static final String SUFFIX_MAGE_7_LEVEL = "-m07.htm";
	private static final String SUFFIX_MAGE_14_LEVEL = "-m14.htm";
	private static final String SUFFIX_MAGE_20_LEVEL = "-m20.htm";
	
	// Vars
	private static final int FIRST_COUPON_SIZE = 5;
	private static final int SECOND_COUPON_SIZE = 1;
	
	// Newbie helpers
	private static final int NEWBIE_GUIDE_HUMAN = 30598;
	private static final int NEWBIE_GUIDE_ELF = 30599;
	private static final int NEWBIE_GUIDE_DARK_ELF = 30600;
	private static final int NEWBIE_GUIDE_DWARF = 30601;
	private static final int NEWBIE_GUIDE_ORC = 30602;
	private static final int NEWBIE_GUIDE_KAMAEL = 32135;
	private static final int NEWBIE_GUIDE_GLUDIN = 31076;
	private static final int NEWBIE_GUIDE_GLUDIO = 31077;
	private static final int ADVENTURERS_GUIDE = 32327;
	
	private static final int GUIDE_MISSION = 41;
	
	// Item
	private static final int SOULSHOT_NO_GRADE_FOR_BEGINNERS = 5789;
	private static final int SPIRITSHOT_NO_GRADE_FOR_BEGINNERS = 5790;
	private static final int SCROLL_RECOVERY_NO_GRADE = 8594;
	
	private static final int APPRENTICE_ADVENTURERS_WEAPON_EXCHANGE_COUPON = 7832;
	private static final int ADVENTURERS_MAGIC_ACCESSORY_EXCHANGE_COUPON = 7833;
	
	// Buffs
	private static final SkillHolder WIND_WALK_FOR_BEGINNERS = new SkillHolder(4322, 1);
	private static final SkillHolder SHIELD_FOR_BEGINNERS = new SkillHolder(4323, 1);
	private static final SkillHolder BLESS_THE_BODY_FOR_BEGINNERS = new SkillHolder(4324, 1);
	private static final SkillHolder VAMPIRIC_RAGE_FOR_BEGINNERS = new SkillHolder(4325, 1);
	private static final SkillHolder REGENERATION_FOR_BEGINNERS = new SkillHolder(4326, 1);
	private static final SkillHolder HASTE_FOR_BEGINNERS = new SkillHolder(4327, 1);
	private static final SkillHolder BLESS_THE_SOUL_FOR_BEGINNERS = new SkillHolder(4328, 1);
	private static final SkillHolder ACUMEN_FOR_BEGINNERS = new SkillHolder(4329, 1);
	private static final SkillHolder CONCENTRATION_FOR_BEGINNERS = new SkillHolder(4330, 1);
	private static final SkillHolder EMPOWER_FOR_BEGINNERS = new SkillHolder(4331, 1);
	private static final SkillHolder LIFE_CUBIC_FOR_BEGINNERS = new SkillHolder(4338, 1);
	private static final SkillHolder BLESSING_OF_PROTECTION = new SkillHolder(5182, 1);
	private static final SkillHolder ADVENTURERS_HASTE = new SkillHolder(5632, 1);
	private static final SkillHolder ADVENTURERS_MAGIC_BARRIER = new SkillHolder(5637, 1);
	
	// Buylist
	private static final int WEAPON_MULTISELL = 305986001;
	private static final int ACCESORIES_MULTISELL = 305986002;
	
	private static final Map<Integer, List<Location>> TELEPORT_MAP = new HashMap<>();
	static
	{
		final Location TALKING_ISLAND_VILLAGE = new Location(-84081, 243227, -3723);
		final Location DARK_ELF_VILLAGE = new Location(12111, 16686, -4582);
		final Location DWARVEN_VILLAGE = new Location(115632, -177996, -905);
		final Location ELVEN_VILLAGE = new Location(45475, 48359, -3060);
		final Location ORC_VILLAGE = new Location(-45032, -113598, -192);
		final Location KAMAEL_VILLAGE = new Location(-119697, 44532, 380);
		
		TELEPORT_MAP.put(NEWBIE_GUIDE_HUMAN, Arrays.asList(DARK_ELF_VILLAGE, DWARVEN_VILLAGE, ELVEN_VILLAGE, ORC_VILLAGE, KAMAEL_VILLAGE));
		TELEPORT_MAP.put(NEWBIE_GUIDE_ELF, Arrays.asList(DARK_ELF_VILLAGE, DWARVEN_VILLAGE, TALKING_ISLAND_VILLAGE, ORC_VILLAGE, KAMAEL_VILLAGE));
		TELEPORT_MAP.put(NEWBIE_GUIDE_DARK_ELF, Arrays.asList(DWARVEN_VILLAGE, TALKING_ISLAND_VILLAGE, ELVEN_VILLAGE, ORC_VILLAGE, KAMAEL_VILLAGE));
		TELEPORT_MAP.put(NEWBIE_GUIDE_DWARF, Arrays.asList(DARK_ELF_VILLAGE, TALKING_ISLAND_VILLAGE, ELVEN_VILLAGE, ORC_VILLAGE, KAMAEL_VILLAGE));
		TELEPORT_MAP.put(NEWBIE_GUIDE_ORC, Arrays.asList(DARK_ELF_VILLAGE, DWARVEN_VILLAGE, TALKING_ISLAND_VILLAGE, ELVEN_VILLAGE, KAMAEL_VILLAGE));
		TELEPORT_MAP.put(NEWBIE_GUIDE_KAMAEL, Arrays.asList(TALKING_ISLAND_VILLAGE, DARK_ELF_VILLAGE, ELVEN_VILLAGE, DWARVEN_VILLAGE, ORC_VILLAGE));
	}
	
	private NewbieGuide()
	{
		final int[] newbieList =
		{
			NEWBIE_GUIDE_HUMAN,
			NEWBIE_GUIDE_ELF,
			NEWBIE_GUIDE_DARK_ELF,
			NEWBIE_GUIDE_DWARF,
			NEWBIE_GUIDE_ORC,
			NEWBIE_GUIDE_KAMAEL,
			NEWBIE_GUIDE_GLUDIN,
			NEWBIE_GUIDE_GLUDIO,
			ADVENTURERS_GUIDE
		};
		
		addStartNpc(newbieList);
		addFirstTalkId(newbieList);
		addTalkId(newbieList);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player talker)
	{
		if (event.endsWith(".htm"))
		{
			return event;
		}
		
		if (event.startsWith("teleport"))
		{
			final String[] tel = event.split("_");
			if (tel.length != 2)
			{
				return teleportRequest(talker, npc, -1);
			}
			
			return teleportRequest(talker, npc, Integer.parseInt(tel[1]));
		}
		
		String htmltext = null;
		final QuestState qs = getQuestState(talker, true);
		int ask = Integer.parseInt(event.split(";")[0]);
		int reply = Integer.parseInt(event.split(";")[1]);
		switch (ask)
		{
			case -7:
			{
				switch (reply)
				{
					case 1:
					{
						if (talker.getRace() == Race.KAMAEL)
						{
							if (talker.getRace() != npc.getRace())
							{
								htmltext = "32135-003.htm";
							}
							else if ((talker.getLevel() > 20) || ((talker.getRace() != Race.KAMAEL) || (talker.getPlayerClass().level() != 0)))
							{
								htmltext = "32135-002.htm";
							}
							else if (talker.getPlayerClass() == PlayerClass.MALE_SOLDIER)
							{
								if (talker.getLevel() <= 5)
								{
									htmltext = "32135-kmf05.htm";
								}
								else if (talker.getLevel() <= 10)
								{
									htmltext = "32135-kmf10.htm";
								}
								else if (talker.getLevel() <= 15)
								{
									htmltext = "32135-kmf15.htm";
								}
								else
								{
									htmltext = "32135-kmf20.htm";
								}
							}
							else if (talker.getPlayerClass() == PlayerClass.FEMALE_SOLDIER)
							{
								if (talker.getLevel() <= 5)
								{
									htmltext = "32135-kff05.htm";
								}
								else if (talker.getLevel() <= 10)
								{
									htmltext = "32135-kff10.htm";
								}
								else if (talker.getLevel() <= 15)
								{
									htmltext = "32135-kff15.htm";
								}
								else
								{
									htmltext = "32135-kff20.htm";
								}
							}
						}
						else if (talker.getRace() != npc.getRace())
						{
							return npc.getId() + "-003.htm";
						}
						else if ((talker.getLevel() > 20) || (talker.getPlayerClass().level() != 0))
						{
							return npc.getId() + "-002.htm";
						}
						else if (!talker.isMageClass())
						{
							if (talker.getLevel() <= 5)
							{
								htmltext = npc.getId() + SUFFIX_FIGHTER_5_LEVEL;
							}
							else if (talker.getLevel() <= 10)
							{
								htmltext = npc.getId() + SUFFIX_FIGHTER_10_LEVEL;
							}
							else if (talker.getLevel() <= 15)
							{
								htmltext = npc.getId() + SUFFIX_FIGHTER_15_LEVEL;
							}
							else
							{
								htmltext = npc.getId() + SUFFIX_FIGHTER_20_LEVEL;
							}
						}
						else if (talker.getLevel() <= 7)
						{
							htmltext = npc.getId() + SUFFIX_MAGE_7_LEVEL;
						}
						else if (talker.getLevel() <= 14)
						{
							htmltext = npc.getId() + SUFFIX_MAGE_14_LEVEL;
						}
						else
						{
							htmltext = npc.getId() + SUFFIX_MAGE_20_LEVEL;
						}
						break;
					}
					case 2:
					{
						if (talker.getLevel() <= 75)
						{
							if (talker.getLevel() < 6)
							{
								htmltext = "buffs-low-level.htm";
							}
							else if (!talker.isMageClass() && (talker.getPlayerClass().level() < 3))
							{
								npc.setTarget(talker);
								npc.doCast(WIND_WALK_FOR_BEGINNERS.getSkill());
								npc.doCast(WIND_WALK_FOR_BEGINNERS.getSkill());
								npc.doCast(SHIELD_FOR_BEGINNERS.getSkill());
								npc.doCast(ADVENTURERS_MAGIC_BARRIER.getSkill());
								npc.doCast(BLESS_THE_BODY_FOR_BEGINNERS.getSkill());
								npc.doCast(VAMPIRIC_RAGE_FOR_BEGINNERS.getSkill());
								npc.doCast(REGENERATION_FOR_BEGINNERS.getSkill());
								if ((talker.getLevel() >= 6) && (talker.getLevel() <= 39))
								{
									npc.doCast(HASTE_FOR_BEGINNERS.getSkill());
								}
								
								if ((talker.getLevel() >= 40) && (talker.getLevel() <= 75))
								{
									npc.doCast(ADVENTURERS_HASTE.getSkill());
								}
								
								if ((talker.getLevel() >= 16) && (talker.getLevel() <= 34))
								{
									talker.doSimultaneousCast(LIFE_CUBIC_FOR_BEGINNERS.getSkill());
								}
							}
							else if (talker.isMageClass() && (talker.getPlayerClass().level() < 3))
							{
								npc.setTarget(talker);
								npc.doCast(WIND_WALK_FOR_BEGINNERS.getSkill());
								npc.doCast(SHIELD_FOR_BEGINNERS.getSkill());
								npc.doCast(ADVENTURERS_MAGIC_BARRIER.getSkill());
								npc.doCast(BLESS_THE_SOUL_FOR_BEGINNERS.getSkill());
								npc.doCast(ACUMEN_FOR_BEGINNERS.getSkill());
								npc.doCast(CONCENTRATION_FOR_BEGINNERS.getSkill());
								npc.doCast(EMPOWER_FOR_BEGINNERS.getSkill());
								if ((talker.getLevel() >= 16) && (talker.getLevel() <= 34))
								{
									talker.doSimultaneousCast(LIFE_CUBIC_FOR_BEGINNERS.getSkill());
								}
							}
						}
						else
						{
							htmltext = "buffs-big-level.htm";
						}
						break;
					}
					case 3:
					{
						if ((talker.getLevel() <= 39) && (talker.getPlayerClass().level() < 3))
						{
							npc.setTarget(talker);
							npc.doCast(BLESSING_OF_PROTECTION.getSkill());
						}
						else
						{
							htmltext = "pk-protection-002.htm";
						}
						break;
					}
					case 4:
					{
						final Summon summon = talker.getSummon();
						if ((summon != null) && !summon.isPet())
						{
							if ((talker.getLevel() < 6) || (talker.getLevel() > 75))
							{
								htmltext = "buffs-big-level.htm";
							}
							else
							{
								npc.setTarget(talker);
								npc.doCast(WIND_WALK_FOR_BEGINNERS.getSkill());
								npc.doCast(SHIELD_FOR_BEGINNERS.getSkill());
								npc.doCast(ADVENTURERS_MAGIC_BARRIER.getSkill());
								npc.doCast(BLESS_THE_BODY_FOR_BEGINNERS.getSkill());
								npc.doCast(VAMPIRIC_RAGE_FOR_BEGINNERS.getSkill());
								npc.doCast(REGENERATION_FOR_BEGINNERS.getSkill());
								npc.doCast(BLESS_THE_SOUL_FOR_BEGINNERS.getSkill());
								npc.doCast(ACUMEN_FOR_BEGINNERS.getSkill());
								npc.doCast(CONCENTRATION_FOR_BEGINNERS.getSkill());
								npc.doCast(EMPOWER_FOR_BEGINNERS.getSkill());
								if ((talker.getLevel() >= 6) && (talker.getLevel() <= 39))
								{
									npc.doCast(HASTE_FOR_BEGINNERS.getSkill());
								}
								
								if ((talker.getLevel() >= 40) && (talker.getLevel() <= 75))
								{
									npc.doCast(ADVENTURERS_HASTE.getSkill());
								}
							}
						}
						else
						{
							htmltext = "buffs-no-pet.htm";
						}
						break;
					}
				}
				break;
			}
			case -1000:
			{
				switch (reply)
				{
					case 1:
					{
						if (talker.getLevel() > 5)
						{
							if ((talker.getLevel() < 20) && (talker.getPlayerClass().level() == 0))
							{
								if (getOneTimeQuestFlag(talker, 207) == 0)
								{
									giveItems(qs.getPlayer(), APPRENTICE_ADVENTURERS_WEAPON_EXCHANGE_COUPON, FIRST_COUPON_SIZE);
									setOneTimeQuestFlag(talker, 207, 1);
									htmltext = "newbie-guide-002.htm";
									setNRMemoState(qs, GUIDE_MISSION, getNRMemoState(qs, GUIDE_MISSION) + 100);
									showOnScreenMsg(talker, NpcStringId.ACQUISITION_OF_WEAPON_EXCHANGE_COUPON_FOR_BEGINNERS_COMPLETE_N_GO_SPEAK_WITH_THE_NEWBIE_GUIDE, 2, 5000, "");
									
									// Needed for retrieving NewbieGuide quest after playerEnter.
									qs.setState(State.STARTED);
								}
								else
								{
									htmltext = "newbie-guide-004.htm";
								}
							}
							else
							{
								htmltext = "newbie-guide-003.htm";
							}
						}
						else
						{
							htmltext = "newbie-guide-003.htm";
						}
						break;
					}
					case 2:
					{
						if (talker.getPlayerClass().level() == 1)
						{
							if (talker.getLevel() < 40)
							{
								if (getOneTimeQuestFlag(talker, 208) == 0)
								{
									giveItems(qs.getPlayer(), ADVENTURERS_MAGIC_ACCESSORY_EXCHANGE_COUPON, SECOND_COUPON_SIZE);
									setOneTimeQuestFlag(talker, 208, 1);
									htmltext = "newbie-guide-011.htm";
								}
								else
								{
									htmltext = "newbie-guide-013.htm";
								}
							}
							else
							{
								htmltext = "newbie-guide-012.htm";
							}
						}
						else
						{
							htmltext = "newbie-guide-012.htm";
						}
						break;
					}
				}
				break;
				
			}
			case -303:
			{
				switch (reply)
				{
					case 528:
					{
						if (talker.getLevel() > 5)
						{
							if ((talker.getLevel() < 20) && (talker.getPlayerClass().level() == 0))
							{
								MultisellData.getInstance().separateAndSend(WEAPON_MULTISELL, talker, npc, false);
							}
							else
							{
								htmltext = "newbie-guide-005.htm";
							}
						}
						else
						{
							htmltext = "newbie-guide-005.htm";
						}
						break;
					}
					case 529:
					{
						if (talker.getLevel() > 5)
						{
							if ((talker.getLevel() < 40) && (talker.getPlayerClass().level() == 1))
							{
								MultisellData.getInstance().separateAndSend(ACCESORIES_MULTISELL, talker, npc, false);
							}
							else
							{
								htmltext = "newbie-guide-014.htm";
							}
						}
						else
						{
							htmltext = "newbie-guide-014.htm";
						}
						break;
					}
				}
				break;
			}
		}
		
		switch (npc.getId())
		{
			case NEWBIE_GUIDE_HUMAN:
			{
				final String ansGuideHumanCnacelot = eventGuideHumanCnacelot(reply, qs);
				if (!ansGuideHumanCnacelot.isEmpty())
				{
					return ansGuideHumanCnacelot;
				}
				break;
			}
			case NEWBIE_GUIDE_ELF:
			{
				final String ansGuideElfRoios = eventGuideElfRoios(reply, qs);
				if (!ansGuideElfRoios.isEmpty())
				{
					return ansGuideElfRoios;
				}
				break;
			}
			case NEWBIE_GUIDE_DARK_ELF:
			{
				final String ansGuideDelfFrankia = eventGuideDelfFrankia(reply, qs);
				if (!ansGuideDelfFrankia.isEmpty())
				{
					return ansGuideDelfFrankia;
				}
				break;
			}
			case NEWBIE_GUIDE_DWARF:
			{
				final String ansGuideDwarfGullin = eventGuideDwarfGullin(reply, qs);
				if (!ansGuideDwarfGullin.isEmpty())
				{
					return ansGuideDwarfGullin;
				}
				break;
			}
			case NEWBIE_GUIDE_ORC:
			{
				final String ansGuideOrcTanai = eventGuideOrcTanai(reply, qs);
				if (!ansGuideOrcTanai.isEmpty())
				{
					return ansGuideOrcTanai;
				}
				break;
			}
			case NEWBIE_GUIDE_KAMAEL:
			{
				final String ansGuideKrenisk = eventGuideKrenisk(reply, qs);
				if (!ansGuideKrenisk.isEmpty())
				{
					return ansGuideKrenisk;
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	private String teleportRequest(Player talker, Npc npc, int teleportId)
	{
		String htmltext = null;
		if (talker.getLevel() >= 20)
		{
			htmltext = "teleport-big-level.htm";
		}
		else if ((talker.getTransformationId() == 111) || (talker.getTransformationId() == 112) || (talker.getTransformationId() == 124))
		{
			htmltext = "frog-teleport.htm";
		}
		else if ((teleportId < 0) || (teleportId > 5))
		{
			htmltext = npc.getId() + "-teleport.htm";
		}
		else if (TELEPORT_MAP.containsKey(npc.getId()) && (TELEPORT_MAP.get(npc.getId()).size() > teleportId))
		{
			talker.teleToLocation(TELEPORT_MAP.get(npc.getId()).get(teleportId), false);
		}
		
		return htmltext;
	}
	
	private String talkGuide(Player talker, Npc npc, QuestState tutorialQS)
	{
		String hmltext = null;
		final QuestState qs = getQuestState(talker, true);
		if ((tutorialQS.getMemoStateEx(1) < 5) && (getOneTimeQuestFlag(talker, GUIDE_MISSION) == 0))
		{
			if (!talker.isMageClass())
			{
				playSound(qs.getPlayer(), "tutorial_voice_026");
				giveItems(qs.getPlayer(), SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
				giveItems(qs.getPlayer(), SCROLL_RECOVERY_NO_GRADE, 2);
				tutorialQS.setMemoStateEx(1, 5);
				if (talker.getLevel() <= 1)
				{
					addExpAndSp(qs.getPlayer(), 68, 50);
				}
				else
				{
					addExpAndSp(qs.getPlayer(), 0, 50);
				}
			}
			
			if (talker.isMageClass())
			{
				if (talker.getPlayerClass() == PlayerClass.ORC_MAGE)
				{
					playSound(qs.getPlayer(), "tutorial_voice_026");
					giveItems(qs.getPlayer(), SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
				}
				else
				{
					playSound(qs.getPlayer(), "tutorial_voice_027");
					giveItems(qs.getPlayer(), SPIRITSHOT_NO_GRADE_FOR_BEGINNERS, 100);
				}
				
				giveItems(qs.getPlayer(), SCROLL_RECOVERY_NO_GRADE, 2);
				tutorialQS.setMemoStateEx(1, 5);
				if (talker.getLevel() <= 1)
				{
					addExpAndSp(qs.getPlayer(), 68, 50);
				}
				else
				{
					addExpAndSp(qs.getPlayer(), 0, 50);
				}
			}
			
			if (talker.getLevel() < 6)
			{
				if ((getNRMemoState(qs, GUIDE_MISSION) % 10) == 1)
				{
					if (talker.getLevel() >= 5)
					{
						giveAdena(talker, 695, true);
						addExpAndSp(qs.getPlayer(), 3154, 127);
					}
					else if (talker.getLevel() >= 4)
					{
						giveAdena(talker, 1041, true);
						addExpAndSp(qs.getPlayer(), 4870, 195);
					}
					else if (talker.getLevel() >= 3)
					{
						giveAdena(talker, 1186, true);
						addExpAndSp(qs.getPlayer(), 5675, 227);
					}
					else
					{
						giveAdena(talker, 1240, true);
						addExpAndSp(qs.getPlayer(), 5970, 239);
					}
					
					if (!haveNRMemo(qs, GUIDE_MISSION))
					{
						setNRMemo(qs, GUIDE_MISSION);
						setNRMemoState(qs, GUIDE_MISSION, 10);
					}
					else
					{
						setNRMemoState(qs, GUIDE_MISSION, getNRMemoState(qs, GUIDE_MISSION) + 10);
					}
					
					hmltext = "newbie-guide-02.htm";
				}
				else
				{
					switch (npc.getRace())
					{
						case HUMAN:
						{
							addRadar(qs.getPlayer(), -84436, 242793, -3729);
							hmltext = "newbie-guide-01a.htm";
							break;
						}
						case ELF:
						{
							addRadar(qs.getPlayer(), 42978, 49115, 2994);
							hmltext = "newbie-guide-01b.htm";
							break;
						}
						case DARK_ELF:
						{
							addRadar(qs.getPlayer(), 25790, 10844, -3727);
							hmltext = "newbie-guide-01c.htm";
							break;
						}
						case ORC:
						{
							addRadar(qs.getPlayer(), -47360, -113791, -237);
							hmltext = "newbie-guide-01d.htm";
							break;
						}
						case DWARF:
						{
							addRadar(qs.getPlayer(), 112656, -174864, -611);
							hmltext = "newbie-guide-01e.htm";
							break;
						}
						case KAMAEL:
						{
							addRadar(qs.getPlayer(), -119378, 49242, 22);
							hmltext = "newbie-guide-01f.htm";
							break;
						}
					}
					
					if (!haveNRMemo(qs, GUIDE_MISSION))
					{
						setNRMemo(qs, GUIDE_MISSION);
						setNRMemoState(qs, GUIDE_MISSION, 0);
					}
				}
			}
			else if (talker.getLevel() < 10)
			{
				if ((((getNRMemoState(qs, GUIDE_MISSION) % 1000) / 100) == 1) && (((getNRMemoState(qs, GUIDE_MISSION) % 10000) / 100) == 1))
				{
					switch (talker.getRace())
					{
						case HUMAN:
						{
							if (!talker.isMageClass())
							{
								addRadar(qs.getPlayer(), -71384, 258304, -3109);
								hmltext = "newbie-guide-05a.htm";
							}
							else
							{
								addRadar(qs.getPlayer(), -91008, 248016, -3568);
								hmltext = "newbie-guide-05b.htm";
							}
							break;
						}
						case ELF:
						{
							addRadar(qs.getPlayer(), 47595, 51569, -2996);
							hmltext = "newbie-guide-05c.htm";
							break;
						}
						case DARK_ELF:
						{
							if (!talker.isMageClass())
							{
								addRadar(qs.getPlayer(), 10580, 17574, -4554);
								hmltext = "newbie-guide-05d.htm";
							}
							else
							{
								addRadar(qs.getPlayer(), 10775, 14190, -4242);
								hmltext = "newbie-guide-05e.htm";
							}
							break;
						}
						case ORC:
						{
							addRadar(qs.getPlayer(), 46808, -113184, -112);
							hmltext = "newbie-guide-05f.htm";
							break;
						}
						case DWARF:
						{
							addRadar(qs.getPlayer(), 115717, -183488, -1483);
							hmltext = "newbie-guide-05g.htm";
							break;
						}
						case KAMAEL:
						{
							addRadar(qs.getPlayer(), 115717, -183488, -1483);
							hmltext = "newbie-guide-05h.htm";
							break;
						}
					}
					
					if (talker.getLevel() >= 9)
					{
						giveAdena(talker, 5563, true);
						addExpAndSp(qs.getPlayer(), 16851, 711);
					}
					else if (talker.getLevel() >= 8)
					{
						giveAdena(talker, 9290, true);
						addExpAndSp(qs.getPlayer(), 28806, 1207);
					}
					else if (talker.getLevel() >= 7)
					{
						giveAdena(talker, 11567, true);
						addExpAndSp(qs.getPlayer(), 36942, 1541);
					}
					else
					{
						giveAdena(talker, 12928, true);
						addExpAndSp(qs.getPlayer(), 42191, 1753);
					}
					
					if (!haveNRMemo(qs, GUIDE_MISSION))
					{
						setNRMemo(qs, GUIDE_MISSION);
						setNRMemoState(qs, GUIDE_MISSION, 10000);
					}
					else
					{
						setNRMemoState(qs, GUIDE_MISSION, getNRMemoState(qs, GUIDE_MISSION) + 10000);
					}
				}
				else if ((((getNRMemoState(qs, GUIDE_MISSION) % 1000) / 100) == 1) && (((getNRMemoState(qs, GUIDE_MISSION) % 10000) / 100) != 1))
				{
					switch (npc.getRace())
					{
						case HUMAN:
						{
							addRadar(qs.getPlayer(), -82236, 241573, -3728);
							hmltext = "newbie-guide-04a.htm";
							break;
						}
						case ELF:
						{
							addRadar(qs.getPlayer(), 42812, 51138, -2996);
							hmltext = "newbie-guide-04b.htm";
							break;
						}
						case DARK_ELF:
						{
							addRadar(qs.getPlayer(), 7644, 18048, -4377);
							hmltext = "newbie-guide-04c.htm";
							break;
						}
						case ORC:
						{
							addRadar(qs.getPlayer(), -46802, -114011, -112);
							hmltext = "newbie-guide-04d.htm";
							break;
						}
						case DWARF:
						{
							addRadar(qs.getPlayer(), 116103, -178407, -948);
							hmltext = "newbie-guide-04e.htm";
							break;
						}
						case KAMAEL:
						{
							addRadar(qs.getPlayer(), -119378, 49242, 22);
							hmltext = "newbie-guide-04f.htm";
							break;
						}
					}
					
					if (!haveNRMemo(qs, GUIDE_MISSION))
					{
						setNRMemo(qs, GUIDE_MISSION);
						setNRMemoState(qs, GUIDE_MISSION, 0);
					}
				}
				else
				{
					if (!haveNRMemo(qs, GUIDE_MISSION))
					{
						setNRMemo(qs, GUIDE_MISSION);
						setNRMemoState(qs, GUIDE_MISSION, 0);
					}
					
					hmltext = "newbie-guide-03.htm";
				}
			}
			else
			{
				setOneTimeQuestFlag(talker, GUIDE_MISSION, 1);
				if (!haveNRMemo(qs, GUIDE_MISSION))
				{
					setNRMemo(qs, GUIDE_MISSION);
					setNRMemoState(qs, GUIDE_MISSION, 0);
				}
				
				hmltext = "newbie-guide-06.htm";
			}
		}
		else if ((tutorialQS.getMemoStateEx(1) >= 5) && (getOneTimeQuestFlag(talker, GUIDE_MISSION) == 0))
		{
			if (talker.getLevel() < 6)
			{
				if ((getNRMemoState(qs, GUIDE_MISSION) % 10) == 1)
				{
					if (talker.getLevel() >= 5)
					{
						giveAdena(talker, 695, true);
						addExpAndSp(qs.getPlayer(), 3154, 127);
					}
					else if (talker.getLevel() >= 4)
					{
						giveAdena(talker, 1041, true);
						addExpAndSp(qs.getPlayer(), 4870, 195);
					}
					else if (talker.getLevel() >= 3)
					{
						giveAdena(talker, 1186, true);
						addExpAndSp(qs.getPlayer(), 5675, 227);
					}
					else
					{
						giveAdena(talker, 1240, true);
						addExpAndSp(qs.getPlayer(), 5970, 239);
					}
					
					if (!haveNRMemo(qs, GUIDE_MISSION))
					{
						setNRMemo(qs, GUIDE_MISSION);
						setNRMemoState(qs, GUIDE_MISSION, 10);
					}
					else
					{
						setNRMemoState(qs, GUIDE_MISSION, getNRMemoState(qs, GUIDE_MISSION) + 10);
					}
					
					hmltext = "newbie-guide-08.htm";
				}
				else
				{
					switch (npc.getRace())
					{
						case HUMAN:
						{
							addRadar(qs.getPlayer(), -84436, 242793, -3729);
							hmltext = "newbie-guide-07a.htm";
							break;
						}
						case ELF:
						{
							addRadar(qs.getPlayer(), 42978, 49115, 2994);
							hmltext = "newbie-guide-07b.htm";
							break;
						}
						case DARK_ELF:
						{
							addRadar(qs.getPlayer(), 25790, 10844, -3727);
							hmltext = "newbie-guide-07c.htm";
							break;
						}
						case ORC:
						{
							addRadar(qs.getPlayer(), -47360, -113791, -237);
							hmltext = "newbie-guide-07d.htm";
							break;
						}
						case DWARF:
						{
							addRadar(qs.getPlayer(), 112656, -174864, -611);
							hmltext = "newbie-guide-07e.htm";
							break;
						}
						case KAMAEL:
						{
							addRadar(qs.getPlayer(), -119378, 49242, 22);
							hmltext = "newbie-guide-07f.htm";
							break;
						}
					}
					
					if (!haveNRMemo(qs, GUIDE_MISSION))
					{
						setNRMemo(qs, GUIDE_MISSION);
						setNRMemoState(qs, GUIDE_MISSION, 0);
					}
				}
			}
			else if (talker.getLevel() < 10)
			{
				if (((getNRMemoState(qs, GUIDE_MISSION) % 100000) / 10000) == 1)
				{
					hmltext = "newbie-guide-09g.htm";
				}
				else if ((((getNRMemoState(qs, GUIDE_MISSION) % 1000) / 100) == 1) && (((getNRMemoState(qs, GUIDE_MISSION) % 10000) / 1000) == 1) && (((getNRMemoState(qs, GUIDE_MISSION) % 100000) / 10000) != 1))
				{
					switch (talker.getRace())
					{
						case HUMAN:
						{
							if (!talker.isMageClass())
							{
								addRadar(qs.getPlayer(), -71384, 258304, -3109);
								hmltext = "newbie-guide-10a.htm";
							}
							else
							{
								addRadar(qs.getPlayer(), -91008, 248016, -3568);
								hmltext = "newbie-guide-10b.htm";
							}
							break;
						}
						case ELF:
						{
							addRadar(qs.getPlayer(), 47595, 51569, -2996);
							hmltext = "newbie-guide-10c.htm";
							break;
						}
						case DARK_ELF:
						{
							if (!talker.isMageClass())
							{
								addRadar(qs.getPlayer(), 10580, 17574, -4554);
								hmltext = "newbie-guide-10d.htm";
							}
							else
							{
								addRadar(qs.getPlayer(), 10775, 14190, -4242);
								hmltext = "newbie-guide-10e.htm";
							}
							break;
						}
						case ORC:
						{
							addRadar(qs.getPlayer(), -46808, -113184, -112);
							hmltext = "newbie-guide-10f.htm";
							break;
						}
						case DWARF:
						{
							addRadar(qs.getPlayer(), 115717, -183488, -1483);
							hmltext = "newbie-guide-10g.htm";
							break;
						}
						case KAMAEL:
						{
							addRadar(qs.getPlayer(), -118080, 42835, 720);
							hmltext = "newbie-guide-10h.htm";
							break;
						}
					}
					
					if (talker.getLevel() >= 9)
					{
						giveAdena(talker, 5563, true);
						addExpAndSp(qs.getPlayer(), 16851, 711);
					}
					else if (talker.getLevel() >= 8)
					{
						giveAdena(talker, 9290, true);
						addExpAndSp(qs.getPlayer(), 28806, 1207);
					}
					else if (talker.getLevel() >= 7)
					{
						giveAdena(talker, 11567, true);
						addExpAndSp(qs.getPlayer(), 36942, 1541);
					}
					else
					{
						giveAdena(talker, 12928, true);
						addExpAndSp(qs.getPlayer(), 42191, 1753);
					}
					
					if (!haveNRMemo(qs, GUIDE_MISSION))
					{
						setNRMemo(qs, GUIDE_MISSION);
						setNRMemoState(qs, GUIDE_MISSION, 10000);
					}
					else
					{
						setNRMemoState(qs, GUIDE_MISSION, getNRMemoState(qs, GUIDE_MISSION) + 10000);
					}
				}
				else if ((((getNRMemoState(qs, GUIDE_MISSION) % 1000) / 100) == 1) && (((getNRMemoState(qs, GUIDE_MISSION) % 10000) / 1000) != 1))
				{
					switch (npc.getRace())
					{
						case HUMAN:
						{
							addRadar(qs.getPlayer(), -82236, 241573, -3728);
							hmltext = "newbie-guide-09a.htm";
							break;
						}
						case ELF:
						{
							addRadar(qs.getPlayer(), 42812, 51138, -2996);
							hmltext = "newbie-guide-09b.htm";
							break;
						}
						case DARK_ELF:
						{
							addRadar(qs.getPlayer(), 7644, 18048, -4377);
							hmltext = "newbie-guide-09c.htm";
							break;
						}
						case ORC:
						{
							addRadar(qs.getPlayer(), -46802, -114011, -112);
							hmltext = "newbie-guide-09d.htm";
							break;
						}
						case DWARF:
						{
							addRadar(qs.getPlayer(), 116103, -178407, -948);
							hmltext = "newbie-guide-09e.htm";
							break;
						}
						case KAMAEL:
						{
							addRadar(qs.getPlayer(), -119378, 49242, 22);
							hmltext = "newbie-guide-09f.htm";
							break;
						}
					}
					
					if (!haveNRMemo(qs, GUIDE_MISSION))
					{
						setNRMemo(qs, GUIDE_MISSION);
						setNRMemoState(qs, GUIDE_MISSION, 0);
					}
				}
				else
				{
					if (!haveNRMemo(qs, GUIDE_MISSION))
					{
						setNRMemo(qs, GUIDE_MISSION);
						setNRMemoState(qs, GUIDE_MISSION, 0);
					}
					
					hmltext = "newbie-guide-08.htm";
				}
			}
			else if (talker.getLevel() < 15)
			{
				if ((((getNRMemoState(qs, GUIDE_MISSION) % 1000000) / 100000) == 1) && (((getNRMemoState(qs, GUIDE_MISSION) % 10000000) / 1000000) == 1))
				{
					hmltext = "newbie-guide-15.htm";
				}
				else if ((((getNRMemoState(qs, GUIDE_MISSION) % 1000000) / 100000) == 1) && (((getNRMemoState(qs, GUIDE_MISSION) % 10000000) / 1000000) != 1))
				{
					switch (npc.getRace())
					{
						case HUMAN:
						{
							addRadar(qs.getPlayer(), -84057, 242832, -3729);
							hmltext = "newbie-guide-11a.htm";
							break;
						}
						case ELF:
						{
							addRadar(qs.getPlayer(), 45859, 50827, -3058);
							hmltext = "newbie-guide-11b.htm";
							break;
						}
						case DARK_ELF:
						{
							addRadar(qs.getPlayer(), 11258, 14431, -4242);
							hmltext = "newbie-guide-11c.htm";
							break;
						}
						case ORC:
						{
							addRadar(qs.getPlayer(), -45863, -112621, -200);
							hmltext = "newbie-guide-11d.htm";
							break;
						}
						case DWARF:
						{
							addRadar(qs.getPlayer(), 116268, -177524, -914);
							hmltext = "newbie-guide-11e.htm";
							break;
						}
						case KAMAEL:
						{
							addRadar(qs.getPlayer(), -125872, 38208, 1251);
							hmltext = "newbie-guide-11f.htm";
							break;
						}
					}
					
					if (talker.getLevel() >= 14)
					{
						giveAdena(talker, 13002, true);
						addExpAndSp(qs.getPlayer(), 62876, 2891);
					}
					else if (talker.getLevel() >= 13)
					{
						giveAdena(talker, 23468, true);
						addExpAndSp(qs.getPlayer(), 113137, 5161);
					}
					else if (talker.getLevel() >= 12)
					{
						giveAdena(talker, 31752, true);
						addExpAndSp(qs.getPlayer(), 152653, 6914);
					}
					else if (talker.getLevel() >= 11)
					{
						giveAdena(talker, 38180, true);
						addExpAndSp(qs.getPlayer(), 183128, 8242);
					}
					else
					{
						giveAdena(talker, 43054, true);
						addExpAndSp(qs.getPlayer(), 206101, 9227);
					}
					
					if (!haveNRMemo(qs, GUIDE_MISSION))
					{
						setNRMemo(qs, GUIDE_MISSION);
						setNRMemoState(qs, GUIDE_MISSION, 1000000);
					}
					else
					{
						setNRMemoState(qs, GUIDE_MISSION, getNRMemoState(qs, GUIDE_MISSION) + 1000000);
					}
				}
				else if (((getNRMemoState(qs, GUIDE_MISSION) % 1000000) / 100000) != 1)
				{
					switch (talker.getRace())
					{
						case HUMAN:
						{
							if (!talker.isMageClass())
							{
								addRadar(qs.getPlayer(), -71384, 258304, -3109);
								hmltext = "newbie-guide-10a.htm";
							}
							else
							{
								addRadar(qs.getPlayer(), -91008, 248016, -3568);
								hmltext = "newbie-guide-10b.htm";
							}
							break;
						}
						case ELF:
						{
							addRadar(qs.getPlayer(), 47595, 51569, -2996);
							hmltext = "newbie-guide-10c.htm";
							break;
						}
						case DARK_ELF:
						{
							if (!talker.isMageClass())
							{
								addRadar(qs.getPlayer(), 10580, 17574, -4554);
								hmltext = "newbie-guide-10d.htm";
							}
							else
							{
								addRadar(qs.getPlayer(), 10775, 14190, -4242);
								hmltext = "newbie-guide-10e.htm";
							}
							break;
						}
						case ORC:
						{
							addRadar(qs.getPlayer(), -46808, -113184, -112);
							hmltext = "newbie-guide-10f.htm";
							break;
						}
						case DWARF:
						{
							addRadar(qs.getPlayer(), 115717, -183488, -1483);
							hmltext = "newbie-guide-10g.htm";
							break;
						}
						case KAMAEL:
						{
							addRadar(qs.getPlayer(), -118080, 42835, 720);
							hmltext = "newbie-guide-10h.htm";
							break;
						}
					}
					
					if (!haveNRMemo(qs, GUIDE_MISSION))
					{
						setNRMemo(qs, GUIDE_MISSION);
						setNRMemoState(qs, GUIDE_MISSION, 0);
					}
				}
			}
			else if (talker.getLevel() < 18)
			{
				if ((((getNRMemoState(qs, GUIDE_MISSION) % 100000000) / 10000000) == 1) && (((getNRMemoState(qs, GUIDE_MISSION) % 1000000000) / 100000000) == 1))
				{
					setOneTimeQuestFlag(talker, GUIDE_MISSION, 1);
					hmltext = "newbie-guide-13.htm";
				}
				else if ((((getNRMemoState(qs, GUIDE_MISSION) % 100000000) / 10000000) == 1) && (((getNRMemoState(qs, GUIDE_MISSION) % 1000000000) / 100000000) != 1))
				{
					if (talker.getLevel() >= 17)
					{
						giveAdena(talker, 22996, true);
						addExpAndSp(qs.getPlayer(), 113712, 5518);
					}
					else if (talker.getLevel() >= 16)
					{
						giveAdena(talker, 10018, true);
						addExpAndSp(qs.getPlayer(), 208133, 42237);
					}
					else
					{
						giveAdena(talker, 13648, true);
						addExpAndSp(qs.getPlayer(), 285670, 58155);
					}
					
					if (!haveNRMemo(qs, GUIDE_MISSION))
					{
						setNRMemo(qs, GUIDE_MISSION);
						setNRMemoState(qs, GUIDE_MISSION, 100000000);
					}
					else
					{
						setNRMemoState(qs, GUIDE_MISSION, getNRMemoState(qs, GUIDE_MISSION) + 100000000);
					}
					
					setOneTimeQuestFlag(talker, GUIDE_MISSION, 1);
					hmltext = "newbie-guide-12.htm";
				}
				else if (((getNRMemoState(qs, GUIDE_MISSION) % 100000000) / 10000000) != 1)
				{
					switch (npc.getRace())
					{
						case HUMAN:
						{
							addRadar(qs.getPlayer(), -84057, 242832, -3729);
							hmltext = "newbie-guide-11a.htm";
							break;
						}
						case ELF:
						{
							addRadar(qs.getPlayer(), 45859, 50827, -3058);
							hmltext = "newbie-guide-11b.htm";
							break;
						}
						case DARK_ELF:
						{
							addRadar(qs.getPlayer(), 11258, 14431, -4242);
							hmltext = "newbie-guide-11c.htm";
							break;
						}
						case ORC:
						{
							addRadar(qs.getPlayer(), -45863, -112621, -200);
							hmltext = "newbie-guide-11d.htm";
							break;
						}
						case DWARF:
						{
							addRadar(qs.getPlayer(), 116268, -177524, -914);
							hmltext = "newbie-guide-11e.htm";
							break;
						}
						case KAMAEL:
						{
							addRadar(qs.getPlayer(), -125872, 38208, 1251);
							hmltext = "newbie-guide-11f.htm";
							break;
						}
					}
				}
				
				if (!haveNRMemo(qs, GUIDE_MISSION))
				{
					setNRMemo(qs, GUIDE_MISSION);
					setNRMemoState(qs, GUIDE_MISSION, 0);
				}
			}
			else if (talker.getPlayerClass().level() == 1)
			{
				setOneTimeQuestFlag(talker, GUIDE_MISSION, 1);
				if (!haveNRMemo(qs, GUIDE_MISSION))
				{
					setNRMemo(qs, GUIDE_MISSION);
					setNRMemoState(qs, GUIDE_MISSION, 0);
				}
				
				hmltext = "newbie-guide-13.htm";
			}
			else
			{
				setOneTimeQuestFlag(talker, GUIDE_MISSION, 1);
				if (!haveNRMemo(qs, GUIDE_MISSION))
				{
					setNRMemo(qs, GUIDE_MISSION);
					setNRMemoState(qs, GUIDE_MISSION, 0);
				}
				
				hmltext = "newbie-guide-14.htm";
			}
		}
		
		return hmltext;
	}
	
	private String eventGuideHumanCnacelot(int event, QuestState qs)
	{
		switch (event)
		{
			case 10:
			{
				return "30598-04.htm";
			}
			case 11:
			{
				return "30598-04a.htm";
			}
			case 12:
			{
				return "30598-04b.htm";
			}
			case 13:
			{
				return "30598-04c.htm";
			}
			case 14:
			{
				return "30598-04d.htm";
			}
			case 15:
			{
				return "30598-04e.htm";
			}
			case 16:
			{
				return "30598-04f.htm";
			}
			case 17:
			{
				return "30598-04g.htm";
			}
			case 18:
			{
				return "30598-04h.htm";
			}
			case 19:
			{
				return "30598-04i.htm";
			}
			case 31:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -84108, 244604, -3729);
				return "30598-05.htm";
			}
			case 32:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -82236, 241573, -3728);
				return "30598-05.htm";
			}
			case 33:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -82515, 241221, -3728);
				return "30598-05.htm";
			}
			case 34:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -82319, 244709, -3727);
				return "30598-05.htm";
			}
			case 35:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -82659, 244992, -3717);
				return "30598-05.htm";
			}
			case 36:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -86114, 244682, -3727);
				return "30598-05.htm";
			}
			case 37:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -86328, 244448, -3724);
				return "30598-05.htm";
			}
			case 38:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -86322, 241215, -3727);
				return "30598-05.htm";
			}
			case 39:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -85964, 240947, -3727);
				return "30598-05.htm";
			}
			case 40:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -85026, 242689, -3729);
				return "30598-05.htm";
			}
			case 41:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -83789, 240799, -3717);
				return "30598-05.htm";
			}
			case 42:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -84204, 240403, -3717);
				return "30598-05.htm";
			}
			case 43:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -86385, 243267, -3717);
				return "30598-05.htm";
			}
			case 44:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -86733, 242918, -3717);
				return "30598-05.htm";
			}
			case 45:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -84516, 245449, -3714);
				return "30598-05.htm";
			}
			case 46:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -84729, 245001, -3726);
				return "30598-05.htm";
			}
			case 47:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -84965, 245222, -3726);
				return "30598-05.htm";
			}
			case 48:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -84981, 244764, -3726);
				return "30598-05.htm";
			}
			case 49:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -85186, 245001, -3726);
				return "30598-05.htm";
			}
			case 50:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -83326, 242964, -3718);
				return "30598-05.htm";
			}
			case 51:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -83020, 242553, -3718);
				return "30598-05.htm";
			}
			case 52:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -83175, 243065, -3718);
				return "30598-05.htm";
			}
			case 53:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -82809, 242751, -3718);
				return "30598-05.htm";
			}
			case 54:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -81895, 243917, -3721);
				return "30598-05.htm";
			}
			case 55:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -81840, 243534, -3721);
				return "30598-05.htm";
			}
			case 56:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -81512, 243424, -3720);
				return "30598-05.htm";
			}
			case 57:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -84436, 242793, -3729);
				return "30598-05.htm";
			}
			case 58:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -78939, 240305, -3443);
				return "30598-05.htm";
			}
			case 59:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -85301, 244587, -3725);
				return "30598-05.htm";
			}
			case 60:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -83163, 243560, -3728);
				return "30598-05.htm";
			}
			case 61:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -97131, 258946, -3622);
				return "30598-05.htm";
			}
			case 62:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -114685, 222291, -2925);
				return "30598-05.htm";
			}
			case 63:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -84057, 242832, -3729);
				return "30598-05.htm";
			}
			case 64:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -100332, 238019, -3573);
				return "30598-05.htm";
			}
			case 65:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -82041, 242718, -3725);
				return "30598-05.htm";
			}
		}
		
		return "";
	}
	
	private String eventGuideElfRoios(int event, QuestState qs)
	{
		switch (event)
		{
			case 10:
			{
				return "30599-04.htm";
			}
			case 11:
			{
				return "30599-04a.htm";
			}
			case 12:
			{
				return "30599-04b.htm";
			}
			case 13:
			{
				return "30599-04c.htm";
			}
			case 14:
			{
				return "30599-04d.htm";
			}
			case 15:
			{
				return "30599-04e.htm";
			}
			case 16:
			{
				return "30599-04f.htm";
			}
			case 17:
			{
				return "30599-04g.htm";
			}
			case 18:
			{
				return "30599-04h.htm";
			}
			case 31:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 46926, 51511, -2977);
				return "30599-05.htm";
			}
			case 32:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 44995, 51706, -2803);
				return "30599-05.htm";
			}
			case 33:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 45727, 51721, -2803);
				return "30599-05.htm";
			}
			case 34:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 42812, 51138, -2996);
				return "30599-05.htm";
			}
			case 35:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 45487, 46511, -2996);
				return "30599-05.htm";
			}
			case 36:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 47401, 51764, -2996);
				return "30599-05.htm";
			}
			case 37:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 42971, 51372, -2996);
				return "30599-05.htm";
			}
			case 38:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 47595, 51569, -2996);
				return "30599-05.htm";
			}
			case 39:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 45778, 46534, -2996);
				return "30599-05.htm";
			}
			case 40:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 44476, 47153, -2984);
				return "30599-05.htm";
			}
			case 41:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 42700, 50057, -2984);
				return "30599-05.htm";
			}
			case 42:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 42766, 50037, -2984);
				return "30599-05.htm";
			}
			case 43:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 44683, 46952, -2981);
				return "30599-05.htm";
			}
			case 44:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 44667, 46896, -2982);
				return "30599-05.htm";
			}
			case 45:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 45725, 52105, -2795);
				return "30599-05.htm";
			}
			case 46:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 44823, 52414, -2795);
				return "30599-05.htm";
			}
			case 47:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 45000, 52101, -2795);
				return "30599-05.htm";
			}
			case 48:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 45919, 52414, -2795);
				return "30599-05.htm";
			}
			case 49:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 44692, 52261, -2795);
				return "30599-05.htm";
			}
			case 50:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 47780, 49568, -2983);
				return "30599-05.htm";
			}
			case 51:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 47912, 50170, -2983);
				return "30599-05.htm";
			}
			case 52:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 47868, 50167, -2983);
				return "30599-05.htm";
			}
			case 53:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 28928, 74248, -3773);
				return "30599-05.htm";
			}
			case 54:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 43673, 49683, -3046);
				return "30599-05.htm";
			}
			case 55:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 45610, 49008, -3059);
				return "30599-05.htm";
			}
			case 56:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 50592, 54986, -3376);
				return "30599-05.htm";
			}
			case 57:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 42978, 49115, -2994);
				return "30599-05.htm";
			}
			case 58:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 46475, 50495, -3058);
				return "30599-05.htm";
			}
			case 59:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 45859, 50827, -3058);
				return "30599-05.htm";
			}
			case 60:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 51210, 82474, -3283);
				return "30599-05.htm";
			}
			case 61:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 49262, 53607, -3216);
				return "30599-05.htm";
			}
		}
		
		return "";
	}
	
	private String eventGuideDelfFrankia(int event, QuestState qs)
	{
		switch (event)
		{
			case 10:
			{
				return "30600-04.htm";
			}
			case 11:
			{
				return "30600-04a.htm";
			}
			case 12:
			{
				return "30600-04b.htm";
			}
			case 13:
			{
				return "30600-04c.htm";
			}
			case 14:
			{
				return "30600-04d.htm";
			}
			case 15:
			{
				return "30600-04e.htm";
			}
			case 16:
			{
				return "30600-04f.htm";
			}
			case 17:
			{
				return "30600-04g.htm";
			}
			case 18:
			{
				return "30600-04h.htm";
			}
			case 31:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 9670, 15537, -4574);
				return "30600-05.htm";
			}
			case 32:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 15120, 15656, -4376);
				return "30600-05.htm";
			}
			case 33:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 17306, 13592, -3724);
				return "30600-05.htm";
			}
			case 34:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 15272, 16310, -4377);
				return "30600-05.htm";
			}
			case 35:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 6449, 19619, -3694);
				return "30600-05.htm";
			}
			case 36:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -15404, 71131, -3445);
				return "30600-05.htm";
			}
			case 37:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 7496, 17388, -4377);
				return "30600-05.htm";
			}
			case 38:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 17102, 13002, -3743);
				return "30600-05.htm";
			}
			case 39:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 6532, 19903, -3693);
				return "30600-05.htm";
			}
			case 40:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -15648, 71405, -3451);
				return "30600-05.htm";
			}
			case 41:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 7644, 18048, -4377);
				return "30600-05.htm";
			}
			case 42:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -1301, 75883, -3566);
				return "30600-05.htm";
			}
			case 43:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -1152, 76125, -3566);
				return "30600-05.htm";
			}
			case 44:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 10580, 17574, -4554);
				return "30600-05.htm";
			}
			case 45:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 12009, 15704, -4554);
				return "30600-05.htm";
			}
			case 46:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 11951, 15661, -4554);
				return "30600-05.htm";
			}
			case 47:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 10761, 17970, -4554);
				return "30600-05.htm";
			}
			case 48:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 10823, 18013, -4554);
				return "30600-05.htm";
			}
			case 49:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 11283, 14226, -4242);
				return "30600-05.htm";
			}
			case 50:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 10447, 14620, -4242);
				return "30600-05.htm";
			}
			case 51:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 11258, 14431, -4242);
				return "30600-05.htm";
			}
			case 52:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 10344, 14445, -4242);
				return "30600-05.htm";
			}
			case 53:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 10315, 14293, -4242);
				return "30600-05.htm";
			}
			case 54:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 10775, 14190, -4242);
				return "30600-05.htm";
			}
			case 55:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 11235, 14078, -4242);
				return "30600-05.htm";
			}
			case 56:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 11012, 14128, -4242);
				return "30600-05.htm";
			}
			case 57:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 13380, 17430, -4542);
				return "30600-05.htm";
			}
			case 58:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 13464, 17751, -4541);
				return "30600-05.htm";
			}
			case 59:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 13763, 17501, -4542);
				return "30600-05.htm";
			}
			case 60:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -44225, 79721, -3652);
				return "30600-05.htm";
			}
			case 61:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -44015, 79683, -3652);
				return "30600-05.htm";
			}
			case 62:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 25856, 10832, -3724);
				return "30600-05.htm";
			}
			case 63:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 12328, 14947, -4574);
				return "30600-05.htm";
			}
			case 64:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 13081, 18444, -4573);
				return "30600-05.htm";
			}
			case 65:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 12311, 17470, -4574);
				return "30600-05.htm";
			}
		}
		
		return "";
	}
	
	private String eventGuideDwarfGullin(int event, QuestState qs)
	{
		switch (event)
		{
			case 10:
			{
				return "30601-04.htm";
			}
			case 11:
			{
				return "30601-04a.htm";
			}
			case 12:
			{
				return "30601-04b.htm";
			}
			case 13:
			{
				return "30601-04c.htm";
			}
			case 14:
			{
				return "30601-04d.htm";
			}
			case 15:
			{
				return "30601-04e.htm";
			}
			case 16:
			{
				return "30601-04f.htm";
			}
			case 17:
			{
				return "30601-04g.htm";
			}
			case 18:
			{
				return "30601-04h.htm";
			}
			case 31:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 115072, -178176, -906);
				return "30601-05.htm";
			}
			case 32:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 117847, -182339, -1537);
				return "30601-05.htm";
			}
			case 33:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 116617, -184308, -1569);
				return "30601-05.htm";
			}
			case 34:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 117826, -182576, -1537);
				return "30601-05.htm";
			}
			case 35:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 116378, -184308, -1571);
				return "30601-05.htm";
			}
			case 36:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 115183, -176728, -791);
				return "30601-05.htm";
			}
			case 37:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 114969, -176752, -790);
				return "30601-05.htm";
			}
			case 38:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 117366, -178725, -1118);
				return "30601-05.htm";
			}
			case 39:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 117378, -178914, -1120);
				return "30601-05.htm";
			}
			case 40:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 116226, -178529, -948);
				return "30601-05.htm";
			}
			case 41:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 116190, -178441, -948);
				return "30601-05.htm";
			}
			case 42:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 116016, -178615, -948);
				return "30601-05.htm";
			}
			case 43:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 116190, -178615, -948);
				return "30601-05.htm";
			}
			case 44:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 116103, -178407, -948);
				return "30601-05.htm";
			}
			case 45:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 116103, -178653, -948);
				return "30601-05.htm";
			}
			case 46:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 115468, -182446, -1434);
				return "30601-05.htm";
			}
			case 47:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 115315, -182155, -1444);
				return "30601-05.htm";
			}
			case 48:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 115271, -182692, -1445);
				return "30601-05.htm";
			}
			case 49:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 115900, -177316, -915);
				return "30601-05.htm";
			}
			case 50:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 116268, -177524, -914);
				return "30601-05.htm";
			}
			case 51:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 115741, -181645, -1344);
				return "30601-05.htm";
			}
			case 52:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 116192, -181072, -1344);
				return "30601-05.htm";
			}
			case 53:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 115205, -180024, -870);
				return "30601-05.htm";
			}
			case 54:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 114716, -180018, -871);
				return "30601-05.htm";
			}
			case 55:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 114832, -179520, -871);
				return "30601-05.htm";
			}
			case 56:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 115717, -183488, -1483);
				return "30601-05.htm";
			}
			case 57:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 115618, -183265, -1483);
				return "30601-05.htm";
			}
			case 58:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 114348, -178537, -813);
				return "30601-05.htm";
			}
			case 59:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 114990, -177294, -854);
				return "30601-05.htm";
			}
			case 60:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 114426, -178672, -812);
				return "30601-05.htm";
			}
			case 61:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 114409, -178415, -812);
				return "30601-05.htm";
			}
			case 62:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 117061, -181867, -1413);
				return "30601-05.htm";
			}
			case 63:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 116164, -184029, -1507);
				return "30601-05.htm";
			}
			case 64:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 115563, -182923, -1448);
				return "30601-05.htm";
			}
			case 65:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 112656, -174864, -611);
				return "30601-05.htm";
			}
			case 66:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), 116852, -183595, -1566);
				return "30601-05.htm";
			}
		}
		
		return "";
	}
	
	private String eventGuideOrcTanai(int event, QuestState qs)
	{
		switch (event)
		{
			case 10:
			{
				return "30602-04.htm";
			}
			case 11:
			{
				return "30602-04a.htm";
			}
			case 12:
			{
				return "30602-04b.htm";
			}
			case 13:
			{
				return "30602-04c.htm";
			}
			case 14:
			{
				return "30602-04d.htm";
			}
			case 15:
			{
				return "30602-04e.htm";
			}
			case 16:
			{
				return "30602-04f.htm";
			}
			case 17:
			{
				return "30602-04g.htm";
			}
			case 18:
			{
				return "30602-04h.htm";
			}
			case 19:
			{
				return "30602-04i.htm";
			}
			case 31:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -45264, -112512, -235);
				return "30602-05.htm";
			}
			case 32:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -46576, -117311, -242);
				return "30602-05.htm";
			}
			case 33:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -47360, -113791, -237);
				return "30602-05.htm";
			}
			case 34:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -47360, -113424, -235);
				return "30602-05.htm";
			}
			case 35:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -45744, -117165, -236);
				return "30602-05.htm";
			}
			case 36:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -46528, -109968, -250);
				return "30602-05.htm";
			}
			case 37:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -45808, -110055, -255);
				return "30602-05.htm";
			}
			case 38:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -45731, -113844, -237);
				return "30602-05.htm";
			}
			case 39:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -45728, -113360, -237);
				return "30602-05.htm";
			}
			case 40:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -45952, -114784, -199);
				return "30602-05.htm";
			}
			case 41:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -45952, -114496, -199);
				return "30602-05.htm";
			}
			case 42:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -45863, -112621, -200);
				return "30602-05.htm";
			}
			case 43:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -45864, -112540, -199);
				return "30602-05.htm";
			}
			case 44:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -43264, -112532, -220);
				return "30602-05.htm";
			}
			case 45:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -43910, -115518, -194);
				return "30602-05.htm";
			}
			case 46:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -43950, -115457, -194);
				return "30602-05.htm";
			}
			case 47:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -44416, -111486, -222);
				return "30602-05.htm";
			}
			case 48:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -43926, -111794, -222);
				return "30602-05.htm";
			}
			case 49:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -43109, -113770, -221);
				return "30602-05.htm";
			}
			case 50:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -43114, -113404, -221);
				return "30602-05.htm";
			}
			case 51:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -46768, -113610, -3);
				return "30602-05.htm";
			}
			case 52:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -46802, -114011, -112);
				return "30602-05.htm";
			}
			case 53:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -46247, -113866, -21);
				return "30602-05.htm";
			}
			case 54:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -46808, -113184, -112);
				return "30602-05.htm";
			}
			case 55:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -45328, -114736, -237);
				return "30602-05.htm";
			}
			case 56:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -44624, -111873, -238);
				return "30602-05.htm";
			}
		}
		
		return "";
	}
	
	private String eventGuideKrenisk(int event, QuestState qs)
	{
		switch (event)
		{
			case 10:
			{
				return "32135-04.htm";
			}
			case 11:
			{
				return "32135-04a.htm";
			}
			case 12:
			{
				return "32135-04b.htm";
			}
			case 13:
			{
				return "32135-04c.htm";
			}
			case 14:
			{
				return "32135-04d.htm";
			}
			case 15:
			{
				return "32135-04e.htm";
			}
			case 16:
			{
				return "32135-04f.htm";
			}
			case 17:
			{
				return "32135-04g.htm";
			}
			case 18:
			{
				return "32135-04h.htm";
			}
			case 19:
			{
				return "32135-04i.htm";
			}
			case 20:
			{
				return "32135-04j.htm";
			}
			case 21:
			{
				return "32135-04k.htm";
			}
			case 22:
			{
				return "32135-04l.htm";
			}
			case 31:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -116879, 46591, 380);
				return "32135-05.htm";
			}
			case 32:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -119378, 49242, 22);
				return "32135-05.htm";
			}
			case 33:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -119774, 49245, 22);
				return "32135-05.htm";
			}
			case 34:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -119830, 51860, -787);
				return "32135-05.htm";
			}
			case 35:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -119362, 51862, -780);
				return "32135-05.htm";
			}
			case 36:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -112872, 46850, 68);
				return "32135-05.htm";
			}
			case 37:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -112352, 47392, 68);
				return "32135-05.htm";
			}
			case 38:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -110544, 49040, -1124);
				return "32135-05.htm";
			}
			case 39:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -110536, 45162, -1132);
				return "32135-05.htm";
			}
			case 40:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -115888, 43568, 524);
				return "32135-05.htm";
			}
			case 41:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -115486, 43567, 525);
				return "32135-05.htm";
			}
			case 42:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -116920, 47792, 464);
				return "32135-05.htm";
			}
			case 43:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -116749, 48077, 462);
				return "32135-05.htm";
			}
			case 44:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -117153, 48075, 463);
				return "32135-05.htm";
			}
			case 45:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -119104, 43280, 559);
				return "32135-05.htm";
			}
			case 46:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -119104, 43152, 559);
				return "32135-05.htm";
			}
			case 47:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -117056, 43168, 559);
				return "32135-05.htm";
			}
			case 48:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -117060, 43296, 559);
				return "32135-05.htm";
			}
			case 49:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -118192, 42384, 838);
				return "32135-05.htm";
			}
			case 50:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -117968, 42384, 838);
				return "32135-05.htm";
			}
			case 51:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -118132, 42788, 723);
				return "32135-05.htm";
			}
			case 52:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -118028, 42788, 720);
				return "32135-05.htm";
			}
			case 53:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -114802, 44821, 524);
				return "32135-05.htm";
			}
			case 54:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -114975, 44658, 524);
				return "32135-05.htm";
			}
			case 55:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -114801, 45031, 525);
				return "32135-05.htm";
			}
			case 56:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -120432, 45296, 416);
				return "32135-05.htm";
			}
			case 57:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -120706, 45079, 419);
				return "32135-05.htm";
			}
			case 58:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -120356, 45293, 416);
				return "32135-05.htm";
			}
			case 59:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -120604, 44960, 423);
				return "32135-05.htm";
			}
			case 60:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -120294, 46013, 384);
				return "32135-05.htm";
			}
			case 61:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -120157, 45813, 355);
				return "32135-05.htm";
			}
			case 62:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -120158, 46221, 354);
				return "32135-05.htm";
			}
			case 63:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -120400, 46921, 415);
				return "32135-05.htm";
			}
			case 64:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -120407, 46755, 423);
				return "32135-05.htm";
			}
			case 65:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -120442, 47125, 422);
				return "32135-05.htm";
			}
			case 66:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -118720, 48062, 473);
				return "32135-05.htm";
			}
			case 67:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -118918, 47956, 474);
				return "32135-05.htm";
			}
			case 68:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -118527, 47955, 473);
				return "32135-05.htm";
			}
			case 69:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -117605, 48079, 472);
				return "32135-05.htm";
			}
			case 70:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -117824, 48080, 476);
				return "32135-05.htm";
			}
			case 71:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -118030, 47930, 465);
				return "32135-05.htm";
			}
			case 72:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -119221, 46981, 380);
				return "32135-05.htm";
			}
			case 73:
			{
				clearRadar(qs.getPlayer());
				addRadar(qs.getPlayer(), -118080, 42835, 720);
				return "32135-05.htm";
			}
		}
		
		return "";
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final QuestState qs = player.getQuestState(Q00255_Tutorial.class.getSimpleName());
		if (qs != null)
		{
			switch (npc.getId())
			{
				case ADVENTURERS_GUIDE:
				{
					return "32327.htm";
				}
				case NEWBIE_GUIDE_GLUDIN:
				{
					return "31076.htm";
				}
				case NEWBIE_GUIDE_GLUDIO:
				{
					return "31077.htm";
				}
			}
			
			return talkGuide(player, npc, qs);
		}
		
		return super.onFirstTalk(npc, player);
	}
	
	public static void main(String[] args)
	{
		new NewbieGuide();
	}
}
