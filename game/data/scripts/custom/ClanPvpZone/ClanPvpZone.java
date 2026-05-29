/*
 * ClanPvpZone - Evento de clanes con RaidBoss final.
 *
 * Flujo:
 *   IDLE -> registro de clanes -> COUNTDOWN -> ACTIVE (combate clan vs clan)
 *   -> RAID (boss spawn para clan ganador) -> recompensa -> IDLE
 *
 * Config: game/config/Custom/ClanPvPZone.ini
 * Integracion: GlobalGatekeeper llama ClanPvpZone.getInstance() para mostrar estado.
 */
package custom.ClanPvpZone;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.zone.type.BossZone;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * Clan PvP Zone Event.
 * @author Custom - Aden Chronicles
 */
public class ClanPvpZone extends Script
{
	private static final Logger LOGGER = Logger.getLogger(ClanPvpZone.class.getName());

	// Singleton para que GlobalGatekeeper pueda consultar el estado.
	private static volatile ClanPvpZone _instance;

	public static ClanPvpZone getInstance()
	{
		return _instance;
	}

	// ---------------------------------------------------------------------------
	// Config
	// ---------------------------------------------------------------------------
	private static boolean ENABLED = true;
	private static int MIN_CLANS = 2;
	private static int MAX_CLANS = 4;
	private static int COUNTDOWN_MINUTES = 5;
	private static int REP_PER_KILL = 100;
	private static int RAID_BOSS_ID = 25286;
	private static int RAID_BOSS_X = 114714;
	private static int RAID_BOSS_Y = -117072;
	private static int RAID_BOSS_Z = -11080;
	private static int RAID_BOSS_HEADING = 0;
	private static int REWARD_ITEM_ID = 57;
	private static long REWARD_COUNT = 1_000_000L;
	private static int RETURN_X = 82698;
	private static int RETURN_Y = 148638;
	private static int RETURN_Z = -3473;
	private static int RAID_CURSE_SKILL_ID = 4215;
	private static final List<Location> CLAN_SPAWNS = new ArrayList<>();
	private static int[] _bossZoneIds = new int[0];

	// ---------------------------------------------------------------------------
	// Estado del evento
	// ---------------------------------------------------------------------------
	public enum EventState
	{
		IDLE,
		COUNTDOWN,
		ACTIVE,
		RAID
	}

	private volatile EventState _state = EventState.IDLE;

	/**
	 * Clanes registrados en orden de inscripcion: clanId -> clanName.
	 * LinkedHashMap para preservar el orden y asignar spawns consistentemente.
	 */
	private final Map<Integer, String> _registeredClans = new LinkedHashMap<>();

	/** ID del ultimo clan ganador (para el cooldown de re-inscripcion). */
	private volatile int _lastWinnerClanId = -1;

	/** Jugadores vivos por clan durante la fase ACTIVE: clanId -> Set<Player>. */
	private final Map<Integer, Set<Player>> _clanPlayers = new ConcurrentHashMap<>();

	/** Todos los participantes activos (ACTIVE + RAID). */
	private final Set<Player> _allParticipants = ConcurrentHashMap.newKeySet();

	/** Reputacion ganada en el evento por clan: clanId -> puntos. */
	private final Map<Integer, Integer> _eventReputation = new ConcurrentHashMap<>();

	// Raid
	private volatile Npc _raidBoss = null;
	private volatile int _raidWinnerClanId = -1;
	private final Set<Player> _raidParticipants = ConcurrentHashMap.newKeySet();
	private ScheduledFuture<?> _raidCurseTask = null;
	private final AtomicBoolean _raidBossDeathHandled = new AtomicBoolean(false);

	// Timers
	private ScheduledFuture<?> _countdownTask = null;
	private final AtomicInteger _countdownSeconds = new AtomicInteger(0);

	/** Flag para no reaccionar a nuestros propios teleports. */
	private final Set<Integer> _internalTeleport = ConcurrentHashMap.newKeySet();

	/** Flag para evitar iniciar la fase raid multiples veces. */
	private final AtomicBoolean _raidPhaseStarting = new AtomicBoolean(false);

	// ---------------------------------------------------------------------------
	// AntiFeed (leido desde PVP.ini, seccion AntiFeed - Zona Clan PvP)
	// Bloquea reputacion de clan si killer y killed pertenecen al mismo clan.
	// ---------------------------------------------------------------------------
	private static boolean ANTIFEED_CLAN_ENABLED = true;

	// ---------------------------------------------------------------------------
	// Constructor
	// ---------------------------------------------------------------------------
	private ClanPvpZone()
	{
		loadConfig();
		_instance = this;

		if (!ENABLED)
		{
			LOGGER.info("ClanPvpZone: Desactivado en config.");
			return;
		}

		BypassHandler.getInstance().registerHandler(new ClanPvpBypass());

		LOGGER.info("ClanPvpZone: Activo | MinClanes=" + MIN_CLANS
			+ " | Countdown=" + COUNTDOWN_MINUTES + "min"
			+ " | Rep/Kill=" + REP_PER_KILL
			+ " | Boss=" + RAID_BOSS_ID
			+ " | Spawns=" + CLAN_SPAWNS.size());
	}

	// ---------------------------------------------------------------------------
	// Carga de config
	// ---------------------------------------------------------------------------
	private void loadConfig()
	{
		final Properties props = new Properties();
		try (InputStream is = new FileInputStream("./config/Custom/ClanPvPZone.ini"))
		{
			props.load(is);
		}
		catch (Exception e)
		{
			LOGGER.warning("ClanPvpZone: No se pudo cargar ClanPvPZone.ini: " + e.getMessage() + " - Usando valores por defecto.");
		}

		ENABLED = Boolean.parseBoolean(props.getProperty("Enabled", "True").trim());
		MIN_CLANS = Integer.parseInt(props.getProperty("MinClansToStart", "2").trim());
		MAX_CLANS = Math.max(MIN_CLANS, Integer.parseInt(props.getProperty("MaxClansToStart", "4").trim()));
		COUNTDOWN_MINUTES = Integer.parseInt(props.getProperty("CountdownMinutes", "5").trim());
		REP_PER_KILL = Integer.parseInt(props.getProperty("ClanReputationPerKill", "100").trim());
		RAID_BOSS_ID = Integer.parseInt(props.getProperty("RaidBossId", "25286").trim());
		RAID_BOSS_X = Integer.parseInt(props.getProperty("RaidBossX", "114714").trim());
		RAID_BOSS_Y = Integer.parseInt(props.getProperty("RaidBossY", "-117072").trim());
		RAID_BOSS_Z = Integer.parseInt(props.getProperty("RaidBossZ", "-11080").trim());
		RAID_BOSS_HEADING = Integer.parseInt(props.getProperty("RaidBossHeading", "0").trim());
		REWARD_ITEM_ID = Integer.parseInt(props.getProperty("RewardItemId", "57").trim());
		REWARD_COUNT = Long.parseLong(props.getProperty("RewardCount", "1000000").trim());
		RETURN_X = Integer.parseInt(props.getProperty("ReturnX", "82698").trim());
		RETURN_Y = Integer.parseInt(props.getProperty("ReturnY", "148638").trim());
		RETURN_Z = Integer.parseInt(props.getProperty("ReturnZ", "-3473").trim());
		RAID_CURSE_SKILL_ID = Integer.parseInt(props.getProperty("RaidCurseSkillId", "4215").trim());

		// Spawns de clanes (hasta 8 slots)
		CLAN_SPAWNS.clear();
		for (int i = 1; i <= 8; i++)
		{
			final String val = props.getProperty("ClanSpawn" + i, "").trim();
			if (val.isEmpty())
			{
				continue;
			}
			final String[] parts = val.split(",");
			if (parts.length < 3)
			{
				LOGGER.warning("ClanPvpZone: Formato invalido en ClanSpawn" + i + " (se esperan X,Y,Z).");
				continue;
			}
			try
			{
				CLAN_SPAWNS.add(new Location(
					Integer.parseInt(parts[0].trim()),
					Integer.parseInt(parts[1].trim()),
					Integer.parseInt(parts[2].trim())));
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("ClanPvpZone: Coordenadas invalidas en ClanSpawn" + i + ".");
			}
		}

		// Fallback si no se configuraron spawns
		if (CLAN_SPAWNS.isEmpty())
		{
			CLAN_SPAWNS.add(new Location(116344, -114805, -10984));
			CLAN_SPAWNS.add(new Location(113104, -114815, -10984));
			CLAN_SPAWNS.add(new Location(114714, -117072, -11080));
			CLAN_SPAWNS.add(new Location(114707, -113195, -10984));
			LOGGER.warning("ClanPvpZone: No se configuraron ClanSpawn1-4. Usando coordenadas de Freya por defecto.");
		}

		// BossZone IDs (separados por coma)
		final String zoneIdStr = props.getProperty("BossZoneIds", "").trim();
		if (!zoneIdStr.isEmpty())
		{
			final String[] parts = zoneIdStr.split(",");
			_bossZoneIds = new int[parts.length];
			for (int i = 0; i < parts.length; i++)
			{
				try
				{
					_bossZoneIds[i] = Integer.parseInt(parts[i].trim());
				}
				catch (NumberFormatException e)
				{
					LOGGER.warning("ClanPvpZone: ID de BossZone invalido: " + parts[i]);
				}
			}
		}

		// Cargar configuracion AntiFeed desde PVP.ini
		loadAntiFeedConfig();
	}

	/** Lee la seccion AntiFeed de PVP.ini para la Zona Clan PvP. */
	private void loadAntiFeedConfig()
	{
		final Properties pvpProps = new Properties();
		try (InputStream is = new FileInputStream("./config/PVP.ini"))
		{
			pvpProps.load(is);
		}
		catch (Exception e)
		{
			LOGGER.warning("ClanPvpZone: No se pudo leer PVP.ini para AntiFeed: " + e.getMessage() + " - Usando valores por defecto.");
		}

		ANTIFEED_CLAN_ENABLED = Boolean.parseBoolean(pvpProps.getProperty("AntiFeedClanEventEnabled", "True").trim());

		LOGGER.info("ClanPvpZone: AntiFeed - ClanEnabled=" + ANTIFEED_CLAN_ENABLED);
	}

	// ---------------------------------------------------------------------------
	// API publica para GlobalGatekeeper
	// ---------------------------------------------------------------------------

	public EventState getState()
	{
		return _state;
	}

	public int getRegisteredClanCount()
	{
		return _registeredClans.size();
	}

	public int getMinClans()
	{
		return MIN_CLANS;
	}

	public int getMaxClans()
	{
		return MAX_CLANS;
	}

	public int getCountdownSeconds()
	{
		return _countdownSeconds.get();
	}

	public boolean isClanRegistered(int clanId)
	{
		return _registeredClans.containsKey(clanId);
	}

	public boolean isClanOnCooldown(int clanId)
	{
		return _lastWinnerClanId == clanId;
	}

	public Collection<String> getRegisteredClanNames()
	{
		return _registeredClans.values();
	}

	/**
	 * Registra el clan del jugador en el evento.
	 * @return Mensaje de error en caso de fallo, null si el registro fue exitoso.
	 */
	public synchronized String registerClan(Player player)
	{
		if (!ENABLED)
		{
			return "El evento no esta disponible.";
		}

		final Clan clan = player.getClan();
		if (clan == null)
		{
			return "Debes pertenecer a un clan para inscribirte.";
		}

		if ((_state != EventState.IDLE) && (_state != EventState.COUNTDOWN))
		{
			return "El evento ya ha comenzado. Espera la proxima ronda.";
		}

		if (_registeredClans.size() >= MAX_CLANS)
		{
			return "El evento ya tiene el maximo de " + MAX_CLANS + " clanes inscritos.";
		}

		final int clanId = clan.getId();

		if (isClanOnCooldown(clanId))
		{
			return "Tu clan gano el ultimo evento. Espera a que otro clan gane antes de volver a inscribirte.";
		}

		if (_registeredClans.containsKey(clanId))
		{
			return "Tu clan ya esta inscrito en el evento.";
		}

		_registeredClans.put(clanId, clan.getName());
		LOGGER.info("ClanPvpZone: Clan registrado: " + clan.getName() + " (ID=" + clanId + ")");

		Broadcast.toAllOnlinePlayers(
			"[Clan PvP Zone] El clan <" + clan.getName() + "> se ha inscrito! ("
				+ _registeredClans.size() + "/" + MIN_CLANS + " clanes listos)", false);

		// Iniciar cuenta regresiva al alcanzar el minimo de clanes
		if ((_registeredClans.size() >= MIN_CLANS) && (_state == EventState.IDLE))
		{
			startCountdown();
		}

		return null; // exito
	}

	// ---------------------------------------------------------------------------
	// Fase: COUNTDOWN
	// ---------------------------------------------------------------------------
	private void startCountdown()
	{
		_state = EventState.COUNTDOWN;
		_countdownSeconds.set(COUNTDOWN_MINUTES * 60);

		Broadcast.toAllOnlinePlayers(
			"[Clan PvP Zone] El evento comenzara en " + COUNTDOWN_MINUTES
				+ " minuto(s)! Ya hay " + _registeredClans.size() + " clanes inscritos. !Registra tu clan ahora!",
			false);

		_countdownTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			final int sec = _countdownSeconds.decrementAndGet();

			if (sec <= 0)
			{
				if (_countdownTask != null)
				{
					_countdownTask.cancel(false);
					_countdownTask = null;
				}
				startEvent();
				return;
			}

			// Anunciar en momentos clave
			if ((sec == 240) || (sec == 180) || (sec == 120) || (sec == 60) || (sec == 30) || (sec == 10) || (sec == 5) || (sec == 3) || (sec == 2) || (sec == 1))
			{
				final String timeStr = (sec >= 60) ? (sec / 60) + " minuto(s)" : sec + " segundo(s)";
				Broadcast.toAllOnlinePlayers("[Clan PvP Zone] El evento comienza en " + timeStr + "!", false);
				broadcastToRegisteredMembers(new ExShowScreenMessage("CLAN PVP ZONE comienza en " + timeStr + "!", 5000));
			}
		}, 1000, 1000);
	}

	// ---------------------------------------------------------------------------
	// Fase: ACTIVE
	// ---------------------------------------------------------------------------
	private void startEvent()
	{
		_state = EventState.ACTIVE;
		_raidPhaseStarting.set(false);
		_raidBossDeathHandled.set(false);
		_clanPlayers.clear();
		_allParticipants.clear();
		_eventReputation.clear();
		_internalTeleport.clear();

		// Lista de clanes en orden de inscripcion
		final List<Integer> clanIds = new ArrayList<>(_registeredClans.keySet());

		int clanIndex = 0;
		int totalParticipants = 0;

		for (int clanId : clanIds)
		{
			// Asignar punto de spawn por orden de inscripcion (ciclico si hay mas clanes que spawns)
			final Location spawn = CLAN_SPAWNS.get(clanIndex % CLAN_SPAWNS.size());
			final Set<Player> members = ConcurrentHashMap.newKeySet();
			_clanPlayers.put(clanId, members);
			_eventReputation.put(clanId, 0);

			// Teleportar todos los miembros online de este clan
			for (Player online : World.getInstance().getPlayers())
			{
				if ((online == null) || !online.isOnline() || (online.getClan() == null))
				{
					continue;
				}
				if (online.getClan().getId() != clanId)
				{
					continue;
				}

				members.add(online);
				_allParticipants.add(online);
				totalParticipants++;

				addParticipantListeners(online);
				whitelistInBossZones(online);

				_internalTeleport.add(online.getObjectId());
				online.teleToLocation(
					spawn.getX() + Rnd.get(-80, 80),
					spawn.getY() + Rnd.get(-80, 80),
					spawn.getZ(), 0);

				online.sendPacket(new ExShowScreenMessage(
					"CLAN PVP ZONE - El evento ha comenzado! Elimina a los clanes rivales!", 8000));
				online.sendMessage("[Clan PvP Zone] El evento ha comenzado! Solo el ultimo clan vivo avanzara al RaidBoss.");
			}

			clanIndex++;
		}

		Broadcast.toAllOnlinePlayersOnScreen(
			"CLAN PVP ZONE ha comenzado! " + clanIds.size() + " clanes en combate!");
		Broadcast.toAllOnlinePlayers(
			"[Clan PvP Zone] El evento ha comenzado! " + clanIds.size() + " clanes y " + totalParticipants + " jugadores en combate!",
			false);

		LOGGER.info("ClanPvpZone: Evento iniciado con " + clanIds.size() + " clanes y " + totalParticipants + " participantes.");

		// Verificar inmediatamente si algún clan no tenia miembros online
		ThreadPool.schedule(this::checkRemainingClans, 5000);
	}

	// ---------------------------------------------------------------------------
	// Kill handling
	// ---------------------------------------------------------------------------
	private void onParticipantDeath(OnCreatureDeath event)
	{
		if (!(event.getTarget() instanceof Player))
		{
			return;
		}
		final Player killed = (Player) event.getTarget();
		if (!_allParticipants.contains(killed))
		{
			return;
		}

		// Reputacion al clan del asesino (con control AntiFeed)
		if (event.getAttacker() instanceof Player)
		{
			final Player killer = (Player) event.getAttacker();
			if (_allParticipants.contains(killer) && (killer != killed))
			{
				final Clan killerClan = killer.getClan();
				final Clan killedClan = killed.getClan();

				if ((killerClan != null) && (killedClan != null))
				{
					// ---- AntiFeed: bloquear si killer y killed son del mismo clan ----
					if (ANTIFEED_CLAN_ENABLED && (killerClan.getId() == killedClan.getId()))
					{
						killer.sendMessage("[Anti-Feed] Kill no recompensado: el objetivo pertenece a tu mismo clan.");
					}
					else if (killerClan.getId() != killedClan.getId())
					{
						// ---- Kill valido entre clanes rivales: dar reputacion ----
						killerClan.addReputationScore(REP_PER_KILL);
						final int totalEventRep = _eventReputation.merge(killerClan.getId(), REP_PER_KILL, Integer::sum);

						killer.sendMessage("[Clan PvP Zone] +" + REP_PER_KILL
							+ " reputacion para tu clan! (Total en el evento: " + totalEventRep + ")");
					}
				}
			}
		}

		// Eliminar al jugador muerto del evento tras breve retardo (permite animacion de muerte)
		ThreadPool.schedule(() -> eliminatePlayer(killed), 3000);
	}

	private void eliminatePlayer(Player player)
	{
		if (!_allParticipants.remove(player))
		{
			return; // Ya fue eliminado
		}

		final Clan clan = player.getClan();
		if (clan != null)
		{
			final Set<Player> members = _clanPlayers.get(clan.getId());
			if (members != null)
			{
				members.remove(player);
			}
		}

		removeParticipantListeners(player);

		// Teletransportar al jugador eliminado al punto de retorno
		if (player.isOnline())
		{
			_internalTeleport.add(player.getObjectId());
			player.teleToLocation(RETURN_X, RETURN_Y, RETURN_Z, 0);
			player.sendPacket(new ExShowScreenMessage("Has sido eliminado del Clan PvP Zone.", 5000));
			player.sendMessage("[Clan PvP Zone] Has sido eliminado. El evento continua sin ti.");
		}

		// Verificar si queda solo un clan
		if (_state == EventState.ACTIVE)
		{
			checkRemainingClans();
		}
	}

	private void checkRemainingClans()
	{
		if (_state != EventState.ACTIVE)
		{
			return;
		}

		int activeClanCount = 0;
		int lastActiveClanId = -1;

		for (Map.Entry<Integer, Set<Player>> entry : _clanPlayers.entrySet())
		{
			if (!entry.getValue().isEmpty())
			{
				activeClanCount++;
				lastActiveClanId = entry.getKey();
			}
		}

		if (activeClanCount <= 1)
		{
			// Un solo clan (o ninguno) queda: avanzar a fase Raid
			// compareAndSet previene doble inicio si dos muertes simultaneas llegan aqui
			if (_raidPhaseStarting.compareAndSet(false, true))
			{
				startRaidPhase(lastActiveClanId);
			}
		}
	}

	// ---------------------------------------------------------------------------
	// Fase: RAID
	// ---------------------------------------------------------------------------
	private void startRaidPhase(int winnerClanId)
	{
		_state = EventState.RAID;
		_raidWinnerClanId = winnerClanId;
		_raidParticipants.clear();

		final String clanName = _registeredClans.getOrDefault(winnerClanId, "Desconocido");

		// Anuncio global: clan ganador de la batalla
		Broadcast.toAllOnlinePlayersOnScreen(
			"CLAN PVP ZONE: El clan <" + clanName + "> ha ganado la batalla!");
		Broadcast.toAllOnlinePlayers(
			"[Clan PvP Zone] El clan <" + clanName + "> ha vencido en el combate! Ahora enfrentara al RaidBoss!",
			false);

		// Reunir supervivientes del clan ganador
		final Set<Player> survivors = _clanPlayers.get(winnerClanId);
		if (survivors != null)
		{
			_raidParticipants.addAll(survivors);
		}

		if (_raidParticipants.isEmpty())
		{
			LOGGER.warning("ClanPvpZone: Fase Raid iniciada pero no hay supervivientes del clan " + clanName + ". Finalizando evento.");
			endEvent(true);
			return;
		}

		// Teletransportar supervivientes al area de raid (spawn 1)
		final Location raidArea = CLAN_SPAWNS.get(0);
		for (Player p : _raidParticipants)
		{
			if ((p == null) || !p.isOnline())
			{
				continue;
			}
			whitelistInBossZones(p);
			_internalTeleport.add(p.getObjectId());
			p.teleToLocation(raidArea.getX() + Rnd.get(-80, 80), raidArea.getY() + Rnd.get(-80, 80), raidArea.getZ(), 0);
			p.sendPacket(new ExShowScreenMessage(
				"!HAN AVANZADO AL RAIDBOSS! Preparate para el combate final!", 8000));
			p.sendMessage("[Clan PvP Zone] Tu clan ha avanzado! El RaidBoss aparecera en 5 segundos. !Eliminalo para ganar la recompensa!");
		}

		// Activar tarea de neutralizacion del Raid Curse
		startRaidCurseRemoval();

		// Spawnear el boss 5 segundos despues (tiempo para que los jugadores carguen la zona)
		ThreadPool.schedule(this::spawnRaidBoss, 5000);

		LOGGER.info("ClanPvpZone: Fase Raid iniciada para clan " + clanName + " con " + _raidParticipants.size() + " supervivientes.");
	}

	private void spawnRaidBoss()
	{
		try
		{
			_raidBoss = addSpawn(RAID_BOSS_ID, RAID_BOSS_X, RAID_BOSS_Y, RAID_BOSS_Z, RAID_BOSS_HEADING, false, 0, false);

			if (_raidBoss == null)
			{
				LOGGER.warning("ClanPvpZone: addSpawn retorno null para RaidBoss ID=" + RAID_BOSS_ID + ". Verifica el ID en el config.");
				endEvent(false);
				return;
			}

			// Agregar listener de muerte directamente al NPC instanciado
			_raidBoss.addListener(new ConsumerEventListener(
				_raidBoss,
				EventType.ON_CREATURE_DEATH,
				(OnCreatureDeath event) ->
				{
					if (event.getTarget() == _raidBoss)
					{
						onRaidBossDeath();
					}
				},
				this));

			// Notificar a los participantes
			for (Player p : _raidParticipants)
			{
				if ((p != null) && p.isOnline())
				{
					p.sendPacket(new ExShowScreenMessage("!EL RAIDBOSS HA APARECIDO! ELIMINALO!", 5000));
				}
			}

			LOGGER.info("ClanPvpZone: RaidBoss " + RAID_BOSS_ID + " spawneado en ("
				+ RAID_BOSS_X + "," + RAID_BOSS_Y + "," + RAID_BOSS_Z + ").");
		}
		catch (Exception e)
		{
			LOGGER.warning("ClanPvpZone: Error al spawnear RaidBoss ID=" + RAID_BOSS_ID + ": " + e.getMessage());
			endEvent(false);
		}
	}

	/**
	 * Tarea periodica que remueve el Raid Curse de los participantes cada segundo.
	 * Previene el debuff automatico que aplica el engine al atacar un RaidBoss.
	 */
	private void startRaidCurseRemoval()
	{
		if (_raidCurseTask != null)
		{
			_raidCurseTask.cancel(false);
		}

		final Skill curseSkill = SkillData.getInstance().getSkill(RAID_CURSE_SKILL_ID, 1);
		if (curseSkill == null)
		{
			LOGGER.warning("ClanPvpZone: Skill de Raid Curse ID=" + RAID_CURSE_SKILL_ID + " no encontrado. El debuff puede aplicarse.");
			return;
		}

		_raidCurseTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			for (Player p : _raidParticipants)
			{
				if ((p != null) && p.isOnline() && !p.isDead())
				{
					p.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, curseSkill);
				}
			}
		}, 500, 1000);
	}

	private void stopRaidCurseRemoval()
	{
		if (_raidCurseTask != null)
		{
			_raidCurseTask.cancel(false);
			_raidCurseTask = null;
		}
	}

	private void onRaidBossDeath()
	{
		// Proteccion contra doble ejecucion (listeners pueden disparar mas de una vez)
		if (!_raidBossDeathHandled.compareAndSet(false, true))
		{
			return;
		}

		stopRaidCurseRemoval();

		final String clanName = _registeredClans.getOrDefault(_raidWinnerClanId, "Desconocido");

		// Anuncio global de victoria final
		Broadcast.toAllOnlinePlayersOnScreen(
			"CLAN PVP ZONE: El clan <" + clanName + "> ha vencido al RaidBoss y ganado el torneo!");
		Broadcast.toAllOnlinePlayers(
			"[Clan PvP Zone] !El clan <" + clanName + "> ha vencido al RaidBoss! Son los campeones del Clan PvP Zone! Felicidades!",
			false);

		// Entregar recompensa a cada participante de raid que este vivo y online
		int rewarded = 0;
		for (Player p : _raidParticipants)
		{
			if ((p != null) && p.isOnline())
			{
				p.addItem(ItemProcessType.REWARD, REWARD_ITEM_ID, REWARD_COUNT, p, true);
				p.sendPacket(new ExShowScreenMessage(
					"VICTORIA! Has recibido tu recompensa! Seras teletransportado en 15 segundos.", 10000));
				p.sendMessage("[Clan PvP Zone] !Victoria! Recibiste " + REWARD_COUNT + " de recompensa. Teletransportando en 15 segundos...");
				rewarded++;
			}
		}

		// Marcar clan ganador para cooldown
		_lastWinnerClanId = _raidWinnerClanId;

		LOGGER.info("ClanPvpZone: RaidBoss eliminado. Recompensa entregada a " + rewarded + " jugadores del clan " + clanName + ".");

		// Mensajes de cuenta regresiva antes del teleport forzado
		ThreadPool.schedule(() -> broadcastToRaidParticipants(new ExShowScreenMessage("Teletransportando en 10 segundos...", 5000)), 5000);
		ThreadPool.schedule(() -> broadcastToRaidParticipants(new ExShowScreenMessage("Teletransportando en 5 segundos...", 5000)), 10000);

		// Teleport forzado 15 segundos despues de recibir la recompensa
		ThreadPool.schedule(this::ejectRaidParticipants, 15000);
	}

	private void ejectRaidParticipants()
	{
		stopRaidCurseRemoval();

		for (Player p : _raidParticipants)
		{
			if (p == null)
			{
				continue;
			}
			removeParticipantListeners(p);
			if (p.isOnline())
			{
				_internalTeleport.add(p.getObjectId());
				p.teleToLocation(RETURN_X, RETURN_Y, RETURN_Z, 0);
				p.sendMessage("[Clan PvP Zone] Has sido teletransportado. Hasta la proxima!");
			}
		}

		endEvent(true);
	}

	// ---------------------------------------------------------------------------
	// Fin del evento — limpieza total
	// ---------------------------------------------------------------------------
	private void endEvent(boolean cleanupRemaining)
	{
		// Cancelar countdown si aun estaba activo
		if (_countdownTask != null)
		{
			_countdownTask.cancel(false);
			_countdownTask = null;
		}
		stopRaidCurseRemoval();

		// Teletransportar y limpiar participantes que aun no fueron procesados
		if (cleanupRemaining)
		{
			for (Player p : _allParticipants)
			{
				if (p == null)
				{
					continue;
				}
				removeParticipantListeners(p);
				if (p.isOnline())
				{
					_internalTeleport.add(p.getObjectId());
					p.teleToLocation(RETURN_X, RETURN_Y, RETURN_Z, 0);
				}
			}
		}

		// Eliminar boss si aun vive
		final Npc boss = _raidBoss;
		if ((boss != null) && !boss.isDead())
		{
			boss.deleteMe();
		}
		_raidBoss = null;

		// Limpiar todas las estructuras de estado
		_allParticipants.clear();
		_clanPlayers.clear();
		_raidParticipants.clear();
		_registeredClans.clear();
		_eventReputation.clear();
		_internalTeleport.clear();
		_countdownSeconds.set(0);
		_raidWinnerClanId = -1;
		_raidPhaseStarting.set(false);
		_raidBossDeathHandled.set(false);

		_state = EventState.IDLE;

		LOGGER.info("ClanPvpZone: Evento finalizado. Estado: IDLE.");
	}

	// ---------------------------------------------------------------------------
	// Listeners por participante
	// ---------------------------------------------------------------------------
	private void addParticipantListeners(Player player)
	{
		player.addListener(new ConsumerEventListener(
			player,
			EventType.ON_CREATURE_DEATH,
			(OnCreatureDeath event) -> onParticipantDeath(event),
			this));

		player.addListener(new ConsumerEventListener(
			player,
			EventType.ON_PLAYER_LOGOUT,
			(OnPlayerLogout event) -> eliminatePlayer(event.getPlayer()),
			this));
	}

	private void removeParticipantListeners(Player player)
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
	}

	// ---------------------------------------------------------------------------
	// BossZone whitelist
	// ---------------------------------------------------------------------------
	private void whitelistInBossZones(Player player)
	{
		for (int zoneId : _bossZoneIds)
		{
			try
			{
				final BossZone bz = ZoneManager.getInstance().getZoneById(zoneId, BossZone.class);
				if (bz != null)
				{
					// Permitir entrada por el tiempo maximo del evento + margen
					bz.allowPlayerEntry(player, (COUNTDOWN_MINUTES * 60) + 7200);
				}
			}
			catch (Exception e)
			{
				// Zone no cargada o tipo incorrecto — ignorar
			}
		}
	}

	// ---------------------------------------------------------------------------
	// Helpers de broadcast
	// ---------------------------------------------------------------------------
	private void broadcastToRegisteredMembers(ExShowScreenMessage msg)
	{
		for (Player p : World.getInstance().getPlayers())
		{
			if ((p != null) && p.isOnline() && (p.getClan() != null)
				&& _registeredClans.containsKey(p.getClan().getId()))
			{
				p.sendPacket(msg);
			}
		}
	}

	private void broadcastToRaidParticipants(ExShowScreenMessage msg)
	{
		for (Player p : _raidParticipants)
		{
			if ((p != null) && p.isOnline())
			{
				p.sendPacket(msg);
			}
		}
	}

	// ---------------------------------------------------------------------------
	// Bypass Handler: clanpvz_register
	// ---------------------------------------------------------------------------
	private class ClanPvpBypass implements IBypassHandler
	{
		private final String[] COMMANDS =
		{
			"clanpvz_register"
		};

		@Override
		public boolean onCommand(String command, Player player, Creature target)
		{
			if (!ENABLED || (player == null))
			{
				return false;
			}

			if ("clanpvz_register".equals(command))
			{
				final String error = registerClan(player);
				if (error != null)
				{
					player.sendMessage("[Clan PvP Zone] " + error);
				}
				else
				{
					player.sendMessage("[Clan PvP Zone] !Tu clan ha sido inscrito exitosamente!");
					player.sendPacket(new ExShowScreenMessage("Clan inscrito en el Clan PvP Zone!", 4000));
				}
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
	// Entry point
	// ---------------------------------------------------------------------------
	public static void main(String[] args)
	{
		new ClanPvpZone();
	}
}
