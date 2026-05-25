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
package quests.Q00661_MakingTheHarvestGroundsSafe;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * Making the Harvest Grounds Safe (661)
 * @author Mobius
 */
public class Q00661_MakingTheHarvestGroundsSafe extends Quest
{
	// NPC
	private static final int NORMAN = 30210;
	
	// Items
	private static final int BIG_HORNET_STING = 8283;
	private static final int CLOUD_GEM = 8284;
	private static final int YOUNG_ARANEID_CLAW = 8285;
	
	// Monsters
	private static final Map<Integer, ItemChanceHolder> MONSTER_CHANCES = new HashMap<>();
	static
	{
		MONSTER_CHANCES.put(21095, new ItemChanceHolder(BIG_HORNET_STING, 0.508)); // Giant Poison Bee
		MONSTER_CHANCES.put(21096, new ItemChanceHolder(CLOUD_GEM, 0.5)); // Cloudy Beast
		MONSTER_CHANCES.put(21097, new ItemChanceHolder(YOUNG_ARANEID_CLAW, 0.516)); // Young Araneid
	}
	
	// Misc
	private static final int MIN_LEVEL = 21;
	
	public Q00661_MakingTheHarvestGroundsSafe()
	{
		super(661);
		addStartNpc(NORMAN);
		addTalkId(NORMAN);
		addKillId(MONSTER_CHANCES.keySet());
		registerQuestItems(BIG_HORNET_STING, CLOUD_GEM, YOUNG_ARANEID_CLAW);
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
			case "30210-01.htm":
			case "30210-02.htm":
			case "30210-04.html":
			case "30210-06.html":
			{
				htmltext = event;
				break;
			}
			case "30210-03.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					htmltext = event;
				}
				break;
			}
			case "30210-08.html":
			{
				final long stingCount = getQuestItemsCount(player, BIG_HORNET_STING);
				final long gemCount = getQuestItemsCount(player, CLOUD_GEM);
				final long clawCount = getQuestItemsCount(player, YOUNG_ARANEID_CLAW);
				long reward = (57 * stingCount) + (56 * gemCount) + (60 * clawCount);
				if ((stingCount + gemCount + clawCount) >= 10)
				{
					reward += 5773;
				}
				
				takeItems(player, BIG_HORNET_STING, -1);
				takeItems(player, CLOUD_GEM, -1);
				takeItems(player, YOUNG_ARANEID_CLAW, -1);
				giveAdena(player, reward, true);
				htmltext = event;
				break;
			}
			case "30210-09.html":
			{
				qs.exitQuest(true, true);
				htmltext = event;
				break;
			}
		}
		
		return htmltext;
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
				htmltext = (talker.getLevel() >= MIN_LEVEL) ? "30210-01.htm" : "30210-02.htm";
				break;
			}
			case State.STARTED:
			{
				if (hasQuestItems(talker, BIG_HORNET_STING, CLOUD_GEM, YOUNG_ARANEID_CLAW))
				{
					htmltext = "30210-04.html";
				}
				else
				{
					htmltext = "30210-05.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, -1, 3, npc);
		if (qs != null)
		{
			final ItemChanceHolder item = MONSTER_CHANCES.get(npc.getId());
			giveItemRandomly(qs.getPlayer(), npc, item.getId(), item.getCount(), 0, item.getChance(), true);
		}
	}
}
