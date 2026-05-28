/*
 * Custom PvP Zone System
 */
package custom.PvpZone;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.managers.CustomPvpZoneRegistry;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanPrivileges;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.zone.type.BossZone;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureTeleported;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.model.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.model.punishment.PunishmentTask;
import org.l2jmobius.gameserver.model.punishment.PunishmentType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExPVPMatchCCRecord;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * Custom PvP Zone with rotation, kill streaks, hero, class name, timer, chat block.
 */
public class PvpZone extends Script
{
	// ---------------------------------------------------------------------------
	// Config
	// ---------------------------------------------------------------------------
	private static boolean ENABLED = false;
	private static int ROTATION_MINUTES = 60;
	private static int REWARD_ITEM_ID = 57;
	private static int REWARD_BASE_COUNT = 50000;
	private static int HERO_STREAK_REQUIRED = 5;
	private static int RESPAWN_DELAY = 5;
	// Top killer reward per zone rotation
	private static int TOP_KILLER_REWARD_ITEM_ID = 0;
	private static int TOP_KILLER_REWARD_COUNT = 1;
	// Custom title shown inside the zone (empty = keep original)
	private static String ZONE_TITLE = "Lineage2IA";
	// Hide clan crest/title and force clan skills to self-only (configurable)
	private static boolean HIDE_CLAN = true;
	// Block party invitations for zone participants (configurable)
	private static boolean BLOCK_PARTY = true;
	// Return / recovery location: used on Restart inside the zone AND on startup recovery
	private static int RETURN_X = 83400;
	private static int RETURN_Y = 147943;
	private static int RETURN_Z = -3400;
	private static final List<int[]> STREAKS = new ArrayList<int[]>();
	private static final List<PvpZoneData> ZONES = new ArrayList<PvpZoneData>();

	// ---------------------------------------------------------------------------
	// Runtime state
	// ---------------------------------------------------------------------------
	private static int _currentZoneIndex = 0;
	private static final Set<Player> PARTICIPANTS = ConcurrentHashMap.newKeySet();
	private static final Map<Integer, Integer> KILL_STREAKS = new ConcurrentHashMap<Integer, Integer>();
	/** Total kills en la zona actual, por jugador (objectId -> total kills) */
	private static final Map<Integer, Integer> ZONE_KILLS = new ConcurrentHashMap<Integer, Integer>();
	/** Nombre real del jugador asociado a sus kills (persiste hasta rotacion) */
	private static final Map<Integer, String> ZONE_KILL_NAMES = new ConcurrentHashMap<Integer, String>();
	/** Clase/profesion del jugador asociado a sus kills (persiste hasta rotacion) */
	private static final Map<Integer, String> ZONE_KILL_CLASSES = new ConcurrentHashMap<Integer, String>();
	private static final Map<Integer, String> ORIGINAL_NAMES = new ConcurrentHashMap<Integer, String>();
	private static final Set<Integer> STREAK_HEROES = ConcurrentHashMap.newKeySet();
	private static final Set<Integer> CHAT_BANNED = ConcurrentHashMap.newKeySet();
	/** Scoreboard: Player -> kills (for ExPVPMatchCCRecord, uses Player keys) */
	private static final Map<Player, Integer> SCOREBOARD = new ConcurrentHashMap<Player, Integer>();
	private static long _rotationStartTime = 0;
	private static ScheduledFuture<?> _countdownTask = null;
	/** Flag to distinguish our own teleport calls from external ones (SOE, /unstuck) */
	private static final Set<Integer> INTERNAL_TELEPORT = ConcurrentHashMap.newKeySet();
	/** Backup of clan data per player (objectId -> backup) for restore on zone exit */
	private static final Map<Integer, ClanBackup> CLAN_BACKUPS = new ConcurrentHashMap<Integer, ClanBackup>();
	/** Original personal title of each participant, saved on entry and restored on exit */
	private static final Map<Integer, String> ORIGINAL_TITLES = new ConcurrentHashMap<Integer, String>();

	private static final int NPC_ID = 30999;
	private static final int NOBLESSE_BLESSING_ID = 1323;

	// ---------------------------------------------------------------------------
	// Inner class: backup of clan data stripped on zone entry
	// ---------------------------------------------------------------------------
	private static class ClanBackup
	{
		final Clan clan;
		final String title;
		final int pledgeType;
		final int powerGrade;
		final int lvlJoinedAcademy;
		final int apprentice;
		final int sponsor;
		final ClanPrivileges clanPrivileges;

		ClanBackup(Player player)
		{
			this.clan = player.getClan();
			this.title = player.getTitle();
			this.pledgeType = player.getPledgeType();
			this.powerGrade = player.getPowerGrade();
			this.lvlJoinedAcademy = player.getLvlJoinedAcademy();
			this.apprentice = player.getApprentice();
			this.sponsor = player.getSponsor();
			this.clanPrivileges = player.getClanPrivileges();
		}

		void restore(Player player)
		{
			if (clan != null)
			{
				player.setClan(clan);
				// Note: personal title is restored separately via ORIGINAL_TITLES map.
				player.setPledgeType(pledgeType);
				player.setPowerGrade(powerGrade);
				player.setLvlJoinedAcademy(lvlJoinedAcademy);
				player.setApprentice(apprentice);
				player.setSponsor(sponsor);
				player.setClanPrivileges(clanPrivileges);
			}
		}
	}

	// ---------------------------------------------------------------------------
	// Inner class for zone data
	// ---------------------------------------------------------------------------
	private static class PvpZoneData
	{
		final String name;
		final List<Location> spawns;

		PvpZoneData(String name, List<Location> spawns)
		{
			this.name = name;
			this.spawns = spawns;
		}

		Location getRandomSpawn()
		{
			return spawns.get(Rnd.get(spawns.size()));
		}
	}

	// ---------------------------------------------------------------------------
	// Constructor
	// ---------------------------------------------------------------------------
	public PvpZone()
	{
		loadConfig();

		// Publish config values to the shared registry so that core packet handlers
		// (RequestJoinParty, RequestRestart) can use them without depending on this script.
		CustomPvpZoneRegistry.setReturnLocation(RETURN_X, RETURN_Y, RETURN_Z);
		CustomPvpZoneRegistry.setPartyBlockEnabled(BLOCK_PARTY);

		// Recovery for players that were inside the PvP zone when the server died abruptly.
		// Runs BEFORE any player can log in, on every server start.
		recoverStuckPlayers();

		if (!ENABLED || ZONES.isEmpty())
		{
			LOGGER.info("PvpZone: Disabled or no zones configured. Enabled=" + ENABLED + " Zones=" + ZONES.size());
			return;
		}

		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);

		BypassHandler.getInstance().registerHandler(new PvpZoneBypass(this));

		// Global login listener to restore real name in case player logged out while in zone
		Containers.Players().addListener(new ConsumerEventListener(
			Containers.Players(),
			EventType.ON_PLAYER_LOGIN,
			(OnPlayerLogin event) -> onPlayerLogin(event),
			this));

		_rotationStartTime = System.currentTimeMillis();
		final long rotationMs = ROTATION_MINUTES * 60000L;
		ThreadPool.scheduleAtFixedRate(this::rotateZone, rotationMs, rotationMs);
		scheduleCountdown();

		// JVM shutdown hook: restore real names for any participants still inside the zone
		// when the server is restarted or shut down, BEFORE players are saved to DB.
		Runtime.getRuntime().addShutdownHook(new Thread(this::onServerShutdown, "PvpZone-Shutdown"));

		LOGGER.info("PvpZone: Loaded " + ZONES.size() + " zones. Active: " + ZONES.get(0).name
			+ ". Rotation=" + ROTATION_MINUTES + "min, HeroStreak=" + HERO_STREAK_REQUIRED
			+ ", RewardItem=" + REWARD_ITEM_ID + ", RespawnDelay=" + RESPAWN_DELAY + "s");

		// Log all zone names for verification
		for (int i = 0; i < ZONES.size(); i++)
		{
			final PvpZoneData z = ZONES.get(i);
			LOGGER.info("PvpZone:   Zone " + i + ": " + z.name + " (" + z.spawns.size() + " spawns)");
		}
	}

	// ---------------------------------------------------------------------------
	// Config Loader
	// ---------------------------------------------------------------------------
	private void loadConfig()
	{
		final Properties props = new Properties();
		try (InputStream is = new FileInputStream("./config/Custom/PvpZone.ini"))
		{
			props.load(is);
		}
		catch (Exception e)
		{
			LOGGER.warning("PvpZone: Could not load config file: " + e.getMessage());
			return;
		}

		ENABLED = Boolean.parseBoolean(props.getProperty("Enabled", "True").trim());
		ROTATION_MINUTES = Integer.parseInt(props.getProperty("RotationMinutes", "60").trim());
		REWARD_ITEM_ID = Integer.parseInt(props.getProperty("RewardItemId", "57").trim());
		REWARD_BASE_COUNT = Integer.parseInt(props.getProperty("RewardBaseCount", "50000").trim());
		HERO_STREAK_REQUIRED = Integer.parseInt(props.getProperty("HeroStreakRequired", "5").trim());
		RESPAWN_DELAY = Integer.parseInt(props.getProperty("RespawnDelay", "5").trim());
		TOP_KILLER_REWARD_ITEM_ID = Integer.parseInt(props.getProperty("TopKillerRewardItemId", "0").trim());
		TOP_KILLER_REWARD_COUNT = Integer.parseInt(props.getProperty("TopKillerRewardCount", "1").trim());
		ZONE_TITLE = props.getProperty("ZoneTitle", "Lineage2IA").trim();
		HIDE_CLAN = Boolean.parseBoolean(props.getProperty("HideClan", "True").trim());
		BLOCK_PARTY = Boolean.parseBoolean(props.getProperty("BlockParty", "True").trim());
		RETURN_X = Integer.parseInt(props.getProperty("ReturnX", "83400").trim());
		RETURN_Y = Integer.parseInt(props.getProperty("ReturnY", "147943").trim());
		RETURN_Z = Integer.parseInt(props.getProperty("ReturnZ", "-3400").trim());

		// Parse streaks: "1,50000;2,60000;3,70000"
		STREAKS.clear();
		final String streaksStr = props.getProperty("Streaks", "1,50000;2,60000;3,70000;4,80000;5,90000").trim();
		for (String entry : streaksStr.split(";"))
		{
			final String[] parts = entry.trim().split(",");
			if (parts.length == 2)
			{
				STREAKS.add(new int[] { Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()) });
			}
		}

		// Parse zones: "ZoneName;x1,y1,z1;x2,y2,z2|OtherZone;x1,y1,z1;x2,y2,z2"
		ZONES.clear();
		final String zonesStr = props.getProperty("Zones", "").trim();
		if (!zonesStr.isEmpty())
		{
			for (String zoneDef : zonesStr.split("\\|"))
			{
				final String[] zoneParts = zoneDef.trim().split(";");
				if (zoneParts.length < 2)
				{
					continue;
				}
				final String zoneName = zoneParts[0].trim();
				final List<Location> spawnList = new ArrayList<Location>();
				for (int i = 1; i < zoneParts.length; i++)
				{
					final String[] coords = zoneParts[i].trim().split(",");
					if (coords.length == 3)
					{
						try
						{
							spawnList.add(new Location(
								Integer.parseInt(coords[0].trim()),
								Integer.parseInt(coords[1].trim()),
								Integer.parseInt(coords[2].trim())));
						}
						catch (NumberFormatException e)
						{
							LOGGER.warning("PvpZone: Bad coordinate in zone '" + zoneName + "': " + zoneParts[i] + " -> " + e.getMessage());
						}
					}
					else
					{
						LOGGER.warning("PvpZone: Expected 3 coords (x,y,z) but got " + coords.length + " in zone '" + zoneName + "': " + zoneParts[i]);
					}
				}
				if (!spawnList.isEmpty())
				{
					ZONES.add(new PvpZoneData(zoneName, spawnList));
				}
			}
		}

		LOGGER.info("PvpZone: Config loaded - Enabled=" + ENABLED + " RotationMin=" + ROTATION_MINUTES
			+ " RewardId=" + REWARD_ITEM_ID + " BaseCount=" + REWARD_BASE_COUNT
			+ " HeroStreak=" + HERO_STREAK_REQUIRED + " RespawnDelay=" + RESPAWN_DELAY
			+ " BlockParty=" + BLOCK_PARTY
			+ " Return=(" + RETURN_X + "," + RETURN_Y + "," + RETURN_Z + ")"
			+ " Zones=" + ZONES.size() + " Streaks=" + STREAKS.size());
	}

	// ---------------------------------------------------------------------------
	// NPC Interaction
	// ---------------------------------------------------------------------------
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final PvpZoneData currentZone = ZONES.get(_currentZoneIndex);
		final long elapsed = System.currentTimeMillis() - _rotationStartTime;
		final long remaining = Math.max(0, (ROTATION_MINUTES * 60000L) - elapsed);
		final int remainingMin = (int) (remaining / 60000);

		String htmltext = getHtm(player, "main.htm");
		htmltext = htmltext.replace("%pvpzone_name%", currentZone.name);
		htmltext = htmltext.replace("%pvpzone_players%", String.valueOf(PARTICIPANTS.size()));
		htmltext = htmltext.replace("%pvpzone_timer%", String.valueOf(remainingMin));
		htmltext = htmltext.replace("%hero_streak%", String.valueOf(HERO_STREAK_REQUIRED));
		return htmltext;
	}

	// ---------------------------------------------------------------------------
	// Bypass Handler
	// ---------------------------------------------------------------------------
	private static class PvpZoneBypass implements IBypassHandler
	{
		private static final String[] COMMANDS =
		{
			"pvpzone_teleport",
			"pvpzone_info",
			"pvpzone_leave",
			"pvpzone_respawn",
			"pvpzone_ranking"
		};

		private final PvpZone _owner;

		PvpZoneBypass(PvpZone owner)
		{
			_owner = owner;
		}

		@Override
		public boolean onCommand(String command, Player player, Creature target)
		{
			if (!ENABLED || ZONES.isEmpty())
			{
				player.sendMessage("La zona PvP no esta disponible.");
				return false;
			}

			if ("pvpzone_teleport".equals(command))
			{
				_owner.handleTeleport(player);
			}
			else if ("pvpzone_info".equals(command))
			{
				_owner.handleInfo(player);
			}
			else if ("pvpzone_leave".equals(command))
			{
				_owner.handleLeave(player);
			}
			else if ("pvpzone_respawn".equals(command))
			{
				_owner.respawnInZone(player);
			}
			else if ("pvpzone_ranking".equals(command))
			{
				_owner.showRankingHtml(player);
			}

			return true;
		}

		@Override
		public String[] getCommandList()
		{
			return COMMANDS;
		}
	}

	// ---------------------------------------------------------------------------
	// Teleport into PvP Zone
	// ---------------------------------------------------------------------------
	private void handleTeleport(Player player)
	{
		if (player.isDead())
		{
			player.sendMessage("No puedes teletransportarte mientras estas muerto.");
			return;
		}

		if (player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player))
		{
			player.sendMessage("No puedes entrar a la zona PvP mientras estas inscripto en las Olimpiadas.");
			return;
		}

		if (player.isOnEvent() || player.isRegisteredOnEvent())
		{
			player.sendMessage("No puedes entrar a la zona PvP mientras estas inscripto en un evento.");
			return;
		}

		if (player.getBlockCheckerArena() > -1)
		{
			player.sendMessage("No puedes entrar a la zona PvP mientras estas en un evento.");
			return;
		}

		if (BLOCK_PARTY && player.isInParty())
		{
			player.sendMessage("Debes salir del party antes de entrar a la Zona PvP.");
			return;
		}

		if (PARTICIPANTS.contains(player))
		{
			player.sendMessage("Ya te encuentras en la zona PvP.");
			return;
		}

		// Store original name and class, then change to class name
		final String realName = player.getName();
		ORIGINAL_NAMES.put(player.getObjectId(), realName);
		// Persist in PlayerVariables and FORCE immediate DB save BEFORE the name is changed,
		// so a crash within milliseconds will still have the backup available for recovery.
		player.getVariables().set("PVPZ_REAL_NAME", realName);
		player.getVariables().storeMe();
		// Also write the backup directly to character_variables via SQL so we don't depend on
		// any caching layer — guarantees the row exists in the DB right now.
		persistRealNameBackup(player.getObjectId(), realName);

		final String className = ClassListData.getInstance().getClass(player.getActiveClass()).getClassName();
		ZONE_KILL_NAMES.put(player.getObjectId(), realName);
		ZONE_KILL_CLASSES.put(player.getObjectId(), className);

		// Save original personal title and apply the zone title (works for all players,
		// regardless of clan membership or HideClan setting).
		final String originalTitle = player.getTitle();
		ORIGINAL_TITLES.put(player.getObjectId(), originalTitle);
		player.getVariables().set("PVPZ_REAL_TITLE", originalTitle);
		if (!ZONE_TITLE.isEmpty())
		{
			player.setTitle(ZONE_TITLE);
		}

		// Backup and hide clan identity (crest, title, privileges).
		// With clanId = 0 clan buff/heal skills won't affect other clan members.
		if (HIDE_CLAN && (player.getClan() != null))
		{
			CLAN_BACKUPS.put(player.getObjectId(), new ClanBackup(player));
			player.setClan(null); // Resets clanId, title, crest, privileges
		}

		player.setName(className);
		player.broadcastUserInfo();
		// Force the chars table row to be persisted now so the DB has a consistent state
		// (disguised name + backup variable) for recovery on abrupt restart.
		player.storeMe();

		// Add to participants BEFORE teleport
		PARTICIPANTS.add(player);
		CustomPvpZoneRegistry.register(player.getObjectId());
		KILL_STREAKS.put(player.getObjectId(), 0);

		// Add event listeners
		addDeathListener(player);
		addLogoutListener(player);
		addTeleportListener(player);

		// Block chat
		blockChat(player);

		// Mark as internal teleport so the teleport listener doesn't kick them
		INTERNAL_TELEPORT.add(player.getObjectId());

		// Whitelist player in known BossZones (e.g. Beleth) so they don't get kicked
		whitelistInBossZones(player);

		// Teleport to random spawn in current zone
		final PvpZoneData zone = ZONES.get(_currentZoneIndex);
		final Location spawn = zone.getRandomSpawn();
		player.teleToLocation(spawn.getX() + Rnd.get(-50, 50), spawn.getY() + Rnd.get(-50, 50), spawn.getZ(), 0);

		// Set PvP flag (long duration, removed on exit)
		player.setPvpFlagLasts(System.currentTimeMillis() + 86400000L);
		player.startPvPFlag();

		// Initialize scoreboard for this player and send current state
		SCOREBOARD.put(player, 0);
		player.sendPacket(new ExPVPMatchCCRecord(ExPVPMatchCCRecord.INITIALIZE, buildRealNameScoreboard(), true));

		// Big screen announcement
		player.sendPacket(new ExShowScreenMessage("ZONA PVP: " + zone.name, 7000));
		player.sendMessage("Has ingresado a la Zona PvP: " + zone.name);
		player.sendMessage("Tu nombre ha sido cambiado a tu profesion: " + className);
		player.sendMessage("El chat esta bloqueado mientras permanezcas en la zona.");

		// Show countdown timer immediately
		final long elapsed = System.currentTimeMillis() - _rotationStartTime;
		final long remaining = Math.max(0, (ROTATION_MINUTES * 60000L) - elapsed);
		final int remainingSec = (int) (remaining / 1000);
		if (remainingSec > 0)
		{
			player.sendPacket(new ExSendUIEvent(player, false, false, remainingSec, 0, NpcStringId.TIME_REMAINING));
		}
	}

	// ---------------------------------------------------------------------------
	// Chat block / unblock
	// ---------------------------------------------------------------------------
	private void blockChat(Player player)
	{
		if (!player.isChatBanned())
		{
			PunishmentManager.getInstance().startPunishment(new PunishmentTask(
				player.getObjectId(),
				PunishmentAffect.CHARACTER,
				PunishmentType.CHAT_BAN,
				0,
				"PvP Zone chat block",
				"PvpZone"));
			CHAT_BANNED.add(player.getObjectId());
		}
	}

	private void unblockChat(Player player)
	{
		if (CHAT_BANNED.remove(player.getObjectId()))
		{
			PunishmentManager.getInstance().stopPunishment(
				player.getObjectId(),
				PunishmentAffect.CHARACTER,
				PunishmentType.CHAT_BAN);
		}
	}

	// ---------------------------------------------------------------------------
	// Info display
	// ---------------------------------------------------------------------------
	private void handleInfo(Player player)
	{
		final PvpZoneData zone = ZONES.get(_currentZoneIndex);
		final long elapsed = System.currentTimeMillis() - _rotationStartTime;
		final long remaining = Math.max(0, (ROTATION_MINUTES * 60000L) - elapsed);
		final int remainingMin = (int) (remaining / 60000);
		final int remainingSec = (int) ((remaining % 60000) / 1000);

		final ItemTemplate rewardTemplate = ItemData.getInstance().getTemplate(REWARD_ITEM_ID);
		final String rewardName = (rewardTemplate != null) ? rewardTemplate.getName() : "Item " + REWARD_ITEM_ID;

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<center><font color=\"LEVEL\">Zona PvP - Informacion</font></center><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
		sb.append("<table width=\"270\">");
		sb.append("<tr><td>Zona actual:</td><td><font color=\"00FF00\">").append(zone.name).append("</font></td></tr>");
		sb.append("<tr><td>Jugadores:</td><td><font color=\"FFDF00\">").append(PARTICIPANTS.size()).append("</font></td></tr>");
		sb.append("<tr><td>Tiempo restante:</td><td><font color=\"FF6347\">").append(remainingMin).append("m ").append(remainingSec).append("s</font></td></tr>");
		sb.append("<tr><td>Recompensa base:</td><td><font color=\"FFDF00\">").append(String.format("%,d", REWARD_BASE_COUNT)).append(" ").append(rewardName).append("</font></td></tr>");
		sb.append("</table><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
		sb.append("<font color=\"808080\">Sistema de Rachas:</font><br>");
		for (int[] streak : STREAKS)
		{
			sb.append("Kill ").append(streak[0]).append(": <font color=\"FFDF00\">").append(String.format("%,d", streak[1])).append(" ").append(rewardName).append("</font><br>");
		}
		sb.append("<br>Hero a las <font color=\"FF0000\">").append(HERO_STREAK_REQUIRED).append("</font> kills seguidas.<br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");

		if (PARTICIPANTS.contains(player))
		{
			final int currentStreak = KILL_STREAKS.containsKey(player.getObjectId()) ? KILL_STREAKS.get(player.getObjectId()) : 0;
			sb.append("<br><font color=\"LEVEL\">Tu racha actual: ").append(currentStreak).append(" kills</font><br>");
			sb.append("<br><center><a action=\"bypass -h pvpzone_leave\"><font color=\"FF6666\">Salir de la Zona PvP</font></a></center>");
		}

		sb.append("</body></html>");

		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}

	// ---------------------------------------------------------------------------
	// Leave PvP Zone
	// ---------------------------------------------------------------------------
	private void handleLeave(Player player)
	{
		if (!PARTICIPANTS.contains(player))
		{
			player.sendMessage("No estas en la zona PvP.");
			return;
		}

		removeFromZone(player, true);

		// Mark as internal teleport
		INTERNAL_TELEPORT.add(player.getObjectId());
		player.teleToLocation(83400, 147943, -3400, 0);
		player.sendPacket(new ExShowScreenMessage("Has salido de la zona PvP.", 5000));
	}

	// ---------------------------------------------------------------------------
	// Remove player from zone (cleanup)
	// ---------------------------------------------------------------------------
	private void removeFromZone(Player player, boolean restoreName)
	{
		if (!PARTICIPANTS.remove(player))
		{
			return; // Already removed
		}

		CustomPvpZoneRegistry.unregister(player.getObjectId());
		KILL_STREAKS.remove(player.getObjectId());
		ZONE_KILLS.remove(player.getObjectId());
		ZONE_KILL_NAMES.remove(player.getObjectId());
		ZONE_KILL_CLASSES.remove(player.getObjectId());

		// Restore clan identity (crest, privileges)
		final ClanBackup clanBackup = CLAN_BACKUPS.remove(player.getObjectId());
		if (clanBackup != null)
		{
			clanBackup.restore(player);
		}

		// Restore original personal title (applies to all players regardless of clan)
		final String originalTitle = ORIGINAL_TITLES.remove(player.getObjectId());
		if (originalTitle != null)
		{
			player.setTitle(originalTitle);
		}
		player.getVariables().remove("PVPZ_REAL_TITLE");

		// Restore original name
		if (restoreName)
		{
			String originalName = ORIGINAL_NAMES.remove(player.getObjectId());
			if (originalName == null)
			{
				originalName = player.getVariables().getString("PVPZ_REAL_NAME", null);
			}
			if (originalName != null)
			{
				player.setName(originalName);
			}
		}
		else
		{
			ORIGINAL_NAMES.remove(player.getObjectId());
		}
		// Broadcast once after both clan and name are restored
		player.broadcastUserInfo();
		// Always clear the persisted backup once cleanup runs (both in-memory variables and direct DB row)
		player.getVariables().remove("PVPZ_REAL_NAME");
		player.getVariables().storeMe();
		clearRealNameBackup(player.getObjectId());
		// Persist the restored chars row so the DB definitely has the real name
		try
		{
			player.storeMe();
		}
		catch (Exception ignored)
		{
		}

		// Remove hero if it was granted by streak
		if (STREAK_HEROES.remove(player.getObjectId()))
		{
			player.setHero(false);
			player.broadcastUserInfo();
			player.sendMessage("Tu status de Hero ha sido removido.");
		}

		// Remove PvP flag immediately
		player.setPvpFlagLasts(0);
		player.stopPvPFlag();

		// Clear combat state so the player isn't stuck in combat mode at the NPC
		player.abortAttack();
		player.abortCast();
		player.setTarget(null);
		player.getAI().setIntention(Intention.IDLE);

		// Unblock chat
		unblockChat(player);

		// Close scoreboard for this player and remove from tracking
		SCOREBOARD.remove(player);
		player.sendPacket(new ExPVPMatchCCRecord(ExPVPMatchCCRecord.FINISH, buildRealNameScoreboard(), true));

		// Clear timer UI
		player.sendPacket(new ExSendUIEvent(player, true, true, 0, 0, ""));

		// Remove all listeners
		removeListeners(player);
	}

	// ---------------------------------------------------------------------------
	// Teleport detection (SOE, /unstuck, etc.)
	// ---------------------------------------------------------------------------
	private void onCreatureTeleported(OnCreatureTeleported event)
	{
		final Creature creature = event.getCreature();
		if (!(creature instanceof Player))
		{
			return;
		}

		final Player player = (Player) creature;

		// If this is our own teleport (we called teleToLocation), ignore it
		if (INTERNAL_TELEPORT.remove(player.getObjectId()))
		{
			return;
		}

		// Player teleported by external means (SOE, /unstuck, GM, etc.) — remove from zone
		if (PARTICIPANTS.contains(player))
		{
			removeFromZone(player, true);
			player.sendPacket(new ExShowScreenMessage("Has salido de la zona PvP.", 5000));
			player.sendMessage("Has abandonado la zona PvP.");
		}
	}

	// ---------------------------------------------------------------------------
	// Kill handling
	// ---------------------------------------------------------------------------
	private void onPlayerDeath(OnCreatureDeath event)
	{
		if (!(event.getTarget() instanceof Player))
		{
			return;
		}

		final Player killed = (Player) event.getTarget();
		if (!PARTICIPANTS.contains(killed))
		{
			return;
		}

		// Reset killed player's streak
		KILL_STREAKS.put(killed.getObjectId(), 0);

		// If hero was granted by PvP zone streak, remove it (Olympiad hero is NOT in STREAK_HEROES)
		if (STREAK_HEROES.remove(killed.getObjectId()))
		{
			killed.setHero(false);
			killed.broadcastUserInfo();
			killed.sendMessage("Has perdido el status de Hero al morir en la Zona PvP.");
		}

		// Re-apply PvP flag on death to ensure it never drops
		killed.setPvpFlagLasts(System.currentTimeMillis() + 86400000L);
		killed.startPvPFlag();

		// Handle killer rewards
		if ((event.getAttacker() != null) && (event.getAttacker() instanceof Player))
		{
			final Player killer = (Player) event.getAttacker();
			if (PARTICIPANTS.contains(killer) && (killer.getObjectId() != killed.getObjectId()))
			{
				int streak = KILL_STREAKS.containsKey(killer.getObjectId()) ? KILL_STREAKS.get(killer.getObjectId()) : 0;
				streak++;
				KILL_STREAKS.put(killer.getObjectId(), streak);

				// Track total kills in this zone with real name
				final int totalKills = ZONE_KILLS.containsKey(killer.getObjectId()) ? ZONE_KILLS.get(killer.getObjectId()) + 1 : 1;
				ZONE_KILLS.put(killer.getObjectId(), totalKills);
				final String killerRealName = ORIGINAL_NAMES.get(killer.getObjectId());
				if (killerRealName != null)
				{
					ZONE_KILL_NAMES.put(killer.getObjectId(), killerRealName);
				}
				if (!ZONE_KILL_CLASSES.containsKey(killer.getObjectId()))
				{
					ZONE_KILL_CLASSES.put(killer.getObjectId(), ClassListData.getInstance().getClass(killer.getActiveClass()).getClassName());
				}
				// Persist kill to DB for web panel ranking
				final String realName = killerRealName != null ? killerRealName : killer.getName();
				final String currentZoneName = ZONES.get(_currentZoneIndex).name;
				persistZoneKill(realName, currentZoneName, totalKills);

				int rewardCount = REWARD_BASE_COUNT;
				for (int[] streakData : STREAKS)
				{
					if (streak >= streakData[0])
					{
						rewardCount = streakData[1];
					}
				}

				killer.addItem(ItemProcessType.REWARD, REWARD_ITEM_ID, rewardCount, killer, true);

				// Update scoreboard for all participants
				SCOREBOARD.put(killer, totalKills);
				final Map<String, Integer> updatedScores = buildRealNameScoreboard();
				for (Player participant : PARTICIPANTS)
				{
					participant.sendPacket(new ExPVPMatchCCRecord(ExPVPMatchCCRecord.UPDATE, updatedScores, true));
				}

				// Refresh PvP flag for killer
				killer.setPvpFlagLasts(System.currentTimeMillis() + 86400000L);
				killer.startPvPFlag();

				if ((streak >= HERO_STREAK_REQUIRED) && !killer.isHero())
				{
					killer.setHero(true);
					killer.broadcastUserInfo();
					STREAK_HEROES.add(killer.getObjectId());
					killer.sendPacket(new ExShowScreenMessage("Has alcanzado " + streak + " kills seguidas. Eres Hero!", 5000));

					// Use real character name for the global announcement, not the class disguise
					String heroRealName = ORIGINAL_NAMES.get(killer.getObjectId());
					if (heroRealName == null)
					{
						heroRealName = killer.getName();
					}
					Broadcast.toAllOnlinePlayersOnScreen(heroRealName + " es Hero con " + streak + " kills seguidas en la Zona PvP!");
				}
				else
				{
					killer.sendPacket(new ExShowScreenMessage("Racha: " + streak + " kills!", 2000));
				}
			}
		}

		// Show respawn dialog after delay
		startQuestTimer("ShowRespawnDialog", RESPAWN_DELAY * 1000, null, killed);
	}

	// ---------------------------------------------------------------------------
	// Respawn dialog & action
	// ---------------------------------------------------------------------------
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ("ShowRespawnDialog".equals(event))
		{
			if ((player != null) && player.isDead() && PARTICIPANTS.contains(player))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage();
				final StringBuilder sb = new StringBuilder();
				sb.append("<html><body>");
				sb.append("<center>");
				sb.append("<br><br><br><br>");
				sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\"><br>");
				sb.append("<font color=\"LEVEL\">Zona PvP</font><br><br>");
				sb.append("<font color=\"FF6347\">Has muerto en combate!</font><br><br>");
				sb.append("<button value=\"Respawn en Zona PvP\" action=\"bypass -h pvpzone_respawn\" width=\"200\" height=\"30\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br>");
				sb.append("<font color=\"808080\">O usa 'To Village' para salir de la zona.</font>");
				sb.append("<br><img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\">");
				sb.append("</center>");
				sb.append("</body></html>");
				html.setHtml(sb.toString());
				player.sendPacket(html);
			}
			return null;
		}

		return super.onEvent(event, npc, player);
	}

	private void respawnInZone(Player player)
	{
		if ((player == null) || !player.isDead() || !PARTICIPANTS.contains(player))
		{
			return;
		}

		final PvpZoneData zone = ZONES.get(_currentZoneIndex);
		final Location spawn = zone.getRandomSpawn();

		// Mark as internal teleport
		INTERNAL_TELEPORT.add(player.getObjectId());

		whitelistInBossZones(player);

		player.setIsPendingRevive(true);
		player.teleToLocation(spawn.getX() + Rnd.get(-50, 50), spawn.getY() + Rnd.get(-50, 50), spawn.getZ(), 0);
		player.doRevive();

		player.setCurrentHp(player.getMaxHp());
		player.setCurrentMp(player.getMaxMp());
		player.setCurrentCp(player.getMaxCp());

		// Aplicar Noblesse Blessing al respawnear dentro de la zona
		final Skill noblesseBlessing = SkillData.getInstance().getSkill(NOBLESSE_BLESSING_ID, 1);
		if (noblesseBlessing != null)
		{
			noblesseBlessing.applyEffects(player, player);
		}

		player.setPvpFlagLasts(System.currentTimeMillis() + 86400000L);
		player.startPvPFlag();

		player.sendPacket(new ExShowScreenMessage("Has respawneado en la zona PvP.", 3000));
	}

	// ---------------------------------------------------------------------------
	// Logout handling
	// ---------------------------------------------------------------------------
	private void onPlayerLogout(OnPlayerLogout event)
	{
		final Player player = event.getPlayer();
		if ((player != null) && PARTICIPANTS.contains(player))
		{
			removeFromZone(player, true);
		}
	}

	// ---------------------------------------------------------------------------
	// Zone Rotation
	// ---------------------------------------------------------------------------
	private void rotateZone()
	{
		final PvpZoneData oldZone = ZONES.get(_currentZoneIndex);

		// Announce top killer of the ending zone
		announceTopKiller(oldZone.name);

		// Close scoreboard (FINISH) and clear timer for all participants
		final Map<String, Integer> finalScores = buildRealNameScoreboard();
		for (Player participant : PARTICIPANTS)
		{
			participant.sendPacket(new ExPVPMatchCCRecord(ExPVPMatchCCRecord.FINISH, finalScores, true));
			participant.sendPacket(new ExSendUIEvent(participant, true, true, 0, 0, ""));
		}

		_currentZoneIndex = (_currentZoneIndex + 1) % ZONES.size();
		_rotationStartTime = System.currentTimeMillis();

		// Reset zone kills and scoreboard for the new zone
		ZONE_KILLS.clear();
		ZONE_KILL_NAMES.clear();
		ZONE_KILL_CLASSES.clear();
		SCOREBOARD.clear();
		// Clear DB kill records for the upcoming zone so it starts fresh
		final String upcomingZoneName = ZONES.get((_currentZoneIndex) % ZONES.size()).name;
		resetZoneKillsInDb(upcomingZoneName);

		final PvpZoneData newZone = ZONES.get(_currentZoneIndex);

		for (Player participant : PARTICIPANTS)
		{
			if (participant.isDead())
			{
				participant.doRevive();
				participant.setCurrentHp(participant.getMaxHp());
				participant.setCurrentMp(participant.getMaxMp());
				participant.setCurrentCp(participant.getMaxCp());
			}

			// Mark as internal teleport
			INTERNAL_TELEPORT.add(participant.getObjectId());

			whitelistInBossZones(participant);

			final Location spawn = newZone.getRandomSpawn();
			participant.teleToLocation(spawn.getX() + Rnd.get(-50, 50), spawn.getY() + Rnd.get(-50, 50), spawn.getZ(), 0);

			participant.setPvpFlagLasts(System.currentTimeMillis() + 86400000L);
			participant.startPvPFlag();

			KILL_STREAKS.put(participant.getObjectId(), 0);

			// Re-populate name/class tracking for the new zone
			final String realName = ORIGINAL_NAMES.get(participant.getObjectId());
			if (realName != null)
			{
				ZONE_KILL_NAMES.put(participant.getObjectId(), realName);
			}
			ZONE_KILL_CLASSES.put(participant.getObjectId(), ClassListData.getInstance().getClass(participant.getActiveClass()).getClassName());

			// Reset scoreboard entry for this participant
			SCOREBOARD.put(participant, 0);

			// Show new zone name + timer
			participant.sendPacket(new ExShowScreenMessage("Nueva zona PvP: " + newZone.name, 5000));
			final int newTimerSec = ROTATION_MINUTES * 60;
			participant.sendPacket(new ExSendUIEvent(participant, false, false, newTimerSec, 0, NpcStringId.TIME_REMAINING));
		}

		// Send fresh scoreboard (INITIALIZE) to all participants after the loop
		final Map<String, Integer> freshScores = buildRealNameScoreboard();
		for (Player participant : PARTICIPANTS)
		{
			participant.sendPacket(new ExPVPMatchCCRecord(ExPVPMatchCCRecord.INITIALIZE, freshScores, true));
		}

		scheduleCountdown();

		LOGGER.info("PvpZone: Rotated to zone: " + newZone.name + " (" + PARTICIPANTS.size() + " participants)");
	}

	// ---------------------------------------------------------------------------
	// Top Killer announcement
	// ---------------------------------------------------------------------------
	private void announceTopKiller(String zoneName)
	{
		if (ZONE_KILLS.isEmpty())
		{
			return;
		}

		int topObjectId = 0;
		int topKills = 0;
		for (Map.Entry<Integer, Integer> entry : ZONE_KILLS.entrySet())
		{
			if (entry.getValue() > topKills)
			{
				topKills = entry.getValue();
				topObjectId = entry.getKey();
			}
		}

		if ((topObjectId == 0) || (topKills == 0))
		{
			return;
		}

		// Get the player's real name from zone kill tracking
		String topName = ZONE_KILL_NAMES.get(topObjectId);
		if (topName == null)
		{
			topName = ORIGINAL_NAMES.get(topObjectId);
		}
		if (topName == null)
		{
			topName = "Desconocido";
		}

		final String announcement = topName + " domino la zona PvP " + zoneName + " con " + topKills + " kills!";

		// Global announcement on screen for ALL online players
		Broadcast.toAllOnlinePlayersOnScreen(announcement);
		Broadcast.toAllOnlinePlayers("[Zona PvP] " + announcement, false);

		// Reward the top killer if configured and the player is still online
		if ((TOP_KILLER_REWARD_ITEM_ID > 0) && (TOP_KILLER_REWARD_COUNT > 0) && (topKills >= 1))
		{
			Player topPlayer = null;
			for (Player participant : PARTICIPANTS)
			{
				if (participant.getObjectId() == topObjectId)
				{
					topPlayer = participant;
					break;
				}
			}
			if (topPlayer != null)
			{
				topPlayer.addItem(ItemProcessType.REWARD, TOP_KILLER_REWARD_ITEM_ID, TOP_KILLER_REWARD_COUNT, topPlayer, true);
				final ItemTemplate rewardItem = ItemData.getInstance().getTemplate(TOP_KILLER_REWARD_ITEM_ID);
				final String rewardName = (rewardItem != null) ? rewardItem.getName() : ("Item " + TOP_KILLER_REWARD_ITEM_ID);
				topPlayer.sendPacket(new ExShowScreenMessage("Top Killer! Recibiste " + TOP_KILLER_REWARD_COUNT + " " + rewardName, 5000));
			}
		}

		LOGGER.info("PvpZone: Top killer in " + zoneName + ": " + topName + " (" + topKills + " kills)");
	}

	// ---------------------------------------------------------------------------
	// 5-minute countdown timer (300 -> 0)
	// ---------------------------------------------------------------------------
	private void scheduleCountdown()
	{
		if (_countdownTask != null)
		{
			_countdownTask.cancel(false);
			_countdownTask = null;
		}

		final long rotationMs = ROTATION_MINUTES * 60000L;
		final long countdownStartDelay = rotationMs - 300000L;

		if (countdownStartDelay <= 0)
		{
			// Zone rotates in less than 5 minutes, start countdown immediately
			startCountdownForAll();
		}
		else
		{
			_countdownTask = ThreadPool.schedule(this::startCountdownForAll, countdownStartDelay);
		}
	}

	private void startCountdownForAll()
	{
		final long elapsed = System.currentTimeMillis() - _rotationStartTime;
		final long remaining = Math.max(0, (ROTATION_MINUTES * 60000L) - elapsed);
		final int remainingSec = (int) (remaining / 1000);

		for (Player participant : PARTICIPANTS)
		{
			participant.sendPacket(new ExSendUIEvent(participant, false, false, remainingSec, 0, NpcStringId.TIME_REMAINING));
			participant.sendPacket(new ExShowScreenMessage("La zona PvP cambiara en " + (remainingSec / 60) + " minutos!", 5000));
		}
	}

	// ---------------------------------------------------------------------------
	// Event Listeners
	// ---------------------------------------------------------------------------
	private void addDeathListener(Player player)
	{
		player.addListener(new ConsumerEventListener(player, EventType.ON_CREATURE_DEATH, (OnCreatureDeath event) -> onPlayerDeath(event), this));
	}

	private void addLogoutListener(Player player)
	{
		player.addListener(new ConsumerEventListener(player, EventType.ON_PLAYER_LOGOUT, (OnPlayerLogout event) -> onPlayerLogout(event), this));
	}

	private void addTeleportListener(Player player)
	{
		player.addListener(new ConsumerEventListener(player, EventType.ON_CREATURE_TELEPORTED, (OnCreatureTeleported event) -> onCreatureTeleported(event), this));
	}

	private void removeListeners(Player player)
	{
		for (AbstractEventListener listener : player.getListeners(EventType.ON_CREATURE_DEATH))
		{
			if (listener.getOwner() == this)
			{
				listener.unregisterMe();
			}
		}
		for (AbstractEventListener listener : player.getListeners(EventType.ON_PLAYER_LOGOUT))
		{
			if (listener.getOwner() == this)
			{
				listener.unregisterMe();
			}
		}
		for (AbstractEventListener listener : player.getListeners(EventType.ON_CREATURE_TELEPORTED))
		{
			if (listener.getOwner() == this)
			{
				listener.unregisterMe();
			}
		}
	}

	// ---------------------------------------------------------------------------
	// BossZone whitelist helper
	// ---------------------------------------------------------------------------
	/** Known BossZone IDs that overlap with PvP zone spawns and would kick non-allowed players. */
	private static final int[] BOSS_ZONE_IDS = { 12018 }; // Beleth's Chamber

	private void whitelistInBossZones(Player player)
	{
		if (player == null)
		{
			return;
		}
		final long durationSec = (ROTATION_MINUTES * 60L) + 600L;
		for (int zoneId : BOSS_ZONE_IDS)
		{
			try
			{
				final BossZone bz = ZoneManager.getInstance().getZoneById(zoneId, BossZone.class);
				if (bz != null)
				{
					bz.allowPlayerEntry(player, (int) durationSec);
				}
			}
			catch (Exception e)
			{
				// Zone not loaded or wrong type — ignore
			}
		}
	}

	// ---------------------------------------------------------------------------
	// Scoreboard helper: builds a Map<String, Integer> using real player names
	// sorted by kills (descending) for ExPVPMatchCCRecord custom-name constructor.
	// ---------------------------------------------------------------------------
	private Map<String, Integer> buildRealNameScoreboard()
	{
		// Build list of entries sorted by kills descending
		final List<Map.Entry<Player, Integer>> sorted = new ArrayList<>(SCOREBOARD.entrySet());
		sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

		final Map<String, Integer> result = new LinkedHashMap<>(sorted.size());
		for (Map.Entry<Player, Integer> entry : sorted)
		{
			final Player p = entry.getKey();
			String realName = ORIGINAL_NAMES.get(p.getObjectId());
			if (realName == null)
			{
				realName = p.getName();
			}
			result.put(realName, entry.getValue());
		}
		return result;
	}

	// ---------------------------------------------------------------------------
	// Top 10 Ranking (NPC HTML window, on-demand)
	// ---------------------------------------------------------------------------
	private void showRankingHtml(Player player)
	{
		final PvpZoneData zone = ZONES.get(_currentZoneIndex);

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<center><font color=\"LEVEL\">Top 10 PvP - ").append(zone.name).append("</font></center><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");

		if (ZONE_KILLS.isEmpty())
		{
			sb.append("<center><font color=\"808080\">Aun no hay kills registradas en esta zona.</font></center>");
		}
		else
		{
			final List<Map.Entry<Integer, Integer>> sorted = new ArrayList<>(ZONE_KILLS.entrySet());
			sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

			sb.append("<table width=270>");
			sb.append("<tr><td width=25><font color=\"808080\">#</font></td><td width=110><font color=\"808080\">Jugador</font></td><td width=90><font color=\"808080\">Clase</font></td><td width=45 align=right><font color=\"808080\">Kills</font></td></tr>");

			final int max = Math.min(sorted.size(), 10);
			for (int i = 0; i < max; i++)
			{
				final Map.Entry<Integer, Integer> entry = sorted.get(i);
				final int objId = entry.getKey();
				final int kills = entry.getValue();

				String name = ZONE_KILL_NAMES.get(objId);
				if (name == null)
				{
					name = ORIGINAL_NAMES.get(objId);
				}
				if (name == null)
				{
					name = "???";
				}

				String className = ZONE_KILL_CLASSES.get(objId);
				if (className == null)
				{
					className = "";
				}

				final String color;
				if (i == 0)
				{
					color = "FF0000";
				}
				else if (i == 1)
				{
					color = "FF8C00";
				}
				else if (i == 2)
				{
					color = "FFD700";
				}
				else
				{
					color = "FFFFFF";
				}

				sb.append("<tr><td><font color=\"").append(color).append("\">").append(i + 1).append("</font></td>");
				sb.append("<td><font color=\"").append(color).append("\">").append(name).append("</font></td>");
				sb.append("<td><font color=\"B0B0B0\">").append(className).append("</font></td>");
				sb.append("<td align=right><font color=\"").append(color).append("\">").append(kills).append("</font></td></tr>");
			}
			sb.append("</table>");
		}

		sb.append("<br><img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
		sb.append("<center><button value=\"Volver\" action=\"bypass -h npc_%objectId%_Chat 0\" width=\"100\" height=\"22\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>");
		sb.append("</body></html>");

		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}

	// ---------------------------------------------------------------------------
	// Persist zone kill count to pvp_zone_kills for the web panel ranking.
	// Uses UPSERT: inserts on first kill, updates count on subsequent kills.
	// ---------------------------------------------------------------------------
	private void persistZoneKill(String charName, String zoneName, int totalKills)
	{
		final String UPSERT_SQL = "INSERT INTO pvp_zone_kills (char_name, zone_name, kills, last_kill) "
			+ "VALUES (?, ?, ?, NOW()) "
			+ "ON DUPLICATE KEY UPDATE kills = ?, last_kill = NOW()";
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPSERT_SQL))
		{
			ps.setString(1, charName);
			ps.setString(2, zoneName);
			ps.setInt(3, totalKills);
			ps.setInt(4, totalKills);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PvpZone: Could not persist zone kill for " + charName + ": " + e.getMessage());
		}
	}

	// ---------------------------------------------------------------------------
	// Reset pvp_zone_kills at zone rotation so each zone starts fresh.
	// ---------------------------------------------------------------------------
	private void resetZoneKillsInDb(String zoneName)
	{
		final String DELETE_SQL = "DELETE FROM pvp_zone_kills WHERE zone_name = ?";
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_SQL))
		{
			ps.setString(1, zoneName);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PvpZone: Could not reset zone kills in DB for zone " + zoneName + ": " + e.getMessage());
		}
	}

	// ---------------------------------------------------------------------------
	// Direct DB write of the backup name so recovery is reliable even on crashes
	// that happen within milliseconds of entering the zone.
	// ---------------------------------------------------------------------------
	private void persistRealNameBackup(int charId, String realName)
	{
		final String UPSERT_SQL = "INSERT INTO character_variables (charId, var, val) VALUES (?, 'PVPZ_REAL_NAME', ?) "
			+ "ON DUPLICATE KEY UPDATE val = VALUES(val)";
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPSERT_SQL))
		{
			ps.setInt(1, charId);
			ps.setString(2, realName);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PvpZone: Could not persist PVPZ_REAL_NAME backup for charId=" + charId + ": " + e.getMessage());
		}
	}

	private void clearRealNameBackup(int charId)
	{
		final String DELETE_SQL = "DELETE FROM character_variables WHERE charId = ? AND var = 'PVPZ_REAL_NAME'";
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_SQL))
		{
			ps.setInt(1, charId);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PvpZone: Could not clear PVPZ_REAL_NAME backup for charId=" + charId + ": " + e.getMessage());
		}
	}

	// ---------------------------------------------------------------------------
	// Startup recovery: scan DB for players that crashed inside the PvP zone
	// and restore their real name + teleport to the recovery location.
	// ---------------------------------------------------------------------------
	private void recoverStuckPlayers()
	{
		final String SELECT_SQL = "SELECT charId, val FROM character_variables WHERE var = 'PVPZ_REAL_NAME'";
		final String UPDATE_SQL = "UPDATE characters SET char_name = ?, x = ?, y = ?, z = ? WHERE charId = ?";
		final String DELETE_SQL = "DELETE FROM character_variables WHERE var = 'PVPZ_REAL_NAME'";

		int recovered = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement select = con.prepareStatement(SELECT_SQL);
			ResultSet rs = select.executeQuery())
		{
			while (rs.next())
			{
				final int charId = rs.getInt("charId");
				final String realName = rs.getString("val");
				if ((realName == null) || realName.isEmpty())
				{
					continue;
				}

				try (PreparedStatement update = con.prepareStatement(UPDATE_SQL))
				{
					update.setString(1, realName);
					update.setInt(2, RETURN_X);
					update.setInt(3, RETURN_Y);
					update.setInt(4, RETURN_Z);
					update.setInt(5, charId);
					update.executeUpdate();
				}
				LOGGER.info("PvpZone: Recovered stuck player charId=" + charId + " name='" + realName + "' -> teleported to (" + RETURN_X + "," + RETURN_Y + "," + RETURN_Z + ")");
				recovered++;
			}

			try (PreparedStatement delete = con.prepareStatement(DELETE_SQL))
			{
				delete.executeUpdate();
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("PvpZone: Recovery on startup failed: " + e.getMessage());
		}

		if (recovered > 0)
		{
			LOGGER.info("PvpZone: Startup recovery completed. " + recovered + " player(s) restored.");
		}
	}

	// ---------------------------------------------------------------------------
	// Server shutdown / restart: restore names BEFORE players are saved
	// ---------------------------------------------------------------------------
	private void onServerShutdown()
	{
		try
		{
			CustomPvpZoneRegistry.clear();

			if (PARTICIPANTS.isEmpty())
			{
				return;
			}

			LOGGER.info("PvpZone: Server shutdown detected. Restoring real names for " + PARTICIPANTS.size() + " participant(s)...");

			for (Player player : PARTICIPANTS)
			{
				if (player == null)
				{
					continue;
				}
				try
				{
					// Restore clan identity before saving
					final ClanBackup cb = CLAN_BACKUPS.remove(player.getObjectId());
					if (cb != null)
					{
						cb.restore(player);
					}

					// Restore original title
					String originalTitle = ORIGINAL_TITLES.remove(player.getObjectId());
					if (originalTitle == null)
					{
						originalTitle = player.getVariables().getString("PVPZ_REAL_TITLE", null);
					}
					if (originalTitle != null)
					{
						player.setTitle(originalTitle);
					}
					player.getVariables().remove("PVPZ_REAL_TITLE");

					String realName = ORIGINAL_NAMES.get(player.getObjectId());
					if (realName == null)
					{
						realName = player.getVariables().getString("PVPZ_REAL_NAME", null);
					}
					if ((realName != null) && !realName.equals(player.getName()))
					{
						player.setName(realName);
					}

					// Remove the hero granted by streak (Olympiad hero is not in this set)
					if (STREAK_HEROES.remove(player.getObjectId()))
					{
						player.setHero(false);
					}

					// Clear PvP flag so it doesn't carry over after restart
					player.setPvpFlagLasts(0);

					// Clear the persisted backup
					player.getVariables().remove("PVPZ_REAL_NAME");

					// Force immediate DB save so the corrected name is persisted
					player.getVariables().storeMe();
					player.storeMe();
				}
				catch (Exception inner)
				{
					LOGGER.warning("PvpZone: Error restoring name for " + player.getObjectId() + " during shutdown: " + inner.getMessage());
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("PvpZone: Shutdown hook failed: " + e.getMessage());
		}
	}

	// ---------------------------------------------------------------------------
	// Login: restore real name if player was in PvP zone at last logout/crash
	// ---------------------------------------------------------------------------
	private void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		boolean needsBroadcast = false;

		final String backupName = player.getVariables().getString("PVPZ_REAL_NAME", null);
		if ((backupName != null) && !backupName.isEmpty() && !backupName.equals(player.getName()))
		{
			player.setName(backupName);
			needsBroadcast = true;
			LOGGER.info("PvpZone: Restored real name '" + backupName + "' on login for objectId " + player.getObjectId());
		}
		if (backupName != null)
		{
			player.getVariables().remove("PVPZ_REAL_NAME");
		}

		// Restore original title if crash/logout happened while inside the zone
		final String backupTitle = player.getVariables().getString("PVPZ_REAL_TITLE", null);
		if (backupTitle != null)
		{
			player.setTitle(backupTitle);
			needsBroadcast = true;
			player.getVariables().remove("PVPZ_REAL_TITLE");
		}

		if (needsBroadcast)
		{
			player.broadcastUserInfo();
			player.getVariables().storeMe();
		}
	}

	// ---------------------------------------------------------------------------
	// Public API
	// ---------------------------------------------------------------------------
	public static int getParticipantCount()
	{
		return PARTICIPANTS.size();
	}

	public static void main(String[] args)
	{
		new PvpZone();
	}
}
