package handlers.chat.commands.voiced;

import custom.DailyReward.DailyReward;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Voiced command .reward — abre el Community Board mostrando la recompensa
 * diaria (Daily Reward) para reclamar el premio del dia.
 */
public class Reward implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"reward"
	};

	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		// Mostrar la pagina de Daily Reward dentro del panel del Community Board.
		CommunityBoardHandler.separateAndSend(DailyReward.buildPageHtml(player), player);
		return true;
	}

	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
