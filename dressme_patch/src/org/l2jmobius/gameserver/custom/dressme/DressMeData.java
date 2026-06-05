/*
 * DressMe System for L2JMobius High Five
 * L2 Zona Zero - Custom Implementation
 */
package org.l2jmobius.gameserver.custom.dressme;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Stores per-player DressMe visual override data.
 * Keyed by Paperdoll slot constants.
 */
public class DressMeData
{
	private volatile boolean _enabled = false;
	private final Map<Integer, Integer> _visualSlots = new ConcurrentHashMap<>();

	// Paperdoll slot constants - L2JMobius High Five (from Inventory.class)
	public static final int SLOT_RHAND   = 5;
	public static final int SLOT_CHEST   = 6;  // Body armor
	public static final int SLOT_LHAND   = 7;  // Shield / left weapon
	public static final int SLOT_GLOVES  = 10;
	public static final int SLOT_LEGS    = 11;
	public static final int SLOT_FEET    = 12;
	public static final int SLOT_CLOAK   = 23;
	public static final int SLOT_BELT    = 24;

	public boolean isEnabled()
	{
		return _enabled;
	}

	public void setEnabled(boolean enabled)
	{
		_enabled = enabled;
	}

	/**
	 * Returns visual item ID for a paperdoll slot, or 0 if not set.
	 */
	public int getVisualId(int slot)
	{
		return _visualSlots.getOrDefault(slot, 0);
	}

	/**
	 * Sets visual item ID for a paperdoll slot. 0 = remove override.
	 */
	public void setVisualId(int slot, int itemId)
	{
		if (itemId <= 0)
		{
			_visualSlots.remove(slot);
		}
		else
		{
			_visualSlots.put(slot, itemId);
		}
	}

	public boolean hasVisual(int slot)
	{
		return _visualSlots.containsKey(slot) && _visualSlots.get(slot) > 0;
	}

	public Map<Integer, Integer> getAllVisuals()
	{
		return _visualSlots;
	}

	public void clearAll()
	{
		_visualSlots.clear();
		_enabled = false;
	}

	public boolean isEmpty()
	{
		return _visualSlots.isEmpty();
	}
}
