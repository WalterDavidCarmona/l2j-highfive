/*
 * DressMe System for L2JMobius High Five
 * L2 Zona Zero - Custom Implementation
 */
package org.l2jmobius.gameserver.custom.dressme;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;

/**
 * Singleton manager for the DressMe visual appearance system.
 * Stores visual overrides per player (keyed by objectId).
 * Used by CharInfo/UserInfo packets to override paperdoll display IDs.
 */
public class DressMeManager
{
	private static final Logger LOGGER = Logger.getLogger(DressMeManager.class.getName());

	private static final String LOAD_SQL   = "SELECT slot, visual_id FROM character_dressme WHERE charId=?";
	private static final String SAVE_SQL   = "INSERT INTO character_dressme (charId, slot, visual_id) VALUES (?,?,?) ON DUPLICATE KEY UPDATE visual_id=VALUES(visual_id)";
	private static final String DELETE_SQL = "DELETE FROM character_dressme WHERE charId=? AND slot=?";
	private static final String DELETE_ALL = "DELETE FROM character_dressme WHERE charId=?";
	private static final String CREATE_SQL =
		"CREATE TABLE IF NOT EXISTS `character_dressme` (" +
		"`charId` INT NOT NULL," +
		"`slot` TINYINT NOT NULL," +
		"`visual_id` INT NOT NULL DEFAULT 0," +
		"PRIMARY KEY (`charId`,`slot`)" +
		") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

	/** In-memory map: objectId -> DressMeData */
	private final ConcurrentHashMap<Integer, DressMeData> _data = new ConcurrentHashMap<>();

	private DressMeManager()
	{
		createTable();
		LOGGER.info("DressMeManager: Initialized.");
	}

	// ──────────────────────────────────────────────────────────────
	// Public API used by CharInfo / UserInfo packets
	// ──────────────────────────────────────────────────────────────

	/**
	 * Returns the visual item ID that should be displayed for the given
	 * player + paperdoll slot, or 0 if no override is active.
	 */
	public int getVisualId(int objectId, int slot)
	{
		final DressMeData d = _data.get(objectId);
		if (d == null || !d.isEnabled())
		{
			return 0;
		}
		return d.getVisualId(slot);
	}

	/**
	 * Returns true if this player has DressMe active AND has a visual set
	 * for the requested slot.
	 */
	public boolean isActiveForSlot(int objectId, int slot)
	{
		return getVisualId(objectId, slot) > 0;
	}

	// ──────────────────────────────────────────────────────────────
	// Session management
	// ──────────────────────────────────────────────────────────────

	/** Load a player's DressMe data from DB on login. */
	public void load(int objectId)
	{
		final DressMeData data = new DressMeData();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_SQL))
		{
			ps.setInt(1, objectId);
			try (ResultSet rs = ps.executeQuery())
			{
				boolean hasRows = false;
				while (rs.next())
				{
					data.setVisualId(rs.getInt("slot"), rs.getInt("visual_id"));
					hasRows = true;
				}
				if (hasRows)
				{
					data.setEnabled(true);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "DressMeManager: Failed to load data for charId=" + objectId, e);
		}
		_data.put(objectId, data);
	}

	/** Remove from memory on logout. */
	public void unload(int objectId)
	{
		_data.remove(objectId);
	}

	/** Get (or create) data object for a player. */
	public DressMeData getData(int objectId)
	{
		return _data.computeIfAbsent(objectId, k -> new DressMeData());
	}

	// ──────────────────────────────────────────────────────────────
	// Persistence
	// ──────────────────────────────────────────────────────────────

	/** Save a single visual override to DB. */
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
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "DressMeManager: Failed to save slot for charId=" + objectId, e);
		}
	}

	/** Delete a single slot override from DB. */
	public void deleteSlot(int objectId, int slot)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_SQL))
		{
			ps.setInt(1, objectId);
			ps.setInt(2, slot);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "DressMeManager: Failed to delete slot for charId=" + objectId, e);
		}
	}

	/** Delete ALL overrides from DB for a player. */
	public void deleteAll(int objectId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_ALL))
		{
			ps.setInt(1, objectId);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "DressMeManager: Failed to delete all for charId=" + objectId, e);
		}
	}

	// ──────────────────────────────────────────────────────────────
	// Init
	// ──────────────────────────────────────────────────────────────

	private void createTable()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(CREATE_SQL))
		{
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "DressMeManager: Failed to create table.", e);
		}
	}

	// ──────────────────────────────────────────────────────────────
	// Singleton
	// ──────────────────────────────────────────────────────────────

	public static DressMeManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		static final DressMeManager INSTANCE = new DressMeManager();
	}
}
