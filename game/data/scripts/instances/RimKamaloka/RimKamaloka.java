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
package instances.RimKamaloka;

import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Mobius
 */
public class RimKamaloka extends Script
{
	// NPCs.
	private static final int START_NPC = 32484;
	private static final int REWARDER = 32485;
	
	// Reset time for all Kamaloka Default: 6:30AM on server time.
	private static final int RESET_HOUR = 6;
	private static final int RESET_MIN = 30;
	
	private static final int LOCK_TIME = 10;
	
	// Duration of the instance, minutes.
	private static final int DURATION = 20;
	
	// Time after which instance without players will be destroyed Default: 5 minutes.
	private static final int EMPTY_DESTROY_TIME = 5;
	
	// Time to destroy instance (and eject player away) Default: 10 minutes.
	private static final int EXIT_TIME = 10;
	
	// Maximum level difference between players level and Kamaloka level Default: 5
	private static final int MAX_LEVEL_DIFFERENCE = 5;
	private static final int RESPAWN_DELAY = 30;
	private static final int DESPAWN_DELAY = 10000;
	
	// Hardcoded instance ids for Kamaloka.
	// @formatter:off
	private static final int[] INSTANCE_IDS =
	{
		46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56
	};
	
	// Level of the Kamaloka.
	private static final int[] LEVEL =
	{
		25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75
	};
	
	// Teleport points into instances x, y, z.
	private static final Location[] TELEPORTS =
	{
		new Location(10025, -219868, -8021),
		new Location(15617, -219883, -8021),
		new Location(22742, -220079, -7802),
		new Location(8559, -212987, -7802),
		new Location(15867, -212994, -7802),
		new Location(23038, -213052, -8007),
		new Location(9139, -205132, -8007),
		new Location(15943, -205740, -8008),
		new Location(22343, -206237, -7991),
		new Location(41496, -219694, -8759),
		new Location(48137, -219716, -8759)
	};
	
	private static final int[][] KANABIONS =
	{
		{22452, 22453, 22454},
		{22455, 22456, 22457},
		{22458, 22459, 22460},
		{22461, 22462, 22463},
		{22464, 22465, 22466},
		{22467, 22468, 22469},
		{22470, 22471, 22472},
		{22473, 22474, 22475},
		{22476, 22477, 22478},
		{22479, 22480, 22481},
		{22482, 22483, 22484}
	};
	
	private static final int[][][] SPAWNLIST =
	{
		{
			{8971, -219546, -8021},
			{9318, -219644, -8021},
			{9266, -220208, -8021},
			{9497, -220054,-8024}
		},
		{
			{16107, -219574, -8021},
			{16769, -219885, -8021},
			{16363, -220219, -8021},
			{16610, -219523, -8021}
		},
		{
			{23019, -219730, -7803},
			{23351, -220455, -7803},
			{23900, -219864, -7803},
			{23851, -220294, -7803}
		},
		{
			{9514, -212478, -7803},
			{9236, -213348, -7803},
			{8868, -212683, -7803},
			{9719, -213042, -7803}
		},
		{
			{16925, -212811, -7803},
			{16885, -213199, -7802},
			{16487, -213339, -7803},
			{16337, -212529, -7803}
		},
		{
			{23958, -213282, -8009},
			{23292, -212782, -8012},
			{23844, -212781, -8009},
			{23533, -213301, -8009}
		},
		{
			{8828, -205518, -8009},
			{8895, -205989, -8009},
			{9398, -205967, -8009},
			{9393, -205409, -8009}
		},
		{
			{16185, -205472, -8009},
			{16808, -205929, -8009},
			{16324, -206042, -8009},
			{16782, -205454, -8009}
		},
		{
			{23476, -206310, -7991},
			{23230, -205861, -7991},
			{22644, -205888, -7994},
			{23078, -206714, -7991}
		},
		{
			{42981, -219308, -8759},
			{42320, -220160, -8759},
			{42434, -219181, -8759},
			{42101, -219550, -8759},
			{41859, -220236, -8759},
			{42881, -219942, -8759}
		},
		{
			{48770, -219304, -8759},
			{49036, -220190, -8759},
			{49363, -219814, -8759},
			{49393, -219102, -8759},
			{49618, -220490, -8759},
			{48526, -220493, -8759}
		}
	};
	
	public static final int[][] REWARDERS =
	{
		{9261, -219862, -8021},
		{16301, -219806, -8021},
		{23478, -220079, -7799},
		{9290, -212993, -7799},
		{16598, -212997, -7802},
		{23650, -213051, -8007},
		{9136, -205733, -8007},
		{16508, -205737, -8007},
		{23229, -206316, -7991},
		{42638, -219781, -8759},
		{49014, -219737, -8759}
	};
	
	private static final int[][][] REWARDS =
	{
		{ // 20-30
			null, // Grade F
			{13002, 2, 10839, 1}, // Grade D
			{13002, 2, 10838, 1}, // Grade C
			{13002, 2, 10837, 1}, // Grade B
			{13002, 2, 10836, 1}, // Grade A
			{13002, 2, 12824, 1}, // Grade S
		},
		{ // 25-35
			null,
			{13002, 3, 10838, 1},
			{13002, 3, 10837, 1},
			{13002, 3, 10836, 1},
			{13002, 3, 10840, 1},
			{13002, 3, 12825, 1}
		},
		{ // 30-40
			null,
			{13002, 3, 10841, 1},
			{13002, 3, 10842, 1},
			{13002, 3, 10843, 1},
			{13002, 3, 10844, 1},
			{13002, 3, 12826, 1}
		},
		{ // 35-45
			null,
			{13002, 5, 10842, 1},
			{13002, 5, 10843, 1},
			{13002, 5, 10844, 1},
			{13002, 5, 10845, 1},
			{13002, 5, 12827, 1}
		},
		{ // 40-50
			null,
			{13002, 7, 10846, 1},
			{13002, 7, 10847, 1},
			{13002, 7, 10848, 1},
			{13002, 7, 10849, 1},
			{13002, 7, 12828, 1}
		},
		{ // 45-55
			null,
			{13002, 8, 10847, 1},
			{13002, 8, 10848, 1},
			{13002, 8, 10849, 1},
			{13002, 8, 10850, 1},
			{13002, 8, 12829, 1}
		},
		{ // 50-60
			null,
			{13002, 10, 10851, 1},
			{13002, 10, 10852, 1},
			{13002, 10, 10853, 1},
			{13002, 10, 10854, 1},
			{13002, 10, 12830, 1}
		},
		{ // 55-65
			null,
			{13002, 12, 10852, 1},
			{13002, 12, 10853, 1},
			{13002, 12, 10854, 1},
			{13002, 12, 10855, 1},
			{13002, 12, 12831, 1}
		},
		{ // 60-70
			null,
			{13002, 13, 10856, 1},
			{13002, 13, 10857, 1},
			{13002, 13, 10858, 1},
			{13002, 13, 10859, 1},
			{13002, 13, 12832, 1}
		},
		{ // 65-75
			null,
			{13002, 15, 10857, 1},
			{13002, 15, 10858, 1},
			{13002, 15, 10859, 1},
			{13002, 15, 10860, 1},
			{13002, 15, 12833, 1}
		},
		{ // 70-80
			null,
			{13002, 17, 10861, 1},
			{13002, 17, 12834, 1},
			{13002, 17, 10862, 1},
			{13002, 17, 10863, 1},
			{13002, 17, 10864, 1}
		}
	};
	// @formatter:on
	
	private class RimKamaWorld extends InstanceWorld
	{
		public int index;
		public int KANABION;
		public int DOPPLER;
		public int VOIDER;
		
		public int kanabionsCount = 0;
		public int dopplersCount = 0;
		public int voidersCount = 0;
		public int grade = 0;
		public boolean isFinished = false;
		public boolean isRewarded = false;
		
		public ScheduledFuture<?> finishTask = null;
		public Set<Monster> spawnedMobs = ConcurrentHashMap.newKeySet();
		public Map<Integer, Long> lastAttack = new ConcurrentHashMap<>();
		public ScheduledFuture<?> despawnTask = null;
		
		public RimKamaWorld()
		{
		}
	}
	
	private RimKamaloka()
	{
		addStartNpc(START_NPC);
		addFirstTalkId(START_NPC, REWARDER);
		addTalkId(START_NPC);
		addTalkId(REWARDER);
		
		for (int[] list : KANABIONS)
		{
			addFactionCallId(list[0]);
			addAttackId(list);
			addKillId(list);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((npc == null) || (player == null))
		{
			return null;
		}
		
		switch (event)
		{
			case "npc_rim_maker001.htm":
			case "npc_rim_maker001a.htm":
			case "npc_rim_maker001b.htm":
			case "npc_rim_maker001ba.htm":
			case "npc_rim_maker007.htm":
			case "npc_rim_maker010.htm":
			case "npc_rim_maker011.htm":
			{
				return event;
			}
			case "Exit":
			{
				try
				{
					final InstanceWorld world = InstanceManager.getInstance().getWorld(npc.getInstanceId());
					if ((world instanceof RimKamaWorld) && world.isAllowed(player))
					{
						final Instance inst = InstanceManager.getInstance().getInstance(world.getInstanceId());
						teleportPlayer(player, inst.getExitLoc(), 0);
					}
				}
				catch (Exception e)
				{
					LOGGER.warning("RimKamaloka: problem with exit: " + e.getMessage());
				}
				break;
			}
			case "Reward":
			{
				try
				{
					final InstanceWorld world = InstanceManager.getInstance().getWorld(npc.getInstanceId());
					if ((world instanceof RimKamaWorld) && world.isAllowed(player))
					{
						rewardPlayer((RimKamaWorld) world, npc);
					}
				}
				catch (Exception e)
				{
					LOGGER.warning("RimKamaloka: problem with reward: " + e.getMessage());
				}
				
				return "npc_rim_gift_giver003.htm";
			}
			case "challenge":
			{
				final NpcHtmlMessage htmlPacket = new NpcHtmlMessage(npc.getObjectId());
				String htmltext = getHtm(player, "npc_rim_maker002_" + npc.getCastle().getResidenceId() + ".htm");
				if (!PremiumSystemConfig.PC_CAFE_ENABLED)
				{
					htmltext = htmltext.replace("; <font color=\"LEVEL\">1000 PC Cafe points</font> are required", "");
				}
				
				htmlPacket.setHtml(htmltext);
				player.sendPacket(htmlPacket);
				break;
			}
			case "0":
			case "1":
			case "2":
			case "3":
			case "4":
			case "5":
			case "6":
			case "7":
			case "8":
			case "9":
			case "10":
			{
				enterInstance(player, npc, Integer.parseInt(event));
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public void onFactionCall(Npc npc, Npc caller, Player attacker, boolean isPet)
	{
		// if ((npc == null) || (caller == null))
		// {
		// return;
		// }
		
		// if (npc.getId() == caller.getId())
		// {
		// return;
		// }
		
		// super.onFactionCall(npc, caller, attacker, isPet);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (npc == null)
		{
			return null;
		}
		
		final int npcId = npc.getId();
		if (npcId == START_NPC)
		{
			return npc.getCastle().getName() + ".htm";
		}
		else if (npcId == REWARDER)
		{
			final InstanceWorld tmpWorld = InstanceManager.getInstance().getWorld(npc.getInstanceId());
			if (tmpWorld instanceof RimKamaWorld)
			{
				final RimKamaWorld world = (RimKamaWorld) tmpWorld;
				if (!world.isFinished)
				{
					return "";
				}
				
				switch (world.grade)
				{
					case 0:
					{
						return "GradeF.htm";
					}
					case 1:
					{
						return "GradeD.htm";
					}
					case 2:
					{
						return "GradeC.htm";
					}
					case 3:
					{
						return "GradeB.htm";
					}
					case 4:
					{
						return "GradeA.htm";
					}
					case 5:
					{
						return "GradeS.htm";
					}
				}
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		switch (npc.getId())
		{
			case START_NPC:
			{
				return "npc_rim_maker001.htm";
			}
			case REWARDER:
			{
				return "npc_rim_gift_giver001.htm";
			}
			default:
			{
				return npc.getId() + ".htm";
			}
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isPet)
	{
		if ((npc == null) || (attacker == null))
		{
			return;
		}
		
		final InstanceWorld tmpWorld = InstanceManager.getInstance().getWorld(npc.getInstanceId());
		if (tmpWorld instanceof RimKamaWorld)
		{
			final RimKamaWorld world = (RimKamaWorld) tmpWorld;
			synchronized (world.lastAttack)
			{
				world.lastAttack.put(npc.getObjectId(), System.currentTimeMillis());
			}
			
			final int maxHp = npc.getMaxHp();
			if (npc.getCurrentHp() == maxHp)
			{
				if (((damage * 100) / maxHp) > 40)
				{
					final int npcId = npc.getId();
					final int chance = Rnd.get(100);
					int nextId = 0;
					
					if (npcId == world.KANABION)
					{
						if (chance < 5)
						{
							nextId = world.DOPPLER;
						}
					}
					else if (npcId == world.DOPPLER)
					{
						if (chance < 5)
						{
							nextId = world.DOPPLER;
						}
						else if (chance < 10)
						{
							nextId = world.VOIDER;
						}
					}
					else if (npcId == world.VOIDER)
					{
						if (chance < 5)
						{
							nextId = world.VOIDER;
						}
					}
					
					if (nextId > 0)
					{
						spawnNextMob(world, npc, nextId, attacker);
					}
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		if ((npc == null) || (player == null))
		{
			return;
		}
		
		final InstanceWorld tmpWorld = InstanceManager.getInstance().getWorld(npc.getInstanceId());
		if (tmpWorld instanceof RimKamaWorld)
		{
			final RimKamaWorld world = (RimKamaWorld) tmpWorld;
			synchronized (world.lastAttack)
			{
				world.lastAttack.remove(npc.getObjectId());
			}
			
			final int npcId = npc.getId();
			final int chance = Rnd.get(100);
			int nextId = 0;
			
			if (npcId == world.KANABION)
			{
				world.kanabionsCount++;
				if (((Attackable) npc).isOverhit())
				{
					if (chance < 30)
					{
						nextId = world.DOPPLER;
					}
					else if (chance < 40)
					{
						nextId = world.VOIDER;
					}
				}
				else if (chance < 15)
				{
					nextId = world.DOPPLER;
				}
			}
			else if (npcId == world.DOPPLER)
			{
				world.dopplersCount++;
				if (((Attackable) npc).isOverhit())
				{
					if (chance < 30)
					{
						nextId = world.DOPPLER;
					}
					else if (chance < 60)
					{
						nextId = world.VOIDER;
					}
				}
				else
				{
					if (chance < 10)
					{
						nextId = world.DOPPLER;
					}
					else if (chance < 20)
					{
						nextId = world.VOIDER;
					}
				}
			}
			else if (npcId == world.VOIDER)
			{
				world.voidersCount++;
				if (((Attackable) npc).isOverhit())
				{
					if (chance < 50)
					{
						nextId = world.VOIDER;
					}
				}
				else if (chance < 20)
				{
					nextId = world.VOIDER;
				}
			}
			
			if (nextId > 0)
			{
				spawnNextMob(world, npc, nextId, player);
			}
		}
	}
	
	/**
	 * Check if party with player as leader allowed to enter
	 * @param player party leader
	 * @param npc the Npc
	 * @param index (0-17) index of the kamaloka in arrays
	 * @return true if party allowed to enter
	 */
	private boolean checkConditions(Player player, Npc npc, int index)
	{
		final Party party = player.getParty();
		if (party != null)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_DUE_TO_THE_PARTY_HAVING_EXCEEDED_THE_LIMIT);
			return false;
		}
		
		// Get level of the instance.
		final int level = LEVEL[index];
		
		// and client name
		final String instanceName = InstanceManager.getInstance().getInstanceIdName(INSTANCE_IDS[index]);
		
		// Player level must be in range.
		if (Math.abs(player.getLevel() - level) > MAX_LEVEL_DIFFERENCE)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_S_LEVEL_DOES_NOT_CORRESPOND_TO_THE_REQUIREMENTS_FOR_ENTRY);
			sm.addPcName(player);
			player.sendPacket(sm);
			
			final NpcHtmlMessage htmlPacket = new NpcHtmlMessage(npc.getObjectId());
			htmlPacket.setHtml(getHtm(player, "npc_rim_maker004.htm"));
			player.sendPacket(htmlPacket);
			return false;
		}
		
		// Get instances reenter times for player.
		final Map<Integer, Long> instanceTimes = InstanceManager.getInstance().getAllInstanceTimes(player.getObjectId());
		if (instanceTimes != null)
		{
			for (int id : instanceTimes.keySet())
			{
				// Find instance with same name (Kamaloka or labyrinth).
				if (!instanceName.equals(InstanceManager.getInstance().getInstanceIdName(id)))
				{
					continue;
				}
				
				// If found instance still can't be reentered - exit.
				if (System.currentTimeMillis() < instanceTimes.get(id))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.C1_MAY_NOT_RE_ENTER_YET);
					sm.addPcName(player);
					player.sendPacket(sm);
					return false;
				}
			}
		}
		
		if (PremiumSystemConfig.PC_CAFE_ENABLED)
		{
			final int points = player.getPcCafePoints();
			if (points < 1000)
			{
				final NpcHtmlMessage htmlPacket = new NpcHtmlMessage(npc.getObjectId());
				htmlPacket.setHtml(getHtm(player, "npc_rim_maker003.htm"));
				player.sendPacket(htmlPacket);
				return false;
			}
			
			player.setPcCafePoints(points - 1000);
			player.sendPacket(new ExPCCafePointInfo(player.getPcCafePoints(), -1000, 0));
		}
		
		return true;
	}
	
	/**
	 * Handling enter of the players into Kamaloka
	 * @param player party leader
	 * @param npc the Npc
	 * @param index (0-17) Kamaloka index in arrays
	 */
	protected synchronized void enterInstance(Player player, Npc npc, int index)
	{
		int templateId;
		try
		{
			templateId = INSTANCE_IDS[index];
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			return;
		}
		
		// Check for existing instances for this player.
		final InstanceWorld tmpWorld = InstanceManager.getInstance().getPlayerWorld(player);
		if (tmpWorld != null) // Player already in the instance.
		{
			// But not in kamaloka.
			if (!(tmpWorld instanceof RimKamaWorld) || (tmpWorld.getTemplateId() != templateId))
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_CORRESPONDING_DUNGEON);
				return;
			}
			
			// Check for level difference again on reenter.
			final RimKamaWorld world = (RimKamaWorld) tmpWorld;
			if (Math.abs(player.getLevel() - LEVEL[world.index]) > MAX_LEVEL_DIFFERENCE)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_S_LEVEL_DOES_NOT_CORRESPOND_TO_THE_REQUIREMENTS_FOR_ENTRY);
				sm.addPcName(player);
				player.sendPacket(sm);
				return;
			}
			
			// Check what instance still exist.
			Instance inst = InstanceManager.getInstance().getInstance(world.getInstanceId());
			if (inst != null)
			{
				teleportPlayer(player, TELEPORTS[index], world.getInstanceId());
			}
		}
		else // Creating new kamaloka instance.
		{
			if (!checkConditions(player, npc, index))
			{
				return;
			}
			
			// Creating new instanceWorld, using our instanceId and templateId.
			final RimKamaWorld world = new RimKamaWorld();
			final Instance instance = InstanceManager.getInstance().createDynamicInstance(templateId);
			world.setInstance(instance);
			InstanceManager.getInstance().addWorld(world);
			
			// Set return location.
			instance.setExitLoc(new Location(player));
			
			// Set index for easy access to the arrays.
			world.index = index;
			
			// Spawn NPCs.
			spawnKama(world);
			world.finishTask = ThreadPool.schedule(new FinishTask(world), DURATION * 60000);
			world.despawnTask = ThreadPool.scheduleAtFixedRate(new DespawnTask(world), 1000, 1000);
			ThreadPool.schedule(new LockTask(world), LOCK_TIME * 60000);
			
			world.addAllowed(player);
			
			teleportPlayer(player, TELEPORTS[index], world.getInstanceId());
		}
	}
	
	/**
	 * Spawn all NPCs in kamaloka
	 * @param world instanceWorld
	 */
	private static void spawnKama(RimKamaWorld world)
	{
		int[][] spawnlist;
		final int index = world.index;
		world.KANABION = KANABIONS[index][0];
		world.DOPPLER = KANABIONS[index][1];
		world.VOIDER = KANABIONS[index][2];
		
		try
		{
			final NpcTemplate mob1 = NpcData.getInstance().getTemplate(world.KANABION);
			spawnlist = SPAWNLIST[index];
			
			Spawn spawn;
			for (int[] loc : spawnlist)
			{
				spawn = new Spawn(mob1);
				spawn.setInstanceId(world.getInstanceId());
				spawn.setXYZ(loc[0], loc[1], loc[2]);
				spawn.setHeading(-1);
				spawn.setRespawnDelay(RESPAWN_DELAY);
				spawn.setAmount(1);
				spawn.startRespawn();
				spawn.doSpawn();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void spawnNextMob(RimKamaWorld world, Npc oldNpc, int npcId, Player player)
	{
		if (world.isFinished)
		{
			return;
		}
		
		Monster monster = null;
		if (!world.spawnedMobs.isEmpty())
		{
			for (Monster mob : world.spawnedMobs)
			{
				if ((mob == null) || !mob.isDecayed() || (mob.getId() != npcId))
				{
					continue;
				}
				
				mob.setDecayed(false);
				mob.setDead(false);
				mob.overhitEnabled(false);
				mob.refreshId();
				monster = mob;
				break;
			}
		}
		
		if (monster == null)
		{
			final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
			monster = new Monster(template);
			world.spawnedMobs.add(monster);
		}
		
		synchronized (world.lastAttack)
		{
			world.lastAttack.put(monster.getObjectId(), System.currentTimeMillis());
		}
		
		monster.setCurrentHpMp(monster.getMaxHp(), monster.getMaxMp());
		monster.setHeading(oldNpc.getHeading());
		monster.setInstanceId(oldNpc.getInstanceId());
		monster.spawnMe(oldNpc.getX(), oldNpc.getY(), oldNpc.getZ() + 20);
		monster.setRunning();
		monster.addDamageHate(player, 0, 9999);
		monster.getAI().setIntention(Intention.ATTACK, player);
	}
	
	private synchronized void rewardPlayer(RimKamaWorld world, Npc npc)
	{
		if (!world.isFinished || world.isRewarded)
		{
			return;
		}
		
		world.isRewarded = true;
		
		final int[][] allRewards = REWARDS[world.index];
		world.grade = Math.min(world.grade, allRewards.length);
		final int[] reward = allRewards[world.grade];
		if (reward == null)
		{
			return;
		}
		
		for (Player player : world.getAllowed())
		{
			if ((player != null) && player.isOnline())
			{
				player.sendMessage("Grade:" + world.grade);
				for (int i = 0; i < reward.length; i += 2)
				{
					player.addItem(ItemProcessType.REWARD, reward[i], reward[i + 1], npc, true);
				}
			}
		}
	}
	
	private class LockTask implements Runnable
	{
		private final RimKamaWorld _world;
		
		LockTask(RimKamaWorld world)
		{
			_world = world;
		}
		
		@Override
		public void run()
		{
			if (_world != null)
			{
				final Calendar reenter = Calendar.getInstance();
				reenter.set(Calendar.MINUTE, RESET_MIN);
				
				// If time is >= RESET_HOUR - roll to the next day.
				if (reenter.get(Calendar.HOUR_OF_DAY) >= RESET_HOUR)
				{
					reenter.roll(Calendar.DATE, true);
				}
				
				reenter.set(Calendar.HOUR_OF_DAY, RESET_HOUR);
				
				final SystemMessage sm = new SystemMessage(SystemMessageId.INSTANT_ZONE_S1_S_ENTRY_HAS_BEEN_RESTRICTED_YOU_CAN_CHECK_THE_NEXT_POSSIBLE_ENTRY_TIME_BY_USING_THE_COMMAND_INSTANCEZONE);
				sm.addString(InstanceManager.getInstance().getInstanceIdName(_world.getTemplateId()));
				
				// Set instance reenter time for all allowed players.
				boolean found = false;
				for (Player player : _world.getAllowed())
				{
					if ((player != null) && player.isOnline())
					{
						found = true;
						InstanceManager.getInstance().setInstanceTime(player.getObjectId(), _world.getTemplateId(), reenter.getTimeInMillis());
						player.sendPacket(sm);
					}
				}
				
				if (!found)
				{
					_world.isFinished = true;
					_world.spawnedMobs.clear();
					_world.lastAttack.clear();
					if (_world.finishTask != null)
					{
						_world.finishTask.cancel(false);
						_world.finishTask = null;
					}
					
					if (_world.despawnTask != null)
					{
						_world.despawnTask.cancel(false);
						_world.despawnTask = null;
					}
					
					InstanceManager.getInstance().destroyInstance(_world.getInstanceId());
				}
			}
		}
	}
	
	private class FinishTask implements Runnable
	{
		private final RimKamaWorld _world;
		
		FinishTask(RimKamaWorld world)
		{
			_world = world;
		}
		
		@Override
		public void run()
		{
			if (_world != null)
			{
				_world.isFinished = true;
				if (_world.despawnTask != null)
				{
					_world.despawnTask.cancel(false);
					_world.despawnTask = null;
				}
				
				_world.spawnedMobs.clear();
				_world.lastAttack.clear();
				
				// Destroy instance after EXIT_TIME.
				final Instance inst = InstanceManager.getInstance().getInstance(_world.getInstanceId());
				if (inst != null)
				{
					inst.removeNpcs();
					inst.setDuration(EXIT_TIME * 60000);
					if (inst.getPlayers().isEmpty())
					{
						inst.setDuration(EMPTY_DESTROY_TIME * 60000);
					}
					else
					{
						inst.setDuration(EXIT_TIME * 60000);
						inst.setEmptyDestroyTime(EMPTY_DESTROY_TIME * 60000);
					}
				}
				
				// Calculate reward.
				if (_world.kanabionsCount < 10)
				{
					_world.grade = 0;
				}
				else
				{
					_world.grade = Math.min(((_world.dopplersCount + (2 * _world.voidersCount)) / _world.kanabionsCount) + 1, 5);
				}
				
				final int index = _world.index;
				
				// Spawn rewarder NPC.
				addSpawn(REWARDER, REWARDERS[index][0], REWARDERS[index][1], REWARDERS[index][2], 0, false, 0, false, _world.getInstanceId());
			}
		}
	}
	
	private class DespawnTask implements Runnable
	{
		private final RimKamaWorld _world;
		
		DespawnTask(RimKamaWorld world)
		{
			_world = world;
		}
		
		@Override
		public void run()
		{
			if ((_world != null) && !_world.isFinished && !_world.lastAttack.isEmpty() && !_world.spawnedMobs.isEmpty())
			{
				final long time = System.currentTimeMillis();
				for (Monster mob : _world.spawnedMobs)
				{
					if ((mob == null) || mob.isDead() || mob.isInvisible())
					{
						continue;
					}
					
					if (_world.lastAttack.containsKey(mob.getObjectId()) && ((time - _world.lastAttack.get(mob.getObjectId())) > DESPAWN_DELAY))
					{
						mob.deleteMe();
						synchronized (_world.lastAttack)
						{
							_world.lastAttack.remove(mob.getObjectId());
						}
					}
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		new RimKamaloka();
	}
}
