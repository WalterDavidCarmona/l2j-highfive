/*
 * Torneo 1v1 estilo Copa del Mundo
 * 16 jugadores -> Ronda de 16 -> Cuartos -> Semis -> Final
 * Ganador: Hero 7 dias + Premium 30 dias
 */
package custom.events.Tournament1v1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.commons.time.SchedulingPattern;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.PremiumManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Team;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.model.script.Event;
import org.l2jmobius.gameserver.model.script.QuestTimer;
import org.l2jmobius.gameserver.model.zone.ZoneForm;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * Torneo 1v1 - Copa del Mundo
 * Activado por admin desde panel (//admin startEvent Tournament1v1)
 */
public class Tournament1v1 extends Event
{
	// NPC - mismo manager que Deathmatch
	private static final int MANAGER_NPC_ID = 70011;

	// Arena: Colosseum
	private static final int      INSTANCE_ID      = 3049;
	private static final int      BLUE_DOOR_ID     = 24190002;
	private static final int      RED_DOOR_ID      = 24190003;
	private static final Location MANAGER_SPAWN    = new Location(83472, 149090, -3400, 32938);
	private static final Location LOBBY_LOC        = new Location(148497, 46712, -3400, 0);
	private static final Location GIRAN_LOC        = new Location(82698, 148638, -3473, 0);
	private static ZoneForm BLUE_SPAWN_ZONE = null;
	private static ZoneForm RED_SPAWN_ZONE  = null;

	// Configuracion
	private static final int REGISTRATION_TIME = 10; // minutos de inscripcion
	private static final int WAIT_TIME         = 20; // segundos de cuenta regresiva antes de pelea
	private static final int CONFIRM_TIME      = 60; // segundos para confirmar disponibilidad
	private static final int FIGHT_TIME        = 5;  // minutos maximos por combate
	private static final int BETWEEN_FIGHTS    = 30; // segundos entre combates
	private static final int MAX_PLAYERS       = 16;
	private static final int MIN_LEVEL         = 85;
	private static final int HERO_DAYS         = 7;
	private static final int PREMIUM_DAYS      = 30;

	// Hero persistence path
	private static final String HERO_FILE = "./config/Custom/TournamentHero.properties";

	// -------------------------------------------------------------------------
	// Estado en tiempo de ejecucion
	// -------------------------------------------------------------------------
	private static final Set<Player> REGISTERED     = ConcurrentHashMap.newKeySet();
	private static InstanceWorld     TOURNAMENT_WORLD = null;
	private static Npc               MANAGER_NPC      = null;
	private static boolean           EVENT_ACTIVE     = false;

	// Bracket
	private static List<Player> CURRENT_ROUND = new ArrayList<>();
	private static List<Player> ROUND_WINNERS = new ArrayList<>();
	private static int          MATCH_INDEX   = 0;
	private static Player       FIGHTER_BLUE      = null;
	private static Player       FIGHTER_RED       = null;
	private static boolean      FIGHT_ONGOING     = false;
	private static volatile boolean BLUE_CONFIRMED = false;
	private static volatile boolean RED_CONFIRMED  = false;

	// -------------------------------------------------------------------------

	private Tournament1v1()
	{
		addTalkId(MANAGER_NPC_ID);
		addFirstTalkId(MANAGER_NPC_ID);

		// Resolver zonas de spawn del Colosseum
		try
		{
			BLUE_SPAWN_ZONE = ZoneManager.getInstance().getZoneByName("colosseum_battle1").getZone();
			RED_SPAWN_ZONE  = ZoneManager.getInstance().getZoneByName("colosseum_battle2").getZone();
			LOGGER.info("Tournament1v1: Zonas cargadas correctamente.");
		}
		catch (Exception e)
		{
			LOGGER.warning("Tournament1v1: Error al cargar zonas de spawn: " + e.getMessage());
		}

		// Listener global para restaurar hero al login
		Containers.Players().addListener(new ConsumerEventListener(
			Containers.Players(),
			EventType.ON_PLAYER_LOGIN,
			(OnPlayerLogin ev) -> onPlayerLogin(ev),
			this));

		loadConfig();
		checkHeroPersistence();
	}

	// -------------------------------------------------------------------------
	// Configuracion / schedule
	// -------------------------------------------------------------------------
	private void loadConfig()
	{
		new IXmlReader()
		{
			@Override
			public void load()
			{
				parseDatapackFile("data/scripts/custom/events/Tournament1v1/config.xml");
			}

			@Override
			public void parseDocument(Document document, File file)
			{
				final AtomicInteger count = new AtomicInteger(0);
				for (Node node = document.getDocumentElement().getFirstChild(); node != null; node = node.getNextSibling())
				{
					if ("schedule".equals(node.getNodeName()))
					{
						final StatSet att    = new StatSet(parseAttributes(node));
						final String pattern = att.getString("pattern");
						final SchedulingPattern sp = new SchedulingPattern(pattern);
						final StatSet params = new StatSet();
						params.set("SchedulingPattern", pattern);
						final long delay = sp.getDelayToNextFromNow();
						getTimers().addTimer("Schedule" + count.incrementAndGet(), params, delay + 5000, null, null);
						LOGGER.info("Tournament1v1 programado en " + TimeUtil.getDateTimeString(System.currentTimeMillis() + delay));
					}
				}
			}
		}.load();
	}

	// -------------------------------------------------------------------------
	// Hero persistence
	// -------------------------------------------------------------------------
	private void checkHeroPersistence()
	{
		final File f = new File(HERO_FILE);
		if (!f.exists())
		{
			return;
		}
		try (FileInputStream fis = new FileInputStream(f))
		{
			final Properties p = new Properties();
			p.load(fis);
			final String objId  = p.getProperty("heroObjectId");
			final String expiry = p.getProperty("heroExpiry");
			if ((objId == null) || (expiry == null))
			{
				return;
			}
			final long expiryMs = Long.parseLong(expiry);
			final long now = System.currentTimeMillis();
			if (now >= expiryMs)
			{
				f.delete();
				return;
			}
			final int heroObjId = Integer.parseInt(objId);
			ThreadPool.schedule(() -> removeHeroByObjectId(heroObjId), expiryMs - now);
			LOGGER.info("Tournament1v1: Hero activo para objectId=" + heroObjId + ", expira " + TimeUtil.getDateTimeString(expiryMs));
		}
		catch (Exception e)
		{
			LOGGER.warning("Tournament1v1: Error leyendo hero persistence: " + e.getMessage());
		}
	}

	private void saveHeroPersistence(Player winner)
	{
		try
		{
			final long expiryMs = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(HERO_DAYS);
			final Properties p = new Properties();
			p.setProperty("heroObjectId",    String.valueOf(winner.getObjectId()));
			p.setProperty("heroAccountName", winner.getAccountName());
			p.setProperty("heroCharName",    winner.getName());
			p.setProperty("heroExpiry",      String.valueOf(expiryMs));
			new File("./config/Custom").mkdirs();
			try (FileOutputStream fos = new FileOutputStream(HERO_FILE))
			{
				p.store(fos, "Tournament1v1 hero persistence - no editar manualmente");
			}
			ThreadPool.schedule(() -> removeHeroByObjectId(winner.getObjectId()), TimeUnit.DAYS.toMillis(HERO_DAYS));
		}
		catch (Exception e)
		{
			LOGGER.warning("Tournament1v1: Error guardando hero persistence: " + e.getMessage());
		}
	}

	private void removeHeroByObjectId(int objectId)
	{
		new File(HERO_FILE).delete();
		for (Player online : World.getInstance().getPlayers())
		{
			if (online.getObjectId() == objectId)
			{
				online.setHero(false);
				online.broadcastUserInfo();
				online.sendMessage("[Torneo] Tu status de Hero ha expirado.");
				return;
			}
		}
		// Si esta offline, onPlayerLogin no restaurara el hero porque el archivo fue borrado
	}

	// -------------------------------------------------------------------------
	// Timer central
	// -------------------------------------------------------------------------
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		// Schedule cron
		if (event.startsWith("Schedule"))
		{
			eventStart(null);
			final SchedulingPattern sp = new SchedulingPattern(params.getString("SchedulingPattern"));
			getTimers().addTimer(event, params, sp.getDelayToNextFromNow() + 5000, null, null);
			return;
		}

		// Advertencias de registro (tiempo de inscripcion abierta)
		if (event.startsWith("RegistrationWarn"))
		{
			final int min = params.getInt("minutes");
			Broadcast.toAllOnlinePlayers("Torneo 1v1: Quedan " + min + " minuto(s) para cerrar inscripciones. (" + REGISTERED.size() + "/" + MAX_PLAYERS + ")");
			return;
		}

		// Advertencias de cuenta regresiva post-cupos-llenos
		if (event.startsWith("FullWarn_"))
		{
			final int min = params.getInt("minutes");
			Broadcast.toAllOnlinePlayers("Torneo 1v1: CUPOS COMPLETOS! El torneo inicia en " + min + " minuto(s)!");
			broadcastToRegistered("El torneo inicia en " + min + " minuto(s)! Prepara tu equipo.");
			return;
		}

		// Cuenta regresiva de combate
		if (event.startsWith("Countdown_"))
		{
			final int sec = params.getInt("sec");
			broadcastToFighters(sec + "...", 3);
			return;
		}

		switch (event)
		{
			case "TeleportToArena":
				onTeleportToArena();
				break;
			case "StartFight":
				onStartFight();
				break;
			case "FightTimeout":
				onFightTimeout();
				break;
			case "NextMatch":
				startNextMatch();
				break;
			case "ConfirmTimeout":
				onConfirmTimeout();
				break;
			case "EndTournament":
				cleanupEvent(true);
				break;
		}
	}

	// -------------------------------------------------------------------------
	// NPC
	// -------------------------------------------------------------------------
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (!EVENT_ACTIVE)
		{
			return null;
		}
		// Panel de administrador
		if (player.isGM())
		{
			showAdminPanel(player, npc);
			return null;
		}
		if (REGISTERED.contains(player))
		{
			sendManagerHtml(player, npc, "manager-cancel.html");
			return null;
		}
		sendManagerHtml(player, npc, "manager-register.html");
		return null;
	}

	private void showAdminPanel(Player gm, Npc npc)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<center><br>");
		sb.append("<font name=\"hs15\" color=\"CDB67F\">Torneo 1v1 - Panel Admin</font><br1>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
		sb.append("<table width=260 border=0>");
		sb.append("<tr><td align=center><font color=\"FFFFFF\">Inscritos: <font color=\"LEVEL\">")
			.append(REGISTERED.size()).append(" / ").append(MAX_PLAYERS).append("</font></font></td></tr>");
		if (!CURRENT_ROUND.isEmpty())
		{
			sb.append("<tr><td align=center><font color=\"FFAA00\">Ronda en curso: ")
				.append(getRoundName(CURRENT_ROUND.size())).append("</font></td></tr>");
		}
		sb.append("</table><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=240 height=1><br><br>");

		// Botones de accion
		sb.append("<table width=260 border=0><tr>");
		sb.append("<td align=center><button value=\"Forzar Inicio\" action=\"bypass -h Script Tournament1v1 AdminForceStart\"")
			.append(" width=120 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/></td>");
		sb.append("<td align=center><button value=\"Cancelar Evento\" action=\"bypass -h Script Tournament1v1 AdminCancelEvent\"")
			.append(" width=120 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/></td>");
		sb.append("</tr></table><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=240 height=1><br>");

		// Lista de inscritos con boton de quitar
		if (!REGISTERED.isEmpty())
		{
			sb.append("<br><font color=\"CDB67F\">Jugadores inscritos:</font><br1>");
			sb.append("<table width=260 border=0>");
			for (Player p : REGISTERED)
			{
				sb.append("<tr>")
					.append("<td width=170><font color=\"FFFFFF\">").append(p.getName()).append("</font></td>")
					.append("<td width=90><button value=\"Quitar\" action=\"bypass -h Script Tournament1v1 AdminRemove_")
					.append(p.getObjectId()).append("\"")
					.append(" width=80 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/></td>")
					.append("</tr>");
			}
			sb.append("</table>");
		}
		else
		{
			sb.append("<br><font color=\"B09878\">Sin inscritos aun.</font>");
		}

		sb.append("<br></center></body></html>");
		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setHtml(sb.toString());
		gm.sendPacket(msg);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!EVENT_ACTIVE)
		{
			return null;
		}

		// --- Comandos admin ---
		if (event.startsWith("AdminRemove_") && player.isGM())
		{
			final int objId = Integer.parseInt(event.substring(12));
			removeRegisteredById(objId, player);
			showAdminPanel(player, npc);
			return null;
		}

		if ("AdminForceStart".equals(event) && player.isGM())
		{
			cancelQuestTimer("TeleportToArena", null, null);
			for (int min : new int[]{ 3, 1 })
			{
				cancelQuestTimer("FullWarn_" + min, null, null);
			}
			getTimers().addTimer("TeleportToArena", new StatSet(), 10000L, null, null);
			Broadcast.toAllOnlinePlayers("Torneo 1v1: Un GM fuerza el inicio en 10 segundos!");
			broadcastToRegistered("El torneo inicia en 10 segundos! Preparate!");
			player.sendMessage("[Torneo] Inicio forzado.");
			return null;
		}

		if ("AdminCancelEvent".equals(event) && player.isGM())
		{
			eventStop();
			return null;
		}

		// --- Confirmacion de combate ---
		if ("ConfirmFight".equals(event))
		{
			if ((FIGHTER_BLUE == null) || (FIGHTER_RED == null))
			{
				return null;
			}
			if (player == FIGHTER_BLUE && !BLUE_CONFIRMED)
			{
				BLUE_CONFIRMED = true;
				player.sendMessage("[Torneo] Confirmado! Esperando confirmacion del rival...");
			}
			else if (player == FIGHTER_RED && !RED_CONFIRMED)
			{
				RED_CONFIRMED = true;
				player.sendMessage("[Torneo] Confirmado! Esperando confirmacion del rival...");
			}
			// Si ambos confirmaron, iniciar combate
			if (BLUE_CONFIRMED && RED_CONFIRMED)
			{
				startConfirmedFight();
			}
			return null;
		}

		// --- Comandos de jugador ---
		switch (event)
		{
			case "Participate":
			{
				if (!CURRENT_ROUND.isEmpty() || FIGHT_ONGOING)
				{
					player.sendMessage("El torneo ya comenzo. No es posible inscribirse.");
					break;
				}
				if (canRegister(player))
				{
					final boolean ipOk = (DualboxCheckConfig.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP == 0) ||
						AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.L2EVENT_ID, player, DualboxCheckConfig.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP);
					if (ipOk)
					{
						REGISTERED.add(player);
						player.setRegisteredOnEvent(true);
						addLogoutListener(player);
						sendManagerHtml(player, npc, "registration-success.html");
						Broadcast.toAllOnlinePlayers(player.getName() + " se inscribio al Torneo 1v1. (" + REGISTERED.size() + "/" + MAX_PLAYERS + ")");

						if (REGISTERED.size() >= MAX_PLAYERS)
						{
							startFullSlotsCountdown();
						}
					}
					else
					{
						sendManagerHtml(player, npc, "registration-ip.html");
					}
				}
				else
				{
					sendManagerHtml(player, npc, "registration-failed.html");
				}
				break;
			}
			case "CancelParticipation":
			{
				if (player.isOnEvent())
				{
					player.sendMessage("No puedes cancelar mientras el torneo esta en curso.");
					break;
				}
				if (DualboxCheckConfig.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP > 0)
				{
					AntiFeedManager.getInstance().removePlayer(AntiFeedManager.L2EVENT_ID, player);
				}
				REGISTERED.remove(player);
				removeListeners(player);
				player.setRegisteredOnEvent(false);
				sendManagerHtml(player, npc, "registration-canceled.html");
				Broadcast.toAllOnlinePlayers("Torneo 1v1: " + player.getName() + " cancelo su inscripcion. (" + REGISTERED.size() + "/" + MAX_PLAYERS + ")");
				break;
			}
		}
		return null;
	}

	// Cuando los cupos se llenan: countdown de 5 minutos antes de iniciar
	private void startFullSlotsCountdown()
	{
		// Cancelar timers de inscripcion anteriores
		cancelQuestTimer("TeleportToArena", null, null);
		for (int min : new int[]{ 5, 3, 1 })
		{
			cancelQuestTimer("RegistrationWarn_" + min, null, null);
		}

		final long fiveMin = 5 * 60000L;

		// Programar inicio en 5 minutos
		getTimers().addTimer("TeleportToArena", new StatSet(), fiveMin, null, null);

		// Advertencias: 3 min y 1 min antes del inicio
		final StatSet w3 = new StatSet();
		w3.set("minutes", 3);
		getTimers().addTimer("FullWarn_3", w3, 2 * 60000L, null, null); // a los 2 min (quedan 3)

		final StatSet w1 = new StatSet();
		w1.set("minutes", 1);
		getTimers().addTimer("FullWarn_1", w1, 4 * 60000L, null, null); // a los 4 min (queda 1)

		// Anuncio inmediato
		Broadcast.toAllOnlinePlayers("==============================");
		Broadcast.toAllOnlinePlayers("  TORNEO 1v1: CUPOS COMPLETOS!");
		Broadcast.toAllOnlinePlayers("  El torneo inicia en 5 minutos.");
		Broadcast.toAllOnlinePlayers("  Ultima oportunidad para salir: habla con el NPC Event Manager.");
		Broadcast.toAllOnlinePlayers("==============================");
		broadcastToRegistered("Cupos llenos! Tienes 5 minutos para cancelar tu inscripcion si lo deseas.");
	}

	// -------------------------------------------------------------------------
	// Logica del torneo
	// -------------------------------------------------------------------------
	private void onTeleportToArena()
	{
		REGISTERED.removeIf(p -> (p == null) || (p.isOnlineInt() != 1));

		if (REGISTERED.size() < 2)
		{
			Broadcast.toAllOnlinePlayers("Torneo 1v1: Cancelado - participantes insuficientes.");
			cleanupEvent(false);
			return;
		}

		// Truncar a MAX_PLAYERS si hay mas
		final List<Player> list = new ArrayList<>(REGISTERED);
		if (list.size() > MAX_PLAYERS)
		{
			list.subList(MAX_PLAYERS, list.size()).clear();
		}
		Collections.shuffle(list);

		// Crear instancia
		final InstanceWorld world = new InstanceWorld();
		world.setInstance(InstanceManager.getInstance().createDynamicInstance(INSTANCE_ID));
		InstanceManager.getInstance().addWorld(world);
		TOURNAMENT_WORLD = world;
		TOURNAMENT_WORLD.getDoors().forEach(Door::closeMe);

		// Inicializar bracket
		CURRENT_ROUND = list;
		ROUND_WINNERS.clear();
		MATCH_INDEX = 0;

		// Teleportar todos al lobby
		for (Player p : CURRENT_ROUND)
		{
			preparePlayer(p);
			p.teleToLocation(LOBBY_LOC, TOURNAMENT_WORLD.getInstanceId(), 50);
			p.sendMessage("[Torneo] Bienvenido al Torneo 1v1!");
			p.sendMessage("[Torneo] Puedes usar tus buffs y pociones en el area de espera.");
			p.sendMessage("[Torneo] Al ser llamado al combate se restaurara tu HP/MP/CP.");
		}

		// Anunciar bracket
		announceBracket();

		// Programar primer combate
		getTimers().addTimer("NextMatch", new StatSet(), (long) WAIT_TIME * 1000, null, null);
	}

	private void announceBracket()
	{
		final String roundName = getRoundName(CURRENT_ROUND.size());
		final StringBuilder sb = new StringBuilder("=== TORNEO 1v1 - ").append(roundName).append(" ===\n");
		for (int i = 0; i + 1 < CURRENT_ROUND.size(); i += 2)
		{
			sb.append("  Partido ").append((i / 2) + 1).append(": ")
				.append(CURRENT_ROUND.get(i).getName())
				.append(" vs ")
				.append(CURRENT_ROUND.get(i + 1).getName())
				.append("\n");
		}
		if ((CURRENT_ROUND.size() % 2) != 0)
		{
			sb.append("  ").append(CURRENT_ROUND.get(CURRENT_ROUND.size() - 1).getName()).append(" -> Bye (pasa automatico)\n");
		}
		final String msg = sb.toString();
		Broadcast.toAllOnlinePlayers(msg);
	}

	private void startNextMatch()
	{
		final int totalInRound = CURRENT_ROUND.size();

		// Verificar si terminaron todos los partidos de la ronda
		if ((MATCH_INDEX * 2) >= totalInRound)
		{
			if (ROUND_WINNERS.size() == 1)
			{
				// Campeon
				announceChampion(ROUND_WINNERS.get(0));
				return;
			}
			// Siguiente ronda
			CURRENT_ROUND = new ArrayList<>(ROUND_WINNERS);
			ROUND_WINNERS.clear();
			MATCH_INDEX = 0;

			// Llevar a todos al lobby
			for (Player p : CURRENT_ROUND)
			{
				if ((p != null) && (p.isOnlineInt() == 1) && (TOURNAMENT_WORLD != null))
				{
					p.teleToLocation(LOBBY_LOC, TOURNAMENT_WORLD.getInstanceId(), 50);
				}
			}

			final String nextRoundName = getRoundName(CURRENT_ROUND.size());
			Broadcast.toAllOnlinePlayers("Torneo 1v1: *** " + nextRoundName.toUpperCase() + " ***");
			announceBracket();
			getTimers().addTimer("NextMatch", new StatSet(), 20000L, null, null);
			return;
		}

		final int idx1 = MATCH_INDEX * 2;
		final int idx2 = idx1 + 1;

		final Player p1 = idx1 < CURRENT_ROUND.size() ? CURRENT_ROUND.get(idx1) : null;
		final Player p2 = idx2 < CURRENT_ROUND.size() ? CURRENT_ROUND.get(idx2) : null;

		// Bye: numero impar de jugadores
		if (p2 == null)
		{
			if ((p1 != null) && (p1.isOnlineInt() == 1))
			{
				ROUND_WINNERS.add(p1);
				p1.sendMessage("[Torneo] Avanzas automaticamente (Bye).");
			}
			MATCH_INDEX++;
			getTimers().addTimer("NextMatch", new StatSet(), 5000L, null, null);
			return;
		}

		// Manejar desconectados
		final boolean ok1 = (p1 != null) && (p1.isOnlineInt() == 1);
		final boolean ok2 = (p2 != null) && (p2.isOnlineInt() == 1);

		if (!ok1 && !ok2)
		{
			MATCH_INDEX++;
			getTimers().addTimer("NextMatch", new StatSet(), 3000L, null, null);
			return;
		}
		if (!ok1)
		{
			advanceByWalkover(p2, p1);
			return;
		}
		if (!ok2)
		{
			advanceByWalkover(p1, p2);
			return;
		}

		// Registrar combatientes y pedir confirmacion
		FIGHTER_BLUE   = p1;
		FIGHTER_RED    = p2;
		FIGHT_ONGOING  = false;
		BLUE_CONFIRMED = false;
		RED_CONFIRMED  = false;

		final String matchAnnounce = getRoundName(CURRENT_ROUND.size()) + ": " + p1.getName() + " VS " + p2.getName();
		Broadcast.toAllOnlinePlayers("Torneo 1v1: Proximo combate -> " + matchAnnounce);
		if (TOURNAMENT_WORLD != null)
		{
			TOURNAMENT_WORLD.broadcastPacket(new ExShowScreenMessage(matchAnnounce, ExShowScreenMessage.TOP_CENTER, 5000, 0, true, true));
		}

		// Enviar dialogo de confirmacion a cada combatiente
		sendConfirmationHtml(p1, p2.getName());
		sendConfirmationHtml(p2, p1.getName());

		// Timer de inactividad
		getTimers().addTimer("ConfirmTimeout", new StatSet(), (long) CONFIRM_TIME * 1000, null, null);
	}

	private void sendConfirmationHtml(Player p, String rivalName)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(0);
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center><br>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
		sb.append("<font name=\"hs15\" color=\"CDB67F\">TORNEO 1v1</font><br><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=220 height=1><br><br>");
		sb.append("<font color=\"93FFA8\">Es tu turno de combatir!</font><br><br>");
		sb.append("<table width=220 border=0>");
		sb.append("<tr><td align=center><font color=\"FFFFFF\">Rival: <font color=\"LEVEL\">").append(rivalName).append("</font></font></td></tr>");
		sb.append("<tr><td height=8></td></tr>");
		sb.append("<tr><td align=center><font color=\"FF9900\">Tienes <font color=\"FF6060\">").append(CONFIRM_TIME).append(" segundos</font> para confirmar.</font></td></tr>");
		sb.append("<tr><td align=center><font color=\"B09878\">Si no respondes, tu rival gana por inactividad.</font></td></tr>");
		sb.append("</table><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=220 height=1><br><br>");
		sb.append("<button value=\"  Estoy Listo!  \" action=\"bypass -h Script Tournament1v1 ConfirmFight\"");
		sb.append(" width=150 height=32 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>");
		sb.append("<br></center></body></html>");
		msg.setHtml(sb.toString());
		p.sendPacket(msg);
	}

	private void startConfirmedFight()
	{
		if ((FIGHTER_BLUE == null) || (FIGHTER_RED == null))
		{
			return;
		}
		cancelQuestTimer("ConfirmTimeout", null, null);

		final String matchAnnounce = getRoundName(CURRENT_ROUND.size()) + ": " + FIGHTER_BLUE.getName() + " (Azul) VS " + FIGHTER_RED.getName() + " (Rojo)";
		if (TOURNAMENT_WORLD != null)
		{
			TOURNAMENT_WORLD.broadcastPacket(new ExShowScreenMessage(matchAnnounce, ExShowScreenMessage.TOP_CENTER, 5000, 0, true, true));
		}

		final Location blueLoc = (BLUE_SPAWN_ZONE != null) ? BLUE_SPAWN_ZONE.getRandomPoint() : LOBBY_LOC;
		final Location redLoc  = (RED_SPAWN_ZONE  != null) ? RED_SPAWN_ZONE.getRandomPoint()  : LOBBY_LOC;
		setupFighter(FIGHTER_BLUE, Team.BLUE, blueLoc);
		setupFighter(FIGHTER_RED,  Team.RED,  redLoc);

		for (int s = WAIT_TIME; s >= 1; s--)
		{
			final StatSet sp = new StatSet();
			sp.set("sec", s);
			getTimers().addTimer("Countdown_" + s, sp, (long) (WAIT_TIME - s) * 1000, null, null);
		}
		getTimers().addTimer("StartFight", new StatSet(), (long) WAIT_TIME * 1000, null, null);
	}

	private void onConfirmTimeout()
	{
		if ((FIGHTER_BLUE == null) && (FIGHTER_RED == null))
		{
			return;
		}

		if (BLUE_CONFIRMED && !RED_CONFIRMED)
		{
			// Rojo inactivo -> Azul gana
			final String msg = FIGHTER_BLUE.getName() + " gana por inactividad de " + FIGHTER_RED.getName() + "!";
			Broadcast.toAllOnlinePlayers("Torneo 1v1: " + msg);
			if (FIGHTER_RED.isOnlineInt() == 1)
			{
				FIGHTER_RED.sendMessage("[Torneo] Eliminado por no confirmar a tiempo.");
			}
			endMatch(FIGHTER_BLUE, FIGHTER_RED);
		}
		else if (!BLUE_CONFIRMED && RED_CONFIRMED)
		{
			// Azul inactivo -> Rojo gana
			final String msg = FIGHTER_RED.getName() + " gana por inactividad de " + FIGHTER_BLUE.getName() + "!";
			Broadcast.toAllOnlinePlayers("Torneo 1v1: " + msg);
			if (FIGHTER_BLUE.isOnlineInt() == 1)
			{
				FIGHTER_BLUE.sendMessage("[Torneo] Eliminado por no confirmar a tiempo.");
			}
			endMatch(FIGHTER_RED, FIGHTER_BLUE);
		}
		else
		{
			// Ninguno confirmo -> ambos eliminados
			Broadcast.toAllOnlinePlayers("Torneo 1v1: Ambos jugadores inactivos (" + FIGHTER_BLUE.getName() + " y " + FIGHTER_RED.getName() + "). Partido anulado.");
			for (Player f : new Player[]{ FIGHTER_BLUE, FIGHTER_RED })
			{
				if (f == null)
				{
					continue;
				}
				f.setOnEvent(false);
				f.setOnSoloEvent(false);
				f.setInvul(false);
				f.setImmobilized(false);
				f.enableAllSkills();
				removeListeners(f);
				CURRENT_ROUND.remove(f);
				if (f.isOnlineInt() == 1)
				{
					f.teleToLocation(GIRAN_LOC, 0, 50);
					f.sendMessage("[Torneo] Eliminado por inactividad. Fuiste transportado a Giran.");
				}
			}
			FIGHTER_BLUE = null;
			FIGHTER_RED  = null;
			MATCH_INDEX++;
			getTimers().addTimer("NextMatch", new StatSet(), 5000L, null, null);
		}
	}

	private void advanceByWalkover(Player winner, Player loser)
	{
		ROUND_WINNERS.add(winner);
		winner.sendMessage("[Torneo] Avanzas por abandono de tu rival.");
		if (loser != null)
		{
			CURRENT_ROUND.remove(loser);
		}
		MATCH_INDEX++;
		getTimers().addTimer("NextMatch", new StatSet(), 5000L, null, null);
	}

	private void setupFighter(Player p, Team team, Location loc)
	{
		p.setTeam(team);
		p.teleToLocation(loc, TOURNAMENT_WORLD.getInstanceId(), 50);
		p.setInvul(true);
		p.setImmobilized(true);
		p.disableAllSkills();
		p.fullRestore();
		addDeathListener(p);
	}

	private void onStartFight()
	{
		if ((FIGHTER_BLUE == null) || (FIGHTER_RED == null))
		{
			return;
		}
		FIGHT_ONGOING = true;

		if (TOURNAMENT_WORLD != null)
		{
			TOURNAMENT_WORLD.openDoor(BLUE_DOOR_ID);
			TOURNAMENT_WORLD.openDoor(RED_DOOR_ID);
		}

		for (Player f : new Player[]{ FIGHTER_BLUE, FIGHTER_RED })
		{
			if ((f != null) && (f.isOnlineInt() == 1))
			{
				f.setInvul(false);
				f.setImmobilized(false);
				f.enableAllSkills();
			}
		}

		broadcastToWorld("¡PELEA!", 5);
		getTimers().addTimer("FightTimeout", new StatSet(), (long) FIGHT_TIME * 60 * 1000, null, null);
	}

	private void onFightTimeout()
	{
		if (!FIGHT_ONGOING || (FIGHTER_BLUE == null) || (FIGHTER_RED == null))
		{
			return;
		}
		broadcastToWorld("Tiempo agotado!", 4);
		final double hp1 = (FIGHTER_BLUE.isOnlineInt() == 1) ? ((double) FIGHTER_BLUE.getCurrentHp() / FIGHTER_BLUE.getMaxHp()) : 0;
		final double hp2 = (FIGHTER_RED.isOnlineInt() == 1)  ? ((double) FIGHTER_RED.getCurrentHp()  / FIGHTER_RED.getMaxHp())  : 0;
		final Player winner = (hp1 >= hp2) ? FIGHTER_BLUE : FIGHTER_RED;
		final Player loser  = (winner == FIGHTER_BLUE) ? FIGHTER_RED : FIGHTER_BLUE;
		winner.sendMessage("[Torneo] Ganas por tiempo! (HP superior)");
		endMatch(winner, loser);
	}

	private void endMatch(Player winner, Player loser)
	{
		FIGHT_ONGOING = false;
		cancelQuestTimer("FightTimeout", null, null);
		cancelQuestTimer("StartFight", null, null);

		if (TOURNAMENT_WORLD != null)
		{
			TOURNAMENT_WORLD.closeDoor(BLUE_DOOR_ID);
			TOURNAMENT_WORLD.closeDoor(RED_DOOR_ID);
		}

		// Reset combatientes
		for (Player f : new Player[]{ FIGHTER_BLUE, FIGHTER_RED })
		{
			if (f == null)
			{
				continue;
			}
			f.setTeam(Team.NONE);
			f.setInvul(true);
			f.setImmobilized(true);
			f.disableAllSkills();
			removeDeathListener(f);
			if (f.isDead())
			{
				f.doRevive();
			}
		}

		// Anunciar
		final String roundName = getRoundName(CURRENT_ROUND.size());
		if (winner != null)
		{
			final String winMsg = winner.getName() + " gana en " + roundName + "!";
			Broadcast.toAllOnlinePlayers("Torneo 1v1: " + winMsg);
			if (TOURNAMENT_WORLD != null)
			{
				TOURNAMENT_WORLD.broadcastPacket(new ExShowScreenMessage(winMsg, ExShowScreenMessage.TOP_CENTER, 5000, 0, true, false));
			}
			ROUND_WINNERS.add(winner);
		}
		MATCH_INDEX++;

		// Perder -> teleportar a Giran
		if (loser != null)
		{
			loser.setOnEvent(false);
			loser.setOnSoloEvent(false);
			loser.setInvul(false);
			loser.setImmobilized(false);
			loser.enableAllSkills();
			removeListeners(loser);
			if (loser.isOnlineInt() == 1)
			{
				loser.teleToLocation(GIRAN_LOC, 0, 50);
				loser.sendMessage("[Torneo] Has sido eliminado. Fuiste transportado a Giran.");
			}
			else if (TOURNAMENT_WORLD != null)
			{
				TOURNAMENT_WORLD.ejectPlayer(loser);
			}
			CURRENT_ROUND.remove(loser);
		}

		// Ganador vuelve al lobby con skills habilitadas para poder buffarse
		if ((winner != null) && (winner.isOnlineInt() == 1) && (TOURNAMENT_WORLD != null))
		{
			winner.setInvul(false);
			winner.setImmobilized(false);
			winner.enableAllSkills();
			winner.teleToLocation(LOBBY_LOC, TOURNAMENT_WORLD.getInstanceId(), 50);
			winner.sendMessage("[Torneo] Victoria! Aguarda el proximo combate en el area de espera.");
			winner.sendMessage("[Torneo] Puedes buffarte mientras esperas.");
		}

		FIGHTER_BLUE = null;
		FIGHTER_RED  = null;

		getTimers().addTimer("NextMatch", new StatSet(), (long) BETWEEN_FIGHTS * 1000, null, null);
	}

	private void announceChampion(Player champion)
	{
		// Otorgar recompensas primero
		if ((champion != null) && (champion.isOnlineInt() == 1))
		{
			champion.setHero(true);
			champion.broadcastUserInfo();
			saveHeroPersistence(champion);
			PremiumManager.getInstance().addPremiumTime(champion.getAccountName(), PREMIUM_DAYS, TimeUnit.DAYS);

			// Registrar en el ranking del Community Board
			final String className = ClassListData.getInstance().getClass(champion.getActiveClass()).getClassName();
			final String date      = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
			saveChampionToRanking(champion.getName(), className, date);
		}

		// Anuncio global con nombre y recompensas
		Broadcast.toAllOnlinePlayers("**********************************************");
		Broadcast.toAllOnlinePlayers("  TORNEO 1v1 - CAMPEON: " + (champion != null ? champion.getName() : "???"));
		Broadcast.toAllOnlinePlayers("  Recompensas obtenidas:");
		Broadcast.toAllOnlinePlayers("   * Status Hero por " + HERO_DAYS + " dias");
		Broadcast.toAllOnlinePlayers("   * Premium por " + PREMIUM_DAYS + " dias");
		Broadcast.toAllOnlinePlayers("  Felicitaciones " + (champion != null ? champion.getName() : "") + "!");
		Broadcast.toAllOnlinePlayers("**********************************************");

		// Mensaje en pantalla para todos los jugadores del mundo
		final String screenMsg = "CAMPEON: " + (champion != null ? champion.getName() : "???") + " | Hero " + HERO_DAYS + "d + Premium " + PREMIUM_DAYS + "d";
		if (TOURNAMENT_WORLD != null)
		{
			TOURNAMENT_WORLD.broadcastPacket(new ExShowScreenMessage(screenMsg, ExShowScreenMessage.TOP_CENTER, 15000, 0, true, true));
		}
		// Tambien a todos los online fuera de la instancia
		for (Player online : World.getInstance().getPlayers())
		{
			if ((TOURNAMENT_WORLD == null) || (online.getInstanceId() != TOURNAMENT_WORLD.getInstanceId()))
			{
				online.sendPacket(new ExShowScreenMessage(screenMsg, ExShowScreenMessage.TOP_CENTER, 15000, 0, true, true));
			}
		}

		// Mensaje privado al campeon
		if ((champion != null) && (champion.isOnlineInt() == 1))
		{
			champion.sendMessage("[Torneo] FELICITACIONES! Eres el CAMPEON del Torneo 1v1!");
			champion.sendMessage("[Torneo] Has recibido: Hero " + HERO_DAYS + " dias + Premium " + PREMIUM_DAYS + " dias.");
		}

		getTimers().addTimer("EndTournament", new StatSet(), 15000L, null, null);
	}

	// -------------------------------------------------------------------------
	// eventStart / eventStop  (panel admin)
	// -------------------------------------------------------------------------
	@Override
	public boolean eventStart(Player eventMaker)
	{
		if (EVENT_ACTIVE)
		{
			return false;
		}
		EVENT_ACTIVE = true;

		// Cancelar timers pendientes
		for (List<QuestTimer> list : getQuestTimers().values())
		{
			list.forEach(QuestTimer::cancel);
		}

		if (DualboxCheckConfig.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP > 0)
		{
			AntiFeedManager.getInstance().registerEvent(AntiFeedManager.L2EVENT_ID);
			AntiFeedManager.getInstance().clear(AntiFeedManager.L2EVENT_ID);
		}

		REGISTERED.clear();
		CURRENT_ROUND.clear();
		ROUND_WINNERS.clear();
		MATCH_INDEX   = 0;
		FIGHTER_BLUE  = null;
		FIGHTER_RED   = null;
		FIGHT_ONGOING = false;

		MANAGER_NPC = addSpawn(MANAGER_NPC_ID, MANAGER_SPAWN, false, (long) REGISTRATION_TIME * 60000);
		MANAGER_NPC.setTitle("L2ZonaZero-Event");
		MANAGER_NPC.broadcastStatusUpdate();

		getTimers().addTimer("TeleportToArena", new StatSet(), (long) REGISTRATION_TIME * 60000, null, null);

		Broadcast.toAllOnlinePlayers("==============================");
		Broadcast.toAllOnlinePlayers("      TORNEO 1v1 - INSCRIPCION");
		Broadcast.toAllOnlinePlayers("  Cupos: " + MAX_PLAYERS + " jugadores | Duracion por combate: " + FIGHT_TIME + " min");
		Broadcast.toAllOnlinePlayers("  Premio: Hero " + HERO_DAYS + " dias + Premium " + PREMIUM_DAYS + " dias");
		Broadcast.toAllOnlinePlayers("  Registrate en el NPC Event Manager (Giran)");
		Broadcast.toAllOnlinePlayers("  Tiempo de inscripcion: " + REGISTRATION_TIME + " minutos");
		Broadcast.toAllOnlinePlayers("==============================");

		for (int min : new int[]{ 5, 3, 1 })
		{
			if (REGISTRATION_TIME > min)
			{
				final StatSet sp = new StatSet();
				sp.set("minutes", min);
				getTimers().addTimer("RegistrationWarn_" + min, sp, (long) (REGISTRATION_TIME - min) * 60000, null, null);
			}
		}

		return true;
	}

	@Override
	public boolean eventStop()
	{
		if (!EVENT_ACTIVE)
		{
			return false;
		}
		for (List<QuestTimer> list : getQuestTimers().values())
		{
			list.forEach(QuestTimer::cancel);
		}
		cleanupEvent(false);
		Broadcast.toAllOnlinePlayers("Torneo 1v1: Evento cancelado por GM.");
		return true;
	}

	// -------------------------------------------------------------------------
	// Login: restaurar hero si es campeon vigente
	// -------------------------------------------------------------------------
	private void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		final File f = new File(HERO_FILE);
		if (!f.exists())
		{
			return;
		}
		try (FileInputStream fis = new FileInputStream(f))
		{
			final Properties p = new Properties();
			p.load(fis);
			final String objIdStr  = p.getProperty("heroObjectId");
			final String expiryStr = p.getProperty("heroExpiry");
			if ((objIdStr == null) || (expiryStr == null))
			{
				return;
			}
			final long expiryMs = Long.parseLong(expiryStr);
			if (System.currentTimeMillis() >= expiryMs)
			{
				f.delete();
				return;
			}
			if (Integer.parseInt(objIdStr) == player.getObjectId())
			{
				player.setHero(true);
				player.broadcastUserInfo();
				final long daysLeft = TimeUnit.MILLISECONDS.toDays(expiryMs - System.currentTimeMillis());
				player.sendMessage("[Torneo] Tu status Hero esta activo. Expira en ~" + daysLeft + " dia(s).");
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Tournament1v1 onPlayerLogin: " + e.getMessage());
		}
	}

	// -------------------------------------------------------------------------
	// Death listener (global via @RegisterEvent)
	// -------------------------------------------------------------------------
	@RegisterEvent(EventType.ON_CREATURE_DEATH)
	public void onCreatureDeath(OnCreatureDeath event)
	{
		if (!FIGHT_ONGOING || !event.getTarget().isPlayer())
		{
			return;
		}
		final Player killed = event.getTarget().asPlayer();
		if ((killed != FIGHTER_BLUE) && (killed != FIGHTER_RED))
		{
			return;
		}
		final Player winner = (killed == FIGHTER_BLUE) ? FIGHTER_RED : FIGHTER_BLUE;
		endMatch(winner, killed);
	}

	// -------------------------------------------------------------------------
	// Logout listener (global via @RegisterEvent)
	// -------------------------------------------------------------------------
	@RegisterEvent(EventType.ON_PLAYER_LOGOUT)
	private void onPlayerLogout(OnPlayerLogout event)
	{
		final Player player = event.getPlayer();
		REGISTERED.remove(player);
		player.setRegisteredOnEvent(false);

		if (!player.isOnEvent())
		{
			return;
		}

		if ((player == FIGHTER_BLUE) || (player == FIGHTER_RED))
		{
			if (FIGHT_ONGOING)
			{
				// Desconexion durante pelea activa
				final Player winner = (player == FIGHTER_BLUE) ? FIGHTER_RED : FIGHTER_BLUE;
				endMatch(winner, player);
			}
			else
			{
				// Desconexion durante ventana de confirmacion -> otro gana por inactividad
				cancelQuestTimer("ConfirmTimeout", null, null);
				if (player == FIGHTER_BLUE)
				{
					BLUE_CONFIRMED = false;
					RED_CONFIRMED  = true; // forzar para que onConfirmTimeout declare ganador rojo
				}
				else
				{
					RED_CONFIRMED  = false;
					BLUE_CONFIRMED = true;
				}
				onConfirmTimeout();
			}
		}
		else
		{
			CURRENT_ROUND.remove(player);
			ROUND_WINNERS.remove(player);
			player.setTeam(Team.NONE);
			player.setOnEvent(false);
			player.setOnSoloEvent(false);
			removeListeners(player);
		}
	}

	// -------------------------------------------------------------------------
	// Validacion de registro
	// -------------------------------------------------------------------------
	private boolean canRegister(Player player)
	{
		if (REGISTERED.contains(player))
		{
			player.sendMessage("Ya estas inscrito en el torneo.");
			return false;
		}
		if (REGISTERED.size() >= MAX_PLAYERS)
		{
			player.sendMessage("Cupos llenos (" + MAX_PLAYERS + "/" + MAX_PLAYERS + ").");
			return false;
		}
		if (player.getLevel() < MIN_LEVEL)
		{
			player.sendMessage("Necesitas nivel " + MIN_LEVEL + " para participar.");
			return false;
		}
		if (player.isRegisteredOnEvent() || (player.getBlockCheckerArena() > -1))
		{
			player.sendMessage("Ya estas registrado en otro evento.");
			return false;
		}
		if (player.isFlyingMounted() || player.isTransformed())
		{
			player.sendMessage("No puedes registrarte transformado o montado en vuelo.");
			return false;
		}
		if (!player.isInventoryUnder80(false) || (player.getWeightPenalty() != 0))
		{
			player.sendMessage("Inventario demasiado lleno. Libera espacio e intenta de nuevo.");
			return false;
		}
		if (player.isCursedWeaponEquipped() || (player.getKarma() > 0))
		{
			player.sendMessage("No puedes participar con karma negativo.");
			return false;
		}
		if (player.isInDuel())
		{
			player.sendMessage("No puedes registrarte en un duelo.");
			return false;
		}
		if (player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player))
		{
			player.sendMessage("No puedes participar mientras estas en la Olimpiada.");
			return false;
		}
		if (player.getInstanceId() > 0)
		{
			player.sendMessage("No puedes registrarte estando en una instancia.");
			return false;
		}
		if (player.isInSiege() || player.isInsideZone(ZoneId.SIEGE))
		{
			player.sendMessage("No puedes registrarte en un asedio.");
			return false;
		}
		if (player.isFishing())
		{
			player.sendMessage("No puedes registrarte mientras pescas.");
			return false;
		}
		return true;
	}

	// -------------------------------------------------------------------------
	// Limpieza
	// -------------------------------------------------------------------------
	private void cleanupEvent(boolean endedNormally)
	{
		if (!endedNormally)
		{
			for (Player p : REGISTERED)
			{
				removeListeners(p);
				p.setRegisteredOnEvent(false);
			}
		}

		for (Player p : CURRENT_ROUND)
		{
			if (p == null)
			{
				continue;
			}
			p.setTeam(Team.NONE);
			p.setOnEvent(false);
			p.setOnSoloEvent(false);
			p.setInvul(false);
			p.setImmobilized(false);
			p.enableAllSkills();
			removeListeners(p);
			if (p.isOnlineInt() == 1)
			{
				p.teleToLocation(GIRAN_LOC, 0, 50);
			}
			else if (TOURNAMENT_WORLD != null)
			{
				TOURNAMENT_WORLD.ejectPlayer(p);
			}
		}

		if (TOURNAMENT_WORLD != null)
		{
			final Instance inst = InstanceManager.getInstance().getInstance(TOURNAMENT_WORLD.getInstanceId());
			if (inst != null)
			{
				inst.setDuration(60000);
				inst.setEmptyDestroyTime(0);
			}
			TOURNAMENT_WORLD = null;
		}

		if (MANAGER_NPC != null)
		{
			MANAGER_NPC.deleteMe();
			MANAGER_NPC = null;
		}

		REGISTERED.clear();
		CURRENT_ROUND.clear();
		ROUND_WINNERS.clear();
		MATCH_INDEX    = 0;
		FIGHTER_BLUE   = null;
		FIGHTER_RED    = null;
		FIGHT_ONGOING  = false;
		BLUE_CONFIRMED = false;
		RED_CONFIRMED  = false;
		EVENT_ACTIVE   = false;
	}

	// -------------------------------------------------------------------------
	// Utilidades
	// -------------------------------------------------------------------------
	private String getRoundName(int players)
	{
		switch (players)
		{
			case 16: return "Ronda de 16";
			case 8:  return "Cuartos de Final";
			case 4:  return "Semifinal";
			case 2:  return "Gran Final";
			default: return "Ronda (" + players + " jugadores)";
		}
	}

	private void broadcastToWorld(String msg, int durationSec)
	{
		if (TOURNAMENT_WORLD != null)
		{
			TOURNAMENT_WORLD.broadcastPacket(new ExShowScreenMessage(msg, ExShowScreenMessage.TOP_CENTER, durationSec * 1000, 0, true, false));
		}
	}

	private void broadcastToRegistered(String msg)
	{
		for (Player p : REGISTERED)
		{
			if ((p != null) && (p.isOnlineInt() == 1))
			{
				p.sendMessage("[Torneo] " + msg);
			}
		}
	}

	private void broadcastToFighters(String msg, int durationSec)
	{
		for (Player f : new Player[]{ FIGHTER_BLUE, FIGHTER_RED })
		{
			if ((f != null) && (f.isOnlineInt() == 1))
			{
				f.sendPacket(new ExShowScreenMessage(msg, ExShowScreenMessage.TOP_CENTER, durationSec * 1000, 0, true, false));
			}
		}
	}

	private void sendManagerHtml(Player player, Npc npc, String file)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setFile(player, "data/scripts/custom/events/Tournament1v1/" + file);
		msg.replace("%player_numbers%", String.valueOf(REGISTERED.size()));
		msg.replace("%max_players%",    String.valueOf(MAX_PLAYERS));
		msg.replace("%hero_days%",      String.valueOf(HERO_DAYS));
		msg.replace("%premium_days%",   String.valueOf(PREMIUM_DAYS));
		msg.replace("%min_level%",      String.valueOf(MIN_LEVEL));
		player.sendPacket(msg);
	}

	private void preparePlayer(Player p)
	{
		p.setOnEvent(true);
		p.setOnSoloEvent(true);
		p.setRegisteredOnEvent(false);
		TOURNAMENT_WORLD.addAllowed(p);
		p.leaveParty();
		p.setInvul(true);
		p.setImmobilized(false); // puede moverse en el lobby
		// Skills habilitadas: el jugador puede buffarse libremente en el lobby
	}

	private void addLogoutListener(Player player)
	{
		player.addListener(new ConsumerEventListener(player, EventType.ON_PLAYER_LOGOUT, (OnPlayerLogout ev) -> onPlayerLogout(ev), this));
	}

	private void addDeathListener(Player player)
	{
		player.addListener(new ConsumerEventListener(player, EventType.ON_CREATURE_DEATH, (OnCreatureDeath ev) -> onCreatureDeath(ev), this));
	}

	private void removeDeathListener(Player player)
	{
		for (AbstractEventListener l : player.getListeners(EventType.ON_CREATURE_DEATH))
		{
			if (l.getOwner() == this)
			{
				l.unregisterMe();
			}
		}
	}

	private void removeListeners(Player player)
	{
		for (EventType type : new EventType[]{ EventType.ON_PLAYER_LOGOUT, EventType.ON_CREATURE_DEATH })
		{
			for (AbstractEventListener l : player.getListeners(type))
			{
				if (l.getOwner() == this)
				{
					l.unregisterMe();
				}
			}
		}
	}

	// Ruta compartida con TournamentBoard (ambos leen/escriben el mismo archivo)
	private static final String CHAMPIONS_FILE = "./config/Custom/TournamentChampions.csv";

	private void saveChampionToRanking(String name, String className, String date)
	{
		try
		{
			new java.io.File("./config/Custom").mkdirs();
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(CHAMPIONS_FILE, true)))
			{
				bw.write(name + "|" + className + "|" + date);
				bw.newLine();
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Tournament1v1: Error guardando campeon en ranking: " + e.getMessage());
		}
	}

	public static void main(String[] args)
	{
		new Tournament1v1();
	}

	@Override
	public boolean eventBypass(Player player, String bypass)
	{
		if (!player.isGM())
		{
			return false;
		}

		// Quitar jugador inscrito: Remove_objectId
		if (bypass.startsWith("Remove_"))
		{
			final int objId = Integer.parseInt(bypass.substring(7));
			removeRegisteredById(objId, player);
			// Refrescar lista
			eventBypass(player, "ListPlayers");
			return true;
		}

		switch (bypass)
		{
			case "ForceStart":
			{
				if (!EVENT_ACTIVE)
				{
					player.sendSysMessage("Torneo 1vs1: El evento no esta activo. Usa Iniciar primero.");
					break;
				}
				cancelQuestTimer("TeleportToArena", null, null);
				for (int min : new int[]{ 3, 1 })
				{
					cancelQuestTimer("FullWarn_" + min, null, null);
				}
				getTimers().addTimer("TeleportToArena", new StatSet(), 10000L, null, null);
				Broadcast.toAllOnlinePlayers("Torneo 1vs1: Un GM fuerza el inicio en 10 segundos!");
				broadcastToRegistered("El torneo inicia en 10 segundos! Preparate!");
				player.sendSysMessage("Torneo 1vs1: Inicio forzado. Iniciando en 10s. Inscritos: " + REGISTERED.size());
				break;
			}
			case "ListPlayers":
			{
				final NpcHtmlMessage html = new NpcHtmlMessage();
				final StringBuilder sb = new StringBuilder();
				sb.append("<html><title>Torneo 1vs1 - Panel Admin</title><body>");
				sb.append("<table width=270 border=0 bgcolor=\"444444\">");
				sb.append("<tr><td><button value=\"Back\" action=\"bypass -h admin_event_menu\" width=65 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("<td align=center><font color=\"CDB67F\">Torneo 1vs1</font></td></tr>");
				sb.append("</table><br>");

				// Estado del evento
				sb.append("<table width=270 border=0>");
				sb.append("<tr><td align=center><font color=\"FFFFFF\">Estado: <font color=\"").append(EVENT_ACTIVE ? "93FFA8" : "FF6060").append("\">").append(EVENT_ACTIVE ? "ACTIVO" : "INACTIVO").append("</font></font></td></tr>");
				sb.append("<tr><td align=center><font color=\"FFFFFF\">Inscritos: <font color=\"LEVEL\">").append(REGISTERED.size()).append(" / ").append(MAX_PLAYERS).append("</font></font></td></tr>");
				if (!CURRENT_ROUND.isEmpty())
				{
					sb.append("<tr><td align=center><font color=\"FFAA00\">Ronda: ").append(getRoundName(CURRENT_ROUND.size())).append("</font></td></tr>");
				}
				if (FIGHT_ONGOING && FIGHTER_BLUE != null && FIGHTER_RED != null)
				{
					sb.append("<tr><td align=center><font color=\"FF9900\">Combate: ").append(FIGHTER_BLUE.getName()).append(" vs ").append(FIGHTER_RED.getName()).append("</font></td></tr>");
				}
				sb.append("</table><br>");

				// Botones de control
				sb.append("<table width=270 border=0>");
				sb.append("<tr>");
				sb.append("<td><button value=\"Iniciar\" action=\"bypass -h admin_event_start_menu Tournament1v1\" width=65 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("<td><button value=\"Detener\" action=\"bypass -h admin_event_stop_menu Tournament1v1\" width=65 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("<td><button value=\"Forzar\" action=\"bypass -h admin_event_bypass Tournament1v1 ForceStart\" width=65 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("<td><button value=\"Refresh\" action=\"bypass -h admin_event_bypass Tournament1v1 ListPlayers\" width=65 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("</tr></table><br>");

				// Lista de inscritos
				if (REGISTERED.isEmpty())
				{
					sb.append("<table width=270><tr><td align=center><font color=\"B09878\">Sin inscritos actualmente.</font></td></tr></table>");
				}
				else
				{
					sb.append("<table width=270 border=0 bgcolor=\"222222\">");
					sb.append("<tr><td width=190><font color=\"CDB67F\">Jugador</font></td><td width=80><font color=\"CDB67F\">Accion</font></td></tr>");
					for (Player p : REGISTERED)
					{
						sb.append("<tr>")
							.append("<td><font color=\"FFFFFF\">").append(p.getName()).append("</font></td>")
							.append("<td><button value=\"Quitar\" action=\"bypass -h admin_event_bypass Tournament1v1 Remove_").append(p.getObjectId()).append("\" width=60 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>")
							.append("</tr>");
					}
					sb.append("</table>");
				}
				sb.append("</body></html>");
				html.setHtml(sb.toString());
				player.sendPacket(html);
				break;
			}
		}
		return true;
	}

	private void removeRegisteredById(int objectId, Player gm)
	{
		Player target = null;
		for (Player p : REGISTERED)
		{
			if (p.getObjectId() == objectId)
			{
				target = p;
				break;
			}
		}
		if (target == null)
		{
			if (gm != null)
			{
				gm.sendSysMessage("Torneo 1vs1: Jugador no encontrado en el registro.");
			}
			return;
		}
		if (DualboxCheckConfig.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP > 0)
		{
			AntiFeedManager.getInstance().removePlayer(AntiFeedManager.L2EVENT_ID, target);
		}
		REGISTERED.remove(target);
		removeListeners(target);
		target.setRegisteredOnEvent(false);
		target.sendMessage("[Torneo] Has sido retirado del registro por un GM.");
		if (gm != null)
		{
			gm.sendSysMessage("Torneo 1vs1: " + target.getName() + " removido del registro.");
		}
	}

}
