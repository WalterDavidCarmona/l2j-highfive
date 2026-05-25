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
package quests.Q10280_MutatedKaneusSchuttgart;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * Mutated Kaneus - Schuttgart (10280)<br>
 * Original Jython script by Gnacik on 2010-06-29.
 * @author nonom
 */
public class Q10280_MutatedKaneusSchuttgart extends Quest
{
	// NPCs
	private static final int VISHOTSKY = 31981;
	private static final int ATRAXIA = 31972;
	private static final int VENOMOUS_STORACE = 18571;
	private static final int KEL_BILETTE = 18573;
	
	// Items
	private static final int TISSUE_VS = 13838;
	private static final int TISSUE_KB = 13839;
	
	public Q10280_MutatedKaneusSchuttgart()
	{
		super(10280);
		addStartNpc(VISHOTSKY);
		addTalkId(VISHOTSKY, ATRAXIA);
		addKillId(VENOMOUS_STORACE, KEL_BILETTE);
		registerQuestItems(TISSUE_VS, TISSUE_KB);
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
			case "31981-03.htm":
			{
				qs.startQuest();
				break;
			}
			case "31972-03.htm":
			{
				giveAdena(player, 210000, true);
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
				if ((qs != null) && qs.isStarted() && (((npcId == VENOMOUS_STORACE) && !hasQuestItems(member, TISSUE_VS)) || ((npcId == KEL_BILETTE) && !hasQuestItems(member, TISSUE_KB))))
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
			case VISHOTSKY:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() > 57) ? "31981-01.htm" : "31981-00.htm";
						break;
					}
					case State.STARTED:
					{
						htmltext = (hasQuestItems(player, TISSUE_VS) && hasQuestItems(player, TISSUE_KB)) ? "31981-05.htm" : "31981-04.htm";
						break;
					}
					case State.COMPLETED:
					{
						htmltext = "31981-06.htm";
						break;
					}
				}
				break;
			}
			case ATRAXIA:
			{
				switch (qs.getState())
				{
					case State.STARTED:
					{
						htmltext = (hasQuestItems(player, TISSUE_VS) && hasQuestItems(player, TISSUE_KB)) ? "31972-02.htm" : "31972-01.htm";
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
		if ((npcId == VENOMOUS_STORACE) && !hasQuestItems(player, TISSUE_VS))
		{
			giveItems(player, TISSUE_VS, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		else if ((npcId == KEL_BILETTE) && !hasQuestItems(player, TISSUE_KB))
		{
			giveItems(player, TISSUE_KB, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
