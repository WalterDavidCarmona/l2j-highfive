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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.CancelReturnConfig;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.EffectScope;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.stats.Formulas;

/**
 * Steal Abnormal effect implementation.
 * @author Adry_85, Zoey76
 */
public class StealAbnormal extends AbstractEffect
{
	private final String _slot;
	private final int _rate;
	private final int _max;
	
	public StealAbnormal(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_slot = params.getString("slot", null);
		_rate = params.getInt("rate", 0);
		_max = params.getInt("max", 0);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.STEAL_ABNORMAL;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		if ((effected == null) || !effected.isPlayer() || (effector == effected))
		{
			return;
		}

		final List<BuffInfo> toSteal = Formulas.calcCancelStealEffects(effector, effected, skill, _slot, _rate, _max);
		if (toSteal.isEmpty())
		{
			return;
		}

		// Verificar si CancelReturn esta activo y aplica a este escenario.
		final boolean shouldReturn = CancelReturnConfig.CANCEL_RETURN_ON
			&& !(effector.isPlayer() && !CancelReturnConfig.CANCEL_RETURN_PLAYER)
			&& !((effector.isMonster() || effector.isRaid()) && !CancelReturnConfig.CANCEL_RETURN_MOB)
			&& (CancelReturnConfig.CANCEL_RETURN_PLAYER_OLYS || !effected.asPlayer().isInOlympiadMode());

		// Capturar el tiempo restante de cada buff ANTES del robo.
		// remove() puede resetear el campo time del BuffInfo a 0.
		final Map<Skill, Integer> timeSnapshot = shouldReturn ? new LinkedHashMap<>() : null;
		if (shouldReturn)
		{
			for (BuffInfo info : toSteal)
			{
				timeSnapshot.put(info.getSkill(), info.getTime());
			}
		}

		// Realizar el robo: transferir cada buff de la victima al ladron.
		for (BuffInfo infoToSteal : toSteal)
		{
			// Invertir effected y effector para que el buff quede en el ladron.
			final BuffInfo stolen = new BuffInfo(effected, effector, infoToSteal.getSkill());
			stolen.setAbnormalTime(infoToSteal.getTime()); // Copiar el tiempo restante.

			// Aplicar los efectos mediante el template para incluir todos los scopes.
			infoToSteal.getSkill().applyEffectScope(EffectScope.GENERAL, stolen, true, true);
			effected.getEffectList().remove(SkillFinishType.REMOVED, infoToSteal);
			effector.getEffectList().add(stolen);
		}

		// Si CancelReturn esta activo, programar la devolucion de los buffs robados.
		if (shouldReturn)
		{
			ThreadPool.schedule(() ->
			{
				if (!effected.isPlayer() || effected.isDead() || !effected.asPlayer().isOnline())
				{
					return;
				}

				for (Map.Entry<Skill, Integer> entry : timeSnapshot.entrySet())
				{
					final Skill sk = entry.getKey();
					final int timeLeft = entry.getValue();

					if ((sk == null) || (timeLeft <= 0))
					{
						continue;
					}

					// No restaurar si el personaje ya tiene el buff activo (p.ej. fue rebuffeado).
					if (effected.getEffectList().getBuffInfoBySkillId(sk.getId()) != null)
					{
						continue;
					}

					sk.applyEffects(effected, effected);

					final BuffInfo newInfo = effected.getEffectList().getBuffInfoBySkillId(sk.getId());
					if (newInfo != null)
					{
						newInfo.setAbnormalTime(timeLeft);
					}
				}

			}, CancelReturnConfig.TIME_TO_RETURN);
		}
	}
}
