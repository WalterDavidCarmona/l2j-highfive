package custom.DressMe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * DressMe Manager - pure script, zero JAR modifications.
 * Visual changes use item.setTransmogId() which is already hooked into
 * Inventory.getPaperdollItemDisplayId() → all packets update automatically.
 *
 * Paperdoll slots (L2JMobius H5 Inventory constants):
 *   RHAND=5  CHEST=6  LHAND=7  GLOVES=10  LEGS=11  FEET=12  CLOAK=23  BELT=24
 */
public class DressMeManager
{
	private static final Logger LOGGER = Logger.getLogger(DressMeManager.class.getName());

	// Slot constants
	public static final int SLOT_RHAND  = 5;
	public static final int SLOT_CHEST  = 6;
	public static final int SLOT_LHAND  = 7;
	public static final int SLOT_GLOVES = 10;
	public static final int SLOT_LEGS   = 11;
	public static final int SLOT_FEET   = 12;
	public static final int SLOT_CLOAK  = 23;
	public static final int SLOT_BELT   = 24;

	public static final int[] ALL_SLOTS = { SLOT_RHAND, SLOT_CHEST, SLOT_LHAND, SLOT_GLOVES, SLOT_LEGS, SLOT_FEET, SLOT_CLOAK, SLOT_BELT };

	private static final String LOAD_SQL   = "SELECT slot, visual_id FROM character_dressme WHERE charId=?";
	private static final String SAVE_SQL   = "INSERT INTO character_dressme (charId, slot, visual_id) VALUES (?,?,?) ON DUPLICATE KEY UPDATE visual_id=VALUES(visual_id)";
	private static final String DELETE_SQL = "DELETE FROM character_dressme WHERE charId=? AND slot=?";
	private static final String DELETE_ALL = "DELETE FROM character_dressme WHERE charId=?";

	private final ConcurrentHashMap<Integer, DressMeData> _data = new ConcurrentHashMap<>();

	// ─────────────────────────────────────────────────────────────
	// Public API
	// ─────────────────────────────────────────────────────────────

	/** Load player's DressMe config from DB. Call on login. */
	public void load(Player player)
	{
		final DressMeData d = new DressMeData();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_SQL))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					d.setVisualId(rs.getInt("slot"), rs.getInt("visual_id"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "DressMe: load failed for " + player.getName(), e);
		}
		_data.put(player.getObjectId(), d);
	}

	/** Remove from memory on logout. Does NOT revert visuals (items restore on equip). */
	public void unload(int objectId)
	{
		_data.remove(objectId);
	}

	public DressMeData getData(int objectId)
	{
		return _data.computeIfAbsent(objectId, k -> new DressMeData());
	}

	// ─────────────────────────────────────────────────────────────
	// Apply / Remove visuals via item.setTransmogId()
	// ─────────────────────────────────────────────────────────────

	/**
	 * Activates DressMe: unequips each item, sets transmogId, re-equips, then broadcasts.
	 * This replicates exactly how the native Transmog (Zumzi NPC) applies skins.
	 */
	public void applyVisuals(Player player)
	{
		final DressMeData d = getData(player.getObjectId());
		if (d.isEmpty())
		{
			player.sendMessage("[DressMe] No tienes apariencias configuradas.");
			return;
		}

		d.clearSavedTransmogs();
		boolean changed = false;

		for (int slot : ALL_SLOTS)
		{
			final int visualId = d.getVisualId(slot);
			if (visualId <= 0) continue;

			// Unequip → set transmog → re-equip (same as Transmog NPC)
			// unEquipSlot uses item.getTemplate().getBodyPart() so it handles
			// FULL_ARMOR, LR_HAND (2H weapons), separate CHEST/LEGS, etc. automatically
			final Item item = unEquipSlot(player, slot);
			if (item == null) continue;

			d.setSavedTransmog(slot, item.getTransmogId());
			item.setTransmogId(visualId);
			player.getInventory().equipItem(item);
			changed = true;
		}

		d.setEnabled(true);

		if (changed)
		{
			player.broadcastInfo();
			player.sendMessage("[DressMe] Apariencia activada.");
		}
		else
		{
			player.sendMessage("[DressMe] No habia items equipados en los slots configurados.");
		}
	}

	/**
	 * Deactivates DressMe: unequips each item, restores original transmogId, re-equips.
	 * Clears ALL armor slots to ensure no visual artifacts remain.
	 */
	public void removeVisuals(Player player)
	{
		final DressMeData d = getData(player.getObjectId());
		d.setEnabled(false);

		boolean changed = false;

		// Clean ALL armor slots to remove any visual artifacts
		for (int slot : ALL_SLOTS)
		{
			final Item item = player.getInventory().getPaperdollItem(slot);
			if (item == null) continue;

			// Check if this slot has a saved transmog from DressMe activation
			final int originalTransmog = d.getSavedTransmog(slot);
			final int currentTransmog = item.getTransmogId();

			// Only modify if the item actually has a transmog set
			if (currentTransmog > 0)
			{
				// Unequip the item
				final BodyPart bp = item.getTemplate().getBodyPart();
				if (bp == null || bp == BodyPart.NONE) continue;

				final Item unequipped = player.getInventory().unEquipItemInBodySlot(bp);
				if (unequipped == null) continue;

				// Restore original transmog or clear it
				if (originalTransmog > 0)
					unequipped.setTransmogId(originalTransmog);
				else
					unequipped.removeTransmog();

				// Re-equip
				player.getInventory().equipItem(unequipped);
				changed = true;
			}
		}

		d.clearSavedTransmogs();

		if (changed)
		{
			player.broadcastInfo();
			player.sendMessage("[DressMe] Apariencia desactivada. Equipo real restaurado.");
		}
		else
		{
			player.sendMessage("[DressMe] DressMe desactivado.");
		}
	}

	/**
	 * Unequips the item in a paperdoll slot using the item's ACTUAL BodyPart.
	 * This handles Full Armor, 2H weapons, etc. correctly — same approach as Transmog NPC.
	 */
	private Item unEquipSlot(Player player, int paperdollSlot)
	{
		// Get the item currently in this paperdoll slot
		final Item item = player.getInventory().getPaperdollItem(paperdollSlot);
		if (item == null) return null;

		// Use the item's own bodypart to unequip — works for LR_HAND, FULL_ARMOR, etc.
		final BodyPart bp = item.getTemplate().getBodyPart();
		if (bp == null || bp == BodyPart.NONE) return null;

		return player.getInventory().unEquipItemInBodySlot(bp);
	}

	// ─────────────────────────────────────────────────────────────
	// Persistence
	// ─────────────────────────────────────────────────────────────

	public void saveSlot(int objectId, int slot, int visualId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SAVE_SQL))
		{
			ps.setInt(1, objectId);
			ps.setInt(2, slot);
			ps.setInt(3, visualId);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "DressMe: saveSlot failed", e);
		}
	}

	public void deleteSlot(int objectId, int slot)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_SQL))
		{
			ps.setInt(1, objectId);
			ps.setInt(2, slot);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "DressMe: deleteSlot failed", e);
		}
	}

	public void deleteAll(int objectId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_ALL))
		{
			ps.setInt(1, objectId);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "DressMe: deleteAll failed", e);
		}
	}

	// ─────────────────────────────────────────────────────────────
	// Singleton
	// ─────────────────────────────────────────────────────────────

	public static DressMeManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		static final DressMeManager INSTANCE = new DressMeManager();
	}
}
