package handlers.chat.commands.voiced;

import custom.DressMe.DressMeData;
import custom.DressMe.DressMeManager;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * .dressme voiced command - L2 Zona Zero
 *
 * Commands:
 *   .dressme              - Open panel
 *   .dressme [slot] [id]  - Copy item
 *   .dressmeon            - Activate
 *   .dressmeof            - Deactivate
 *   .dressmereset         - Reset all
 *   .dressmetarget [slot] - Copy from target's slot
 */
public class DressMe implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS = { "dressme", "dressmeon", "dressmeof", "dressmereset", "dressmetarget" };

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
				// Nuclear reset: clear everything and clean all armor transmogs
				if (data.isEnabled())
					mgr.removeVisuals(player);

				data.clearAll();
				mgr.deleteAll(player.getObjectId());

				// Extra safety: remove transmogs from all armor items
				mgr.cleanAllArmorTransmogs(player);

				player.sendMessage("[DressMe] Todas las apariencias y datos residuales eliminados.");
				showPanel(player, data);
				break;

			case "dressmetarget":
				// Show target panel or handle copy action
				if (params == null || params.trim().isEmpty())
				{
					// Show target panel without params
					showTargetPanel(player, mgr, data, null);
				}
				else if (params.startsWith("copy"))
				{
					// Handle copy action: .dressmetarget copy [slot]
					final String[] parts = params.split("\\s+");
					if (parts.length >= 2)
					{
						handleTargetCopy(player, parts[1], mgr, data);
					}
					showTargetPanel(player, mgr, data, null);
				}
				else
				{
					// Show target panel (fallback)
					showTargetPanel(player, mgr, data, null);
				}
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

		// Header - sin Unicode, solo ASCII
		sb.append("<center>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br1>");
		sb.append("<font color=\"LEVEL\">-- DressMe - Apariencia Visual --</font><br>");
		sb.append("</center>");

		// Status
		sb.append("<center>");
		if (data.isEnabled())
			sb.append("<font color=\"00CC44\">Estado: ACTIVO</font><br>");
		else
			sb.append("<font color=\"999999\">Estado: INACTIVO</font><br>");
		sb.append("</center><br>");

		// Main action buttons
		sb.append("<center>");
		if (data.isEnabled())
		{
			sb.append("<button value=\"Desactivar\" action=\"bypass -h voice .dressmeof\" "
				+ "width=120 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		}
		else
		{
			sb.append("<button value=\"Activar\" action=\"bypass -h voice .dressmeon\" "
				+ "width=120 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		}
		sb.append("&nbsp;");
		sb.append("<button value=\"Resetear Todo\" action=\"bypass -h voice .dressmereset\" "
			+ "width=120 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		sb.append("</center><br>");

		// Equipment table - atributos simples, sin border/cellpadding/cellspacing
		sb.append("<table width=270 bgcolor=111111>");

		// Header row
		sb.append("<tr>");
		sb.append("<td width=80><font color=\"LEVEL\">Slot</font></td>");
		sb.append("<td width=110><font color=\"LEVEL\">Tu Equipo</font></td>");
		sb.append("<td width=80></td>");
		sb.append("</tr>");

		// Track if LEGS row should be skipped because it's covered by a FULL_ARMOR chest visual
		boolean legsSkip = false;

		for (int i = 0; i < SLOT_IDS.length; i++)
		{
			final int    slot     = SLOT_IDS[i];
			final String slotKey  = SLOT_KEYS[i];
			String       slotName = SLOT_NAMES[i];

			// Skip LEGS row if covered by a FULL_ARMOR visual on chest
			if (slot == DressMeManager.SLOT_LEGS && legsSkip)
			{
				continue;
			}

			final Item   equipped  = player.getInventory().getPaperdollItem(slot);
			final int    equipId   = equipped != null ? equipped.getId() : 0;
			final String equipName = equipId > 0 ? getItemName(equipId) : "Sin equipo";
			final int    visualId  = data.getVisualId(slot);
			final String visualName = visualId > 0 ? getItemName(visualId) : "";

			// Detect Full Armor: chest item that also covers legs
			boolean isFullArmor = false;
			if (equipId > 0 && slot == DressMeManager.SLOT_CHEST)
			{
				final ItemTemplate tpl = ItemData.getInstance().getTemplate(equipId);
				isFullArmor = (tpl != null && tpl.getBodyPart() == BodyPart.FULL_ARMOR);
				if (isFullArmor)
				{
					slotName = "Pecho+Piernas";
					legsSkip = true;
				}
			}
			// Also skip legs row if visual set is from a full armor
			if (slot == DressMeManager.SLOT_CHEST && visualId > 0)
			{
				final ItemTemplate vtpl = ItemData.getInstance().getTemplate(visualId);
				if (vtpl != null && vtpl.getBodyPart() == BodyPart.FULL_ARMOR)
				{
					slotName = "Pecho+Piernas";
					legsSkip = true;
				}
			}

			sb.append("<tr>");

			// Slot name
			sb.append("<td><font color=\"B09878\">").append(slotName).append("</font></td>");

			// Equipped + visual info
			sb.append("<td>");
			if (equipId > 0)
			{
				sb.append("<font color=\"C8C8A0\">").append(shortName(equipName)).append("</font>");
				if (visualId > 0)
				{
					sb.append("<br1><font color=\"00AA33\">&gt; ").append(shortName(visualName)).append("</font>");
				}
			}
			else
			{
				// No item in this slot — but there may be a visual from a previously saved full armor
				if (visualId > 0)
					sb.append("<font color=\"00AA33\">").append(shortName(visualName)).append("</font>");
				else
					sb.append("<font color=\"555555\">-</font>");
			}
			sb.append("</td>");

			// Action buttons
			sb.append("<td>");
			if (equipId > 0)
			{
				sb.append("<button value=\"Copiar\" "
					+ "action=\"bypass -h voice .dressme " + slotKey + " " + equipId + "\" "
					+ "width=55 height=18 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			}
			if (visualId > 0)
			{
				sb.append("<button value=\"Quitar\" "
					+ "action=\"bypass -h voice .dressme " + slotKey + " 0\" "
					+ "width=55 height=18 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			}
			sb.append("</td>");

			sb.append("</tr>");
		}

		sb.append("</table><br>");

		// Instructions - solo ASCII
		sb.append("<center>");
		sb.append("<font color=\"808080\">Presiona Copiar para guardar la apariencia</font><br>");
		sb.append("<font color=\"808080\">del item equipado, luego pulsa Activar.</font>");
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
			// Clear this slot
			data.setVisualId(slot, 0);
			mgr.deleteSlot(player.getObjectId(), slot);

			// If clearing chest, also clear legs (in case they were linked via FULL_ARMOR)
			if (slot == DressMeManager.SLOT_CHEST)
			{
				data.setVisualId(DressMeManager.SLOT_LEGS, 0);
				mgr.deleteSlot(player.getObjectId(), DressMeManager.SLOT_LEGS);
			}
		}
		else
		{
			final ItemTemplate tpl = ItemData.getInstance().getTemplate(itemId);
			if (tpl == null)
			{
				player.sendMessage("[DressMe] Item no encontrado.");
				showPanel(player, data);
				return;
			}

			final BodyPart bp = tpl.getBodyPart();
			final boolean isFullArmor = (bp == BodyPart.FULL_ARMOR);
			// alldress cubre todo el cuerpo (Formal Wear, trajes de novia, etc.)
			final boolean isFullBodyItem = (bp == BodyPart.ALLDRESS);

			if (isFullBodyItem)
			{
				// ALLDRESS (Formal Wear, trajes completos): el item REAL es UN solo
				// objeto en el slot de PECHO; piernas/guantes/BOTAS quedan VACIOS y por
				// eso el cliente camina en SILENCIO (modo traje, sin sonido de botas).
				// Si guardamos el visual tambien en botas/piernas/guantes, el cliente
				// coloca un item no-botas en el slot de botas y suena un paso por
				// defecto (el ruido reportado). Solucion: guardar SOLO en el pecho,
				// replicando exactamente como se equipa el item real.
				data.getAllVisuals().clear();
				mgr.deleteAll(player.getObjectId());

				data.setVisualId(DressMeManager.SLOT_CHEST, itemId);
				mgr.saveSlot(player.getObjectId(), DressMeManager.SLOT_CHEST, itemId);

				player.sendMessage("[DressMe] " + tpl.getName() + " COPIADO - cubrira tu apariencia completa (silencioso).");
			}
			else if (slot == DressMeManager.SLOT_CHEST && isFullArmor)
			{
				// FULL_ARMOR: covers chest + legs
				data.setVisualId(slot, itemId);
				mgr.saveSlot(player.getObjectId(), slot, itemId);

				data.setVisualId(DressMeManager.SLOT_LEGS, itemId);
				mgr.saveSlot(player.getObjectId(), DressMeManager.SLOT_LEGS, itemId);

				player.sendMessage("[DressMe] Full Armor detectado: visual guardado para Pecho y Piernas.");
			}
			else
			{
				// Normal item
				data.setVisualId(slot, itemId);
				mgr.saveSlot(player.getObjectId(), slot, itemId);
				player.sendMessage("[DressMe] Visual '" + parts[0] + "' -> " + tpl.getName() + " guardado.");
			}
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
	// Target Panel - Show target equipment with copy buttons
	// ─────────────────────────────────────────────────────────────

	private void showTargetPanel(Player player, DressMeManager mgr, DressMeData data, String dummy)
	{
		// Check if player has a target
		Object targetObj = player.getTarget();
		if (targetObj == null || !(targetObj instanceof Player))
		{
			player.sendMessage("[DressMe] Debes hacer target en otro jugador.");
			showPanel(player, data);
			return;
		}

		final Player target = (Player) targetObj;

		// Build target panel HTML
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");

		// Header
		sb.append("<center>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br1>");
		sb.append("<font color=\"LEVEL\">-- DressMe - Copiar de Jugador --</font><br>");
		sb.append("Target: <font color=\"00CCFF\">").append(target.getName()).append("</font><br>");
		sb.append("</center><br>");

		// Equipment table
		sb.append("<table width=270 bgcolor=111111>");

		// Header row
		sb.append("<tr>");
		sb.append("<td width=80><font color=\"LEVEL\">Slot</font></td>");
		sb.append("<td width=130><font color=\"LEVEL\">Equipo</font></td>");
		sb.append("<td width=60></td>");
		sb.append("</tr>");

		// List target's equipment
		for (int i = 0; i < SLOT_IDS.length; i++)
		{
			final int slot = SLOT_IDS[i];
			final String slotKey = SLOT_KEYS[i];
			final String slotName = SLOT_NAMES[i];

			final Item equipped = target.getInventory().getPaperdollItem(slot);
			if (equipped == null) continue;

			final int equipId = equipped.getId();
			final String equipName = getItemName(equipId);

			sb.append("<tr>");
			sb.append("<td><font color=\"B09878\">").append(slotName).append("</font></td>");
			sb.append("<td><font color=\"C8C8A0\">").append(shortName(equipName)).append("</font></td>");
			sb.append("<td>");
			sb.append("<button value=\"Copiar\" "
				+ "action=\"bypass -h voice .dressmetarget copy " + slotKey + "\" "
				+ "width=55 height=18 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			sb.append("</td>");
			sb.append("</tr>");
		}

		sb.append("</table><br>");

		// Back button
		sb.append("<center>");
		sb.append("<button value=\"Volver\" action=\"bypass -h voice .dressme\" "
			+ "width=80 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		sb.append("</center>");

		sb.append("</body></html>");

		final NpcHtmlMessage msg = new NpcHtmlMessage();
		msg.setHtml(sb.toString());
		player.sendPacket(msg);
	}

	private void handleTargetCopy(Player player, String slotName, DressMeManager mgr, DressMeData data)
	{
		// Get target
		Object targetObj = player.getTarget();
		if (targetObj == null || !(targetObj instanceof Player))
		{
			player.sendMessage("[DressMe] Debes hacer target en otro jugador.");
			return;
		}

		final Player target = (Player) targetObj;
		final int slot = parseSlot(slotName);

		if (slot < 0)
		{
			player.sendMessage("[DressMe] Slot invalido.");
			return;
		}

		final Item targetItem = target.getInventory().getPaperdollItem(slot);
		if (targetItem == null)
		{
			player.sendMessage("[DressMe] El target no tiene equipo en ese slot.");
			return;
		}

		final int copyItemId = targetItem.getId();
		final ItemTemplate copyTpl = ItemData.getInstance().getTemplate(copyItemId);

		if (copyTpl == null)
		{
			player.sendMessage("[DressMe] Item no encontrado.");
			return;
		}

		// Check weapon slot compatibility
		if (isWeaponSlot(slot))
		{
			final Item playerItem = player.getInventory().getPaperdollItem(slot);
			if (playerItem != null)
			{
				final ItemTemplate playerTpl = ItemData.getInstance().getTemplate(playerItem.getId());
				if (playerTpl != null && !areWeaponTypesCompatible(playerTpl, copyTpl))
				{
					final String myType  = (playerTpl instanceof Weapon) ? ((Weapon) playerTpl).getItemType().toString() : "?";
					final String tgtType = (copyTpl   instanceof Weapon) ? ((Weapon) copyTpl).getItemType().toString()   : "?";
					player.sendMessage("[DressMe] No puedes copiar ese arma. Tipo incompatible.");
					player.sendMessage("[DressMe] Tu tipo: " + myType + "  /  Target tipo: " + tgtType);
					return;
				}
			}
		}

		// Save the visual
		data.setVisualId(slot, copyItemId);
		mgr.saveSlot(player.getObjectId(), slot, copyItemId);

		// If Full Armor, also save to legs
		if (slot == DressMeManager.SLOT_CHEST && copyTpl.getBodyPart() == BodyPart.FULL_ARMOR)
		{
			data.setVisualId(DressMeManager.SLOT_LEGS, copyItemId);
			mgr.saveSlot(player.getObjectId(), DressMeManager.SLOT_LEGS, copyItemId);
			player.sendMessage("[DressMe] Copiado de " + target.getName() + ": " + copyTpl.getName() + " (Full Armor)");
		}
		else
		{
			player.sendMessage("[DressMe] Copiado de " + target.getName() + ": " + copyTpl.getName());
		}

		// Re-apply if active
		if (data.isEnabled())
		{
			mgr.removeVisuals(player);
			mgr.applyVisuals(player);
		}
	}

	/**
	 * Check if a slot is a weapon slot (right hand or left hand)
	 */
	private boolean isWeaponSlot(int slot)
	{
		return slot == DressMeManager.SLOT_RHAND || slot == DressMeManager.SLOT_LHAND;
	}

	/**
	 * Check if two weapon templates are compatible (same WeaponType).
	 * Uses WeaponType enum for precise matching: DUAL==DUAL, BOW==BOW, DAGGER==DAGGER, etc.
	 */
	private boolean areWeaponTypesCompatible(ItemTemplate playerWeapon, ItemTemplate targetWeapon)
	{
		// Must be Weapon instances to compare types
		if (!(playerWeapon instanceof Weapon) || !(targetWeapon instanceof Weapon))
			return true;

		final WeaponType playerType = ((Weapon) playerWeapon).getItemType();
		final WeaponType targetType = ((Weapon) targetWeapon).getItemType();

		// Exact match required: DUAL=DUAL, BOW=BOW, DAGGER=DAGGER, SWORD=SWORD, etc.
		return playerType == targetType;
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
