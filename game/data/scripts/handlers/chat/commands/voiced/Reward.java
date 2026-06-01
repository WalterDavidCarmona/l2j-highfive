package handlers.chat.commands.voiced;

import custom.DailyReward.DailyReward;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;

public class Reward implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"reward"
	};

	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		DailyReward.sendHtml(player, DailyReward.buildPageHtml(player));
		return true;
	}

	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
