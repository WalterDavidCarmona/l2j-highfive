/*
 * PvpDeathAnnounce - Muestra un mensaje en pantalla al jugador que muere en PvP
 * indicando quien lo elimino.
 *
 * Condiciones para activarse:
 *   - Tanto el muerto como el asesino deben ser jugadores (Player).
 *   - El asesino debe tener flag PvP activo en el momento de la muerte,
 *     o bien el muerto debe tenerlo (combate PvP real, no PK contra neutro).
 *
 * Mensaje mostrado SOLO al jugador muerto, centrado en pantalla, durante 5 segundos.
 */
package custom.ServerAnnouncements;

import java.util.logging.Logger;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * Notifica al jugador muerto en PvP quien lo elimino mediante un mensaje en pantalla.
 * @author Custom - Aden Chronicles
 */
public class PvpDeathAnnounce extends Script
{
	private static final Logger LOGGER = Logger.getLogger(PvpDeathAnnounce.class.getName());

	/** Duracion del mensaje en pantalla en milisegundos. */
	private static final int MSG_DURATION_MS = 5000;

	private PvpDeathAnnounce()
	{
		LOGGER.info("PvpDeathAnnounce: Cargado.");
	}

	@RegisterEvent(EventType.ON_CREATURE_DEATH)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onCreatureDeath(OnCreatureDeath event)
	{
		final Creature target = event.getTarget();
		final Creature attacker = event.getAttacker();

		// Solo muertes entre jugadores
		if (!target.isPlayer() || !attacker.isPlayer())
		{
			return;
		}

		final Player killed = target.asPlayer();
		final Player killer = attacker.asPlayer();

		// Solo PvP: al menos uno de los dos tenia flag PvP activo
		// (excluye PK contra alguien sin bandera ni karma previo)
		if ((killer.getPvpFlag() == 0) && (killed.getPvpFlag() == 0))
		{
			return;
		}

		// No notificar en duelos (ya tienen su propia pantalla)
		if (killed.isInDuel())
		{
			return;
		}

		killed.sendPacket(new ExShowScreenMessage(
			"Fuiste eliminado por " + killer.getName() + "!",
			MSG_DURATION_MS,
			2 // posicion central
		));
	}

	public static void main(String[] args)
	{
		new PvpDeathAnnounce();
	}
}
