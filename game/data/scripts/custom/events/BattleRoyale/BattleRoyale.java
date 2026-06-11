/*
 * Battle Royale Event — Last Man Standing
 * Zona: Kamaloka, centro -14539 -17892 -10680
 * Solo control desde Admin Panel (seccion Events).
 * Registro via voiced command .registerbr
 */
package custom.events.BattleRoyale;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanPrivileges;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureTeleported;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.model.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.model.punishment.PunishmentTask;
import org.l2jmobius.gameserver.model.punishment.PunishmentType;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.model.script.Event;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.RadarControl;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author Custom — BattleRoyale
 */
public class BattleRoyale extends Event
{
	// =========================================================================
	// Estado del evento
	// =========================================================================
	public enum EventState
	{
		INACTIVE,
		REGISTRATION,
		WAITING,
		ACTIVE
	}

	private static final String CONFIG          = "config/Custom/BattleRoyale.ini";
	private static final int    C_NPC_ID        = 70023;
	private static final int    C_BOX_NPC_ID    = 70024; // NPC-caja que se spawnea en la arena
	private static final int    ARENA_TEMPLATE_ID = 9002; // game/data/instances/BattleRoyaleArena.xml

	// =========================================================================
	// Config
	// =========================================================================
	private static boolean C_ENABLED = true;
	private static int C_MIN_PL = 2;
	private static int C_MAX_PL = 20;
	private static int C_MIN_LV = 1;
	private static int C_MAX_LV = 85;
	private static boolean C_DUALBOX = false;
	private static int C_REG_DURATION  = 5;
	private static int C_DURATION = 30;
	private static int C_PREFIGHT_SECS = 60;
	private static int C_READY_TIMEOUT = 30;
	private static int C_INACTIVITY_TIMEOUT = 180;
	private static int C_CENTER_X = -14539;
	private static int C_CENTER_Y = -17892;
	private static int C_CENTER_Z = -10680;
	private static int C_INITIAL_RADIUS = 2000;
	private static int C_SHRINK_AMOUNT = 350;
	private static int C_MIN_RADIUS = 300;
	private static int C_ZONE_START_MINUTE = 5;
	private static int C_SHRINK_INTERVAL = 5;
	private static int C_ZONE_DAMAGE = 1000;
	private static int C_BOX_ITEM_ID = 20515;
	private static int C_BOX_INTERVAL = 5;
	private static int C_PULSE_SKILL_ID = 0;
	private static String C_TITLE = "Battle Royale";
	private static boolean C_HIDE_CLAN = true;
	private static boolean C_WINNER_HERO = true;
	private static int C_HERO_DAYS = 7;
	private static int C_RETURN_X = 83400;
	private static int C_RETURN_Y = 147943;
	private static int C_RETURN_Z = -3400;
	private static final List<Location> C_SPAWNS = new ArrayList<>();
	private static final List<ItemHolder> C_BOX_REWARDS = new ArrayList<>();
	private static final List<ItemHolder> C_WIN_REWARDS = new ArrayList<>();
	private static String C_ANN1 = "=== BATTLE ROYALE ===";
	private static String C_ANN2 = "Registro abierto! Habla con el Event Manager Battle Royale en Giran.";
	private static String C_ANN3 = "IMPORTANTE: Buffeate ANTES de entrar — no hay buffer en la arena!";

	// =========================================================================
	// Estado runtime
	// =========================================================================
	private static volatile EventState _state = EventState.INACTIVE;
	/** Jugadores registrados (antes de iniciar) */
	private static final Set<Player> REGISTERED = ConcurrentHashMap.newKeySet();
	/** Jugadores dentro de la arena */
	private static final Set<Player> PARTICIPANTS = ConcurrentHashMap.newKeySet();
	/** Jugadores vivos actualmente */
	private static final Set<Player> ALIVE = ConcurrentHashMap.newKeySet();
	/** Jugadores que confirmaron "listo" para el ready check */
	private static final Set<Integer> READY_CONFIRMED = ConcurrentHashMap.newKeySet();
	/** Timers individuales de ready-check por jugador */
	private static final Map<Integer, ScheduledFuture<?>> READY_TIMERS = new ConcurrentHashMap<>();
	/** Backup de datos de clan por jugador */
	private static final Map<Integer, ClanBackup> CLAN_BACKUPS = new ConcurrentHashMap<>();
	/** Titulo original del jugador */
	private static final Map<Integer, String> ORIGINAL_TITLES = new ConcurrentHashMap<>();
	/** Nombre original del jugador */
	private static final Map<Integer, String> ORIGINAL_NAMES = new ConcurrentHashMap<>();
	/** Players con ban de chat activo por este evento */
	private static final Set<Integer> CHAT_BANNED = ConcurrentHashMap.newKeySet();
	/** Flag para identificar teleports internos del evento */
	private static final Set<Integer> INTERNAL_TELEPORT = ConcurrentHashMap.newKeySet();
	/** Ultima actividad del jugador (ms) */
	private static final Map<Integer, Long> LAST_ACTIVITY = new ConcurrentHashMap<>();
	/** Ultima posicion del jugador para deteccion de inactividad */
	private static final Map<Integer, Location> LAST_POSITION = new ConcurrentHashMap<>();
	/** NPCs de caja spawneados en la arena (al hablar entregan la Huge Box) */
	private static final List<Npc> SPAWNED_BOXES = new ArrayList<>();
	/** Radio actual de la zona segura */
	private static volatile int _currentRadius = 2000;
	/** Indica si el daño de zona esta activo */
	private static volatile boolean _zoneDamageActive = false;
	/** Listener global de login */
	private static AbstractEventListener _loginListener = null;
	/** Task de daño/zona (cada segundo) */
	private static ScheduledFuture<?> _zoneTask = null;
	/** Task de inactividad (cada 30s) */
	private static ScheduledFuture<?> _inactivityTask = null;
	/** true cuando el inicio fue forzado por GM — saltea el countdown de 60s pre-teleport */
	private static volatile boolean _gmForced = false;
	/** Instancia que aísla a los jugadores del evento del mundo principal */
	private static BattleRoyaleWorld _arenaWorld = null;
	/** Jugadores con la flecha de radar activa (están fuera de la zona segura) */
	private static final Set<Integer> RADAR_ACTIVE = ConcurrentHashMap.newKeySet();

	// =========================================================================
	// Backup de datos de clan
	// =========================================================================
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
			clan = player.getClan();
			title = player.getTitle();
			pledgeType = player.getPledgeType();
			powerGrade = player.getPowerGrade();
			lvlJoinedAcademy = player.getLvlJoinedAcademy();
			apprentice = player.getApprentice();
			sponsor = player.getSponsor();
			clanPrivileges = player.getClanPrivileges();
		}

		void restore(Player player)
		{
			if (clan != null)
			{
				player.setClan(clan);
				player.setPledgeType(pledgeType);
				player.setPowerGrade(powerGrade);
				player.setLvlJoinedAcademy(lvlJoinedAcademy);
				player.setApprentice(apprentice);
				player.setSponsor(sponsor);
				player.setClanPrivileges(clanPrivileges);
			}
		}
	}

	// =========================================================================
	// Instance Zone de la arena
	// =========================================================================
	private static class BattleRoyaleWorld extends InstanceWorld {}

	// =========================================================================
	// Constructor
	// =========================================================================
	private BattleRoyale()
	{
		loadConfig();

		if (!C_ENABLED)
		{
			LOGGER.info("BattleRoyale: deshabilitado.");
			return;
		}

		_loginListener = new ConsumerEventListener(
			Containers.Players(),
			EventType.ON_PLAYER_LOGIN,
			(OnPlayerLogin ev) -> onPlayerLogin(ev),
			this);
		Containers.Players().addListener(_loginListener);

		// Registrar NPC de registro
		addStartNpc(C_NPC_ID);
		addTalkId(C_NPC_ID);
		addFirstTalkId(C_NPC_ID);
		// Registrar NPC de caja (se spawnea en la arena, al hablar entrega la Huge Box)
		addTalkId(C_BOX_NPC_ID);
		addFirstTalkId(C_BOX_NPC_ID);
		// NPC de registro: su spawn se define por XML (spawnlist), no se fija aqui.

		recoverPlayers();

		LOGGER.info("BattleRoyale: listo. Min=" + C_MIN_PL + " Max=" + C_MAX_PL
			+ " Duration=" + C_DURATION + "min ZoneStart=" + C_ZONE_START_MINUTE + "min");
	}

	// =========================================================================
	// Config
	// =========================================================================
	private void loadConfig()
	{
		final Properties p = new Properties();
		try (FileInputStream fis = new FileInputStream(CONFIG))
		{
			p.load(fis);
		}
		catch (IOException e)
		{
			LOGGER.warning("BattleRoyale: no se pudo cargar " + CONFIG + " — usando defaults.");
		}

		C_ENABLED = bl(p, "BattleRoyaleEnabled", true);
		C_MIN_PL = in(p, "BattleRoyaleMinPlayers", 2);
		C_MAX_PL = in(p, "BattleRoyaleMaxPlayers", 20);
		C_MIN_LV = in(p, "BattleRoyaleMinLevel", 1);
		C_MAX_LV = in(p, "BattleRoyaleMaxLevel", 85);
		C_DUALBOX = bl(p, "BattleRoyaleAllowDualbox", false);
		C_REG_DURATION = in(p, "BattleRoyaleRegistrationDuration", 5);
		C_DURATION = in(p, "BattleRoyaleEventDuration", 30);
		C_PREFIGHT_SECS = in(p, "BattleRoyalePreFightCountdown", 60);
		C_READY_TIMEOUT = in(p, "BattleRoyaleReadyTimeout", 30);
		C_INACTIVITY_TIMEOUT = in(p, "BattleRoyaleInactivityTimeout", 180);
		C_CENTER_X = in(p, "BattleRoyaleCenterX", -14539);
		C_CENTER_Y = in(p, "BattleRoyaleCenterY", -17892);
		C_CENTER_Z = in(p, "BattleRoyaleCenterZ", -10680);
		C_INITIAL_RADIUS = in(p, "BattleRoyaleInitialRadius", 2000);
		C_SHRINK_AMOUNT = in(p, "BattleRoyaleRadiusShrinkAmount", 350);
		C_MIN_RADIUS = in(p, "BattleRoyaleMinRadius", 300);
		C_ZONE_START_MINUTE = in(p, "BattleRoyaleZoneStartMinute", 5);
		C_SHRINK_INTERVAL = in(p, "BattleRoyaleZoneShrinkInterval", 5);
		C_ZONE_DAMAGE = in(p, "BattleRoyaleZoneDamagePerSecond", 1000);
		C_BOX_ITEM_ID = in(p, "BattleRoyaleBoxItemId", 20515);
		C_BOX_INTERVAL = in(p, "BattleRoyaleBoxSpawnInterval", 5);
		C_PULSE_SKILL_ID = in(p, "BattleRoyaleZonePulseSkillId", 0);
		C_TITLE = p.getProperty("BattleRoyaleTitle", "Battle Royale").trim();
		C_HIDE_CLAN = bl(p, "BattleRoyaleHideClan", true);
		C_WINNER_HERO = bl(p, "BattleRoyaleWinnerHero", true);
		C_HERO_DAYS = in(p, "BattleRoyaleHeroDays", 7);
		C_RETURN_X = in(p, "BattleRoyaleReturnX", 83400);
		C_RETURN_Y = in(p, "BattleRoyaleReturnY", 147943);
		C_RETURN_Z = in(p, "BattleRoyaleReturnZ", -3400);
		C_ANN1 = p.getProperty("BattleRoyaleAnnouncement1", "=== BATTLE ROYALE ===").trim();
		C_ANN2 = p.getProperty("BattleRoyaleAnnouncement2", "Registro abierto! Usa .registerbr para unirte.").trim();
		C_ANN3 = p.getProperty("BattleRoyaleAnnouncement3", "IMPORTANTE: Buffeate ANTES de entrar!").trim();

		C_SPAWNS.clear();
		final String spawnsStr = p.getProperty("BattleRoyaleSpawns", C_CENTER_X + "," + C_CENTER_Y + "," + C_CENTER_Z).trim();
		for (String entry : spawnsStr.split(";"))
		{
			final String[] parts = entry.trim().split(",");
			if (parts.length >= 3)
			{
				try
				{
					C_SPAWNS.add(new Location(
						Integer.parseInt(parts[0].trim()),
						Integer.parseInt(parts[1].trim()),
						Integer.parseInt(parts[2].trim())));
				}
				catch (NumberFormatException ignored)
				{
				}
			}
		}
		if (C_SPAWNS.isEmpty())
		{
			C_SPAWNS.add(new Location(C_CENTER_X, C_CENTER_Y, C_CENTER_Z));
		}

		C_BOX_REWARDS.clear();
		parseItemList(p, "BattleRoyaleBoxRewards", "57,50000", C_BOX_REWARDS);

		C_WIN_REWARDS.clear();
		parseItemList(p, "BattleRoyaleWinnerRewards", "6673,5;57,500000", C_WIN_REWARDS);
	}

	private void parseItemList(Properties p, String key, String def, List<ItemHolder> target)
	{
		for (String entry : p.getProperty(key, def).trim().split(";"))
		{
			final String[] parts = entry.trim().split(",");
			if (parts.length == 2)
			{
				try
				{
					target.add(new ItemHolder(Integer.parseInt(parts[0].trim()), Long.parseLong(parts[1].trim())));
				}
				catch (NumberFormatException ignored)
				{
				}
			}
		}
	}

	private boolean bl(Properties p, String k, boolean d)
	{
		return Boolean.parseBoolean(p.getProperty(k, String.valueOf(d)).trim());
	}

	private int in(Properties p, String k, int d)
	{
		try
		{
			return Integer.parseInt(p.getProperty(k, String.valueOf(d)).trim());
		}
		catch (NumberFormatException e)
		{
			return d;
		}
	}

	// =========================================================================
	// API publica para BattleRoyaleVoice
	// =========================================================================
	public static boolean isRegistrationOpen()
	{
		return _state == EventState.REGISTRATION;
	}

	/** Retorna true si el jugador tiene una confirmacion de ready pendiente. */
	public static boolean isPendingReady(Player player)
	{
		return player != null && READY_TIMERS.containsKey(player.getObjectId());
	}

	public static boolean isInEvent(Player player)
	{
		return PARTICIPANTS.contains(player) || REGISTERED.contains(player);
	}

	/** Devuelve true solo si el jugador esta en la arena activamente (no solo registrado). */
	public static boolean isActiveParticipant(Player player)
	{
		return ALIVE.contains(player);
	}

	public static int getRegisteredCount()
	{
		return REGISTERED.size();
	}

	public static int getMinPlayers()
	{
		return C_MIN_PL;
	}

	public static int getMaxPlayers()
	{
		return C_MAX_PL;
	}

	public static EventState getState()
	{
		return _state;
	}

	/**
	 * Intenta registrar al jugador. Devuelve null en exito o mensaje de error.
	 */
	public static String tryRegister(Player player)
	{
		if (_state != EventState.REGISTRATION)
		{
			return "El registro no esta abierto en este momento.";
		}
		if (REGISTERED.contains(player))
		{
			return null;
		}
		if (REGISTERED.size() >= C_MAX_PL)
		{
			return "El evento esta lleno (" + C_MAX_PL + " jugadores maximo).";
		}
		if (player.getLevel() < C_MIN_LV || player.getLevel() > C_MAX_LV)
		{
			return "Nivel requerido: " + C_MIN_LV + " - " + C_MAX_LV + ".";
		}
		if (player.isInParty())
		{
			return "Debes salir del party antes de registrarte.";
		}
		if (player.isRegisteredOnEvent() || player.isOnEvent() || player.getBlockCheckerArena() > -1)
		{
			return "Ya estas inscripto en otro evento.";
		}
		if (player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player))
		{
			return "No puedes registrarte durante las Olimpiadas.";
		}
		if (player.isInSiege() || player.isInsideZone(ZoneId.SIEGE))
		{
			return "No puedes registrarte durante un asedio.";
		}
		if (!C_DUALBOX && DualboxCheckConfig.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP > 0)
		{
			if (!AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.L2EVENT_ID, player, 1))
			{
				return "Ya hay una cuenta de tu IP registrada en este evento.";
			}
		}

		REGISTERED.add(player);
		player.setRegisteredOnEvent(true);
		addLogoutListener(player);
		return null;
	}

	/**
	 * Cancela el registro del jugador.
	 */
	public static boolean tryUnregister(Player player)
	{
		if (_state != EventState.REGISTRATION)
		{
			return false;
		}
		if (!REGISTERED.remove(player))
		{
			return false;
		}
		player.setRegisteredOnEvent(false);
		removePlayerListeners(player);
		removeDualbox(player);
		return true;
	}

	// =========================================================================
	// eventStart — abre el registro
	// =========================================================================
	@Override
	public boolean eventStart(Player eventMaker)
	{
		if (!C_ENABLED)
		{
			if (eventMaker != null) eventMaker.sendMessage("BattleRoyale esta deshabilitado en la config.");
			return false;
		}
		if (_state != EventState.INACTIVE)
		{
			if (eventMaker != null) eventMaker.sendMessage("BattleRoyale: ya activo (estado: " + _state + ").");
			return false;
		}

		setState(EventState.REGISTRATION);
		REGISTERED.clear();
		READY_CONFIRMED.clear();

		if (!C_DUALBOX && DualboxCheckConfig.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP > 0)
		{
			AntiFeedManager.getInstance().registerEvent(AntiFeedManager.L2EVENT_ID);
			AntiFeedManager.getInstance().clear(AntiFeedManager.L2EVENT_ID);
		}

		Broadcast.toAllOnlinePlayers(C_ANN1);
		Broadcast.toAllOnlinePlayers(C_ANN2);
		Broadcast.toAllOnlinePlayers(C_ANN3);
		Broadcast.toAllOnlinePlayers("Battle Royale: Tienes " + C_REG_DURATION + " minuto(s) para registrarte. El evento comienza automaticamente!");

		// Aviso a 1 minuto del cierre
		if (C_REG_DURATION > 1)
		{
			startQuestTimer("RegWarning1", (long)(C_REG_DURATION - 1) * 60_000L, null, null);
		}
		// Cierre automatico del registro
		startQuestTimer("RegistrationEnd", (long) C_REG_DURATION * 60_000L, null, null);

		if (eventMaker != null) eventMaker.sendMessage("BattleRoyale: registro abierto por " + C_REG_DURATION + " minuto(s).");
		return true;
	}

	/**
	 * Inicia el evento con los jugadores registrados, ignorando el minimo.
	 */
	public void forceStart(Player admin)
	{
		if (_state == EventState.INACTIVE)
		{
			// Setup minimo sin anuncios globales ni timers de registro automatico
			if (!C_ENABLED)
			{
				if (admin != null) admin.sendMessage("BattleRoyale esta deshabilitado en la config.");
				return;
			}
			setState(EventState.REGISTRATION);
			REGISTERED.clear();
			READY_CONFIRMED.clear();
			if (!C_DUALBOX && DualboxCheckConfig.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP > 0)
			{
				AntiFeedManager.getInstance().registerEvent(AntiFeedManager.L2EVENT_ID);
				AntiFeedManager.getInstance().clear(AntiFeedManager.L2EVENT_ID);
			}
			if (admin != null) admin.sendMessage("BattleRoyale: registro abierto por orden del GM. Esperando jugadores...");
		}

		if (_state == EventState.REGISTRATION)
		{
			if (REGISTERED.size() < 2)
			{
				if (admin != null) admin.sendMessage("BattleRoyale: necesitas al menos 2 jugadores registrados.");
				return;
			}
			beginReadyCheck();
		}
		else if (admin != null)
		{
			admin.sendMessage("BattleRoyale: ya en progreso (estado: " + _state + ").");
		}
	}

	// =========================================================================
	// eventStop — cancela el evento
	// =========================================================================
	@Override
	public boolean eventStop()
	{
		if (_state == EventState.INACTIVE)
		{
			return false;
		}

		cancelAllTimers();
		stopZoneTasks();

		for (ScheduledFuture<?> t : READY_TIMERS.values())
		{
			if (t != null) t.cancel(false);
		}
		READY_TIMERS.clear();

		for (Player p : new ArrayList<>(PARTICIPANTS))
		{
			disqualify(p, true);
		}
		for (Player p : new ArrayList<>(REGISTERED))
		{
			p.setRegisteredOnEvent(false);
			removePlayerListeners(p);
			removeDualbox(p);
		}
		REGISTERED.clear();
		ALIVE.clear();

		// Destruir la instancia despues de sacar a todos los jugadores
		destroyArenaInstance();

		despawnBoxes();
		resetState();

		Broadcast.toAllOnlinePlayers("Battle Royale: evento cancelado por el administrador.");
		return true;
	}

	// =========================================================================
	// eventBypass — panel de admin
	// =========================================================================
	@Override
	public boolean eventBypass(Player player, String command)
	{
		if (player == null) return false;

		// BRReady: cualquier jugador registrado puede confirmar (no requiere GM)
		if ("BRReady".equals(command))
		{
			if (_state == EventState.WAITING)
			{
				handleReadyConfirm(player);
			}
			else
			{
				player.sendMessage("[Battle Royale] La confirmacion ya no esta disponible.");
			}
			return true;
		}

		// El resto de comandos es solo para GM
		if (!player.isGM()) return false;

		switch (command)
		{
			case "Start":
				eventStart(player);
				break;
			case "ForceStart":
				forceStart(player);
				break;
			case "Stop":
				eventStop();
				player.sendMessage("BattleRoyale: evento detenido.");
				break;
			default:
				player.sendMessage("BattleRoyale bypass desconocido: " + command);
				break;
		}
		return true;
	}

	// =========================================================================
	// onFirstTalk — NPC de registro en Giran
	// =========================================================================
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		// NPC-caja: entrega la Huge Box al participante y desaparece
		if (npc.getId() == C_BOX_NPC_ID)
		{
			claimBox(npc, player);
			return null;
		}
		if (npc.getId() != C_NPC_ID) return null;
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setHtml(buildBrokerHtml(player));
		player.sendPacket(html);
		return null;
	}

	/**
	 * Entrega la Huge Box al jugador que habla con un NPC-caja y elimina el NPC.
	 * Solo participantes vivos del evento pueden reclamarla.
	 */
	private void claimBox(Npc npc, Player player)
	{
		if (player == null || !ALIVE.contains(player))
		{
			return;
		}

		// Evitar doble reclamo si dos jugadores hablan casi a la vez
		synchronized (SPAWNED_BOXES)
		{
			if (!SPAWNED_BOXES.remove(npc))
			{
				return;
			}
		}

		player.addItem(ItemProcessType.REWARD, C_BOX_ITEM_ID, 1, player, true);
		player.sendPacket(new ExShowScreenMessage("Reclamaste una Huge Box!", 3000));
		npc.deleteMe();
	}

	private String buildBrokerHtml(Player player)
	{
		final boolean isRegistered = REGISTERED.contains(player);
		final int count = REGISTERED.size();
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<center>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\"><br>");
		sb.append("<font color=\"LEVEL\">Battle Royale</font><br>");
		sb.append("<font color=\"808080\">Ultimo Jugador en Pie</font><br><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");

		switch (_state)
		{
			case REGISTRATION:
			{
				sb.append("<table width=\"270\">");
				sb.append("<tr><td><font color=\"808080\">Estado:</font></td><td><font color=\"00FF00\">Registro Abierto</font></td></tr>");
				sb.append("<tr><td><font color=\"808080\">Inscriptos:</font></td><td><font color=\"LEVEL\">").append(count).append(" / ").append(C_MAX_PL).append("</font></td></tr>");
				sb.append("<tr><td><font color=\"808080\">Minimo:</font></td><td><font color=\"LEVEL\">").append(C_MIN_PL).append(" jugadores</font></td></tr>");
				sb.append("<tr><td><font color=\"808080\">Nivel:</font></td><td><font color=\"LEVEL\">").append(C_MIN_LV).append(" - ").append(C_MAX_LV).append("</font></td></tr>");
				sb.append("</table><br>");
				sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br><br>");
				if (isRegistered)
				{
					sb.append("<font color=\"00FF00\">Estas registrado!</font><br>");
					sb.append("<font color=\"FF6347\">IMPORTANTE: Buffeate antes de confirmar entrada.</font><br><br>");
					sb.append("<button value=\"Cancelar registro\" action=\"bypass -h Script BattleRoyale BRUnregister\" width=\"200\" height=\"30\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
				}
				else
				{
					sb.append("<font color=\"FF6347\">No hay buffer en la arena — buffeate ANTES!</font><br><br>");
					sb.append("<button value=\"Registrarme\" action=\"bypass -h Script BattleRoyale BRRegister\" width=\"200\" height=\"30\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
				}
				break;
			}
			case WAITING:
			{
				sb.append("<font color=\"FFAA00\">El evento esta a punto de comenzar!</font><br><br>");
				if (isPendingReady(player))
				{
					sb.append("<font color=\"FF6347\">Tienes una confirmacion pendiente!</font><br>");
					sb.append("<font color=\"FF6347\">Buffeate AHORA antes de entrar a la arena.</font><br><br>");
					sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br><br>");
					sb.append("<button value=\"Estoy listo! (Confirmar entrada)\" action=\"bypass -h Script BattleRoyale BRReady\" width=\"220\" height=\"30\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
				}
				else
				{
					sb.append("<font color=\"808080\">El registro ya cerro. Espera el proximo evento.</font><br>");
				}
				break;
			}
			case ACTIVE:
			{
				sb.append("<font color=\"FF6347\">El evento esta en curso.</font><br>");
				sb.append("<font color=\"808080\">Podras participar en el proximo evento.</font><br>");
				break;
			}
			default:
			{
				sb.append("<font color=\"808080\">El evento no esta activo en este momento.</font><br><br>");
				sb.append("El Battle Royale es iniciado manualmente por un GM.<br>");
				sb.append("<font color=\"808080\">Vuelve cuando el registro este abierto.</font><br>");
				break;
			}
		}

		sb.append("<br><img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\">");
		sb.append("</center>");
		sb.append("</body></html>");
		return sb.toString();
	}

	// =========================================================================
	// onEvent — timers y dialogo NPC (via startQuestTimer)
	// =========================================================================
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		// Registro desde NPC
		if ("BRRegister".equals(event) && player != null)
		{
			final String error = tryRegister(player);
			if (error != null)
			{
				player.sendMessage("[Battle Royale] No puedes registrarte: " + error);
			}
			else
			{
				player.sendMessage("[Battle Royale] Registro exitoso! Vuelve aqui cuando el evento comience.");
			}
			final NpcHtmlMessage html = new NpcHtmlMessage();
			html.setHtml(buildBrokerHtml(player));
			player.sendPacket(html);
			return null;
		}

		// Desregistro desde NPC
		if ("BRUnregister".equals(event) && player != null)
		{
			if (tryUnregister(player))
			{
				player.sendMessage("[Battle Royale] Registro cancelado.");
			}
			final NpcHtmlMessage html = new NpcHtmlMessage();
			html.setHtml(buildBrokerHtml(player));
			player.sendPacket(html);
			return null;
		}

		// Confirmacion de ready — ahora viene via eventBypass (bypass -h event)
		if ("BRReady".equals(event) && player != null)
		{
			handleReadyConfirm(player);
			return null;
		}

		// Timeout del ready-check
		if (event.startsWith("ReadyTimeout_") && player != null)
		{
			handleReadyTimeout(player);
			return null;
		}

		// Limpieza post-muerte de jugador
		if (event.startsWith("Death_") && player != null)
		{
			if (!ALIVE.contains(player))
			{
				disqualify(player, true);
			}
			return null;
		}

		switch (event)
		{
			// ---- Cierre automatico de inscripcion ----
			case "RegWarning1":
			{
				if (_state == EventState.REGISTRATION)
				{
					Broadcast.toAllOnlinePlayers("Battle Royale: 1 minuto para cerrar el registro! (" + REGISTERED.size() + " inscritos)");
				}
				break;
			}
			case "RegistrationEnd":
			{
				if (_state == EventState.REGISTRATION)
				{
					if (REGISTERED.size() >= C_MIN_PL)
					{
						beginReadyCheck();
					}
					else
					{
						Broadcast.toAllOnlinePlayers("Battle Royale: registro cerrado. Jugadores insuficientes (" + REGISTERED.size() + "/" + C_MIN_PL + " minimo). Cancelado.");
						eventStop();
					}
				}
				break;
			}
			// ---- Cuenta regresiva pre-teleport ----
			case "RegToArena30":
			{
				for (Player online : World.getInstance().getPlayers())
				{
					if (online != null && READY_CONFIRMED.contains(online.getObjectId()))
					{
						online.sendMessage("[Battle Royale] 30 segundos para el traslado!");
						online.sendPacket(new ExShowScreenMessage("Traslado en 30 segundos!", ExShowScreenMessage.TOP_CENTER, 3000, 0, true, false));
					}
				}
				break;
			}
			case "RegToArena10":
			{
				for (Player online : World.getInstance().getPlayers())
				{
					if (online != null && READY_CONFIRMED.contains(online.getObjectId()))
					{
						online.sendMessage("[Battle Royale] 10 segundos para el traslado!");
						online.sendPacket(new ExShowScreenMessage("Traslado en 10 segundos!", ExShowScreenMessage.TOP_CENTER, 3000, 0, true, false));
					}
				}
				break;
			}
			case "TeleportArena":
			{
				startWaitingPhase();
				break;
			}
			// ---- Aviso 1 minuto antes de que active la zona ----
			case "ZoneWarning":
			{
				for (Player p : PARTICIPANTS)
				{
					if (p == null || !p.isOnline()) continue;
					p.sendMessage("[Battle Royale] La zona de dano activa en 1 MINUTO! Muevete al centro ahora!");
					p.sendPacket(new ExShowScreenMessage("ZONA EN 1 MINUTO — Muevete al centro!", ExShowScreenMessage.TOP_CENTER, 5000, 0, true, false));
				}
				break;
			}
			// ---- Countdown pre-pelea (dentro de la arena) ----
			case "PFCount60": broadcastScreenAll("El combate comienza en 60 segundos!", 3000); break;
			case "PFCount30": broadcastScreenAll("El combate comienza en 30 segundos!", 3000); break;
			case "PFCount10": broadcastScreenAll("El combate comienza en 10 segundos!", 3000); break;
			case "PFCount5":  broadcastScreenAll("5", 2000); break;
			case "PFCount4":  broadcastScreenAll("4", 2000); break;
			case "PFCount3":  broadcastScreenAll("3", 2000); break;
			case "PFCount2":  broadcastScreenAll("2", 2000); break;
			case "PFCount1":  broadcastScreenAll("1", 2000); break;
			case "StartFight": startFight(); break;
			case "EventEnd":   endEvent(); break;
			case "ZonePhase":
				if (!_zoneDamageActive)
				{
					// Primera fase: el daño se activa al radio inicial (2000), sin achicar aun
					_zoneDamageActive = true;
					Broadcast.toAllOnlinePlayers("Battle Royale: LA ZONA DE DANO ESTA ACTIVA! Radio: " + _currentRadius + " unidades.");
					for (Player p : PARTICIPANTS)
					{
						if (p == null || !p.isOnline()) continue;
						p.sendPacket(new ExShowScreenMessage("ZONA ACTIVA — Radio: " + _currentRadius + " — Quedate dentro!", ExShowScreenMessage.TOP_CENTER, 5000, 0, true, false));
					}
				}
				else
				{
					// Fases siguientes: achicar el radio (2000 -> 1500 -> 1000 -> 500)
					shrinkZone();
				}
				spawnBoxes();
				scheduleNextZonePhase();
				break;
		}

		return null;
	}

	// =========================================================================
	// Ready check
	// =========================================================================
	private void beginReadyCheck()
	{
		setState(EventState.WAITING);
		cancelAllTimers();

		final List<Player> toCheck = new ArrayList<>(REGISTERED);
		REGISTERED.clear();

		if (toCheck.isEmpty())
		{
			setState(EventState.INACTIVE);
			return;
		}

		Broadcast.toAllOnlinePlayers("Battle Royale: registro cerrado! " + toCheck.size() + " jugadores tienen " + C_READY_TIMEOUT + "s para confirmar.");

		for (Player p : toCheck)
		{
			sendReadyHtml(p);
			final int pid = p.getObjectId();
			final ScheduledFuture<?> t = ThreadPool.schedule(
				() -> startQuestTimer("ReadyTimeout_" + pid, 0, null, p),
				(long) C_READY_TIMEOUT * 1000L);
			READY_TIMERS.put(pid, t);
		}
	}

	private void sendReadyHtml(Player player)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<center>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\"><br>");
		sb.append("<font color=\"LEVEL\">Battle Royale</font><br><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
		sb.append("<font color=\"FFAA00\">El evento esta a punto de comenzar!</font><br><br>");
		sb.append("<font color=\"FF6347\">No hay buffer en la arena — buffeate AHORA!</font><br><br>");
		sb.append("<table width=\"270\">");
		sb.append("<tr><td><font color=\"808080\">Tiempo para confirmar:</font></td><td><font color=\"FF6347\">").append(C_READY_TIMEOUT).append("s</font></td></tr>");
		sb.append("</table><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br><br>");
		sb.append("<button value=\"Estoy listo! (Entrar)\" action=\"bypass -h Script BattleRoyale BRReady\" width=\"220\" height=\"30\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br>");
		sb.append("<font color=\"AAAAAA\">Tambien puedes escribir: <font color=\"FFFF00\">.brready</font></font><br>");
		sb.append("<font color=\"808080\">Sin confirmar en ").append(C_READY_TIMEOUT).append("s seras descalificado.</font><br>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\">");
		sb.append("</center>");
		sb.append("</body></html>");

		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setHtml(sb.toString());
		player.sendPacket(html);
		player.sendMessage("[Battle Royale] Tienes " + C_READY_TIMEOUT + "s para confirmar. Buffeate ya antes de entrar!");
	}

	public void handleReadyConfirm(Player player)
	{
		if (_state != EventState.WAITING) return;

		final int pid = player.getObjectId();
		final ScheduledFuture<?> t = READY_TIMERS.remove(pid);
		if (t != null) t.cancel(false);

		READY_CONFIRMED.add(pid);
		player.sendMessage("[Battle Royale] Confirmado! Seras teletransportado a la arena en breve.");

		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setHtml("<html><body><center><br><br><font color=\"00FF00\">Confirmado! Preparate para la arena...</font></center></body></html>");
		player.sendPacket(html);

		checkAllReady();
	}

	private void handleReadyTimeout(Player player)
	{
		if (_state != EventState.WAITING) return;

		final int pid = player.getObjectId();
		READY_TIMERS.remove(pid);
		if (READY_CONFIRMED.contains(pid)) return;

		player.setRegisteredOnEvent(false);
		removePlayerListeners(player);
		removeDualbox(player);

		player.sendMessage("[Battle Royale] No confirmaste a tiempo. Descalificado.");
		player.teleToLocation(C_RETURN_X, C_RETURN_Y, C_RETURN_Z, 0);

		checkAllReady();
	}

	private void checkAllReady()
	{
		if (!READY_TIMERS.isEmpty()) return;

		if (READY_CONFIRMED.size() < 2)
		{
			Broadcast.toAllOnlinePlayers("Battle Royale: cancelado — jugadores insuficientes confirmaron (" + READY_CONFIRMED.size() + "/2 minimo).");
			// Limpiar estado de los jugadores que habian confirmado
			for (Player online : World.getInstance().getPlayers())
			{
				if (online != null && READY_CONFIRMED.contains(online.getObjectId()))
				{
					online.setRegisteredOnEvent(false);
					removePlayerListeners(online);
				}
			}
			READY_CONFIRMED.clear();
			resetState();
			return;
		}

		// Todos confirmaron — 60s de preparacion exterior antes del traslado
		Broadcast.toAllOnlinePlayers("Battle Royale: " + READY_CONFIRMED.size() + " jugadores listos! Traslado a la arena en 60 segundos!");
		for (Player online : World.getInstance().getPlayers())
		{
			if (online != null && READY_CONFIRMED.contains(online.getObjectId()))
			{
				online.sendMessage("[Battle Royale] Confirmado! Seras trasladado a la arena en 60 segundos.");
				online.sendMessage("[Battle Royale] IMPORTANTE: Buffeate AHORA, no hay buffer en la arena!");
				online.sendPacket(new ExShowScreenMessage("Traslado en 60 segundos — Buffeate ahora!", ExShowScreenMessage.TOP_CENTER, 5000, 0, true, false));
			}
		}
		startQuestTimer("RegToArena30", 30_000L, null, null);
		startQuestTimer("RegToArena10", 50_000L, null, null);
		startQuestTimer("TeleportArena", 60_000L, null, null);
	}

	// =========================================================================
	// Fase de espera: teleport + countdown
	// =========================================================================
	private void startWaitingPhase()
	{
		_currentRadius = C_INITIAL_RADIUS;
		_zoneDamageActive = false;

		// Crear instance zone — aísla la arena del mundo principal
		destroyArenaInstance(); // limpiar instancia previa si existe
		_arenaWorld = new BattleRoyaleWorld();
		final Instance arenaInst = InstanceManager.getInstance().createDynamicInstance(ARENA_TEMPLATE_ID);
		_arenaWorld.setInstance(arenaInst);
		InstanceManager.getInstance().addWorld(_arenaWorld);
		LOGGER.info("BattleRoyale: arena instance creada (dynamicId=" + _arenaWorld.getInstanceId() + ")");

		Broadcast.toAllOnlinePlayers("Battle Royale: teletransportando a " + READY_CONFIRMED.size() + " jugadores a la arena!");
		Broadcast.toAllOnlinePlayers("Battle Royale: el combate comenzara en " + C_PREFIGHT_SECS + " segundos!");

		final List<Player> toTeleport = new ArrayList<>();
		for (Player online : World.getInstance().getPlayers())
		{
			if (online != null && READY_CONFIRMED.contains(online.getObjectId()))
			{
				toTeleport.add(online);
			}
		}

		if (toTeleport.size() < 2)
		{
			Broadcast.toAllOnlinePlayers("Battle Royale: cancelado — jugadores insuficientes en linea.");
			READY_CONFIRMED.clear();
			resetState();
			return;
		}

		for (Player p : toTeleport)
		{
			enterArena(p);
		}

		READY_CONFIRMED.clear();

		// Countdown timers
		final long base = Math.max(0, (C_PREFIGHT_SECS - 60)) * 1000L;
		if (C_PREFIGHT_SECS >= 60) startQuestTimer("PFCount60", base,            null, null);
		if (C_PREFIGHT_SECS >= 30) startQuestTimer("PFCount30", base + 30_000L,  null, null);
		if (C_PREFIGHT_SECS >= 10) startQuestTimer("PFCount10", base + 50_000L,  null, null);
		startQuestTimer("PFCount5",  base + 55_000L, null, null);
		startQuestTimer("PFCount4",  base + 56_000L, null, null);
		startQuestTimer("PFCount3",  base + 57_000L, null, null);
		startQuestTimer("PFCount2",  base + 58_000L, null, null);
		startQuestTimer("PFCount1",  base + 59_000L, null, null);
		startQuestTimer("StartFight", (long) C_PREFIGHT_SECS * 1000L, null, null);
	}

	// =========================================================================
	// Ingresar jugador a la arena
	// =========================================================================
	private void enterArena(Player player)
	{
		final String realName = player.getName();
		ORIGINAL_NAMES.put(player.getObjectId(), realName);
		player.getVariables().set("BR_REAL_NAME", realName);

		final String className = ClassListData.getInstance().getClass(player.getActiveClass()).getClassName();
		player.setName(className);

		final String originalTitle = player.getTitle();
		ORIGINAL_TITLES.put(player.getObjectId(), originalTitle);
		player.getVariables().set("BR_REAL_TITLE", originalTitle);
		if (!C_TITLE.isEmpty())
		{
			player.setTitle(C_TITLE);
		}

		if (C_HIDE_CLAN && player.getClan() != null)
		{
			CLAN_BACKUPS.put(player.getObjectId(), new ClanBackup(player));
			player.setClan(null);
		}

		player.broadcastUserInfo();
		player.storeMe();
		player.getVariables().storeMe(); // Persistir BR_REAL_NAME/BR_REAL_TITLE en DB inmediatamente (crash safety)

		// Invulnerable durante el pre-fight countdown — PvP flag se activa al inicio del combate
		player.setInvul(true);

		player.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true);
		player.setInsideZone(ZoneId.PVP, true); // Permite atacarse sin CTRL dentro del evento

		blockChat(player);

		// Quitar listener de registro antes de agregar los de la arena
		removePlayerListeners(player);

		// Registrar listeners del jugador
		player.addListener(new ConsumerEventListener(player, EventType.ON_CREATURE_DEATH,
			(OnCreatureDeath ev) -> handlePlayerDeath(ev), this));
		player.addListener(new ConsumerEventListener(player, EventType.ON_CREATURE_TELEPORTED,
			(OnCreatureTeleported ev) -> handlePlayerTeleport(ev), this));
		player.addListener(new ConsumerEventListener(player, EventType.ON_PLAYER_LOGOUT,
			(OnPlayerLogout ev) -> handlePlayerLogout(ev), this));

		PARTICIPANTS.add(player);
		ALIVE.add(player);
		player.setOnEvent(true);
		player.setOnSoloEvent(true); // FFA: todos atacables entre si sin tecla CTRL (isAutoAttackable)
		player.setRegisteredOnEvent(false);

		LAST_ACTIVITY.put(player.getObjectId(), System.currentTimeMillis());
		LAST_POSITION.put(player.getObjectId(), player.getLocation());

		// Entrar a la instance zone de la arena
		if (_arenaWorld != null)
		{
			_arenaWorld.addAllowed(player);
			player.setInstanceId(_arenaWorld.getInstanceId());
		}

		INTERNAL_TELEPORT.add(player.getObjectId());
		final Location spawn = getRandomSpawn();
		player.teleToLocation(spawn.getX() + Rnd.get(-100, 100), spawn.getY() + Rnd.get(-100, 100), spawn.getZ(), 0);

		player.sendPacket(new ExSendUIEvent(player, false, false, C_PREFIGHT_SECS, 0, NpcStringId.TIME_REMAINING));
		player.sendPacket(new ExShowScreenMessage("BATTLE ROYALE — Combate en " + C_PREFIGHT_SECS + "s!", 5000));
		player.sendMessage("[Battle Royale] Estas en la arena. Invulnerable por " + C_PREFIGHT_SECS + "s.");
		player.sendMessage("[Battle Royale] Solo puedes usar: .apon .apoff .potionon .potionoff");
	}

	private Location getRandomSpawn()
	{
		return C_SPAWNS.isEmpty()
			? new Location(C_CENTER_X, C_CENTER_Y, C_CENTER_Z)
			: C_SPAWNS.get(Rnd.get(C_SPAWNS.size()));
	}

	// =========================================================================
	// Inicio del combate
	// =========================================================================
	private void startFight()
	{
		if (ALIVE.size() < 2)
		{
			endEvent();
			return;
		}

		setState(EventState.ACTIVE);

		for (Player p : PARTICIPANTS)
		{
			if (p == null || !p.isOnline()) continue;
			p.setInvul(false);
			// Activar PvP flag ahora que comienza el combate — permite atacarse sin CTRL
			p.setPvpFlagLasts(System.currentTimeMillis() + 86400000L);
			p.updatePvPFlag(1); // Forzar nombre amarillo y broadcast inmediato a todos los clientes
			p.startPvPFlag();
			p.sendPacket(new ExSendUIEvent(p, false, false, C_DURATION * 60, 0, NpcStringId.TIME_REMAINING));
			p.sendPacket(new ExShowScreenMessage("BATTLE ROYALE — COMIENZA!", 5000));
			p.sendMessage("[Battle Royale] Combate iniciado! Ultimo en pie gana.");
		}

		Broadcast.toAllOnlinePlayers("Battle Royale: el combate ha comenzado! " + ALIVE.size() + " participantes.");

		startQuestTimer("EventEnd", (long) C_DURATION * 60_000L, null, null);
		startQuestTimer("ZonePhase", (long) C_ZONE_START_MINUTE * 60_000L, null, null);

		// Aviso de zona: al inicio del combate avisar cuándo activa la zona
		for (Player p : PARTICIPANTS)
		{
			if (p == null || !p.isOnline()) continue;
			p.sendMessage("[Battle Royale] En " + C_ZONE_START_MINUTE + " minuto(s) la zona comenzara a hacer dano. Acercate al centro!");
			p.sendPacket(new ExShowScreenMessage("Zona activa en " + C_ZONE_START_MINUTE + " min — Acercate al centro!", ExShowScreenMessage.TOP_CENTER, 6000, 0, true, false));
		}
		// Segundo aviso 1 minuto antes de que active la zona
		if (C_ZONE_START_MINUTE > 1)
		{
			startQuestTimer("ZoneWarning", (long)(C_ZONE_START_MINUTE - 1) * 60_000L, null, null);
		}

		_inactivityTask = ThreadPool.scheduleAtFixedRate(this::checkAllInactivity, 30_000L, 30_000L);
		_zoneTask = ThreadPool.scheduleAtFixedRate(this::applyZoneDamage, 1000L, 1000L);
	}

	// =========================================================================
	// Zona: reduccion y daño
	// =========================================================================
	private void shrinkZone()
	{
		final int newRadius = Math.max(C_MIN_RADIUS, _currentRadius - C_SHRINK_AMOUNT);
		_currentRadius = newRadius;

		Broadcast.toAllOnlinePlayers("Battle Royale: LA ZONA SE CERRO! Radio actual: " + _currentRadius + " unidades.");

		for (Player p : PARTICIPANTS)
		{
			if (p == null || !p.isOnline()) continue;
			p.sendPacket(new ExShowScreenMessage("ZONA REDUCIDA — Radio: " + _currentRadius, ExShowScreenMessage.TOP_CENTER, 5000, 0, true, false));
			if (C_PULSE_SKILL_ID > 0)
			{
				final Skill pulse = SkillData.getInstance().getSkill(C_PULSE_SKILL_ID, 1);
				if (pulse != null)
				{
					p.broadcastPacket(new MagicSkillUse(p, p, C_PULSE_SKILL_ID, 1, 0, 0));
				}
			}

			// Activar flecha de radar para jugadores que quedan fuera de la nueva zona
			final double distToCenter = Math.sqrt(
				Math.pow(p.getX() - C_CENTER_X, 2) +
				Math.pow(p.getY() - C_CENTER_Y, 2));
			if (distToCenter > _currentRadius)
			{
				p.sendMessage("[Battle Royale] Estas fuera de la zona segura! Muevete al centro — ubicacion marcada en el mapa.");
				if (RADAR_ACTIVE.add(p.getObjectId()))
				{
					p.sendPacket(new RadarControl(0, 2, C_CENTER_X, C_CENTER_Y, C_CENTER_Z));
				}
			}
		}
	}

	private void scheduleNextZonePhase()
	{
		if (_currentRadius > C_MIN_RADIUS && _state == EventState.ACTIVE)
		{
			startQuestTimer("ZonePhase", (long) C_SHRINK_INTERVAL * 60_000L, null, null);
		}
	}

	private void applyZoneDamage()
	{
		if (!_zoneDamageActive || _state != EventState.ACTIVE) return;

		for (Player p : ALIVE)
		{
			if (p == null || !p.isOnline() || p.isDead()) continue;

			final double dist = Math.sqrt(
				Math.pow(p.getX() - C_CENTER_X, 2) +
				Math.pow(p.getY() - C_CENTER_Y, 2));

			if (dist > _currentRadius)
			{
				// Fuera de la zona segura — activar flecha de radar si no estaba
				if (RADAR_ACTIVE.add(p.getObjectId()))
				{
					p.sendPacket(new RadarControl(0, 2, C_CENTER_X, C_CENTER_Y, C_CENTER_Z));
				}
				p.reduceCurrentHp(C_ZONE_DAMAGE, p, null);
				p.sendPacket(new ExShowScreenMessage("ZONA PELIGROSA! -" + C_ZONE_DAMAGE + " HP", ExShowScreenMessage.TOP_CENTER, 1500, 0, true, false));

				if (C_PULSE_SKILL_ID > 0)
				{
					p.broadcastPacket(new MagicSkillUse(p, p, C_PULSE_SKILL_ID, 1, 0, 0));
				}

				// Estar fuera de zona es "actividad" (no inactivo)
				LAST_ACTIVITY.put(p.getObjectId(), System.currentTimeMillis());
			}
			else
			{
				// Dentro de la zona segura — desactivar flecha de radar si estaba activa
				if (RADAR_ACTIVE.remove(p.getObjectId()))
				{
					p.sendPacket(new RadarControl(1, 2, C_CENTER_X, C_CENTER_Y, C_CENTER_Z));
					p.sendMessage("[Battle Royale] Volviste a la zona segura!");
				}
			}
		}
	}

	// =========================================================================
	// Cajas
	// =========================================================================
	private void spawnBoxes()
	{
		final int count = ALIVE.size();
		if (count <= 0 || _state != EventState.ACTIVE) return;

		// Radio seguro efectivo: los NPCs-caja SIEMPRE deben aparecer dentro de la zona segura,
		// nunca en el radio donde la zona hace dano (dist > _currentRadius).
		// Dejamos un margen de 200 unidades respecto al borde para que no queden justo en el limite.
		final int safeRadius = Math.max(100, _currentRadius - 200);

		// Los NPCs-caja deben aparecer DENTRO de la instancia del evento, no en el overworld,
		// de lo contrario los jugadores (que estan en la instancia) no los ven.
		final int instanceId = (_arenaWorld != null) ? _arenaWorld.getInstanceId() : 0;

		int spawned = 0;
		synchronized (SPAWNED_BOXES)
		{
			for (int i = 0; i < count; i++)
			{
				final double angle = Math.random() * 2 * Math.PI;
				final double dist = Math.random() * safeRadius;
				final int bx = (int) (C_CENTER_X + dist * Math.cos(angle));
				final int by = (int) (C_CENTER_Y + dist * Math.sin(angle));
				final Npc box = addSpawn(C_BOX_NPC_ID, bx, by, C_CENTER_Z, Rnd.get(65535), false, 0L, false, instanceId);
				if (box != null)
				{
					SPAWNED_BOXES.add(box);
					spawned++;
				}
			}
		}

		if (spawned > 0)
		{
			broadcastMsgParticipants("[Battle Royale] Han aparecido cajas para los sobrevivientes, buscalas!");
			broadcastScreenAll("Han aparecido cajas para los sobrevivientes, buscalas!", 5000);
		}
	}

	private void despawnBoxes()
	{
		synchronized (SPAWNED_BOXES)
		{
			for (Npc box : SPAWNED_BOXES)
			{
				if (box != null && box.isSpawned())
				{
					box.deleteMe();
				}
			}
			SPAWNED_BOXES.clear();
		}
	}

	// =========================================================================
	// Muerte de jugador (listener)
	// =========================================================================
	private void handlePlayerDeath(OnCreatureDeath event)
	{
		if (!(event.getTarget() instanceof Player)) return;

		final Player killed = (Player) event.getTarget();
		if (!ALIVE.contains(killed)) return;

		ALIVE.remove(killed);

		// Usar nombre real del personaje (el nombre en juego es la clase durante el evento)
		final String killedRealName = ORIGINAL_NAMES.getOrDefault(killed.getObjectId(), killed.getName());

		killed.setPvpFlagLasts(0);
		killed.updatePvPFlag(0); // Quitar nombre amarillo inmediatamente
		killed.stopPvPFlag();
		killed.setInvul(false);

		// Actualizar actividad del killer y refrescar PvP flag
		if (event.getAttacker() instanceof Player)
		{
			final Player killer = (Player) event.getAttacker();
			if (PARTICIPANTS.contains(killer))
			{
				// Refrescar PvP flag para que el killer siga atacando sin CTRL
				killer.setPvpFlagLasts(System.currentTimeMillis() + 86400000L);
				killer.updatePvPFlag(1); // Mantener nombre amarillo activo
				LAST_ACTIVITY.put(killer.getObjectId(), System.currentTimeMillis());
				LAST_POSITION.put(killer.getObjectId(), killer.getLocation());
				killer.sendPacket(new ExShowScreenMessage("Eliminaste a " + killedRealName + "! Vivos: " + ALIVE.size(), 3000));
			}
		}

		Broadcast.toAllOnlinePlayers("Battle Royale: " + killedRealName + " eliminado! Quedan " + ALIVE.size() + " jugadores.");

		// Enviar a Giran 3s despues de morir
		startQuestTimer("Death_" + killed.getObjectId(), 3000, null, killed);

		// Si queda 1 (o 0) sobreviviente, termina el evento.
		// Hay que cancelar el timer "EventEnd" de los 30 min creado en startFight(), porque
		// startQuestTimer ignora un nuevo timer si ya existe uno con el mismo nombre.
		if (ALIVE.size() <= 1)
		{
			cancelQuestTimer("EventEnd", null, null);
			startQuestTimer("EventEnd", 3000, null, null);
		}
	}

	// =========================================================================
	// Teleport externo (SOE, /unstuck) → descalificar
	// =========================================================================
	private void handlePlayerTeleport(OnCreatureTeleported ev)
	{
		if (!(ev.getCreature() instanceof Player)) return;
		final Player player = (Player) ev.getCreature();
		if (INTERNAL_TELEPORT.remove(player.getObjectId())) return;
		if (PARTICIPANTS.contains(player))
		{
			player.sendMessage("[Battle Royale] Usaste un teleport externo — descalificado.");
			disqualify(player, false);
		}
	}

	// =========================================================================
	// Logout → descalificar
	// =========================================================================
	private void handlePlayerLogout(OnPlayerLogout ev)
	{
		final Player player = ev.getPlayer();
		if (PARTICIPANTS.contains(player))
		{
			player.getVariables().set("BR_RECOVER", "1");
			player.getVariables().storeMe();
			disqualify(player, false);
		}
		else if (REGISTERED.contains(player))
		{
			REGISTERED.remove(player);
			player.setRegisteredOnEvent(false);
			removeDualbox(player);
		}
	}

	// =========================================================================
	// Login → recuperacion y hero temporal
	// =========================================================================
	private void onPlayerLogin(OnPlayerLogin ev)
	{
		final Player player = ev.getPlayer();

		// Recuperar nombre/titulo/chatban si el servidor crasheo con el jugador dentro del evento
		final String realName = player.getVariables().getString("BR_REAL_NAME", "");
		if (!realName.isEmpty())
		{
			player.setName(realName);
			// Restaurar titulo original
			final String realTitle = player.getVariables().getString("BR_REAL_TITLE", "");
			if (!realTitle.isEmpty())
			{
				player.setTitle(realTitle);
			}
			// Quitar el chat ban puesto por el evento
			PunishmentManager.getInstance().stopPunishment(
				player.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN);
			player.getVariables().remove("BR_REAL_NAME");
			player.getVariables().remove("BR_REAL_TITLE");
			player.getVariables().storeMe();
			player.broadcastUserInfo();
		}

		// Enviar a Giran si estaba en el evento al desconectarse
		if (!player.getVariables().getString("BR_RECOVER", "").isEmpty())
		{
			player.getVariables().remove("BR_RECOVER");
			player.getVariables().storeMe();
			ThreadPool.schedule(() ->
			{
				if (player.isOnline())
				{
					player.teleToLocation(C_RETURN_X, C_RETURN_Y, C_RETURN_Z, 0);
					player.sendMessage("[Battle Royale] Fuiste descalificado por desconexion. Enviado a Giran.");
				}
			}, 3000L);
		}

		// Hero temporal
		final long heroEnd = player.getVariables().getLong("BR_HERO_END", 0L);
		if (heroEnd > 0)
		{
			if (System.currentTimeMillis() < heroEnd)
			{
				if (!player.isHero())
				{
					player.setHero(true);
					player.broadcastUserInfo();
				}
			}
			else
			{
				final boolean wasOlympiadHero = player.getVariables().getBoolean("BR_WAS_OLYMPIAD_HERO", false);
				if (!wasOlympiadHero && player.isHero())
				{
					player.setHero(false);
					player.broadcastUserInfo();
				}
				player.getVariables().remove("BR_HERO_END");
				player.getVariables().remove("BR_WAS_OLYMPIAD_HERO");
				player.getVariables().storeMe();
			}
		}
	}

	// =========================================================================
	// Inactividad
	// =========================================================================
	private void checkAllInactivity()
	{
		if (_state != EventState.ACTIVE) return;

		final long now = System.currentTimeMillis();
		for (Player p : new ArrayList<>(ALIVE))
		{
			if (p == null || !p.isOnline()) continue;

			final Location lastPos = LAST_POSITION.get(p.getObjectId());
			if (lastPos == null || p.calculateDistance2D(lastPos.getX(), lastPos.getY(), lastPos.getZ()) > 100)
			{
				LAST_ACTIVITY.put(p.getObjectId(), now);
				LAST_POSITION.put(p.getObjectId(), p.getLocation());
				continue;
			}

			final long lastActive = LAST_ACTIVITY.getOrDefault(p.getObjectId(), now);
			if (now - lastActive > (long) C_INACTIVITY_TIMEOUT * 1000L)
			{
				p.sendMessage("[Battle Royale] Descalificado por inactividad.");
				disqualify(p, true);
			}
		}
	}

	// =========================================================================
	// Descalificacion
	// =========================================================================
	public void disqualify(Player player, boolean teleport)
	{
		ALIVE.remove(player);
		if (!PARTICIPANTS.remove(player)) return;

		// Quitar flecha de radar si estaba activa
		if (RADAR_ACTIVE.remove(player.getObjectId()))
		{
			player.sendPacket(new RadarControl(1, 2, C_CENTER_X, C_CENTER_Y, C_CENTER_Z));
		}

		player.setInvul(false);
		player.setPvpFlagLasts(0);
		player.updatePvPFlag(0); // Quitar nombre amarillo
		player.stopPvPFlag();

		// Restaurar nombre
		final String origName = ORIGINAL_NAMES.remove(player.getObjectId());
		if (origName != null)
		{
			player.setName(origName);
		}
		else
		{
			final String varName = player.getVariables().getString("BR_REAL_NAME", "");
			if (!varName.isEmpty()) player.setName(varName);
		}
		player.getVariables().remove("BR_REAL_NAME");

		// Restaurar titulo
		final String origTitle = ORIGINAL_TITLES.remove(player.getObjectId());
		if (origTitle != null) player.setTitle(origTitle);
		player.getVariables().remove("BR_REAL_TITLE");

		// Restaurar clan
		final ClanBackup backup = CLAN_BACKUPS.remove(player.getObjectId());
		if (backup != null) backup.restore(player);

		player.broadcastUserInfo();

		unblockChat(player);

		player.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
		player.setInsideZone(ZoneId.PVP, false);
		player.setOnEvent(false);
		player.setOnSoloEvent(false);
		player.setRegisteredOnEvent(false);

		LAST_ACTIVITY.remove(player.getObjectId());
		LAST_POSITION.remove(player.getObjectId());
		INTERNAL_TELEPORT.remove(player.getObjectId());

		// Destruir cajas BR que el jugador tenga en inventario
		final Item brBox = player.getInventory().getItemByItemId(C_BOX_ITEM_ID);
		if (brBox != null)
		{
			player.destroyItem(ItemProcessType.DESTROY, brBox, brBox.getCount(), null, false);
		}

		removePlayerListeners(player);

		player.getVariables().storeMe();
		try { player.storeMe(); } catch (Exception ignored) {}

		// Limpiar timer UI
		player.sendPacket(new ExSendUIEvent(player, true, true, 0, 0, ""));

		// Salir de la instance zone (debe hacerse antes del teleport)
		player.setInstanceId(0);

		if (teleport)
		{
			// Revivir antes del teleport para que el personaje aparezca vivo en Giran
			if (player.isDead())
			{
				player.doRevive();
			}
			INTERNAL_TELEPORT.add(player.getObjectId());
			player.teleToLocation(C_RETURN_X, C_RETURN_Y, C_RETURN_Z, 0);
			player.sendMessage("[Battle Royale] Eliminado. Enviado a Giran.");
			player.sendPacket(new ExShowScreenMessage("Eliminado del Battle Royale.", 5000));
		}
	}

	// =========================================================================
	// Fin del evento
	// =========================================================================
	private void endEvent()
	{
		if (_state == EventState.INACTIVE) return;

		stopZoneTasks();
		cancelAllTimers();

		final List<Player> survivors = new ArrayList<>(ALIVE);

		if (survivors.size() == 1)
		{
			final Player winner = survivors.get(0);
			announceWinner(winner);
			giveWinnerRewards(winner);
		}
		else if (survivors.isEmpty())
		{
			Broadcast.toAllOnlinePlayers("Battle Royale: no hay ganador — todos fueron eliminados.");
		}
		else
		{
			// Tiempo agotado — ganador aleatorio entre sobrevivientes
			final Player winner = survivors.get(Rnd.get(survivors.size()));
			final String wName = ORIGINAL_NAMES.getOrDefault(winner.getObjectId(), winner.getName());
			Broadcast.toAllOnlinePlayers("Battle Royale: tiempo agotado! Ganador aleatorio: " + wName + "!");
			giveWinnerRewards(winner);
		}

		for (Player p : new ArrayList<>(PARTICIPANTS))
		{
			disqualify(p, true);
		}

		// Destruir la instancia despues de sacar a todos los jugadores
		destroyArenaInstance();

		despawnBoxes();
		resetState();
	}

	private void announceWinner(Player winner)
	{
		final String realName = ORIGINAL_NAMES.getOrDefault(winner.getObjectId(), winner.getName());
		Broadcast.toAllOnlinePlayersOnScreen("BATTLE ROYALE — GANADOR: " + realName + "!");
		Broadcast.toAllOnlinePlayers("Battle Royale: GANADOR: " + realName + "! Felicitaciones!");
	}

	private void giveWinnerRewards(Player winner)
	{
		for (ItemHolder reward : C_WIN_REWARDS)
		{
			winner.addItem(ItemProcessType.REWARD, (int) reward.getId(), reward.getCount(), winner, true);
		}

		if (C_WINNER_HERO)
		{
			if (!winner.isHero())
			{
				winner.getVariables().set("BR_WAS_OLYMPIAD_HERO", false);
				winner.setHero(true);
				winner.broadcastUserInfo();
				final long end = System.currentTimeMillis() + (long) C_HERO_DAYS * 24 * 60 * 60 * 1000L;
				winner.getVariables().set("BR_HERO_END", end);
				winner.getVariables().storeMe();
				winner.sendPacket(new ExShowScreenMessage("Ganaste Battle Royale! Hero por " + C_HERO_DAYS + " dias!", 10000));
				Broadcast.toAllOnlinePlayers("Battle Royale: " + winner.getName() + " recibio HERO por " + C_HERO_DAYS + " dias!");
			}
			else
			{
				// Ya es heroe (olympiad) — no tocar el estado, solo dar recompensas
				winner.sendPacket(new ExShowScreenMessage("Ganaste Battle Royale! Recompensas otorgadas.", 7000));
			}
		}
		else
		{
			winner.sendPacket(new ExShowScreenMessage("Ganaste Battle Royale! Recompensas otorgadas.", 7000));
		}
	}

	// =========================================================================
	// Chat ban/unban
	// =========================================================================
	private static void blockChat(Player player)
	{
		if (!player.isChatBanned())
		{
			PunishmentManager.getInstance().startPunishment(new PunishmentTask(
				player.getObjectId(),
				PunishmentAffect.CHARACTER,
				PunishmentType.CHAT_BAN,
				0,
				"Battle Royale chat block",
				"BattleRoyale"));
			CHAT_BANNED.add(player.getObjectId());
		}
	}

	private static void unblockChat(Player player)
	{
		if (CHAT_BANNED.remove(player.getObjectId()))
		{
			PunishmentManager.getInstance().stopPunishment(
				player.getObjectId(),
				PunishmentAffect.CHARACTER,
				PunishmentType.CHAT_BAN);
		}
	}

	// =========================================================================
	// Helpers
	// =========================================================================
	/**
	 * Destruye la instance zone de la arena de forma inmediata.
	 * Usa InstanceManager.destroyInstance() en lugar de setDuration() para evitar que se
	 * programe la task CheckTimeUp (que envia el mensaje "el dungeon expira en X minutos").
	 * Debe llamarse DESPUES de teletransportar a todos los jugadores fuera de la instancia.
	 */
	private void destroyArenaInstance()
	{
		if (_arenaWorld == null) return;
		try
		{
			InstanceManager.getInstance().destroyInstance(_arenaWorld.getInstanceId());
		}
		catch (Exception e)
		{
			LOGGER.warning("BattleRoyale: error al destruir la arena instance: " + e.getMessage());
		}
		_arenaWorld = null;
	}

	private static void setState(EventState s)
	{
		_state = s;
	}

	private void resetState()
	{
		// Seguridad: quitar chat ban a cualquier jugador que aun lo tenga activo por este evento
		for (int pid : new ArrayList<>(CHAT_BANNED))
		{
			PunishmentManager.getInstance().stopPunishment(pid, PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN);
		}
		CHAT_BANNED.clear();

		// Seguridad: restaurar titulo a cualquier jugador online que aun lo tenga modificado
		for (Map.Entry<Integer, String> entry : ORIGINAL_TITLES.entrySet())
		{
			final int pid = entry.getKey();
			for (Player online : World.getInstance().getPlayers())
			{
				if (online != null && online.getObjectId() == pid)
				{
					online.setTitle(entry.getValue());
					online.broadcastUserInfo();
					break;
				}
			}
		}
		ORIGINAL_TITLES.clear();

		REGISTERED.clear();
		PARTICIPANTS.clear();
		ALIVE.clear();
		READY_CONFIRMED.clear();
		CLAN_BACKUPS.clear();
		ORIGINAL_NAMES.clear();
		INTERNAL_TELEPORT.clear();
		LAST_ACTIVITY.clear();
		LAST_POSITION.clear();
		RADAR_ACTIVE.clear();
		_currentRadius = C_INITIAL_RADIUS;
		_zoneDamageActive = false;
		_gmForced = false;
		cancelAllTimers();
		setState(EventState.INACTIVE);
	}

	private void stopZoneTasks()
	{
		if (_zoneTask != null) { _zoneTask.cancel(false); _zoneTask = null; }
		if (_inactivityTask != null) { _inactivityTask.cancel(false); _inactivityTask = null; }
	}

	/**
	 * Cancela todos los quest timers activos del evento.
	 * Reemplaza cancelTimers() que no existe en esta version del framework.
	 */
	private void cancelAllTimers()
	{
		// Timers de registro
		cancelQuestTimer("RegWarning1", null, null);
		cancelQuestTimer("RegistrationEnd", null, null);
		// Timers pre-teleport
		cancelQuestTimer("RegToArena30", null, null);
		cancelQuestTimer("RegToArena10", null, null);
		cancelQuestTimer("TeleportArena", null, null);
		// Timers pre-fight
		cancelQuestTimer("PFCount60", null, null);
		cancelQuestTimer("PFCount30", null, null);
		cancelQuestTimer("PFCount10", null, null);
		cancelQuestTimer("PFCount5", null, null);
		cancelQuestTimer("PFCount4", null, null);
		cancelQuestTimer("PFCount3", null, null);
		cancelQuestTimer("PFCount2", null, null);
		cancelQuestTimer("PFCount1", null, null);
		cancelQuestTimer("StartFight", null, null);
		cancelQuestTimer("EventEnd", null, null);
		cancelQuestTimer("ZonePhase", null, null);
		cancelQuestTimer("ZoneWarning", null, null);
		// Timers por jugador
		for (Player p : PARTICIPANTS)
		{
			if (p != null)
			{
				cancelQuestTimer("ReadyTimeout_" + p.getObjectId(), null, p);
				cancelQuestTimer("Death_" + p.getObjectId(), null, p);
			}
		}
		for (Player p : REGISTERED)
		{
			if (p != null)
			{
				cancelQuestTimer("ReadyTimeout_" + p.getObjectId(), null, p);
			}
		}
	}

	private void broadcastMsgParticipants(String msg)
	{
		for (Player p : PARTICIPANTS)
		{
			if (p != null && p.isOnline()) p.sendMessage(msg);
		}
	}

	private void broadcastScreenAll(String msg, int ms)
	{
		for (Player p : PARTICIPANTS)
		{
			if (p != null && p.isOnline()) p.sendPacket(new ExShowScreenMessage(msg, ms));
		}
	}

	private static void removeDualbox(Player player)
	{
		if (!C_DUALBOX && DualboxCheckConfig.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP > 0)
		{
			AntiFeedManager.getInstance().removePlayer(AntiFeedManager.L2EVENT_ID, player);
		}
	}

	/** Agrega listener de logout de registro (solo para fase REGISTRATION) */
	private static void addLogoutListener(Player player)
	{
		player.addListener(new ConsumerEventListener(player, EventType.ON_PLAYER_LOGOUT,
			(OnPlayerLogout ev) -> {
				if (REGISTERED.remove(ev.getPlayer()))
				{
					ev.getPlayer().setRegisteredOnEvent(false);
					removeDualbox(ev.getPlayer());
				}
			}, BattleRoyale.getInstance()));
	}

	/** Remueve todos los listeners de este evento del jugador */
	private static void removePlayerListeners(Player player)
	{
		final BattleRoyale inst = SingletonHolder.INSTANCE;
		for (AbstractEventListener l : player.getListeners(EventType.ON_CREATURE_DEATH))
		{
			if (l.getOwner() == inst) l.unregisterMe();
		}
		for (AbstractEventListener l : player.getListeners(EventType.ON_PLAYER_LOGOUT))
		{
			if (l.getOwner() == inst) l.unregisterMe();
		}
		for (AbstractEventListener l : player.getListeners(EventType.ON_CREATURE_TELEPORTED))
		{
			if (l.getOwner() == inst) l.unregisterMe();
		}
	}

	// =========================================================================
	// Recuperacion post-restart
	// =========================================================================
	private void recoverPlayers()
	{
		// Jugadores online con nombre de clase → restaurar
		for (Player p : World.getInstance().getPlayers())
		{
			if (p == null) continue;
			final String realName = p.getVariables().getString("BR_REAL_NAME", "");
			if (!realName.isEmpty())
			{
				p.setName(realName);
				final String realTitle = p.getVariables().getString("BR_REAL_TITLE", "");
				if (!realTitle.isEmpty()) p.setTitle(realTitle);
				// Quitar el chat ban puesto por el evento
				PunishmentManager.getInstance().stopPunishment(
					p.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN);
				p.getVariables().remove("BR_REAL_NAME");
				p.getVariables().remove("BR_REAL_TITLE");
				p.getVariables().remove("BR_RECOVER");
				p.getVariables().storeMe();
				p.broadcastUserInfo();
				p.teleToLocation(C_RETURN_X, C_RETURN_Y, C_RETURN_Z, 0);
				LOGGER.info("BattleRoyale: recuperado online " + realName + " → Giran.");
			}
		}

		// Jugadores offline: restaurar nombre, titulo y coordenadas en DB
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"SELECT cv1.charId, cv1.val AS realName, COALESCE(cv2.val,'') AS realTitle " +
				"FROM character_variables cv1 " +
				"LEFT JOIN character_variables cv2 ON cv1.charId = cv2.charId AND cv2.var = 'BR_REAL_TITLE' " +
				"WHERE cv1.var = 'BR_REAL_NAME'"))
		{
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int charId    = rs.getInt("charId");
					final String realName  = rs.getString("realName");
					final String realTitle = rs.getString("realTitle");
					// Restaurar nombre, titulo y coordenadas en la tabla characters
					try (PreparedStatement upd = con.prepareStatement(
						"UPDATE characters SET char_name=?, title=?, x=?, y=?, z=? WHERE charId=?"))
					{
						upd.setString(1, realName);
						upd.setString(2, realTitle);
						upd.setInt(3, C_RETURN_X);
						upd.setInt(4, C_RETURN_Y);
						upd.setInt(5, C_RETURN_Z);
						upd.setInt(6, charId);
						upd.executeUpdate();
					}
					// Quitar el chat ban puesto por el evento
					PunishmentManager.getInstance().stopPunishment(
						charId, PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN);
					// Limpiar variables del evento
					try (PreparedStatement del = con.prepareStatement(
						"DELETE FROM character_variables WHERE charId=? AND var IN ('BR_REAL_NAME','BR_REAL_TITLE','BR_RECOVER')"))
					{
						del.setInt(1, charId);
						del.executeUpdate();
					}
					LOGGER.info("BattleRoyale: recuperado offline id=" + charId + " (" + realName + ").");
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "BattleRoyale: error en recuperacion.", e);
		}
	}

	// =========================================================================
	// Singleton + carga de script
	// =========================================================================
	public static BattleRoyale getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final BattleRoyale INSTANCE = new BattleRoyale();
	}

	public static void main(String[] args)
	{
		getInstance();
	}
}
