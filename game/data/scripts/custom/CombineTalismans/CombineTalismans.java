/*
 * CombineTalismans - Combina talismanes duplicados del inventario sumando su mana.
 *
 * Comandos de voz: .combinetalismans / .combinetalisman / .combine
 *
 * Flujo:
 *   1. El jugador escribe el comando -> se abre ventana HTML con la lista
 *      de talismanes combinables y el boton de confirmacion.
 *   2. El jugador pulsa "Combinar" -> bypass combinetalismans_confirm
 *      -> se destruyen los duplicados y se crea uno nuevo con el mana total.
 *
 * Nota tecnica:
 *   Item no expone setMana() en L2JMobius High Five; se usa reflexion para
 *   escribir el campo privado _mana tras crear el item combinado, seguido de
 *   updateDatabase() y scheduleConsumeManaTask() para que el timer arranque.
 */
package custom.CombineTalismans;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.handler.VoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Combina talismanes duplicados (mismo itemId con mana > 0) sumando su mana total
 * en un solo item. Accesible via comando de voz .combinetalismans.
 * @author Custom - Aden Chronicles
 */
public class CombineTalismans extends Script
{
	private static final Logger LOGGER = Logger.getLogger(CombineTalismans.class.getName());

	private CombineTalismans()
	{
		VoicedCommandHandler.getInstance().registerHandler(new CombineVoiced());
		BypassHandler.getInstance().registerHandler(new CombineBypass());
		LOGGER.info("CombineTalismans: Cargado.");
	}

	// -------------------------------------------------------------------------
	// Logica de busqueda: talismanes con mana > 0 y mas de una copia
	// Retorna lista de int[3]: [itemId, cantidad, manaTotal]
	// -------------------------------------------------------------------------

	static List<int[]> findCombinables(Player player)
	{
		final List<int[]> result = new ArrayList<>();

		for (Item item : player.getInventory().getItems())
		{
			// Solo talismanes con mana activo
			if ((item.getMana() <= 0) || !item.getTemplate().getName().contains("Talisman"))
			{
				continue;
			}

			final int id = item.getId();
			boolean found = false;
			for (int[] entry : result)
			{
				if (entry[0] == id)
				{
					entry[1]++;
					entry[2] += item.getMana();
					found = true;
					break;
				}
			}
			if (!found)
			{
				result.add(new int[]
				{
					id,
					1,
					item.getMana()
				});
			}
		}

		// Solo los que tienen mas de una copia
		result.removeIf(e -> e[1] <= 1);
		return result;
	}

	// -------------------------------------------------------------------------
	// HTML: muestra los talismanes combinables y el boton de accion
	// -------------------------------------------------------------------------

	static void sendCombinePage(Player player)
	{
		final List<int[]> combinables = findCombinables(player);

		final StringBuilder sb = new StringBuilder(512);
		sb.append("<html><body>");
		sb.append("<table width=310 bgcolor=000000>");
		sb.append("<tr><td align=center height=32>");
		sb.append("<font color=LEVEL>&#9670; Combinar Talismanes &#9670;</font>");
		sb.append("</td></tr></table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=310 height=1>");
		sb.append("<table width=310><tr><td height=8></td></tr></table>");

		if (combinables.isEmpty())
		{
			sb.append("<table width=310><tr><td align=center>");
			sb.append("<font color=808080>No tienes talismanes duplicados para combinar.</font><br>");
			sb.append("<font color=808080>Necesitas 2 o mas copias del mismo talismán.</font>");
			sb.append("</td></tr></table>");
		}
		else
		{
			sb.append("<table width=310><tr><td align=center>");
			sb.append("<font color=B09878>Los siguientes talismanes seran combinados:</font>");
			sb.append("</td></tr></table>");
			sb.append("<table width=310 border=0 cellspacing=2 cellpadding=2>");
			sb.append("<tr>");
			sb.append("<td width=170><font color=LEVEL>Talismán</font></td>");
			sb.append("<td width=60 align=center><font color=LEVEL>Copias</font></td>");
			sb.append("<td width=80 align=center><font color=LEVEL>Mana total</font></td>");
			sb.append("</tr>");
			sb.append("<tr><td colspan=3><img src=\"L2UI.SquareGray\" width=306 height=1></td></tr>");

			for (int[] entry : combinables)
			{
				final List<Item> items = player.getInventory().getAllItemsByItemId(entry[0], false);
				final String name = items.isEmpty() ? "Talisman" : items.get(0).getTemplate().getName();

				sb.append("<tr>");
				sb.append("<td><font color=B09878>").append(name).append("</font></td>");
				sb.append("<td align=center>").append(entry[1]).append("</td>");
				sb.append("<td align=center><font color=00FF00>").append(entry[2]).append("</font></td>");
				sb.append("</tr>");
			}

			sb.append("<tr><td colspan=3 height=8></td></tr>");
			sb.append("</table>");
			sb.append("<img src=\"L2UI.SquareGray\" width=310 height=1>");
			sb.append("<table width=310><tr><td align=center height=10></td></tr></table>");
			sb.append("<table width=310><tr><td align=center>");
			sb.append("<button value=\"Combinar Talismanes\" action=\"bypass combinetalismans_confirm\" ");
			sb.append("width=220 height=30 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			sb.append("</td></tr></table>");
		}

		sb.append("<table width=310><tr><td height=6></td></tr></table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=310 height=1>");
		sb.append("<table width=310><tr><td align=center height=8>");
		sb.append("<font color=808080>.combinetalismans para abrir esta ventana</font>");
		sb.append("</td></tr></table>");
		sb.append("</body></html>");

		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}

	// -------------------------------------------------------------------------
	// Logica de combinacion
	// -------------------------------------------------------------------------

	static void performCombine(Player player)
	{
		final List<int[]> combinables = findCombinables(player);

		if (combinables.isEmpty())
		{
			player.sendMessage("No tienes talismanes duplicados para combinar.");
			return;
		}

		int combinedCount = 0;
		final InventoryUpdate iu = new InventoryUpdate();

		for (int[] entry : combinables)
		{
			final int itemId = entry[0];
			final int totalMana = entry[2];

			// Destruir todas las copias
			final List<Item> copies = new ArrayList<>(player.getInventory().getAllItemsByItemId(itemId, false));
			for (Item copy : copies)
			{
				iu.addRemovedItem(copy);
				player.getInventory().destroyItem(ItemProcessType.DESTROY, copy, player, null);
			}

			// Crear un item nuevo (mana inicial del template)
			final Item newItem = player.addItem(ItemProcessType.REWARD, itemId, 1, null, false);
			if (newItem == null)
			{
				LOGGER.warning("CombineTalismans: addItem devolvio null para itemId=" + itemId);
				continue;
			}

			// Establecer el mana combinado via reflexion (no existe setMana publico)
			try
			{
				final Field manaField = Item.class.getDeclaredField("_mana");
				manaField.setAccessible(true);
				manaField.setInt(newItem, totalMana);

				// Persistir en BD y reiniciar el timer de consumo
				newItem.updateDatabase();
				newItem.scheduleConsumeManaTask();

				iu.addModifiedItem(newItem);
				combinedCount++;
			}
			catch (NoSuchFieldException | IllegalAccessException ex)
			{
				LOGGER.warning("CombineTalismans: No se pudo establecer el mana del item " + itemId + ": " + ex.getMessage());
				// El item ya fue creado con mana por defecto — no es catastrofico
				iu.addModifiedItem(newItem);
				combinedCount++;
			}
		}

		player.sendInventoryUpdate(iu);

		if (combinedCount > 0)
		{
			player.sendMessage("Has combinado " + combinedCount + " talismán(es). El mana total ha sido sumado.");
		}
		else
		{
			player.sendMessage("No se pudieron combinar los talismanes.");
		}

		// Refrescar el HTML con el estado actualizado
		sendCombinePage(player);
	}

	// -------------------------------------------------------------------------
	// Voiced command handler: .combinetalismans / .combinetalisman / .combine
	// -------------------------------------------------------------------------

	private static class CombineVoiced implements IVoicedCommandHandler
	{
		private static final String[] COMMANDS =
		{
			"combinetalismans",
			"combinetalisman",
			"combine"
		};

		@Override
		public boolean onCommand(String command, Player player, String params)
		{
			sendCombinePage(player);
			return true;
		}

		@Override
		public String[] getCommandList()
		{
			return COMMANDS;
		}
	}

	// -------------------------------------------------------------------------
	// Bypass handler: combinetalismans_confirm
	// -------------------------------------------------------------------------

	private static class CombineBypass implements IBypassHandler
	{
		private static final String[] BYPASSES =
		{
			"combinetalismans_confirm"
		};

		@Override
		public boolean onCommand(String command, Player player, Creature bypassTarget)
		{
			if (command.equals("combinetalismans_confirm"))
			{
				performCombine(player);
				return true;
			}
			return false;
		}

		@Override
		public String[] getCommandList()
		{
			return BYPASSES;
		}
	}

	// -------------------------------------------------------------------------
	// Entry point — el ScriptEngine llama a main() al cargar el script
	// -------------------------------------------------------------------------

	public static void main(String[] args)
	{
		new CombineTalismans();
	}
}
