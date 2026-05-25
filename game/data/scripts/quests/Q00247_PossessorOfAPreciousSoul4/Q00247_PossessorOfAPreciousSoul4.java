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
package quests.Q00247_PossessorOfAPreciousSoul4;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

import quests.Q00246_PossessorOfAPreciousSoul3.Q00246_PossessorOfAPreciousSoul3;

/**
 * Possessor Of A PreciousSoul part 4 (247)<br>
 * Original Jython script by disKret.
 * @author nonom
 */
public class Q00247_PossessorOfAPreciousSoul4 extends Quest
{
	// NPCs
	private static final int CARADINE = 31740;
	private static final int LADY_OF_LAKE = 31745;
	
	// Items
	private static final int CARADINE_LETTER_LAST = 7679;
	private static final int NOBLESS_TIARA = 7694;
	
	// Location
	private static final Location CARADINE_LOC = new Location(143209, 43968, -3038);
	
	// Skill
	private static final SkillHolder MIMIRS_ELIXIR = new SkillHolder(4339, 1);
	
	public Q00247_PossessorOfAPreciousSoul4()
	{
		super(247);
		addStartNpc(CARADINE);
		addTalkId(CARADINE, LADY_OF_LAKE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState st = getQuestState(player, false);
		
		if (st == null)
		{
			return getNoQuestMsg(player);
		}
		
		if (!player.isSubClassActive())
		{
			return "no_sub.html";
		}
		
		switch (event)
		{
			case "31740-3.html":
			{
				st.startQuest();
				takeItems(player, CARADINE_LETTER_LAST, -1);
				break;
			}
			case "TELEPORT":
			{
				if (st.isCond(1))
				{
					st.setCond(2, true);
					player.teleToLocation(CARADINE_LOC, 0);
				}
				break;
			}
			case "31745-5.html":
			{
				if (st.isCond(2))
				{
					player.setNoble(true);
					addExpAndSp(player, 93836, 0);
					giveItems(player, NOBLESS_TIARA, 1);
					npc.setTarget(player);
					npc.doCast(MIMIRS_ELIXIR.getSkill());
					player.sendPacket(new SocialAction(player.getObjectId(), 3));
					st.exitQuest(false, true);
				}
				break;
			}
		}
		
		return event;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		
		if (st.isStarted() && !player.isSubClassActive())
		{
			return "no_sub.html";
		}
		
		switch (npc.getId())
		{
			case CARADINE:
			{
				switch (st.getState())
				{
					case State.CREATED:
					{
						final QuestState qs = player.getQuestState(Q00246_PossessorOfAPreciousSoul3.class.getSimpleName());
						if ((qs != null) && qs.isCompleted())
						{
							htmltext = ((player.getLevel() >= 75) ? "31740-1.htm" : "31740-2.html");
						}
						break;
					}
					case State.STARTED:
					{
						if (st.isCond(1))
						{
							htmltext = "31740-6.html";
						}
						break;
					}
					case State.COMPLETED:
					{
						htmltext = getAlreadyCompletedMsg(player);
						break;
					}
				}
				break;
			}
			case LADY_OF_LAKE:
			{
				if (st.isCond(2))
				{
					htmltext = "31745-1.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
}
