/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package quests.Q00693_DefeatingDragonkinRemnants;

import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.SeedOfDestructionManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Quest 693 - Defeating Dragonkin Remnants
 * @author Lomka
 */
public class Q00693_DefeatingDragonkinRemnants extends Quest
{
	private static final int EDRIC = 32527;
	private static final int MIN_LEVEL = 75;
	private static Location ENTER_TELEPORT_LOC = new Location(-242754, 219982, -10011);
	
	public Q00693_DefeatingDragonkinRemnants()
	{
		super(693);
		addStartNpc(EDRIC);
		addFirstTalkId(EDRIC);
		addTalkId(EDRIC);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (SeedOfDestructionManager.getInstance().getSoDState() == 2)
		{
			return "32527-00.html";
		}
		
		return "32527-00a.html";
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		if (npc.getId() == EDRIC)
		{
			if (talker.getLevel() < 75)
			{
				htmltext = "32527-lowlevel.htm";
			}
			else if (qs.getMemoState() < 1)
			{
				htmltext = "32527-01.htm";
			}
			else if (qs.isStarted() && (qs.getMemoState() >= 1))
			{
				final Party party = talker.getParty();
				if (qs.getMemoState() >= 3)
				{
					if (qs.isMemoState(3) && rewardPlayer(qs, qs.getInt("difficulty"), qs.getInt("members")))
					{
						htmltext = "32527-reward.html";
					}
					else
					{
						htmltext = "32527-noreward.html";
					}
					
					qs.unset("difficulty");
					qs.unset("members");
					playSound(talker, QuestSound.ITEMSOUND_QUEST_FINISH);
					qs.exitQuest(true);
				}
				else if (qs.isMemoState(2))
				{
					htmltext = "32527-11.html";
				}
				else if (party == null)
				{
					htmltext = "32527-noparty.html";
				}
				else if (!party.getLeader().equals(talker))
				{
					htmltext = getHtm(talker, "32527-noleader.html");
					htmltext = htmltext.replace("%leader%", party.getLeader().getName());
				}
				else
				{
					for (Player member : party.getMembers())
					{
						final QuestState questState = getQuestState(member, false);
						if ((questState == null) || (!questState.isStarted()) || (!questState.isMemoState(1)))
						{
							htmltext = getHtm(talker, "32527-noquest.html");
							htmltext = htmltext.replace("%member%", member.getName());
							return htmltext;
						}
					}
					
					htmltext = "32527-06.htm";
				}
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = event;
		if (qs == null)
		{
			return null;
		}
		
		switch (event)
		{
			case "32527-05.htm":
			{
				if (checkInstances(player))
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ACCEPT);
					if (player.getLevel() >= MIN_LEVEL)
					{
						qs.startQuest();
						qs.setMemoState(1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ACCEPT);
						htmltext = event;
					}
				}
				else
				{
					htmltext = "32527-12.html";
				}
				break;
			}
			case "reenter":
			{
				htmltext = "";
				if (qs.getInt("difficulty") >= 1)
				{
					final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
					if (world != null) // player already in the instance
					{
						if ((world.getTemplateId() >= 123) && (world.getTemplateId() <= 126))
						{
							teleportPlayer(player, ENTER_TELEPORT_LOC, world.getInstanceId(), true);
						}
						else
						{
							player.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_CORRESPONDING_DUNGEON);
						}
					}
					else
					{
						htmltext = "32527-noreward.html";
						qs.unset("difficulty");
						qs.unset("members");
						playSound(player, QuestSound.ITEMSOUND_QUEST_FINISH);
						qs.exitQuest(true);
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	private boolean checkInstances(Player talker)
	{
		if ((System.currentTimeMillis() < InstanceManager.getInstance().getInstanceTime(talker.getObjectId(), 123)) //
			|| (System.currentTimeMillis() < InstanceManager.getInstance().getInstanceTime(talker.getObjectId(), 124)) //
			|| (System.currentTimeMillis() < InstanceManager.getInstance().getInstanceTime(talker.getObjectId(), 125)) //
			|| (System.currentTimeMillis() < InstanceManager.getInstance().getInstanceTime(talker.getObjectId(), 126)))
		{
			return false;
		}
		
		return true;
	}
	
	private boolean rewardPlayer(QuestState qs, int difficulty, int memberCount)
	{
		if (getRandom(1000) < ((10000 / (memberCount * 10)) * (1 + (difficulty * 2))))
		{
			if (difficulty == 4)
			{
				giveItems(qs.getPlayer(), 14638, 1L); // Best Quality Battle Reward Chest
			}
			else if (difficulty == 3)
			{
				giveItems(qs.getPlayer(), 14637, 1L); // High-Grade Battle Reward Chest
			}
			else if (difficulty == 2)
			{
				giveItems(qs.getPlayer(), 14636, 1L); // Middle Quality Battle Reward Chest
			}
			else if (difficulty == 1)
			{
				giveItems(qs.getPlayer(), 14635, 1L); // Low Quality Battle Reward Chest
			}
			
			return true;
		}
		
		return false;
	}
}
