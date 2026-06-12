package org.l2jmobius.gameserver.managers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CustomPvpZoneRegistry
{
	private static int _returnX;
	private static int _returnY;
	private static int _returnZ;
	private static boolean _partyBlockEnabled;
	private static final Set<Integer> REGISTERED = ConcurrentHashMap.newKeySet();

	public static void setReturnLocation(int x, int y, int z)
	{
		_returnX = x;
		_returnY = y;
		_returnZ = z;
	}

	public static int getReturnX() { return _returnX; }
	public static int getReturnY() { return _returnY; }
	public static int getReturnZ() { return _returnZ; }

	public static void setPartyBlockEnabled(boolean enabled)
	{
		_partyBlockEnabled = enabled;
	}

	public static boolean isPartyBlockEnabled() { return _partyBlockEnabled; }

	public static void register(int objectId)
	{
		REGISTERED.add(objectId);
	}

	public static void unregister(int objectId)
	{
		REGISTERED.remove(objectId);
	}

	public static boolean isRegistered(int objectId)
	{
		return REGISTERED.contains(objectId);
	}

	public static void clear()
	{
		REGISTERED.clear();
	}
}

