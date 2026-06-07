/*
 * Faction Battleground Event — Pagan Temple
 * Radiant (Team.BLUE) vs Dire (Team.RED)
 *
 * Mecanica:
 *  - Cada bando destruye 3 torres enemigas (Tier 1 → 2 → 3) y luego la Bandera.
 *  - Guardias de cada bando avanzan automaticamente al Tier activo enemigo.
 *  - PvP libre: sin karma, sin PK count, sin teleports de scroll.
 *  - Anti-dualbox via AntiFeedManager (configurable).
 *  - Ganador: multisell de equipo +20. Perdedor: item de consolacion.
 *  - Al terminar: todos van a Giran y el registro se reabre (loop continuo).
 */
package custom.events.FactionBattleground;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Team;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.groups.CommandChannel;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.groups.PartyDistributionType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.model.script.Event;
import org.l2jmobius.gameserver.model.script.QuestTimer;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.RadarControl;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author Custom — FactionBattleground
 */
public class FactionBattleground extends Event
{
	// =========================================================================
	// Estado del evento
	// =========================================================================
	enum EventState { INACTIVE, REGISTRATION, STARTING, ACTIVE }

	private static final String HTML  = "data/scripts/custom/events/FactionBattleground/";
	private static final String CONFIG = "config/Custom/FactionBattleground.ini";

	// =========================================================================
	// Configuracion
	// =========================================================================
	private static boolean      C_ENABLED           = true;
	private static boolean      C_AUTO              = true;
	private static List<Integer> C_DAYS             = new ArrayList<>();
	private static boolean      C_ALTERNATE         = true;
	private static int          C_HOUR              = 20;
	private static int          C_MINUTE            = 0;
	private static int          C_MIN_PL            = 4;
	private static int          C_MAX_PL            = 100;
	private static int          C_MIN_LV            = 76;
	private static int          C_MAX_LV            = 85;
	private static int          C_REG_TIME          = 5;   // minutos
	private static boolean      C_DUALBOX           = false;
	private static String       C_RADIANT_NAME      = "Radiant";
	private static String       C_DIRE_NAME         = "Dire";
	private static int          C_RADIANT_COLOR     = 0xFF4444;
	private static int          C_DIRE_COLOR        = 0x4444FF;
	private static int          C_REG_NPC           = 70020;
	private static int          C_TOWER_NPC         = 70021;
	private static int          C_FLAG_NPC          = 70022;
	private static int          C_RADIANT_GUARD     = 21070; // Seal Arcangel
	private static int          C_DIRE_GUARD        = 22350; // Chimera of Darkness
	private static int          C_GUARD_COUNT       = 10;
	private static int          C_GUARD_RESPAWN     = 60;   // segundos
	private static int          C_TOWER_HP          = 1_000_000;
	private static int          C_FLAG_HP           = 4_000_000;
	private static ItemHolder   C_WIN_REWARD        = new ItemHolder(6673, 5);
	private static ItemHolder   C_LOSE_REWARD       = new ItemHolder(6673, 1);
	private static int          C_WIN_MULTISELL     = 32100;
	private static int[]        C_DOORS             = {};

	// Localizaciones
	private static Location C_MANAGER_LOC   = new Location(83478, 149264, -3400, 0);
	private static Location C_GIRAN         = new Location(83478, 149264, -3400);
	private static Location C_RADIANT_SPAWN = new Location(-20389, -54336, -11112, 0);
	private static Location C_DIRE_SPAWN    = new Location(-12682, -54193, -11112, 0);
	private static Location C_RADIANT_FLAG  = new Location(-20229, -54359, -11114, 0);
	private static Location C_DIRE_FLAG     = new Location(-12479, -54302, -11114, 0);
	// Torres[0]=Tier1(mas externo)  [1]=Tier2  [2]=Tier3(mas interno)
	private static final Location[] C_RADIANT_TOWERS = new Location[3]; // Dire las destruye
	private static final Location[] C_DIRE_TOWERS    = new Location[3]; // Radiant las destruye

	// =========================================================================
	// Estado runtime
	// =========================================================================
	private static volatile EventState _state = EventState.INACTIVE;

	private static final Set<Player> ALL     = ConcurrentHashMap.newKeySet();
	private static final Set<Player> RADIANT = ConcurrentHashMap.newKeySet();
	private static final Set<Player> DIRE    = ConcurrentHashMap.newKeySet();

	// Progreso: cuantas torres del lado contrario destruyo este equipo (0-3)
	private static volatile int _radiantProg = 0; // torres Dire destruidas por Radiant
	private static volatile int _direProg    = 0; // torres Radiant destruidas por Dire

	// NPCs activos
	private static final Npc[] _rTowers = new Npc[3]; // protegen base Radiant; Dire los ataca
	private static final Npc[] _dTowers = new Npc[3]; // protegen base Dire; Radiant los ataca
	private static Npc _rFlag = null;
	private static Npc _dFlag = null;
	private static final List<Npc> _rGuards = new ArrayList<>();
	private static final List<Npc> _dGuards = new ArrayList<>();

	// Seguimiento de recompensas (una sola vez por ciclo de evento)
	private static final Set<Integer> _pendingReward = ConcurrentHashMap.newKeySet();
	private static final Set<Integer> _claimedReward = ConcurrentHashMap.newKeySet();

	// Fuego amigo: objectId → cantidad de infracciones en este ciclo
	private static final Map<Integer, Integer> _friendlyFireStrikes = new ConcurrentHashMap<>();
	// Jugadores petrificados permanentemente hasta el fin del evento
	private static final Set<Integer> _permanentPetrify = ConcurrentHashMap.newKeySet();

	// Skills de petrificacion (Dance of Medusa): TURN_STONE + Petrification (PARALYZED|INVUL)
	// Definidas en data/stats/skills/custom/faction_battleground.xml
	private static final int PETRIFY_10S  = 10050; // 1ra infraccion: 10 segundos
	private static final int PETRIFY_60S  = 10051; // 2da infraccion: 60 segundos
	private static final int PETRIFY_PERM = 10052; // 3ra infraccion: hasta fin del evento

	// Control de dias alternos
	private static int     _lastDayOfYear   = -1;
	private static boolean _nextSlotEnabled = true;

	// =========================================================================
	// Constructor
	// =========================================================================
	private FactionBattleground()
	{
		loadConfig();
		if (!C_ENABLED) { LOGGER.info("FactionBattleground: deshabilitado."); return; }

		addStartNpc(C_REG_NPC);
		addTalkId(C_REG_NPC);
		addFirstTalkId(C_REG_NPC);
		addKillId(C_TOWER_NPC, C_FLAG_NPC, C_RADIANT_GUARD, C_DIRE_GUARD);
		addAttackId(C_TOWER_NPC, C_FLAG_NPC, C_RADIANT_GUARD, C_DIRE_GUARD);

		// Auto-scheduler (cada 60s via getTimers para no perder el ciclo)
		if (C_AUTO) getTimers().addTimer("AutoCheck", 60_000, null, null);

		LOGGER.info("FactionBattleground: listo. Auto=" + C_AUTO + " Hora=" + C_HOUR + ":" + String.format("%02d", C_MINUTE) + " Dias=" + C_DAYS);
	}

	// =========================================================================
	// Carga de config
	// =========================================================================
	private void loadConfig()
	{
		final Properties p = new Properties();
		try (FileInputStream fis = new FileInputStream(CONFIG)) { p.load(fis); }
		catch (IOException e) { LOGGER.warning("FactionBattleground: no se pudo cargar " + CONFIG); }

		C_ENABLED       = bl(p, "EnableFactionBattleground", true);
		C_AUTO          = bl(p, "AutoEventEnabled", true);
		C_ALTERNATE     = bl(p, "AlternateDays", true);
		C_HOUR          = in(p, "EventHour", 20);
		C_MINUTE        = in(p, "EventMinute", 0);
		C_MIN_PL        = in(p, "MinPlayers", 4);
		C_MAX_PL        = in(p, "MaxPlayers", 100);
		C_MIN_LV        = in(p, "MinLevel", 76);
		C_MAX_LV        = in(p, "MaxLevel", 85);
		C_REG_TIME      = in(p, "RegistrationTime", 5);
		C_DUALBOX       = bl(p, "AllowDualbox", false);
		C_RADIANT_NAME  = p.getProperty("RadiantName", "Radiant").trim();
		C_DIRE_NAME     = p.getProperty("DireName", "Dire").trim();
		C_RADIANT_COLOR = hx(p, "RadiantColor", 0xFF4444);
		C_DIRE_COLOR    = hx(p, "DireColor", 0x4444FF);

		C_DAYS = new ArrayList<>();
		for (String d : p.getProperty("EventDays", "Saturday,Sunday").split(","))
		{
			switch (d.trim().toLowerCase())
			{
				case "monday":    C_DAYS.add(Calendar.MONDAY);    break;
				case "tuesday":   C_DAYS.add(Calendar.TUESDAY);   break;
				case "wednesday": C_DAYS.add(Calendar.WEDNESDAY); break;
				case "thursday":  C_DAYS.add(Calendar.THURSDAY);  break;
				case "friday":    C_DAYS.add(Calendar.FRIDAY);    break;
				case "saturday":  C_DAYS.add(Calendar.SATURDAY);  break;
				case "sunday":    C_DAYS.add(Calendar.SUNDAY);    break;
			}
		}

		C_REG_NPC       = in(p, "RegistrationNpcId", 70020);
		C_TOWER_NPC     = in(p, "TowerNpcId", 70021);
		C_FLAG_NPC      = in(p, "FlagNpcId", 70022);
		C_RADIANT_GUARD = in(p, "RadiantGuardId", 21070);
		C_DIRE_GUARD    = in(p, "DireGuardId", 22350);
		C_GUARD_COUNT   = in(p, "GuardCountPerFaction", 10);
		C_GUARD_RESPAWN = in(p, "GuardRespawnSeconds", 60);
		C_TOWER_HP      = in(p, "TowerHp", 1_000_000);
		C_FLAG_HP       = in(p, "FlagHp", 4_000_000);
		C_WIN_REWARD    = new ItemHolder(in(p, "WinnerRewardId", 6673), in(p, "WinnerRewardCount", 5));
		C_LOSE_REWARD   = new ItemHolder(in(p, "LoserRewardId",  6673), in(p, "LoserRewardCount",  1));
		C_WIN_MULTISELL = in(p, "WinnerMultisellId", 32100);

		final String ds = p.getProperty("DoorIds", "0").trim();
		if (!ds.equals("0") && !ds.isEmpty())
		{
			final String[] parts = ds.split(",");
			C_DOORS = new int[parts.length];
			for (int i = 0; i < parts.length; i++) try { C_DOORS[i] = Integer.parseInt(parts[i].trim()); } catch (NumberFormatException e2) {}
		}
		else C_DOORS = new int[0];

		C_MANAGER_LOC   = lc(p, "ManagerSpawn",  "83478,149264,-3400,0");
		C_GIRAN         = new Location(C_MANAGER_LOC.getX(), C_MANAGER_LOC.getY(), C_MANAGER_LOC.getZ());
		C_RADIANT_SPAWN = lc(p, "RadiantSpawn",  "-20389,-54336,-11112,0");
		C_DIRE_SPAWN    = lc(p, "DireSpawn",     "-12682,-54193,-11112,0");
		C_RADIANT_FLAG  = lc(p, "RadiantFlagLoc","-20229,-54359,-11114,0");
		C_DIRE_FLAG     = lc(p, "DireFlagLoc",   "-12479,-54302,-11114,0");

		C_RADIANT_TOWERS[0] = lc(p, "RadiantTower1", "-20568,-45117,-10720,0");
		C_RADIANT_TOWERS[1] = lc(p, "RadiantTower2", "-20608,-49150,-10912,0");
		C_RADIANT_TOWERS[2] = lc(p, "RadiantTower3", "-20278,-52651,-10936,0");
		C_DIRE_TOWERS[0]    = lc(p, "DireTower1",    "-12171,-45024,-10720,0");
		C_DIRE_TOWERS[1]    = lc(p, "DireTower2",    "-12151,-49184,-10912,0");
		C_DIRE_TOWERS[2]    = lc(p, "DireTower3",    "-12761,-52626,-10938,0");
	}

	private boolean  bl(Properties p, String k, boolean  d) { return Boolean.parseBoolean(p.getProperty(k, String.valueOf(d)).trim()); }
	private int      in(Properties p, String k, int      d) { try { return Integer.parseInt(p.getProperty(k, String.valueOf(d)).trim()); } catch (NumberFormatException e) { return d; } }
	private int      hx(Properties p, String k, int      d) { try { return (int) Long.parseLong(p.getProperty(k, Integer.toHexString(d)).trim(), 16); } catch (NumberFormatException e) { return d; } }
	private Location lc(Properties p, String k, String   d)
	{
		final String[] s = p.getProperty(k, d).split(",");
		return new Location(Integer.parseInt(s[0].trim()), Integer.parseInt(s[1].trim()), Integer.parseInt(s[2].trim()), s.length > 3 ? Integer.parseInt(s[3].trim()) : 0);
	}

	// =========================================================================
	// onTimerEvent — SOLO para AutoCheck (getTimers().addTimer)
	// =========================================================================
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		if (!"AutoCheck".equals(event)) return;

		// Re-schedule para el minuto siguiente
		getTimers().addTimer("AutoCheck", 60_000, null, null);

		if (_state != EventState.INACTIVE) return;

		final Calendar now = Calendar.getInstance();
		final int dow   = now.get(Calendar.DAY_OF_WEEK);
		final int hour  = now.get(Calendar.HOUR_OF_DAY);
		final int min   = now.get(Calendar.MINUTE);
		final int doy   = now.get(Calendar.DAY_OF_YEAR);

		if (!C_DAYS.contains(dow))   return;
		if (hour != C_HOUR || min != C_MINUTE) return;
		if (doy == _lastDayOfYear)   return;

		if (C_ALTERNATE)
		{
			if (!_nextSlotEnabled) { _nextSlotEnabled = true; return; }
			_nextSlotEnabled = false;
		}

		if (siegeActive())
		{
			Broadcast.toAllOnlinePlayers("Faction Battleground: Evento pospuesto — hay un asedio en curso.");
			return;
		}

		eventStart(null);
	}

	// =========================================================================
	// onEvent — NPC talk + todos los startQuestTimer
	// =========================================================================
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		// ---- Acciones de dialogo NPC (requieren player != null) ----
		if (player != null)
		{
			switch (event)
			{
				case "JoinRadiant": return handleJoin(player, npc, true);
				case "JoinDire":    return handleJoin(player, npc, false);
				case "CancelReg":   return handleCancel(player);
				case "ClaimReward":
					if (_pendingReward.contains(player.getObjectId()) && !_claimedReward.contains(player.getObjectId()))
					{
						_pendingReward.remove(player.getObjectId());
						_claimedReward.add(player.getObjectId());
						MultisellData.getInstance().separateAndSend(C_WIN_MULTISELL, player, null, false);
						player.sendMessage("Elige tu equipo +20 en el multisell.");
					}
					else
					{
						player.sendMessage("Ya reclamaste tu recompensa o no eres ganador de este evento.");
					}
					return null;
				case "AdminStop":
					if (player.isGM()) eventStop();
					return null;
			}
		}

		// ---- Timers de startQuestTimer ----
		switch (event)
		{
			case "5": case "4": case "3": case "2": case "1":
				screenAll("El evento comienza en: " + event, 3);
				break;

			case "RegWarning":
				if (_state == EventState.REGISTRATION)
					Broadcast.toAllOnlinePlayers("Faction Battleground: Queda 1 minuto para cerrar el registro! Registrados: " + ALL.size() + " jugadores. Habla con el NPC en Giran.");
				break;

			case "StartFight":
				startFight();
				break;

			case "SpawnZone":
				spawnZoneElements();
				break;

			case "GuardAI":
				if (_state == EventState.ACTIVE)
				{
					redirectGuards();
					startQuestTimer("GuardAI", 5_000, null, null);
				}
				break;

			case "RespawnRGuard":
				if (_state == EventState.ACTIVE) spawnGuard(C_RADIANT_GUARD, C_RADIANT_FLAG, _rGuards, activeDireStructureForGuard());
				break;

			case "RespawnDGuard":
				if (_state == EventState.ACTIVE) spawnGuard(C_DIRE_GUARD, C_DIRE_FLAG, _dGuards, activeRadiantStructureForGuard());
				break;

			case "TeleportOut":
				doTeleportOut();
				break;

			case "AutoRestart":
				if (_state == EventState.INACTIVE && !siegeActive()) eventStart(null);
				break;
		}

		// Respawn individual de jugador muerto: key "Respawn_<objectId>"
		if (event.startsWith("Unpetrify_") && player != null)
		{
			if (!_permanentPetrify.contains(player.getObjectId()))
				unpetrify(player);
		}

		if (event.startsWith("Respawn_") && player != null && ALL.contains(player) && player.isDead())
		{
			final Location base = (player.getTeam() == Team.BLUE) ? C_RADIANT_SPAWN : C_DIRE_SPAWN;
			player.setIsPendingRevive(true);
			player.teleToLocation(base, 80);
		}

		return null;
	}

	// =========================================================================
	// onFirstTalk — dialogo del NPC de registro (HTML dinamico estilo PvpZone)
	// =========================================================================
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.getId() != C_REG_NPC) return null;

		final StringBuilder sb = new StringBuilder();

		// Premio pendiente: mostrar boton de reclamo sin importar el estado del evento
		if (_pendingReward.contains(player.getObjectId()))
		{
			sb.append("<html><body>");
			sb.append("<center><font color=\"LEVEL\">Faction Battleground</font></center><br>");
			sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
			sb.append("<br><center><font color=\"00FF00\">Tienes un premio pendiente!</font></center><br><br>");
			sb.append("<center><button value=\"Reclamar Premio\" action=\"bypass -h Script FactionBattleground ClaimReward\" width=\"200\" height=\"30\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center><br>");
			sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\">");
			sb.append("</body></html>");
			final NpcHtmlMessage html = new NpcHtmlMessage();
			html.setHtml(sb.toString());
			player.sendPacket(html);
			return null;
		}

		switch (_state)
		{
			case REGISTRATION:
			{
				sb.append("<html><body>");
				sb.append("<center><font color=\"LEVEL\">Faction Battleground</font></center><br>");
				sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
				if (ALL.contains(player))
				{
					final String faction = RADIANT.contains(player) ? "<font color=\"6699FF\">" + C_RADIANT_NAME + " (Azul)</font>" : "<font color=\"FF4444\">" + C_DIRE_NAME + " (Rojo)</font>";
					sb.append("<center><font color=\"00FF00\">Ya estas registrado - Bando: ").append(faction).append("</font></center><br>");
					sb.append("<table width=\"270\">");
					sb.append("<tr><td><font color=\"6699FF\">").append(C_RADIANT_NAME).append("</font></td><td><font color=\"FF4444\">").append(C_DIRE_NAME).append("</font></td></tr>");
					sb.append("<tr><td><font color=\"LEVEL\">").append(RADIANT.size()).append(" jugadores</font></td><td><font color=\"LEVEL\">").append(DIRE.size()).append(" jugadores</font></td></tr>");
					sb.append("</table><br>");
					sb.append("Espera el inicio - seras teletransportado automaticamente.<br><br>");
					sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
					sb.append("<center><button value=\"Cancelar registro\" action=\"bypass -h Script FactionBattleground CancelReg\" width=\"200\" height=\"30\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>");
				}
				else
				{
					sb.append("<font color=\"808080\">Pagan Temple - War Zone</font><br><br>");
					sb.append("<table width=\"270\">");
					sb.append("<tr><td><font color=\"6699FF\">").append(C_RADIANT_NAME).append("</font></td><td><font color=\"FF4444\">").append(C_DIRE_NAME).append("</font></td></tr>");
					sb.append("<tr><td><font color=\"LEVEL\">").append(RADIANT.size()).append(" jugadores</font></td><td><font color=\"LEVEL\">").append(DIRE.size()).append(" jugadores</font></td></tr>");
					sb.append("</table><br>");
					sb.append("<font color=\"808080\">Nivel: ").append(C_MIN_LV).append("-").append(C_MAX_LV).append(" | Jugadores: min ").append(C_MIN_PL).append(" / max ").append(C_MAX_PL).append("</font><br><br>");
					sb.append("<font color=\"FFAA00\">Destruye las 3 torres y la bandera enemiga para ganar.</font><br>");
					sb.append("<font color=\"FFAA00\">Sin karma. Sin PK. Sin limite de tiempo.</font><br><br>");
					sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
					sb.append("<center>");
					sb.append("<button value=\"").append(C_RADIANT_NAME).append(" (Azul)\" action=\"bypass -h Script FactionBattleground JoinRadiant\" width=\"200\" height=\"30\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br>");
					sb.append("<button value=\"").append(C_DIRE_NAME).append(" (Rojo)\" action=\"bypass -h Script FactionBattleground JoinDire\" width=\"200\" height=\"30\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
					sb.append("</center>");
				}
				sb.append("</body></html>");
				break;
			}
			case ACTIVE:
			case STARTING:
			{
				sb.append("<html><body>");
				sb.append("<center><font color=\"LEVEL\">Faction Battleground</font></center><br>");
				sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
				sb.append("<br><center><font color=\"FFAA00\">El evento esta en curso.</font></center><br>");
				sb.append("El combate en el Pagan Temple ya inicio.<br>");
				sb.append("Podras unirte en el siguiente registro.<br><br>");
				sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\">");
				sb.append("</body></html>");
				break;
			}
			default:
			{
				sb.append("<html><body>");
				sb.append("<center><font color=\"LEVEL\">Faction Battleground</font></center><br>");
				sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
				sb.append("<font color=\"808080\">Pagan Temple - War Zone</font><br><br>");
				sb.append("El evento no esta activo en este momento.<br><br>");
				sb.append("<font color=\"808080\">Se activa automaticamente los sabados y domingos a las 20:00.</font><br>");
				sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\">");
				sb.append("</body></html>");
				break;
			}
		}

		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setHtml(sb.toString());
		player.sendPacket(html);
		return null;
	}

	// =========================================================================
	// Registro
	// =========================================================================
	private String handleJoin(Player pl, Npc npc, boolean joinRadiant)
	{
		if (_state != EventState.REGISTRATION) { pl.sendMessage("El registro no esta abierto."); return null; }
		if (!canReg(pl)) return null;

		// Dualbox
		if (!C_DUALBOX && DualboxCheckConfig.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP > 0)
		{
			if (!AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.L2EVENT_ID, pl, 1))
			{
				final StringBuilder sb = new StringBuilder();
				sb.append("<html><body>");
				sb.append("<center><font color=\"LEVEL\">Faction Battleground</font></center><br>");
				sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
				sb.append("<br><center><font color=\"FF6347\">Acceso denegado</font></center><br><br>");
				sb.append("Ya hay una cuenta de tu IP registrada en este evento.<br>");
				sb.append("No se permite dualbox en Faction Battleground.<br><br>");
				sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\">");
				sb.append("</body></html>");
				final NpcHtmlMessage h = new NpcHtmlMessage();
				h.setHtml(sb.toString());
				pl.sendPacket(h);
				return null;
			}
		}

		// Balance: max 2 de diferencia
		if (joinRadiant && RADIANT.size() >= DIRE.size() + 2) { pl.sendMessage("Elige " + C_DIRE_NAME + " para equilibrar."); removeDualbox(pl); return null; }
		if (!joinRadiant && DIRE.size() >= RADIANT.size() + 2) { pl.sendMessage("Elige " + C_RADIANT_NAME + " para equilibrar."); removeDualbox(pl); return null; }

		ALL.add(pl);
		(joinRadiant ? RADIANT : DIRE).add(pl);
		pl.setRegisteredOnEvent(true);
		addLogoutL(pl);

		final String factionName = joinRadiant ? C_RADIANT_NAME : C_DIRE_NAME;
		final String factionColor = joinRadiant ? "6699FF" : "FF4444";
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<center><font color=\"LEVEL\">Faction Battleground</font></center><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
		sb.append("<center><font color=\"00FF00\">Registro exitoso!</font></center><br>");
		sb.append("<font color=\"LEVEL\">Bando: <font color=\"").append(factionColor).append("\">").append(factionName).append("</font></font><br><br>");
		sb.append("<table width=\"270\">");
		sb.append("<tr><td><font color=\"6699FF\">").append(C_RADIANT_NAME).append("</font></td><td><font color=\"FF4444\">").append(C_DIRE_NAME).append("</font></td></tr>");
		sb.append("<tr><td><font color=\"LEVEL\">").append(RADIANT.size()).append("</font></td><td><font color=\"LEVEL\">").append(DIRE.size()).append("</font></td></tr>");
		sb.append("</table><br>");
		sb.append("Espera el inicio - seras teletransportado automaticamente.<br><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\">");
		sb.append("</body></html>");
		final NpcHtmlMessage h = new NpcHtmlMessage();
		h.setHtml(sb.toString());
		pl.sendPacket(h);
		return null;
	}

	private String handleCancel(Player pl)
	{
		if (!ALL.contains(pl))               { pl.sendMessage("No estas registrado."); return null; }
		if (_state != EventState.REGISTRATION) { pl.sendMessage("Ya no puedes cancelar."); return null; }
		removeDualbox(pl);
		ALL.remove(pl); RADIANT.remove(pl); DIRE.remove(pl);
		removeL(pl); pl.setRegisteredOnEvent(false);
		pl.sendMessage("Registro cancelado.");
		return null;
	}

	private boolean canReg(Player pl)
	{
		if (ALL.contains(pl))                           { pl.sendMessage("Ya estas registrado."); return false; }
		if (pl.getLevel() < C_MIN_LV || pl.getLevel() > C_MAX_LV) { pl.sendMessage("Nivel requerido: " + C_MIN_LV + "-" + C_MAX_LV + "."); return false; }
		if (ALL.size() >= C_MAX_PL)                     { pl.sendMessage("El evento esta lleno."); return false; }
		if (pl.isRegisteredOnEvent() || pl.getBlockCheckerArena() > -1) { pl.sendMessage("Ya estas en otro evento."); return false; }
		if (pl.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(pl)) { pl.sendMessage("No durante la Olimpiada."); return false; }
		if (pl.isInSiege() || pl.isInsideZone(ZoneId.SIEGE)) { pl.sendMessage("No durante un asedio."); return false; }
		if (pl.isFlyingMounted() || pl.isTransformed())  { pl.sendMessage("No puedes registrarte en ese estado."); return false; }
		if (pl.getKarma() > 0 || pl.isCursedWeaponEquipped()) { pl.sendMessage("No con karma negativo."); return false; }
		if (!pl.isInventoryUnder80(false))               { pl.sendMessage("Inventario muy lleno."); return false; }
		return true;
	}

	private void removeDualbox(Player pl)
	{
		if (!C_DUALBOX && DualboxCheckConfig.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP > 0)
			AntiFeedManager.getInstance().removePlayer(AntiFeedManager.L2EVENT_ID, pl);
	}

	// =========================================================================
	// eventStart — abre el registro
	// =========================================================================
	@Override
	public boolean eventStart(Player eventMaker)
	{
		if (_state != EventState.INACTIVE) { if (eventMaker != null) eventMaker.sendMessage("El evento ya esta activo."); return false; }

		_pendingReward.clear(); _claimedReward.clear(); _friendlyFireStrikes.clear(); _permanentPetrify.clear();
		setState(EventState.REGISTRATION);
		cancelTimers();

		if (!C_DUALBOX && DualboxCheckConfig.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP > 0)
		{
			AntiFeedManager.getInstance().registerEvent(AntiFeedManager.L2EVENT_ID);
			AntiFeedManager.getInstance().clear(AntiFeedManager.L2EVENT_ID);
		}

		_lastDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
		ALL.clear(); RADIANT.clear(); DIRE.clear();

		startQuestTimer("StartFight", (long) C_REG_TIME * 60_000, null, null);
		if (C_REG_TIME > 1) startQuestTimer("RegWarning", (long) (C_REG_TIME - 1) * 60_000, null, null);

		Broadcast.toAllOnlinePlayers("=== Faction Battleground ===");
		Broadcast.toAllOnlinePlayers("Registro abierto por " + C_REG_TIME + " min — habla con el NPC en Giran.");
		Broadcast.toAllOnlinePlayers(C_RADIANT_NAME + " (Azul) vs " + C_DIRE_NAME + " (Rojo) — Pagan Temple");
		return true;
	}

	// =========================================================================
	// Inicio del combate
	// =========================================================================
	private void startFight()
	{
		setState(EventState.STARTING);
		ALL.removeIf(p -> p == null || p.isOnlineInt() != 1);
		RADIANT.removeIf(p -> !ALL.contains(p));
		DIRE.removeIf(p -> !ALL.contains(p));

		if (ALL.size() < C_MIN_PL)
		{
			Broadcast.toAllOnlinePlayers("Faction Battleground: Cancelado — jugadores insuficientes (" + ALL.size() + "/" + C_MIN_PL + ").");
			cleanupReg(); resetState();
			startQuestTimer("AutoRestart", 5_000, null, null);
			return;
		}

		_radiantProg = 0; _direProg = 0;
		clearNpcRefs();
		setupTeam(RADIANT, Team.BLUE, C_RADIANT_SPAWN, C_RADIANT_NAME, C_RADIANT_COLOR);
		setupTeam(DIRE,    Team.RED,  C_DIRE_SPAWN,    C_DIRE_NAME,    C_DIRE_COLOR);

		startQuestTimer("5",        1_000, null, null);
		startQuestTimer("4",        2_000, null, null);
		startQuestTimer("3",        3_000, null, null);
		startQuestTimer("2",        4_000, null, null);
		startQuestTimer("1",        5_000, null, null);
		startQuestTimer("SpawnZone",6_000, null, null);
	}

	private void setupTeam(Set<Player> team, Team teamEnum, Location spawn, String name, int color)
	{
		CommandChannel cc = null; Party lastParty = null; int cnt = 0;
		for (Player pl : team)
		{
			pl.setOnEvent(true); pl.setRegisteredOnEvent(false);
			pl.setTeam(teamEnum);
			pl.getAppearance().setNameColor(color);
			pl.getAppearance().setTitleColor(color);
			pl.broadcastUserInfo();
			pl.leaveParty();
			pl.teleToLocation(spawn, 100);
			addDeathL(pl);
			cnt++;
			if (cnt == 1)
			{
				lastParty = new Party(pl, PartyDistributionType.FINDERS_KEEPERS);
				pl.joinParty(lastParty);
				if (team.size() > 7) { if (cc == null) cc = new CommandChannel(pl); else cc.addParty(lastParty); }
			}
			else pl.joinParty(lastParty);
			if (cnt == 7) cnt = 0;
			pl.sendMessage("Bando: " + name + " | Destruye las torres enemigas desde el Tier 1. Sigue el marcador del minimapa.");
		}
	}

	// =========================================================================
	// Spawneo de zona
	// =========================================================================
	private void spawnZoneElements()
	{
		setState(EventState.ACTIVE);

		for (int i = 0; i < 3; i++)
		{
			_rTowers[i] = spawnStructure(C_TOWER_NPC, C_RADIANT_TOWERS[i], C_TOWER_HP, "Radiant", i);
			_dTowers[i] = spawnStructure(C_TOWER_NPC, C_DIRE_TOWERS[i],    C_TOWER_HP, "Dire",    i);
		}
		_rFlag = spawnStructure(C_FLAG_NPC, C_RADIANT_FLAG, C_FLAG_HP, "Radiant", -1);
		_dFlag = spawnStructure(C_FLAG_NPC, C_DIRE_FLAG,    C_FLAG_HP, "Dire",    -1);

		// Solo el Tier 0 (Tier 1) de cada bando es vulnerable al inicio
		refreshInvul();

		openDoors();
		spawnGuards();
		startQuestTimer("GuardAI", 5_000, null, null);
		sendArrows();

		screenAll("!!! FACTION BATTLEGROUND — INICIO !!!", 7);
		Broadcast.toAllOnlinePlayers("Faction Battleground: Combate iniciado! Objetivo: Torre Tier 1 enemiga.");
	}

	private Npc spawnStructure(int npcId, Location loc, int hp, String side, int tier)
	{
		final int groundZ = GeoEngine.getInstance().getHeight(loc.getX(), loc.getY(), loc.getZ());
		final Npc n = addSpawn(npcId, new Location(loc.getX(), loc.getY(), groundZ, loc.getHeading()), false, 0);
		if (n != null)
		{
			n.setCurrentHp(hp);
			n.setInvul(true);
			n.getVariables().set("Side", side);
			n.getVariables().set("Tier", tier); // -1 = flag
		}
		return n;
	}

	// =========================================================================
	// onAttack — sistema de sanciones por fuego amigo
	// =========================================================================
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (_state != EventState.ACTIVE || damage <= 0) return;

		if (isFriendlyNpc(attacker, npc))
		{
			applyFriendlyFirePenalty(attacker);
			return;
		}

		// Backup: si algun danio atraveso la invulnerabilidad en una torre fuera de orden, curar
		if (npc.getId() == C_TOWER_NPC)
		{
			final String side = npc.getVariables().getString("Side", "");
			final int tier    = npc.getVariables().getInt("Tier", -1);
			if ("Dire".equals(side) && RADIANT.contains(attacker) && _radiantProg < 3 && tier != _radiantProg)
				npc.setCurrentHp(npc.getMaxHp());
			else if ("Radiant".equals(side) && DIRE.contains(attacker) && _direProg < 3 && tier != _direProg)
				npc.setCurrentHp(npc.getMaxHp());
		}
	}

	private boolean isFriendlyNpc(Player player, Npc npc)
	{
		final int id = npc.getId();
		if (id == C_RADIANT_GUARD) return RADIANT.contains(player);
		if (id == C_DIRE_GUARD)    return DIRE.contains(player);
		final String side = npc.getVariables().getString("Side", "");
		if ("Radiant".equals(side)) return RADIANT.contains(player);
		if ("Dire".equals(side))    return DIRE.contains(player);
		return false;
	}

	/**
	 * Aplica petrificacion perfecta (estilo Dance of Medusa) al jugador.
	 * El efecto Petrification setea PARALYZED|INVUL y bloquea TODA accion:
	 * no puede moverse, atacar, usar skills ni items hasta que expire.
	 */
	private void petrify(Player player, int skillId)
	{
		if (player == null) return;
		// Limpia cualquier petrificacion previa para evitar solapamientos.
		player.stopSkillEffects(SkillFinishType.REMOVED, PETRIFY_10S);
		player.stopSkillEffects(SkillFinishType.REMOVED, PETRIFY_60S);
		player.stopSkillEffects(SkillFinishType.REMOVED, PETRIFY_PERM);
		player.abortAttack();
		player.abortCast();
		final Skill sk = SkillData.getInstance().getSkill(skillId, 1);
		if (sk != null) sk.applyEffects(player, player);
	}

	/** Remueve cualquier petrificacion del Battleground aplicada al jugador. */
	private void unpetrify(Player player)
	{
		if (player == null) return;
		player.stopSkillEffects(SkillFinishType.REMOVED, PETRIFY_10S);
		player.stopSkillEffects(SkillFinishType.REMOVED, PETRIFY_60S);
		player.stopSkillEffects(SkillFinishType.REMOVED, PETRIFY_PERM);
	}

	private void applyFriendlyFirePenalty(Player player)
	{
		final int strikes = _friendlyFireStrikes.merge(player.getObjectId(), 1, Integer::sum);
		if (strikes == 1)
		{
			petrify(player, PETRIFY_10S);
			player.sendPacket(new ExShowScreenMessage("ADVERTENCIA: Fuego amigo! Petrificado 10 segundos.", ExShowScreenMessage.TOP_CENTER, 10_000, 0, true, false));

			final StringBuilder sb = new StringBuilder();
			sb.append("<html><body>");
			sb.append("<center><font color=\"LEVEL\">Faction Battleground</font></center><br>");
			sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
			sb.append("<br><center><font color=\"FF6347\">ADVERTENCIA - FUEGO AMIGO</font></center><br><br>");
			sb.append("Atacaste a un NPC de tu propio bando.<br><br>");
			sb.append("<font color=\"FFAA00\">Petrificado durante 10 segundos.</font><br><br>");
			sb.append("<font color=\"FF4444\">Reincidencia: 1 minuto de petrificacion.</font><br>");
			sb.append("<font color=\"FF4444\">3ra infraccion: petrificado hasta el fin del evento.</font><br><br>");
			sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\">");
			sb.append("</body></html>");
			final NpcHtmlMessage html = new NpcHtmlMessage();
			html.setHtml(sb.toString());
			player.sendPacket(html);
		}
		else if (strikes == 2)
		{
			petrify(player, PETRIFY_60S);
			player.sendPacket(new ExShowScreenMessage("FUEGO AMIGO REITERADO: Petrificado 1 minuto!", ExShowScreenMessage.TOP_CENTER, 10_000, 0, true, false));

			final StringBuilder sb = new StringBuilder();
			sb.append("<html><body>");
			sb.append("<center><font color=\"LEVEL\">Faction Battleground</font></center><br>");
			sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
			sb.append("<br><center><font color=\"FF4444\">SANCION - FUEGO AMIGO REITERADO</font></center><br><br>");
			sb.append("Volviste a atacar NPCs de tu propio bando.<br><br>");
			sb.append("<font color=\"FF4444\">Petrificado durante 1 minuto.</font><br><br>");
			sb.append("<font color=\"FF4444\">Una infraccion mas y seras petrificado hasta el fin del evento!</font><br><br>");
			sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\">");
			sb.append("</body></html>");
			final NpcHtmlMessage html = new NpcHtmlMessage();
			html.setHtml(sb.toString());
			player.sendPacket(html);
		}
		else
		{
			_permanentPetrify.add(player.getObjectId());
			petrify(player, PETRIFY_PERM);
			player.sendPacket(new ExShowScreenMessage("SANCION MAXIMA: Petrificado hasta el fin del evento!", ExShowScreenMessage.TOP_CENTER, 10_000, 0, true, false));
			player.sendMessage("Fuego amigo repetido: quedaras petrificado hasta que termine el evento.");
		}
	}

	// =========================================================================
	// onKill
	// =========================================================================
	@Override
	public void onKill(Npc killed, Player killer, boolean isSummon)
	{
		if (_state != EventState.ACTIVE) return;
		final int id = killed.getId();
		if      (id == C_RADIANT_GUARD) { _rGuards.remove(killed); startQuestTimer("RespawnRGuard", (long) C_GUARD_RESPAWN * 1_000, null, null); }
		else if (id == C_DIRE_GUARD)    { _dGuards.remove(killed); startQuestTimer("RespawnDGuard", (long) C_GUARD_RESPAWN * 1_000, null, null); }
		else if (id == C_TOWER_NPC)     onTowerKill(killed);
		else if (id == C_FLAG_NPC)      onFlagKill(killed);
	}

	private void onTowerKill(Npc tower)
	{
		final String side = tower.getVariables().getString("Side", "");
		final int tier    = tower.getVariables().getInt("Tier", 0);

		if ("Radiant".equals(side))
		{
			_direProg++;
			announce("[" + C_DIRE_NAME + "] destruyo Torre " + C_RADIANT_NAME + " Tier " + (tier + 1) + "!");
			if (_direProg >= 3) { if (_rFlag != null) _rFlag.setInvul(false); announce("Bandera " + C_RADIANT_NAME + " EXPUESTA!"); sendArrowsTeam(DIRE, C_RADIANT_FLAG, "Bandera " + C_RADIANT_NAME); }
			else { refreshInvul(); sendArrows(); }
		}
		else if ("Dire".equals(side))
		{
			_radiantProg++;
			announce("[" + C_RADIANT_NAME + "] destruyo Torre " + C_DIRE_NAME + " Tier " + (tier + 1) + "!");
			if (_radiantProg >= 3) { if (_dFlag != null) _dFlag.setInvul(false); announce("Bandera " + C_DIRE_NAME + " EXPUESTA!"); sendArrowsTeam(RADIANT, C_DIRE_FLAG, "Bandera " + C_DIRE_NAME); }
			else { refreshInvul(); sendArrows(); }
		}

		redirectGuards();
	}

	private void onFlagKill(Npc flag)
	{
		final String side     = flag.getVariables().getString("Side", "");
		final boolean radiantFlagDied = "Radiant".equals(side);

		final Set<Player> winners = radiantFlagDied ? DIRE    : RADIANT;
		final Set<Player> losers  = radiantFlagDied ? RADIANT : DIRE;
		final String wName        = radiantFlagDied ? C_DIRE_NAME    : C_RADIANT_NAME;
		final String lName        = radiantFlagDied ? C_RADIANT_NAME : C_DIRE_NAME;

		screenAll("!!! " + wName.toUpperCase() + " GANO EL EVENTO !!!", 10);
		Broadcast.toAllOnlinePlayers("Faction Battleground: " + wName + " destruyo la Bandera de " + lName + " — VICTORIA!");

		final Skill fw = CommonSkill.FIREWORK.getSkill();
		for (Player pl : winners)
		{
			if (pl == null || !pl.isOnline()) continue;
			pl.broadcastPacket(new MagicSkillUse(pl, pl, fw.getId(), fw.getLevel(), fw.getHitTime(), fw.getReuseDelay()));
			pl.broadcastSocialAction(3);
			giveItems(pl, C_WIN_REWARD);
			_pendingReward.add(pl.getObjectId());
			pl.sendMessage("GANASTE! Habla con el NPC en Giran para reclamar tu equipo +20. Recompensa: " + C_WIN_REWARD.getCount() + "x.");
		}
		for (Player pl : losers)
		{
			if (pl == null || !pl.isOnline()) continue;
			giveItems(pl, C_LOSE_REWARD);
			pl.sendMessage("Perdiste. Gracias por participar — recompensa de consolacion entregada.");
		}

		startQuestTimer("TeleportOut", 10_000, null, null);
	}

	// =========================================================================
	// Invulnerabilidad de torres
	// =========================================================================
	private void refreshInvul()
	{
		for (int i = 0; i < 3; i++)
		{
			if (_rTowers[i] != null && !_rTowers[i].isDead()) _rTowers[i].setInvul(i != _direProg);
			if (_dTowers[i] != null && !_dTowers[i].isDead()) _dTowers[i].setInvul(i != _radiantProg);
		}
	}

	// =========================================================================
	// Guardias
	// =========================================================================
	private void spawnGuards()
	{
		_rGuards.clear(); _dGuards.clear();
		final Npc rTarget = activeDireStructureForGuard();
		final Npc dTarget = activeRadiantStructureForGuard();
		for (int i = 0; i < C_GUARD_COUNT; i++)
		{
			spawnGuard(C_RADIANT_GUARD, C_RADIANT_FLAG, _rGuards, rTarget);
			spawnGuard(C_DIRE_GUARD,    C_DIRE_FLAG,    _dGuards, dTarget);
		}
	}

	private void spawnGuard(int id, Location spawnLoc, List<Npc> list, Npc initialTarget)
	{
		final Npc g = addSpawn(id, new Location(spawnLoc.getX() + getRandom(-150, 150), spawnLoc.getY() + getRandom(-150, 150), spawnLoc.getZ()), false, 0);
		if (g != null)
		{
			list.add(g);
			// Limpiar aggro inicial y enviar al guardia inmediatamente hacia el enemigo
			if (g instanceof Attackable) ((Attackable) g).clearAggroList();
			if (initialTarget != null && !initialTarget.isDead()) { g.setTarget(initialTarget); g.doAttack(initialTarget); }
		}
	}

	private void redirectGuards()
	{
		redirectList(_rGuards, activeDireStructureForGuard(),    DIRE,    _dGuards, RADIANT);
		redirectList(_dGuards, activeRadiantStructureForGuard(), RADIANT, _rGuards, DIRE);
	}

	private void redirectList(List<Npc> guards, Npc structTarget, Set<Player> enemies, List<Npc> enemyGuards, Set<Player> friendlies)
	{
		for (Npc g : guards)
		{
			if (g == null || g.isDead()) continue;
			// Correccion: si el guardia esta atacando a un aliado, limpiar aggro y redirigir
			if (g.getTarget() != null && g.getTarget().isPlayer() && friendlies.contains(g.getTarget().asPlayer()))
			{
				if (g instanceof Attackable) ((Attackable) g).clearAggroList();
				g.setTarget(null);
			}
			// Prioridad 1: atacar jugador enemigo cercano
			final Player nearestPlayer = findNearestPlayer(g, enemies, 1000);
			if (nearestPlayer != null) { g.setTarget(nearestPlayer); g.doAttack(nearestPlayer); continue; }
			// Prioridad 2: atacar guardia enemigo cercano
			final Npc nearestGuard = findNearestNpc(g, enemyGuards, 1000);
			if (nearestGuard != null) { g.setTarget(nearestGuard); g.doAttack(nearestGuard); continue; }
			// Prioridad 3: atacar estructura activa (Tier3→Tier2→Tier1→flag)
			if (structTarget != null && !structTarget.isDead()) { g.setTarget(structTarget); g.doAttack(structTarget); }
		}
	}

	private Player findNearestPlayer(Npc guard, Set<Player> team, int range)
	{
		Player nearest = null;
		double minDist = Double.MAX_VALUE;
		for (Player pl : team)
		{
			if (pl == null || !pl.isOnline() || pl.isDead()) continue;
			final double d = guard.calculateDistance3D(pl);
			if (d < range && d < minDist) { nearest = pl; minDist = d; }
		}
		return nearest;
	}

	private Npc findNearestNpc(Npc guard, List<Npc> npcList, int range)
	{
		Npc nearest = null;
		double minDist = Double.MAX_VALUE;
		for (Npc n : npcList)
		{
			if (n == null || n.isDead()) continue;
			final double d = guard.calculateDistance3D(n);
			if (d < range && d < minDist) { nearest = n; minDist = d; }
		}
		return nearest;
	}

	// Estructura Dire activa para jugadores Radiant (Tier 1→2→3→flag, por _radiantProg)
	private Npc activeDireStructure()
	{
		if (_radiantProg < 3) { final Npc t = _dTowers[_radiantProg]; return (t != null && !t.isDead()) ? t : null; }
		return (_dFlag != null && !_dFlag.isDead()) ? _dFlag : null;
	}

	// Estructura Radiant activa para jugadores Dire (Tier 1→2→3→flag, por _direProg)
	private Npc activeRadiantStructure()
	{
		if (_direProg < 3) { final Npc t = _rTowers[_direProg]; return (t != null && !t.isDead()) ? t : null; }
		return (_rFlag != null && !_rFlag.isDead()) ? _rFlag : null;
	}

	// Guardias Radiant avanzan desde bandera propia hacia Dire: Tier3→Tier2→Tier1→flag
	private Npc activeDireStructureForGuard()
	{
		for (int i = 2; i >= 0; i--)
			if (_dTowers[i] != null && !_dTowers[i].isDead()) return _dTowers[i];
		return (_dFlag != null && !_dFlag.isDead()) ? _dFlag : null;
	}

	// Guardias Dire avanzan desde bandera propia hacia Radiant: Tier3→Tier2→Tier1→flag
	private Npc activeRadiantStructureForGuard()
	{
		for (int i = 2; i >= 0; i--)
			if (_rTowers[i] != null && !_rTowers[i].isDead()) return _rTowers[i];
		return (_rFlag != null && !_rFlag.isDead()) ? _rFlag : null;
	}

	// =========================================================================
	// Flechas de tutorial (RadarControl)
	// =========================================================================
	private void sendArrows()
	{
		sendArrowsTeam(RADIANT, _radiantProg < 3 ? C_DIRE_TOWERS[_radiantProg]    : C_DIRE_FLAG,
		                        _radiantProg < 3 ? "Torre " + C_DIRE_NAME    + " Tier " + (_radiantProg + 1) : "Bandera " + C_DIRE_NAME    + " — FINAL!");
		sendArrowsTeam(DIRE,    _direProg    < 3 ? C_RADIANT_TOWERS[_direProg]    : C_RADIANT_FLAG,
		                        _direProg    < 3 ? "Torre " + C_RADIANT_NAME + " Tier " + (_direProg + 1)    : "Bandera " + C_RADIANT_NAME + " — FINAL!");
	}

	private void sendArrowsTeam(Set<Player> team, Location target, String label)
	{
		if (target == null) return;
		for (Player pl : team)
		{
			if (pl == null || !pl.isOnline()) continue;
			pl.sendPacket(new RadarControl(2, 1, target.getX(), target.getY(), target.getZ()));
			pl.sendPacket(new RadarControl(0, 1, target.getX(), target.getY(), target.getZ()));
			pl.sendPacket(new ExShowScreenMessage("Objetivo: " + label + " | Sigue el marcador en el minimapa.", ExShowScreenMessage.TOP_CENTER, 7_000, 0, true, false));
		}
	}

	// =========================================================================
	// Muerte de jugadores (listener)
	// =========================================================================
	private void onDeath(OnCreatureDeath ev)
	{
		if (!ev.getTarget().isPlayer()) return;
		final Player killed = ev.getTarget().asPlayer();
		if (!ALL.contains(killed)) return;

		// Sin karma dentro del evento
		if (ev.getAttacker().isPlayer())
		{
			final Player killer = ev.getAttacker().asPlayer();
			if (ALL.contains(killer) && killer.getKarma() > 0) killer.setKarma(0);
		}

		// Re-enfocar guardias que estaban atacando al jugador muerto
		if (_state == EventState.ACTIVE) redirectGuards();

		startQuestTimer("Respawn_" + killed.getObjectId(), 10_000, null, killed);
	}

	// =========================================================================
	// Logout (listener)
	// =========================================================================
	private void onLogout(OnPlayerLogout ev)
	{
		final Player pl = ev.getPlayer();
		ALL.remove(pl); RADIANT.remove(pl); DIRE.remove(pl);
	}

	private void addDeathL(Player pl)  { pl.addListener(new ConsumerEventListener(pl, EventType.ON_CREATURE_DEATH, (OnCreatureDeath e) -> onDeath(e), this)); }
	private void addLogoutL(Player pl) { pl.addListener(new ConsumerEventListener(pl, EventType.ON_PLAYER_LOGOUT,  (OnPlayerLogout  e) -> onLogout(e), this)); }
	private void removeL(Player pl)
	{
		pl.getListeners(EventType.ON_PLAYER_LOGOUT).stream().filter(l -> l.getOwner() == this).forEach(AbstractEventListener::unregisterMe);
		pl.getListeners(EventType.ON_CREATURE_DEATH).stream().filter(l -> l.getOwner() == this).forEach(AbstractEventListener::unregisterMe);
	}

	// =========================================================================
	// Teleport fuera y reset (TeleportOut timer)
	// =========================================================================
	private void doTeleportOut()
	{
		for (Player pl : ALL)
		{
			if (pl == null || !pl.isOnline()) continue;
			removeL(pl);
			_permanentPetrify.remove(pl.getObjectId());
			unpetrify(pl);
			pl.setTeam(Team.NONE); pl.setOnEvent(false); pl.setRegisteredOnEvent(false);
			pl.setInvul(false); pl.setImmobilized(false); pl.enableAllSkills();
			pl.getAppearance().setNameColor(0xFFFFFF);
			pl.getAppearance().setTitleColor(0xFFFF77);
			pl.broadcastUserInfo();
			pl.leaveParty();
			pl.teleToLocation(C_GIRAN);
			pl.sendPacket(new RadarControl(2, 1, 0, 0, 0));
		}
		cleanupNpcs(); resetState();
		Broadcast.toAllOnlinePlayers("Faction Battleground: Evento terminado. Proximo registro en " + C_REG_TIME + " min.");
		startQuestTimer("AutoRestart", 5_000, null, null);
	}

	// =========================================================================
	// eventStop — comando GM
	// =========================================================================
	@Override
	public boolean eventStop()
	{
		cancelTimers();
		for (Player pl : ALL)
		{
			if (pl == null) continue;
			removeL(pl); _permanentPetrify.remove(pl.getObjectId()); unpetrify(pl);
			pl.setTeam(Team.NONE); pl.setOnEvent(false); pl.setRegisteredOnEvent(false);
			pl.setInvul(false); pl.setImmobilized(false); pl.enableAllSkills();
			pl.getAppearance().setNameColor(0xFFFFFF); pl.getAppearance().setTitleColor(0xFFFF77);
			pl.broadcastUserInfo(); pl.leaveParty(); pl.teleToLocation(C_GIRAN);
			pl.sendPacket(new RadarControl(2, 1, 0, 0, 0));
		}
		cleanupNpcs(); resetState();
		Broadcast.toAllOnlinePlayers("Faction Battleground: Detenido por un GM.");
		return true;
	}

	@Override public boolean eventBypass(Player player, String bypass) { return false; }

	// =========================================================================
	// Puertas
	// =========================================================================
	private void openDoors()
	{
		for (int id : C_DOORS)
		{
			if (id <= 0) continue;
			final Door door = DoorData.getInstance().getDoor(id);
			if (door == null) { LOGGER.warning("FactionBattleground: openDoors — ID " + id + " no encontrado en DoorData."); continue; }
			door.openMe();
		}
	}

	private void closeDoors()
	{
		for (int id : C_DOORS)
		{
			if (id <= 0) continue;
			final Door door = DoorData.getInstance().getDoor(id);
			if (door == null) continue;
			door.closeMe();
		}
	}

	// =========================================================================
	// Limpieza
	// =========================================================================
	private void cleanupNpcs()
	{
		for (Npc t : _rTowers) despawn(t);
		for (Npc t : _dTowers) despawn(t);
		despawn(_rFlag); despawn(_dFlag);
		_rGuards.forEach(this::despawn); _rGuards.clear();
		_dGuards.forEach(this::despawn); _dGuards.clear();
		clearNpcRefs(); closeDoors();
	}

	private void despawn(Npc n) { if (n != null && !n.isDecayed()) n.deleteMe(); }
	private void clearNpcRefs()
	{
		for (int i = 0; i < 3; i++) { _rTowers[i] = null; _dTowers[i] = null; }
		_rFlag = null; _dFlag = null;
	}

	private void cleanupReg()
	{
		ALL.forEach(pl -> { removeL(pl); pl.setRegisteredOnEvent(false); pl.setOnEvent(false); });
		ALL.clear(); RADIANT.clear(); DIRE.clear();
	}

	private void resetState() { _radiantProg = 0; _direProg = 0; setState(EventState.INACTIVE); }
	private void cancelTimers() { getQuestTimers().values().forEach(ts -> ts.forEach(QuestTimer::cancel)); }

	// =========================================================================
	// Siege check
	// =========================================================================
	private boolean siegeActive()
	{
		try { for (org.l2jmobius.gameserver.model.siege.Castle c : CastleManager.getInstance().getCastles()) if (c.getSiege().isInProgress()) return true; }
		catch (Exception ignored) {}
		return false;
	}

	// =========================================================================
	// Mensajes
	// =========================================================================
	private void screenAll(String msg, int secs)
	{
		for (Player pl : ALL) if (pl != null && pl.isOnline())
			pl.sendPacket(new ExShowScreenMessage(msg, ExShowScreenMessage.TOP_CENTER, secs * 1_000, 0, true, false));
	}

	private void announce(String msg) { screenAll(msg, 6); Broadcast.toAllOnlinePlayers("Faction Battleground: " + msg); }

	// =========================================================================
	// Estado
	// =========================================================================
	private static synchronized void setState(EventState s) { _state = s; }

	// =========================================================================
	// Entry point
	// =========================================================================
	public static void main(String[] args) { new FactionBattleground(); }
}
