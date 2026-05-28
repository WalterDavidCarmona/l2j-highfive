/*
 * Custom Vote System
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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
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

	// ---------------------------------------------------------------------------
	// Config
	// ---------------------------------------------------------------------------
	private static boolean ENABLED = false;
	private static int CHECK_INTERVAL_MINUTES = 5;
	private static int GLOBAL_THRESHOLD = 20;
	private static int INDIVIDUAL_COOLDOWN_HOURS = 12;
	private static boolean ENABLE_GLOBAL_REWARD = true;
	private static boolean ENABLE_INDIVIDUAL_REWARD = true;
	private static int GLOBAL_REWARD_ITEM_ID = 57;
	private static long GLOBAL_REWARD_COUNT = 1000000;
	private static int INDIVIDUAL_REWARD_ITEM_ID = 6673;
	private static long INDIVIDUAL_REWARD_COUNT = 1;

	static final List<SiteConfig> SITES = new ArrayList<>();

	// ---------------------------------------------------------------------------
	// Site config holder
	// ---------------------------------------------------------------------------
	static class SiteConfig
	{
		final int ordinal;
		final String name;
		final String globalUrl;
		final String individualUrl;
		final String siteUrl;

		SiteConfig(int ordinal, String name, String globalUrl, String individualUrl, String siteUrl)
		{
			this.ordinal = ordinal;
			this.name = name;
			this.globalUrl = globalUrl;
			this.individualUrl = individualUrl;
			this.siteUrl = siteUrl;
		}
	}

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

		if (ENABLE_GLOBAL_REWARD && !SITES.isEmpty())
		{
			final long intervalMs = CHECK_INTERVAL_MINUTES * 60000L;
			ThreadPool.scheduleAtFixedRate(VoteSystem::checkGlobalVotes, intervalMs, intervalMs);
		}

		LOGGER.info("VoteSystem: Loaded " + SITES.size() + " site(s)."
			+ " Interval=" + CHECK_INTERVAL_MINUTES + "min"
			+ " GlobalThreshold=" + GLOBAL_THRESHOLD
			+ " IndividualCooldown=" + INDIVIDUAL_COOLDOWN_HOURS + "h");
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
		GLOBAL_REWARD_ITEM_ID = Integer.parseInt(props.getProperty("GlobalRewardItemId", "57").trim());
		GLOBAL_REWARD_COUNT = Long.parseLong(props.getProperty("GlobalRewardCount", "1000000").trim());
		INDIVIDUAL_REWARD_ITEM_ID = Integer.parseInt(props.getProperty("IndividualRewardItemId", "6673").trim());
		INDIVIDUAL_REWARD_COUNT = Long.parseLong(props.getProperty("IndividualRewardCount", "1").trim());

		SITES.clear();
		final String[][] siteKeys =
		{
			{ "Hopzone", "0", "https://hopzone.eu" },
			{ "L2TopZone", "1", "https://l2topzone.com" },
			{ "ItopZ", "2", "https://itopz.com" },
			{ "TopZone", "3", "https://l2.topgameserver.net" }
		};
		for (String[] sk : siteKeys)
		{
			final String key = sk[0];
			final int ordinal = Integer.parseInt(sk[1]);
			final String defaultSiteUrl = sk[2];
			if (Boolean.parseBoolean(props.getProperty(key + "Enabled", "False").trim()))
			{
				final String token = props.getProperty(key + "Token", "").trim();
				final String globalUrl = props.getProperty(key + "GlobalUrl", "").trim().replace("%TOKEN%", token);
				final String individualUrl = props.getProperty(key + "IndividualUrl", "").trim().replace("%TOKEN%", token);
				SITES.add(new SiteConfig(ordinal, key, globalUrl, individualUrl, defaultSiteUrl));
				LOGGER.info("VoteSystem: Registered site " + key + " (ordinal=" + ordinal + ")");
			}
		}
	}

	// ---------------------------------------------------------------------------
	// Global vote polling (scheduled, runs every CheckIntervalMinutes)
	// ---------------------------------------------------------------------------
	private static void checkGlobalVotes()
	{
		for (SiteConfig site : SITES)
		{
			if (site.globalUrl.isEmpty())
			{
				continue;
			}
			try
			{
				final String response = httpGet(site.globalUrl);
				if (response == null)
				{
					continue;
				}
				final int currentVotes = parseVoteCount(response);
				if (currentVotes < 0)
				{
					LOGGER.warning("VoteSystem: Could not parse vote count from " + site.name + ": " + response);
					continue;
				}
				final int lastVotes = loadLastRewardVotes(site.ordinal);
				if ((currentVotes - lastVotes) >= GLOBAL_THRESHOLD)
				{
					rewardAllPlayers(site.name);
					saveLastRewardVotes(site.ordinal, currentVotes);
					LOGGER.info("VoteSystem: Global reward triggered by " + site.name
						+ " (" + lastVotes + " -> " + currentVotes + " votes)");
				}
			}
			catch (Exception e)
			{
				LOGGER.warning("VoteSystem: Error polling " + site.name + ": " + e.getMessage());
			}
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
	// Individual vote claim — called from HomeBoard via claimReward()
	// ---------------------------------------------------------------------------
	public static void claimReward(Player player)
	{
		if (!ENABLE_INDIVIDUAL_REWARD)
		{
			player.sendMessage("[Votos] Las recompensas individuales no estan habilitadas.");
			return;
		}

		if (SITES.isEmpty())
		{
			player.sendMessage("[Votos] No hay sitios de votacion configurados.");
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

		for (SiteConfig site : SITES)
		{
			if (site.individualUrl.isEmpty())
			{
				continue;
			}

			final long lastRewardTime = loadIndividualRewardTime(ip, site.ordinal);
			if ((now - lastRewardTime) < cooldownMs)
			{
				final long remainMs = cooldownMs - (now - lastRewardTime);
				final long h = remainMs / 3600000L;
				final long m = (remainMs % 3600000L) / 60000L;
				player.sendMessage("[" + site.name + "] Ya reclamaste tu recompensa. Proxima en: " + h + "h " + m + "m.");
				continue;
			}

			try
			{
				final String checkUrl = site.individualUrl.replace("%IP%", ip);
				final String response = httpGet(checkUrl);
				if (response == null)
				{
					player.sendMessage("[" + site.name + "] No se pudo verificar tu voto. Intenta mas tarde.");
					continue;
				}

				if (hasVoted(response))
				{
					player.addItem(ItemProcessType.REWARD, INDIVIDUAL_REWARD_ITEM_ID, INDIVIDUAL_REWARD_COUNT, player, true);
					saveIndividualRewardTime(ip, site.ordinal, now);
					player.sendMessage("[" + site.name + "] ¡Gracias por votar! Recompensa entregada.");
					LOGGER.info("VoteSystem: Individual reward -> " + player.getName() + " (" + ip + ") [" + site.name + "]");
				}
				else
				{
					player.sendMessage("[" + site.name + "] Voto no encontrado. Vota en: " + site.siteUrl);
				}
			}
			catch (Exception e)
			{
				LOGGER.warning("VoteSystem: Individual check error [" + site.name + "] player=" + player.getName() + ": " + e.getMessage());
				player.sendMessage("[" + site.name + "] Error al verificar. Intenta mas tarde.");
			}
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

		int idx = response.indexOf("\"votes\"");
		if (idx >= 0)
		{
			final int v = extractJsonInt(response, idx + 7);
			if (v >= 0)
			{
				return v;
			}
		}

		idx = response.indexOf("\"Server_Votes\"");
		if (idx >= 0)
		{
			final int v = extractJsonInt(response, idx + 14);
			if (v >= 0)
			{
				return v;
			}
		}

		idx = response.indexOf("\"total\"");
		if (idx >= 0)
		{
			final int v = extractJsonInt(response, idx + 7);
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
		if (r.contains("\"voted\":true") || r.contains("\"voted\": true")
			|| r.contains("\"hasVoted\":true") || r.contains("\"hasVoted\": true"))
		{
			return true;
		}
		final int idx = r.indexOf("\"votes\"");
		if (idx >= 0)
		{
			return extractJsonInt(r, idx + 7) > 0;
		}
		return false;
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
