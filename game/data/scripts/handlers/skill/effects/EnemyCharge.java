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
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.FlyType;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;

/**
 * Enemy Charge effect implementation.
 */
public class EnemyCharge extends AbstractEffect
{
	public EnemyCharge(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		if (effector.isMovementDisabled())
		{
			return;
		}
		
		// Get current position of the Creature
		final int curX = effector.getX();
		final int curY = effector.getY();
		final int curZ = effector.getZ();
		
		// Calculate distance (dx,dy) between current position and destination
		final double dx = effected.getX() - curX;
		final double dy = effected.getY() - curY;
		final double dz = effected.getZ() - curZ;
		final double distance = Math.sqrt((dx * dx) + (dy * dy));
		if (distance > 2000)
		{
			LOGGER.info("EffectEnemyCharge was going to use invalid coordinates for characters, getEffector: " + curX + "," + curY + " and getEffected: " + effected.getX() + "," + effected.getY());
			return;
		}
		
		int offset = Math.max(skill.getFlyRadius(), 30);
		
		// approximation for moving closer when z coordinates are different
		// TODO: handle Z axis movement better
		offset -= Math.abs(dz);
		if (offset < 5)
		{
			offset = 5;
		}
		
		// If no distance
		if ((distance < 1) || ((distance - offset) <= 0))
		{
			return;
		}
		
		// Calculate movement angles needed
		final double sin = dy / distance;
		final double cos = dx / distance;
		
		// Calculate the new destination with offset included
		final int x = curX + (int) ((distance - offset) * cos);
		final int y = curY + (int) ((distance - offset) * sin);
		final int z = effected.getZ();
		final Location destination = GeoEngine.getInstance().getValidLocation(effector.getX(), effector.getY(), effector.getZ(), x, y, z, effector.getInstanceId());
		effector.broadcastPacket(new FlyToLocation(effector, destination, FlyType.CHARGE));
		effector.setXYZ(destination);
		effector.broadcastPacket(new ValidateLocation(effector));
	}
}
