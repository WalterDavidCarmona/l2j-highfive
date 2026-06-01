/*
 * PremiumShop - Extension del NPC 30085 (Stanford)
 * Agrega boton "Items Premium" exclusivo para jugadores con cuenta Premium.
 * Bypass en HTML: bypass -h Script PremiumShop premium_shop
 */
package custom.PremiumShop;

import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.managers.PremiumManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class PremiumShop extends Script
{
	private static final int NPC_ID    = 30085;
	private static final int MULTISELL = 70001;

	private PremiumShop()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!"premium_shop".equals(event))
		{
			return null;
		}

		final boolean isPremium = PremiumManager.getInstance().getPremiumExpiration(player.getAccountName()) > System.currentTimeMillis();
		if (isPremium)
		{
			MultisellData.getInstance().separateAndSend(MULTISELL, player, npc, false);
		}
		else
		{
			final NpcHtmlMessage msg = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0);
			msg.setHtml(buildNoPremiumHtml());
			player.sendPacket(msg);
		}
		return null;
	}

	private String buildNoPremiumHtml()
	{
		return "<html><body>" +
			"<center><br>" +
			"<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>" +
			"<font color=\"CDB67F\">Items Premium</font><br><br>" +
			"<img src=\"L2UI.SquareGray\" width=240 height=1><br><br>" +
			"<font color=\"FF6060\">Acceso restringido.</font><br><br>" +
			"<font color=\"B09878\">Esta tienda es exclusiva para<br>" +
			"jugadores con <font color=\"CDB67F\">cuenta Premium</font>.</font><br><br>" +
			"<font color=\"7A7060\">Activa tu Premium para acceder<br>" +
			"a los Divine Enchant Crystals.</font><br><br>" +
			"<img src=\"L2UI.SquareGray\" width=240 height=1><br>" +
			"</center></body></html>";
	}

	public static void main(String[] args)
	{
		new PremiumShop();
	}
}
