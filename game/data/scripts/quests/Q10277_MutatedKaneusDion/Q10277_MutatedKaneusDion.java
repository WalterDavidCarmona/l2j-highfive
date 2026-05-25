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
package quests.Q10277_MutatedKaneusDion;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * Mutated Kaneus - Dion (10277)<br>
 * Original Jython script by Gnacik on 2010-06-29.
 * @author nonom
 */
public class Q10277_MutatedKaneusDion extends Quest
{
	// NPCs
	private static final int LUKAS = 30071;
	private static final int MIRIEN = 30461;
	private static final int CRIMSON_HATU = 18558;
	private static final int SEER_FLOUROS = 18559;
	
	// Items
	private static final int TISSUE_CH = 13832;
	private static final int TISSUE_SF = 13833;
	
	public Q10277_MutatedKaneusDion()
	{
		super(10277);
		addStartNpc(LUKAS);
		addTalkId(LUKAS, MIRIEN);
		addKillId(CRIMSON_HATU, SEER_FLOUROS);
		registerQuestItems(TISSUE_CH, TISSUE_SF);
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
			case "30071-03.html":
			{
				qs.startQuest();
				break;
			}
			case "30461-03.html":
			{
				giveAdena(player, 20000, true);
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
				if ((qs != null) && qs.isStarted() && (((npcId == CRIMSON_HATU) && !hasQuestItems(member, TISSUE_CH)) || ((npcId == SEER_FLOUROS) && !hasQuestItems(member, TISSUE_SF))))
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
			case LUKAS:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() > 27) ? "30071-01.htm" : "30071-00.html";
						break;
					}
					case State.STARTED:
					{
						htmltext = (hasQuestItems(player, TISSUE_CH) && hasQuestItems(player, TISSUE_SF)) ? "30071-05.html" : "30071-04.html";
						break;
					}
					case State.COMPLETED:
					{
						htmltext = "30071-06.html";
						break;
					}
				}
				break;
			}
			case MIRIEN:
			{
				switch (qs.getState())
				{
					case State.STARTED:
					{
						htmltext = (hasQuestItems(player, TISSUE_CH) && hasQuestItems(player, TISSUE_SF)) ? "30461-02.html" : "30461-01.html";
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
		if ((npcId == CRIMSON_HATU) && !hasQuestItems(player, TISSUE_CH))
		{
			giveItems(player, TISSUE_CH, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		else if ((npcId == SEER_FLOUROS) && !hasQuestItems(player, TISSUE_SF))
		{
			giveItems(player, TISSUE_SF, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
