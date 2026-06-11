/*
 * Item handler para la Huge Fortuna Box (id 20515) del evento Battle Royale.
 * Recompensas configuradas desde BattleRoyale.ini (BattleRoyaleBoxRewards).
 * Solo jugadores activos dentro del evento pueden abrir la caja.
 */
package handlers.items;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import custom.events.BattleRoyale.BattleRoyale;

/**
 * Handler para la caja Battle Royale (item 20515 — Huge Fortuna Box).
 * Las recompensas se leen de config/Custom/BattleRoyale.ini.
 */
public class BattleRoyaleBox implements IItemHandler
{
	private static final String CONFIG = "config/Custom/BattleRoyale.ini";
	private final List<ItemHolder> _boxRewards = new ArrayList<>();

	public BattleRoyaleBox()
	{
		loadRewards();
	}

	private void loadRewards()
	{
		try (FileInputStream fis = new FileInputStream(CONFIG))
		{
			final Properties p = new Properties();
			p.load(fis);

			_boxRewards.clear();
			final String raw = p.getProperty("BattleRoyaleBoxRewards", "57,50000").trim();
			for (String entry : raw.split(";"))
			{
				final String[] parts = entry.trim().split(",");
				if (parts.length == 2)
				{
					final int itemId = Integer.parseInt(parts[0].trim());
					final long count = Long.parseLong(parts[1].trim());
					_boxRewards.add(new ItemHolder(itemId, count));
				}
			}
			LOGGER.info("BattleRoyaleBox: " + _boxRewards.size() + " recompensas cargadas.");
		}
		catch (Exception e)
		{
			LOGGER.warning("BattleRoyaleBox: error cargando recompensas: " + e.getMessage());
		}
	}

	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			return false;
		}

		final Player player = playable.asPlayer();

		// Solo participantes activos del evento
		if (!BattleRoyale.isActiveParticipant(player))
		{
			player.sendMessage("[Battle Royale] Solo los participantes activos pueden abrir esta caja.");
			return false;
		}

		// Destruir la caja del inventario
		if (!player.destroyItem(ItemProcessType.DESTROY, item, 1, player, true))
		{
			return false;
		}

		// Dar premios
		for (ItemHolder reward : _boxRewards)
		{
			player.addItem(ItemProcessType.REWARD, (int) reward.getId(), reward.getCount(), player, true);
		}

		player.sendPacket(new ExShowScreenMessage("Caja abierta!", ExShowScreenMessage.TOP_CENTER, 3000, 0, true, false));
		return true;
	}
}
