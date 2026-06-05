package handlers.chat.commands.voiced;

import custom.DressMe.DressMeData;
import custom.DressMe.DressMeManager;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * .dressme voiced command - L2 Zona Zero
 *
 * Bypass format (VoiceCommand handler requires dot at position 6):
 *   bypass -h voice .dressme [slot] [itemId]
 *   bypass -h voice .dressmeon
 *   bypass -h voice .dressmeof
 *   bypass -h voice .dressmereset
 */
public class DressMe implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS = { "dressme", "dressmeon", "dressmeof", "dressmereset" };

	// Slot display names
	private static final String[] SLOT_NAMES  = { "Arma Derecha", "Escudo / Izq.", "Pecho", "Piernas", "Guantes", "Botas", "Capa", "Cinturon" };
	private static final int[]    SLOT_IDS    = {
		DressMeManager.SLOT_RHAND,
		DressMeManager.SLOT_LHAND,
		DressMeManager.SLOT_CHEST,
		DressMeManager.SLOT_LEGS,
		DressMeManager.SLOT_GLOVES,
		DressMeManager.SLOT_FEET,
		DressMeManager.SLOT_CLOAK,
		DressMeManager.SLOT_BELT
	};
	private static final String[] SLOT_KEYS   = { "rhand", "lhand", "chest", "legs", "gloves", "feet", "cloak", "belt" };

	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		if (player == null) return false;

		final DressMeManager mgr  = DressMeManager.getInstance();
		final DressMeData    data = mgr.getData(player.getObjectId());

		switch (command)
		{
			case "dressme":
				if (params != null && !params.trim().isEmpty())
					handleSlotSet(player, params.trim(), mgr, data);
				else
					showPanel(player, data);
				break;

			case "dressmeon":
				mgr.applyVisuals(player);
				showPanel(player, mgr.getData(player.getObjectId()));
				break;

			case "dressmeof":
				mgr.removeVisuals(player);
				showPanel(player, mgr.getData(player.getObjectId()));
				break;

			case "dressmereset":
				if (data.isEnabled()) mgr.removeVisuals(player);
				data.clearAll();
				mgr.deleteAll(player.getObjectId());
				player.sendMessage("[DressMe] Apariencias eliminadas.");
				showPanel(player, data);
				break;
		}
		return true;
	}

	// ─────────────────────────────────────────────────────────────
	// Main HTML Panel
	// ─────────────────────────────────────────────────────────────

	private void showPanel(Player player, DressMeData data)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");

		// ── Header ──
		sb.append("<center>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=292 height=32><br1>");
		sb.append("<font color=\"LEVEL\" name=\"hs9\">✦ DressMe ✦</font><br1>");
		sb.append("<font color=\"808080\">Apariencia Visual - L2 Zona Zero</font><br>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=292 height=32><br>");
		sb.append("</center>");

		// ── Status + main buttons ──
		sb.append("<table width=292><tr>");
		sb.append("<td width=146 align=center>");
		if (data.isEnabled())
		{
			sb.append("<font color=\"00CC44\">◆ ACTIVO</font>");
		}
		else
		{
			sb.append("<font color=\"888888\">◇ INACTIVO</font>");
		}
		sb.append("</td>");
		sb.append("<td width=146 align=center>");
		if (data.isEnabled())
		{
			sb.append("<button value=\"  Desactivar  \" action=\"bypass -h voice .dressmeof\" "
				+ "width=130 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		}
		else
		{
			sb.append("<button value=\"  Activar  \" action=\"bypass -h voice .dressmeon\" "
				+ "width=130 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		}
		sb.append("</td></tr></table>");
		sb.append("<center><button value=\" Resetear Todo \" action=\"bypass -h voice .dressmereset\" "
			+ "width=130 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center><br>");

		// ── Equipment slots table ──
		sb.append("<table width=292 bgcolor=0A0A0A border=0 cellpadding=2 cellspacing=0>");

		// Table header
		sb.append("<tr bgcolor=1A1A2E>");
		sb.append("<td width=90><font color=9090FF>Slot</font></td>");
		sb.append("<td width=110><font color=9090FF>Tu Equipo</font></td>");
		sb.append("<td width=60><font color=9090FF>Visual</font></td>");
		sb.append("<td width=32></td>");
		sb.append("</tr>");

		for (int i = 0; i < SLOT_IDS.length; i++)
		{
			final int    slot     = SLOT_IDS[i];
			final String slotKey  = SLOT_KEYS[i];
			final String slotName = SLOT_NAMES[i];

			final Item   equipped  = player.getInventory().getPaperdollItem(slot);
			final int    equipId   = equipped != null ? equipped.getId() : 0;
			final String equipName = equipId > 0 ? getItemName(equipId) : "-";
			final int    visualId  = data.getVisualId(slot);
			final String visualName = visualId > 0 ? getItemName(visualId) : "-";

			// Alternate row colors
			final String rowColor = (i % 2 == 0) ? "131320" : "0A0A18";
			sb.append("<tr bgcolor=").append(rowColor).append(">");

			// Slot name
			sb.append("<td><font color=B0A060>").append(slotName).append("</font></td>");

			// Equipped item - with "Copiar" button if item is equipped
			sb.append("<td>");
			if (equipId > 0)
			{
				sb.append("<font color=C8C8A0>").append(shortName(equipName)).append("</font>");
			}
			else
			{
				sb.append("<font color=555555>Sin equipo</font>");
			}
			sb.append("</td>");

			// Current visual
			sb.append("<td>");
			if (visualId > 0)
			{
				sb.append("<font color=00DD44>").append(shortName(visualName)).append("</font>");
			}
			else
			{
				sb.append("<font color=444444>-</font>");
			}
			sb.append("</td>");

			// Action buttons
			sb.append("<td>");
			if (equipId > 0)
			{
				// "Copiar" → sets the equipped item as visual
				sb.append("<button value=\"Copiar\" "
					+ "action=\"bypass -h voice .dressme " + slotKey + " " + equipId + "\" "
					+ "width=55 height=18 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			}
			else if (visualId > 0)
			{
				// "X" → remove visual
				sb.append("<button value=\" X \" "
					+ "action=\"bypass -h voice .dressme " + slotKey + " 0\" "
					+ "width=28 height=18 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			}
			sb.append("</td>");

			sb.append("</tr>");

			// If visual is set AND different from equipped → show extra row with remove button
			if (visualId > 0 && equipId > 0)
			{
				sb.append("<tr bgcolor=").append(rowColor).append(">");
				sb.append("<td></td>");
				sb.append("<td colspan=2><font color=555555>Visual activo: </font><font color=00AA33>")
					.append(shortName(visualName)).append("</font></td>");
				sb.append("<td><button value=\" X \" "
					+ "action=\"bypass -h voice .dressme " + slotKey + " 0\" "
					+ "width=28 height=18 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">"
					+ "</td>");
				sb.append("</tr>");
			}
		}
		sb.append("</table>");

		// ── Instructions ──
		sb.append("<br><center>");
		sb.append("<font color=606060>Presiona </font><font color=FFFF00>Copiar</font>");
		sb.append("<font color=606060> para usar la apariencia de tu equipo actual.<br>");
		sb.append("Luego presiona </font><font color=00CC44>Activar</font>");
		sb.append("<font color=606060> para aplicar los cambios.</font>");
		sb.append("</center>");

		sb.append("</body></html>");

		final NpcHtmlMessage msg = new NpcHtmlMessage();
		msg.setHtml(sb.toString());
		player.sendPacket(msg);
	}

	// ─────────────────────────────────────────────────────────────
	// Slot Set Handler
	// ─────────────────────────────────────────────────────────────

	private void handleSlotSet(Player player, String params, DressMeManager mgr, DressMeData data)
	{
		final String[] parts = params.split("\\s+");
		if (parts.length < 2)
		{
			player.sendMessage("[DressMe] Uso: .dressme [slot] [itemId]");
			showPanel(player, data);
			return;
		}

		final int slot = parseSlot(parts[0]);
		if (slot < 0)
		{
			player.sendMessage("[DressMe] Slot invalido.");
			showPanel(player, data);
			return;
		}

		int itemId;
		try { itemId = Integer.parseInt(parts[1]); }
		catch (NumberFormatException e)
		{
			player.sendMessage("[DressMe] ID de item invalido.");
			showPanel(player, data);
			return;
		}

		if (itemId == 0)
		{
			data.setVisualId(slot, 0);
			mgr.deleteSlot(player.getObjectId(), slot);
		}
		else
		{
			data.setVisualId(slot, itemId);
			mgr.saveSlot(player.getObjectId(), slot, itemId);

			final String itemName = getItemName(itemId);
			player.sendMessage("[DressMe] Visual '" + parts[0] + "' -> " + itemName + " guardado.");
		}

		// Re-apply if already active
		if (data.isEnabled())
		{
			mgr.removeVisuals(player);
			mgr.applyVisuals(player);
		}

		showPanel(player, data);
	}

	// ─────────────────────────────────────────────────────────────
	// Helpers
	// ─────────────────────────────────────────────────────────────

	private String getItemName(int itemId)
	{
		if (itemId <= 0) return "-";
		final ItemTemplate t = ItemData.getInstance().getTemplate(itemId);
		return t != null ? t.getName() : "ID:" + itemId;
	}

	/** Truncates long names for the HTML table */
	private String shortName(String name)
	{
		if (name == null) return "-";
		return name.length() > 16 ? name.substring(0, 14) + ".." : name;
	}

	private int parseSlot(String name)
	{
		switch (name.toLowerCase())
		{
			case "rhand":  return DressMeManager.SLOT_RHAND;
			case "lhand":  return DressMeManager.SLOT_LHAND;
			case "chest":  return DressMeManager.SLOT_CHEST;
			case "legs":   return DressMeManager.SLOT_LEGS;
			case "gloves": return DressMeManager.SLOT_GLOVES;
			case "feet":   return DressMeManager.SLOT_FEET;
			case "cloak":  return DressMeManager.SLOT_CLOAK;
			case "belt":   return DressMeManager.SLOT_BELT;
			default:       return -1;
		}
	}

	@Override
	public String[] getCommandList() { return VOICED_COMMANDS; }
}
