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
package quests.Q00255_Tutorial;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureAttacked;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLevelChanged;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerSit;
import org.l2jmobius.gameserver.model.events.holders.actor.player.inventory.OnPlayerItemPickup;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.TutorialCloseHtml;
import org.l2jmobius.gameserver.network.serverpackets.TutorialEnableClientEvent;
import org.l2jmobius.gameserver.network.serverpackets.TutorialShowHtml;
import org.l2jmobius.gameserver.network.serverpackets.TutorialShowQuestionMark;

/**
 * Tutorial (255)
 * @author Zealar, Mobius
 */
public class Q00255_Tutorial extends Quest
{
	// NPCs
	private static final int ROIEN = 30008;
	private static final int NEWBIE_HELPER_HUMAN_FIGHTER = 30009;
	private static final int GALLINT = 30017;
	private static final int NEWBIE_HELPER_HUMAN_MAGE = 30019;
	private static final int MITRAELL = 30129;
	private static final int NEWBIE_HELPER_DARK_ELF = 30131;
	private static final int NERUPA = 30370;
	private static final int NEWBIE_HELPER_ELF = 30400;
	private static final int LAFERON = 30528;
	private static final int NEWBIE_HELPER_DWARF = 30530;
	private static final int VULKUS = 30573;
	private static final int NEWBIE_HELPER_ORC = 30575;
	private static final int PERWAN = 32133;
	private static final int NEWBIE_HELPER_KAMAEL = 32134;
	
	// Monster
	private static final int TUTORIAL_GREMLIN = 18342;
	
	// Items
	private static final int SOULSHOT_NO_GRADE_FOR_BEGINNERS = 5789;
	private static final int SPIRITSHOT_NO_GRADE_FOR_BEGINNERS = 5790;
	private static final int BLUE_GEMSTONE = 6353;
	private static final int TUTORIAL_GUIDE = 5588;
	
	// Quest items
	private static final int RECOMMENDATION_1 = 1067;
	private static final int RECOMMENDATION_2 = 1068;
	private static final int LEAF_OF_THE_MOTHER_TREE = 1069;
	private static final int BLOOD_OF_MITRAELL = 1070;
	private static final int LICENSE_OF_MINER = 1498;
	private static final int VOUCHER_OF_FLAME = 1496;
	private static final int DIPLOMA = 9881;
	
	// Territory wars
	private static final int TW_GLUDIO = 81;
	private static final int TW_DION = 82;
	private static final int TW_GIRAN = 83;
	private static final int TW_OREN = 84;
	private static final int TW_ADEN = 85;
	private static final int TW_HEINE = 86;
	private static final int TW_GODDARD = 87;
	private static final int TW_RUNE = 88;
	private static final int TW_SCHUTTGART = 89;
	
	// Connected quests
	private static final int Q10276_MUTATED_KANEUS_GLUDIO = 10276;
	private static final int Q10277_MUTATED_KANEUS_DION = 10277;
	private static final int Q10278_MUTATED_KANEUS_HEINE = 10278;
	private static final int Q10279_MUTATED_KANEUS_OREN = 10279;
	private static final int Q10280_MUTATED_KANEUS_SCHUTTGART = 10280;
	private static final int Q10281_MUTATED_KANEUS_RUNE = 10281;
	private static final int Q192_SEVEN_SIGNS_SERIES_OF_DOUBT = 192;
	private static final int Q10292_SEVEN_SIGNS_GIRL_OF_DOUBT = 10292;
	private static final int Q234_FATES_WHISPER = 234;
	private static final int Q128_PAILAKA_SONG_OF_ICE_AND_FIRE = 128;
	private static final int Q129_PAILAKA_DEVILS_LEGACY = 129;
	private static final int Q144_PAIRAKA_WOUNDED_DRAGON = 144;
	
	private static final int Q729_PROTECT_THE_TERRITORY_CATAPULT = 729;
	private static final int Q730_PROTECT_THE_SUPPLIES_SAFE = 730;
	private static final int Q731_PROTECT_THE_MILITARY_ASSOCIATION_LEADER = 731;
	private static final int Q732_PROTECT_THE_RELIGIOUS_ASSOCIATION_LEADER = 732;
	private static final int Q733_PROTECT_THE_ECONOMIC_ASSOCIATION_LEADER = 733;
	
	private static final int Q201_TUTORIAL_HUMAN_FIGHTER = 201;
	private static final int Q202_TUTORIAL_HUMAN_MAGE = 202;
	private static final int Q203_TUTORIAL_ELF = 203;
	private static final int Q204_TUTORIAL_DARK_ELF = 204;
	private static final int Q205_TUTORIAL_ORC = 205;
	private static final int Q206_TUTORIAL_DWARF = 206;
	
	private static final int Q717_FOR_THE_SAKE_OF_THE_TERRITORY_GLUDIO = 717;
	private static final int Q718_FOR_THE_SAKE_OF_THE_TERRITORY_DION = 718;
	private static final int Q719_FOR_THE_SAKE_OF_THE_TERRITORY_GIRAN = 719;
	private static final int Q720_FOR_THE_SAKE_OF_THE_TERRITORY_OREN = 720;
	private static final int Q721_FOR_THE_SAKE_OF_THE_TERRITORY_ADEN = 721;
	private static final int Q722_FOR_THE_SAKE_OF_THE_TERRITORY_INNADRIL = 722;
	private static final int Q723_FOR_THE_SAKE_OF_THE_TERRITORY_GODDARD = 723;
	private static final int Q724_FOR_THE_SAKE_OF_THE_TERRITORY_RUNE = 724;
	private static final int Q725_FOR_THE_SAKE_OF_THE_TERRITORY_SCHUTTGART = 725;
	private static final int Q728_TERRITORY_WAR = 728;
	
	public Q00255_Tutorial()
	{
		super(255);
		if (!PlayerConfig.DISABLE_TUTORIAL)
		{
			setOnEnterWorld(true);
			addStartNpc(ROIEN, NEWBIE_HELPER_HUMAN_FIGHTER, GALLINT, NEWBIE_HELPER_HUMAN_MAGE, MITRAELL, NEWBIE_HELPER_DARK_ELF, NERUPA, NEWBIE_HELPER_ELF, LAFERON, NEWBIE_HELPER_DWARF, VULKUS, NEWBIE_HELPER_ORC, PERWAN, NEWBIE_HELPER_KAMAEL);
			addFirstTalkId(ROIEN, NEWBIE_HELPER_HUMAN_FIGHTER, GALLINT, NEWBIE_HELPER_HUMAN_MAGE, MITRAELL, NEWBIE_HELPER_DARK_ELF, NERUPA, NEWBIE_HELPER_ELF, LAFERON, NEWBIE_HELPER_DWARF, VULKUS, NEWBIE_HELPER_ORC, PERWAN, NEWBIE_HELPER_KAMAEL);
			addTalkId(ROIEN, NEWBIE_HELPER_HUMAN_FIGHTER, GALLINT, NEWBIE_HELPER_HUMAN_MAGE, MITRAELL, NEWBIE_HELPER_DARK_ELF, NERUPA, NEWBIE_HELPER_ELF, LAFERON, NEWBIE_HELPER_DWARF, VULKUS, NEWBIE_HELPER_ORC, PERWAN, NEWBIE_HELPER_KAMAEL);
			addKillId(TUTORIAL_GREMLIN);
		}
	}
	
	@Override
	public void onEnterWorld(Player player)
	{
		userConnected(player);
		player.addListener(new ConsumerEventListener(player, EventType.ON_PLAYER_LEVEL_CHANGED, (OnPlayerLevelChanged event) ->
		{
			levelUp(event.getPlayer(), event.getNewLevel());
		}, player));
	}
	
	private void enableTutorialEvent(QuestState qs, int eventStatus)
	{
		final Player player = qs.getPlayer();
		if (((eventStatus & (1048576 | 2097152)) != 0))
		{
			if (player.getLevel() < 6)
			{
				player.addListener(new ConsumerEventListener(player, EventType.ON_PLAYER_ITEM_PICKUP, (OnPlayerItemPickup event) ->
				{
					if ((event.getItem().getId() == BLUE_GEMSTONE) && ((qs.getMemoState() & 1048576) != 0))
					{
						tutorialEvent(event.getPlayer(), 1048576);
					}
					else if ((event.getItem().getId() == 57) && ((qs.getMemoState() & 2097152) != 0))
					{
						tutorialEvent(event.getPlayer(), 2097152);
					}
				}, player));
			}
		}
		else if (player.hasListener(EventType.ON_PLAYER_ITEM_PICKUP))
		{
			player.removeListenerIf(EventType.ON_PLAYER_ITEM_PICKUP, listener -> listener.getOwner() == player);
		}
		
		if ((eventStatus & 8388608) != 0)
		{
			if (!player.hasListener(EventType.ON_PLAYER_SIT))
			{
				player.addListener(new ConsumerEventListener(player, EventType.ON_PLAYER_SIT, (OnPlayerSit event) ->
				{
					tutorialEvent(player, 8388608);
				}, player));
			}
		}
		else if (player.hasListener(EventType.ON_PLAYER_SIT))
		{
			player.removeListenerIf(EventType.ON_PLAYER_SIT, listener -> listener.getOwner() == player);
		}
		
		if ((eventStatus & 256) != 0)
		{
			if (player.getLevel() < 6)
			{
				player.addListener(new ConsumerEventListener(player, EventType.ON_CREATURE_ATTACKED, (OnCreatureAttacked event) ->
				{
					final Player pp = event.getTarget().asPlayer();
					if ((pp != null) && (pp.getCurrentHp() <= (pp.getStat().getMaxHp() * 0.3)))
					{
						tutorialEvent(pp, 256);
					}
				}, player));
			}
		}
		else
		{
			player.removeListenerIf(EventType.ON_CREATURE_ATTACKED, listener -> listener.getOwner() == player);
		}
		
		player.sendPacket(new TutorialEnableClientEvent(eventStatus));
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player talker)
	{
		final QuestState qs = getQuestState(talker, true);
		switch (npc.getId())
		{
			case ROIEN:
			{
				talkRoien(npc, talker, qs);
				break;
			}
			case NEWBIE_HELPER_HUMAN_FIGHTER:
			{
				talkCarl(npc, talker, qs);
				break;
			}
			case GALLINT:
			{
				talkGallin(npc, talker, qs);
				break;
			}
			case NEWBIE_HELPER_HUMAN_MAGE:
			{
				talkDoff(npc, talker, qs);
				break;
			}
			case MITRAELL:
			{
				talkJundin(npc, talker, qs);
				break;
			}
			case NEWBIE_HELPER_DARK_ELF:
			{
				talkPoeny(npc, talker, qs);
				break;
			}
			case NERUPA:
			{
				talkNerupa(npc, talker, qs);
				break;
			}
			case NEWBIE_HELPER_ELF:
			{
				talkMotherTemp(npc, talker, qs);
				break;
			}
			case LAFERON:
			{
				talkForemanLaferon(npc, talker, qs);
				break;
			}
			case NEWBIE_HELPER_DWARF:
			{
				talkMinerMai(npc, talker, qs);
				break;
			}
			case VULKUS:
			{
				talkGuardianVullkus(npc, talker, qs);
				break;
			}
			case NEWBIE_HELPER_ORC:
			{
				talkShelaPriestess(npc, talker, qs);
				break;
			}
			case PERWAN:
			{
				talkSubelderPerwan(npc, talker, qs);
				break;
			}
			case NEWBIE_HELPER_KAMAEL:
			{
				talkHelperKrenisk(npc, talker, qs);
				break;
			}
		}
		
		return "";
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player talker)
	{
		if (!StringUtil.isNumeric(event))
		{
			if (PlayerConfig.DISABLE_TUTORIAL)
			{
				return null;
			}
			
			if (event.startsWith("CE:"))
			{
				tutorialEvent(talker, Integer.parseInt(event.substring(3)));
			}
			else if (event.startsWith("TL:"))
			{
				int pass = Integer.parseInt(event.substring(event.contains("_close_") ? 18 : 5));
				if (pass < 302)
				{
					pass = -pass;
				}
				
				tutorialEvent(talker, pass);
			}
			else if (event.startsWith("TC:"))
			{
				selectFromMenu(talker, Integer.parseInt(event.substring(3)));
			}
			else if (event.startsWith("QM:"))
			{
				questionMarkClicked(talker, Integer.parseInt(event.substring(3)));
			}
			
			return null;
		}
		
		final int eventId = Integer.parseInt(event);
		if (eventId > 1000000)
		{
			fireEvent(eventId, talker);
			return super.onEvent(event, npc, talker);
		}
		
		if (talker.isDead())
		{
			return super.onEvent(event, npc, talker);
		}
		
		final QuestState qs = getQuestState(talker, true);
		switch (npc.getId())
		{
			case NEWBIE_HELPER_HUMAN_FIGHTER:
			{
				switch (qs.getMemoStateEx(1))
				{
					case 0:
					{
						playTutorialVoice(talker, "tutorial_voice_009a");
						qs.setMemoStateEx(1, 1);
						break;
					}
					case 3:
					{
						playTutorialVoice(talker, "tutorial_voice_010a");
						break;
					}
				}
				break;
			}
			case NEWBIE_HELPER_HUMAN_MAGE:
			{
				switch (qs.getMemoStateEx(1))
				{
					case 0:
					{
						playTutorialVoice(talker, "tutorial_voice_009b");
						qs.setMemoStateEx(1, 1);
						break;
					}
					case 3:
					{
						playTutorialVoice(talker, "tutorial_voice_010b");
						break;
					}
				}
				break;
			}
			case NEWBIE_HELPER_DARK_ELF:
			{
				switch (qs.getMemoStateEx(1))
				{
					case 0:
					{
						if (!talker.isMageClass())
						{
							playTutorialVoice(talker, "tutorial_voice_009a");
						}
						else
						{
							playTutorialVoice(talker, "tutorial_voice_009b");
						}
						
						qs.setMemoStateEx(1, 1);
						break;
					}
					case 3:
					{
						playTutorialVoice(talker, "tutorial_voice_010d");
						break;
					}
				}
				break;
			}
			case NEWBIE_HELPER_ELF:
			{
				switch (qs.getMemoStateEx(1))
				{
					case 0:
					{
						if (!talker.isMageClass())
						{
							playTutorialVoice(talker, "tutorial_voice_009a");
						}
						else
						{
							playTutorialVoice(talker, "tutorial_voice_009b");
						}
						
						qs.setMemoStateEx(1, 1);
						break;
					}
					case 3:
					{
						playTutorialVoice(talker, "tutorial_voice_010c");
						break;
					}
				}
				break;
			}
			case NEWBIE_HELPER_DWARF:
			{
				switch (qs.getMemoStateEx(1))
				{
					case 0:
					{
						playTutorialVoice(talker, "tutorial_voice_009a");
						qs.setMemoStateEx(1, 1);
						break;
					}
					case 3:
					{
						playTutorialVoice(talker, "tutorial_voice_010f");
						break;
					}
				}
				break;
			}
			case NEWBIE_HELPER_ORC:
			{
				switch (qs.getMemoStateEx(1))
				{
					case 0:
					{
						if (!talker.isMageClass())
						{
							playTutorialVoice(talker, "tutorial_voice_009a");
						}
						else
						{
							playTutorialVoice(talker, "tutorial_voice_009c");
						}
						
						qs.setMemoStateEx(1, 1);
						break;
					}
					case 3:
					{
						playTutorialVoice(talker, "tutorial_voice_010e");
						break;
					}
				}
				break;
			}
			case NEWBIE_HELPER_KAMAEL:
			{
				switch (qs.getMemoStateEx(1))
				{
					case 0:
					{
						playTutorialVoice(talker, "tutorial_voice_009a");
						qs.setMemoStateEx(1, 1);
						break;
					}
					case 3:
					{
						playTutorialVoice(talker, "tutorial_voice_010g");
						break;
					}
				}
				break;
			}
			case ROIEN:
			{
				if (eventId == ROIEN)
				{
					if (qs.getMemoStateEx(1) >= 4)
					{
						showQuestionMark(talker, 7);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
						playTutorialVoice(talker, "tutorial_voice_025");
					}
					break;
				}
				
				eventRoien(eventId, talker, npc, qs);
				break;
			}
			case GALLINT:
			{
				eventGallin(eventId, talker, npc, qs);
				break;
			}
			case MITRAELL:
			{
				if (eventId == MITRAELL)
				{
					if (qs.getMemoStateEx(1) >= 4)
					{
						showQuestionMark(talker, 7);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
						playTutorialVoice(talker, "tutorial_voice_025");
					}
					break;
				}
				
				eventJundin(eventId, talker, npc, qs);
				break;
			}
			case NERUPA:
			{
				if (eventId == NERUPA)
				{
					if (qs.getMemoStateEx(1) >= 4)
					{
						showQuestionMark(talker, 7);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
						playTutorialVoice(talker, "tutorial_voice_025");
					}
					break;
				}
				
				eventNerupa(eventId, talker, npc, qs);
				break;
			}
			case LAFERON:
			{
				if (eventId == LAFERON)
				{
					if (qs.getMemoStateEx(1) >= 4)
					{
						showQuestionMark(talker, 7);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
						playTutorialVoice(talker, "tutorial_voice_025");
					}
					break;
				}
				
				eventForemanLaferon(eventId, talker, npc, qs);
				break;
			}
			case VULKUS:
			{
				if (eventId == VULKUS)
				{
					if (qs.getMemoStateEx(1) >= 4)
					{
						showQuestionMark(talker, 7);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
						playTutorialVoice(talker, "tutorial_voice_025");
					}
					break;
				}
				
				eventGuardianVullkus(eventId, talker, npc, qs);
				break;
			}
			case PERWAN:
			{
				if (eventId == PERWAN)
				{
					if (qs.getMemoStateEx(1) >= 4)
					{
						showQuestionMark(talker, 7);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
						playTutorialVoice(talker, "tutorial_voice_025");
					}
					break;
				}
				
				eventSubelderPerwan(eventId, talker, npc, qs);
				break;
			}
		}
		
		return "";
	}
	
	private void fireEvent(int timer_id, Player talker)
	{
		if ((talker == null) || talker.isDead() || !talker.isPlayer() || (timer_id <= 1000000))
		{
			return;
		}
		
		final QuestState qs = getQuestState(talker, true);
		switch (qs.getMemoStateEx(1))
		{
			case -2:
			{
				switch (talker.getPlayerClass())
				{
					case FIGHTER:
					{
						playTutorialVoice(talker, "tutorial_voice_001a");
						showTutorialHTML(talker, "tutorial-human-fighter-001.html");
						break;
					}
					case MAGE:
					{
						playTutorialVoice(talker, "tutorial_voice_001b");
						showTutorialHTML(talker, "tutorial-human-mage-001.html");
						break;
					}
					case ELVEN_FIGHTER:
					{
						playTutorialVoice(talker, "tutorial_voice_001c");
						showTutorialHTML(talker, "tutorial-elven-fighter-001.html");
						break;
					}
					case ELVEN_MAGE:
					{
						playTutorialVoice(talker, "tutorial_voice_001d");
						showTutorialHTML(talker, "tutorial-elven-mage-001.html");
						break;
					}
					case DARK_FIGHTER:
					{
						playTutorialVoice(talker, "tutorial_voice_001e");
						showTutorialHTML(talker, "tutorial-delf-fighter-001.html");
						break;
					}
					case DARK_MAGE:
					{
						playTutorialVoice(talker, "tutorial_voice_001f");
						showTutorialHTML(talker, "tutorial-delf-mage-001.html");
						break;
					}
					case ORC_FIGHTER:
					{
						playTutorialVoice(talker, "tutorial_voice_001g");
						showTutorialHTML(talker, "tutorial-orc-fighter-001.html");
						break;
					}
					case ORC_MAGE:
					{
						playTutorialVoice(talker, "tutorial_voice_001h");
						showTutorialHTML(talker, "tutorial-orc-mage-001.html");
						break;
					}
					case DWARVEN_FIGHTER:
					{
						playTutorialVoice(talker, "tutorial_voice_001i");
						showTutorialHTML(talker, "tutorial-dwarven-fighter-001.html");
						break;
					}
					case MALE_SOLDIER:
					case FEMALE_SOLDIER:
					{
						playTutorialVoice(talker, "tutorial_voice_001k");
						showTutorialHTML(talker, "tutorial-kamael-001.html");
						break;
					}
				}
				
				if (!hasQuestItems(talker, TUTORIAL_GUIDE))
				{
					giveItems(talker, TUTORIAL_GUIDE, 1);
				}
				
				startQuestTimer((talker.getObjectId() + 1000000) + "", 30000, null, qs.getPlayer());
				qs.setMemoStateEx(1, -3);
				break;
			}
			case -3:
			{
				playTutorialVoice(talker, "tutorial_voice_002");
				break;
			}
			case -4:
			{
				playTutorialVoice(talker, "tutorial_voice_008");
				qs.setMemoStateEx(1, -5);
				break;
			}
		}
	}
	
	private void tutorialEvent(Player talker, int event_id)
	{
		final QuestState qs = talker.getQuestState(Q00255_Tutorial.class.getSimpleName());
		if (qs == null)
		{
			return;
		}
		
		if (event_id == 0)
		{
			closeTutorialHtml(talker);
			return;
		}
		
		int memoState = qs.getMemoState();
		int memoFlag = memoState & 2147483632;
		
		if (event_id < 0)
		{
			switch (Math.abs(event_id))
			{
				case 1:
				{
					closeTutorialHtml(talker);
					playTutorialVoice(talker, "tutorial_voice_006");
					showQuestionMark(talker, 1);
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					startQuestTimer((talker.getObjectId() + 1000000) + "", 30000, null, qs.getPlayer());
					if (qs.getMemoStateEx(1) <= 0)
					{
						qs.setMemoStateEx(1, -4);
					}
					break;
				}
				case 2:
				{
					playTutorialVoice(talker, "tutorial_voice_003");
					showTutorialHTML(talker, "tutorial-02.html");
					enableTutorialEvent(qs, memoFlag | 1);
					if (qs.getMemoStateEx(1) <= 0)
					{
						qs.setMemoStateEx(1, -5);
					}
					break;
				}
				case 3:
				{
					showTutorialHTML(talker, "tutorial-03.html");
					enableTutorialEvent(qs, memoFlag | 2);
					break;
				}
				case 4:
				{
					showTutorialHTML(talker, "tutorial-04.html");
					enableTutorialEvent(qs, memoFlag | 4);
					break;
				}
				case 5:
				{
					showTutorialHTML(talker, "tutorial-05.html");
					enableTutorialEvent(qs, memoFlag | 8);
					break;
				}
				case 6:
				{
					showTutorialHTML(talker, "tutorial-06.html");
					enableTutorialEvent(qs, memoFlag | 16);
					break;
				}
				case 7:
				{
					showTutorialHTML(talker, "tutorial-100.html");
					enableTutorialEvent(qs, memoFlag);
					break;
				}
				case 8:
				{
					showTutorialHTML(talker, "tutorial-101.html");
					enableTutorialEvent(qs, memoFlag);
					break;
				}
				case 9:
				{
					showTutorialHTML(talker, "tutorial-102.html");
					enableTutorialEvent(qs, memoFlag);
					break;
				}
				case 10:
				{
					showTutorialHTML(talker, "tutorial-103.html");
					enableTutorialEvent(qs, memoFlag);
					break;
				}
				case 11:
				{
					showTutorialHTML(talker, "tutorial-104.html");
					enableTutorialEvent(qs, memoFlag);
					break;
				}
				case 12:
				{
					closeTutorialHtml(talker);
					break;
				}
			}
			return;
		}
		
		switch (event_id)
		{
			case 1:
			{
				if (talker.getLevel() < 6)
				{
					playTutorialVoice(talker, "tutorial_voice_004");
					showTutorialHTML(talker, "tutorial-03.html");
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					enableTutorialEvent(qs, memoFlag | 2);
				}
				break;
			}
			case 2:
			{
				if (talker.getLevel() < 6)
				{
					playTutorialVoice(talker, "tutorial_voice_005");
					showTutorialHTML(talker, "tutorial-05.html");
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					enableTutorialEvent(qs, memoFlag | 8);
				}
				break;
			}
			case 8:
			{
				if (talker.getLevel() < 6)
				{
					showTutorialHTML(talker, "tutorial-human-fighter-007.html");
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					switch (talker.getPlayerClass())
					{
						case FIGHTER:
						{
							addRadar(talker, -71424, 258336, -3109);
							break;
						}
						case MAGE:
						{
							addRadar(talker, -91036, 248044, -3568);
							break;
						}
						case ELVEN_FIGHTER:
						case ELVEN_MAGE:
						{
							addRadar(talker, 46112, 41200, -3504);
							break;
						}
						case DARK_FIGHTER:
						case DARK_MAGE:
						{
							addRadar(talker, 28384, 11056, -4233);
							break;
						}
						case ORC_FIGHTER:
						case ORC_MAGE:
						{
							addRadar(talker, -56736, -113680, -672);
							break;
						}
						case DWARVEN_FIGHTER:
						{
							addRadar(talker, 108567, -173994, -406);
							break;
						}
						case MALE_SOLDIER:
						case FEMALE_SOLDIER:
						{
							addRadar(talker, -125872, 38016, 1251);
							break;
						}
					}
					
					playTutorialVoice(talker, "tutorial_voice_007");
					qs.setMemoState(memoFlag | 2);
					if (qs.getMemoStateEx(1) <= 0)
					{
						qs.setMemoStateEx(1, -5);
					}
				}
				break;
			}
			case 256:
			{
				if (talker.getLevel() < 6)
				{
					playTutorialVoice(talker, "tutorial_voice_017");
					showQuestionMark(talker, 10);
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					qs.setMemoState(memoState & ~256);
					enableTutorialEvent(qs, (memoFlag & ~256) | 8388608);
				}
				break;
			}
			case 512:
			{
				showQuestionMark(talker, 8);
				playTutorialVoice(talker, "tutorial_voice_016");
				playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
				qs.setMemoState(memoState & ~512);
				break;
			}
			case 1024:
			{
				qs.setMemoState(memoState & ~1024);
				switch (talker.getPlayerClass())
				{
					case FIGHTER:
					{
						addRadar(talker, -83020, 242553, -3718);
						break;
					}
					case ELVEN_FIGHTER:
					{
						addRadar(talker, 45061, 52468, -2796);
						break;
					}
					case DARK_FIGHTER:
					{
						addRadar(talker, 10447, 14620, -4242);
						break;
					}
					case ORC_FIGHTER:
					{
						addRadar(talker, -46389, -113905, -21);
						break;
					}
					case DWARVEN_FIGHTER:
					{
						addRadar(talker, 115271, -182692, -1445);
						break;
					}
					case MALE_SOLDIER:
					case FEMALE_SOLDIER:
					{
						addRadar(talker, -118132, 42788, 723);
						break;
					}
				}
				
				if (!talker.isMageClass())
				{
					playTutorialVoice(talker, "tutorial_voice_014");
					showQuestionMark(talker, 9);
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
				}
				
				enableTutorialEvent(qs, memoFlag | 134217728);
				qs.setMemoState(memoState & ~1024);
				break;
			}
			case 134217728:
			{
				showQuestionMark(talker, 24);
				playTutorialVoice(talker, "tutorial_voice_020");
				playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
				enableTutorialEvent(qs, memoFlag & ~134217728);
				qs.setMemoState(memoState & ~134217728);
				enableTutorialEvent(qs, memoFlag | 2048);
				break;
			}
			case 2048:
			{
				if (talker.isMageClass())
				{
					playTutorialVoice(talker, "tutorial_voice_019");
					showQuestionMark(talker, 11);
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					switch (talker.getPlayerClass())
					{
						case MAGE:
						{
							addRadar(talker, -84981, 244764, -3726);
							break;
						}
						case ELVEN_MAGE:
						{
							addRadar(talker, 45701, 52459, -2796);
							break;
						}
						case DARK_MAGE:
						{
							addRadar(talker, 45701, 52459, -2796);
							break;
						}
						case ORC_MAGE:
						{
							addRadar(talker, -46225, -113312, -21);
							break;
						}
					}
					
					qs.setMemoState(memoState & ~2048);
				}
				
				enableTutorialEvent(qs, memoFlag | 268435456);
				break;
			}
			case 268435456:
			{
				if (talker.getPlayerClass() == PlayerClass.FIGHTER)
				{
					playTutorialVoice(talker, "tutorial_voice_021");
					showQuestionMark(talker, 25);
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					qs.setMemoState(memoState & ~268435456);
				}
				
				enableTutorialEvent(qs, memoFlag | 536870912);
				break;
			}
			case 536870912:
			{
				switch (talker.getPlayerClass())
				{
					case DWARVEN_FIGHTER:
					case MAGE:
					case ELVEN_FIGHTER:
					case ELVEN_MAGE:
					case DARK_MAGE:
					case DARK_FIGHTER:
					case MALE_SOLDIER:
					case FEMALE_SOLDIER:
					{
						playTutorialVoice(talker, "tutorial_voice_021");
						showQuestionMark(talker, 25);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
						qs.setMemoState(memoState & ~536870912);
						break;
					}
					default:
					{
						playTutorialVoice(talker, "tutorial_voice_030");
						showQuestionMark(talker, 27);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
						qs.setMemoState(memoState & ~536870912);
						break;
					}
				}
				
				enableTutorialEvent(qs, memoFlag | 1073741824);
				break;
			}
			case 1073741824:
			{
				switch (talker.getPlayerClass())
				{
					case ORC_FIGHTER:
					case ORC_MAGE:
					{
						playTutorialVoice(talker, "tutorial_voice_021");
						showQuestionMark(talker, 25);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
						qs.setMemoState(memoState & ~1073741824);
						break;
					}
				}
				
				enableTutorialEvent(qs, memoFlag | 67108864);
				break;
			}
			case 67108864:
			{
				showQuestionMark(talker, 17);
				playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
				qs.setMemoState(memoState & ~67108864);
				enableTutorialEvent(qs, memoFlag | 4096);
				break;
			}
			case 4096:
			{
				showQuestionMark(talker, 13);
				playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
				qs.setMemoState(memoState & ~4096);
				enableTutorialEvent(qs, memoFlag | 16777216);
				break;
			}
			case 16777216:
			{
				if (talker.getPlayerClass().getRace() != Race.KAMAEL)
				{
					playTutorialVoice(talker, "tutorial_voice_023");
					showQuestionMark(talker, 15);
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					qs.setMemoState(memoState & ~16777216);
				}
				
				enableTutorialEvent(qs, memoFlag | 32);
				break;
			}
			case 16384:
			{
				if ((talker.getPlayerClass().getRace() == Race.KAMAEL) && (talker.getPlayerClass().level() == 1))
				{
					playTutorialVoice(talker, "tutorial_voice_028");
					showQuestionMark(talker, 15);
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					qs.setMemoState(memoState & ~16384);
				}
				
				enableTutorialEvent(qs, memoFlag | 64);
				break;
			}
			case 33554432:
			{
				if (getOneTimeQuestFlag(talker, Q234_FATES_WHISPER) == 0)
				{
					playTutorialVoice(talker, "tutorial_voice_024");
					showQuestionMark(talker, 16);
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					qs.setMemoState(memoState & ~33554432);
				}
				
				enableTutorialEvent(qs, memoFlag | 32768);
				break;
			}
			case 32768:
			{
				if (getOneTimeQuestFlag(talker, Q234_FATES_WHISPER) == 1)
				{
					showQuestionMark(talker, 29);
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					qs.setMemoState(memoState & ~32768);
				}
				break;
			}
			case 32:
			{
				if (getOneTimeQuestFlag(talker, Q128_PAILAKA_SONG_OF_ICE_AND_FIRE) == 0)
				{
					showQuestionMark(talker, 30);
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					qs.setMemoState(memoState & ~32);
				}
				
				enableTutorialEvent(qs, memoFlag | 16384);
				break;
			}
			case 64:
			{
				if (getOneTimeQuestFlag(talker, Q129_PAILAKA_DEVILS_LEGACY) == 0)
				{
					showQuestionMark(talker, 31);
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					qs.setMemoState(memoState & ~64);
				}
				
				enableTutorialEvent(qs, memoFlag | 128);
				break;
			}
			case 128:
			{
				if (getOneTimeQuestFlag(talker, Q144_PAIRAKA_WOUNDED_DRAGON) == 0)
				{
					showQuestionMark(talker, 32);
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					qs.setMemoState(memoState & ~128);
				}
				
				enableTutorialEvent(qs, memoFlag | 33554432);
				break;
			}
			case 2097152:
			{
				if (talker.getLevel() < 6)
				{
					showQuestionMark(talker, 23);
					playTutorialVoice(talker, "tutorial_voice_012");
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					qs.setMemoState(memoState & ~2097152);
				}
				break;
			}
			case 1048576:
			{
				if (talker.getLevel() < 6)
				{
					showQuestionMark(talker, 5);
					playTutorialVoice(talker, "tutorial_voice_013");
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					qs.setMemoState(memoState & ~1048576);
				}
				break;
			}
			case 8388608:
			{
				if (talker.getLevel() < 6)
				{
					playTutorialVoice(talker, "tutorial_voice_018");
					showTutorialHTML(talker, "tutorial-21z.html");
					qs.setMemoState(memoState & ~8388608);
					enableTutorialEvent(qs, (memoFlag & ~8388608));
				}
				break;
			}
		}
	}
	
	private void levelUp(Player player, int level)
	{
		switch (level)
		{
			case 5:
			{
				tutorialEvent(player, 1024);
				break;
			}
			case 6:
			{
				tutorialEvent(player, 134217728);
				break;
			}
			case 7:
			{
				tutorialEvent(player, 2048);
				break;
			}
			case 9:
			{
				tutorialEvent(player, 268435456);
				break;
			}
			case 10:
			{
				tutorialEvent(player, 536870912);
				break;
			}
			case 12:
			{
				tutorialEvent(player, 1073741824);
				break;
			}
			case 15:
			{
				tutorialEvent(player, 67108864);
				break;
			}
			case 18:
			{
				tutorialEvent(player, 4096);
				if (!haveMemo(player, Q10276_MUTATED_KANEUS_GLUDIO) || (getOneTimeQuestFlag(player, Q10276_MUTATED_KANEUS_GLUDIO) == 0))
				{
					showTutorialHTML(player, "tw-gludio.html");
					playSound(player, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					addRadar(player, -13900, 123822, -3112);
				}
				break;
			}
			case 28:
			{
				if (!haveMemo(player, Q10277_MUTATED_KANEUS_DION) || (getOneTimeQuestFlag(player, Q10277_MUTATED_KANEUS_DION) == 0))
				{
					showTutorialHTML(player, "tw-dion.html");
					playSound(player, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					addRadar(player, 18199, 146081, -3080);
				}
				break;
			}
			case 35:
			{
				tutorialEvent(player, 16777216);
				break;
			}
			case 36:
			{
				tutorialEvent(player, 32);
				break;
			}
			case 38:
			{
				if (!haveMemo(player, Q10278_MUTATED_KANEUS_HEINE) || (getOneTimeQuestFlag(player, Q10278_MUTATED_KANEUS_HEINE) == 0))
				{
					showTutorialHTML(player, "tw-heine.html");
					playSound(player, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					addRadar(player, 108384, 221563, -3592);
				}
				break;
			}
			case 39:
			{
				if (player.getRace() == Race.KAMAEL)
				{
					tutorialEvent(player, 16384);
				}
				break;
			}
			case 48:
			{
				if (!haveMemo(player, Q10279_MUTATED_KANEUS_OREN) || (getOneTimeQuestFlag(player, Q10279_MUTATED_KANEUS_OREN) == 0))
				{
					showTutorialHTML(player, "tw-oren.html");
					playSound(player, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					addRadar(player, 81023, 56456, -1552);
				}
				break;
			}
			case 58:
			{
				if (!haveMemo(player, Q10280_MUTATED_KANEUS_SCHUTTGART) || (getOneTimeQuestFlag(player, Q10280_MUTATED_KANEUS_SCHUTTGART) == 0))
				{
					showTutorialHTML(player, "tw-schuttgart.html");
					playSound(player, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					addRadar(player, 85868, -142164, -1342);
				}
				break;
			}
			case 61:
			{
				tutorialEvent(player, 64);
				break;
			}
			case 68:
			{
				if (!haveMemo(player, Q10281_MUTATED_KANEUS_RUNE) || (getOneTimeQuestFlag(player, Q10281_MUTATED_KANEUS_RUNE) == 0))
				{
					showTutorialHTML(player, "tw-rune.html");
					playSound(player, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					addRadar(player, 42596, -47988, -800);
				}
				break;
			}
			case 73:
			{
				tutorialEvent(player, 128);
				break;
			}
			case 79:
			{
				if (!haveMemo(player, Q192_SEVEN_SIGNS_SERIES_OF_DOUBT) || (getOneTimeQuestFlag(player, Q192_SEVEN_SIGNS_SERIES_OF_DOUBT) == 0))
				{
					showTutorialHTML(player, "tutorial-ss-79.html");
					playSound(player, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					addRadar(player, 81655, 54736, -1509);
				}
				break;
			}
			case 81:
			{
				if (!haveMemo(player, Q10292_SEVEN_SIGNS_GIRL_OF_DOUBT) || (getOneTimeQuestFlag(player, Q10292_SEVEN_SIGNS_GIRL_OF_DOUBT) == 0))
				{
					showTutorialHTML(player, "tutorial-ss-81.html");
					playSound(player, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					addRadar(player, 146995, 23755, -1984);
				}
				break;
			}
		}
	}
	
	private void selectFromMenu(Player talker, int reply)
	{
		switch (reply)
		{
			case 1:
			{
				showTutorialHTML(talker, "tutorial-22g.html");
				break;
			}
			case 2:
			{
				showTutorialHTML(talker, "tutorial-22w.html");
				break;
			}
			case 3:
			{
				showTutorialHTML(talker, "tutorial-22ap.html");
				break;
			}
			case 4:
			{
				showTutorialHTML(talker, "tutorial-22ad.html");
				break;
			}
			case 5:
			{
				showTutorialHTML(talker, "tutorial-22bt.html");
				break;
			}
			case 6:
			{
				showTutorialHTML(talker, "tutorial-22bh.html");
				break;
			}
			case 7:
			{
				showTutorialHTML(talker, "tutorial-22cs.html");
				break;
			}
			case 8:
			{
				showTutorialHTML(talker, "tutorial-22cn.html");
				break;
			}
			case 9:
			{
				showTutorialHTML(talker, "tutorial-22cw.html");
				break;
			}
			case 10:
			{
				showTutorialHTML(talker, "tutorial-22db.html");
				break;
			}
			case 11:
			{
				showTutorialHTML(talker, "tutorial-22dp.html");
				break;
			}
			case 12:
			{
				showTutorialHTML(talker, "tutorial-22et.html");
				break;
			}
			case 13:
			{
				showTutorialHTML(talker, "tutorial-22es.html");
				break;
			}
			case 14:
			{
				showTutorialHTML(talker, "tutorial-22fp.html");
				break;
			}
			case 15:
			{
				showTutorialHTML(talker, "tutorial-22fs.html");
				break;
			}
			case 16:
			{
				showTutorialHTML(talker, "tutorial-22gs.html");
				break;
			}
			case 17:
			{
				showTutorialHTML(talker, "tutorial-22ge.html");
				break;
			}
			case 18:
			{
				showTutorialHTML(talker, "tutorial-22ko.html");
				break;
			}
			case 19:
			{
				showTutorialHTML(talker, "tutorial-22kw.html");
				break;
			}
			case 20:
			{
				showTutorialHTML(talker, "tutorial-22ns.html");
				break;
			}
			case 21:
			{
				showTutorialHTML(talker, "tutorial-22nb.html");
				break;
			}
			case 22:
			{
				showTutorialHTML(talker, "tutorial-22oa.html");
				break;
			}
			case 23:
			{
				showTutorialHTML(talker, "tutorial-22op.html");
				break;
			}
			case 24:
			{
				showTutorialHTML(talker, "tutorial-22ps.html");
				break;
			}
			case 25:
			{
				showTutorialHTML(talker, "tutorial-22pp.html");
				break;
			}
			case 26:
			{
				switch (talker.getPlayerClass())
				{
					case WARRIOR:
					{
						showTutorialHTML(talker, "tutorial-22.html");
						break;
					}
					case KNIGHT:
					{
						showTutorialHTML(talker, "tutorial-22a.html");
						break;
					}
					case ROGUE:
					{
						showTutorialHTML(talker, "tutorial-22b.html");
						break;
					}
					case WIZARD:
					{
						showTutorialHTML(talker, "tutorial-22c.html");
						break;
					}
					case CLERIC:
					{
						showTutorialHTML(talker, "tutorial-22d.html");
						break;
					}
					case ELVEN_KNIGHT:
					{
						showTutorialHTML(talker, "tutorial-22e.html");
						break;
					}
					case ELVEN_SCOUT:
					{
						showTutorialHTML(talker, "tutorial-22f.html");
						break;
					}
					case ELVEN_WIZARD:
					{
						showTutorialHTML(talker, "tutorial-22g.html");
						break;
					}
					case ORACLE:
					{
						showTutorialHTML(talker, "tutorial-22h.html");
						break;
					}
					case ORC_RAIDER:
					{
						showTutorialHTML(talker, "tutorial-22i.html");
						break;
					}
					case ORC_MONK:
					{
						showTutorialHTML(talker, "tutorial-22j.html");
						break;
					}
					case ORC_SHAMAN:
					{
						showTutorialHTML(talker, "tutorial-22k.html");
						break;
					}
					case SCAVENGER:
					{
						showTutorialHTML(talker, "tutorial-22l.html");
						break;
					}
					case ARTISAN:
					{
						showTutorialHTML(talker, "tutorial-22m.html");
						break;
					}
					case PALUS_KNIGHT:
					{
						showTutorialHTML(talker, "tutorial-22n.html");
						break;
					}
					case ASSASSIN:
					{
						showTutorialHTML(talker, "tutorial-22o.html");
						break;
					}
					case DARK_WIZARD:
					{
						showTutorialHTML(talker, "tutorial-22p.html");
						break;
					}
					case SHILLIEN_ORACLE:
					{
						showTutorialHTML(talker, "tutorial-22q.html");
						break;
					}
					default:
					{
						showTutorialHTML(talker, "tutorial-22qe.html");
						break;
					}
				}
				break;
			}
			case 27:
			{
				showTutorialHTML(talker, "tutorial-29.html");
				break;
			}
			case 28:
			{
				showTutorialHTML(talker, "tutorial-28.html");
				break;
			}
			case 29:
			{
				showTutorialHTML(talker, "tutorial-07a.html");
				break;
			}
			case 30:
			{
				showTutorialHTML(talker, "tutorial-07b.html");
				break;
			}
			case 31:
			{
				switch (talker.getPlayerClass())
				{
					case TROOPER:
					{
						showTutorialHTML(talker, "tutorial-28a.html");
						break;
					}
					case WARDER:
					{
						showTutorialHTML(talker, "tutorial-28b.html");
						break;
					}
				}
				break;
			}
			case 32:
			{
				showTutorialHTML(talker, "tutorial-22qa.html");
				break;
			}
			case 33:
			{
				switch (talker.getPlayerClass())
				{
					case TROOPER:
					{
						showTutorialHTML(talker, "tutorial-22qb.html");
						break;
					}
					case WARDER:
					{
						showTutorialHTML(talker, "tutorial-22qc.html");
						break;
					}
				}
				break;
			}
			case 34:
			{
				showTutorialHTML(talker, "tutorial-22qd.html");
				break;
			}
		}
	}
	
	private void questionMarkClicked(Player talker, int question_id)
	{
		final QuestState qs = talker.getQuestState(this.getClass().getSimpleName());
		
		final int memoFlag = qs.getMemoState() & 2147483392;
		switch (question_id)
		{
			case 1:
			{
				playTutorialVoice(talker, "tutorial_voice_007");
				if (qs.getMemoStateEx(1) <= 0)
				{
					qs.setMemoStateEx(1, -5);
				}
				
				switch (talker.getPlayerClass())
				{
					case FIGHTER:
					{
						showTutorialHTML(talker, "tutorial-human-fighter-007.html");
						addRadar(talker, -71424, 258336, -3109);
						break;
					}
					case MAGE:
					{
						showTutorialHTML(talker, "tutorial-human-fighter-007.html");
						addRadar(talker, -91036, 248044, -3568);
						break;
					}
					case ELVEN_FIGHTER:
					case ELVEN_MAGE:
					{
						showTutorialHTML(talker, "tutorial-human-fighter-007.html");
						addRadar(talker, -91036, 248044, -3568);
						break;
					}
					case DARK_FIGHTER:
					case DARK_MAGE:
					{
						showTutorialHTML(talker, "tutorial-human-fighter-007.html");
						addRadar(talker, 28384, 11056, -4233);
						break;
					}
					case ORC_FIGHTER:
					case ORC_MAGE:
					{
						showTutorialHTML(talker, "tutorial-human-fighter-007.html");
						addRadar(talker, -56736, -113680, -672);
						break;
					}
					case DWARVEN_FIGHTER:
					{
						showTutorialHTML(talker, "tutorial-human-fighter-007.html");
						addRadar(talker, 108567, -173994, -406);
						break;
					}
					case MALE_SOLDIER:
					case FEMALE_SOLDIER:
					{
						showTutorialHTML(talker, "tutorial-human-fighter-007.html");
						addRadar(talker, -125872, 38016, 1251);
						break;
					}
				}
				
				qs.setMemoState(memoFlag | 2);
				break;
			}
			case 2:
			{
				switch (talker.getPlayerClass())
				{
					case FIGHTER:
					{
						showTutorialHTML(talker, "tutorial-human-fighter-008.html");
						break;
					}
					case MAGE:
					{
						showTutorialHTML(talker, "tutorial-human-mage-008.html");
						break;
					}
					case ELVEN_FIGHTER:
					case ELVEN_MAGE:
					{
						showTutorialHTML(talker, "tutorial-elf-008.html");
						break;
					}
					case DARK_FIGHTER:
					case DARK_MAGE:
					{
						showTutorialHTML(talker, "tutorial-delf-008.html");
						break;
					}
					case ORC_FIGHTER:
					case ORC_MAGE:
					{
						showTutorialHTML(talker, "tutorial-orc-008.html");
						break;
					}
					case DWARVEN_FIGHTER:
					{
						showTutorialHTML(talker, "tutorial-dwarven-fighter-008.html");
						break;
					}
					case MALE_SOLDIER:
					case FEMALE_SOLDIER:
					{
						showTutorialHTML(talker, "tutorial-kamael-008.html");
						break;
					}
				}
				
				qs.setMemoState(memoFlag | 2);
				break;
			}
			case 3:
			{
				showTutorialHTML(talker, "tutorial-09.html");
				enableTutorialEvent(qs, memoFlag | 1048576);
				qs.setMemoState(qs.getMemoState() | 1048576); // TODO find better way!
				break;
			}
			case 4:
			{
				showTutorialHTML(talker, "tutorial-10.html");
				break;
			}
			case 5:
			{
				switch (talker.getPlayerClass())
				{
					case FIGHTER:
					{
						addRadar(talker, -71424, 258336, -3109);
						break;
					}
					case MAGE:
					{
						addRadar(talker, -91036, 248044, -3568);
						break;
					}
					case ELVEN_FIGHTER:
					case ELVEN_MAGE:
					{
						addRadar(talker, 46112, 41200, -3504);
						break;
					}
					case DARK_FIGHTER:
					case DARK_MAGE:
					{
						addRadar(talker, 28384, 11056, -4233);
						break;
					}
					case ORC_FIGHTER:
					case ORC_MAGE:
					{
						addRadar(talker, -56736, -113680, -672);
						break;
					}
					case DWARVEN_FIGHTER:
					{
						addRadar(talker, 108567, -173994, -406);
						break;
					}
					case MALE_SOLDIER:
					case FEMALE_SOLDIER:
					{
						addRadar(talker, -125872, 38016, 1251);
						break;
					}
				}
				
				showTutorialHTML(talker, "tutorial-11.html");
				break;
			}
			case 7:
			{
				showTutorialHTML(talker, "tutorial-15.html");
				qs.setMemoState(memoFlag | 5);
				break;
			}
			case 8:
			{
				showTutorialHTML(talker, "tutorial-18.html");
				break;
			}
			case 9:
			{
				if (!talker.isMageClass())
				{
					switch (talker.getRace())
					{
						case HUMAN:
						case ELF:
						case DARK_ELF:
						{
							showTutorialHTML(talker, "tutorial-fighter-017.html");
							break;
						}
						case DWARF:
						{
							showTutorialHTML(talker, "tutorial-fighter-dwarf-017.html");
							break;
						}
						case ORC:
						{
							showTutorialHTML(talker, "tutorial-fighter-orc-017.html");
							break;
						}
						case KAMAEL:
						{
							showTutorialHTML(talker, "tutorial-kamael-017.html");
							break;
						}
					}
				}
				break;
			}
			case 10:
			{
				showTutorialHTML(talker, "tutorial-19.html");
				break;
			}
			case 11:
			{
				switch (talker.getRace())
				{
					case HUMAN:
					{
						showTutorialHTML(talker, "tutorial-mage-020.html");
						break;
					}
					case ELF:
					case DARK_ELF:
					{
						showTutorialHTML(talker, "tutorial-mage-elf-020.html");
						break;
					}
					case ORC:
					{
						showTutorialHTML(talker, "tutorial-mage-orc-020.html");
						break;
					}
				}
				break;
			}
			case 12:
			{
				showTutorialHTML(talker, "tutorial-15.html");
				break;
			}
			case 13:
			{
				switch (talker.getPlayerClass())
				{
					case FIGHTER:
					{
						showTutorialHTML(talker, "tutorial-21.html");
						break;
					}
					case MAGE:
					{
						showTutorialHTML(talker, "tutorial-21a.html");
						break;
					}
					case ELVEN_FIGHTER:
					{
						showTutorialHTML(talker, "tutorial-21b.html");
						break;
					}
					case ELVEN_MAGE:
					{
						showTutorialHTML(talker, "tutorial-21c.html");
						break;
					}
					case ORC_FIGHTER:
					{
						showTutorialHTML(talker, "tutorial-21d.html");
						break;
					}
					case ORC_MAGE:
					{
						showTutorialHTML(talker, "tutorial-21e.html");
						break;
					}
					case DWARVEN_FIGHTER:
					{
						showTutorialHTML(talker, "tutorial-21f.html");
						break;
					}
					case DARK_FIGHTER:
					{
						showTutorialHTML(talker, "tutorial-21g.html");
						break;
					}
					case DARK_MAGE:
					{
						showTutorialHTML(talker, "tutorial-21h.html");
						break;
					}
					case MALE_SOLDIER:
					{
						showTutorialHTML(talker, "tutorial-21i.html");
						break;
					}
					case FEMALE_SOLDIER:
					{
						showTutorialHTML(talker, "tutorial-21j.html");
						break;
					}
				}
				break;
			}
			case 15:
			{
				if (talker.getRace() != Race.KAMAEL)
				{
					showTutorialHTML(talker, "tutorial-28.html");
				}
				else if (talker.getPlayerClass() == PlayerClass.TROOPER)
				{
					showTutorialHTML(talker, "tutorial-28a.html");
				}
				else if (talker.getPlayerClass() == PlayerClass.WARDER)
				{
					showTutorialHTML(talker, "tutorial-28b.html");
				}
				break;
			}
			case 16:
			{
				showTutorialHTML(talker, "tutorial-30.html");
				break;
			}
			case 17:
			{
				showTutorialHTML(talker, "tutorial-27.html");
				break;
			}
			case 19:
			{
				showTutorialHTML(talker, "tutorial-07.html");
				break;
			}
			case 20:
			{
				showTutorialHTML(talker, "tutorial-14.html");
				break;
			}
			case 21:
			{
				showTutorialHTML(talker, "tutorial-newbie-001.html");
				break;
			}
			case 22:
			{
				showTutorialHTML(talker, "tutorial-14.html");
				break;
			}
			case 23:
			{
				showTutorialHTML(talker, "tutorial-24.html");
				break;
			}
			case 24:
			{
				switch (talker.getRace())
				{
					case HUMAN:
					{
						showTutorialHTML(talker, "tutorial-newbie-003a.html");
						break;
					}
					case ELF:
					{
						showTutorialHTML(talker, "tutorial-newbie-003b.html");
						break;
					}
					case DARK_ELF:
					{
						showTutorialHTML(talker, "tutorial-newbie-003c.html");
						break;
					}
					case ORC:
					{
						showTutorialHTML(talker, "tutorial-newbie-003d.html");
						break;
					}
					case DWARF:
					{
						showTutorialHTML(talker, "tutorial-newbie-003e.html");
						break;
					}
					case KAMAEL:
					{
						showTutorialHTML(talker, "tutorial-newbie-003f.html");
						break;
					}
				}
				break;
			}
			case 25:
			{
				switch (talker.getPlayerClass())
				{
					case FIGHTER:
					{
						showTutorialHTML(talker, "tutorial-newbie-002a.html");
						break;
					}
					case MAGE:
					{
						showTutorialHTML(talker, "tutorial-newbie-002b.html");
						break;
					}
					case ELVEN_FIGHTER:
					case ELVEN_MAGE:
					{
						showTutorialHTML(talker, "tutorial-newbie-002c.html");
						break;
					}
					case DARK_MAGE:
					{
						showTutorialHTML(talker, "tutorial-newbie-002d.html");
						break;
					}
					case DARK_FIGHTER:
					{
						showTutorialHTML(talker, "tutorial-newbie-002e.html");
						break;
					}
					case DWARVEN_FIGHTER:
					{
						showTutorialHTML(talker, "tutorial-newbie-002g.html");
						break;
					}
					case ORC_FIGHTER:
					case ORC_MAGE:
					{
						showTutorialHTML(talker, "tutorial-newbie-002f.html");
						break;
					}
					case MALE_SOLDIER:
					case FEMALE_SOLDIER:
					{
						showTutorialHTML(talker, "tutorial-newbie-002i.html");
						break;
					}
				}
				break;
			}
			case 26:
			{
				if (!talker.isMageClass() || (talker.getPlayerClass() == PlayerClass.ORC_MAGE))
				{
					showTutorialHTML(talker, "tutorial-newbie-004a.html");
				}
				else
				{
					showTutorialHTML(talker, "tutorial-newbie-004b.html");
				}
				break;
			}
			case 27:
			{
				switch (talker.getPlayerClass())
				{
					case FIGHTER:
					case ORC_MAGE:
					case ORC_FIGHTER:
					{
						showTutorialHTML(talker, "tutorial-newbie-002h.html");
						break;
					}
				}
				break;
			}
			case 28:
			{
				showTutorialHTML(talker, "tutorial-31.html");
				break;
			}
			case 29:
			{
				showTutorialHTML(talker, "tutorial-32.html");
				break;
			}
			case 30:
			{
				showTutorialHTML(talker, "tutorial-33.html");
				break;
			}
			case 31:
			{
				showTutorialHTML(talker, "tutorial-34.html");
				break;
			}
			case 32:
			{
				showTutorialHTML(talker, "tutorial-35.html");
				break;
			}
			case 33:
			{
				switch (talker.getLevel())
				{
					case 18:
					{
						showTutorialHTML(talker, "tw-gludio.html");
						break;
					}
					case 28:
					{
						showTutorialHTML(talker, "tw-dion.html");
						break;
					}
					case 38:
					{
						showTutorialHTML(talker, "tw-heine.html");
						break;
					}
					case 48:
					{
						showTutorialHTML(talker, "tw-oren.html");
						break;
					}
					case 58:
					{
						showTutorialHTML(talker, "tw-shuttgart.html");
						break;
					}
					case 68:
					{
						showTutorialHTML(talker, "tw-rune.html");
						break;
					}
				}
				break;
			}
			case 34:
			{
				if (talker.getLevel() == 79)
				{
					showTutorialHTML(talker, "tutorial-ss-79.html");
				}
				break;
			}
		}
	}
	
	private void userConnected(Player talker)
	{
		final QuestState qs = getQuestState(talker, true);
		if (qs == null)
		{
			return;
		}
		
		if (!qs.isStarted())
		{
			qs.setState(State.STARTED);
		}
		
		if (talker.getLevel() < 6)
		{
			if (getOneTimeQuestFlag(talker, 255) != 0)
			{
				return;
			}
			
			int memoState = qs.getMemoState();
			int memoFlag;
			if (memoState == -1)
			{
				memoState = 0;
				memoFlag = 0;
			}
			else
			{
				memoFlag = memoState & 255;
				memoState = memoState & 2147483392;
			}
			
			switch (memoFlag)
			{
				case 0:
				{
					startQuestTimer((talker.getObjectId() + 1000000) + "", 5000, null, qs.getPlayer());
					memoState = 2147483392 & ~(8388608 | 1048576);
					qs.setMemoState(1 | memoState);
					if (qs.getMemoStateEx(1) <= 0)
					{
						qs.setMemoStateEx(1, -2);
					}
					break;
				}
				case 1:
				{
					showQuestionMark(talker, 1);
					playTutorialVoice(talker, "tutorial_voice_006");
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					break;
				}
				case 2:
				{
					if (haveMemo(talker, Q201_TUTORIAL_HUMAN_FIGHTER) || haveMemo(talker, Q202_TUTORIAL_HUMAN_MAGE) || haveMemo(talker, Q203_TUTORIAL_ELF) || haveMemo(talker, Q204_TUTORIAL_DARK_ELF) || haveMemo(talker, Q205_TUTORIAL_ORC) || haveMemo(talker, Q206_TUTORIAL_DWARF))
					{
						showQuestionMark(talker, 6);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					}
					else
					{
						showQuestionMark(talker, 2);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					}
					break;
				}
				case 3:
				{
					int stateMark = 1;
					if (getQuestItemsCount(talker, BLUE_GEMSTONE) == 1)
					{
						stateMark = 3;
					}
					else if (qs.getMemoStateEx(1) == 2)
					{
						stateMark = 2;
					}
					
					switch (stateMark)
					{
						case 1:
						{
							showQuestionMark(talker, 3);
							break;
						}
						case 2:
						{
							showQuestionMark(talker, 4);
							break;
						}
						case 3:
						{
							showQuestionMark(talker, 5);
							break;
						}
					}
					
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					break;
				}
				case 4:
				{
					showQuestionMark(talker, 12);
					playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					break;
				}
			}
			
			enableTutorialEvent(qs, memoState);
		}
		else
		{
			switch (talker.getLevel())
			{
				case 18:
				{
					if (haveMemo(talker, 10276) && (getOneTimeQuestFlag(talker, 10276) == 0))
					{
						showQuestionMark(talker, 33);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					}
					break;
				}
				case 28:
				{
					if (haveMemo(talker, 10277) && (getOneTimeQuestFlag(talker, 10277) == 0))
					{
						showQuestionMark(talker, 33);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					}
					break;
				}
				case 38:
				{
					if (haveMemo(talker, 10278) && (getOneTimeQuestFlag(talker, 10278) == 0))
					{
						showQuestionMark(talker, 33);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					}
					break;
				}
				case 48:
				{
					if (haveMemo(talker, 10279) && (getOneTimeQuestFlag(talker, 10279) == 0))
					{
						showQuestionMark(talker, 33);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					}
					break;
				}
				case 58:
				{
					if (haveMemo(talker, 10280) && (getOneTimeQuestFlag(talker, 10280) == 0))
					{
						showQuestionMark(talker, 33);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					}
					break;
				}
				case 68:
				{
					if (haveMemo(talker, 10281) && (getOneTimeQuestFlag(talker, 10281) == 0))
					{
						showQuestionMark(talker, 33);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					}
					break;
				}
				case 79:
				{
					if (haveMemo(talker, 192) && (getOneTimeQuestFlag(talker, 192) == 0))
					{
						showQuestionMark(talker, 34);
						playSound(talker, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
					}
					break;
				}
			}
			
			int territoryWarId = getDominionSiegeID(talker);
			int territoryWarState = getNRMemoStateEx(qs, 728, 1);
			
			if ((territoryWarId > 0) && (getDominionWarState(territoryWarId) == 5))
			{
				if (!haveNRMemo(qs, 728))
				{
					setNRMemo(qs, 728);
					setNRMemoState(qs, 728, 0);
					setNRMemoStateEx(qs, 728, 1, territoryWarId);
				}
				else if (territoryWarId != territoryWarState)
				{
					setNRMemoState(qs, 728, 0);
					setNRMemoStateEx(qs, 728, 1, territoryWarId);
				}
				
				switch (territoryWarId)
				{
					case 81:
					{
						if (getDominionWarState(TW_GLUDIO) == 5)
						{
							if (!haveNRMemo(qs, Q717_FOR_THE_SAKE_OF_THE_TERRITORY_GLUDIO))
							{
								setNRMemo(qs, Q717_FOR_THE_SAKE_OF_THE_TERRITORY_GLUDIO);
								setNRMemoState(qs, Q717_FOR_THE_SAKE_OF_THE_TERRITORY_GLUDIO, 0);
								setNRFlagJournal(qs, Q717_FOR_THE_SAKE_OF_THE_TERRITORY_GLUDIO, 1);
								showQuestionMark(talker, Q717_FOR_THE_SAKE_OF_THE_TERRITORY_GLUDIO);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
							else
							{
								showQuestionMark(talker, Q717_FOR_THE_SAKE_OF_THE_TERRITORY_GLUDIO);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
						}
						break;
					}
					case 82:
					{
						if (getDominionWarState(TW_DION) == 5)
						{
							if (!haveNRMemo(qs, Q718_FOR_THE_SAKE_OF_THE_TERRITORY_DION))
							{
								setNRMemo(qs, Q718_FOR_THE_SAKE_OF_THE_TERRITORY_DION);
								setNRMemoState(qs, Q718_FOR_THE_SAKE_OF_THE_TERRITORY_DION, 0);
								setNRFlagJournal(qs, Q718_FOR_THE_SAKE_OF_THE_TERRITORY_DION, 1);
								showQuestionMark(talker, Q718_FOR_THE_SAKE_OF_THE_TERRITORY_DION);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
							else
							{
								showQuestionMark(talker, Q718_FOR_THE_SAKE_OF_THE_TERRITORY_DION);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
						}
						break;
					}
					case 83:
					{
						if (getDominionWarState(TW_GIRAN) == 5)
						{
							if (!haveNRMemo(qs, Q719_FOR_THE_SAKE_OF_THE_TERRITORY_GIRAN))
							{
								setNRMemo(qs, Q719_FOR_THE_SAKE_OF_THE_TERRITORY_GIRAN);
								setNRMemoState(qs, Q719_FOR_THE_SAKE_OF_THE_TERRITORY_GIRAN, 0);
								setNRFlagJournal(qs, Q719_FOR_THE_SAKE_OF_THE_TERRITORY_GIRAN, 1);
								showQuestionMark(talker, Q719_FOR_THE_SAKE_OF_THE_TERRITORY_GIRAN);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
							else
							{
								showQuestionMark(talker, Q719_FOR_THE_SAKE_OF_THE_TERRITORY_GIRAN);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
						}
						break;
					}
					case 84:
					{
						if (getDominionWarState(TW_OREN) == 5)
						{
							if (!haveNRMemo(qs, Q720_FOR_THE_SAKE_OF_THE_TERRITORY_OREN))
							{
								setNRMemo(qs, Q720_FOR_THE_SAKE_OF_THE_TERRITORY_OREN);
								setNRMemoState(qs, Q720_FOR_THE_SAKE_OF_THE_TERRITORY_OREN, 0);
								setNRFlagJournal(qs, Q720_FOR_THE_SAKE_OF_THE_TERRITORY_OREN, 1);
								showQuestionMark(talker, Q720_FOR_THE_SAKE_OF_THE_TERRITORY_OREN);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
							else
							{
								showQuestionMark(talker, Q720_FOR_THE_SAKE_OF_THE_TERRITORY_OREN);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
						}
						break;
					}
					case 85:
					{
						if (getDominionWarState(TW_ADEN) == 5)
						{
							if (!haveNRMemo(qs, Q721_FOR_THE_SAKE_OF_THE_TERRITORY_ADEN))
							{
								setNRMemo(qs, Q721_FOR_THE_SAKE_OF_THE_TERRITORY_ADEN);
								setNRMemoState(qs, Q721_FOR_THE_SAKE_OF_THE_TERRITORY_ADEN, 0);
								setNRFlagJournal(qs, Q721_FOR_THE_SAKE_OF_THE_TERRITORY_ADEN, 1);
								showQuestionMark(talker, Q721_FOR_THE_SAKE_OF_THE_TERRITORY_ADEN);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
							else
							{
								showQuestionMark(talker, Q721_FOR_THE_SAKE_OF_THE_TERRITORY_ADEN);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
						}
						break;
					}
					case 86:
					{
						if (getDominionWarState(TW_HEINE) == 5)
						{
							if (!haveNRMemo(qs, Q722_FOR_THE_SAKE_OF_THE_TERRITORY_INNADRIL))
							{
								setNRMemo(qs, Q722_FOR_THE_SAKE_OF_THE_TERRITORY_INNADRIL);
								setNRMemoState(qs, Q722_FOR_THE_SAKE_OF_THE_TERRITORY_INNADRIL, 0);
								setNRFlagJournal(qs, Q722_FOR_THE_SAKE_OF_THE_TERRITORY_INNADRIL, 1);
								showQuestionMark(talker, Q722_FOR_THE_SAKE_OF_THE_TERRITORY_INNADRIL);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
							else
							{
								showQuestionMark(talker, Q722_FOR_THE_SAKE_OF_THE_TERRITORY_INNADRIL);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
						}
						break;
					}
					case 87:
					{
						if (getDominionWarState(TW_GODDARD) == 5)
						{
							if (!haveNRMemo(qs, Q723_FOR_THE_SAKE_OF_THE_TERRITORY_GODDARD))
							{
								setNRMemo(qs, Q723_FOR_THE_SAKE_OF_THE_TERRITORY_GODDARD);
								setNRMemoState(qs, Q723_FOR_THE_SAKE_OF_THE_TERRITORY_GODDARD, 0);
								setNRFlagJournal(qs, Q723_FOR_THE_SAKE_OF_THE_TERRITORY_GODDARD, 1);
								showQuestionMark(talker, Q723_FOR_THE_SAKE_OF_THE_TERRITORY_GODDARD);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
							else
							{
								showQuestionMark(talker, Q723_FOR_THE_SAKE_OF_THE_TERRITORY_GODDARD);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
						}
						break;
					}
					case 88:
					{
						if (getDominionWarState(TW_RUNE) == 5)
						{
							if (!haveNRMemo(qs, Q724_FOR_THE_SAKE_OF_THE_TERRITORY_RUNE))
							{
								setNRMemo(qs, Q724_FOR_THE_SAKE_OF_THE_TERRITORY_RUNE);
								setNRMemoState(qs, Q724_FOR_THE_SAKE_OF_THE_TERRITORY_RUNE, 0);
								setNRFlagJournal(qs, Q724_FOR_THE_SAKE_OF_THE_TERRITORY_RUNE, 1);
								showQuestionMark(talker, Q724_FOR_THE_SAKE_OF_THE_TERRITORY_RUNE);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
							else
							{
								showQuestionMark(talker, Q724_FOR_THE_SAKE_OF_THE_TERRITORY_RUNE);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
						}
						break;
					}
					case 89:
					{
						if (getDominionWarState(TW_SCHUTTGART) == 5)
						{
							if (!haveNRMemo(qs, Q725_FOR_THE_SAKE_OF_THE_TERRITORY_SCHUTTGART))
							{
								setNRMemo(qs, Q725_FOR_THE_SAKE_OF_THE_TERRITORY_SCHUTTGART);
								setNRMemoState(qs, Q725_FOR_THE_SAKE_OF_THE_TERRITORY_SCHUTTGART, 0);
								setNRFlagJournal(qs, Q725_FOR_THE_SAKE_OF_THE_TERRITORY_SCHUTTGART, 1);
								showQuestionMark(talker, Q725_FOR_THE_SAKE_OF_THE_TERRITORY_SCHUTTGART);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
							else
							{
								showQuestionMark(talker, Q725_FOR_THE_SAKE_OF_THE_TERRITORY_SCHUTTGART);
								playSound(talker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
						}
						break;
					}
				}
			}
			else
			{
				if (haveNRMemo(qs, Q728_TERRITORY_WAR) && (territoryWarState >= 81) && (territoryWarState <= 89))
				{
					int twNRState = getNRMemoState(qs, Q728_TERRITORY_WAR);
					int twNRStateForCurrentWar = getNRMemoState(qs, 636 + territoryWarState);
					if (twNRStateForCurrentWar >= 0)
					{
						setNRMemoState(qs, Q728_TERRITORY_WAR, twNRStateForCurrentWar + twNRState);
						removeNRMemo(qs, 636 + territoryWarState);
					}
				}
				
				if (haveNRMemo(qs, 739) && (getNRMemoState(qs, 739) > 0))
				{
					setNRMemoState(qs, 739, 0);
				}
				
				if (haveNRMemo(qs, Q729_PROTECT_THE_TERRITORY_CATAPULT))
				{
					removeNRMemo(qs, 729);
				}
				
				if (haveNRMemo(qs, Q730_PROTECT_THE_SUPPLIES_SAFE))
				{
					removeNRMemo(qs, 730);
				}
				
				if (haveNRMemo(qs, Q731_PROTECT_THE_MILITARY_ASSOCIATION_LEADER))
				{
					removeNRMemo(qs, 731);
				}
				
				if (haveNRMemo(qs, Q732_PROTECT_THE_RELIGIOUS_ASSOCIATION_LEADER))
				{
					removeNRMemo(qs, 732);
				}
				
				if (haveNRMemo(qs, Q733_PROTECT_THE_ECONOMIC_ASSOCIATION_LEADER))
				{
					removeNRMemo(qs, 733);
				}
			}
		}
	}
	
	// ---------------------------------- Event
	
	private void eventRoien(int event, Player talker, Npc npc, QuestState qs)
	{
		switch (event)
		{
			case 31:
			{
				if (hasQuestItems(talker, RECOMMENDATION_1))
				{
					if (!talker.isMageClass() && (qs.getMemoStateEx(1) <= 3))
					{
						giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
						playTutorialVoice(talker, "tutorial_voice_026");
						addExpAndSp(talker, 0, 50);
						qs.setMemoStateEx(1, 4);
					}
					
					if (talker.isMageClass() && (qs.getMemoStateEx(1) <= 3))
					{
						if (talker.getPlayerClass() == PlayerClass.ORC_MAGE)
						{
							giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
							playTutorialVoice(talker, "tutorial_voice_026");
						}
						else
						{
							giveItems(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS, 100);
							playTutorialVoice(talker, "tutorial_voice_027");
						}
						
						addExpAndSp(talker, 0, 50);
						qs.setMemoStateEx(1, 4);
					}
					
					startQuestTimer(npc.getId() + "", 60000, npc, talker);
					takeItems(talker, RECOMMENDATION_1, 1);
					showHtmlFile(talker, "30008-002.html");
				}
				break;
			}
			case 41:
			{
				teleportPlayer(talker, new Location(-120050, 44500, 360), 0);
				showHtmlFile(talker, "30008-005.html");
				break;
			}
			case 42:
			{
				addRadar(talker, -84081, 243277, -3723);
				showHtmlFile(talker, "30008-006.html");
				break;
			}
		}
	}
	
	private void eventGallin(int event, Player talker, Npc npc, QuestState qs)
	{
		switch (event)
		{
			case 31:
			{
				if (hasQuestItems(talker, RECOMMENDATION_2))
				{
					if (!talker.isMageClass() && (getQuestItemsCount(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS) <= 200))
					{
						playTutorialVoice(talker, "tutorial_voice_026");
						giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
						addExpAndSp(talker, 0, 50);
					}
					
					if (talker.isMageClass() && (getQuestItemsCount(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS) <= 200) && (getQuestItemsCount(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS) <= 100))
					{
						if (talker.getPlayerClass() == PlayerClass.ORC_MAGE)
						{
							playTutorialVoice(talker, "tutorial_voice_026");
							giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
						}
						else
						{
							playTutorialVoice(talker, "tutorial_voice_027");
							giveItems(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS, 200);
						}
						
						addExpAndSp(talker, 0, 50);
					}
					
					takeItems(talker, RECOMMENDATION_2, 1);
					startQuestTimer(npc.getId() + "", 60000, npc, talker);
					if (qs.getMemoStateEx(1) <= 3)
					{
						qs.setMemoStateEx(1, 4);
					}
					
					showHtmlFile(talker, "30017-002.html");
				}
				break;
			}
			case 41:
			{
				teleportPlayer(talker, new Location(-120050, 44500, 360), 0);
				addRadar(talker, -119692, 44504, 380);
				showHtmlFile(talker, "30017-005.html");
				break;
			}
			case 42:
			{
				addRadar(talker, -84081, 243277, -3723);
				showHtmlFile(talker, "30017-006.html");
				break;
			}
		}
	}
	
	private void eventJundin(int event, Player talker, Npc npc, QuestState qs)
	{
		switch (event)
		{
			case 31:
			{
				if (hasQuestItems(talker, BLOOD_OF_MITRAELL))
				{
					if (!talker.isMageClass() && (getQuestItemsCount(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS) <= 200))
					{
						playTutorialVoice(talker, "tutorial_voice_026");
						giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
						addExpAndSp(talker, 0, 50);
					}
					
					if (talker.isMageClass() && (getQuestItemsCount(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS) <= 200) && (getQuestItemsCount(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS) <= 100))
					{
						if (talker.getPlayerClass() == PlayerClass.ORC_MAGE)
						{
							playTutorialVoice(talker, "tutorial_voice_026");
							giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
						}
						else
						{
							playTutorialVoice(talker, "tutorial_voice_027");
							giveItems(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS, 200);
						}
						
						addExpAndSp(talker, 0, 50);
					}
					
					takeItems(talker, BLOOD_OF_MITRAELL, 1);
					startQuestTimer(npc.getId() + "", 60000, npc, talker);
					if (qs.getMemoStateEx(1) <= 3)
					{
						qs.setMemoStateEx(1, 4);
					}
					
					showHtmlFile(talker, "30129-002.html");
				}
				break;
			}
			case 41:
			{
				teleportPlayer(talker, new Location(-120050, 44500, 360), 0);
				addRadar(talker, -119692, 44504, 380);
				showHtmlFile(talker, "30129-005.html");
				break;
			}
			case 42:
			{
				addRadar(talker, 17024, 13296, -3744);
				showHtmlFile(talker, "30129-006.html");
				break;
			}
		}
	}
	
	private void eventNerupa(int event, Player talker, Npc npc, QuestState qs)
	{
		switch (event)
		{
			case 31:
			{
				if (hasQuestItems(talker, LEAF_OF_THE_MOTHER_TREE))
				{
					if (!talker.isMageClass() && (getQuestItemsCount(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS) <= 200))
					{
						
						playTutorialVoice(talker, "tutorial_voice_026");
						giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
						addExpAndSp(talker, 0, 50);
					}
					
					if (talker.isMageClass() && (getQuestItemsCount(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS) <= 200) && (getQuestItemsCount(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS) <= 100))
					{
						playTutorialVoice(talker, "tutorial_voice_027");
						giveItems(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS, 200);
						addExpAndSp(talker, 0, 50);
					}
					
					takeItems(talker, LEAF_OF_THE_MOTHER_TREE, 1);
					startQuestTimer(npc.getId() + "", 60000, npc, talker);
					if (qs.getMemoStateEx(1) <= 3)
					{
						qs.setMemoStateEx(1, 4);
					}
					
					showHtmlFile(talker, "30370-002.html");
				}
				break;
			}
			case 41:
			{
				teleportPlayer(talker, new Location(-120050, 44500, 360), 0);
				addRadar(talker, -119692, 44504, 380);
				showHtmlFile(talker, "30370-005.html");
				break;
			}
			case 42:
			{
				addRadar(talker, 45475, 48359, -3060);
				showHtmlFile(talker, "30370-006.html");
				break;
			}
		}
	}
	
	private void eventForemanLaferon(int event, Player talker, Npc npc, QuestState qs)
	{
		switch (event)
		{
			case 31:
			{
				if (hasQuestItems(talker, LICENSE_OF_MINER))
				{
					if (!talker.isMageClass() && (getQuestItemsCount(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS) <= 200))
					{
						
						playTutorialVoice(talker, "tutorial_voice_026");
						giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
						addExpAndSp(talker, 0, 50);
					}
					
					if (talker.isMageClass() && (getQuestItemsCount(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS) <= 200) && (getQuestItemsCount(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS) <= 100))
					{
						playTutorialVoice(talker, "tutorial_voice_027");
						giveItems(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS, 100);
						addExpAndSp(talker, 0, 50);
					}
					
					takeItems(talker, LICENSE_OF_MINER, 1);
					startQuestTimer(npc.getId() + "", 60000, npc, talker);
					if (qs.getMemoStateEx(1) <= 3)
					{
						qs.setMemoStateEx(1, 4);
					}
					
					showHtmlFile(talker, "30528-002.html");
				}
				break;
			}
			case 41:
			{
				teleportPlayer(talker, new Location(-120050, 44500, 360), 0);
				addRadar(talker, -119692, 44504, 380);
				showHtmlFile(talker, "30528-005.html");
				break;
			}
			case 42:
			{
				addRadar(talker, 115632, -177996, -905);
				showHtmlFile(talker, "30528-006.html");
				break;
			}
		}
	}
	
	private void eventGuardianVullkus(int event, Player talker, Npc npc, QuestState qs)
	{
		switch (event)
		{
			case 31:
			{
				if (hasQuestItems(talker, VOUCHER_OF_FLAME))
				{
					takeItems(talker, VOUCHER_OF_FLAME, 1);
					startQuestTimer(npc.getId() + "", 60000, npc, talker);
					if (qs.getMemoStateEx(1) <= 3)
					{
						qs.setMemoStateEx(1, 4);
					}
					
					if (getQuestItemsCount(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS) <= 200)
					{
						giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
						addExpAndSp(talker, 0, 50);
					}
					
					playTutorialVoice(talker, "tutorial_voice_026");
					showHtmlFile(talker, "30573-002.html");
				}
				break;
			}
			case 41:
			{
				teleportPlayer(talker, new Location(-120050, 44500, 360), 0);
				addRadar(talker, -119692, 44504, 380);
				showHtmlFile(talker, "30573-005.html");
				break;
			}
			case 42:
			{
				addRadar(talker, -45032, -113598, -192);
				showHtmlFile(talker, "30573-006.html");
				break;
			}
		}
	}
	
	private void eventSubelderPerwan(int event, Player talker, Npc npc, QuestState qs)
	{
		if ((event == 31) && hasQuestItems(talker, DIPLOMA))
		{
			if ((talker.getRace() == Race.KAMAEL) && (talker.getPlayerClass().level() == 0) && (qs.getMemoStateEx(1) <= 3))
			{
				giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
				playTutorialVoice(talker, "tutorial_voice_026");
				addExpAndSp(talker, 0, 50);
				qs.setMemoStateEx(1, 4);
			}
			
			takeItems(talker, DIPLOMA, -1);
			startQuestTimer(npc.getId() + "", 60000, npc, talker);
			addRadar(talker, -119692, 44504, 380);
			showHtmlFile(talker, "32133-002.html");
		}
	}
	
	// ---------------------------------- Talks
	
	private void talkRoien(Npc npc, Player talker, QuestState qs)
	{
		if (hasQuestItems(talker, RECOMMENDATION_1))
		{
			showHtmlFile(talker, "30008-001.html", npc);
		}
		else if (qs.getMemoStateEx(1) > 3)
		{
			showHtmlFile(talker, "30008-004.html", npc);
		}
		else if (qs.getMemoStateEx(1) <= 3)
		{
			showHtmlFile(talker, "30008-003.html", npc);
		}
	}
	
	private void talkCarl(Npc npc, Player talker, QuestState qs)
	{
		if (qs.getMemoStateEx(1) <= 0)
		{
			if ((talker.getPlayerClass() == PlayerClass.FIGHTER) && (talker.getRace() == Race.HUMAN))
			{
				removeRadar(talker, -71424, 258336, -3109);
				startQuestTimer(npc.getId() + "", 30000, npc, talker);
				qs.setMemoStateEx(1, 0);
				enableTutorialEvent(qs, (qs.getMemoState() & 2147483392) | 1048576);
				showHtmlFile(talker, "30009-001.html");
			}
			else
			{
				showHtmlFile(talker, "30009-006.html");
			}
		}
		else if (((qs.getMemoStateEx(1) == 0) || (qs.getMemoStateEx(1) == 1) || (qs.getMemoStateEx(1) == 2)) && !hasQuestItems(talker, BLUE_GEMSTONE))
		{
			showHtmlFile(talker, "30009-002.html");
		}
		else if (((qs.getMemoStateEx(1) == 0) || (qs.getMemoStateEx(1) == 1) || (qs.getMemoStateEx(1) == 2)) && hasQuestItems(talker, BLUE_GEMSTONE))
		{
			takeItems(talker, BLUE_GEMSTONE, -1);
			qs.setMemoStateEx(1, 3);
			giveItems(talker, RECOMMENDATION_1, 1);
			
			startQuestTimer(npc.getId() + "", 30000, npc, talker);
			qs.setMemoState((qs.getMemoState() & 2147483392) | 4);
			if (!talker.isMageClass() && !hasQuestItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS))
			{
				giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
				playTutorialVoice(talker, "tutorial_voice_026");
				
			}
			
			if (talker.isMageClass() && !hasQuestItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS) && !hasQuestItems(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS))
			{
				if (talker.getPlayerClass() == PlayerClass.ORC_MAGE)
				{
					playTutorialVoice(talker, "tutorial_voice_026");
					giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
				}
				else
				{
					playTutorialVoice(talker, "tutorial_voice_027");
					giveItems(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS, 100);
				}
			}
			
			showHtmlFile(talker, "30009-003.html");
		}
		else if (qs.getMemoStateEx(1) == 3)
		{
			showHtmlFile(talker, "30009-004.html");
		}
		else if (qs.getMemoStateEx(1) > 3)
		{
			showHtmlFile(talker, "30009-005.html");
		}
	}
	
	private void talkGallin(Npc npc, Player talker, QuestState qs)
	{
		if (hasQuestItems(talker, RECOMMENDATION_2))
		{
			showHtmlFile(talker, "30017-001.html", npc);
		}
		else if (!hasQuestItems(talker, RECOMMENDATION_2) && (qs.getMemoStateEx(1) > 3))
		{
			showHtmlFile(talker, "30017-004.html", npc);
		}
		else if (!hasQuestItems(talker, RECOMMENDATION_2) && (qs.getMemoStateEx(1) <= 3))
		{
			showHtmlFile(talker, "30017-003.html", npc);
		}
	}
	
	private void talkDoff(Npc npc, Player talker, QuestState qs)
	{
		if (qs.getMemoStateEx(1) <= 0)
		{
			if ((talker.getPlayerClass() == PlayerClass.MAGE) && (talker.getRace() == Race.HUMAN))
			{
				removeRadar(talker, -91036, 248044, -3568);
				startQuestTimer(npc.getId() + "", 30000, npc, talker);
				qs.setMemoStateEx(1, 0);
				enableTutorialEvent(qs, (qs.getMemoState() & 2147483392) | 1048576);
				showHtmlFile(talker, "30019-001.html");
			}
			else
			{
				showHtmlFile(talker, "30009-006.html");
			}
		}
		else if (((qs.getMemoStateEx(1) == 0) || (qs.getMemoStateEx(1) == 1) || (qs.getMemoStateEx(1) == 2)) && !hasQuestItems(talker, BLUE_GEMSTONE))
		{
			showHtmlFile(talker, "30019-002.html");
		}
		
		if (((qs.getMemoStateEx(1) == 0) || (qs.getMemoStateEx(1) == 1) || (qs.getMemoStateEx(1) == 2)) && hasQuestItems(talker, BLUE_GEMSTONE))
		{
			takeItems(talker, BLUE_GEMSTONE, -1);
			qs.setMemoStateEx(1, 3);
			giveItems(talker, RECOMMENDATION_2, 1);
			
			startQuestTimer(npc.getId() + "", 30000, npc, talker);
			qs.setMemoState((qs.getMemoState() & 2147483392) | 4);
			if (!talker.isMageClass() && !hasQuestItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS))
			{
				giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
				playTutorialVoice(talker, "tutorial_voice_026");
			}
			
			if (talker.isMageClass() && !hasQuestItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS) && !hasQuestItems(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS))
			{
				if (talker.getPlayerClass() == PlayerClass.ORC_MAGE)
				{
					playTutorialVoice(talker, "tutorial_voice_026");
					giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
				}
				else
				{
					playTutorialVoice(talker, "tutorial_voice_027");
					giveItems(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS, 100);
				}
			}
			
			showHtmlFile(talker, "30019-003.html");
		}
		else if (qs.getMemoStateEx(1) == 3)
		{
			showHtmlFile(talker, "30019-004.html");
		}
		else if (qs.getMemoStateEx(1) > 3)
		{
			showHtmlFile(talker, "30009-005.html");
		}
	}
	
	private void talkJundin(Npc npc, Player talker, QuestState qs)
	{
		if (hasQuestItems(talker, BLOOD_OF_MITRAELL))
		{
			showHtmlFile(talker, "30129-001.html", npc);
		}
		else if (!hasQuestItems(talker, BLOOD_OF_MITRAELL) && (qs.getMemoStateEx(1) > 3))
		{
			showHtmlFile(talker, "30129-004.html", npc);
		}
		else if (!hasQuestItems(talker, BLOOD_OF_MITRAELL) && (qs.getMemoStateEx(1) <= 3))
		{
			showHtmlFile(talker, "30129-003.html", npc);
		}
	}
	
	private void talkPoeny(Npc npc, Player talker, QuestState qs)
	{
		if (qs.getMemoStateEx(1) <= 0)
		{
			if (talker.getRace() == Race.DARK_ELF)
			{
				removeRadar(talker, 28384, 11056, -4233);
				startQuestTimer(npc.getId() + "", 30000, npc, talker);
				
				if (!talker.isMageClass())
				{
					showHtmlFile(talker, "30009-001.html");
				}
				else
				{
					showHtmlFile(talker, "30019-001.html");
				}
				
				qs.setMemoStateEx(1, 0);
				enableTutorialEvent(qs, (qs.getMemoState() & 2147483392) | 1048576);
			}
			else
			{
				showHtmlFile(talker, "30009-006.html");
			}
		}
		else if (((qs.getMemoStateEx(1) == 0) || (qs.getMemoStateEx(1) == 1) || (qs.getMemoStateEx(1) == 2)) && !hasQuestItems(talker, BLUE_GEMSTONE))
		{
			if (!talker.isMageClass())
			{
				showHtmlFile(talker, "30009-002.html");
			}
			else
			{
				showHtmlFile(talker, "30019-002.html");
			}
		}
		else if (((qs.getMemoStateEx(1) == 0) || (qs.getMemoStateEx(1) == 1) || (qs.getMemoStateEx(1) == 2)) && hasQuestItems(talker, BLUE_GEMSTONE))
		{
			
			if (!talker.isMageClass())
			{
				showHtmlFile(talker, "30131-003f.html");
			}
			else
			{
				showHtmlFile(talker, "30131-003m.html");
			}
			
			takeItems(talker, BLUE_GEMSTONE, -1);
			qs.setMemoStateEx(1, 3);
			giveItems(talker, BLOOD_OF_MITRAELL, 1);
			startQuestTimer(npc.getId() + "", 30000, npc, talker);
			
			qs.setMemoState((qs.getMemoState() & 2147483392) | 4);
			
			if (!talker.isMageClass() && !hasQuestItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS))
			{
				playTutorialVoice(talker, "tutorial_voice_026");
				giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
			}
			
			if (talker.isMageClass() && !hasQuestItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS) && !hasQuestItems(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS))
			{
				if (talker.getPlayerClass() == PlayerClass.ORC_MAGE)
				{
					playTutorialVoice(talker, "tutorial_voice_026");
					giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 100);
				}
				else
				{
					playTutorialVoice(talker, "tutorial_voice_027");
					giveItems(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS, 100);
				}
			}
		}
		else if (qs.getMemoStateEx(1) == 3)
		{
			showHtmlFile(talker, "30131-004.html");
		}
		else if (qs.getMemoStateEx(1) > 3)
		{
			showHtmlFile(talker, "30009-005.html");
		}
	}
	
	private void talkNerupa(Npc npc, Player talker, QuestState qs)
	{
		if (hasQuestItems(talker, LEAF_OF_THE_MOTHER_TREE))
		{
			showHtmlFile(talker, "30370-001.html", npc);
		}
		else if (!hasQuestItems(talker, LEAF_OF_THE_MOTHER_TREE) && (qs.getMemoStateEx(1) > 3))
		{
			showHtmlFile(talker, "30370-004.html", npc);
		}
		else if (!hasQuestItems(talker, LEAF_OF_THE_MOTHER_TREE) && (qs.getMemoStateEx(1) <= 3))
		{
			showHtmlFile(talker, "30370-003.html", npc);
		}
	}
	
	private void talkMotherTemp(Npc npc, Player talker, QuestState qs)
	{
		if (qs.getMemoStateEx(1) <= 0)
		{
			if (talker.getRace() == Race.ELF)
			{
				removeRadar(talker, 46112, 41200, -3504);
				startQuestTimer(npc.getId() + "", 30000, npc, talker);
				qs.setMemoStateEx(1, 0);
				enableTutorialEvent(qs, (qs.getMemoState() & 2147483392) | 1048576);
				if (!talker.isMageClass())
				{
					showHtmlFile(talker, "30009-001.html");
				}
				else
				{
					showHtmlFile(talker, "30019-001.html");
				}
			}
			else
			{
				showHtmlFile(talker, "30009-006.html");
			}
		}
		else if (((qs.getMemoStateEx(1) == 0) || (qs.getMemoStateEx(1) == 1) || (qs.getMemoStateEx(1) == 2)) && !hasQuestItems(talker, BLUE_GEMSTONE))
		{
			if (!talker.isMageClass())
			{
				showHtmlFile(talker, "30009-002.html");
			}
			else
			{
				showHtmlFile(talker, "30019-002.html");
			}
		}
		else if (((qs.getMemoStateEx(1) == 0) || (qs.getMemoStateEx(1) == 1) || (qs.getMemoStateEx(1) == 2)) && hasQuestItems(talker, BLUE_GEMSTONE))
		{
			takeItems(talker, BLUE_GEMSTONE, -1);
			qs.setMemoStateEx(1, 3);
			giveItems(talker, LEAF_OF_THE_MOTHER_TREE, 1);
			startQuestTimer(npc.getId() + "", 30000, npc, talker);
			qs.setMemoState((qs.getMemoState() & 2147483392) | 4);
			if (!talker.isMageClass() && !hasQuestItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS))
			{
				playTutorialVoice(talker, "tutorial_voice_026");
				giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
			}
			
			if (talker.isMageClass() && !hasQuestItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS) && !hasQuestItems(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS))
			{
				if (talker.getPlayerClass() == PlayerClass.ORC_MAGE)
				{
					playTutorialVoice(talker, "tutorial_voice_026");
					giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
				}
				else
				{
					playTutorialVoice(talker, "tutorial_voice_027");
					giveItems(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS, 100);
				}
			}
			
			if (!talker.isMageClass())
			{
				showHtmlFile(talker, "30400-003f.html");
			}
			else
			{
				showHtmlFile(talker, "30400-003m.html");
			}
		}
		else if (qs.getMemoStateEx(1) == 3)
		{
			showHtmlFile(talker, "30400-004.html");
		}
		else if (qs.getMemoStateEx(1) > 3)
		{
			showHtmlFile(talker, "30009-005.html");
		}
	}
	
	private void talkForemanLaferon(Npc npc, Player talker, QuestState qs)
	{
		if (hasQuestItems(talker, LICENSE_OF_MINER))
		{
			showHtmlFile(talker, "30528-001.html", npc);
		}
		else if (!hasQuestItems(talker, LICENSE_OF_MINER) && (qs.getMemoStateEx(1) > 3))
		{
			showHtmlFile(talker, "30528-004.html", npc);
		}
		else if (!hasQuestItems(talker, LICENSE_OF_MINER) && (qs.getMemoStateEx(1) <= 3))
		{
			showHtmlFile(talker, "30528-003.html", npc);
		}
	}
	
	private void talkMinerMai(Npc npc, Player talker, QuestState qs)
	{
		if (qs.getMemoStateEx(1) <= 0)
		{
			if (talker.getRace() == Race.DWARF)
			{
				removeRadar(talker, 108567, -173994, -406);
				startQuestTimer(npc.getId() + "", 30000, npc, talker);
				
				qs.setMemoStateEx(1, 0);
				enableTutorialEvent(qs, (qs.getMemoState() & 2147483392) | 1048576);
				if (!talker.isMageClass())
				{
					showHtmlFile(talker, "30009-001.html");
				}
				else
				{
					showHtmlFile(talker, "30019-001.html");
				}
			}
			else
			{
				showHtmlFile(talker, "30009-006.html");
			}
		}
		else if (((qs.getMemoStateEx(1) == 0) || (qs.getMemoStateEx(1) == 1) || (qs.getMemoStateEx(1) == 2)) && !hasQuestItems(talker, BLUE_GEMSTONE))
		{
			showHtmlFile(talker, "30009-002.html");
		}
		else if (((qs.getMemoStateEx(1) == 0) || (qs.getMemoStateEx(1) == 1) || (qs.getMemoStateEx(1) == 2)) && hasQuestItems(talker, BLUE_GEMSTONE))
		{
			takeItems(talker, BLUE_GEMSTONE, -1);
			qs.setMemoStateEx(1, 3);
			giveItems(talker, LICENSE_OF_MINER, 1);
			startQuestTimer(npc.getId() + "", 30000, npc, talker);
			qs.setMemoState((qs.getMemoState() & 2147483392) | 4);
			if (!talker.isMageClass() && !hasQuestItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS))
			{
				playTutorialVoice(talker, "tutorial_voice_026");
				giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
			}
			
			if (talker.isMageClass() && !hasQuestItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS) && !hasQuestItems(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS))
			{
				if (talker.getPlayerClass() == PlayerClass.ORC_MAGE)
				{
					playTutorialVoice(talker, "tutorial_voice_026");
					giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
				}
				else
				{
					playTutorialVoice(talker, "tutorial_voice_027");
					giveItems(talker, SPIRITSHOT_NO_GRADE_FOR_BEGINNERS, 100);
				}
			}
			else
			{
				showHtmlFile(talker, "30530-003.html");
			}
		}
		else if (qs.getMemoStateEx(1) == 3)
		{
			showHtmlFile(talker, "30530-004.html");
		}
		else if (qs.getMemoStateEx(1) > 3)
		{
			showHtmlFile(talker, "30009-005.html");
		}
	}
	
	private void talkGuardianVullkus(Npc npc, Player talker, QuestState qs)
	{
		if (hasQuestItems(talker, VOUCHER_OF_FLAME))
		{
			showHtmlFile(talker, "30573-001.html", npc);
		}
		else if (qs.getMemoStateEx(1) > 3)
		{
			showHtmlFile(talker, "30573-004.html", npc);
		}
		else
		{
			showHtmlFile(talker, "30573-003.html", npc);
		}
	}
	
	private void talkShelaPriestess(Npc npc, Player talker, QuestState qs)
	{
		if (qs.getMemoStateEx(1) <= 0)
		{
			if (talker.getRace() == Race.ORC)
			{
				removeRadar(talker, -56736, -113680, -672);
				startQuestTimer(npc.getId() + "", 30000, npc, talker);
				qs.setMemoStateEx(1, 0);
				enableTutorialEvent(qs, (qs.getMemoState() & 2147483392) | 1048576);
				if (!talker.isMageClass())
				{
					showHtmlFile(talker, "30009-001.html");
				}
				else
				{
					showHtmlFile(talker, "30575-001.html");
				}
			}
			else
			{
				showHtmlFile(talker, "30009-006.html");
			}
		}
		else if (((qs.getMemoStateEx(1) == 0) || (qs.getMemoStateEx(1) == 1) || (qs.getMemoStateEx(1) == 2)) && !hasQuestItems(talker, BLUE_GEMSTONE))
		{
			if (!talker.isMageClass())
			{
				showHtmlFile(talker, "30009-002.html");
			}
			else
			{
				showHtmlFile(talker, "30575-002.html");
			}
		}
		else if (((qs.getMemoStateEx(1) == 0) || (qs.getMemoStateEx(1) == 1) || (qs.getMemoStateEx(1) == 2)) && hasQuestItems(talker, BLUE_GEMSTONE))
		{
			if (!talker.isMageClass())
			{
				showHtmlFile(talker, "30575-003f.html");
			}
			else
			{
				showHtmlFile(talker, "30575-003m.html");
			}
			
			takeItems(talker, BLUE_GEMSTONE, -1);
			qs.setMemoStateEx(1, 3);
			giveItems(talker, VOUCHER_OF_FLAME, 1);
			startQuestTimer(npc.getId() + "", 30000, npc, talker);
			qs.setMemoState((qs.getMemoState() & 2147483392) | 4);
			if (!hasQuestItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS))
			{
				giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
			}
			
			playTutorialVoice(talker, "tutorial_voice_026");
		}
		else if (qs.getMemoStateEx(1) == 3)
		{
			showHtmlFile(talker, "30575-004.html");
		}
		else if (qs.getMemoStateEx(1) > 3)
		{
			showHtmlFile(talker, "30009-005.html");
		}
	}
	
	private void talkSubelderPerwan(Npc npc, Player talker, QuestState qs)
	{
		if (hasQuestItems(talker, DIPLOMA))
		{
			showHtmlFile(talker, "32133-001.html", npc);
		}
		else if ((qs.getMemoStateEx(1) > 3))
		{
			showHtmlFile(talker, "32133-004.html", npc);
		}
		else if (qs.getMemoStateEx(1) <= 3)
		{
			showHtmlFile(talker, "32133-003.html", npc);
		}
	}
	
	private void talkHelperKrenisk(Npc npc, Player talker, QuestState qs)
	{
		if (qs.getMemoStateEx(1) <= 0)
		{
			if (talker.getRace() == Race.KAMAEL)
			{
				removeRadar(talker, -125872, 38016, 1251);
				qs.setMemoStateEx(1, 0);
				startQuestTimer(npc.getId() + "", 30000, npc, talker);
				enableTutorialEvent(qs, (qs.getMemoState() & 2147483392) | 1048576);
				showHtmlFile(talker, "32134-001.html");
			}
			else
			{
				showHtmlFile(talker, "30009-006.html");
			}
		}
		else if (((qs.getMemoStateEx(1) == 0) || (qs.getMemoStateEx(1) == 1) || (qs.getMemoStateEx(1) == 2)) && !hasQuestItems(talker, BLUE_GEMSTONE))
		{
			showHtmlFile(talker, "32134-002.html");
		}
		else if (((qs.getMemoStateEx(1) == 0) || (qs.getMemoStateEx(1) == 1) || (qs.getMemoStateEx(1) == 2)) && hasQuestItems(talker, BLUE_GEMSTONE))
		{
			showHtmlFile(talker, "32134-003.html");
			takeItems(talker, BLUE_GEMSTONE, -1);
			qs.setMemoStateEx(1, 3);
			giveItems(talker, DIPLOMA, 1);
			startQuestTimer(npc.getId() + "", 30000, npc, talker);
			qs.setMemoState((qs.getMemoState() & 2147483392) | 4);
			if ((talker.getRace() == Race.KAMAEL) && (talker.getPlayerClass().level() == 0) && !hasQuestItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS))
			{
				giveItems(talker, SOULSHOT_NO_GRADE_FOR_BEGINNERS, 200);
				playTutorialVoice(talker, "tutorial_voice_026");
			}
		}
		else if (qs.getMemoStateEx(1) == 3)
		{
			showHtmlFile(talker, "32134-004.html");
		}
		else if (qs.getMemoStateEx(1) > 3)
		{
			showHtmlFile(talker, "32134-005.html");
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if (npc.getId() == TUTORIAL_GREMLIN)
		{
			if ((qs.getMemoStateEx(1) == 1) || (qs.getMemoStateEx(1) == 0))
			{
				playSound(killer, "tutorial_voice_011");
				showQuestionMark(killer.asPlayer(), 3);
				qs.setMemoStateEx(1, 2);
			}
			
			if (((qs.getMemoStateEx(1) == 1) || (qs.getMemoStateEx(1) == 2) || (qs.getMemoStateEx(1) == 0)) && !hasQuestItems(killer, BLUE_GEMSTONE) && (getRandom(2) <= 1))
			{
				npc.dropItem(killer, BLUE_GEMSTONE, 1);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
			}
		}
	}
	
	private void showQuestionMark(Player talker, int number)
	{
		talker.sendPacket(new TutorialShowQuestionMark(number));
	}
	
	public void playTutorialVoice(Player player, String voice)
	{
		player.sendPacket(new PlaySound(2, voice, 0, 0, player.getX(), player.getY(), player.getZ()));
	}
	
	private void showTutorialHTML(Player player, String fileName)
	{
		final String content = getHtm(player, fileName);
		if (content != null)
		{
			player.sendPacket(new TutorialShowHtml(content));
		}
	}
	
	private void closeTutorialHtml(Player player)
	{
		player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
	}
	
	private int getDominionSiegeID(Player talker)
	{
		return TerritoryWarManager.getInstance().getRegisteredTerritoryId(talker);
	}
	
	private int getDominionWarState(int castleId)
	{
		return TerritoryWarManager.getInstance().isTWInProgress() ? 5 : 0;
	}
}
