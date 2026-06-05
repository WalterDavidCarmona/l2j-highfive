package custom.Achievements;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.model.olympiad.Hero;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class Achievements extends Script
{
	private static final Logger LOGGER = Logger.getLogger(Achievements.class.getName());
	private static final String CONFIG_FILE = "config/Custom/Achievements.ini";

	private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `achievements` ("
		+ "`char_id` INT NOT NULL,"
		+ "`mission_id` INT NOT NULL,"
		+ "`progress` INT NOT NULL DEFAULT 0,"
		+ "`completed` TINYINT NOT NULL DEFAULT 0,"
		+ "`claimed` TINYINT NOT NULL DEFAULT 0,"
		+ "PRIMARY KEY (`char_id`, `mission_id`)"
		+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

	private static final String SELECT_ALL = "SELECT `mission_id`, `progress`, `completed`, `claimed` FROM `achievements` WHERE `char_id`=?";
	private static final String UPSERT = "INSERT INTO `achievements` (`char_id`, `mission_id`, `progress`, `completed`, `claimed`) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE `progress`=VALUES(`progress`), `completed`=VALUES(`completed`), `claimed`=VALUES(`claimed`)";

	private static int NPC_ID = 50025;
	private static boolean ENABLED = true;
	private static final List<MissionDef> MISSIONS = new ArrayList<>();

	private static final ConcurrentHashMap<Integer, Long> LOGIN_TIMES = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Integer, ScheduledFuture<?>> ONLINE_TASKS = new ConcurrentHashMap<>();

	private static Achievements _instance;

	private Achievements()
	{
		loadConfig();
		if (!ENABLED)
		{
			LOGGER.info("Achievements: Sistema desactivado.");
			return;
		}
		createTable();
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);

		Containers.Players().addListener(new ConsumerEventListener(
			Containers.Players(), EventType.ON_PLAYER_LOGIN,
			(OnPlayerLogin ev) -> onPlayerLogin(ev), this));
		Containers.Players().addListener(new ConsumerEventListener(
			Containers.Players(), EventType.ON_PLAYER_LOGOUT,
			(OnPlayerLogout ev) -> onPlayerLogout(ev), this));
		Containers.Monsters().addListener(new ConsumerEventListener(
			Containers.Monsters(), EventType.ON_CREATURE_DEATH,
			(OnCreatureDeath ev) -> onMonsterDeath(ev), this));

		_instance = this;
		LOGGER.info("Achievements: Cargado con " + MISSIONS.size() + " misiones. NPC=" + NPC_ID);
	}

	private void loadConfig()
	{
		final Properties props = new Properties();
		try (InputStream is = new FileInputStream(CONFIG_FILE))
		{
			props.load(is);
		}
		catch (Exception e)
		{
			LOGGER.warning("Achievements: No se pudo cargar " + CONFIG_FILE + ": " + e.getMessage());
			return;
		}

		ENABLED = Boolean.parseBoolean(props.getProperty("AchievementsEnabled", "True").trim());
		NPC_ID = Integer.parseInt(props.getProperty("AchievementsNpcId", "50025").trim());

		MISSIONS.clear();
		for (int i = 1; i <= 50; i++)
		{
			final String val = props.getProperty("Mission" + i);
			if (val == null)
			{
				continue;
			}
			final String[] parts = val.split(";");
			if (parts.length < 5)
			{
				continue;
			}
			try
			{
				final MissionDef m = new MissionDef();
				m.id = i;
				m.type = MissionType.valueOf(parts[0].trim());
				m.goal = Integer.parseInt(parts[1].trim());
				m.rewardItemId = Integer.parseInt(parts[2].trim());
				m.rewardCount = Integer.parseInt(parts[3].trim());
				m.description = parts[4].trim();
				MISSIONS.add(m);
			}
			catch (Exception e)
			{
				LOGGER.warning("Achievements: Error en Mission" + i + ": " + e.getMessage());
			}
		}
	}

	private void createTable()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			con.prepareStatement(CREATE_TABLE).execute();
		}
		catch (Exception e)
		{
			LOGGER.warning("Achievements: Error creando tabla: " + e.getMessage());
		}
	}

	// -- Listeners --

	private void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		LOGIN_TIMES.put(player.getObjectId(), System.currentTimeMillis());
		scheduleOnlineChecks(player);
		checkHeroOlympiad(player);
	}

	private void onPlayerLogout(OnPlayerLogout event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		LOGIN_TIMES.remove(player.getObjectId());
		final ScheduledFuture<?> task = ONLINE_TASKS.remove(player.getObjectId());
		if (task != null)
		{
			task.cancel(false);
		}
	}

	private void scheduleOnlineChecks(Player player)
	{
		final ScheduledFuture<?> old = ONLINE_TASKS.remove(player.getObjectId());
		if (old != null)
		{
			old.cancel(false);
		}
		final ScheduledFuture<?> task = ThreadPool.scheduleAtFixedRate(() ->
		{
			checkOnlineTimeMissions(player);
		}, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(1));
		ONLINE_TASKS.put(player.getObjectId(), task);
	}

	private void checkOnlineTimeMissions(Player player)
	{
		if ((player == null) || (player.isOnlineInt() != 1))
		{
			final ScheduledFuture<?> task = ONLINE_TASKS.remove(player != null ? player.getObjectId() : 0);
			if (task != null)
			{
				task.cancel(false);
			}
			return;
		}
		final Long loginTime = LOGIN_TIMES.get(player.getObjectId());
		if (loginTime == null)
		{
			return;
		}
		final int minutesOnline = (int) ((System.currentTimeMillis() - loginTime) / 60000);
		final Map<Integer, int[]> data = loadPlayerData(player.getObjectId());
		for (MissionDef m : MISSIONS)
		{
			if (m.type != MissionType.ONLINE_TIME)
			{
				continue;
			}
			final int[] d = data.getOrDefault(m.id, new int[]{0, 0, 0});
			if (d[1] == 1)
			{
				continue;
			}
			if (minutesOnline >= m.goal)
			{
				d[0] = m.goal;
				d[1] = 1;
				saveProgress(player.getObjectId(), m.id, d[0], d[1], d[2]);
				notifyMissionComplete(player, m);
			}
			else if (minutesOnline > d[0])
			{
				d[0] = minutesOnline;
				saveProgress(player.getObjectId(), m.id, d[0], d[1], d[2]);
			}
		}
	}

	private void checkHeroOlympiad(Player player)
	{
		if (Hero.getInstance().isHero(player.getObjectId()))
		{
			final Map<Integer, int[]> data = loadPlayerData(player.getObjectId());
			for (MissionDef m : MISSIONS)
			{
				if (m.type == MissionType.HERO_OLYMPIAD)
				{
					final int[] d = data.getOrDefault(m.id, new int[]{0, 0, 0});
					if (d[1] != 1)
					{
						d[0] = m.goal;
						d[1] = 1;
						saveProgress(player.getObjectId(), m.id, d[0], d[1], d[2]);
						notifyMissionComplete(player, m);
					}
				}
			}
		}
	}

	private void onMonsterDeath(OnCreatureDeath event)
	{
		// El attacker debe ser un Player
		if (!(event.getAttacker() instanceof Player))
		{
			return;
		}
		if (!(event.getTarget() instanceof Attackable))
		{
			return;
		}
		final Player killer = (Player) event.getAttacker();
		final Attackable mob = (Attackable) event.getTarget();
		final int mobLevel   = mob.getLevel();

		final Map<Integer, int[]> data = loadPlayerData(killer.getObjectId());
		for (MissionDef m : MISSIONS)
		{
			if (m.type == MissionType.MONSTER_KILL)
			{
				// Solo monstruos de nivel 85 a 90
				if ((mobLevel >= 85) && (mobLevel <= 90))
				{
					incrementMission(killer, data, m);
				}
			}
			else if (m.type == MissionType.CHAMPION_KILL)
			{
				// Cualquier champion de nivel 85+
				if (mob.isChampion() && (mobLevel >= 85))
				{
					incrementMission(killer, data, m);
				}
			}
		}
	}

	private void incrementMission(Player player, Map<Integer, int[]> data, MissionDef m)
	{
		int[] d = data.get(m.id);
		if (d == null)
		{
			d = new int[]{0, 0, 0};
			data.put(m.id, d);
		}
		if (d[1] == 1)
		{
			return;
		}
		d[0]++;
		if (d[0] >= m.goal)
		{
			d[0] = m.goal;
			d[1] = 1;
			notifyMissionComplete(player, m);
		}
		saveProgress(player.getObjectId(), m.id, d[0], d[1], d[2]);
	}

	// -- Metodos publicos para otros sistemas --

	public static void onPvpKill(Player player)
	{
		if ((_instance == null) || !ENABLED)
		{
			return;
		}
		_instance.addProgress(player, MissionType.PVP_WIN);
	}

	public static void onTvtWin(Player player)
	{
		if ((_instance == null) || !ENABLED)
		{
			return;
		}
		_instance.addProgress(player, MissionType.TVT_WIN);
	}

	public static void onCtfWin(Player player)
	{
		if ((_instance == null) || !ENABLED)
		{
			return;
		}
		_instance.addProgress(player, MissionType.CTF_WIN);
	}

	public static void onDeathmatchWin(Player player)
	{
		if ((_instance == null) || !ENABLED)
		{
			return;
		}
		_instance.addProgress(player, MissionType.DEATHMATCH_WIN);
	}

	public static void onHeroOlympiad(Player player)
	{
		if ((_instance == null) || !ENABLED)
		{
			return;
		}
		_instance.addProgress(player, MissionType.HERO_OLYMPIAD);
	}

	public static void onTournamentWin(Player player)
	{
		if ((_instance == null) || !ENABLED)
		{
			return;
		}
		_instance.addProgress(player, MissionType.TOURNAMENT_WIN);
	}

	private void addProgress(Player player, MissionType type)
	{
		if (player == null)
		{
			return;
		}
		final Map<Integer, int[]> data = loadPlayerData(player.getObjectId());
		for (MissionDef m : MISSIONS)
		{
			if (m.type == type)
			{
				incrementMission(player, data, m);
			}
		}
	}

	private void notifyMissionComplete(Player player, MissionDef m)
	{
		// Mensaje en pantalla (5 segundos)
		player.sendPacket(new ExShowScreenMessage("Mision completada: " + m.description + "  |  Visita al Maestro de Logros para reclamar tu recompensa!", 6000));

		// Popup Community Board
		final String itemName = getItemName(m.rewardItemId);
		final String html = "<html><body>"
			+ "<center>"
			+ "<table border=0 cellpadding=0 cellspacing=0 width=292 background=L2UI_CH3.refinewnd_back_Pattern>"
			+ "<tr><td valign=top align=center>"
			+ "<table border=0 cellpadding=0 cellspacing=0>"
			+ "<tr><td width=256 height=100 background=\"L2UI_CT1.OlympiadWnd_DF_GrandTexture\"></td></tr>"
			+ "</table>"
			+ "<table border=0 cellpadding=0 cellspacing=0>"
			+ "<tr><td align=center fixwidth=292>"
			+ "<font name=\"hs15\" color=\"CDB67F\">Logro Desbloqueado!</font><br1>"
			+ "<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32>"
			+ "</td></tr></table><br>"
			+ "<table width=250 border=0>"
			+ "<tr><td align=center><font color=\"00FF00\">MISION COMPLETADA</font></td></tr>"
			+ "<tr><td height=6></td></tr>"
			+ "<tr><td align=center><font color=\"LEVEL\">" + m.description + "</font></td></tr>"
			+ "<tr><td height=6></td></tr>"
			+ "<tr><td><center><img src=\"L2UI.SquareGray\" width=220 height=1></center></td></tr>"
			+ "<tr><td height=6></td></tr>"
			+ "<tr><td align=center><font color=\"B09878\">Recompensa disponible:</font></td></tr>"
			+ "<tr><td align=center><font color=\"CDB67F\">" + m.rewardCount + " " + itemName + "</font></td></tr>"
			+ "<tr><td height=6></td></tr>"
			+ "<tr><td><center><img src=\"L2UI.SquareGray\" width=220 height=1></center></td></tr>"
			+ "<tr><td height=8></td></tr>"
			+ "<tr><td align=center><font color=\"FFFFFF\">Acercate al</font></td></tr>"
			+ "<tr><td align=center><font color=\"CDB67F\">Maestro de Logros</font></td></tr>"
			+ "<tr><td align=center><font color=\"FFFFFF\">en Giran para reclamar tu premio.</font></td></tr>"
			+ "<tr><td height=8></td></tr>"
			+ "</table>"
			+ "</td></tr></table>"
			+ "</center></body></html>";

		CommunityBoardHandler.separateAndSend(html, player);
	}

	// -- NPC Dialog --

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMissionList(npc, player);
		return null;
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.startsWith("claim_"))
		{
			try
			{
				final int missionId = Integer.parseInt(event.substring(6));
				claimReward(player, missionId, npc);
			}
			catch (Exception e)
			{
				// ignore
			}
		}
		else if (event.equals("list"))
		{
			showMissionList(npc, player);
		}
		return null;
	}

	private void showMissionList(Npc npc, Player player)
	{
		final Map<Integer, int[]> data = loadPlayerData(player.getObjectId());
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<center>");
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=292 background=L2UI_CH3.refinewnd_back_Pattern>");
		sb.append("<tr><td valign=top align=center>");

		sb.append("<table border=0 cellpadding=0 cellspacing=0>");
		sb.append("<tr><td width=256 height=100 background=\"L2UI_CT1.OlympiadWnd_DF_GrandTexture\"></td></tr>");
		sb.append("</table>");

		sb.append("<table border=0 cellpadding=0 cellspacing=0>");
		sb.append("<tr><td align=center fixwidth=292>");
		sb.append("<font name=\"hs15\" color=\"CDB67F\">Achievements</font><br1>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32>");
		sb.append("</td></tr></table><br>");

		for (MissionDef m : MISSIONS)
		{
			final int[] d = data.getOrDefault(m.id, new int[]{0, 0, 0});
			final int progress = d[0];
			final int completed = d[1];
			final int claimed = d[2];
			final int pct = Math.min(100, (int) ((progress * 100L) / m.goal));

			final String itemName = getItemName(m.rewardItemId);

			sb.append("<table width=270 border=0 cellpadding=2 cellspacing=0>");
			sb.append("<tr><td align=left>");
			sb.append("<font color=\"LEVEL\">" + m.description + "</font>");
			sb.append("</td></tr>");

			sb.append("<tr><td align=left>");
			sb.append("<font color=\"FFFFFF\">Progreso: " + progress + " / " + m.goal + " (" + pct + "%)</font>");
			sb.append("</td></tr>");

			sb.append("<tr><td align=left>");
			buildProgressBar(sb, pct);
			sb.append("</td></tr>");

			sb.append("<tr><td align=left>");
			sb.append("<font color=\"B09878\">Recompensa: " + m.rewardCount + " " + itemName + "</font>");
			sb.append("</td></tr>");

			sb.append("<tr><td align=center>");
			if (claimed == 1)
			{
				sb.append("<font color=\"00FF00\">RECLAMADO</font>");
			}
			else if (completed == 1)
			{
				sb.append("<button value=\"Reclamar\" action=\"bypass -h Script Achievements claim_" + m.id + "\" width=120 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			}
			else
			{
				sb.append("<font color=\"999999\">En progreso...</font>");
			}
			sb.append("</td></tr>");

			sb.append("<tr><td><center><img src=\"L2UI.SquareGray\" width=250 height=1></center></td></tr>");
			sb.append("</table>");
		}

		sb.append("</td></tr></table>");
		sb.append("</center></body></html>");

		final NpcHtmlMessage html = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0);
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}

	private void buildProgressBar(StringBuilder sb, int pct)
	{
		final int filled = (int) (2.2 * pct);
		final int empty = 220 - filled;
		sb.append("<table width=220 border=0 cellpadding=0 cellspacing=0><tr>");
		if (filled > 0)
		{
			sb.append("<td width=" + filled + " height=8 background=\"L2UI_CH3.BR_BAR1_MP1\"></td>");
		}
		if (empty > 0)
		{
			sb.append("<td width=" + empty + " height=8 background=\"L2UI_CH3.BR_BAR1_HP1\"></td>");
		}
		sb.append("</tr></table>");
	}

	private void claimReward(Player player, int missionId, Npc npc)
	{
		MissionDef mission = null;
		for (MissionDef m : MISSIONS)
		{
			if (m.id == missionId)
			{
				mission = m;
				break;
			}
		}
		if (mission == null)
		{
			return;
		}

		final Map<Integer, int[]> data = loadPlayerData(player.getObjectId());
		final int[] d = data.getOrDefault(missionId, new int[]{0, 0, 0});
		if (d[1] != 1)
		{
			player.sendMessage("Mision no completada aun.");
			return;
		}
		if (d[2] == 1)
		{
			player.sendMessage("Ya reclamaste esta recompensa.");
			return;
		}

		player.addItem(ItemProcessType.REWARD, mission.rewardItemId, mission.rewardCount, player, true);
		d[2] = 1;
		saveProgress(player.getObjectId(), missionId, d[0], d[1], d[2]);
		player.sendMessage("Recompensa reclamada: " + mission.rewardCount + " " + getItemName(mission.rewardItemId));

		showMissionList(npc, player);
	}

	// -- DB --

	private Map<Integer, int[]> loadPlayerData(int charId)
	{
		final Map<Integer, int[]> result = new HashMap<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_ALL))
		{
			ps.setInt(1, charId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					result.put(rs.getInt("mission_id"), new int[]{
						rs.getInt("progress"),
						rs.getInt("completed"),
						rs.getInt("claimed")
					});
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Achievements: Error cargando datos: " + e.getMessage());
		}
		return result;
	}

	private void saveProgress(int charId, int missionId, int progress, int completed, int claimed)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPSERT))
		{
			ps.setInt(1, charId);
			ps.setInt(2, missionId);
			ps.setInt(3, progress);
			ps.setInt(4, completed);
			ps.setInt(5, claimed);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.warning("Achievements: Error guardando progreso: " + e.getMessage());
		}
	}

	private String getItemName(int itemId)
	{
		final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
		return (item != null) ? item.getName() : ("Item#" + itemId);
	}

	// -- Modelo --

	private static enum MissionType
	{
		ONLINE_TIME,
		MONSTER_KILL,
		CHAMPION_KILL,
		PVP_WIN,
		TVT_WIN,
		CTF_WIN,
		DEATHMATCH_WIN,
		HERO_OLYMPIAD,
		TOURNAMENT_WIN
	}

	private static class MissionDef
	{
		int id;
		MissionType type;
		int goal;
		int rewardItemId;
		int rewardCount;
		String description;
	}

	public static void main(String[] args)
	{
		new Achievements();
	}
}
