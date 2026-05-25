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
package handlers.chat.commands.admin;

import java.util.Calendar;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.SeedOfDestructionManager;
import org.l2jmobius.gameserver.managers.SeedOfInfinityManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminGraciaSeeds implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_gracia_seeds",
		"admin_kill_tiat",
		"admin_set_sodstate",
		"admin_set_soistage"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken(); // Get actual command
		String val = "";
		if (st.countTokens() >= 1)
		{
			val = st.nextToken();
		}
		
		if (actualCommand.equalsIgnoreCase("admin_kill_tiat"))
		{
			SeedOfDestructionManager.getInstance().increaseSoDTiatKilled();
		}
		else if (actualCommand.equalsIgnoreCase("admin_set_sodstate"))
		{
			SeedOfDestructionManager.getInstance().setSoDState(Integer.parseInt(val), true);
		}
		else if (actualCommand.equalsIgnoreCase("admin_set_soistage"))
		{
			SeedOfInfinityManager.setCurrentStage(Integer.parseInt(val));
		}
		
		showMenu(activeChar);
		return true;
	}
	
	private void showMenu(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(activeChar, "data/html/admin/graciaseeds.htm");
		
		// Seed of destruction
		html.replace("%sodstage%", String.valueOf(SeedOfDestructionManager.getInstance().getSoDState()));
		html.replace("%sodtiatkill%", String.valueOf(SeedOfDestructionManager.getInstance().getSoDTiatKilled()));
		if (SeedOfDestructionManager.getInstance().getSoDTimeForNextStateChange() > 0)
		{
			final Calendar nextChangeDate = Calendar.getInstance();
			nextChangeDate.setTimeInMillis(System.currentTimeMillis() + SeedOfDestructionManager.getInstance().getSoDTimeForNextStateChange());
			html.replace("%sodtime%", nextChangeDate.getTime().toString());
		}
		else
		{
			html.replace("%sodtime%", "-1");
		}
		
		// Seed of infinity
		html.replace("%soistage%", SeedOfInfinityManager.getCurrentStage());
		html.replace("%twinkills%", "N/A");
		html.replace("%cohemeneskills%", SeedOfInfinityManager.getCohemenesCount());
		html.replace("%ekimuskills%", SeedOfInfinityManager.getEkimusCount());
		html.replace("%soitime%", "N/A");
		activeChar.sendPacket(html);
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
