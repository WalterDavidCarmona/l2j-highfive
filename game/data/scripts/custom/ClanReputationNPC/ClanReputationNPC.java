/*
 * NPC de intercambio de Reputacion de Clan.
 * Solo el lider de clan puede usar este NPC.
 * Configuracion editable en config/Custom/ClanReputationNPC.ini
 */
package custom.ClanReputationNPC;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class ClanReputationNPC extends Script
{
	private static final Logger LOGGER = Logger.getLogger(ClanReputationNPC.class.getName());

	// Valores por defecto (sobreescritos por la config)
	private static boolean ENABLED    = true;
	private static int     NPC_ID     = 50020;
	private static int     ITEM_ID    = 10639;
	private static String  ITEM_NAME  = "";   // vacio = usar nombre del ItemData
	private static long    ITEM_COUNT = 1000;
	private static int     REP_POINTS = 100;

	private ClanReputationNPC()
	{
		loadConfig();
		if (ENABLED)
		{
			addStartNpc(NPC_ID);
			addTalkId(NPC_ID);
			addFirstTalkId(NPC_ID);
			LOGGER.info("ClanReputationNPC: Cargado. NPC=" + NPC_ID + " ItemId=" + ITEM_ID + " x" + ITEM_COUNT + " -> " + REP_POINTS + " rep.");
		}
	}

	// -------------------------------------------------------------------------
	// Config
	// -------------------------------------------------------------------------
	private void loadConfig()
	{
		try (InputStream is = new FileInputStream("./config/Custom/ClanReputationNPC.ini"))
		{
			final Properties p = new Properties();
			p.load(is);
			ENABLED    = Boolean.parseBoolean(p.getProperty("ClanRepNpcEnabled",  "True").trim());
			NPC_ID     = Integer.parseInt(p.getProperty("ClanRepNpcId",      "50020").trim());
			ITEM_ID    = Integer.parseInt(p.getProperty("ClanRepItemId",      "10639").trim());
			ITEM_NAME  = p.getProperty("ClanRepItemName", "").trim();
			ITEM_COUNT = Long.parseLong(p.getProperty("ClanRepItemCount",  "1000").trim());
			REP_POINTS = Integer.parseInt(p.getProperty("ClanRepPoints",       "100").trim());
		}
		catch (Exception e)
		{
			LOGGER.warning("ClanReputationNPC: No se pudo cargar config/Custom/ClanReputationNPC.ini: " + e.getMessage());
		}
	}

	// -------------------------------------------------------------------------
	// Dialogo inicial
	// -------------------------------------------------------------------------
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (!ENABLED)
		{
			return null;
		}

		if ((player.getClan() == null) || !player.isClanLeader())
		{
			return "no_leader.htm";
		}

		// Generar HTML dinamico con datos del jugador
		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setHtml(buildMainHtml(player));
		player.sendPacket(msg);
		return null;
	}

	// -------------------------------------------------------------------------
	// Eventos (bypass)
	// -------------------------------------------------------------------------
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!ENABLED)
		{
			return null;
		}

		if ("back".equals(event))
		{
			if ((player.getClan() == null) || !player.isClanLeader())
			{
				return "no_leader.htm";
			}
			final NpcHtmlMessage msg = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0);
			msg.setHtml(buildMainHtml(player));
			player.sendPacket(msg);
			return null;
		}

		if ("exchange".equals(event))
		{
			if ((player.getClan() == null) || !player.isClanLeader())
			{
				return "no_leader.htm";
			}

			final long currentCount = player.getInventory().getInventoryItemCount(ITEM_ID, -1);
			if (currentCount < ITEM_COUNT)
			{
				final NpcHtmlMessage msg = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0);
				msg.setHtml(buildNoItemsHtml(player, currentCount));
				player.sendPacket(msg);
				return null;
			}

			player.destroyItemByItemId(ItemProcessType.FEE, ITEM_ID, ITEM_COUNT, player, true);
			player.getClan().addReputationScore(REP_POINTS);

			final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_CLAN_HAS_ADDED_1S_POINTS_TO_ITS_CLAN_REPUTATION_SCORE);
			sm.addInt(REP_POINTS);
			player.sendPacket(sm);

			final NpcHtmlMessage msg = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0);
			msg.setHtml(buildSuccessHtml(player));
			player.sendPacket(msg);
		}

		return null;
	}

	// -------------------------------------------------------------------------
	// HTML dinámico
	// -------------------------------------------------------------------------
	private String buildMainHtml(Player player)
	{
		final String itemName = getItemName(ITEM_ID);
		final int currentRep  = (player.getClan() != null) ? player.getClan().getReputationScore() : 0;
		final long playerItems = player.getInventory().getInventoryItemCount(ITEM_ID, -1);

		return "<html><body>" +
			"<center><br>" +
			"<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>" +
			"<font name=\"hs13\" color=\"CDB67F\">Maestro de Reputacion de Clan</font><br><br>" +
			"<img src=\"L2UI.SquareGray\" width=270 height=1><br><br>" +
			"<table width=270 border=0>" +
			"<tr><td align=center><font color=\"B09878\">Intercambia items por reputacion de clan.</font></td></tr>" +
			"<tr><td height=8></td></tr>" +
			"<tr>" +
			"<td align=center><font color=\"FFFFFF\">Reputacion actual del clan: <font color=\"LEVEL\">" + currentRep + "</font></font></td>" +
			"</tr>" +
			"<tr><td height=8></td></tr>" +
			"<tr><td><img src=\"L2UI.SquareGray\" width=270 height=1></td></tr>" +
			"<tr><td height=10></td></tr>" +
			"<tr><td align=center><font color=\"CDB67F\">Oferta de intercambio:</font></td></tr>" +
			"<tr><td height=6></td></tr>" +
			"<tr><td align=center>" +
			"<table border=0 width=270 bgcolor=\"0A0A1E\">" +
			"<tr><td height=8></td></tr>" +
			"<tr>" +
			"<td width=135 align=center><font color=\"FFFFFF\">" + itemName + " x" + ITEM_COUNT + "</font></td>" +
			"<td width=30 align=center><font color=\"7A7060\">→</font></td>" +
			"<td width=105 align=center><font color=\"93FFA8\">" + REP_POINTS + " pts. rep.</font></td>" +
			"</tr>" +
			"<tr><td height=8></td></tr>" +
			"</table>" +
			"</td></tr>" +
			"<tr><td height=6></td></tr>" +
			"<tr><td align=center><font color=\"7A7060\">Tienes: <font color=\"" + (playerItems >= ITEM_COUNT ? "93FFA8" : "FF6060") + "\">" + playerItems + "</font> / " + ITEM_COUNT + " " + itemName + "</font></td></tr>" +
			"<tr><td height=10></td></tr>" +
			"</table>" +
			"<button value=\"Intercambiar\" action=\"bypass -h Script ClanReputationNPC exchange\"" +
			" width=160 height=30 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>" +
			"<br></center></body></html>";
	}



	private String buildNoItemsHtml(Player player, long currentCount)
	{
		final String itemName = getItemName(ITEM_ID);
		return "<html><body><center><br>" +
			"<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>" +
			"<font name=\"hs13\" color=\"CDB67F\">Maestro de Reputacion de Clan</font><br><br>" +
			"<img src=\"L2UI.SquareGray\" width=270 height=1><br><br>" +
			"<font color=\"FF6060\">Items insuficientes.</font><br><br>" +
			"<table width=270 border=0>" +
			"<tr><td align=center><font color=\"FFFFFF\">Necesitas: <font color=\"LEVEL\">" + ITEM_COUNT + "</font> " + itemName + "</font></td></tr>" +
			"<tr><td height=4></td></tr>" +
			"<tr><td align=center><font color=\"FFFFFF\">Tienes: <font color=\"FF6060\">" + currentCount + "</font> " + itemName + "</font></td></tr>" +
			"<tr><td height=4></td></tr>" +
			"<tr><td align=center><font color=\"B09878\">Te faltan: <font color=\"FF9900\">" + (ITEM_COUNT - currentCount) + "</font> " + itemName + "</font></td></tr>" +
			"</table><br>" +
			"<img src=\"L2UI.SquareGray\" width=270 height=1><br><br>" +
			"<button value=\"Volver\" action=\"bypass -h Script ClanReputationNPC back\"" +
			" width=120 height=28 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>" +
			"<br></center></body></html>";
	}

	private String buildSuccessHtml(Player player)
	{
		final String itemName = getItemName(ITEM_ID);
		final int newRep      = (player.getClan() != null) ? player.getClan().getReputationScore() : 0;
		return "<html><body><center><br>" +
			"<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>" +
			"<font name=\"hs13\" color=\"CDB67F\">Maestro de Reputacion de Clan</font><br><br>" +
			"<img src=\"L2UI.SquareGray\" width=270 height=1><br><br>" +
			"<font color=\"93FFA8\">Intercambio exitoso!</font><br><br>" +
			"<table width=270 border=0>" +
			"<tr><td align=center><font color=\"B09878\">Consumido: <font color=\"FF9900\">" + ITEM_COUNT + "</font> " + itemName + "</font></td></tr>" +
			"<tr><td height=4></td></tr>" +
			"<tr><td align=center><font color=\"B09878\">Reputacion ganada: <font color=\"93FFA8\">+" + REP_POINTS + "</font> pts.</font></td></tr>" +
			"<tr><td height=4></td></tr>" +
			"<tr><td align=center><font color=\"FFFFFF\">Reputacion total del clan: <font color=\"LEVEL\">" + newRep + "</font></font></td></tr>" +
			"</table><br>" +
			"<img src=\"L2UI.SquareGray\" width=270 height=1><br><br>" +
			"<button value=\"Intercambiar de nuevo\" action=\"bypass -h Script ClanReputationNPC back\"" +
			" width=180 height=28 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>" +
			"<br></center></body></html>";
	}

	// -------------------------------------------------------------------------
	// Utilidades
	// -------------------------------------------------------------------------
	private String getItemName(int itemId)
	{
		// Si la config define un nombre personalizado, usarlo
		if (!ITEM_NAME.isEmpty())
		{
			return ITEM_NAME;
		}
		// Fallback: nombre del ItemData del servidor
		final ItemTemplate tpl = ItemData.getInstance().getTemplate(itemId);
		return (tpl != null) ? tpl.getName() : ("Item #" + itemId);
	}



	public static void main(String[] args)
	{
		new ClanReputationNPC();
	}
}
