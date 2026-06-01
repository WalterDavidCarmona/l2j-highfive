/*
 * Anti-Feed PvP global: cancela el conteo de PvP Kill cuando killer y killed
 * comparten el mismo HWID (mismo equipo) o la misma IP.
 * Los kills VALIDOS (diferente HWID e IP) cuentan normalmente.
 */
package custom.AntiFeedPvP;

import java.util.logging.Logger;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerPvPKill;
import org.l2jmobius.gameserver.model.events.listeners.FunctionEventListener;
import org.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.model.script.Script;

public class AntiFeedPvP extends Script
{
	private static final Logger LOGGER = Logger.getLogger(AntiFeedPvP.class.getName());

	private AntiFeedPvP()
	{
		// Registrar listener global en todos los Players
		Containers.Players().addListener(new FunctionEventListener(
			Containers.Players(),
			EventType.ON_PLAYER_PVP_KILL,
			(OnPlayerPvPKill event) -> onPvPKill(event),
			this));

		LOGGER.info("AntiFeedPvP: Sistema anti-feed global cargado.");
	}

	private TerminateReturn onPvPKill(OnPlayerPvPKill event)
	{
		final Player killer = event.getPlayer();
		final Player killed = event.getTarget();

		if ((killer == null) || (killed == null))
		{
			return null;
		}

		// Si comparten HWID o IP -> cancelar el PvP kill (feed detectado)
		if (isDualbox(killer, killed))
		{
			killer.sendMessage("[Anti-Feed] PvP kill ignorado: no se permite feed desde el mismo equipo o IP.");
			// TerminateReturn(terminate=true, override=true, abort=true)
			return new TerminateReturn(true, true, true);
		}

		// Kill legitimo -> no interferir, retornar null = dejar pasar normalmente
		return null;
	}

	/**
	 * Retorna true si killer y killed comparten HWID o IP (dualbox / mismo equipo).
	 */
	private static boolean isDualbox(Player killer, Player killed)
	{
		// 1. Comparar HWID (fingerprint de hardware del cliente L2)
		try
		{
			if ((killer.getClient() != null) && (killer.getClient().getHardwareInfo() != null)
				&& (killed.getClient() != null) && (killed.getClient().getHardwareInfo() != null))
			{
				final String hwidKiller = killer.getClient().getHardwareInfo().getMacAddress();
				final String hwidKilled = killed.getClient().getHardwareInfo().getMacAddress();
				if ((hwidKiller != null) && !hwidKiller.isEmpty()
					&& !hwidKiller.equals("Unknown")
					&& hwidKiller.equals(hwidKilled))
				{
					return true;
				}
			}
		}
		catch (Exception ignored) {}

		// 2. Fallback: comparar por IP
		final String ipKiller = killer.getIPAddress();
		final String ipKilled = killed.getIPAddress();
		if ((ipKiller != null) && (ipKilled != null) && ipKiller.equals(ipKilled))
		{
			return true;
		}

		return false;
	}

	public static void main(String[] args)
	{
		new AntiFeedPvP();
	}
}
