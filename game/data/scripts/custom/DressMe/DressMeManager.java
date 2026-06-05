package custom.DressMe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.actor.Player;
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
	 * Activates DressMe: applies stored visual_id as transmogId on each slot's item.
	 * Saves the item's original transmogId so it can be restored on deactivate.
	 */
	public void applyVisuals(Player player)
	{
		final DressMeData d = getData(player.getObjectId());
		if (d.isEmpty())
		{
			player.sendMessage("[DressMe] No tienes apariencias configuradas. Usa .dressme [slot] [itemId]");
			return;
		}

		d.clearSavedTransmogs();

		for (int slot : ALL_SLOTS)
		{
			final int visualId = d.getVisualId(slot);
			if (visualId <= 0) continue;

			final Item item = player.getInventory().getPaperdollItem(slot);
			if (item == null) continue;

			// Save original transmog so we can restore it
			d.setSavedTransmog(slot, item.getTransmogId());

			// Apply visual
			item.setTransmogId(visualId);
		}

		d.setEnabled(true);
		player.broadcastUserInfo();
		player.sendMessage("[DressMe] Apariencia activada.");
	}

	/**
	 * Deactivates DressMe: restores each slot's original transmogId.
	 */
	public void removeVisuals(Player player)
	{
		final DressMeData d = getData(player.getObjectId());
		d.setEnabled(false);

		for (int slot : ALL_SLOTS)
		{
			final Item item = player.getInventory().getPaperdollItem(slot);
			if (item == null) continue;

			final int originalTransmog = d.getSavedTransmog(slot);
			if (originalTransmog > 0)
			{
				item.setTransmogId(originalTransmog);
			}
			else
			{
				item.removeTransmog();
			}
		}

		d.clearSavedTransmogs();
		player.broadcastUserInfo();
		player.sendMessage("[DressMe] Apariencia desactivada. Equipo real restaurado.");
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
