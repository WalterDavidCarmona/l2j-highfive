/*
 * Torneo 1vs1 - Hall of Champions
 * Reemplaza el tab FAV del Community Board con el ranking de campeones del torneo.
 */
package handlers.bypass.communityboard;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;

public class TournamentBoard implements IParseBoardHandler
{
	private static final Logger LOGGER = Logger.getLogger(TournamentBoard.class.getName());

	public static final String CHAMPIONS_FILE = "./config/Custom/TournamentChampions.csv";

	private static final String[] COMMANDS =
	{
		"_bbsgetfav",
		"_bbstournament"
	};

	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}

	@Override
	public boolean onCommand(String command, Player player)
	{
		CommunityBoardHandler.separateAndSend(buildRankingHtml(), player);
		return true;
	}

	private String buildRankingHtml()
	{
		final List<String[]> champions = readChampions();

		final StringBuilder rows = new StringBuilder();
		if (champions.isEmpty())
		{
			rows.append("<tr>")
				.append("<td colspan=4 align=center height=50>")
				.append("<font color=\"B09878\">Aun no hay campeones registrados.</font>")
				.append("</td></tr>");
		}
		else
		{
			int rank = 1;
			for (int i = champions.size() - 1; i >= 0; i--)
			{
				final String[] c       = champions.get(i);
				final String name      = (c.length > 0) ? c[0] : "?";
				final String className = (c.length > 1) ? c[1] : "?";
				final String date      = (c.length > 2) ? c[2] : "?";
				final String rowBg     = ((rank % 2) == 0) ? "0A0A1E" : "111130";
				final String rankColor = (rank == 1) ? "CDB67F" : (rank <= 3) ? "93FFA8" : "C8C0A0";
				rows.append("<tr bgcolor=\"").append(rowBg).append("\">")
					.append("<td width=50 align=center height=24><font color=\"").append(rankColor).append("\">#").append(rank).append("</font></td>")
					.append("<td width=200 align=center><font color=\"FFFFFF\">").append(name).append("</font></td>")
					.append("<td width=220 align=center><font color=\"B09878\">").append(className).append("</font></td>")
					.append("<td width=160 align=center><font color=\"7A7060\">").append(date).append("</font></td>")
					.append("</tr>");
				rank++;
			}
		}

		return "<html><title>Eventos</title><body>" +
			"<br>" +
			"<table border=0 width=755>" +
			"<tr><td width=10></td>" +
			"<td width=740 align=left>" +
			"<font color=\"CDB67F\">Eventos</font>&nbsp;&gt;&nbsp;" +
			"<font color=\"FFFFFF\">Torneo 1vs1 - Hall of Champions</font>" +
			"</td></tr>" +
			"</table>" +
			"<center>" +
			"<img src=\"L2UI.squareblank\" width=1 height=10>" +
			// Cabecera
			"<table border=0 cellspacing=0 cellpadding=0 width=755 bgcolor=\"A7A19A\">" +
			"<tr>" +
			"<td fixwidth=50 align=center><font color=\"000000\">#</font></td>" +
			"<td fixwidth=200 align=center><font color=\"000000\">Personaje</font></td>" +
			"<td fixwidth=220 align=center><font color=\"000000\">Clase</font></td>" +
			"<td fixwidth=160 align=center><font color=\"000000\">Fecha</font></td>" +
			"</tr>" +
			"</table>" +
			// Filas de campeones
			"<table border=0 cellspacing=0 cellpadding=0 width=755>" +
			rows.toString() +
			"</table>" +
			// Pie
			"<img src=\"L2UI.squareblank\" width=1 height=10>" +
			"<table border=0 width=755>" +
			"<tr><td align=center>" +
			"<font color=\"696969\">Total de campeones registrados: " + champions.size() + "</font>" +
			"</td></tr>" +
			"</table>" +
			"</center>" +
			"</body></html>";
	}

	// -------------------------------------------------------------------------
	// Persistencia de campeones (leida por el board y escrita por Tournament1v1)
	// -------------------------------------------------------------------------

	public static void addChampion(String name, String className, String date)
	{
		try
		{
			new File("./config/Custom").mkdirs();
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(CHAMPIONS_FILE, true)))
			{
				bw.write(name + "|" + className + "|" + date);
				bw.newLine();
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("TournamentBoard: Error guardando campeon: " + e.getMessage());
		}
	}

	public static List<String[]> readChampions()
	{
		final List<String[]> list = new ArrayList<>();
		final File f = new File(CHAMPIONS_FILE);
		if (!f.exists())
		{
			return list;
		}
		try (BufferedReader br = new BufferedReader(new FileReader(f)))
		{
			String line;
			while ((line = br.readLine()) != null)
			{
				line = line.trim();
				if (!line.isEmpty())
				{
					list.add(line.split("\\|", -1));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("TournamentBoard: Error leyendo campeones: " + e.getMessage());
		}
		return list;
	}
}
