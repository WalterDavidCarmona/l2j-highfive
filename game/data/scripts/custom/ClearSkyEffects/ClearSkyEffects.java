/*
 * ClearSkyEffects - Elimina la niebla de Seven Signs (SSQ Dusk) para todos los jugadores.
 *
 * Mecanismo:
 *   - OnPlayerLogin : envia cielo limpio con retardo de 2s (despues del paquete del core)
 *   - Tarea periodica : cada 8 segundos neutraliza la niebla activa para todos
 *
 * Nota sobre ExRedSky (lluvia roja de asedios):
 *   ExRedSky solo acepta una duracion; no existe un paquete de cancelacion.
 *   Enviar ExRedSky(0) dispara la animacion de inicio aunque sea por 0 segundos,
 *   lo que provoca un parpadeo rojo visible cada vez que se envia. Por eso NO
 *   se contrarresta aqui — la lluvia roja expira sola al terminar el asedio.
 */
package custom.ClearSkyEffects;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.SSQInfo;

/**
 * Neutraliza el efecto visual de niebla de Seven Signs (estado Dusk).
 * @author Custom - Aden Chronicles
 */
public class ClearSkyEffects extends Script
{
	/** SSQInfo(2) = estado Dawn = cielo limpio, sin niebla. */
	private static final SSQInfo CLEAR_FOG = new SSQInfo(2);

	private ClearSkyEffects()
	{
		// Tarea periodica: neutraliza la niebla para todos los jugadores online.
		// Se ejecuta cada 8 segundos para contrarrestar el reenvio del core durante SSQ Dusk.
		ThreadPool.scheduleAtFixedRate(() ->
		{
			for (Player player : World.getInstance().getPlayers())
			{
				if ((player != null) && player.isOnline())
				{
					player.sendPacket(CLEAR_FOG);
				}
			}
		}, 5000, 8000);
	}

	/**
	 * Al iniciar sesion, envia cielo limpio con retardo de 2 segundos para que
	 * llegue DESPUES de los paquetes de login del core (que incluyen el SSQInfo del estado actual).
	 */
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		ThreadPool.schedule(() ->
		{
			if ((player != null) && player.isOnline())
			{
				player.sendPacket(CLEAR_FOG);
			}
		}, 2000);
	}

	public static void main(String[] args)
	{
		new ClearSkyEffects();
	}
}
