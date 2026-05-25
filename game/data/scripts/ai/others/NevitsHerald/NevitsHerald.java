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
package ai.others.NevitsHerald;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * Nevit's Herald AI.
 * @author Sacrifice, Mobius
 */
public class NevitsHerald extends Script
{
	private static final int NEVITS_HERALD = 4326;
	private static final List<Npc> SPAWNS = new ArrayList<>();
	private static final Location[] NEVITS_HERALD_LOC =
	{
		new Location(86971, -142772, -1336, 20480), // Town of Schuttgart
		new Location(44165, -48494, -792, 32768), // Rune Township
		new Location(148017, -55264, -2728, 49152), // Town of Goddard
		new Location(147919, 26631, -2200, 16384), // Town of Aden
		new Location(82325, 53278, -1488, 16384), // Town of Oren
		new Location(81925, 148302, -3464, 49152), // Town of Giran
		new Location(111678, 219197, -3536, 49152), // Heine
		new Location(16254, 142808, -2696, 16384), // Town of Dion
		new Location(-13865, 122081, -2984, 32768), // Town of Gludio
		new Location(-83248, 150832, -3136, 32768), // Gludin Village
		new Location(116899, 77256, -2688, 49152) // Hunters Village
	};
	private static final int ANTHARAS = 29068; // Antharas Strong (85)
	private static final int VALAKAS = 29028; // Valakas (85)
	private static final NpcStringId[] SPAM =
	{
		NpcStringId.SHOW_RESPECT_TO_THE_HEROES_WHO_DEFEATED_THE_EVIL_DRAGON_AND_PROTECTED_THIS_ADEN_WORLD,
		NpcStringId.SHOUT_TO_CELEBRATE_THE_VICTORY_OF_THE_HEROES,
		NpcStringId.PRAISE_THE_ACHIEVEMENT_OF_THE_HEROES_AND_RECEIVE_NEVIT_S_BLESSING
	};
	
	// Skill
	private static final SkillHolder FALL_OF_THE_DRAGON = new SkillHolder(23312, 1);
	
	private NevitsHerald()
	{
		addFirstTalkId(NEVITS_HERALD);
		addStartNpc(NEVITS_HERALD);
		addTalkId(NEVITS_HERALD);
		addKillId(ANTHARAS, VALAKAS);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "4326.htm";
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (npc.getId() == NEVITS_HERALD)
		{
			if (event.equalsIgnoreCase("buff"))
			{
				if (player.getEffectList().getFirstEffect(EffectType.NEVITS_HOURGLASS) != null)
				{
					return "4326-1.htm";
				}
				
				npc.setTarget(player);
				npc.doCast(FALL_OF_THE_DRAGON.getSkill());
			}
		}
		else if (event.equalsIgnoreCase("text_spam"))
		{
			npc.broadcastSay(ChatType.SHOUT, SPAM[getRandom(0, SPAM.length - 1)]);
			startQuestTimer("text_spam", 60000, npc, null);
		}
		else if (event.equalsIgnoreCase("despawn"))
		{
			for (Npc spawn : SPAWNS)
			{
				cancelQuestTimer("text_spam", spawn, null);
				spawn.deleteMe();
			}
			
			SPAWNS.clear();
		}
		
		return null;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		ExShowScreenMessage message = null;
		if (npc.getId() == VALAKAS)
		{
			message = new ExShowScreenMessage(NpcStringId.THE_EVIL_FIRE_DRAGON_VALAKAS_HAS_BEEN_DEFEATED, 2, 10000);
		}
		else
		{
			message = new ExShowScreenMessage(NpcStringId.THE_EVIL_LAND_DRAGON_ANTHARAS_HAS_BEEN_DEFEATED, 2, 10000);
		}
		
		for (Player onlinePlayer : World.getInstance().getPlayers())
		{
			if (onlinePlayer == null)
			{
				continue;
			}
			
			onlinePlayer.sendPacket(message);
		}
		
		if (SPAWNS.isEmpty())
		{
			for (Location loc : NEVITS_HERALD_LOC)
			{
				final Npc herald = addSpawn(NEVITS_HERALD, loc, false, 0);
				startQuestTimer("text_spam", 3000, herald, null);
				SPAWNS.add(herald);
			}
			
			startQuestTimer("despawn", 14400000, null, null);
		}
	}
	
	public static void main(String[] args)
	{
		new NevitsHerald();
	}
}
