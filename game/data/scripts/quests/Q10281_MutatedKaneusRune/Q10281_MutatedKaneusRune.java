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
package quests.Q10281_MutatedKaneusRune;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * Mutated Kaneus - Rune (10281)<br>
 * Original Jython script by Gnacik on 2010-06-29.
 * @author nonom
 */
public class Q10281_MutatedKaneusRune extends Quest
{
	// NPCs
	private static final int MATHIAS = 31340;
	private static final int KAYAN = 31335;
	private static final int WHITE_ALLOSCE = 18577;
	
	// Item
	private static final int TISSUE_WA = 13840;
	
	public Q10281_MutatedKaneusRune()
	{
		super(10281);
		addStartNpc(MATHIAS);
		addTalkId(MATHIAS, KAYAN);
		addKillId(WHITE_ALLOSCE);
		registerQuestItems(TISSUE_WA);
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
			case "31340-03.htm":
			{
				qs.startQuest();
				break;
			}
			case "31335-03.htm":
			{
				giveAdena(player, 360000, true);
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
		
		if (killer.getParty() != null)
		{
			final List<Player> partyMembers = new ArrayList<>();
			for (Player member : killer.getParty().getMembers())
			{
				qs = getQuestState(member, false);
				if ((qs != null) && qs.isStarted() && !hasQuestItems(member, TISSUE_WA))
				{
					partyMembers.add(member);
				}
			}
			
			if (!partyMembers.isEmpty())
			{
				rewardItem(getRandomEntry(partyMembers));
			}
		}
		else if (qs.isStarted() && !hasQuestItems(killer, TISSUE_WA))
		{
			rewardItem(killer);
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case MATHIAS:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() > 67) ? "31340-01.htm" : "31340-00.htm";
						break;
					}
					case State.STARTED:
					{
						htmltext = hasQuestItems(player, TISSUE_WA) ? "31340-05.htm" : "31340-04.htm";
						break;
					}
					case State.COMPLETED:
					{
						htmltext = "31340-06.htm";
						break;
					}
				}
				break;
			}
			case KAYAN:
			{
				switch (qs.getState())
				{
					case State.STARTED:
					{
						htmltext = hasQuestItems(player, TISSUE_WA) ? "31335-02.htm" : "31335-01.htm";
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
	
	private void rewardItem(Player player)
	{
		giveItems(player, TISSUE_WA, 1);
		playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
	}
}
