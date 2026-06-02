/*
 * Anti-Feed PvP global: revierte el conteo de PvP Kill cuando killer y killed
 * comparten el mismo HWID (mismo equipo) o la misma IP.
 * Los kills VALIDOS (diferente HWID e IP) cuentan normalmente.
 *
 * NOTA: OnPlayerPvPKill en esta version de L2JMobius no soporta TerminateReturn.
 * Se usa ConsumerEventListener (sin retorno) y se revierte el contador manualmente.
 */
package custom.AntiFeedPvP;

import java.util.logging.Logger;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerPvPKill;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.script.Script;

public class AntiFeedPvP extends Script
{
	private static final Logger LOGGER = Logger.getLogger(AntiFeedPvP.class.getName());

	private AntiFeedPvP()
	{
		// ConsumerEventListener no retorna valor, evita el NullPointerException
		// en FunctionEventListener.executeEvent() causado por returnBackClass=null
		Containers.Players().addListener(new ConsumerEventListener(
			Containers.Players(),
			EventType.ON_PLAYER_PVP_KILL,
			(OnPlayerPvPKill event) -> onPvPKill(event),
			this));

		LOGGER.info("AntiFeedPvP: Sistema anti-feed global cargado.");
	}

	private static void onPvPKill(OnPlayerPvPKill event)
	{
		final Player killer = event.getPlayer();
		final Player killed = event.getTarget();

		if ((killer == null) || (killed == null))
		{
			return;
		}

		// Si comparten HWID o IP -> revertir el PvP kill ya contabilizado
		if (isDualbox(killer, killed))
		{
			// El kill ya fue contado por el engine; lo deshacemos manualmente
			if (killer.getPvpKills() > 0)
			{
				killer.setPvpKills(killer.getPvpKills() - 1);
			}
			killer.broadcastUserInfo();
			killer.sendMessage("[Anti-Feed] PvP kill ignorado: no se permite feed desde el mismo equipo o IP.");
			LOGGER.info("AntiFeedPvP: Feed detectado - " + killer.getName() + " vs " + killed.getName());
		}
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
