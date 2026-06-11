/*
 * WelcomeScreen - Envia pantalla de bienvenida a cada jugador al iniciar sesion.
 *
 * Muestra una ventana HTML con informacion del servidor L2 Zona Zero y la lista
 * completa de comandos de voz disponibles para jugadores no-GM.
 *
 * El HTML se envia con un retardo de 3 segundos para que llegue despues de que
 * el cliente haya cargado el personaje completamente (evita ventana en negro).
 */
package custom.WelcomeScreen;

import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Pantalla de bienvenida de L2 Zona Zero.
 * Se muestra una vez al iniciar sesion con un retardo de 3s.
 * @author Custom - Aden Chronicles
 */
public class WelcomeScreen extends Script
{
	private static final Logger LOGGER = Logger.getLogger(WelcomeScreen.class.getName());

	/** Ruta al HTML relativa a la raiz del datapack (data/...). */
	private static final String WELCOME_HTML = "data/scripts/custom/WelcomeScreen/welcome.htm";

	/** Retardo en ms antes de enviar el HTML (espera que el cliente termine de cargar). */
	private static final int LOGIN_DELAY_MS = 3000;

	private WelcomeScreen()
	{
		LOGGER.info("WelcomeScreen: Cargado. HTML=" + WELCOME_HTML);
	}

	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();

		ThreadPool.schedule(() ->
		{
			if ((player == null) || !player.isOnline())
			{
				return;
			}

			final String html = org.l2jmobius.gameserver.cache.HtmCache.getInstance().getHtm(player, WELCOME_HTML);
			CommunityBoardHandler.separateAndSend(html, player);
		}, LOGIN_DELAY_MS);
	}

	public static void main(String[] args)
	{
		new WelcomeScreen();
	}
}
