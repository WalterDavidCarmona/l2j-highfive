/*
 * Global Gatekeeper NPC - Custom Teleporter
 * Teleporta a Ciudades, Castillos, Fortalezas, Zonas Farm, Clan PvP, Party PvP.
 *
 * Restricciones configurables:
 *   - Modo Combate activo
 *   - Registrado / activo en Olympiad
 *   - Karma (PK)
 *
 * Zonas especiales:
 *   - Zona Clan PvP : solo si el jugador pertenece a un clan
 *   - Zona Party PvP: solo el LIDER del grupo puede usarlo.
 *                     Los demas miembros reciben una invitacion con YES/NO.
 *                     Cada jugador aparece en un punto de spawn aleatorio (4 configurables).
 *
 * Config: game/config/Custom/GlobalGatekeeper.ini
 * NPC XML: game/data/stats/npcs/custom/GlobalGatekeeper.xml (ID 50009)
 */
package custom.GlobalGatekeeper;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import custom.ClanPvpZone.ClanPvpZone;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Global Gatekeeper NPC Script.
 * @author Custom - Aden Chronicles
 */
public class GlobalGatekeeper extends Script
{
	private static final Logger LOGGER = Logger.getLogger(GlobalGatekeeper.class.getName());

	// ---------------------------------------------------------------------------
	// Config Fields
	// ---------------------------------------------------------------------------
	private static boolean ENABLED = true;
	private static int NPC_ID = 50009;
	private static long TELEPORT_PRICE = 0;
	private static int CURRENCY_ID = 57;
	private static boolean BLOCK_COMBAT = true;
	private static boolean BLOCK_OLYMPIAD = true;
	private static boolean BLOCK_PK = true;
	private static String CLAN_PVP_ZONE_NAME = "Zona Clan PvP";
	private static String PARTY_PVP_ZONE_NAME = "Zona Party PvP";

	/** Tiempo en ms que un miembro tiene para aceptar la invitacion al Party PvP. */
	private static long PARTY_PVP_INVITE_EXPIRE_MS = 30_000L;

	// ---------------------------------------------------------------------------
	// Teleport Lists
	// ---------------------------------------------------------------------------
	private static final Map<String, Location> TOWN_TELEPORTS = new LinkedHashMap<>();
	private static final Map<String, Location> CASTLE_TELEPORTS = new LinkedHashMap<>();
	private static final Map<String, Location> FORTRESS_TELEPORTS = new LinkedHashMap<>();
	private static final Map<String, Location> FARM_EASY = new LinkedHashMap<>();
	private static final Map<String, Location> FARM_MEDIUM = new LinkedHashMap<>();
	private static final Map<String, Location> FARM_HARD = new LinkedHashMap<>();

	// ---------------------------------------------------------------------------
	// Special Zones
	// ---------------------------------------------------------------------------

	/** Zona Clan PvP: un unico punto de llegada. */
	private static Location CLAN_PVP_ZONE = null;

	/**
	 * Zona Party PvP: 4 puntos de spawn configurables.
	 * Cada jugador teletransportado llega a uno aleatorio.
	 */
	private static final List<Location> PARTY_PVP_SPAWNS = new ArrayList<>();

	/**
	 * Invitaciones pendientes de Party PvP.
	 * Clave: objectId del miembro | Valor: timestamp de cuando se envio.
	 */
	private static final Map<Integer, Long> PENDING_PARTY_INVITES = new ConcurrentHashMap<>();

	// ---------------------------------------------------------------------------
	// Default Teleport Lists (fallback si no se encuentra el config)
	// ---------------------------------------------------------------------------
	private static final String DEFAULT_TOWNS =
		"Giran,82698,148638,-3473;Aden,147450,27064,-2208;Goddard,147725,-56517,-2780;" +
		"Rune,44070,-50243,-796;Dion,18748,145437,-3132;Oren,82321,55139,-1529;" +
		"Gludio,-14225,123540,-3121;Schuttgart,87358,-141982,-1341;Heine,111115,219017,-3547;" +
		"Gludin,-83063,150791,-3133;Hunters Village,116589,76268,-2734;" +
		"Talking Island,-82687,243157,-3734;Dwarven Village,116551,-182493,-1525;" +
		"Orc Village,-44211,-113521,-241;Dark Elven Village,12428,16551,-4588;" +
		"Elven Village,45873,49288,-3064;Kamael Village,-116934,46616,368";

	private static final String DEFAULT_CASTLES =
		"Castillo de Gludio,17128,169904,-3507;Castillo de Dion,14784,142848,-2709;" +
		"Castillo de Giran,110784,219520,-3546;Castillo de Innadril,111296,218880,-3547;" +
		"Castillo de Oren,80000,57216,-1520;Castillo de Aden,148224,25984,-2192;" +
		"Castillo de Goddard,147456,-55616,-2780;Castillo de Rune,43008,-47872,-800;" +
		"Castillo de Schuttgart,85632,-143360,-1340";

	private static final String DEFAULT_FORTRESSES =
		"Fortin Shanty,-84558,151735,-3129;Fortin Southern,-46068,97000,-5792;" +
		"Fortin Hive,47136,-47936,512;Fortin Valley,59520,-94144,-1392;" +
		"Fortin Ivory,6592,158656,-3402;Fortin Narsell,110064,-175040,-1360;" +
		"Fortin Bayou,-7696,12928,-3600;Fortin White Sands,115008,-179136,-1280;" +
		"Fortin Borderland,17536,73280,-3152;Fortin Swamp,-15744,19456,-3120;" +
		"Fortin Archaic,73728,180736,-3552;Fortin Floran,16640,16128,-3499;" +
		"Fortin Cloud Mountain,-15744,-49024,-3128;Fortin Tanor,110560,-178048,-672;" +
		"Fortin Dragonspine,-78016,-62336,-3096;Fortin Antharas Lair,74496,196096,-3552;" +
		"Fortin Western,-45888,97088,-5744;Fortin Innadril,110592,-17440,-3520;" +
		"Fortin Monastic,34112,255936,-1472;Castillo Devastado,48384,-19072,-3504";

	// ---------------------------------------------------------------------------
	// Constructor
	// ---------------------------------------------------------------------------
	private GlobalGatekeeper()
	{
		loadConfig();
		if (ENABLED)
		{
			addStartNpc(NPC_ID);
			addFirstTalkId(NPC_ID);
			addTalkId(NPC_ID);

			// Registrar bypass handler para las respuestas YES/NO de la invitacion Party PvP
			BypassHandler.getInstance().registerHandler(new PartyPvpBypass());

			LOGGER.info("GlobalGatekeeper: Activado | NPC ID: " + NPC_ID
				+ " | Precio: " + TELEPORT_PRICE
				+ " | Ciudades: " + TOWN_TELEPORTS.size()
				+ " | Castillos: " + CASTLE_TELEPORTS.size()
				+ " | Fortalezas: " + FORTRESS_TELEPORTS.size()
				+ " | Farm Easy/Med/Hard: " + FARM_EASY.size() + "/" + FARM_MEDIUM.size() + "/" + FARM_HARD.size()
				+ " | Party Spawns: " + PARTY_PVP_SPAWNS.size()
				+ " | Invite Expire: " + (PARTY_PVP_INVITE_EXPIRE_MS / 1000) + "s");
		}
		else
		{
			LOGGER.info("GlobalGatekeeper: Desactivado en config.");
		}
	}

	// ---------------------------------------------------------------------------
	// Config Loading
	// ---------------------------------------------------------------------------
	private void loadConfig()
	{
		final Properties props = new Properties();
		try (InputStream is = new FileInputStream("./config/Custom/GlobalGatekeeper.ini"))
		{
			props.load(is);
		}
		catch (Exception e)
		{
			LOGGER.warning("GlobalGatekeeper: No se pudo cargar GlobalGatekeeper.ini: " + e.getMessage() + " - Usando valores por defecto.");
		}

		ENABLED = Boolean.parseBoolean(props.getProperty("GlobalGatekeeperEnabled", "True").trim());
		NPC_ID = Integer.parseInt(props.getProperty("GlobalGatekeeperNpcId", "50009").trim());
		TELEPORT_PRICE = Long.parseLong(props.getProperty("GlobalGatekeeperTeleportPrice", "0").trim());
		CURRENCY_ID = Integer.parseInt(props.getProperty("GlobalGatekeeperCurrencyId", "57").trim());
		BLOCK_COMBAT = Boolean.parseBoolean(props.getProperty("GlobalGatekeeperBlockCombat", "True").trim());
		BLOCK_OLYMPIAD = Boolean.parseBoolean(props.getProperty("GlobalGatekeeperBlockOlympiad", "True").trim());
		BLOCK_PK = Boolean.parseBoolean(props.getProperty("GlobalGatekeeperBlockPK", "True").trim());
		CLAN_PVP_ZONE_NAME = props.getProperty("ClanPvpZoneName", "Zona Clan PvP").trim();
		PARTY_PVP_ZONE_NAME = props.getProperty("PartyPvpZoneName", "Zona Party PvP").trim();
		PARTY_PVP_INVITE_EXPIRE_MS = Long.parseLong(props.getProperty("PartyPvpInviteExpireSeconds", "30").trim()) * 1000L;

		// Listas de teleport
		TOWN_TELEPORTS.clear();
		parseList(props.getProperty("TownTeleports", DEFAULT_TOWNS), TOWN_TELEPORTS);

		CASTLE_TELEPORTS.clear();
		parseList(props.getProperty("CastleTeleports", DEFAULT_CASTLES), CASTLE_TELEPORTS);

		FORTRESS_TELEPORTS.clear();
		parseList(props.getProperty("FortressTeleports", DEFAULT_FORTRESSES), FORTRESS_TELEPORTS);

		FARM_EASY.clear();
		parseList(props.getProperty("FarmZonesEasy", ""), FARM_EASY);

		FARM_MEDIUM.clear();
		parseList(props.getProperty("FarmZonesMedium", ""), FARM_MEDIUM);

		FARM_HARD.clear();
		parseList(props.getProperty("FarmZonesHard", ""), FARM_HARD);

		// Zona Clan PvP (punto unico)
		try
		{
			final int cx = Integer.parseInt(props.getProperty("ClanPvpZoneX", "82698").trim());
			final int cy = Integer.parseInt(props.getProperty("ClanPvpZoneY", "148638").trim());
			final int cz = Integer.parseInt(props.getProperty("ClanPvpZoneZ", "-3473").trim());
			CLAN_PVP_ZONE = new Location(cx, cy, cz);
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning("GlobalGatekeeper: Coordenadas invalidas en ClanPvpZone.");
		}

		// Zona Party PvP: 4 puntos de spawn
		PARTY_PVP_SPAWNS.clear();
		for (int i = 1; i <= 4; i++)
		{
			final String val = props.getProperty("PartyPvpSpawn" + i, "").trim();
			if (val.isEmpty())
			{
				continue;
			}
			final String[] parts = val.split(",");
			if (parts.length < 3)
			{
				LOGGER.warning("GlobalGatekeeper: Formato invalido en PartyPvpSpawn" + i + " (se esperan X,Y,Z).");
				continue;
			}
			try
			{
				PARTY_PVP_SPAWNS.add(new Location(
					Integer.parseInt(parts[0].trim()),
					Integer.parseInt(parts[1].trim()),
					Integer.parseInt(parts[2].trim())));
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("GlobalGatekeeper: Coordenadas invalidas en PartyPvpSpawn" + i + ".");
			}
		}

		// Fallback si no se configuraron puntos de spawn
		if (PARTY_PVP_SPAWNS.isEmpty())
		{
			PARTY_PVP_SPAWNS.add(new Location(147450, 27064, -2208));
			PARTY_PVP_SPAWNS.add(new Location(147650, 27264, -2208));
			PARTY_PVP_SPAWNS.add(new Location(147250, 27264, -2208));
			PARTY_PVP_SPAWNS.add(new Location(147450, 26864, -2208));
			LOGGER.warning("GlobalGatekeeper: No se configuraron PartyPvpSpawn1-4. Usando coordenadas de Aden por defecto.");
		}
	}

	/**
	 * Parsea "Nombre,X,Y,Z;Nombre2,X2,Y2,Z2;..." y llena el mapa destino.
	 */
	private void parseList(String value, Map<String, Location> map)
	{
		if ((value == null) || value.trim().isEmpty())
		{
			return;
		}
		for (String entry : value.split(";"))
		{
			entry = entry.trim();
			if (entry.isEmpty())
			{
				continue;
			}
			final String[] parts = entry.split(",");
			if (parts.length < 4)
			{
				LOGGER.warning("GlobalGatekeeper: Entrada invalida (Nombre,X,Y,Z): " + entry);
				continue;
			}
			try
			{
				map.put(parts[0].trim(), new Location(
					Integer.parseInt(parts[1].trim()),
					Integer.parseInt(parts[2].trim()),
					Integer.parseInt(parts[3].trim())));
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("GlobalGatekeeper: Coordenadas invalidas en: " + entry);
			}
		}
	}

	// ---------------------------------------------------------------------------
	// NPC Dialog Handlers
	// ---------------------------------------------------------------------------

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (!ENABLED)
		{
			return null;
		}
		return buildMainPage(player);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!ENABLED || (player == null))
		{
			return null;
		}

		switch (event)
		{
			// ---- Navegacion ----
			case "main":
				return buildMainPage(player);
			case "towns":
				return buildListPage("Ciudades", TOWN_TELEPORTS, "town", "main");
			case "castles":
				return buildListPage("Zonas de Castillo", CASTLE_TELEPORTS, "castle", "main");
			case "fortress":
				return buildListPage("Fortalezas", FORTRESS_TELEPORTS, "fort", "main");
			case "farm":
				return buildFarmMenu();
			case "farm_easy":
				return buildListPage("Zona Farm Facil", FARM_EASY, "feasy", "farm");
			case "farm_medium":
				return buildListPage("Zona Farm Media", FARM_MEDIUM, "fmed", "farm");
			case "farm_hard":
				return buildListPage("Zona Farm Dificil", FARM_HARD, "fhard", "farm");

			// ---- Zonas especiales: paginas de info ----
			case "clanpvp":
				return buildClanPvpPage(player);
			case "partypvp":
				return buildPartyPvpPage(player);

			// ---- Zona Clan PvP: el teleport directo ya no existe;
			//      el registro se hace via bypass clanpvz_register desde buildClanPvpPage ----
			case "tp_partypvp":
				return handlePartyPvpTeleport(npc, player);
		}

		// ---- Teleports dinamicos: tp_TIPO_INDICE ----
		if (event.startsWith("tp_town_"))
		{
			return handleIndexTeleport(TOWN_TELEPORTS, event.substring(8), player, "towns");
		}
		if (event.startsWith("tp_castle_"))
		{
			return handleIndexTeleport(CASTLE_TELEPORTS, event.substring(10), player, "castles");
		}
		if (event.startsWith("tp_fort_"))
		{
			return handleIndexTeleport(FORTRESS_TELEPORTS, event.substring(8), player, "fortress");
		}
		if (event.startsWith("tp_feasy_"))
		{
			return handleIndexTeleport(FARM_EASY, event.substring(9), player, "farm_easy");
		}
		if (event.startsWith("tp_fmed_"))
		{
			return handleIndexTeleport(FARM_MEDIUM, event.substring(8), player, "farm_medium");
		}
		if (event.startsWith("tp_fhard_"))
		{
			return handleIndexTeleport(FARM_HARD, event.substring(9), player, "farm_hard");
		}

		return null;
	}

	// ---------------------------------------------------------------------------
	// Teleport Logic
	// ---------------------------------------------------------------------------

	/** Teleport a una entrada del mapa por indice. */
	private String handleIndexTeleport(Map<String, Location> map, String indexStr, Player player, String backEvent)
	{
		final String blocked = checkRestrictions(player, backEvent);
		if (blocked != null)
		{
			return blocked;
		}

		int index;
		try
		{
			index = Integer.parseInt(indexStr);
		}
		catch (NumberFormatException e)
		{
			return null;
		}

		final Location loc = getLocationByIndex(map, index);
		if (loc == null)
		{
			return buildErrorPage("Destino no encontrado.", backEvent);
		}

		executeTeleport(player, loc);
		return null;
	}

	// handleClanPvpTeleport eliminado: el registro lo maneja ClanPvpZone via bypass clanpvz_register.

	/**
	 * Zona Party PvP: SOLO el lider del grupo puede usarlo.
	 * El lider es teletransportado de inmediato.
	 * Cada miembro del grupo recibe una invitacion YES/NO via popup.
	 */
	private String handlePartyPvpTeleport(Npc npc, Player leader)
	{
		// Verificar restricciones del lider
		final String blocked = checkRestrictions(leader, "partypvp");
		if (blocked != null)
		{
			return blocked;
		}

		// Debe estar en party
		if (!leader.isInParty())
		{
			return buildErrorPage("Debes estar en un grupo para acceder a esta zona.", "partypvp");
		}

		// Solo el lider puede usar esto
		if (leader.getParty().getLeader() != leader)
		{
			return buildErrorPage("Solo el lider del grupo puede usar este teleport.", "partypvp");
		}

		// Verificar que hay puntos de spawn configurados
		if (PARTY_PVP_SPAWNS.isEmpty())
		{
			return buildErrorPage("La Zona Party PvP no tiene puntos de spawn configurados.", "main");
		}

		// Teletransportar al lider inmediatamente
		executeTeleport(leader, getRandomPartySpawn());
		leader.sendMessage("Has llevado a tu grupo a " + PARTY_PVP_ZONE_NAME + ".");

		// Enviar invitacion a cada miembro (excepto lider)
		final String leaderName = leader.getName();
		int invited = 0;
		for (Player member : leader.getParty().getMembers())
		{
			if ((member == leader) || (member == null) || !member.isOnline())
			{
				continue;
			}

			// Registrar invitacion pendiente (timestamp actual)
			PENDING_PARTY_INVITES.put(member.getObjectId(), System.currentTimeMillis());

			// Enviar popup de invitacion
			sendPartyInvite(member, leaderName);
			invited++;
		}

		if (invited > 0)
		{
			leader.sendMessage("Se enviaron invitaciones a " + invited + " miembro(s). Tienen " + (PARTY_PVP_INVITE_EXPIRE_MS / 1000) + " segundos para aceptar.");
		}

		return null;
	}

	/**
	 * Envia el popup de invitacion a un miembro de la party.
	 */
	private static void sendPartyInvite(Player member, String leaderName)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(buildPartyInviteHtml(leaderName));
		member.sendPacket(html);
	}

	/**
	 * Construye el HTML del popup de invitacion a la Zona Party PvP.
	 */
	private static String buildPartyInviteHtml(String leaderName)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table width=270 cellpadding=0 cellspacing=2>");

		sb.append("<tr><td align=center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></td></tr>");
		sb.append("<tr><td align=center><font color=\"C8A84B\">Invitacion de Zona PvP</font></td></tr>");
		sb.append("<tr><td align=center><img src=\"L2UI.SquareGray\" width=250 height=1></td></tr>");
		sb.append("<tr><td height=6></td></tr>");

		sb.append("<tr><td><font color=\"9A9280\">");
		sb.append("El lider <font color=\"FFAA00\">").append(leaderName).append("</font><br>");
		sb.append("quiere llevarte a<br>");
		sb.append("<font color=\"C8A84B\">").append(PARTY_PVP_ZONE_NAME).append("</font>.<br><br>");
		sb.append("<font color=\"707070\">Tienes ").append(PARTY_PVP_INVITE_EXPIRE_MS / 1000).append(" segundos para responder.</font>");
		sb.append("</font></td></tr>");

		sb.append("<tr><td height=10></td></tr>");

		// Botones Aceptar / Rechazar en la misma fila
		sb.append("<tr><td align=center>");
		sb.append("<table width=250 cellspacing=4><tr>");
		sb.append("<td><button value=\"Aceptar\" action=\"bypass -h gk_party_accept\" width=118 height=26 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		sb.append("<td><button value=\"Rechazar\" action=\"bypass -h gk_party_decline\" width=118 height=26 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		sb.append("</tr></table>");
		sb.append("</td></tr>");

		sb.append("<tr><td height=4></td></tr>");
		sb.append("</table>");
		sb.append("</body></html>");
		return sb.toString();
	}

	// ---------------------------------------------------------------------------
	// Restriction Checks & Teleport Execution
	// ---------------------------------------------------------------------------

	/** Verifica las restricciones configuradas. Retorna pagina de error o null si puede teleportarse. */
	private String checkRestrictions(Player player, String backEvent)
	{
		if (BLOCK_PK && (player.getKarma() > 0))
		{
			return buildErrorPage("No puedes teleportarte siendo PK (tienes Karma activo).", backEvent);
		}
		if (BLOCK_COMBAT && player.isInCombat())
		{
			return buildErrorPage("No puedes teleportarte mientras estes en combate.", backEvent);
		}
		if (BLOCK_OLYMPIAD && (player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player)))
		{
			return buildErrorPage("No puedes teleportarte estando registrado en las Olimpiadas.", backEvent);
		}
		if ((TELEPORT_PRICE > 0) && (player.getInventory().getInventoryItemCount(CURRENCY_ID, -1) < TELEPORT_PRICE))
		{
			return buildErrorPage("Moneda insuficiente. Necesitas " + TELEPORT_PRICE + " (ID: " + CURRENCY_ID + ").", backEvent);
		}
		return null;
	}

	/** Ejecuta el teleport: desactiva skills, consume moneda, teletransporta, reactiva skills. */
	private static void executeTeleport(Player player, Location loc)
	{
		if (TELEPORT_PRICE > 0)
		{
			player.destroyItemByItemId(ItemProcessType.FEE, CURRENCY_ID, TELEPORT_PRICE, player, true);
		}
		player.disableAllSkills();
		player.setIn7sDungeon(false);
		player.setInstanceId(0);
		player.teleToLocation(loc, 0);
		ThreadPool.schedule(player::enableAllSkills, 3000);
	}

	/** Retorna un punto de spawn aleatorio de la lista de Zona Party PvP. */
	private static Location getRandomPartySpawn()
	{
		if (PARTY_PVP_SPAWNS.isEmpty())
		{
			return null;
		}
		return PARTY_PVP_SPAWNS.get(Rnd.get(PARTY_PVP_SPAWNS.size()));
	}

	// ---------------------------------------------------------------------------
	// Bypass Handler para invitaciones Party PvP (gk_party_accept / gk_party_decline)
	// Funciona independientemente de si el jugador esta hablando con el NPC.
	// ---------------------------------------------------------------------------
	private static class PartyPvpBypass implements IBypassHandler
	{
		private static final String[] COMMANDS =
		{
			"gk_party_accept",
			"gk_party_decline"
		};

		@Override
		public boolean onCommand(String command, Player player, Creature target)
		{
			if (!ENABLED)
			{
				return false;
			}

			switch (command)
			{
				case "gk_party_accept":
				{
					final Long inviteTime = PENDING_PARTY_INVITES.remove(player.getObjectId());

					if (inviteTime == null)
					{
						player.sendMessage("[Party PvP] No tienes una invitacion pendiente.");
						return false;
					}

					// Verificar que no haya expirado
					if ((System.currentTimeMillis() - inviteTime) > PARTY_PVP_INVITE_EXPIRE_MS)
					{
						player.sendMessage("[Party PvP] La invitacion ha expirado.");
						return false;
					}

					// Verificar restricciones basicas del miembro
					if (BLOCK_PK && (player.getKarma() > 0))
					{
						player.sendMessage("[Party PvP] No puedes ir siendo PK (tienes Karma).");
						return false;
					}
					if (BLOCK_COMBAT && player.isInCombat())
					{
						player.sendMessage("[Party PvP] No puedes ir estando en combate.");
						return false;
					}
					if (BLOCK_OLYMPIAD && (player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player)))
					{
						player.sendMessage("[Party PvP] No puedes ir estando en Olimpiadas.");
						return false;
					}

					// Verificar precio
					if ((TELEPORT_PRICE > 0) && (player.getInventory().getInventoryItemCount(CURRENCY_ID, -1) < TELEPORT_PRICE))
					{
						player.sendMessage("[Party PvP] Moneda insuficiente para el teleport.");
						return false;
					}

					// Obtener spawn aleatorio
					final Location spawn = getRandomPartySpawn();
					if (spawn == null)
					{
						player.sendMessage("[Party PvP] La zona no esta configurada.");
						return false;
					}

					// Teletransportar
					executeTeleport(player, spawn);
					player.sendMessage("[Party PvP] Bienvenido a " + PARTY_PVP_ZONE_NAME + "!");
					return true;
				}

				case "gk_party_decline":
				{
					final boolean hadInvite = PENDING_PARTY_INVITES.remove(player.getObjectId()) != null;
					if (hadInvite)
					{
						player.sendMessage("[Party PvP] Rechazaste la invitacion a " + PARTY_PVP_ZONE_NAME + ".");
					}
					return true;
				}
			}

			return false;
		}

		@Override
		public String[] getCommandList()
		{
			return COMMANDS;
		}
	}

	// ---------------------------------------------------------------------------
	// HTML Builders
	// ---------------------------------------------------------------------------

	/** Pagina principal: menu de categorias con indicador de restricciones activas. */
	private String buildMainPage(Player player)
	{
		final boolean isPK = BLOCK_PK && (player.getKarma() > 0);
		final boolean inCombat = BLOCK_COMBAT && player.isInCombat();
		final boolean inOlympiad = BLOCK_OLYMPIAD && (player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player));
		final boolean hasRestriction = isPK || inCombat || inOlympiad;

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table width=270 cellpadding=0 cellspacing=2>");

		sb.append("<tr><td align=center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></td></tr>");
		sb.append("<tr><td align=center><font color=\"LEVEL\">Fiorella</font></td></tr>");
		sb.append("<tr><td align=center><font color=\"B09878\">Gatekeeper Global</font></td></tr>");
		sb.append("<tr><td align=center><img src=\"L2UI.SquareGray\" width=250 height=1></td></tr>");
		sb.append("<tr><td height=4></td></tr>");

		if (hasRestriction)
		{
			sb.append("<tr><td>");
			sb.append("<table width=250 bgcolor=220000 cellpadding=3>");
			sb.append("<tr><td align=center><font color=\"FF6666\">Restricciones Activas</font></td></tr>");
			if (isPK) sb.append("<tr><td><font color=\"FF9999\"> - Tienes Karma (PK activo)</font></td></tr>");
			if (inCombat) sb.append("<tr><td><font color=\"FF9999\"> - Estas en modo combate</font></td></tr>");
			if (inOlympiad) sb.append("<tr><td><font color=\"FF9999\"> - Registrado en Olimpiadas</font></td></tr>");
			sb.append("</table></td></tr>");
			sb.append("<tr><td height=4></td></tr>");
		}

		if (TELEPORT_PRICE > 0)
		{
			sb.append("<tr><td align=center><font color=\"808080\">Precio: ").append(TELEPORT_PRICE).append(" por teleport</font></td></tr>");
			sb.append("<tr><td height=3></td></tr>");
		}

		appendButton(sb, "Ciudades", "bypass -h Script GlobalGatekeeper towns");
		appendButton(sb, "Zonas de Castillo", "bypass -h Script GlobalGatekeeper castles");
		appendButton(sb, "Fortalezas", "bypass -h Script GlobalGatekeeper fortress");
		appendButton(sb, "Zonas Farm", "bypass -h Script GlobalGatekeeper farm");

		sb.append("<tr><td height=4></td></tr>");
		sb.append("<tr><td align=center><img src=\"L2UI.SquareGray\" width=250 height=1></td></tr>");
		sb.append("<tr><td height=4></td></tr>");

		sb.append("<tr><td align=center><font color=\"C8A84B\">Zonas Especiales</font></td></tr>");
		sb.append("<tr><td height=2></td></tr>");
		appendButton(sb, CLAN_PVP_ZONE_NAME + " [Clan]", "bypass -h Script GlobalGatekeeper clanpvp");
		appendButton(sb, PARTY_PVP_ZONE_NAME + " [Party]", "bypass -h Script GlobalGatekeeper partypvp");

		sb.append("<tr><td height=4></td></tr>");
		sb.append("<tr><td align=center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></td></tr>");
		sb.append("</table></body></html>");
		return sb.toString();
	}

	/**
	 * Pagina de lista de teleports para una categoria.
	 * @param title     Titulo mostrado en cabecera
	 * @param locations Mapa de destinos
	 * @param typeKey   Prefijo de evento (town, castle, fort, feasy, fmed, fhard)
	 * @param backEvent Evento del boton Atras
	 */
	private String buildListPage(String title, Map<String, Location> locations, String typeKey, String backEvent)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table width=270 cellpadding=0 cellspacing=2>");

		sb.append("<tr><td align=center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></td></tr>");
		sb.append("<tr><td align=center><font color=\"LEVEL\">").append(title).append("</font></td></tr>");
		sb.append("<tr><td align=center><img src=\"L2UI.SquareGray\" width=250 height=1></td></tr>");
		sb.append("<tr><td height=4></td></tr>");

		if (locations.isEmpty())
		{
			sb.append("<tr><td align=center><font color=\"808080\">No hay destinos configurados.</font></td></tr>");
			sb.append("<tr><td align=center><font color=\"606060\">(Edita GlobalGatekeeper.ini)</font></td></tr>");
		}
		else
		{
			int index = 0;
			for (String name : locations.keySet())
			{
				appendButton(sb, name, "bypass -h Script GlobalGatekeeper tp_" + typeKey + "_" + index);
				index++;
			}
		}

		sb.append("<tr><td height=4></td></tr>");
		sb.append("<tr><td align=center><img src=\"L2UI.SquareGray\" width=250 height=1></td></tr>");
		sb.append("<tr><td height=3></td></tr>");
		sb.append("<tr><td align=center>");
		sb.append("<button value=\"Atras\" action=\"bypass -h Script GlobalGatekeeper ").append(backEvent).append("\" width=120 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		sb.append("</td></tr>");
		sb.append("</table></body></html>");
		return sb.toString();
	}

	/** Menu de Zonas Farm con submenu por dificultad. */
	private String buildFarmMenu()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table width=270 cellpadding=0 cellspacing=2>");

		sb.append("<tr><td align=center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></td></tr>");
		sb.append("<tr><td align=center><font color=\"LEVEL\">Zonas Farm</font></td></tr>");
		sb.append("<tr><td align=center><font color=\"808080\">Elige la dificultad</font></td></tr>");
		sb.append("<tr><td align=center><img src=\"L2UI.SquareGray\" width=250 height=1></td></tr>");
		sb.append("<tr><td height=6></td></tr>");

		sb.append("<tr><td align=center><font color=\"44BB44\">-- Baja Dificultad --</font></td></tr>");
		sb.append("<tr><td height=2></td></tr>");
		appendButton(sb, "Zona Farm Facil (" + FARM_EASY.size() + " zonas)", "bypass -h Script GlobalGatekeeper farm_easy");
		sb.append("<tr><td height=6></td></tr>");

		sb.append("<tr><td align=center><font color=\"FFAA00\">-- Dificultad Media --</font></td></tr>");
		sb.append("<tr><td height=2></td></tr>");
		appendButton(sb, "Zona Farm Media (" + FARM_MEDIUM.size() + " zonas)", "bypass -h Script GlobalGatekeeper farm_medium");
		sb.append("<tr><td height=6></td></tr>");

		sb.append("<tr><td align=center><font color=\"FF5555\">-- Alta Dificultad --</font></td></tr>");
		sb.append("<tr><td height=2></td></tr>");
		appendButton(sb, "Zona Farm Dificil (" + FARM_HARD.size() + " zonas)", "bypass -h Script GlobalGatekeeper farm_hard");

		sb.append("<tr><td height=4></td></tr>");
		sb.append("<tr><td align=center><img src=\"L2UI.SquareGray\" width=250 height=1></td></tr>");
		sb.append("<tr><td height=3></td></tr>");
		sb.append("<tr><td align=center>");
		sb.append("<button value=\"Atras\" action=\"bypass -h Script GlobalGatekeeper main\" width=120 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		sb.append("</td></tr>");
		sb.append("</table></body></html>");
		return sb.toString();
	}

	/**
	 * Pagina del evento Clan PvP Zone.
	 * Muestra el estado del evento e integra el boton de inscripcion
	 * que llama al bypass handler de ClanPvpZone.
	 */
	private String buildClanPvpPage(Player player)
	{
		final ClanPvpZone clanEvent = ClanPvpZone.getInstance();
		final boolean hasClan = player.getClan() != null;

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table width=270 cellpadding=0 cellspacing=0>");

		// Cabecera
		sb.append("<tr><td align=center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></td></tr>");
		sb.append("<tr><td height=2></td></tr>");
		sb.append("<tr><td align=center><font color=\"C8A84B\">").append(CLAN_PVP_ZONE_NAME).append("</font></td></tr>");
		sb.append("<tr><td align=center><font color=\"707070\">Evento de Clanes con RaidBoss</font></td></tr>");
		sb.append("<tr><td height=3></td></tr>");
		sb.append("<tr><td align=center><img src=\"L2UI.SquareGray\" width=256 height=1></td></tr>");
		sb.append("<tr><td height=5></td></tr>");

		if (clanEvent == null)
		{
			sb.append("<tr><td align=center><font color=\"FF5555\">El evento no esta disponible.</font></td></tr>");
		}
		else
		{
			final ClanPvpZone.EventState state = clanEvent.getState();
			final int registered = clanEvent.getRegisteredClanCount();
			final int minClans = clanEvent.getMinClans();
			final int maxClans = clanEvent.getMaxClans();

			// --- Bloque de estado ---
			sb.append("<tr><td align=center>");
			sb.append("<table width=256 bgcolor=0A0A0A cellpadding=0 cellspacing=0>");

			// Fila de estado
			switch (state)
			{
				case IDLE:
					sb.append("<tr><td align=center height=22><font color=\"AAAAAA\">Estado: </font><font color=\"C8A84B\">Abierto - Inscripciones</font></td></tr>");
					break;
				case COUNTDOWN:
					final int sec = clanEvent.getCountdownSeconds();
					final String timeLeft = (sec >= 60) ? (sec / 60) + "m " + (sec % 60) + "s" : sec + "s";
					sb.append("<tr><td align=center height=22><font color=\"FFAA00\">Cuenta regresiva: </font><font color=\"FF6347\"><b>").append(timeLeft).append("</b></font></td></tr>");
					break;
				case ACTIVE:
					sb.append("<tr><td align=center height=22><font color=\"FF5555\">COMBATE EN PROGRESO</font></td></tr>");
					break;
				case RAID:
					sb.append("<tr><td align=center height=22><font color=\"FF0000\">RAIDBOSS ACTIVO</font></td></tr>");
					break;
			}

			sb.append("<tr><td><img src=\"L2UI.SquareGray\" width=256 height=1></td></tr>");

			// Contador de clanes (solo en IDLE y COUNTDOWN)
			if ((state == ClanPvpZone.EventState.IDLE) || (state == ClanPvpZone.EventState.COUNTDOWN))
			{
				sb.append("<tr><td align=center height=20>");
				sb.append("<font color=\"9A9280\">Clanes: </font>");
				sb.append("<font color=\"FFDF00\">").append(registered).append("</font>");
				sb.append("<font color=\"707070\"> / ").append(maxClans).append(" </font>");
				sb.append("<font color=\"606060\">(min: ").append(minClans).append(")</font>");
				sb.append("</td></tr>");

				// Lista de clanes inscritos
				final Collection<String> names = clanEvent.getRegisteredClanNames();
				if (!names.isEmpty())
				{
					sb.append("<tr><td><img src=\"L2UI.SquareGray\" width=256 height=1></td></tr>");
					sb.append("<tr><td align=center height=4></td></tr>");
					for (String clanName : names)
					{
						sb.append("<tr><td align=center>");
						sb.append("<font color=\"C8A84B\">&lt;").append(clanName).append("&gt;</font>");
						sb.append("</td></tr>");
					}
					sb.append("<tr><td height=4></td></tr>");
				}
				else
				{
					sb.append("<tr><td height=4></td></tr>");
				}
			}
			else
			{
				sb.append("<tr><td align=center height=20><font color=\"707070\">No se puede ingresar durante el evento.</font></td></tr>");
				sb.append("<tr><td height=4></td></tr>");
			}

			sb.append("</table>");
			sb.append("</td></tr>");
			sb.append("<tr><td height=6></td></tr>");

			// --- Descripcion del evento (tabla con ancho fijo para evitar overflow) ---
			sb.append("<tr><td align=center>");
			sb.append("<table width=256 cellpadding=3 cellspacing=0>");
			sb.append("<tr><td><font color=\"9A9280\">");
			sb.append("Cada kill otorga <font color=\"C8A84B\">reputacion</font> a tu clan.<br>");
			sb.append("El ultimo clan en pie enfrenta al <font color=\"FF5555\">RaidBoss</font>.<br>");
			sb.append("El clan vencedor recibe <font color=\"FFDF00\">recompensa</font> por miembro.");
			sb.append("</font></td></tr>");
			sb.append("</table>");
			sb.append("</td></tr>");
			sb.append("<tr><td height=6></td></tr>");

			// --- Boton de accion segun estado ---
			if (!hasClan)
			{
				sb.append("<tr><td align=center><font color=\"FF5555\">[X] Debes pertenecer a un clan.</font></td></tr>");
			}
			else if ((state == ClanPvpZone.EventState.IDLE) || (state == ClanPvpZone.EventState.COUNTDOWN))
			{
				final int clanId = player.getClan().getId();
				final boolean isLeader = player.getClan().getLeaderId() == player.getObjectId();

				if (clanEvent.isClanOnCooldown(clanId))
				{
					sb.append("<tr><td align=center>");
					sb.append("<table width=256 bgcolor=1A1000 cellpadding=3><tr><td align=center>");
					sb.append("<font color=\"FFAA00\">Tu clan gano el ultimo evento.</font><br>");
					sb.append("<font color=\"707070\">Espera a que otro clan gane primero.</font>");
					sb.append("</td></tr></table></td></tr>");
				}
				else if (clanEvent.isClanRegistered(clanId))
				{
					sb.append("<tr><td align=center>");
					sb.append("<table width=256 bgcolor=001A00 cellpadding=3><tr><td align=center>");
					sb.append("<font color=\"44BB44\">[OK] Tu clan esta inscrito.</font><br>");
					sb.append("<font color=\"707070\">En espera de inicio del evento.</font>");
					sb.append("</td></tr></table></td></tr>");
					if (isLeader)
					{
						sb.append("<tr><td height=3></td></tr>");
						appendButton(sb, "Cancelar Inscripcion", "bypass -h clanpvz_unregister");
					}
				}
				else if (registered >= maxClans)
				{
					sb.append("<tr><td align=center><font color=\"FF5555\">Cupo lleno (").append(maxClans).append(" clanes).</font></td></tr>");
				}
				else if (!isLeader)
				{
					sb.append("<tr><td align=center><font color=\"FFAA00\">[!] Solo el lider del clan puede inscribirse.</font></td></tr>");
				}
				else
				{
					sb.append("<tr><td height=2></td></tr>");
					appendButton(sb, "Inscribir Clan al Evento", "bypass -h clanpvz_register");
				}
			}
			else
			{
				sb.append("<tr><td align=center><font color=\"707070\">El evento esta en progreso.<br>Espera la proxima ronda.</font></td></tr>");
			}
		}

		// Pie de pagina
		sb.append("<tr><td height=6></td></tr>");
		sb.append("<tr><td align=center><img src=\"L2UI.SquareGray\" width=256 height=1></td></tr>");
		sb.append("<tr><td height=4></td></tr>");
		sb.append("<tr><td align=center>");
		sb.append("<button value=\"Atras\" action=\"bypass -h Script GlobalGatekeeper main\" width=120 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		sb.append("</td></tr>");
		sb.append("<tr><td height=4></td></tr>");
		sb.append("</table></body></html>");
		return sb.toString();
	}

	/**
	 * Pagina de info de la Zona Party PvP.
	 * Muestra el boton "Llevar al grupo" solo si el jugador es lider de party.
	 * Si no es lider, muestra un aviso explicativo.
	 */
	private String buildPartyPvpPage(Player player)
	{
		final boolean isInParty = player.isInParty();
		final boolean isLeader = isInParty && (player.getParty().getLeader() == player);
		final int partySize = isInParty ? player.getParty().getMemberCount() : 0;

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table width=270 cellpadding=0 cellspacing=2>");

		sb.append("<tr><td align=center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></td></tr>");
		sb.append("<tr><td align=center><font color=\"C8A84B\">").append(PARTY_PVP_ZONE_NAME).append("</font></td></tr>");
		sb.append("<tr><td align=center><img src=\"L2UI.SquareGray\" width=250 height=1></td></tr>");
		sb.append("<tr><td height=5></td></tr>");

		sb.append("<tr><td><font color=\"9A9280\">");
		sb.append("Zona exclusiva para grupos.<br>");
		sb.append("El PvP esta siempre activo en este area.<br><br>");
		sb.append("<font color=\"C8A84B\">Solo el lider del grupo puede usar<br>este teleport.</font><br><br>");
		sb.append("Al activarlo, cada miembro recibira<br>");
		sb.append("una invitacion para unirse.<br>");
		sb.append("<font color=\"707070\">(" + PARTY_PVP_SPAWNS.size() + " puntos de spawn aleatorios)</font>");
		sb.append("</font></td></tr>");
		sb.append("<tr><td height=8></td></tr>");

		if (isInParty)
		{
			if (isLeader)
			{
				sb.append("<tr><td align=center><font color=\"44BB44\">[OK] Lider de grupo (").append(partySize).append(" miembros)</font></td></tr>");
				sb.append("<tr><td height=4></td></tr>");
				appendButton(sb, "Llevar grupo a " + PARTY_PVP_ZONE_NAME, "bypass -h Script GlobalGatekeeper tp_partypvp");
			}
			else
			{
				final String leaderName = player.getParty().getLeader().getName();
				sb.append("<tr><td align=center><font color=\"FFAA00\">[!] No eres el lider del grupo</font></td></tr>");
				sb.append("<tr><td align=center><font color=\"707070\">Lider: ").append(leaderName).append("</font></td></tr>");
				sb.append("<tr><td align=center><font color=\"9A9280\">Pidele al lider que use el teleport.</font></td></tr>");
			}
		}
		else
		{
			sb.append("<tr><td align=center><font color=\"FF5555\">[X] No estas en ningun grupo</font></td></tr>");
		}

		sb.append("<tr><td height=4></td></tr>");
		sb.append("<tr><td align=center><img src=\"L2UI.SquareGray\" width=250 height=1></td></tr>");
		sb.append("<tr><td height=3></td></tr>");
		sb.append("<tr><td align=center>");
		sb.append("<button value=\"Atras\" action=\"bypass -h Script GlobalGatekeeper main\" width=120 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		sb.append("</td></tr>");
		sb.append("</table></body></html>");
		return sb.toString();
	}

	/** Pagina de error con mensaje y boton de regreso. */
	private String buildErrorPage(String message, String backEvent)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table width=270 cellpadding=0 cellspacing=2>");
		sb.append("<tr><td align=center><font color=\"FF4444\">Acceso Denegado</font></td></tr>");
		sb.append("<tr><td align=center><img src=\"L2UI.SquareGray\" width=250 height=1></td></tr>");
		sb.append("<tr><td height=6></td></tr>");
		sb.append("<tr><td><font color=\"C0B090\">").append(message).append("</font></td></tr>");
		sb.append("<tr><td height=10></td></tr>");
		sb.append("<tr><td align=center>");
		sb.append("<button value=\"Atras\" action=\"bypass -h Script GlobalGatekeeper ").append(backEvent).append("\" width=150 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		sb.append("</td></tr>");
		sb.append("</table></body></html>");
		return sb.toString();
	}

	// ---------------------------------------------------------------------------
	// HTML Helpers
	// ---------------------------------------------------------------------------

	private void appendButton(StringBuilder sb, String label, String action)
	{
		sb.append("<tr><td align=center>");
		sb.append("<button value=\"").append(label).append("\" action=\"").append(action)
			.append("\" width=250 height=26 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		sb.append("</td></tr>");
	}

	private Location getLocationByIndex(Map<String, Location> map, int index)
	{
		int i = 0;
		for (Location loc : map.values())
		{
			if (i == index)
			{
				return loc;
			}
			i++;
		}
		return null;
	}

	// ---------------------------------------------------------------------------
	// Entry Point
	// ---------------------------------------------------------------------------
	public static void main(String[] args)
	{
		new GlobalGatekeeper();
	}
}
