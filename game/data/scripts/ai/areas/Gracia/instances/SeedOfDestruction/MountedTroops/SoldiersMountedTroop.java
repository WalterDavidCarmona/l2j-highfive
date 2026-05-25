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
package ai.areas.Gracia.instances.SeedOfDestruction.MountedTroops;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.PolygonTerritory;

import quests.Q00693_DefeatingDragonkinRemnants.Q00693_DefeatingDragonkinRemnants;

/**
 * Gracia SoD Soldiers Mounted Troop Instance
 * @author Lomka, Mobius
 */
public final class SoldiersMountedTroop extends InstanceScript
{
	private static final int TEMPLATE_ID = 124;
	private static final int MAX_PLAYERS = 9;
	private static final int MIN_LEVEL = 75;
	private static final int EDRIC = 32527;
	
	private static final int DEFEATED_TROOPS_WHITE_DRAGON_LEADER = 18785;
	private static final int DEFEATED_TROOPS_INFANTRY = 18786;
	private static final int DEFEATED_TROOPS_MAGIC_SOLDIER = 18789;
	private static final int DOORMAN = DEFEATED_TROOPS_INFANTRY;
	
	private static final int REMNANT_MACHINE = 18703;
	private static final int SEED_CONTROLLER = 32602;
	
	private static final int[] DOORS =
	{
		12240001,
		12240002
	};
	
	private static final Location ENTER_TELEPORT_LOC = new Location(-242754, 219982, -10011);
	private static final Location DOORMAN_SPAWN_LOC = new Location(-239504, 219984, -10112, 32767);
	private static final Location REMNANT_MACHINE_LOC = new Location(-238320, 219983, -10112, 0);
	
	private static final PolygonTerritory _hallZone1 = new PolygonTerritory(1);
	private static final PolygonTerritory _hallZone2 = new PolygonTerritory(2);
	
	private static final Map<Integer, Integer> HALL_1_SPAWNS = new HashMap<>();
	static
	{
		HALL_1_SPAWNS.put(DEFEATED_TROOPS_WHITE_DRAGON_LEADER, 4);
		HALL_1_SPAWNS.put(DEFEATED_TROOPS_INFANTRY, 3);
		HALL_1_SPAWNS.put(DEFEATED_TROOPS_MAGIC_SOLDIER, 3);
	}
	
	private static final Map<Integer, Integer> HALL_2_SPAWNS = new HashMap<>();
	static
	{
		HALL_2_SPAWNS.put(DEFEATED_TROOPS_INFANTRY, 4);
		HALL_2_SPAWNS.put(DEFEATED_TROOPS_MAGIC_SOLDIER, 3);
	}
	
	// @formatter:off
	private static final int[][] HALL_ZONE_1_COORDINATES =
	{
		{-240766, 219168, -10178, -9978},
		{-240755, 220795, -10178, -9978},
		{-240667, 220889, -10178, -9978},
		{-239595, 220886, -10178, -9978},
		{-239487, 220765, -10178, -9978},
		{-239490, 219170, -10178, -9978},
		{-239583, 219059, -10178, -9978},
		{-240665, 219065, -10178, -9978},
	};
	
	private static final int[][] HALL_ZONE_2_COORDINATES =
	{
		{-238353, 219165, -10175, -9975},
		{-238340, 220788, -10175, -9975},
		{-238431, 220888, -10175, -9975},
		{-238933, 220890, -10175, -9975},
		{-239027, 220789, -10175, -9975},
		{-239034, 219195, -10175, -9975},
		{-238924, 219067, -10175, -9975},
		{-238445, 219065, -10175, -9975},
	};
	// @formatter:on
	
	public SoldiersMountedTroop()
	{
		addStartNpc(EDRIC);
		addTalkId(EDRIC);
		addKillId(DOORMAN, REMNANT_MACHINE);
		for (int[] coord : HALL_ZONE_1_COORDINATES)
		{
			_hallZone1.add(coord[0], coord[1], coord[2], coord[3], 0);
		}
		
		for (int[] coord : HALL_ZONE_2_COORDINATES)
		{
			_hallZone2.add(coord[0], coord[1], coord[2], coord[3], 0);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "enter":
			{
				if ((npc.getId() == EDRIC) && checkConditions(player))
				{
					for (Player partyMember : player.getParty().getMembers())
					{
						InstanceManager.getInstance().setInstanceTime(partyMember.getObjectId(), TEMPLATE_ID, System.currentTimeMillis());
						enterInstance(partyMember, TEMPLATE_ID);
					}
					
					return "32527-entrance.html";
				}
				break;
			}
			case "DOORMAN_KILLED":
			{
				openDoor(DOORS[0], npc.getInstanceId());
				openDoor(DOORS[1], npc.getInstanceId());
				break;
			}
		}
		
		return null;
	}
	
	@Override
	protected boolean checkConditions(Player player)
	{
		final Party party = player.getParty();
		if (party == null)
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_CURRENTLY_IN_A_PARTY_SO_YOU_CANNOT_ENTER);
			return false;
		}
		
		if (party.getLeader() != player)
		{
			player.sendPacket(SystemMessageId.ONLY_A_PARTY_LEADER_CAN_MAKE_THE_REQUEST_TO_ENTER);
			return false;
		}
		
		if (party.getMemberCount() > MAX_PLAYERS)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_DUE_TO_THE_PARTY_HAVING_EXCEEDED_THE_LIMIT);
			return false;
		}
		
		for (Player partyMember : party.getMembers())
		{
			if (partyMember.getLevel() < MIN_LEVEL)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_S_LEVEL_DOES_NOT_CORRESPOND_TO_THE_REQUIREMENTS_FOR_ENTRY);
				sm.addPcName(partyMember);
				party.broadcastPacket(sm);
				return false;
			}
			
			if (!LocationUtil.checkIfInRange(1000, player, partyMember, true))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_IN_A_LOCATION_WHICH_CANNOT_BE_ENTERED_THEREFORE_IT_CANNOT_BE_PROCESSED);
				sm.addPcName(partyMember);
				party.broadcastPacket(sm);
				return false;
			}
			
			if (System.currentTimeMillis() < (InstanceManager.getInstance().getInstanceTime(partyMember.getObjectId(), TEMPLATE_ID) + 86400000 /* 24 hours */))
			{
				final NpcHtmlMessage packet = new NpcHtmlMessage();
				packet.setHtml(getHtm(player, "32527-instance-lock.html"));
				player.sendPacket(packet);
				
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_MAY_NOT_RE_ENTER_YET);
				sm.addPcName(partyMember);
				party.broadcastPacket(sm);
				return false;
			}
			
			if (partyMember.isFlyingMounted())
			{
				partyMember.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_A_SEED_WHILE_IN_A_FLYING_TRANSFORMATION_STATE);
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public void onEnterInstance(Player player, InstanceWorld world, boolean firstEntrance)
	{
		world.addAllowed(player);
		if (firstEntrance)
		{
			final Party party = player.getParty();
			final Attackable doorMan = (Attackable) addSpawn(DOORMAN, DOORMAN_SPAWN_LOC, false, 0, false, world.getInstanceId());
			doorMan.setRandomWalking(true);
			world.setParameter("doorMan", doorMan);
			
			final Attackable remnantMachine = (Attackable) addSpawn(REMNANT_MACHINE, REMNANT_MACHINE_LOC, false, 0, false, world.getInstanceId());
			remnantMachine.disableCoreAI(true);
			remnantMachine.setImmobilized(true);
			world.setParameter("remnantMachine", remnantMachine);
			
			// Spawn mobs in the 1st hall.
			for (Entry<Integer, Integer> entry : HALL_1_SPAWNS.entrySet())
			{
				for (int i = 0; i < entry.getValue(); i++)
				{
					final Location location = _hallZone1.getRandomPoint();
					location.setHeading(getRandom(65536));
					final Attackable spawn = (Attackable) addSpawn(entry.getKey(), location, false, 0, false, world.getInstanceId());
					spawn.setRandomWalking(true);
				}
			}
			
			// Spawn mobs in the 2nd hall.
			for (Entry<Integer, Integer> entry : HALL_2_SPAWNS.entrySet())
			{
				for (int i = 0; i < entry.getValue(); i++)
				{
					final Location location = _hallZone2.getRandomPoint();
					location.setHeading(getRandom(65536));
					final Attackable spawn = (Attackable) addSpawn(entry.getKey(), location, false, 0, false, world.getInstanceId());
					spawn.setRandomWalking(true);
				}
			}
			
			// Spawn temporary teleporter.
			addSpawn(SEED_CONTROLLER, ENTER_TELEPORT_LOC, false, 0, false, world.getInstanceId());
			closeDoor(DOORS[0], world.getInstanceId());
			closeDoor(DOORS[1], world.getInstanceId());
			
			if (party != null)
			{
				for (Player players : party.getMembers())
				{
					final QuestState qs = players.getQuestState(Q00693_DefeatingDragonkinRemnants.class.getSimpleName());
					if (qs != null)
					{
						qs.set("difficulty", 2);
						qs.setMemoState(2);
						qs.set("members", party.getMemberCount());
						world.addAllowed(players);
						teleportPlayer(players, ENTER_TELEPORT_LOC, world.getInstanceId(), true);
					}
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final InstanceWorld world = InstanceManager.getInstance().getWorld(npc.getInstanceId());
		if (world == null)
		{
			return;
		}
		
		final Attackable doorMan = world.getParameters().getObject("doorMan", Attackable.class);
		if (npc == doorMan)
		{
			startQuestTimer("DOORMAN_KILLED", 2000, npc, null);
		}
		
		if (npc.getId() == REMNANT_MACHINE)
		{
			for (Monster monster : world.getAliveNpcs(Monster.class))
			{
				monster.deleteMe();
			}
			
			final Party party = killer.getParty();
			if (party != null)
			{
				for (Player member : party.getMembers())
				{
					final QuestState qs = member.getQuestState(Q00693_DefeatingDragonkinRemnants.class.getSimpleName());
					if ((qs != null) && (member.getInstanceId() == world.getInstanceId()))
					{
						qs.setMemoState(3);
					}
				}
			}
			
			// Finish Instance.
			cancelQuestTimers("FAILED_IN_TIME");
			
			final Instance instance = InstanceManager.getInstance().getInstance(npc.getInstanceId());
			if (instance != null)
			{
				instance.setDuration(GeneralConfig.INSTANCE_FINISH_TIME);
				instance.setEmptyDestroyTime(0);
			}
		}
	}
}
