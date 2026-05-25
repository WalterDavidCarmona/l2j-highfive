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
package instances.PailakaInjuredDragon;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.ai.Action;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.SpecialCamera;
import org.l2jmobius.gameserver.util.LocationUtil;

import quests.Q00144_PailakaInjuredDragon.Q00144_PailakaInjuredDragon;

/**
 * Pailaka Injured Dragon instance zone.
 * @author Zoey76, Mobius
 */
public class PailakaInjuredDragon extends InstanceScript
{
	// NPCs
	private static final int KETRA_ORC_SUPPORTER_2 = 32512;
	
	// Monsters
	private static final int LATANA = 18660;
	private static final int LATANA_SKILL_USE = 18661;
	private static final int INJURED_DRAGON_CAMERA_1 = 18603;
	private static final int INJURED_DRAGON_CAMERA_2 = 18604;
	
	// Animals
	private static final int GRAZING_ANTELOPE = 18637;
	private static final int GRAZING_BANDERSNATCH = 18643;
	private static final int GRAZING_FLAVA = 18647;
	private static final int GRAZING_ELDER_ANTELOPE = 18651;
	
	// Guards
	private static final int VARKA_SILENOS_RECRUIT = 18635;
	private static final int VARKA_SILENOS_FOOTMAN = 18636;
	private static final int VARKA_SILENOS_WARRIOR = 18642;
	private static final int VARKA_SILENOS_OFFICER = 18646;
	private static final int VARKA_SILENOS_GREAT_MAGUS = 18649;
	private static final int VARKA_SILENOS_GENERAL = 18650;
	private static final int VARKA_ELITE_GUARD = 18653;
	private static final int VARKA_COMMANDER = 18654;
	private static final int VARKA_HEAD_GUARD = 18655;
	private static final int PROPHET_GUARD = 18657;
	
	// Wizards
	private static final int VARKA_SILENOS_SHAMAN = 18640;
	private static final int VARKA_SILENOS_PRIEST = 18641;
	private static final int VARKA_SILENOS_MEDIUM = 18644;
	private static final int VARKA_SILENOS_MAGUS = 18645;
	private static final int VARKA_SILENOS_SEER = 18648;
	private static final int VARKA_SILENOS_GREAT_SEER = 18652;
	private static final int VARKA_HEAD_MAGUS = 18656;
	private static final int DISCIPLE_OF_PROPHET = 18658;
	private static final int VARKA_PROPHET = 18659;
	
	// Skills
	private static final SkillHolder ANGER = new SkillHolder(5718, 1);
	private static final SkillHolder PRESENTATION_THE_RISE_OF_LATANA = new SkillHolder(5759, 1);
	private static final SkillHolder STUN = new SkillHolder(5716, 1);
	private static final SkillHolder ELECTRIC_FLAME = new SkillHolder(5715, 1);
	private static final SkillHolder FIRE_BREATH = new SkillHolder(5717, 1);
	
	// Drops
	private static final int PAILAKA_INSTANT_SHIELD = 13032;
	private static final int QUICK_HEALING_POTION = 13033;
	
	// Items
	private static final int SPEAR_OF_SILENOS = 13052;
	private static final int ENHANCED_SPEAR_OF_SILENOS = 13053;
	private static final int WEAPON_UPGRADE_STAGE_1 = 13056;
	private static final int WEAPON_UPGRADE_STAGE_2 = 13057;
	
	// AI Parameters
	private static final double HP_ANGER_ACTIVATION = 0.3;
	
	// Locations
	private static final Location ENTRY_POINT = new Location(125738, -40933, -3770);
	
	// Misc
	private static final int INSTANCE_ID = 45;
	// @formatter:off
	// Zones
	private static final Map<Integer, int[]> NOEXIT_ZONES = new HashMap<>();
	static
	{
		NOEXIT_ZONES.put(200001, new int[]{123167, -45743, -3023});
		NOEXIT_ZONES.put(200002, new int[]{117783, -46398, -2560});
		NOEXIT_ZONES.put(200003, new int[]{116791, -51556, -2584});
		NOEXIT_ZONES.put(200004, new int[]{117993, -52505, -2480});
		NOEXIT_ZONES.put(200005, new int[]{113226, -44080, -2776});
		NOEXIT_ZONES.put(200006, new int[]{110326, -45016, -2444});
		NOEXIT_ZONES.put(200007, new int[]{118341, -55951, -2280});
		NOEXIT_ZONES.put(200008, new int[]{110127, -41562, -2332});
	}
	// @formatter:on
	
	private PailakaInjuredDragon()
	{
		addSpawnId(LATANA, LATANA_SKILL_USE, INJURED_DRAGON_CAMERA_1, INJURED_DRAGON_CAMERA_2);
		addCreatureSeeId(LATANA, INJURED_DRAGON_CAMERA_1);
		addAttackId(LATANA);
		addKillId(LATANA);
		addKillId(GRAZING_ANTELOPE, GRAZING_BANDERSNATCH, GRAZING_FLAVA, GRAZING_ELDER_ANTELOPE);
		addKillId(VARKA_SILENOS_FOOTMAN, VARKA_SILENOS_RECRUIT, VARKA_SILENOS_WARRIOR, VARKA_ELITE_GUARD, VARKA_COMMANDER, VARKA_SILENOS_OFFICER, VARKA_SILENOS_GREAT_MAGUS, VARKA_SILENOS_GENERAL, VARKA_PROPHET, VARKA_HEAD_GUARD, PROPHET_GUARD);
		addEnterZoneId(NOEXIT_ZONES.keySet());
	}
	
	@Override
	protected void onEnterInstance(Player player, InstanceWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player);
		}
		
		teleportPlayer(player, ENTRY_POINT, world.getInstanceId(), false);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "enter":
			{
				enterInstance(player, INSTANCE_ID);
				break;
			}
			case "LATANA_500":
			{
				startQuestTimer("LATANA_501", 1000, npc, player);
				break;
			}
			case "LATANA_501":
			{
				// TODO: npc.addEffectActionDesire(npc, 0, 91 * 1000 / 30, 10000);
				startQuestTimer("LATANA_502", 3000, npc, player);
				break;
			}
			case "LATANA_502":
			{
				npc.setTarget(player);
				npc.doCast(PRESENTATION_THE_RISE_OF_LATANA.getSkill());
				startQuestTimer("LATANA_503", 9700, npc, player);
				break;
			}
			case "LATANA_503":
			{
				npc.setTarget(player);
				npc.doCast(STUN.getSkill());
				startQuestTimer("LATANA_504", 6030, npc, player);
				break;
			}
			case "LATANA_504":
			{
				startQuestTimer("LATANA_505", 4000, npc, player);
				break;
			}
			case "LATANA_505":
			{
				startQuestTimer("LATANA_2000", 1000, npc, player);
				break;
			}
			case "LATANA_600":
			{
				startQuestTimer("LATANA_602", 2000, npc, player);
				break;
			}
			case "LATANA_602":
			{
				npc.setTarget(player);
				npc.doCast(ELECTRIC_FLAME.getSkill());
				startQuestTimer("LATANA_603", 2500, npc, player);
				break;
			}
			case "LATANA_603":
			{
				npc.setTarget(player);
				npc.doCast(STUN.getSkill());
				startQuestTimer("LATANA_604", 6030, npc, player);
				break;
			}
			case "LATANA_604":
			{
				startQuestTimer("LATANA_2000", 6000, npc, player);
				break;
			}
			case "LATANA_2000":
			{
				if (player == null)
				{
					startQuestTimer("LATANA_2000", 3000, npc, null);
				}
				else
				{
					if (npc.calculateDistance2D(player) < 100)
					{
						if (getRandom(100) < 30)
						{
							npc.setTarget(player);
							npc.doCast(ELECTRIC_FLAME.getSkill());
						}
						else
						{
							addAttackDesire(npc, player, 1000);
						}
					}
					else if (getRandom(100) < 50)
					{
						// TODO: Implement check.
						// if (npc.inMyTerritory(player)) {
						final Npc latanSkillUse = addSpawn(LATANA_SKILL_USE, player, false, 0, false, player.getInstanceId());
						latanSkillUse.getVariables().set("param1", npc);
						// }
					}
					else
					{
						npc.setTarget(player);
						npc.doCast(FIRE_BREATH.getSkill());
					}
					
					startQuestTimer("LATANA_2000", 6000, npc, player);
				}
				break;
			}
			case "LATANA_4000":
			{
				npc.getVariables().set("i_ai0", false);
				break;
			}
			case "LATANA_9000":
			{
				addSpawn(KETRA_ORC_SUPPORTER_2, npc.getX() + 100, npc.getY() + 100, npc.getZ(), 0, false, 0, false, npc.getInstanceId());
				npc.decayMe();
				break;
			}
			case "LATANA_SKILL_USE_1002":
			{
				startQuestTimer("SCE_BOSS_2ND_SKILL", 2000, npc, player);
				startQuestTimer("LATANA_SKILL_USE_2002", 5000, npc, player);
				break;
			}
			case "LATANA_SKILL_USE_2002":
			{
				// TODO: Check if we should use npc.doDie(null) or npc.decayMe()
				// npc.decayMe();
				break;
			}
			// Cameras
			case "INJURED_DRAGON_CAMERA_1_1000":
			{
				player.sendPacket(new SpecialCamera(npc, 600, 200, 5, 0, 15000, 10000, -10, 8, 1, 1, 1));
				startQuestTimer("INJURED_DRAGON_CAMERA_1_1001", 2000, npc, player);
				break;
			}
			case "INJURED_DRAGON_CAMERA_1_1001":
			{
				player.sendPacket(new SpecialCamera(npc, 400, 200, 5, 4000, 15000, 10000, -10, 8, 1, 1, 0));
				startQuestTimer("INJURED_DRAGON_CAMERA_1_1002", 4000, npc, player);
				break;
			}
			case "INJURED_DRAGON_CAMERA_1_1002":
			{
				player.sendPacket(new SpecialCamera(npc, 300, 195, 4, 1500, 15000, 10000, -5, 10, 1, 1, 0));
				startQuestTimer("INJURED_DRAGON_CAMERA_1_1003", 1700, npc, player);
				break;
			}
			case "INJURED_DRAGON_CAMERA_1_1003":
			{
				player.sendPacket(new SpecialCamera(npc, 130, 2, 5, 0, 15000, 10000, 0, 0, 1, 0, 1));
				startQuestTimer("INJURED_DRAGON_CAMERA_1_1004", 2000, npc, player);
				break;
			}
			case "INJURED_DRAGON_CAMERA_1_1004":
			{
				player.sendPacket(new SpecialCamera(npc, 220, 0, 4, 800, 15000, 10000, 5, 10, 1, 0, 0));
				startQuestTimer("INJURED_DRAGON_CAMERA_1_1005", 2000, npc, player);
				break;
			}
			case "INJURED_DRAGON_CAMERA_1_1005":
			{
				player.sendPacket(new SpecialCamera(npc, 250, 185, 5, 4000, 15000, 10000, -5, 10, 1, 1, 0));
				startQuestTimer("INJURED_DRAGON_CAMERA_1_1006", 4000, npc, player);
				break;
			}
			case "INJURED_DRAGON_CAMERA_1_1006":
			{
				player.sendPacket(new SpecialCamera(npc, 200, 0, 5, 2000, 15000, 10000, 0, 25, 1, 0, 0));
				startQuestTimer("INJURED_DRAGON_CAMERA_1_1007", 4530, npc, player);
				break;
			}
			case "INJURED_DRAGON_CAMERA_1_1007":
			{
				player.sendPacket(new SpecialCamera(npc, 300, -3, 5, 3500, 15000, 6000, 0, 6, 1, 0, 0));
				
				// startQuestTimer("INJURED_DRAGON_CAMERA_1_9999", 10000, npc, player);
				break;
			}
			case "INJURED_DRAGON_CAMERA_1_2000":
			{
				player.sendPacket(new SpecialCamera(npc, 250, 0, 6, 0, 15000, 10000, 2, 0, 1, 0, 1));
				startQuestTimer("INJURED_DRAGON_CAMERA_1_2001", 2000, npc, player);
				break;
			}
			case "INJURED_DRAGON_CAMERA_1_2001":
			{
				player.sendPacket(new SpecialCamera(npc, 230, 0, 5, 2000, 15000, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("INJURED_DRAGON_CAMERA_1_2002", 2500, npc, player);
				break;
			}
			case "INJURED_DRAGON_CAMERA_1_2002":
			{
				player.sendPacket(new SpecialCamera(npc, 180, 175, 2, 1500, 15000, 10000, 0, 10, 1, 1, 0));
				startQuestTimer("INJURED_DRAGON_CAMERA_1_2003", 1500, npc, player);
				break;
			}
			case "INJURED_DRAGON_CAMERA_1_2003":
			{
				player.sendPacket(new SpecialCamera(npc, 300, 180, 5, 1500, 15000, 3000, 0, 6, 1, 1, 0));
				startQuestTimer("INJURED_DRAGON_CAMERA_1_9999", 3000, npc, player);
				break;
			}
			case "INJURED_DRAGON_CAMERA_1_9999":
			{
				final InstanceWorld world = InstanceManager.getInstance().getWorld(npc.getInstanceId());
				if (world != null)
				{
					final Npc latana = world.getNpc(LATANA);
					if (latana != null)
					{
						latana.setInvul(false);
						latana.setParalyzed(false);
					}
				}
				break;
			}
			case "INJURED_DRAGON_CAMERA_1_3000":
			{
				// TODO: npc.lookNeighbor(2000);
				// startQuestTimer("INJURED_DRAGON_CAMERA_1_3000", 10000, npc, player);
				break;
			}
			case "INJURED_DRAGON_CAMERA_2_1000":
			{
				final InstanceWorld world = InstanceManager.getInstance().getWorld(npc.getInstanceId());
				if (world != null)
				{
					final Player plr = world.getAllowed().getFirst();
					plr.sendPacket(new SpecialCamera(npc, 450, 200, 3, 0, 15000, 10000, -15, 20, 1, 1, 1));
					startQuestTimer("INJURED_DRAGON_CAMERA_2_1001", 100, npc, plr);
				}
				break;
			}
			case "INJURED_DRAGON_CAMERA_2_1001":
			{
				player.sendPacket(new SpecialCamera(npc, 350, 200, 5, 5600, 15000, 10000, -15, 10, 1, 1, 0));
				startQuestTimer("INJURED_DRAGON_CAMERA_2_1002", 5600, npc, player);
				break;
			}
			case "INJURED_DRAGON_CAMERA_2_1002":
			{
				player.sendPacket(new SpecialCamera(npc, 360, 200, 5, 1000, 15000, 2000, -15, 10, 1, 1, 0));
				break;
			}
			case "SCE_BOSS_2ND_SKILL":
			{
				final WorldObject target = npc.getTarget();
				if ((target != null) && (player != null) && (npc.calculateDistance2D(player) < 900))
				{
					npc.setTarget(player);
					npc.doCast(STUN.getSkill());
				}
				break;
			}
			case "SCE_RATANA_CAMERA_START":
			{
				switch (npc.getScriptValue())
				{
					case 1:
					{
						startQuestTimer("INJURED_DRAGON_CAMERA_1_1000", 10, npc, player);
					}
					case 2:
					{
						startQuestTimer("INJURED_DRAGON_CAMERA_1_2000", 10, npc, player);
					}
				}
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		switch (npc.getId())
		{
			case LATANA:
			{
				if (!npc.getVariables().getBoolean("i_ai2", false) && creature.isPlayer())
				{
					final Player player = creature.asPlayer();
					final QuestState qs = player.getQuestState(Q00144_PailakaInjuredDragon.class.getSimpleName());
					if (qs == null)
					{
						return;
					}
					
					if (qs.isCond(4))
					{
						startQuestTimer("LATANA_9000", 1000, npc, player);
						npc.getVariables().set("i_ai0", true);
					}
					else
					{
						playSound(player, QuestSound.BS08_A);
						npc.setScriptValue(1);
						startQuestTimer("SCE_RATANA_CAMERA_START", 2000, npc, player);
						startQuestTimer("LATANA_500", 1000, npc, player);
						npc.getVariables().set("i_ai2", true);
					}
				}
				break;
			}
			case INJURED_DRAGON_CAMERA_1:
			{
				if (creature.isPlayer() && !npc.getVariables().getBoolean("i_ai0", false))
				{
					npc.getVariables().set("i_ai0", true);
				}
				break;
			}
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		if (!npc.getVariables().getBoolean("i_ai2", false))
		{
			final QuestState qs = player.getQuestState(Q00144_PailakaInjuredDragon.class.getSimpleName());
			if (qs == null)
			{
				return;
			}
			
			if (qs.isCond(4))
			{
				startQuestTimer("LATANA_9000", 1000, npc, player);
				npc.getVariables().set("i_ai0", true);
			}
			else
			{
				playSound(player, QuestSound.BS08_A);
				npc.setScriptValue(2);
				startQuestTimer("SCE_RATANA_CAMERA_START", 4000, npc, player);
				startQuestTimer("LATANA_600", 1000, npc, player);
				npc.getVariables().set("i_ai2", true);
			}
		}
		
		if ((npc.getCurrentHp() < (npc.getMaxHp() * HP_ANGER_ACTIVATION)) && !npc.getVariables().getBoolean("i_ai0", false))
		{
			npc.setTarget(player);
			npc.doCast(ANGER.getSkill());
			npc.getVariables().set("i_ai0", true);
			startQuestTimer("LATANA_4000", 120000, npc, player);
		}
	}
	
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		if (creature.isPlayer() && !creature.isDead() && !creature.isTeleporting() && creature.asPlayer().isOnline())
		{
			final InstanceWorld world = InstanceManager.getInstance().getWorld(creature);
			if ((world != null) && (world.getTemplateId() == INSTANCE_ID))
			{
				// If a player wants to go by a mob wall without kill it, he will be returned back to a spawn point.
				final int[] zoneTeleport = NOEXIT_ZONES.get(zone.getId());
				if (zoneTeleport != null)
				{
					for (Npc npc : World.getInstance().getVisibleObjectsInRange(creature, Npc.class, 700))
					{
						if (npc.isDead() || npc.isInvisible() || !npc.isMonster())
						{
							continue;
						}
						
						creature.getAI().setIntention(Intention.IDLE);
						creature.setInstanceId(world.getInstanceId());
						creature.teleToLocation(zoneTeleport[0], zoneTeleport[1], zoneTeleport[2], true);
						break;
					}
				}
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		switch (npc.getId())
		{
			case LATANA:
			{
				npc.setInvul(true);
				npc.setParalyzed(true);
				npc.setLethalable(false);
				break;
			}
			case LATANA_SKILL_USE:
			{
				startQuestTimer("LATANA_SKILL_USE_1002", 10, npc, null);
				final Npc latana = npc.getVariables().getObject("param1", Npc.class);
				if (latana != null)
				{
					addAttackDesire(latana, (Creature) latana.getTarget(), 10000000);
				}
				break;
			}
			case INJURED_DRAGON_CAMERA_1:
			{
				// startQuestTimer("INJURED_DRAGON_CAMERA_1_3000", 10, npc, null);
				break;
			}
			case INJURED_DRAGON_CAMERA_2:
			{
				startQuestTimer("INJURED_DRAGON_CAMERA_2_1000", 1, npc, null);
				break;
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		switch (npc.getId())
		{
			case LATANA:
			{
				addSpawn(INJURED_DRAGON_CAMERA_2, 105974, -41794, -1784, 32768, false, 0, false, killer.getInstanceId());
				addSpawn(KETRA_ORC_SUPPORTER_2, killer.getX() + 100, killer.getY() + 100, killer.getZ(), 0, false, 0, false, killer.getInstanceId());
				break;
			}
			case GRAZING_ANTELOPE:
			case GRAZING_BANDERSNATCH:
			case GRAZING_FLAVA:
			case GRAZING_ELDER_ANTELOPE:
			{
				npc.dropItem(killer, getRandomBoolean() ? PAILAKA_INSTANT_SHIELD : QUICK_HEALING_POTION, getRandom(1, 10));
				break;
			}
			case VARKA_SILENOS_FOOTMAN:
			case VARKA_SILENOS_RECRUIT:
			{
				if (hasQuestItems(killer, SPEAR_OF_SILENOS) && !hasQuestItems(killer, WEAPON_UPGRADE_STAGE_1) && (getRandom(100) < 25))
				{
					giveItems(killer, WEAPON_UPGRADE_STAGE_1, 1);
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				
				// Spawns Mage Type silenos behind the one that was killed.
				spawnMageBehind(npc, killer, VARKA_SILENOS_MEDIUM);
				
				// Check if all the first row have been killed. Despawn mages.
				checkIfLastInWall(npc);
				break;
			}
			case VARKA_SILENOS_WARRIOR:
			{
				if (hasQuestItems(killer, SPEAR_OF_SILENOS) && !hasQuestItems(killer, WEAPON_UPGRADE_STAGE_1) && (getRandom(100) < 25))
				{
					giveItems(killer, WEAPON_UPGRADE_STAGE_1, 1);
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				
				// Spawns Mage Type silenos behind the one that was killed.
				spawnMageBehind(npc, killer, VARKA_SILENOS_PRIEST);
				
				// Check if all the first row have been killed. Despawn mages.
				checkIfLastInWall(npc);
				break;
			}
			case VARKA_ELITE_GUARD:
			{
				if (hasQuestItems(killer, SPEAR_OF_SILENOS) && !hasQuestItems(killer, WEAPON_UPGRADE_STAGE_1) && (getRandom(100) < 25))
				{
					giveItems(killer, WEAPON_UPGRADE_STAGE_1, 1);
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				
				// Spawns Mage Type silenos behind the one that was killed.
				spawnMageBehind(npc, killer, VARKA_SILENOS_SHAMAN);
				
				// Check if all the first row have been killed. Despawn mages.
				checkIfLastInWall(npc);
				break;
			}
			case VARKA_COMMANDER:
			case VARKA_SILENOS_OFFICER:
			{
				if (hasQuestItems(killer, SPEAR_OF_SILENOS) && !hasQuestItems(killer, WEAPON_UPGRADE_STAGE_1) && (getRandom(100) < 25))
				{
					giveItems(killer, WEAPON_UPGRADE_STAGE_1, 1);
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				
				// Spawns Mage Type silenos behind the one that was killed.
				spawnMageBehind(npc, killer, VARKA_SILENOS_SEER);
				
				// Check if all the first row have been killed. Despawn mages.
				checkIfLastInWall(npc);
				break;
			}
			case VARKA_SILENOS_GREAT_MAGUS:
			case VARKA_SILENOS_GENERAL:
			{
				if (hasQuestItems(killer, ENHANCED_SPEAR_OF_SILENOS) && !hasQuestItems(killer, WEAPON_UPGRADE_STAGE_2) && (getRandom(100) < 25))
				{
					giveItems(killer, WEAPON_UPGRADE_STAGE_2, 1);
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				
				// Spawns Mage Type silenos behind the one that was killed.
				spawnMageBehind(npc, killer, VARKA_SILENOS_MAGUS);
				
				// Check if all the first row have been killed. Despawn mages.
				checkIfLastInWall(npc);
				break;
			}
			case VARKA_PROPHET:
			{
				if (hasQuestItems(killer, ENHANCED_SPEAR_OF_SILENOS) && !hasQuestItems(killer, WEAPON_UPGRADE_STAGE_2) && (getRandom(100) < 25))
				{
					giveItems(killer, WEAPON_UPGRADE_STAGE_2, 1);
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				
				// Spawns Mage Type silenos behind the one that was killed.
				spawnMageBehind(npc, killer, DISCIPLE_OF_PROPHET);
				
				// Check if all the first row have been killed. Despawn mages.
				checkIfLastInWall(npc);
				break;
			}
			case VARKA_HEAD_GUARD:
			{
				if (hasQuestItems(killer, ENHANCED_SPEAR_OF_SILENOS) && !hasQuestItems(killer, WEAPON_UPGRADE_STAGE_2) && (getRandom(100) < 25))
				{
					giveItems(killer, WEAPON_UPGRADE_STAGE_2, 1);
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				
				// Spawns Mage Type silenos behind the one that was killed.
				spawnMageBehind(npc, killer, VARKA_HEAD_MAGUS);
				
				// Check if all the first row have been killed. Despawn mages.
				checkIfLastInWall(npc);
				break;
			}
			case PROPHET_GUARD:
			{
				if (hasQuestItems(killer, ENHANCED_SPEAR_OF_SILENOS) && !hasQuestItems(killer, WEAPON_UPGRADE_STAGE_2) && (getRandom(100) < 25))
				{
					giveItems(killer, WEAPON_UPGRADE_STAGE_2, 1);
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				
				// Spawns Mage Type silenos behind the one that was killed.
				spawnMageBehind(npc, killer, VARKA_SILENOS_GREAT_SEER);
				
				// Check if all the first row have been killed. Despawn mages.
				checkIfLastInWall(npc);
				break;
			}
		}
	}
	
	// Spawns Mage Type silenos behind the one that was killed. Aggro against the player that kill the mob.
	private void spawnMageBehind(Npc npc, Player player, int mageId)
	{
		final double rads = Math.toRadians(LocationUtil.convertHeadingToDegree(npc.getSpawn().getHeading()) + 180);
		final int mageX = (int) (npc.getX() + (150 * Math.cos(rads)));
		final int mageY = (int) (npc.getY() + (150 * Math.sin(rads)));
		final Npc mageBack = addSpawn(mageId, mageX, mageY, npc.getZ(), npc.getSpawn().getHeading(), false, 0, true, npc.getInstanceId());
		mageBack.getAI().notifyAction(Action.AGGRESSION, player, 1000);
	}
	
	// This will check if there is other mob alive in this wall of mobs. If all mobs in the first row are dead then despawn the second row mobs, the mages.
	private void checkIfLastInWall(Npc npc)
	{
		final Collection<Npc> knowns = World.getInstance().getVisibleObjectsInRange(npc, Npc.class, 700);
		for (Npc npcs : knowns)
		{
			if (npcs.isDead())
			{
				continue;
			}
			
			switch (npc.getId())
			{
				case VARKA_SILENOS_FOOTMAN:
				case VARKA_SILENOS_RECRUIT:
				case VARKA_SILENOS_WARRIOR:
				{
					switch (npcs.getId())
					{
						case VARKA_SILENOS_FOOTMAN:
						case VARKA_SILENOS_RECRUIT:
						case VARKA_SILENOS_WARRIOR:
						{
							return;
						}
					}
					break;
				}
				case VARKA_ELITE_GUARD:
				case VARKA_COMMANDER:
				case VARKA_SILENOS_OFFICER:
				{
					switch (npcs.getId())
					{
						case VARKA_ELITE_GUARD:
						case VARKA_COMMANDER:
						case VARKA_SILENOS_OFFICER:
						{
							return;
						}
					}
					break;
				}
				case VARKA_SILENOS_GREAT_MAGUS:
				case VARKA_SILENOS_GENERAL:
				case VARKA_PROPHET:
				{
					switch (npcs.getId())
					{
						case VARKA_SILENOS_GREAT_MAGUS:
						case VARKA_SILENOS_GENERAL:
						case VARKA_PROPHET:
						{
							return;
						}
					}
					break;
				}
				case VARKA_HEAD_GUARD:
				case PROPHET_GUARD:
				{
					switch (npcs.getId())
					{
						case VARKA_HEAD_GUARD:
						case PROPHET_GUARD:
						{
							return;
						}
					}
					break;
				}
			}
		}
		
		// We did not find any mob on the first row alive, so despawn the second row mobs.
		for (Creature npcs : knowns)
		{
			if (npcs.isDead())
			{
				continue;
			}
			
			switch (npc.getId())
			{
				case VARKA_SILENOS_FOOTMAN:
				case VARKA_SILENOS_RECRUIT:
				case VARKA_SILENOS_WARRIOR:
				{
					switch (npcs.getId())
					{
						case VARKA_SILENOS_MEDIUM:
						case VARKA_SILENOS_PRIEST:
						{
							npcs.abortCast();
							npcs.deleteMe();
							break;
						}
					}
					break;
				}
				case VARKA_ELITE_GUARD:
				case VARKA_COMMANDER:
				case VARKA_SILENOS_OFFICER:
				{
					switch (npcs.getId())
					{
						case VARKA_SILENOS_SHAMAN:
						case VARKA_SILENOS_SEER:
						{
							npcs.abortCast();
							npcs.deleteMe();
							break;
						}
					}
					break;
				}
				case VARKA_SILENOS_GREAT_MAGUS:
				case VARKA_SILENOS_GENERAL:
				case VARKA_PROPHET:
				{
					switch (npcs.getId())
					{
						case VARKA_SILENOS_GREAT_MAGUS:
						case DISCIPLE_OF_PROPHET:
						{
							npcs.abortCast();
							npcs.deleteMe();
							break;
						}
					}
					break;
				}
				case VARKA_HEAD_GUARD:
				case PROPHET_GUARD:
				{
					switch (npcs.getId())
					{
						case VARKA_HEAD_MAGUS:
						case VARKA_SILENOS_GREAT_SEER:
						{
							npcs.abortCast();
							npcs.deleteMe();
							break;
						}
					}
					break;
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		new PailakaInjuredDragon();
	}
}
