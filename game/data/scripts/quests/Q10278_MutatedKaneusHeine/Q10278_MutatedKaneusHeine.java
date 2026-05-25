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
package quests.Q10278_MutatedKaneusHeine;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * Mutated Kaneus - Heine (10278)<br>
 * Original Jython script by Gnacik on 2010-06-29.
 * @author nonom
 */
public class Q10278_MutatedKaneusHeine extends Quest
{
	// NPCs
	private static final int GOSTA = 30916;
	private static final int MINEVIA = 30907;
	private static final int BLADE_OTIS = 18562;
	private static final int WEIRD_BUNEI = 18564;
	
	// Items
	private static final int TISSUE_BO = 13834;
	private static final int TISSUE_WB = 13835;
	
	public Q10278_MutatedKaneusHeine()
	{
		super(10278);
		addStartNpc(GOSTA);
		addTalkId(GOSTA, MINEVIA);
		addKillId(BLADE_OTIS, WEIRD_BUNEI);
		registerQuestItems(TISSUE_BO, TISSUE_WB);
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
			case "30916-03.htm":
			{
				qs.startQuest();
				break;
			}
			case "30907-03.htm":
			{
				giveAdena(player, 50000, true);
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
				if ((qs != null) && qs.isStarted() && (((npcId == BLADE_OTIS) && !hasQuestItems(member, TISSUE_BO)) || ((npcId == WEIRD_BUNEI) && !hasQuestItems(member, TISSUE_WB))))
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
			case GOSTA:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() > 37) ? "30916-01.htm" : "30916-00.htm";
						break;
					}
					case State.STARTED:
					{
						htmltext = (hasQuestItems(player, TISSUE_BO) && hasQuestItems(player, TISSUE_WB)) ? "30916-05.htm" : "30916-04.htm";
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
			case MINEVIA:
			{
				switch (qs.getState())
				{
					case State.STARTED:
					{
						htmltext = (hasQuestItems(player, TISSUE_BO) && hasQuestItems(player, TISSUE_WB)) ? "30907-02.htm" : "30907-01.htm";
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
		if ((npcId == BLADE_OTIS) && !hasQuestItems(player, TISSUE_BO))
		{
			giveItems(player, TISSUE_BO, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		else if ((npcId == WEIRD_BUNEI) && !hasQuestItems(player, TISSUE_WB))
		{
			giveItems(player, TISSUE_WB, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
