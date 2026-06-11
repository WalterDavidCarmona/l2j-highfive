/*
 * Custom Vote System - Hopzone only
 */
package custom.VoteSystem;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.util.Broadcast;

public class VoteSystem extends Script
{
	// ---------------------------------------------------------------------------
	// Singleton
	// ---------------------------------------------------------------------------
	private static VoteSystem _instance;

	public static VoteSystem getInstance()
	{
		return _instance;
	}

	// Indica si el comando de voz .getreward esta habilitado y operativo.
	public static boolean isVoiceCommandEnabled()
	{
		return ENABLED && ENABLE_VOICE_COMMAND;
	}

	// ---------------------------------------------------------------------------
	// Constants
	// ---------------------------------------------------------------------------
	// Ordinal usado en las tablas vote_system_global / vote_system_individual.
	private static final int HOPZONE_ORDINAL = 0;
	private static final String SITE_NAME = "Hopzone";

	// ---------------------------------------------------------------------------
	// Config
	// ---------------------------------------------------------------------------
	private static boolean ENABLED = false;
	private static int CHECK_INTERVAL_MINUTES = 5;
	private static int GLOBAL_THRESHOLD = 20;
	private static int INDIVIDUAL_COOLDOWN_HOURS = 12;
	private static boolean ENABLE_GLOBAL_REWARD = true;
	private static boolean ENABLE_INDIVIDUAL_REWARD = true;
	private static boolean ENABLE_VOICE_COMMAND = true;
	private static boolean ANNOUNCE_PROGRESS = true;
	private static int GLOBAL_REWARD_ITEM_ID = 57;
	private static long GLOBAL_REWARD_COUNT = 1000000;
	private static int INDIVIDUAL_REWARD_ITEM_ID = 6673;
	private static long INDIVIDUAL_REWARD_COUNT = 1;

	// Hopzone endpoints
	private static String GLOBAL_URL = "";
	private static String INDIVIDUAL_URL = "";
	private static String SITE_URL = "https://l2.hopzone.net";

	// Ultimo conteo de votos anunciado (para no repetir el aviso sin votos nuevos).
	private static volatile int _lastAnnouncedVotes = -1;

	// ---------------------------------------------------------------------------
	// Constructor
	// ---------------------------------------------------------------------------
	public VoteSystem()
	{
		_instance = this;
		loadConfig();

		if (!ENABLED)
		{
			LOGGER.info("VoteSystem: Disabled.");
			return;
		}

		if (GLOBAL_URL.isEmpty() && INDIVIDUAL_URL.isEmpty())
		{
			LOGGER.warning("VoteSystem: Hopzone no esta configurado (URLs vacias). Sistema inactivo.");
			return;
		}

		if (ENABLE_GLOBAL_REWARD && !GLOBAL_URL.isEmpty())
		{
			final long intervalMs = CHECK_INTERVAL_MINUTES * 60000L;
			ThreadPool.scheduleAtFixedRate(VoteSystem::checkGlobalVotes, intervalMs, intervalMs);
		}

		LOGGER.info("VoteSystem: Loaded (Hopzone)."
			+ " Interval=" + CHECK_INTERVAL_MINUTES + "min"
			+ " GlobalThreshold=" + GLOBAL_THRESHOLD
			+ " IndividualCooldown=" + INDIVIDUAL_COOLDOWN_HOURS + "h"
			+ " AnnounceProgress=" + ANNOUNCE_PROGRESS);
	}

	// ---------------------------------------------------------------------------
	// Config loader
	// ---------------------------------------------------------------------------
	private void loadConfig()
	{
		final Properties props = new Properties();
		try (InputStream is = new FileInputStream("./config/Custom/VoteSystem.ini"))
		{
			props.load(is);
		}
		catch (Exception e)
		{
			LOGGER.warning("VoteSystem: Could not load config: " + e.getMessage());
			return;
		}

		ENABLED = Boolean.parseBoolean(props.getProperty("Enabled", "False").trim());
		CHECK_INTERVAL_MINUTES = Integer.parseInt(props.getProperty("CheckIntervalMinutes", "5").trim());
		GLOBAL_THRESHOLD = Integer.parseInt(props.getProperty("GlobalVotesThreshold", "20").trim());
		INDIVIDUAL_COOLDOWN_HOURS = Integer.parseInt(props.getProperty("IndividualCooldownHours", "12").trim());
		ENABLE_GLOBAL_REWARD = Boolean.parseBoolean(props.getProperty("EnableGlobalReward", "True").trim());
		ENABLE_INDIVIDUAL_REWARD = Boolean.parseBoolean(props.getProperty("EnableIndividualReward", "True").trim());
		ENABLE_VOICE_COMMAND = Boolean.parseBoolean(props.getProperty("EnableVoteCommand", "True").trim());
		ANNOUNCE_PROGRESS = Boolean.parseBoolean(props.getProperty("AnnounceProgress", "True").trim());
		GLOBAL_REWARD_ITEM_ID = Integer.parseInt(props.getProperty("GlobalRewardItemId", "57").trim());
		GLOBAL_REWARD_COUNT = Long.parseLong(props.getProperty("GlobalRewardCount", "1000000").trim());
		INDIVIDUAL_REWARD_ITEM_ID = Integer.parseInt(props.getProperty("IndividualRewardItemId", "6673").trim());
		INDIVIDUAL_REWARD_COUNT = Long.parseLong(props.getProperty("IndividualRewardCount", "1").trim());

		if (!Boolean.parseBoolean(props.getProperty("HopzoneEnabled", "False").trim()))
		{
			ENABLED = false;
			return;
		}

		final String token = props.getProperty("HopzoneToken", "").trim();
		GLOBAL_URL = props.getProperty("HopzoneGlobalUrl", "").trim().replace("%TOKEN%", token);
		INDIVIDUAL_URL = props.getProperty("HopzoneIndividualUrl", "").trim().replace("%TOKEN%", token);
		SITE_URL = props.getProperty("HopzoneSiteUrl", "https://l2.hopzone.net").trim();
	}

	// ---------------------------------------------------------------------------
	// Global vote polling (scheduled, runs every CheckIntervalMinutes)
	// ---------------------------------------------------------------------------
	private static void checkGlobalVotes()
	{
		if (GLOBAL_URL.isEmpty())
		{
			return;
		}
		try
		{
			final String response = httpGet(GLOBAL_URL);
			if (response == null)
			{
				return;
			}
			final int currentVotes = parseVoteCount(response);
			if (currentVotes < 0)
			{
				LOGGER.warning("VoteSystem: Could not parse vote count from " + SITE_NAME + ": " + response);
				return;
			}

			final int lastVotes = loadLastRewardVotes(HOPZONE_ORDINAL);
			final int progress = currentVotes - lastVotes;

			if (progress >= GLOBAL_THRESHOLD)
			{
				rewardAllPlayers(SITE_NAME);
				saveLastRewardVotes(HOPZONE_ORDINAL, currentVotes);
				_lastAnnouncedVotes = currentVotes;
				LOGGER.info("VoteSystem: Global reward triggered by " + SITE_NAME
					+ " (" + lastVotes + " -> " + currentVotes + " votes)");
				return;
			}

			// Anuncio de progreso: solo cuando hay votos nuevos desde el ultimo aviso.
			if (ANNOUNCE_PROGRESS && (currentVotes != _lastAnnouncedVotes))
			{
				final int remaining = GLOBAL_THRESHOLD - progress;
				if (remaining > 0)
				{
					Broadcast.toAllOnlinePlayers("[Votos] Faltan " + remaining + " voto(s) en " + SITE_NAME
						+ " para la recompensa global. Vota en: " + SITE_URL, false);
				}
				_lastAnnouncedVotes = currentVotes;
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("VoteSystem: Error polling " + SITE_NAME + ": " + e.getMessage());
		}
	}

	private static void rewardAllPlayers(String siteName)
	{
		int count = 0;
		for (Player player : World.getInstance().getPlayers())
		{
			if ((player != null) && player.isOnline())
			{
				player.addItem(ItemProcessType.REWARD, GLOBAL_REWARD_ITEM_ID, GLOBAL_REWARD_COUNT, player, true);
				count++;
			}
		}
		Broadcast.toAllOnlinePlayers("[Vote Reward] ¡Gracias por votar en " + siteName + "! Todos los jugadores en linea recibieron su recompensa.", false);
		LOGGER.info("VoteSystem: Global reward (" + GLOBAL_REWARD_COUNT + "x item " + GLOBAL_REWARD_ITEM_ID + ") given to " + count + " player(s).");
	}

	// ---------------------------------------------------------------------------
	// Vote info page — abierta con .getreward dentro del Community Board.
	// El boton de reclamar usa el bypass _bbsvote.
	// ---------------------------------------------------------------------------
	public static void sendVotePage(Player player)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<center>");
		sb.append("<table width=300 bgcolor=000000><tr><td align=center>");
		sb.append("<font color=\"C8A84B\">VOTA Y RECIBE RECOMPENSAS</font>");
		sb.append("</td></tr></table><br>");
		sb.append("<font color=\"7A7060\">Cada voto ayuda al servidor a crecer.</font><br>");
		sb.append("<font color=\"7A7060\">Puedes votar cada 12 horas.</font><br><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=300 height=1><br><br>");
		sb.append("<font color=\"9A9280\">Sitio de votacion:</font><br>");
		sb.append("<table width=260 bgcolor=0A0A1E><tr><td align=center>");
		sb.append("<font color=\"C8A84B\">HOPZONE</font></td></tr>");
		sb.append("<tr><td align=center><font color=\"7A7060\">" + SITE_URL + "</font></td></tr></table><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=300 height=1><br><br>");
		sb.append("<font color=\"9A9280\">Como reclamar:</font><br>");
		sb.append("<font color=\"7A7060\">1. Vota en Hopzone desde tu navegador.</font><br>");
		sb.append("<font color=\"7A7060\">2. Pulsa el boton de abajo.</font><br>");
		sb.append("<font color=\"7A7060\">3. La recompensa llega a tu inventario.</font><br><br>");
		sb.append("<button value=\"Reclamar Recompensa de Voto\" action=\"bypass _bbsvote\" width=220 height=30 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br>");
		sb.append("</center>");
		sb.append("</body></html>");

		CommunityBoardHandler.separateAndSend(sb.toString(), player);
	}

	// ---------------------------------------------------------------------------
	// Individual vote claim — called from HomeBoard via claimReward()
	// ---------------------------------------------------------------------------
	public static void claimReward(Player player)
	{
		if (!ENABLE_INDIVIDUAL_REWARD)
		{
			player.sendMessage("[Votos] Las recompensas individuales no estan habilitadas.");
			return;
		}

		if (INDIVIDUAL_URL.isEmpty())
		{
			player.sendMessage("[Votos] No hay sitio de votacion configurado.");
			return;
		}

		final String ip = player.getIPAddress();
		if ((ip == null) || ip.isEmpty())
		{
			player.sendMessage("[Votos] No se pudo obtener tu IP.");
			return;
		}

		final long cooldownMs = INDIVIDUAL_COOLDOWN_HOURS * 3600000L;
		final long now = System.currentTimeMillis();

		final long lastRewardTime = loadIndividualRewardTime(ip, HOPZONE_ORDINAL);
		if ((now - lastRewardTime) < cooldownMs)
		{
			final long remainMs = cooldownMs - (now - lastRewardTime);
			final long h = remainMs / 3600000L;
			final long m = (remainMs % 3600000L) / 60000L;
			player.sendMessage("[" + SITE_NAME + "] Ya reclamaste tu recompensa. Proxima en: " + h + "h " + m + "m.");
			return;
		}

		try
		{
			final String checkUrl = INDIVIDUAL_URL.replace("%IP%", ip);
			final String response = httpGet(checkUrl);
			if (response == null)
			{
				player.sendMessage("[" + SITE_NAME + "] No se pudo verificar tu voto. Intenta mas tarde.");
				return;
			}

			if (hasVoted(response))
			{
				player.addItem(ItemProcessType.REWARD, INDIVIDUAL_REWARD_ITEM_ID, INDIVIDUAL_REWARD_COUNT, player, true);
				saveIndividualRewardTime(ip, HOPZONE_ORDINAL, now);
				player.sendMessage("[" + SITE_NAME + "] ¡Gracias por votar! Recompensa entregada.");
				LOGGER.info("VoteSystem: Individual reward -> " + player.getName() + " (" + ip + ") [" + SITE_NAME + "]");
			}
			else
			{
				player.sendMessage("[" + SITE_NAME + "] Voto no encontrado. Vota en: " + SITE_URL);
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("VoteSystem: Individual check error [" + SITE_NAME + "] player=" + player.getName() + ": " + e.getMessage());
			player.sendMessage("[" + SITE_NAME + "] Error al verificar. Intenta mas tarde.");
		}
	}

	// ---------------------------------------------------------------------------
	// API response parsers
	// ---------------------------------------------------------------------------
	private static int parseVoteCount(String response)
	{
		response = response.trim();
		try
		{
			return Integer.parseInt(response);
		}
		catch (NumberFormatException ignored)
		{
		}

		// Hopzone.net API: {"apiver":"0.1","totalvotes":8,"status_code":200}
		final int idx = response.indexOf("\"totalvotes\"");
		if (idx >= 0)
		{
			final int v = extractJsonInt(response, idx + 12);
			if (v >= 0)
			{
				return v;
			}
		}

		return -1;
	}

	private static int extractJsonInt(String json, int fromIdx)
	{
		int i = fromIdx;
		while ((i < json.length()) && ((json.charAt(i) == ' ') || (json.charAt(i) == ':')))
		{
			i++;
		}
		if ((i < json.length()) && (json.charAt(i) == '"'))
		{
			i++;
		}
		final int start = i;
		while ((i < json.length()) && Character.isDigit(json.charAt(i)))
		{
			i++;
		}
		if (i > start)
		{
			try
			{
				return Integer.parseInt(json.substring(start, i));
			}
			catch (NumberFormatException ignored)
			{
			}
		}
		return -1;
	}

	private static boolean hasVoted(String response)
	{
		final String r = response.trim();
		if ("1".equals(r) || "true".equalsIgnoreCase(r))
		{
			return true;
		}
		// Hopzone.net API: {"apiver":"0.1","voted":true,"voteTime":"...","status_code":200}
		return r.contains("\"voted\":true") || r.contains("\"voted\": true");
	}

	// ---------------------------------------------------------------------------
	// HTTP helper
	// ---------------------------------------------------------------------------
	private static String httpGet(String urlStr)
	{
		try
		{
			final URL url = new URL(urlStr);
			final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			conn.setRequestProperty("User-Agent", "L2J-VoteSystem/1.0");

			if (conn.getResponseCode() != 200)
			{
				conn.disconnect();
				return null;
			}

			final StringBuilder sb = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream())))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					sb.append(line);
				}
			}
			conn.disconnect();
			return sb.toString();
		}
		catch (Exception e)
		{
			LOGGER.warning("VoteSystem: HTTP error [" + urlStr + "]: " + e.getMessage());
			return null;
		}
	}

	// ---------------------------------------------------------------------------
	// DB helpers
	// ---------------------------------------------------------------------------
	private static int loadLastRewardVotes(int siteOrdinal)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT lastRewardVotes FROM vote_system_global WHERE voteSite = ?"))
		{
			ps.setInt(1, siteOrdinal);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return rs.getInt("lastRewardVotes");
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("VoteSystem: DB error loadLastRewardVotes site=" + siteOrdinal + ": " + e.getMessage());
		}
		return 0;
	}

	private static void saveLastRewardVotes(int siteOrdinal, int votes)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"INSERT INTO vote_system_global (voteSite, lastRewardVotes) VALUES (?, ?) ON DUPLICATE KEY UPDATE lastRewardVotes = VALUES(lastRewardVotes)"))
		{
			ps.setInt(1, siteOrdinal);
			ps.setInt(2, votes);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("VoteSystem: DB error saveLastRewardVotes site=" + siteOrdinal + ": " + e.getMessage());
		}
	}

	private static long loadIndividualRewardTime(String ip, int siteOrdinal)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"SELECT rewardTime FROM vote_system_individual WHERE voterIp = ? AND voteSite = ?"))
		{
			ps.setString(1, ip);
			ps.setInt(2, siteOrdinal);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return rs.getLong("rewardTime");
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("VoteSystem: DB error loadIndividualRewardTime ip=" + ip + " site=" + siteOrdinal + ": " + e.getMessage());
		}
		return 0;
	}

	private static void saveIndividualRewardTime(String ip, int siteOrdinal, long time)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"INSERT INTO vote_system_individual (voterIp, voteSite, rewardTime) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE rewardTime = VALUES(rewardTime)"))
		{
			ps.setString(1, ip);
			ps.setInt(2, siteOrdinal);
			ps.setLong(3, time);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("VoteSystem: DB error saveIndividualRewardTime ip=" + ip + " site=" + siteOrdinal + ": " + e.getMessage());
		}
	}

	// ---------------------------------------------------------------------------
	// Entry point
	// ---------------------------------------------------------------------------
	public static void main(String[] args)
	{
		new VoteSystem();
	}
}
