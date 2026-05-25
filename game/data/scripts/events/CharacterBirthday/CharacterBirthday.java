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
package events.CharacterBirthday;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.OnDailyReset;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.MessageSenderType;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * @author Nyaran, Mobius
 */
public class CharacterBirthday extends Script
{
	// NPCs
	private static final int ALEGRIA = 32600;
	private static final int[] GATEKEEPERS =
	{
		30006,
		30059,
		30080,
		30134,
		30146,
		30177,
		30233,
		30256,
		30320,
		30540,
		30576,
		30836,
		30848,
		30878,
		30899,
		31275,
		31320,
		31964,
		32163
	};
	
	// Query: Get all players that have had a birthday the last 24 hours.
	private static final String SELECT_PENDING_BIRTHDAY_GIFTS = "SELECT charId, char_name, createDate, (YEAR(NOW()) - YEAR(createDate)) AS age FROM characters WHERE (YEAR(NOW()) - YEAR(createDate) > 0) AND ((DATE_ADD(createDate, INTERVAL (YEAR(NOW()) - YEAR(createDate)) YEAR)) BETWEEN FROM_UNIXTIME(?) AND NOW())";
	
	// Misc
	private static int SPAWNS = 0;
	
	private CharacterBirthday()
	{
		addStartNpc(ALEGRIA);
		addStartNpc(GATEKEEPERS);
		addTalkId(ALEGRIA);
		addTalkId(GATEKEEPERS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		if (event.equalsIgnoreCase("despawn_npc"))
		{
			npc.doDie(player);
			SPAWNS--;
			
			htmltext = null;
		}
		else if (event.equalsIgnoreCase("change"))
		{
			// Change Hat
			if (hasQuestItems(player, 10250))
			{
				takeItems(player, 10250, 1); // Adventurer Hat (Event)
				giveItems(player, 21594, 1); // Birthday Hat
				htmltext = null; // FIXME: Probably has html
				
				// Despawn npc
				npc.doDie(player);
				SPAWNS--;
			}
			else
			{
				htmltext = "32600-nohat.htm";
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (SPAWNS >= 3)
		{
			return "busy.htm";
		}
		
		if (!LocationUtil.checkIfInRange(10, npc, player, true))
		{
			final Npc spawned = addSpawn(32600, player.getX() + 10, player.getY() + 10, player.getZ() + 20, 0, false, 0, true);
			startQuestTimer("despawn_npc", 180000, spawned, player);
			SPAWNS++;
		}
		else
		{
			return "tooclose.htm";
		}
		
		return null;
	}
	
	@RegisterEvent(EventType.ON_DAILY_RESET)
	@RegisterType(ListenerRegisterType.GLOBAL)
	public void onDailyReset(OnDailyReset event)
	{
		int birthdayGiftCount = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_PENDING_BIRTHDAY_GIFTS))
		{
			statement.setLong(1, System.currentTimeMillis() - (24 * 60 * 60 * 1000)); // Last 24 hours.
			try (ResultSet rs = statement.executeQuery())
			{
				while (rs.next())
				{
					final String text = GeneralConfig.ALT_BIRTHDAY_MAIL_TEXT.replaceAll("$c1", rs.getString("char_name")).replaceAll("$s1", Integer.toString(rs.getInt("age")));
					final MailMessage message = new MailMessage(rs.getInt("charId"), GeneralConfig.ALT_BIRTHDAY_MAIL_SUBJECT, text, MessageSenderType.ALEGRIA);
					message.createAttachments().addItem(ItemProcessType.REWARD, GeneralConfig.ALT_BIRTHDAY_GIFT, 1, null, null);
					MailManager.getInstance().sendMessage(message);
					birthdayGiftCount++;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Error checking birthdays. " + e.getMessage());
		}
		
		LOGGER.info(getClass().getSimpleName() + " " + birthdayGiftCount + " gifts sent.");
	}
	
	public static void main(String[] args)
	{
		new CharacterBirthday();
	}
}
