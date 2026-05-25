/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.skill.effects;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.item.enums.ShotType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * Magical soul damage effect implementation.
 * @author Adry_85
 */
public class MagicalSoulDamage extends AbstractEffect
{
	public MagicalSoulDamage(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.MAGICAL_ATTACK;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		if (effector.isAlikeDead())
		{
			return;
		}
		
		if (effected.isPlayer() && effected.asPlayer().isFakeDeath() && PlayerConfig.FAKE_DEATH_DAMAGE_STAND)
		{
			effected.stopFakeDeath(true);
		}
		
		final boolean sps = skill.useSpiritShot() && effector.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = skill.useSpiritShot() && effector.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		final boolean mcrit = Formulas.calcMCrit(effector.getMCriticalHit(effected, skill));
		final byte shld = Formulas.calcShldUse(effector, effected, skill);
		int damage = (int) Formulas.calcMagicDam(effector, effected, skill, shld, sps, bss, mcrit);
		if ((skill.getMaxSoulConsumeCount() > 0) && effector.isPlayer())
		{
			// Souls Formula (each soul increase +4.8%)
			final int chargedSouls = (effector.asPlayer().getChargedSouls() <= skill.getMaxSoulConsumeCount()) ? effector.asPlayer().getChargedSouls() : skill.getMaxSoulConsumeCount();
			damage *= 1 + (chargedSouls * 0.048);
		}
		
		if (damage > 0)
		{
			// Manage attack or cast break of the target (calculating rate, sending message...)
			if (!effected.isRaid() && Formulas.calcAtkBreak(effected, damage))
			{
				effected.breakAttack();
				effected.breakCast();
			}
			
			// Shield Deflect Magic: Reflect all damage on caster.
			if (effected.getStat().calcStat(Stat.VENGEANCE_SKILL_MAGIC_DAMAGE, 0, effected, skill) > Rnd.get(100))
			{
				effector.reduceCurrentHp(damage, effected, skill);
				effector.notifyDamageReceived(damage, effected, skill, mcrit, false);
			}
			else
			{
				effected.reduceCurrentHp(damage, effector, skill);
				effected.notifyDamageReceived(damage, effector, skill, mcrit, false);
				effector.sendDamageMessage(effected, damage, mcrit, false, false);
			}
		}
	}
}
