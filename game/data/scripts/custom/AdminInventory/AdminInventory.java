/*
 * Admin Inventory Editor
 *
 * Voiced command (.inventory / .inv) solo para GM: con un jugador en target,
 * abre una ventana HTML dinamica con su inventario y permite en tiempo real:
 *   - Eliminar objetos
 *   - Agregar objetos
 *   - Modificar la cantidad de objetos (stackables)
 *
 * Construido con el patron dinamico de PvpZone: StringBuilder + NpcHtmlMessage
 * refrescado tras cada accion via IBypassHandler.
 */
package custom.AdminInventory;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.handler.VoicedCommandHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminInventory extends Script
{
	private static final int ITEMS_PER_PAGE = 10;

	// ---------------------------------------------------------------------------
	// Constructor: registra el voiced command y el bypass handler.
	// ---------------------------------------------------------------------------
	public AdminInventory()
	{
		VoicedCommandHandler.getInstance().registerHandler(new InventoryVoiced());
		BypassHandler.getInstance().registerHandler(new InventoryBypass());
		LOGGER.info("AdminInventory: Voiced command .inventory (GM) registrado.");
	}

	// ---------------------------------------------------------------------------
	// Voiced command: .inventory / .inv  (requiere target = jugador, solo GM)
	// ---------------------------------------------------------------------------
	private static class InventoryVoiced implements IVoicedCommandHandler
	{
		private static final String[] COMMANDS =
		{
			"inventory",
			"inv"
		};

		@Override
		public boolean onCommand(String command, Player player, String params)
		{
			if (!player.isGM())
			{
				player.sendMessage("Solo un GM puede usar este comando.");
				return false;
			}

			final Player target = resolveTargetFromSelection(player);
			if (target == null)
			{
				player.sendMessage("Debes seleccionar (target) a un jugador primero.");
				return false;
			}

			showInventory(player, target, 0);
			return true;
		}

		@Override
		public String[] getCommandList()
		{
			return COMMANDS;
		}
	}

	// ---------------------------------------------------------------------------
	// Bypass handler para las acciones dinamicas.
	// ---------------------------------------------------------------------------
	private static class InventoryBypass implements IBypassHandler
	{
		private static final String[] COMMANDS =
		{
			"admininv_show",
			"admininv_del",
			"admininv_edit",
			"admininv_setqty",
			"admininv_add"
		};

		@Override
		public boolean onCommand(String command, Player player, Creature bypassTarget)
		{
			if (!player.isGM())
			{
				player.sendMessage("Solo un GM puede usar este comando.");
				return false;
			}

			final String[] tok = command.trim().split("\\s+");
			final String action = tok[0];

			// Todos requieren al menos el targetId.
			if (tok.length < 2)
			{
				return false;
			}

			final Player target = resolvePlayer(tok[1]);
			if (target == null)
			{
				player.sendMessage("El jugador objetivo ya no esta disponible.");
				return false;
			}

			switch (action)
			{
				case "admininv_show":
				{
					final int page = (tok.length > 2) ? parseInt(tok[2], 0) : 0;
					showInventory(player, target, page);
					break;
				}
				case "admininv_del":
				{
					if (tok.length > 2)
					{
						deleteItem(player, target, parseInt(tok[2], 0));
					}
					showInventory(player, target, lastPageFromArgs(tok, 3));
					break;
				}
				case "admininv_edit":
				{
					if (tok.length > 2)
					{
						showItemEdit(player, target, parseInt(tok[2], 0), lastPageFromArgs(tok, 3));
					}
					break;
				}
				case "admininv_setqty":
				{
					// admininv_setqty <tid> <objId> <qty> [page]
					if (tok.length > 3)
					{
						setItemQuantity(player, target, parseInt(tok[2], 0), parseLong(tok[3], -1));
					}
					showInventory(player, target, lastPageFromArgs(tok, 4));
					break;
				}
				case "admininv_add":
				{
					// admininv_add <tid> <itemId> <count> [page]
					if (tok.length > 3)
					{
						addItem(player, target, parseInt(tok[2], 0), parseLong(tok[3], 0));
					}
					showInventory(player, target, lastPageFromArgs(tok, 4));
					break;
				}
			}
			return true;
		}

		@Override
		public String[] getCommandList()
		{
			return COMMANDS;
		}
	}

	// ---------------------------------------------------------------------------
	// Acciones sobre el inventario
	// ---------------------------------------------------------------------------
	private static void deleteItem(Player gm, Player target, int objectId)
	{
		final PlayerInventory inv = target.getInventory();
		final Item item = inv.getItemByObjectId(objectId);
		if (item == null)
		{
			return;
		}

		final String name = itemLabel(item);
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addRemovedItem(item);
		inv.destroyItem(ItemProcessType.DESTROY, item, target, gm);
		target.sendInventoryUpdate(iu);

		gm.sendMessage("[Inv] Eliminado de " + target.getName() + ": " + name);
	}

	private static void addItem(Player gm, Player target, int itemId, long count)
	{
		if ((itemId <= 0) || (count <= 0))
		{
			gm.sendMessage("[Inv] Item id / cantidad invalidos.");
			return;
		}

		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		if (template == null)
		{
			gm.sendMessage("[Inv] No existe un item con id " + itemId + ".");
			return;
		}

		target.addItem(ItemProcessType.REWARD, itemId, count, gm, false);
		gm.sendMessage("[Inv] Agregado a " + target.getName() + ": " + template.getName() + " x" + count);
	}

	private static void setItemQuantity(Player gm, Player target, int objectId, long newCount)
	{
		final PlayerInventory inv = target.getInventory();
		final Item item = inv.getItemByObjectId(objectId);
		if (item == null)
		{
			return;
		}

		if (newCount < 0)
		{
			gm.sendMessage("[Inv] Cantidad invalida.");
			return;
		}

		final int itemId = item.getId();
		final String name = itemLabel(item);

		// Cantidad 0 = eliminar completamente.
		if (newCount == 0)
		{
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addRemovedItem(item);
			inv.destroyItem(ItemProcessType.DESTROY, item, target, gm);
			target.sendInventoryUpdate(iu);
			gm.sendMessage("[Inv] " + name + " eliminado (cantidad 0) de " + target.getName() + ".");
			return;
		}

		if (!item.isStackable())
		{
			gm.sendMessage("[Inv] " + name + " no es apilable; no se puede modificar la cantidad.");
			return;
		}

		if (newCount == item.getCount())
		{
			return;
		}

		// Reemplazar el stack por la nueva cantidad (seguro para apilables).
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addRemovedItem(item);
		inv.destroyItem(ItemProcessType.DESTROY, item, target, gm);
		target.sendInventoryUpdate(iu);
		target.addItem(ItemProcessType.REWARD, itemId, newCount, gm, false);

		gm.sendMessage("[Inv] " + name + " ajustado a x" + newCount + " en " + target.getName() + ".");
	}

	// ---------------------------------------------------------------------------
	// Render: lista de inventario paginada
	// ---------------------------------------------------------------------------
	private static void showInventory(Player gm, Player target, int page)
	{
		final List<Item> items = new ArrayList<>(target.getInventory().getItems());
		items.sort((a, b) -> itemLabel(a).compareToIgnoreCase(itemLabel(b)));

		final int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) ITEMS_PER_PAGE));
		final int curPage = Math.max(0, Math.min(page, totalPages - 1));
		final int from = curPage * ITEMS_PER_PAGE;
		final int to = Math.min(from + ITEMS_PER_PAGE, items.size());
		final int tid = target.getObjectId();

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<center>");
		sb.append("<font color=\"LEVEL\">Inventario - ").append(target.getName()).append("</font><br>");
		sb.append("<font color=\"808080\">Objetos: ").append(items.size())
			.append(" | Adena: ").append(String.format("%,d", target.getAdena())).append("</font>");
		sb.append("</center>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"290\" height=\"1\"><br>");

		// --- Agregar item ---
		sb.append("<table width=\"290\" bgcolor=\"1A1A1A\">");
		sb.append("<tr><td><font color=\"C8A84B\">Agregar item</font></td></tr>");
		sb.append("<tr><td>");
		sb.append("ID: <edit var=\"additemid\" width=\"70\" height=\"15\"> ");
		sb.append("Cant: <edit var=\"addcount\" width=\"70\" height=\"15\"> ");
		sb.append("<button value=\"Agregar\" action=\"bypass -h admininv_add ").append(tid)
			.append(" $additemid $addcount ").append(curPage)
			.append("\" width=\"70\" height=\"21\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		sb.append("</td></tr>");
		sb.append("</table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"290\" height=\"1\"><br>");

		// --- Cabecera de la lista ---
		sb.append("<table width=\"290\">");
		sb.append("<tr>");
		sb.append("<td width=\"150\"><font color=\"999999\">Item</font></td>");
		sb.append("<td width=\"80\"><font color=\"999999\">Cantidad</font></td>");
		sb.append("<td width=\"60\"><font color=\"999999\">Accion</font></td>");
		sb.append("</tr>");
		sb.append("</table>");

		// --- Filas ---
		for (int i = from; i < to; i++)
		{
			final Item item = items.get(i);
			final String bg = ((i - from) % 2 == 0) ? "0A0A0A" : "151515";
			sb.append("<table width=\"290\" bgcolor=\"").append(bg).append("\"><tr>");
			sb.append("<td width=\"150\">").append(itemLabel(item));
			if (item.isEquipped())
			{
				sb.append(" <font color=\"00A0FF\">[E]</font>");
			}
			sb.append("</td>");
			sb.append("<td width=\"80\">").append(String.format("%,d", item.getCount())).append("</td>");
			sb.append("<td width=\"60\">");
			sb.append("<a action=\"bypass -h admininv_edit ").append(tid).append(" ").append(item.getObjectId()).append(" ").append(curPage)
				.append("\"><font color=\"FFCC00\">Edit</font></a> ");
			sb.append("<a action=\"bypass -h admininv_del ").append(tid).append(" ").append(item.getObjectId()).append(" ").append(curPage)
				.append("\"><font color=\"FF6666\">Del</font></a>");
			sb.append("</td>");
			sb.append("</tr></table>");
		}

		if (items.isEmpty())
		{
			sb.append("<br><center><font color=\"808080\">El inventario esta vacio.</font></center>");
		}

		// --- Paginacion ---
		sb.append("<br><img src=\"L2UI.SquareGray\" width=\"290\" height=\"1\"><br>");
		sb.append("<center><table width=\"290\"><tr>");
		sb.append("<td width=\"96\" align=\"left\">");
		if (curPage > 0)
		{
			sb.append("<a action=\"bypass -h admininv_show ").append(tid).append(" ").append(curPage - 1)
				.append("\"><font color=\"LEVEL\">&lt;&lt; Anterior</font></a>");
		}
		sb.append("</td>");
		sb.append("<td width=\"98\" align=\"center\"><font color=\"808080\">Pag. ").append(curPage + 1).append("/").append(totalPages).append("</font></td>");
		sb.append("<td width=\"96\" align=\"right\">");
		if (curPage < (totalPages - 1))
		{
			sb.append("<a action=\"bypass -h admininv_show ").append(tid).append(" ").append(curPage + 1)
				.append("\"><font color=\"LEVEL\">Siguiente &gt;&gt;</font></a>");
		}
		sb.append("</td>");
		sb.append("</tr></table></center>");

		sb.append("</body></html>");

		sendHtml(gm, sb.toString());
	}

	// ---------------------------------------------------------------------------
	// Render: edicion de un item puntual (modificar cantidad)
	// ---------------------------------------------------------------------------
	private static void showItemEdit(Player gm, Player target, int objectId, int page)
	{
		final Item item = target.getInventory().getItemByObjectId(objectId);
		if (item == null)
		{
			showInventory(gm, target, page);
			return;
		}

		final int tid = target.getObjectId();
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<center>");
		sb.append("<font color=\"LEVEL\">Editar Item</font><br>");
		sb.append("<font color=\"808080\">").append(target.getName()).append("</font>");
		sb.append("</center>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"290\" height=\"1\"><br>");
		sb.append("<table width=\"290\">");
		sb.append("<tr><td>Item:</td><td>").append(itemLabel(item)).append("</td></tr>");
		sb.append("<tr><td>Item ID:</td><td>").append(item.getId()).append("</td></tr>");
		sb.append("<tr><td>ObjectId:</td><td>").append(item.getObjectId()).append("</td></tr>");
		sb.append("<tr><td>Cantidad actual:</td><td>").append(String.format("%,d", item.getCount())).append("</td></tr>");
		sb.append("<tr><td>Apilable:</td><td>").append(item.isStackable() ? "Si" : "No").append("</td></tr>");
		sb.append("</table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"290\" height=\"1\"><br>");

		if (item.isStackable())
		{
			sb.append("<center>");
			sb.append("Nueva cantidad: <edit var=\"qty\" width=\"100\" height=\"15\"><br><br>");
			sb.append("<button value=\"Aplicar\" action=\"bypass -h admininv_setqty ").append(tid).append(" ").append(objectId).append(" $qty ").append(page)
				.append("\" width=\"100\" height=\"21\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			sb.append("<font color=\"808080\"> (0 = eliminar)</font>");
			sb.append("</center>");
		}
		else
		{
			sb.append("<center><font color=\"FF6666\">Item no apilable: la cantidad es fija.</font></center>");
		}

		sb.append("<br><center>");
		sb.append("<button value=\"Volver\" action=\"bypass -h admininv_show ").append(tid).append(" ").append(page)
			.append("\" width=\"100\" height=\"21\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		sb.append("</center>");
		sb.append("</body></html>");

		sendHtml(gm, sb.toString());
	}

	// ---------------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------------
	private static void sendHtml(Player gm, String html)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(0);
		msg.setHtml(html);
		gm.sendPacket(msg);
	}

	private static String itemLabel(Item item)
	{
		String name = item.getTemplate().getName();
		if (name.length() > 22)
		{
			name = name.substring(0, 22);
		}
		if (item.getEnchantLevel() > 0)
		{
			return "+" + item.getEnchantLevel() + " " + name;
		}
		return name;
	}

	private static Player resolveTargetFromSelection(Player gm)
	{
		if ((gm.getTarget() != null) && gm.getTarget().isPlayer())
		{
			return gm.getTarget().asPlayer();
		}
		return null;
	}

	private static Player resolvePlayer(String objectIdStr)
	{
		final int objectId = parseInt(objectIdStr, 0);
		if (objectId == 0)
		{
			return null;
		}
		return World.getInstance().getPlayer(objectId);
	}

	private static int lastPageFromArgs(String[] tok, int idx)
	{
		return (tok.length > idx) ? parseInt(tok[idx], 0) : 0;
	}

	private static int parseInt(String s, int def)
	{
		try
		{
			return Integer.parseInt(s.trim());
		}
		catch (Exception e)
		{
			return def;
		}
	}

	private static long parseLong(String s, long def)
	{
		try
		{
			return Long.parseLong(s.trim());
		}
		catch (Exception e)
		{
			return def;
		}
	}

	// ---------------------------------------------------------------------------
	// Entry point (auto-cargado por el ScriptEngine)
	// ---------------------------------------------------------------------------
	public static void main(String[] args)
	{
		new AdminInventory();
	}
}
