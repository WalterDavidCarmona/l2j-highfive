/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.skill.effects;

import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectFlag;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.FlyType;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;

/**
 * Throw Up effect implementation.
 */
public class ThrowUp extends AbstractEffect
{
	public ThrowUp(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public int getEffectFlags()
	{
		return EffectFlag.STUNNED.getMask();
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		// Get current position of the Creature.
		final int curX = effected.getX();
		final int curY = effected.getY();
		final int curZ = effected.getZ();
		
		// Calculate distance between effector and effected current position.
		final double dx = effector.getX() - curX;
		final double dy = effector.getY() - curY;
		final double dz = effector.getZ() - curZ;
		final double distance = Math.sqrt((dx * dx) + (dy * dy));
		if (distance > 2000)
		{
			LOGGER.info("EffectThrow was going to use invalid coordinates for characters, getEffected: " + curX + "," + curY + " and getEffector: " + effector.getX() + "," + effector.getY());
			return;
		}
		
		int offset = Math.min((int) distance + skill.getFlyRadius(), 1400);
		double cos;
		double sin;
		
		// Approximation for moving futher when z coordinates are different.
		// TODO: Handle Z axis movement better.
		offset += Math.abs(dz);
		if (offset < 5)
		{
			offset = 5;
		}
		
		// If no distance.
		if (distance < 1)
		{
			return;
		}
		
		// Calculate movement angles needed.
		sin = dy / distance;
		cos = dx / distance;
		
		// Calculate the new destination with offset included.
		final int x = effector.getX() - (int) (offset * cos);
		final int y = effector.getY() - (int) (offset * sin);
		final int z = effected.getZ();
		final Location destination = GeoEngine.getInstance().getValidLocation(effected.getX(), effected.getY(), effected.getZ(), x, y, z, effected.getInstanceId());
		effected.broadcastPacket(new FlyToLocation(effected, destination, FlyType.THROW_UP));
		effected.setXYZ(destination);
		effected.broadcastPacket(new ValidateLocation(effected));
	}
}
