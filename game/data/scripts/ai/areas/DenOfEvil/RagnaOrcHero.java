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
package ai.areas.DenOfEvil;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.holders.npc.MinionHolder;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Ragna Orc Hero AI.
 * @author Zealar, Mobius
 */
public class RagnaOrcHero extends Script
{
	private static final int RAGNA_ORC_HERO = 22693;
	
	private RagnaOrcHero()
	{
		addSpawnId(RAGNA_ORC_HERO);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		for (MinionHolder minionHolder : npc.getTemplate().getParameters().getMinionList(getRandom(100) < 70 ? "Privates1" : "Privates2"))
		{
			addMinion(npc.asMonster(), minionHolder.getId());
		}
	}
	
	public static void main(String[] args)
	{
		new RagnaOrcHero();
	}
}
