package handlers.chat.commands.voiced;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class Noble implements IVoicedCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(Noble.class.getName());

	private static final String[] VOICED_COMMANDS =
	{
		"noble"
	};

	private static boolean ENABLED = true;
	private static int ITEM_ID = 6673;
	private static long ITEM_COUNT = 10;
	private static boolean ALLOW_REUSE = false;

	public Noble()
	{
		final Properties props = new Properties();
		try (InputStream is = new FileInputStream("./config/Custom/Noble.ini"))
		{
			props.load(is);
			ENABLED = Boolean.parseBoolean(props.getProperty("NobleCmdEnabled", "True").trim());
			ITEM_ID = Integer.parseInt(props.getProperty("NobleCmdItemId", "6673").trim());
			ITEM_COUNT = Long.parseLong(props.getProperty("NobleCmdItemCount", "10").trim());
			ALLOW_REUSE = Boolean.parseBoolean(props.getProperty("NobleCmdAllowReuse", "False").trim());
		}
		catch (Exception e)
		{
			LOGGER.warning("Noble: No se pudo cargar config/Custom/Noble.ini: " + e.getMessage());
		}

		LOGGER.info("Noble: VoiceCommand .noble cargado. Enabled=" + ENABLED
			+ " ItemId=" + ITEM_ID + " Count=" + ITEM_COUNT + " AllowReuse=" + ALLOW_REUSE);
	}

	private void sendHtml(Player player, String body)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(5);
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append(body);
		sb.append("</body></html>");
		msg.setHtml(sb.toString());
		player.sendPacket(msg);
	}

	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		if (!ENABLED)
		{
			sendHtml(player,
				"<center><br><br>" +
				"<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>" +
				"<font color=\"FF0000\">Comando no disponible</font><br><br>" +
				"El comando <font color=\"LEVEL\">.noble</font> esta desactivado en este momento.<br>" +
				"</center>");
			return false;
		}

		final ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(ITEM_ID);
		final String itemName = (itemTemplate != null) ? itemTemplate.getName() : ("Item #" + ITEM_ID);

		if (player.isNoble() && !ALLOW_REUSE)
		{
			sendHtml(player,
				"<center><br><br>" +
				"<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>" +
				"<font color=\"LEVEL\">Estado Noblesse</font><br><br>" +
				"<img src=\"L2UI.SquareGray\" width=200 height=1><br><br>" +
				"Ya posees el status <font color=\"93FFA8\">Noblesse</font>.<br>" +
				"No es necesario volver a obtenerlo.<br><br>" +
				"<img src=\"L2UI.SquareGray\" width=200 height=1><br>" +
				"</center>");
			return false;
		}

		final long currentCount = player.getInventory().getInventoryItemCount(ITEM_ID, -1);

		if (currentCount < ITEM_COUNT)
		{
			sendHtml(player,
				"<center><br><br>" +
				"<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>" +
				"<font color=\"LEVEL\">Estado Noblesse</font><br><br>" +
				"<img src=\"L2UI.SquareGray\" width=200 height=1><br><br>" +
				"Para obtener el status <font color=\"93FFA8\">Noblesse</font> necesitas:<br><br>" +
				"<table width=200>" +
				"<tr><td align=right width=100><font color=\"LEVEL\">" + itemName + "</font></td>" +
				"<td width=10></td>" +
				"<td align=left width=90>x <font color=\"LEVEL\">" + ITEM_COUNT + "</font></td></tr>" +
				"</table><br>" +
				"<img src=\"L2UI.SquareGray\" width=200 height=1><br>" +
				"Tienes actualmente: <font color=\"" + (currentCount == 0 ? "FF0000" : "FFAA00") + "\">" + currentCount + "</font> / <font color=\"LEVEL\">" + ITEM_COUNT + "</font><br><br>" +
				"</center>");
			return false;
		}

		if (!player.destroyItemByItemId(ItemProcessType.FEE, ITEM_ID, ITEM_COUNT, player, true))
		{
			sendHtml(player,
				"<center><br><br>" +
				"<font color=\"FF0000\">Error al procesar el pago.</font><br>" +
				"Intenta nuevamente.<br>" +
				"</center>");
			return false;
		}

		player.setNoble(true);
		sendHtml(player,
			"<center><br><br>" +
			"<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>" +
			"<font color=\"LEVEL\">Estado Noblesse</font><br><br>" +
			"<img src=\"L2UI.SquareGray\" width=200 height=1><br><br>" +
			"<font color=\"93FFA8\">Felicitaciones, " + player.getName() + "!</font><br><br>" +
			"Has obtenido el status <font color=\"93FFA8\">Noblesse</font>.<br>" +
			"Ahora tienes acceso a todas las habilidades<br>" +
			"y ventajas de los Nobles.<br><br>" +
			"<img src=\"L2UI.SquareGray\" width=200 height=1><br><br>" +
			"<font color=\"B09878\">" + itemName + " x" + ITEM_COUNT + " consumido.</font><br><br>" +
			"<img src=\"L2UI.SquareGray\" width=200 height=1><br>" +
			"</center>");
		return true;
	}

	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
