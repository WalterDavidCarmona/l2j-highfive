package custom.PremiumNPC;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.custom.CommunityBoardConfig;
import org.l2jmobius.gameserver.managers.PremiumManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class PremiumNPC extends Script
{
	private static final Logger LOGGER = Logger.getLogger(PremiumNPC.class.getName());
	private static final int NPC_ID = 50026;

	private PremiumNPC()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
		LOGGER.info("PremiumNPC: Cargado. NPC=" + NPC_ID);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMainPage(npc, player);
		return null;
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.startsWith("buy_"))
		{
			try
			{
				final int days = Integer.parseInt(event.substring(4));
				buyPremium(npc, player, days);
			}
			catch (Exception ignored) {}
		}
		else if (event.equals("main"))
		{
			showMainPage(npc, player);
		}
		return null;
	}

	private void showMainPage(Npc npc, Player player)
	{
		final int coinId       = CommunityBoardConfig.COMMUNITY_PREMIUM_COIN_ID;
		final long pricePerDay = CommunityBoardConfig.COMMUNITY_PREMIUM_PRICE_PER_DAY;
		final long balance     = player.getInventory().getInventoryItemCount(coinId, -1);
		final boolean hasPremium = PremiumManager.getInstance().getPremiumExpiration(player.getAccountName()) > System.currentTimeMillis();
		final String expiry = hasPremium
			? new SimpleDateFormat("dd/MM/yyyy HH:mm").format(PremiumManager.getInstance().getPremiumExpiration(player.getAccountName()))
			: "Sin estado Premium";

		final String coinName = getCoinName(coinId);

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=292 background=L2UI_CH3.refinewnd_back_Pattern>");
		sb.append("<tr><td valign=top align=center>");

		// Banner superior
		sb.append("<table border=0 cellpadding=0 cellspacing=0>");
		sb.append("<tr><td width=256 height=120 background=\"L2UI_CT1.OlympiadWnd_DF_GrandTexture\"></td></tr>");
		sb.append("</table>");
		sb.append("<table border=0 cellpadding=0 cellspacing=0>");
		sb.append("<tr><td align=center fixwidth=292>");
		sb.append("<font name=\"hs15\" color=\"CDB67F\">Agente Premium</font><br1>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32>");
		sb.append("</td></tr></table>");
		sb.append("<br>");

		// Estado actual
		sb.append("<table width=250 border=0 cellpadding=3 cellspacing=0 bgcolor=\"0D0D1E\">");
		sb.append("<tr><td align=center>");
		if (hasPremium)
		{
			sb.append("<font color=\"00FF00\">PREMIUM ACTIVO</font>");
		}
		else
		{
			sb.append("<font color=\"FF6060\">Sin Premium</font>");
		}
		sb.append("</td></tr>");
		sb.append("<tr><td align=center><font color=\"B09878\">Vence: </font><font color=\"FFFFFF\">").append(expiry).append("</font></td></tr>");
		sb.append("<tr><td align=center><font color=\"B09878\">Balance: </font><font color=\"CDB67F\">").append(formatNumber(balance)).append(" ").append(coinName).append("</font></td></tr>");
		sb.append("</table>");
		sb.append("<br>");

		// Separador + titulo
		sb.append("<img src=\"L2UI.SquareGray\" width=220 height=1><br>");
		sb.append("<table width=250 border=0>");
		sb.append("<tr><td align=center><font color=\"CDB67F\">Adquirir Premium</font></td></tr>");
		sb.append("<tr><td align=center><font color=\"7A7060\">").append(formatNumber(pricePerDay)).append(" ").append(coinName).append(" / dia</font></td></tr>");
		sb.append("</table><br>");

		// Botones
		final int[][] packages = { {1, 1}, {7, 7}, {15, 15}, {30, 30} };
		sb.append("<table width=250 border=0 cellpadding=2 cellspacing=2>");
		for (int[] pkg : packages)
		{
			final int days    = pkg[0];
			final long total  = pricePerDay * days;
			final boolean canBuy = balance >= total;
			final String btnStyle = canBuy
				? "back=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Info\""
				: "back=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\"";
			final String label = days + " dia" + (days > 1 ? "s" : "") + " - " + formatNumber(total) + " " + coinName;

			sb.append("<tr><td align=center>");
			sb.append("<button value=\"").append(label).append("\"")
				.append(" action=\"bypass -h Script PremiumNPC buy_").append(days).append("\"")
				.append(" width=220 height=28 ").append(btnStyle).append(">");
			sb.append("</td></tr>");
		}
		sb.append("</table>");
		sb.append("<br>");

		// Beneficios
		sb.append("<img src=\"L2UI.SquareGray\" width=220 height=1><br>");
		sb.append("<table width=250 border=0 cellpadding=2>");
		sb.append("<tr><td align=center><font color=\"CDB67F\">Beneficios Premium</font></td></tr>");
		sb.append("<tr><td align=center><font color=\"B09878\">+ EXP/SP  + Drop/Spoil  + Daily x2</font></td></tr>");
		sb.append("<tr><td align=center><font color=\"B09878\">+ Tienda exclusiva</font></td></tr>");
		sb.append("</table><br>");

		sb.append("</td></tr></table>");
		sb.append("</body></html>");

		sendHtml(npc, player, sb.toString());
	}

	private void buyPremium(Npc npc, Player player, int days)
	{
		if ((days < 1) || (days > 30))
		{
			player.sendMessage("Cantidad de dias invalida.");
			showMainPage(npc, player);
			return;
		}

		final int  coinId    = CommunityBoardConfig.COMMUNITY_PREMIUM_COIN_ID;
		final long price     = CommunityBoardConfig.COMMUNITY_PREMIUM_PRICE_PER_DAY * days;
		final long balance   = player.getInventory().getInventoryItemCount(coinId, -1);
		final String coinName = getCoinName(coinId);

		if (balance < price)
		{
			player.sendMessage("No tienes suficiente " + coinName + ". Necesitas " + formatNumber(price) + ".");
			showMainPage(npc, player);
			return;
		}

		player.destroyItemByItemId(ItemProcessType.FEE, coinId, price, npc, true);
		PremiumManager.getInstance().addPremiumTime(player.getAccountName(), days, TimeUnit.DAYS);

		final String expiry = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(
			PremiumManager.getInstance().getPremiumExpiration(player.getAccountName()));

		player.sendMessage("Premium activado por " + days + " dia(s). Vence: " + expiry);

		// Mostrar pagina de confirmacion
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=292 background=L2UI_CH3.refinewnd_back_Pattern>");
		sb.append("<tr><td valign=top align=center>");
		sb.append("<table border=0 cellpadding=0 cellspacing=0>");
		sb.append("<tr><td width=256 height=100 background=\"L2UI_CT1.OlympiadWnd_DF_GrandTexture\"></td></tr>");
		sb.append("</table>");
		sb.append("<table border=0 cellpadding=0 cellspacing=0>");
		sb.append("<tr><td align=center fixwidth=292>");
		sb.append("<font name=\"hs15\" color=\"CDB67F\">Agente Premium</font><br1>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32>");
		sb.append("</td></tr></table><br>");
		sb.append("<table width=250 border=0 cellpadding=4>");
		sb.append("<tr><td align=center><font color=\"00FF00\">COMPRA EXITOSA</font></td></tr>");
		sb.append("<tr><td height=6></td></tr>");
		sb.append("<tr><td><center><img src=\"L2UI.SquareGray\" width=220 height=1></center></td></tr>");
		sb.append("<tr><td height=6></td></tr>");
		sb.append("<tr><td align=center><font color=\"FFFFFF\">Premium activado por <font color=\"CDB67F\">").append(days).append(" dia").append(days > 1 ? "s" : "").append("</font></font></td></tr>");
		sb.append("<tr><td align=center><font color=\"B09878\">Vence: ").append(expiry).append("</font></td></tr>");
		sb.append("<tr><td height=8></td></tr>");
		sb.append("<tr><td align=center>");
		sb.append("<button value=\"Volver\" action=\"bypass -h Script PremiumNPC main\" width=160 height=30 back=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Info\">");
		sb.append("</td></tr>");
		sb.append("</table><br>");
		sb.append("</td></tr></table>");
		sb.append("</center></body></html>");

		sendHtml(npc, player, sb.toString());
	}

	private static void sendHtml(Npc npc, Player player, String html)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0);
		msg.setHtml(html);
		player.sendPacket(msg);
	}

	private static String getCoinName(int itemId)
	{
		if (itemId == 57)
		{
			return "Adena";
		}
		final org.l2jmobius.gameserver.model.item.ItemTemplate tpl =
			org.l2jmobius.gameserver.data.xml.ItemData.getInstance().getTemplate(itemId);
		return (tpl != null) ? tpl.getName() : ("Item#" + itemId);
	}

	private static String formatNumber(long n)
	{
		if (n >= 1_000_000)
		{
			return (n / 1_000_000) + "kk";
		}
		if (n >= 1_000)
		{
			return (n / 1_000) + "k";
		}
		return String.valueOf(n);
	}

	public static void main(String[] args)
	{
		new PremiumNPC();
	}
}
