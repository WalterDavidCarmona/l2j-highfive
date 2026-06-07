package custom.SetEnchantEffect;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.holders.ArmorSet;
import org.l2jmobius.gameserver.data.xml.ArmorSetData;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerItemEquip;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerItemUnequip;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;

/**
 * SetEnchantEffect - Aplica un Abnormal Visual Effect cuando el jugador
 * viste un set completo de armadura con todas las piezas al nivel de encantamiento requerido.
 *
 * Config: game/config/Custom/SetEnchantEffect.ini
 */
public class SetEnchantEffect extends Script
{
	private static final Logger LOGGER = Logger.getLogger(SetEnchantEffect.class.getName());
	private static final String CONFIG_PATH = "./config/Custom/SetEnchantEffect.ini";

	// Paperdoll slots de armadura usando BodyPart para evitar hardcodear indices
	private static final int SLOT_HEAD   = BodyPart.HEAD.getPaperdollSlot();
	private static final int SLOT_CHEST  = BodyPart.CHEST.getPaperdollSlot();
	private static final int SLOT_LEGS   = BodyPart.LEGS.getPaperdollSlot();
	private static final int SLOT_GLOVES = BodyPart.GLOVES.getPaperdollSlot();
	private static final int SLOT_FEET   = BodyPart.FEET.getPaperdollSlot();
	private static final int[] ARMOR_SLOTS = { SLOT_HEAD, SLOT_CHEST, SLOT_LEGS, SLOT_GLOVES, SLOT_FEET };

	// Configuracion cargada desde .ini
	private static boolean ENABLED;
	private static int ENCHANT_LEVEL;
	private static AbnormalVisualEffect EFFECT;

	private SetEnchantEffect()
	{
		loadConfig();
		LOGGER.info("SetEnchantEffect: Loaded. Enabled=" + ENABLED + " MinEnchant=+" + ENCHANT_LEVEL + " Effect=" + EFFECT);

		// Tarea periodica: verifica todos los jugadores online cada 3 segundos.
		// Cubre: unequip, enchant cambiado, login tardio, cualquier cambio de estado.
		ThreadPool.scheduleAtFixedRate(() ->
		{
			if (!ENABLED) return;
			for (Player p : World.getInstance().getPlayers())
			{
				if (p != null && p.isOnline())
				{
					checkAndUpdate(p);
				}
			}
		}, 3000, 3000);
	}

	// ─────────────────────────────────────────────────────────────
	// Event handlers
	// ─────────────────────────────────────────────────────────────

	@RegisterEvent(EventType.ON_PLAYER_ITEM_EQUIP)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onItemEquip(OnPlayerItemEquip event)
	{
		checkAndUpdate(event.getPlayer());
	}

	@RegisterEvent(EventType.ON_PLAYER_ITEM_UNEQUIP)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onItemUnequip(OnPlayerItemUnequip event)
	{
		// Delay: el evento se dispara ANTES de que el item salga del paperdoll
		final Player player = event.getPlayer();
		ThreadPool.schedule(() -> checkAndUpdate(player), 200);
	}

	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onLogin(OnPlayerLogin event)
	{
		checkAndUpdate(event.getPlayer());
	}

	// ─────────────────────────────────────────────────────────────
	// Core logic
	// ─────────────────────────────────────────────────────────────

	private void checkAndUpdate(Player player)
	{
		if (player == null) return;

		if (ENABLED && hasEnchantedFullSet(player))
		{
			if (!player.hasAbnormalVisualEffect(EFFECT))
			{
				player.startAbnormalVisualEffect(true, EFFECT);
				LOGGER.info("SetEnchantEffect: APPLIED " + EFFECT + " to " + player.getName());
			}
		}
		else
		{
			if (player.hasAbnormalVisualEffect(EFFECT))
			{
				player.stopAbnormalVisualEffect(true, EFFECT);
				LOGGER.info("SetEnchantEffect: REMOVED " + EFFECT + " from " + player.getName());
			}
		}
	}

	/**
	 * Retorna true si el jugador tiene un set completo equipado
	 * y TODAS las piezas de armadura tienen enchant >= ENCHANT_LEVEL.
	 */
	private boolean hasEnchantedFullSet(Player player)
	{
		// Verificar que hay un chest equipado como punto de entrada al set
		final Item chestItem = player.getInventory().getPaperdollItem(SLOT_CHEST);
		if (chestItem == null) return false;

		// Buscar el set de armadura correspondiente al chest
		final ArmorSet armorSet = ArmorSetData.getInstance().getSet(chestItem.getId());
		if (armorSet == null) return false;

		// Verificar que todas las piezas del set estan equipadas
		if (!armorSet.containAll(player)) return false;

		// Verificar nivel de encantamiento en cada slot de armadura
		for (int slot : ARMOR_SLOTS)
		{
			final Item item = player.getInventory().getPaperdollItem(slot);
			if (item == null) continue; // pieza no requerida por este set

			if (item.getEnchantLevel() < ENCHANT_LEVEL) return false;
		}

		return true;
	}

	// ─────────────────────────────────────────────────────────────
	// Config
	// ─────────────────────────────────────────────────────────────

	private static void loadConfig()
	{
		final Properties props = new Properties();
		try (InputStream is = new FileInputStream(CONFIG_PATH))
		{
			props.load(is);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "SetEnchantEffect: Could not load config from " + CONFIG_PATH + ", using defaults.", e);
		}

		ENABLED       = Boolean.parseBoolean(props.getProperty("SetEnchantEffectEnabled", "True"));
		ENCHANT_LEVEL = Integer.parseInt(props.getProperty("SetEnchantMinLevel", "20"));

		final String effectName = props.getProperty("SetAbnormalVisualEffect", "VP_UP").trim();
		AbnormalVisualEffect parsed = AbnormalVisualEffect.NONE;
		try
		{
			parsed = AbnormalVisualEffect.valueOf(effectName);
		}
		catch (IllegalArgumentException e)
		{
			LOGGER.warning("SetEnchantEffect: Unknown AbnormalVisualEffect '" + effectName + "', falling back to VP_UP.");
			parsed = AbnormalVisualEffect.VP_UP;
		}
		EFFECT = parsed;
	}

	// ─────────────────────────────────────────────────────────────
	// Entry point (Script Engine auto-discovery)
	// ─────────────────────────────────────────────────────────────

	public static void main(String[] args)
	{
		new SetEnchantEffect();
	}
}
