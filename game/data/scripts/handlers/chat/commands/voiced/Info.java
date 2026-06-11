package handlers.chat.commands.voiced;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;

public class Info implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"info"
	};

	private static final String WELCOME_HTML = "data/scripts/custom/WelcomeScreen/welcome.htm";

	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		final String html = HtmCache.getInstance().getHtm(player, WELCOME_HTML);
		if (html == null)
		{
			player.sendMessage("La informacion del servidor no esta disponible.");
			return false;
		}

		CommunityBoardHandler.separateAndSend(html, player);
		return true;
	}

	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
