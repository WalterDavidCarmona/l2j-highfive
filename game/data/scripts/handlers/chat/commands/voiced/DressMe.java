/*
 * DressMe System for L2JMobius High Five
 * L2 Zona Zero - Custom Implementation
 */
package handlers.chat.commands.voiced;

import org.l2jmobius.gameserver.custom.dressme.DressMeData;
import org.l2jmobius.gameserver.custom.dressme.DressMeManager;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * .dressme voiced command handler.
 *
 * Commands:
 *   .dressme          - Open DressMe panel
 *   .dressmeon        - Enable DressMe (apply saved visuals)
 *   .dressmeof        - Disable DressMe (show real equipment)
 *   .dressmereset     - Clear all visual overrides
 *
 * The HTML panel sends bypass commands handled here for slot-by-slot config.
 */
public class DressMe implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"dressme",
		"dressmeon",
		"dressmeof",
		"dressmereset"
	};

	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		if (player == null)
		{
			return false;
		}

		final DressMeManager mgr = DressMeManager.getInstance();
		final DressMeData data = mgr.getData(player.getObjectId());

		switch (command)
		{
			case "dressme":
			{
				// If params given: ".dressme chest 6611" → set slot
				if (params != null && !params.trim().isEmpty())
				{
					handleSlotSet(player, params.trim());
					return true;
				}
				showMainPanel(player, data);
				break;
			}
			case "dressmeon":
			{
				if (data.isEmpty())
				{
					player.sendMessage("[DressMe] No tienes ninguna apariencia guardada. Configura los slots primero.");
					showMainPanel(player, data);
					break;
				}
				data.setEnabled(true);
				player.broadcastUserInfo();
				player.sendMessage("[DressMe] ¡Apariencia activada!");
				showMainPanel(player, data);
				break;
			}
			case "dressmeof":
			{
				data.setEnabled(false);
				player.broadcastUserInfo();
				player.sendMessage("[DressMe] Apariencia desactivada. Mostrando equipo real.");
				showMainPanel(player, data);
				break;
			}
			case "dressmereset":
			{
				data.clearAll();
				mgr.deleteAll(player.getObjectId());
				player.broadcastUserInfo();
				player.sendMessage("[DressMe] Todas las apariencias han sido eliminadas.");
				showMainPanel(player, data);
				break;
			}
		}
		return true;
	}

	// ──────────────────────────────────────────────────────────────
	// HTML Panel
	// ──────────────────────────────────────────────────────────────

	private void showMainPanel(Player player, DressMeData data)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<center><br>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br1>");
		sb.append("<font color=\"LEVEL\">✦ DressMe - Apariencia Visual ✦</font><br>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
		sb.append("</center>");

		// Status badge
		if (data.isEnabled())
		{
			sb.append("<center><font color=\"00FF00\">● ACTIVO</font> - Tu apariencia visual está aplicada.<br></center>");
		}
		else
		{
			sb.append("<center><font color=\"FF4444\">○ INACTIVO</font> - Mostrando equipo real.<br></center>");
		}

		sb.append("<br>");

		// Action buttons
		sb.append("<center>");
		if (data.isEnabled())
		{
			sb.append("<button value=\"Desactivar\" action=\"bypass -h voice $dressmeof\" width=120 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">&nbsp;");
		}
		else
		{
			sb.append("<button value=\"Activar\" action=\"bypass -h voice $dressmeon\" width=120 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">&nbsp;");
		}
		sb.append("&nbsp;");
		sb.append("<button value=\"Reset Todo\" action=\"bypass -h voice $dressmereset\" width=120 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		sb.append("</center><br>");

		// Slot configuration table
		sb.append("<table width=280 bgcolor=111111>");
		sb.append("<tr><td width=120><font color=\"LEVEL\">Slot</font></td><td width=80><font color=\"LEVEL\">Equipo Actual</font></td><td width=80><font color=\"LEVEL\">Visual (ID)</font></td></tr>");

		addSlotRow(sb, player, data, DressMeData.SLOT_RHAND,  "Arma Der.");
		addSlotRow(sb, player, data, DressMeData.SLOT_LHAND,  "Escudo/Izq.");
		addSlotRow(sb, player, data, DressMeData.SLOT_CHEST,  "Pecho");
		addSlotRow(sb, player, data, DressMeData.SLOT_LEGS,   "Piernas");
		addSlotRow(sb, player, data, DressMeData.SLOT_GLOVES, "Guantes");
		addSlotRow(sb, player, data, DressMeData.SLOT_FEET,   "Botas");
		addSlotRow(sb, player, data, DressMeData.SLOT_CLOAK,  "Capa");
		addSlotRow(sb, player, data, DressMeData.SLOT_BELT,   "Cinturón");

		sb.append("</table><br>");

		// Instructions to set a slot
		sb.append("<center>");
		sb.append("<font color=\"808080\">Para cambiar un slot escribe en el chat:</font><br>");
		sb.append("<font color=\"FFFF00\">.dressme [slot] [itemId]</font><br>");
		sb.append("<font color=\"808080\">Ej: </font><font color=\"FFFF00\">.dressme chest 6611</font><br>");
		sb.append("<font color=\"808080\">Slots: rhand lhand chest legs gloves feet cloak belt</font><br>");
		sb.append("</center>");

		sb.append("</body></html>");

		final NpcHtmlMessage msg = new NpcHtmlMessage();
		msg.setHtml(sb.toString());
		player.sendPacket(msg);
	}

	private void addSlotRow(StringBuilder sb, Player player, DressMeData data, int slot, String slotName)
	{
		final int equipped = getEquippedItemId(player, slot);
		final int visual   = data.getVisualId(slot);

		sb.append("<tr>");
		sb.append("<td><font color=\"B09878\">").append(slotName).append("</font></td>");
		sb.append("<td><font color=\"808080\">").append(equipped > 0 ? equipped : "-").append("</font></td>");
		if (visual > 0)
		{
			sb.append("<td><font color=\"00FF00\">").append(visual).append("</font></td>");
		}
		else
		{
			sb.append("<td><font color=\"666666\">-</font></td>");
		}
		sb.append("</tr>");
	}

	private int getEquippedItemId(Player player, int slot)
	{
		final Item item = player.getInventory().getPaperdollItem(slot);
		return item != null ? item.getId() : 0;
	}

	// ──────────────────────────────────────────────────────────────
	// Handle ".dressme [slot] [itemId]" from chat
	// ──────────────────────────────────────────────────────────────

	/**
	 * Called when player types ".dressme chest 6611" etc.
	 * params = "chest 6611"
	 */
	private boolean handleSlotSet(Player player, String params)
	{
		if (params == null || params.isEmpty())
		{
			return false;
		}

		final String[] parts = params.trim().split("\\s+");
		if (parts.length < 2)
		{
			return false;
		}

		final int slot = parseSlotName(parts[0]);
		if (slot < 0)
		{
			player.sendMessage("[DressMe] Slot inválido. Usa: rhand, lhand, lrhand, chest, legs, gloves, feet, cloak");
			return false;
		}

		int itemId;
		try
		{
			itemId = Integer.parseInt(parts[1]);
		}
		catch (NumberFormatException e)
		{
			player.sendMessage("[DressMe] ID de item inválido.");
			return false;
		}

		final DressMeManager mgr = DressMeManager.getInstance();
		final DressMeData data   = mgr.getData(player.getObjectId());

		if (itemId == 0)
		{
			data.setVisualId(slot, 0);
			mgr.deleteSlot(player.getObjectId(), slot);
			player.sendMessage("[DressMe] Apariencia del slot '" + parts[0] + "' eliminada.");
		}
		else
		{
			data.setVisualId(slot, itemId);
			mgr.saveSlot(player.getObjectId(), slot, itemId);
			player.sendMessage("[DressMe] Slot '" + parts[0] + "' → item ID " + itemId + " guardado.");
		}

		if (data.isEnabled())
		{
			player.broadcastUserInfo();
		}

		showMainPanel(player, data);
		return true;
	}

	private int parseSlotName(String name)
	{
		switch (name.toLowerCase())
		{
			case "rhand":  return DressMeData.SLOT_RHAND;
			case "lhand":  return DressMeData.SLOT_LHAND;
			case "chest":  return DressMeData.SLOT_CHEST;
			case "legs":   return DressMeData.SLOT_LEGS;
			case "gloves": return DressMeData.SLOT_GLOVES;
			case "feet":   return DressMeData.SLOT_FEET;
			case "cloak":  return DressMeData.SLOT_CLOAK;
			case "belt":   return DressMeData.SLOT_BELT;
			default:       return -1;
		}
	}

	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
