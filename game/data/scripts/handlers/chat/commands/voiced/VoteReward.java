package handlers.chat.commands.voiced;

import custom.VoteSystem.VoteSystem;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Voiced command .getreward — abre la pagina de votos (info Hopzone + boton de
 * reclamar) sin necesidad de visitar el NPC ni abrir el Community Board.
 */
public class VoteReward implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"getreward"
	};

	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		if ((VoteSystem.getInstance() == null) || !VoteSystem.isVoiceCommandEnabled())
		{
			player.sendMessage("[Votos] El sistema de votos no esta disponible.");
			return false;
		}

		VoteSystem.sendVotePage(player);
		return true;
	}

	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
