package custom.DressMe;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores per-player DressMe configuration.
 * visual_id = item ID to display.
 * saved_transmog = original transmogId before DressMe was applied (to restore on deactivate).
 */
public class DressMeData
{
	private boolean _enabled = false;

	// slot -> desired visual item ID
	private final Map<Integer, Integer> _visuals = new HashMap<>();

	// slot -> original transmogId before DressMe (to restore when turned off)
	private final Map<Integer, Integer> _savedTransmogs = new HashMap<>();

	public boolean isEnabled() { return _enabled; }
	public void setEnabled(boolean v) { _enabled = v; }

	public int getVisualId(int slot) { return _visuals.getOrDefault(slot, 0); }
	public void setVisualId(int slot, int id)
	{
		if (id <= 0) _visuals.remove(slot);
		else _visuals.put(slot, id);
	}

	public int getSavedTransmog(int slot) { return _savedTransmogs.getOrDefault(slot, 0); }
	public void setSavedTransmog(int slot, int id) { _savedTransmogs.put(slot, id); }
	public void clearSavedTransmogs() { _savedTransmogs.clear(); }

	public Map<Integer, Integer> getAllVisuals() { return _visuals; }

	public boolean isEmpty() { return _visuals.isEmpty(); }

	public void clearAll()
	{
		_visuals.clear();
		_savedTransmogs.clear();
		_enabled = false;
	}
}
