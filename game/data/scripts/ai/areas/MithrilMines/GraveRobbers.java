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
package ai.areas.MithrilMines;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.holders.npc.MinionHolder;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Grove Robber's AI.<br>
 * <ul>
 * <li>Grove Robber Summoner</li>
 * <li>Grove Robber Megician</li>
 * </ul>
 * @author Zealar, Mobius
 */
public class GraveRobbers extends Script
{
	private static final int GRAVE_ROBBER_SUMMONER = 22678;
	private static final int GRAVE_ROBBER_MEGICIAN = 22679;
	
	private GraveRobbers()
	{
		addSpawnId(GRAVE_ROBBER_SUMMONER, GRAVE_ROBBER_MEGICIAN);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		for (MinionHolder minionHolder : npc.getTemplate().getParameters().getMinionList("Privates" + getRandom(1, 2)))
		{
			addMinion(npc.asMonster(), minionHolder.getId());
		}
	}
	
	public static void main(String[] args)
	{
		new GraveRobbers();
	}
}
