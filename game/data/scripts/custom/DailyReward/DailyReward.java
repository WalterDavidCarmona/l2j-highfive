/*
 * Sistema de Recompensa Diaria por Conexion - 28 dias
 * Dos secciones independientes: Usuario (todos) y Premium (solo premium).
 * Se abre mediante el voice command .reward como ventana NPC normal.
 */
package custom.DailyReward;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.managers.PremiumManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class DailyReward extends Script
{
	private static final Logger LOGGER = Logger.getLogger(DailyReward.class.getName());

	// SQL
	private static final String CREATE_TABLE =
		"CREATE TABLE IF NOT EXISTS `daily_rewards` (" +
		"`char_id` INT NOT NULL, " +
		"`last_claim` BIGINT NOT NULL DEFAULT 0, " +
		"`last_premium_claim` BIGINT NOT NULL DEFAULT 0, " +
		"`streak_day` INT NOT NULL DEFAULT 0, " +
		"PRIMARY KEY (`char_id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8;";

	private static final String CREATE_IP_TABLE =
		"CREATE TABLE IF NOT EXISTS `daily_rewards_hwid` (" +
		"`identifier` VARCHAR(64) NOT NULL, " +
		"`id_type` TINYINT NOT NULL DEFAULT 0, " +
		"`claim_date` INT NOT NULL, " +
		"`user_count` INT NOT NULL DEFAULT 0, " +
		"`premium_count` INT NOT NULL DEFAULT 0, " +
		"PRIMARY KEY (`identifier`, `id_type`, `claim_date`)) ENGINE=InnoDB DEFAULT CHARSET=utf8;";

	private static final String SELECT_PLAYER =
		"SELECT `last_claim`, `last_premium_claim`, `streak_day` FROM `daily_rewards` WHERE `char_id`=?";
	private static final String SELECT_HWID =
		"SELECT `user_count`, `premium_count` FROM `daily_rewards_hwid` WHERE `identifier`=? AND `id_type`=? AND `claim_date`=?";
	private static final String UPSERT_HWID_USER =
		"INSERT INTO `daily_rewards_hwid` (`identifier`, `id_type`, `claim_date`, `user_count`) VALUES (?,?,?,1) " +
		"ON DUPLICATE KEY UPDATE `user_count`=`user_count`+1";
	private static final String UPSERT_HWID_PREMIUM =
		"INSERT INTO `daily_rewards_hwid` (`identifier`, `id_type`, `claim_date`, `premium_count`) VALUES (?,?,?,1) " +
		"ON DUPLICATE KEY UPDATE `premium_count`=`premium_count`+1";
	private static final String UPSERT_USER =
		"INSERT INTO `daily_rewards` (`char_id`, `last_claim`, `streak_day`) VALUES (?,?,?) " +
		"ON DUPLICATE KEY UPDATE `last_claim`=VALUES(`last_claim`), `streak_day`=VALUES(`streak_day`)";
	private static final String UPSERT_PREMIUM =
		"INSERT INTO `daily_rewards` (`char_id`, `last_premium_claim`) VALUES (?,?) " +
		"ON DUPLICATE KEY UPDATE `last_premium_claim`=VALUES(`last_premium_claim`)";

	// Config
	public static boolean           ENABLED       = true;
	public static boolean           RESET_ON_MISS = true;
	public static int               MAX_PER_HWID  = 1;
	public static int               MAX_PER_IP    = 3;
	public static final List<int[]> REWARDS       = new ArrayList<>();

	private static final int TYPE_HWID = 0;
	private static final int TYPE_IP   = 1;


	private DailyReward()
	{
		loadConfig();
		createTable();


		LOGGER.info("DailyReward: Cargado. " + REWARDS.size() + " dias configurados.");
	}

	// -------------------------------------------------------------------------
	// Enviar como ventana NPC (objectId 0 = sin NPC fisico)
	// -------------------------------------------------------------------------
	public static void sendHtml(Player player, String html)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(0);
		msg.setHtml(html);
		player.sendPacket(msg);
	}

	// -------------------------------------------------------------------------
	// Config + DB
	// -------------------------------------------------------------------------
	private static void loadConfig()
	{
		REWARDS.clear();
		try (InputStream is = new FileInputStream("./config/Custom/DailyReward.ini"))
		{
			final Properties p = new Properties();
			p.load(is);
			ENABLED       = Boolean.parseBoolean(p.getProperty("DailyRewardEnabled",     "True").trim());
			RESET_ON_MISS = Boolean.parseBoolean(p.getProperty("DailyRewardResetOnMiss", "True").trim());
			MAX_PER_HWID  = Integer.parseInt(p.getProperty("DailyRewardMaxPerHWID", "1").trim());
			MAX_PER_IP    = Integer.parseInt(p.getProperty("DailyRewardMaxPerIP",   "3").trim());
			for (int day = 1; ; day++)
			{
				final String val = p.getProperty("Day" + day);
				if (val == null)
				{
					break;
				}
				final String[] parts = val.trim().split(":");
				if (parts.length == 2)
				{
					REWARDS.add(new int[]{ Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()) });
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("DailyReward: Error cargando config: " + e.getMessage());
			if (REWARDS.isEmpty())
			{
				REWARDS.add(new int[]{ 10639, 100 });
			}
		}
	}

	private static void createTable()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			con.prepareStatement(CREATE_TABLE).execute();
			con.prepareStatement(CREATE_IP_TABLE).execute();
			try
			{
				con.prepareStatement("ALTER TABLE `daily_rewards` ADD COLUMN `last_premium_claim` BIGINT NOT NULL DEFAULT 0").execute();
			}
			catch (Exception ignored) {}
		}
		catch (Exception e)
		{
			LOGGER.warning("DailyReward: Error creando tabla: " + e.getMessage());
		}
	}

	// -------------------------------------------------------------------------
	// Datos del jugador: [lastClaim, lastPremiumClaim, streakDay]
	// -------------------------------------------------------------------------
	private static int[] getPlayerData(int charId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_PLAYER))
		{
			ps.setInt(1, charId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return new int[]
					{
						(int) rs.getLong("last_claim"),
						(int) rs.getLong("last_premium_claim"),
						rs.getInt("streak_day")
					};
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("DailyReward: Error leyendo datos: " + e.getMessage());
		}
		return new int[]{ 0, 0, 0 };
	}

	private static int todayEpochDay()
	{
		return (int) LocalDate.now(ZoneId.systemDefault()).toEpochDay();
	}

	private static Object[] getPlayerIdentifier(Player player)
	{
		try
		{
			if ((player.getClient() != null) && (player.getClient().getHardwareInfo() != null))
			{
				final String hwid = player.getClient().getHardwareInfo().getMacAddress();
				if ((hwid != null) && !hwid.isEmpty() && !hwid.equals("Unknown"))
				{
					return new Object[]{ hwid, TYPE_HWID };
				}
			}
		}
		catch (Exception ignored) {}
		return new Object[]{ player.getIPAddress(), TYPE_IP };
	}

	private static int[] getIdentifierCounts(String identifier, int idType, int today)
	{
		if ((identifier == null) || identifier.isEmpty())
		{
			return new int[]{ 0, 0 };
		}
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_HWID))
		{
			ps.setString(1, identifier);
			ps.setInt(2, idType);
			ps.setInt(3, today);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return new int[]{ rs.getInt("user_count"), rs.getInt("premium_count") };
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("DailyReward: Error leyendo conteos: " + e.getMessage());
		}
		return new int[]{ 0, 0 };
	}

	private static boolean isPremium(Player player)
	{
		return PremiumManager.getInstance().getPremiumExpiration(player.getAccountName()) > System.currentTimeMillis();
	}

	private static int resolveStreakDay(int lastDay, int streak, int today)
	{
		if (lastDay == 0)
		{
			return 1;
		}
		final int diff  = today - lastDay;
		final int total = REWARDS.size();
		if (diff == 0)
		{
			return streak;
		}
		if (diff == 1)
		{
			return (streak % total) + 1;
		}
		return RESET_ON_MISS ? 1 : ((streak % total) + 1);
	}

	// -------------------------------------------------------------------------
	// Reclamar recompensa de USUARIO
	// -------------------------------------------------------------------------
	public static String claimUserReward(Player player)
	{
		if (!ENABLED || REWARDS.isEmpty())
		{
			return "Sistema desactivado.";
		}
		final int   today = todayEpochDay();
		final int[] data  = getPlayerData(player.getObjectId());
		if (data[0] == today)
		{
			return "Ya reclamaste la recompensa de usuario hoy.";
		}

		final Object[] ident    = getPlayerIdentifier(player);
		final String   idValue  = (String) ident[0];
		final int      idType   = (int) ident[1];
		final int      maxLimit = (idType == TYPE_HWID) ? MAX_PER_HWID : MAX_PER_IP;
		if (maxLimit > 0)
		{
			final int[] counts = getIdentifierCounts(idValue, idType, today);
			if (counts[0] >= maxLimit)
			{
				final String tipo = (idType == TYPE_HWID) ? "PC" : "IP";
				return "Limite de cuentas por " + tipo + " alcanzado (" + maxLimit + " cuenta(s) por dia).";
			}
		}

		final int   newDay = resolveStreakDay(data[0], data[2], today);
		final int[] reward = REWARDS.get(newDay - 1);
		player.addItem(ItemProcessType.REWARD, reward[0], reward[1], player, true);

		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement ps = con.prepareStatement(UPSERT_USER);
			ps.setInt(1, player.getObjectId());
			ps.setLong(2, today);
			ps.setInt(3, newDay);
			ps.execute();
			if (maxLimit > 0)
			{
				ps = con.prepareStatement(UPSERT_HWID_USER);
				ps.setString(1, idValue);
				ps.setInt(2, idType);
				ps.setInt(3, today);
				ps.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("DailyReward: Error guardando claim usuario: " + e.getMessage());
		}
		return "ok:" + newDay + ":" + getItemName(reward[0]) + ":" + reward[1];
	}

	// -------------------------------------------------------------------------
	// Reclamar recompensa PREMIUM (x2)
	// -------------------------------------------------------------------------
	public static String claimPremiumReward(Player player)
	{
		if (!ENABLED || REWARDS.isEmpty())
		{
			return "Sistema desactivado.";
		}
		if (!isPremium(player))
		{
			return "Necesitas cuenta Premium para reclamar esta recompensa.";
		}
		final int   today = todayEpochDay();
		final int[] data  = getPlayerData(player.getObjectId());
		if (data[1] == today)
		{
			return "Ya reclamaste la recompensa Premium de hoy.";
		}

		final Object[] ident    = getPlayerIdentifier(player);
		final String   idValue  = (String) ident[0];
		final int      idType   = (int) ident[1];
		final int      maxLimit = (idType == TYPE_HWID) ? MAX_PER_HWID : MAX_PER_IP;
		if (maxLimit > 0)
		{
			final int[] counts = getIdentifierCounts(idValue, idType, today);
			if (counts[1] >= maxLimit)
			{
				final String tipo = (idType == TYPE_HWID) ? "PC" : "IP";
				return "Limite de cuentas Premium por " + tipo + " alcanzado (" + maxLimit + " cuenta(s) por dia).";
			}
		}

		final int   newDay = resolveStreakDay(data[0] == today ? today - 1 : data[0], data[2], today);
		final int[] reward = REWARDS.get((newDay <= 0 ? 1 : newDay) - 1);
		final int   count  = reward[1] * 2;
		player.addItem(ItemProcessType.REWARD, reward[0], count, player, true);

		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement ps = con.prepareStatement(UPSERT_PREMIUM);
			ps.setInt(1, player.getObjectId());
			ps.setLong(2, today);
			ps.execute();
			if (maxLimit > 0)
			{
				ps = con.prepareStatement(UPSERT_HWID_PREMIUM);
				ps.setString(1, idValue);
				ps.setInt(2, idType);
				ps.setInt(3, today);
				ps.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("DailyReward: Error guardando claim premium: " + e.getMessage());
		}
		return "ok:" + newDay + ":" + getItemName(reward[0]) + ":" + count;
	}

	// -------------------------------------------------------------------------
	// HTML: limite de cuenta por PC/IP alcanzado
	// -------------------------------------------------------------------------
	public static String buildLimitReachedHtml(boolean isPremium, String reason)
	{
		final String title  = isPremium ? "Recompensa Premium" : "Recompensa Usuario";
		final String color  = isPremium ? "CDB67F" : "9A9280";

		return "<html noscrollbar><body>"
			+ "<table width=700><tr><td height=10></td></tr></table>"
			+ "<table border=0 cellpadding=0 cellspacing=0 width=700 background=\"L2UI_CT1.Windows_DF_TooltipBG\">"
			+ "<tr><td height=14></td></tr>"
			+ "<tr><td align=center><img src=\"L2UI_CH3.herotower_deco\" width=560 height=32></td></tr>"
			+ "<tr><td align=center><font name=\"hs12\" color=\"" + color + "\">RECOMPENSA DIARIA</font></td></tr>"
			+ "<tr><td height=10></td></tr>"
			+ "<tr><td><center><img src=\"L2UI.SquareGray\" width=680 height=1></center></td></tr>"
			+ "<tr><td height=12></td></tr>"
			+ "<tr><td align=center><font color=\"FF6060\">LIMITE ALCANZADO</font></td></tr>"
			+ "<tr><td height=8></td></tr>"
			+ "<tr><td align=center><font color=\"FFFFFF\">" + title + "</font></td></tr>"
			+ "<tr><td height=8></td></tr>"
			+ "<tr><td align=center><font color=\"B09878\">Esta recompensa ya fue reclamada por otro personaje de tu cuenta o desde el mismo equipo hoy.</font></td></tr>"
			+ "<tr><td height=8></td></tr>"
			+ "<tr><td align=center><font color=\"555566\">Vuelve manana para reclamar el siguiente dia.</font></td></tr>"
			+ "<tr><td height=14></td></tr>"
			+ "<tr><td align=center>"
			+ "<button value=\"Volver\" action=\"bypass _bbsdailyreward_page\""
			+ " width=160 height=30 back=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Info\">"
			+ "</td></tr>"
			+ "<tr><td height=14></td></tr>"
			+ "</table>"
			+ "</body></html>";
	}

	// -------------------------------------------------------------------------
	// HTML principal — ventana NPC (sin CB, sin %navigation%)
	// -------------------------------------------------------------------------
	public static String buildPageHtml(Player player)
	{
		if (!ENABLED || REWARDS.isEmpty())
		{
			return "<html><body><center>Sistema desactivado.</center></body></html>";
		}
		final int[]   data      = getPlayerData(player.getObjectId());
		final int     today     = todayEpochDay();
		final int     streakDay = resolveStreakDay(data[0], data[2], today);
		final int     total     = REWARDS.size();
		final int[]   reward    = REWARDS.get(Math.max(0, streakDay - 1));
		final String  icon      = getItemIcon(reward[0]);
		final String  itemName  = getItemName(reward[0]);
		final boolean premium   = isPremium(player);
		final boolean userDone  = (data[0] == today);
		final boolean premDone  = (data[1] == today);
		final int     COLS      = 7;

		final StringBuilder userGrid = buildGrid(streakDay, userDone, total, COLS, false);
		final StringBuilder premGrid = buildGrid(streakDay, premDone, total, COLS, true);

		final String userBtn = userDone
			? "<button value=\"Ya reclamado hoy\" action=\"bypass _bbsdailyreward_page\" width=260 height=34 back=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\">"
			: "<button value=\"  Reclamar Dia " + streakDay + "  \" action=\"bypass _bbsdailyreward\" width=260 height=34 back=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Info\">";

		final String premBtn;
		if (!premium)
		{
			premBtn = "<button value=\"Solo para cuentas Premium\" action=\"bypass _bbsdailyreward_page\" width=260 height=34 back=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\">";
		}
		else if (premDone)
		{
			premBtn = "<button value=\"Premium ya reclamado hoy\" action=\"bypass _bbsdailyreward_page\" width=260 height=34 back=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\">";
		}
		else
		{
			premBtn = "<button value=\"  Reclamar Premium Dia " + streakDay + "  \" action=\"bypass _bbsdailyreward_premium\" width=260 height=34 back=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Info\">";
		}

		final String userStatus = userDone
			? "<font color=\"555566\">Vuelve manana - Dia " + ((streakDay % total) + 1) + "</font>"
			: "<font color=\"93FFA8\">Disponible!</font>";
		final String premStatus = !premium
			? "<font color=\"7A7060\">Requiere cuenta Premium</font>"
			: (premDone ? "<font color=\"555566\">Vuelve manana</font>" : "<font color=\"CDB67F\">Disponible!</font>");

		return "<html noscrollbar><body>"
			+ "<table width=700><tr><td height=10></td></tr></table>"
			+ "<table border=0 cellpadding=0 cellspacing=0 width=700 background=\"L2UI_CT1.Windows_DF_TooltipBG\">"
			+ "<tr><td height=14></td></tr>"
			+ "<tr><td align=center><img src=\"L2UI_CH3.herotower_deco\" width=560 height=32></td></tr>"
			+ "<tr><td align=center><font name=\"hs12\" color=\"CDB67F\">RECOMPENSA DIARIA</font></td></tr>"
			+ "<tr><td align=center><font color=\"7A7060\">Dia " + streakDay + " / " + total + "  -  Hoy: <font color=\"FFFFFF\">" + itemName + "</font></font></td></tr>"
			+ "<tr><td height=10></td></tr>"
			+ "<tr><td><center><img src=\"L2UI.SquareGray\" width=680 height=1></center></td></tr>"
			+ "<tr><td height=10></td></tr>"
			// Panel usuario
			+ "<tr><td align=center><font color=\"9A9280\">Recompensa Usuario</font>   " + userStatus + "</td></tr>"
			+ "<tr><td height=6></td></tr>"
			+ "<tr><td align=center><table border=0 cellspacing=2 cellpadding=0 width=690>" + userGrid + "</table></td></tr>"
			+ "<tr><td height=8></td></tr>"
			+ "<tr><td align=center>" + userBtn + "</td></tr>"
			+ "<tr><td height=12></td></tr>"
			+ "<tr><td><center><img src=\"L2UI.SquareGray\" width=680 height=1></center></td></tr>"
			+ "<tr><td height=10></td></tr>"
			// Panel premium
			+ "<tr><td align=center><font color=\"CDB67F\">Recompensa Premium (x2)</font>   " + premStatus + "</td></tr>"
			+ "<tr><td height=6></td></tr>"
			+ "<tr><td align=center><table border=0 cellspacing=2 cellpadding=0 width=690>" + premGrid + "</table></td></tr>"
			+ "<tr><td height=8></td></tr>"
			+ "<tr><td align=center>" + premBtn + "</td></tr>"
			+ "<tr><td height=14></td></tr>"
			+ "</table>"
			+ "</body></html>";
	}

	// -------------------------------------------------------------------------
	// Grid de dias
	// -------------------------------------------------------------------------
	private static StringBuilder buildGrid(int streakDay, boolean claimedToday, int total, int COLS, boolean isPremiumGrid)
	{
		final StringBuilder sb = new StringBuilder();
		final int rows = (total + COLS - 1) / COLS;
		for (int row = 0; row < rows; row++)
		{
			sb.append("<tr>");
			for (int col = 0; col < COLS; col++)
			{
				final int d = (row * COLS) + col + 1;
				if (d > total)
				{
					sb.append("<td width=96></td>");
					continue;
				}
				final int[]  r      = REWARDS.get(d - 1);
				final String icon   = getItemIcon(r[0]);
				final int    count  = isPremiumGrid ? r[1] * 2 : r[1];
				final boolean isCur  = (d == streakDay);
				final boolean isDone = (d < streakDay) || (claimedToday && (d == streakDay));
				final String  bg     = isCur ? (isPremiumGrid ? "201800" : "18183A") : (isDone ? "080810" : "0A0A16");
				final String  dayClr = isCur ? (isPremiumGrid ? "CDB67F" : "6699FF") : (isDone ? "333344" : "555566");
				final String  valClr = isCur ? "FFFFFF" : (isDone ? "333344" : "7A7060");

				sb.append("<td width=96 height=68 align=center bgcolor=\"").append(bg).append("\">")
					.append("<font color=\"").append(dayClr).append("\">Dia ").append(d).append("</font><br1>")
					.append("<img src=\"").append(icon).append("\" width=32 height=32><br1>")
					.append("<font color=\"").append(valClr).append("\">x").append(count).append("</font>")
					.append("</td>");
			}
			sb.append("</tr><tr><td colspan=").append(COLS).append(" height=4></td></tr>");
		}
		return sb;
	}

	// -------------------------------------------------------------------------
	// Utilidades
	// -------------------------------------------------------------------------
	private static String getItemName(int itemId)
	{
		final ItemTemplate tpl = ItemData.getInstance().getTemplate(itemId);
		return (tpl != null) ? tpl.getName() : ("Item #" + itemId);
	}

	private static String getItemIcon(int itemId)
	{
		final ItemTemplate tpl = ItemData.getInstance().getTemplate(itemId);
		return ((tpl != null) && (tpl.getIcon() != null)) ? tpl.getIcon() : "icon.etc_question_mark_i00";
	}

	public static void main(String[] args)
	{
		new DailyReward();
	}
}
