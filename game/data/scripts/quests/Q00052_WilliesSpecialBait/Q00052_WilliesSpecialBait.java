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
package quests.Q00052_WilliesSpecialBait;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * Willie's Special Bait (52)<br>
 * Original Jython script by Kilkenny.
 * @author nonom
 */
public class Q00052_WilliesSpecialBait extends Quest
{
	// NPCs
	private static final int WILLIE = 31574;
	private static final int TARLK_BASILISK = 20573;
	
	// Items
	private static final int TARLK_EYE = 7623;
	private static final int EARTH_FISHING_LURE = 7612;
	
	public Q00052_WilliesSpecialBait()
	{
		super(52);
		addStartNpc(WILLIE);
		addTalkId(WILLIE);
		addKillId(TARLK_BASILISK);
		registerQuestItems(TARLK_EYE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return getNoQuestMsg(player);
		}
		
		String htmltext = event;
		switch (event)
		{
			case "31574-03.htm":
			{
				qs.startQuest();
				break;
			}
			case "31574-07.html":
			{
				if (qs.isCond(2) && (getQuestItemsCount(player, TARLK_EYE) >= 100))
				{
					htmltext = "31574-06.htm";
					giveItems(player, EARTH_FISHING_LURE, 4);
					qs.exitQuest(false, true);
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return;
		}
		
		final QuestState qs = getQuestState(partyMember, false);
		if (getQuestItemsCount(player, TARLK_EYE) < 100)
		{
			if (getRandom(100) < 33)
			{
				rewardItems(player, TARLK_EYE, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
		
		if (getQuestItemsCount(player, TARLK_EYE) >= 100)
		{
			qs.setCond(2, true);
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (qs.getState())
		{
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
			case State.CREATED:
			{
				htmltext = (player.getLevel() >= 48) ? "31574-01.htm" : "31574-02.html";
				break;
			}
			case State.STARTED:
			{
				htmltext = (qs.isCond(1)) ? "31574-05.html" : "31574-04.html";
				break;
			}
		}
		
		return htmltext;
	}
}
