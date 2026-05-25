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
package quests.Q10279_MutatedKaneusOren;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * Mutated Kaneus - Oren (10279)<br>
 * Original Jython script by Gnacik on 2010-06-29.
 * @author nonom
 */
public class Q10279_MutatedKaneusOren extends Quest
{
	// NPCs
	private static final int MOUEN = 30196;
	private static final int ROVIA = 30189;
	private static final int KAIM_ABIGORE = 18566;
	private static final int KNIGHT_MONTAGNAR = 18568;
	
	// Items
	private static final int TISSUE_KA = 13836;
	private static final int TISSUE_KM = 13837;
	
	public Q10279_MutatedKaneusOren()
	{
		super(10279);
		addStartNpc(MOUEN);
		addTalkId(MOUEN, ROVIA);
		addKillId(KAIM_ABIGORE, KNIGHT_MONTAGNAR);
		registerQuestItems(TISSUE_KA, TISSUE_KM);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return getNoQuestMsg(player);
		}
		
		switch (event)
		{
			case "30196-03.htm":
			{
				qs.startQuest();
				break;
			}
			case "30189-03.htm":
			{
				giveAdena(player, 100000, true);
				qs.exitQuest(false, true);
				break;
			}
		}
		
		return event;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		QuestState qs = getQuestState(killer, false);
		if (qs == null)
		{
			return;
		}
		
		final int npcId = npc.getId();
		if (killer.getParty() != null)
		{
			final List<Player> partyMembers = new ArrayList<>();
			for (Player member : killer.getParty().getMembers())
			{
				qs = getQuestState(member, false);
				if ((qs != null) && qs.isStarted() && (((npcId == KAIM_ABIGORE) && !hasQuestItems(member, TISSUE_KA)) || ((npcId == KNIGHT_MONTAGNAR) && !hasQuestItems(member, TISSUE_KM))))
				{
					partyMembers.add(member);
				}
			}
			
			if (!partyMembers.isEmpty())
			{
				rewardItem(npcId, getRandomEntry(partyMembers));
			}
		}
		else if (qs.isStarted())
		{
			rewardItem(npcId, killer);
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case MOUEN:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() > 47) ? "30196-01.htm" : "30196-00.htm";
						break;
					}
					case State.STARTED:
					{
						htmltext = (hasQuestItems(player, TISSUE_KA) && hasQuestItems(player, TISSUE_KM)) ? "30196-05.htm" : "30196-04.htm";
						break;
					}
					case State.COMPLETED:
					{
						htmltext = "30916-06.htm";
						break;
					}
				}
				break;
			}
			case ROVIA:
			{
				switch (qs.getState())
				{
					case State.STARTED:
					{
						htmltext = (hasQuestItems(player, TISSUE_KA) && hasQuestItems(player, TISSUE_KM)) ? "30189-02.htm" : "30189-01.htm";
						break;
					}
					case State.COMPLETED:
					{
						htmltext = getAlreadyCompletedMsg(player);
						break;
					}
					default:
					{
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * @param npcId the ID of the killed monster
	 * @param player
	 */
	private void rewardItem(int npcId, Player player)
	{
		if ((npcId == KAIM_ABIGORE) && !hasQuestItems(player, TISSUE_KA))
		{
			giveItems(player, TISSUE_KA, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		else if ((npcId == KNIGHT_MONTAGNAR) && !hasQuestItems(player, TISSUE_KM))
		{
			giveItems(player, TISSUE_KM, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
