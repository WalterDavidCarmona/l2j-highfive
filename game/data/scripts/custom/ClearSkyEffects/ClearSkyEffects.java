/*
 * ClearSkyEffects - Elimina la niebla de Seven Signs (SSQ Dusk) y fuerza
 *                   cielo de dia permanente para todos los jugadores.
 *
 * Mecanismo:
 *   - OnPlayerLogin : envia cielo limpio + SunRise con retardo de 2s
 *   - Tarea periodica : cada 8 segundos neutraliza niebla y fuerza dia
 *   - OnDayNightChange : al transicionar a noche, envia SunRise inmediato
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
import org.l2jmobius.gameserver.model.events.holders.OnDayNightChange;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.SSQInfo;
import org.l2jmobius.gameserver.network.serverpackets.SunRise;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Neutraliza el efecto visual de niebla de Seven Signs (estado Dusk)
 * y fuerza cielo de dia permanente (sin ciclo dia/noche).
 * @author Custom - Aden Chronicles
 */
public class ClearSkyEffects extends Script
{
	private static final SSQInfo CLEAR_FOG = new SSQInfo(0);

	private ClearSkyEffects()
	{
		ThreadPool.scheduleAtFixedRate(() ->
		{
			final boolean isNight = GameTimeTaskManager.getInstance().isNight();
			for (Player player : World.getInstance().getPlayers())
			{
				if ((player != null) && player.isOnline())
				{
					player.sendPacket(CLEAR_FOG);
					if (isNight)
					{
						player.sendPacket(SunRise.STATIC_PACKET);
					}
				}
			}
		}, 5000, 8000);
	}

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
				if (GameTimeTaskManager.getInstance().isNight())
				{
					player.sendPacket(SunRise.STATIC_PACKET);
				}
			}
		}, 2000);
	}

	@RegisterEvent(EventType.ON_DAY_NIGHT_CHANGE)
	@RegisterType(ListenerRegisterType.GLOBAL)
	public void onDayNightChange(OnDayNightChange event)
	{
		if (event.isNight())
		{
			for (Player player : World.getInstance().getPlayers())
			{
				if ((player != null) && player.isOnline())
				{
					player.sendPacket(SunRise.STATIC_PACKET);
				}
			}
		}
	}

	public static void main(String[] args)
	{
		new ClearSkyEffects();
	}
}
