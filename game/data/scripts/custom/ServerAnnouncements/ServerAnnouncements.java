/*
 * ServerAnnouncements - Anuncios automaticos del servidor L2 Zona Zero.
 *
 * Feature 1 — Enchant +20 global:
 *   Cuando un jugador equipa un item con enchant >= 20 por primera vez
 *   (desde el ultimo reinicio) se lanza un anuncio global en pantalla
 *   y en el chat del servidor para todos los jugadores online.
 *
 *   Mecanismo: ON_PLAYER_ITEM_EQUIP (no existe evento de enchant directo).
 *   Se usa un Set<Integer> de objectIds ya anunciados para evitar
 *   repetir el anuncio cada vez que se equipa el mismo item.
 *
 * Feature 2 — Muerte PvP (solo victima):
 *   Cuando un jugador es eliminado en PvP se le muestra en pantalla
 *   el nombre del personaje que lo mato. Solo lo ve la victima.
 *
 *   Mecanismo: ON_PLAYER_PVP_KILL.
 *   getPlayer() = asesino | getTarget() = victima
 */
package custom.ServerAnnouncements;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerItemEquip;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerPvPKill;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * Anuncios automaticos: enchant +20 global y muerte PvP personal.
 * @author Custom - Aden Chronicles
 */
public class ServerAnnouncements extends Script
{
	private static final Logger LOGGER = Logger.getLogger(ServerAnnouncements.class.getName());

	/** Nivel de enchant que dispara el anuncio global. */
	private static final int ENCHANT_ANNOUNCE_LEVEL = 20;

	/** ObjectIds de items ya anunciados (evita repetir en cada equipamiento). */
	private static final Set<Integer> ANNOUNCED_ITEMS = Collections.newSetFromMap(new ConcurrentHashMap<>());

	/** Duracion del mensaje en pantalla para el anuncio global (ms). */
	private static final int GLOBAL_MSG_DURATION = 8000;

	/** Duracion del mensaje en pantalla para la victima PvP (ms). */
	private static final int PVP_MSG_DURATION = 6000;

	private ServerAnnouncements()
	{
		LOGGER.info("ServerAnnouncements: Cargado. Enchant>=" + ENCHANT_ANNOUNCE_LEVEL + " anunciado globalmente.");
	}

	// -------------------------------------------------------------------------
	// Feature 1: Anuncio global enchant +20
	// -------------------------------------------------------------------------

	@RegisterEvent(EventType.ON_PLAYER_ITEM_EQUIP)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onItemEquip(OnPlayerItemEquip event)
	{
		final Item item = event.getItem();
		final Player player = event.getPlayer();

		if ((item == null) || (player == null))
		{
			return;
		}

		final int enchant = item.getEnchantLevel();
		if (enchant < ENCHANT_ANNOUNCE_LEVEL)
		{
			return;
		}

		// Solo anunciar la primera vez que se equipa este item a +20 o mas
		if (!ANNOUNCED_ITEMS.add(item.getObjectId()))
		{
			return;
		}

		final String itemName = item.getTemplate().getName();
		final String playerName = player.getName();

		// Mensaje en pantalla para TODOS (centro de pantalla)
		final String screenMsg = "» " + playerName + " ha encantado " + itemName + " a +" + enchant + "! «";
		Broadcast.toAllOnlinePlayersOnScreen(screenMsg);

		// Anuncio en chat del servidor
		Broadcast.toAllOnlinePlayers("[Servidor] " + playerName + " logro encatar su " + itemName + " a +" + enchant + "! Felicitaciones!");

		LOGGER.info("ServerAnnouncements: Enchant+" + enchant + " anunciado. Player=" + playerName + " Item=" + itemName + " ObjId=" + item.getObjectId());
	}

	// -------------------------------------------------------------------------
	// Feature 2: Mensaje personal a la victima al morir en PvP
	// -------------------------------------------------------------------------

	@RegisterEvent(EventType.ON_PLAYER_PVP_KILL)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPvpKill(OnPlayerPvPKill event)
	{
		final Player killer = event.getPlayer();
		final Player victim = event.getTarget();

		if ((killer == null) || (victim == null))
		{
			return;
		}

		// Mensaje solo para la victima, centro de pantalla
		victim.sendPacket(new ExShowScreenMessage(
			"Fuiste eliminado por " + killer.getName() + "!",
			PVP_MSG_DURATION,
			ExShowScreenMessage.MIDDLE_CENTER));
	}

	// -------------------------------------------------------------------------
	// Entry point
	// -------------------------------------------------------------------------

	public static void main(String[] args)
	{
		new ServerAnnouncements();
	}
}
