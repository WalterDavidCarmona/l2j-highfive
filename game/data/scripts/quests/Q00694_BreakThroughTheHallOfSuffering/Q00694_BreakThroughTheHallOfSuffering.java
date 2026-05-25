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
package quests.Q00694_BreakThroughTheHallOfSuffering;

import java.util.Calendar;

import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Q00694_BreakThroughTheHallOfSuffering extends Quest
{
	private static final int TEPIOS = 32603;
	private static final int TEPIOS2 = 32530;
	private static final int MARK = 13691;
	private static final int SOE = 736;
	private static final int TEMPLATE_ID = 115;
	
	public Q00694_BreakThroughTheHallOfSuffering()
	{
		super(694);
		addStartNpc(TEPIOS);
		addTalkId(TEPIOS);
		addTalkId(TEPIOS2);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState qs = player.getQuestState(getName());
		if (qs == null)
		{
			return htmltext;
		}
		
		if (event.equals("32603-02.html"))
		{
			qs.startQuest();
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if ((player.getLevel() >= 75) && (player.getLevel() <= 82))
				{
					htmltext = "32603-01.htm";
				}
				else
				{
					htmltext = "32603-00.html";
					qs.exitQuest(true);
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case TEPIOS:
					{
						htmltext = "32603-01a.html";
						break;
					}
					case TEPIOS2:
					{
						final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
						if ((world != null) && (world.getTemplateId() == TEMPLATE_ID))
						{
							final int tag = world.getParameters().getInt("tag", -1);
							if (tag == -1)
							{
								htmltext = "32530-11.html";
							}
							else if ((player.getParty() != null) && (player.getParty().getLeaderObjectId() == player.getObjectId()))
							{
								for (Player member : player.getParty().getMembers())
								{
									final QuestState qs1 = member.getQuestState(getName());
									if (qs1 != null)
									{
										if (tag == 13777)
										{
											if (getQuestItemsCount(member, MARK) == 0)
											{
												giveItems(member, MARK, 1);
											}
											
											giveItems(member, 13777, 1);
											giveItems(member, SOE, 1);
											qs1.unset("cond");
											qs1.exitQuest(true);
											playSound(member, "ItemSound.quest_finish");
											htmltext = "32530-00.html";
											finishInstance(player);
										}
										else if (tag == 13778)
										{
											if (getQuestItemsCount(member, MARK) == 0)
											{
												giveItems(member, MARK, 1);
											}
											
											giveItems(member, 13778, 1);
											giveItems(member, SOE, 1);
											qs1.unset("cond");
											qs1.exitQuest(true);
											playSound(member, "ItemSound.quest_finish");
											htmltext = "32530-01.html";
											finishInstance(player);
										}
										else if (tag == 13779)
										{
											if (getQuestItemsCount(member, MARK) == 0)
											{
												giveItems(member, MARK, 1);
											}
											
											giveItems(member, 13779, 1);
											giveItems(member, SOE, 1);
											qs1.unset("cond");
											qs1.exitQuest(true);
											playSound(member, "ItemSound.quest_finish");
											htmltext = "32530-02.html";
											finishInstance(player);
										}
										else if (tag == 13780)
										{
											if (getQuestItemsCount(member, MARK) == 0)
											{
												giveItems(member, MARK, 1);
											}
											
											giveItems(member, 13780, 1);
											giveItems(member, SOE, 1);
											qs1.unset("cond");
											qs1.exitQuest(true);
											playSound(member, "ItemSound.quest_finish");
											htmltext = "32530-03.html";
											finishInstance(player);
										}
										else if (tag == 13781)
										{
											if (getQuestItemsCount(member, MARK) == 0)
											{
												giveItems(member, MARK, 1);
											}
											
											giveItems(member, 13781, 1);
											giveItems(member, SOE, 1);
											qs1.unset("cond");
											qs1.exitQuest(true);
											playSound(member, "ItemSound.quest_finish");
											htmltext = "32530-04.html";
											finishInstance(player);
										}
										else if (tag == 13782)
										{
											if (getQuestItemsCount(member, MARK) == 0)
											{
												giveItems(member, MARK, 1);
											}
											
											giveItems(member, 13782, 1);
											giveItems(member, SOE, 1);
											qs1.unset("cond");
											qs1.exitQuest(true);
											playSound(member, "ItemSound.quest_finish");
											htmltext = "32530-05.html";
											finishInstance(player);
										}
										else if (tag == 13783)
										{
											if (getQuestItemsCount(member, MARK) == 0)
											{
												giveItems(member, MARK, 1);
											}
											
											giveItems(member, 13783, 1);
											giveItems(member, SOE, 1);
											qs1.unset("cond");
											qs1.exitQuest(true);
											playSound(member, "ItemSound.quest_finish");
											htmltext = "32530-06.html";
											finishInstance(player);
										}
										else if (tag == 13784)
										{
											if (getQuestItemsCount(member, MARK) == 0)
											{
												giveItems(member, MARK, 1);
											}
											
											giveItems(member, 13784, 1);
											giveItems(member, SOE, 1);
											qs1.unset("cond");
											qs1.exitQuest(true);
											playSound(member, "ItemSound.quest_finish");
											htmltext = "32530-07.html";
											finishInstance(player);
										}
										else if (tag == 13785)
										{
											if (getQuestItemsCount(member, MARK) == 0)
											{
												giveItems(member, MARK, 1);
											}
											
											giveItems(member, 13785, 1);
											giveItems(member, SOE, 1);
											qs1.unset("cond");
											qs1.exitQuest(true);
											playSound(member, "ItemSound.quest_finish");
											htmltext = "32530-08.html";
											finishInstance(player);
										}
										else if (tag == 13786)
										{
											if (getQuestItemsCount(member, MARK) == 0)
											{
												giveItems(member, MARK, 1);
											}
											
											giveItems(member, 13786, 1);
											giveItems(member, SOE, 1);
											qs1.unset("cond");
											qs1.exitQuest(true);
											playSound(member, "ItemSound.quest_finish");
											htmltext = "32530-09.html";
											finishInstance(player);
										}
										else
										{
											htmltext = "32530-11.html";
										}
									}
								}
							}
							else
							{
								return "32530-10.html";
							}
						}
						else
						{
							htmltext = "32530-11.html";
						}
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	private void finishInstance(Player player)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		final Calendar reenter = Calendar.getInstance();
		reenter.set(Calendar.MINUTE, 30);
		if (reenter.get(Calendar.HOUR_OF_DAY) >= 6)
		{
			reenter.add(Calendar.DATE, 1);
		}
		
		reenter.set(Calendar.HOUR_OF_DAY, 6);
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.INSTANT_ZONE_S1_S_ENTRY_HAS_BEEN_RESTRICTED_YOU_CAN_CHECK_THE_NEXT_POSSIBLE_ENTRY_TIME_BY_USING_THE_COMMAND_INSTANCEZONE);
		sm.addInstanceName(TEMPLATE_ID);
		
		for (Player plr : world.getAllowed())
		{
			if (plr != null)
			{
				InstanceManager.getInstance().setInstanceTime(plr.getObjectId(), TEMPLATE_ID, reenter.getTimeInMillis());
				if (plr.isOnline())
				{
					plr.sendPacket(sm);
				}
			}
		}
		
		final Instance inst = InstanceManager.getInstance().getInstance(world.getInstanceId());
		inst.setDuration(5 * 60000);
		inst.setEmptyDestroyTime(0);
	}
}
