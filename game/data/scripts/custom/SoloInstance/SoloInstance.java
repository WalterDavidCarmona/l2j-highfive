/*
 * SoloInstance - Instancia individual para jugadores.
 *
 * Flujo:
 *   Jugador habla con NPC -> confirmacion de entrada -> se crea instancia personal
 *   -> 5 monstruos en cadena (matar uno spawnea el siguiente) -> recompensa al final
 *   -> instancia destruida
 *
 * Config: game/config/Custom/SoloInstance.ini
 * Instancia base: game/data/instances/custom/SoloInstance.xml (id=9001)
 */
package custom.SoloInstance;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.PremiumManager;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.model.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Solo Instance — Instancia individual de Aden Chronicles.
 * @author Custom - Aden Chronicles
 */
public class SoloInstance extends Script
{
	private static final Logger LOGGER = Logger.getLogger(SoloInstance.class.getName());

	// ID del template de instancia (game/data/instances/custom/SoloInstance.xml)
	private static final int INSTANCE_TEMPLATE_ID = 9001;

	// Coordenadas de salida (Giran)
	private static final Location EXIT_LOC = new Location(82698, 148638, -3473);

	// ---------------------------------------------------------------------------
	// Config (cargada desde .ini)
	// ---------------------------------------------------------------------------
	private static boolean ENABLED             = true;
	private static int     NPC_ID              = 50027;
	private static int     TIME_LIMIT_MINUTES  = 30;
	private static long    COOLDOWN_HOURS      = 24;
	private static int     REWARD_ITEM_ID      = 10639;
	private static int     REWARD_COUNT        = 500;
	private static int     SPAWN_X             = 42660;
	private static int     SPAWN_Y             = -47996;
	private static int     SPAWN_Z             = -727;

	// Lista de monstruos en cadena: [npcId, x, y, z]
	private static final List<int[]> MONSTER_CHAIN = new ArrayList<>();

	// ---------------------------------------------------------------------------
	// Estado en tiempo real
	// ---------------------------------------------------------------------------

	// Archivo de persistencia de cooldowns (sobrevive reinicios)
	private static final String COOLDOWN_FILE = "./config/Custom/SoloInstanceCooldowns.properties";

	/** playerObjectId -> timestamp (ms) de cuando inició la instancia */
	private static final Map<Integer, Long> COOLDOWN_MAP = new ConcurrentHashMap<>();

	/** playerObjectId -> instancia activa */
	private static final Map<Integer, ActiveInstance> ACTIVE_INSTANCES = new ConcurrentHashMap<>();

	// ---------------------------------------------------------------------------
	// Clase interna: datos de una instancia activa
	// ---------------------------------------------------------------------------
	private static class ActiveInstance
	{
		final InstanceWorld world;
		final int           playerObjectId;
		int                 currentMonsterIndex = 0;
		Npc                 currentMonster      = null;
		ScheduledFuture<?>  timeoutTask         = null;
		AbstractEventListener deathListener     = null;

		ActiveInstance(InstanceWorld world, int playerObjectId)
		{
			this.world         = world;
			this.playerObjectId = playerObjectId;
		}
	}

	// ---------------------------------------------------------------------------
	// Constructor
	// ---------------------------------------------------------------------------
	private SoloInstance()
	{
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
		loadConfig();
		loadCooldowns();
		LOGGER.info("SoloInstance: Sistema iniciado. NPC=" + NPC_ID
			+ " | Monstruos=" + MONSTER_CHAIN.size()
			+ " | Limite=" + TIME_LIMIT_MINUTES + "min | Cooldown=" + COOLDOWN_HOURS + "h"
			+ " | Cooldowns cargados=" + COOLDOWN_MAP.size());
	}

	// ---------------------------------------------------------------------------
	// Carga de configuracion
	// ---------------------------------------------------------------------------
	private void loadConfig()
	{
		try (InputStream is = new FileInputStream("config/Custom/SoloInstance.ini"))
		{
			final Properties p = new Properties();
			p.load(is);

			ENABLED             = Boolean.parseBoolean(p.getProperty("SoloInstanceEnabled",      "true").trim());
			NPC_ID              = Integer.parseInt    (p.getProperty("SoloInstanceNpcId",         "50027").trim());
			TIME_LIMIT_MINUTES  = Integer.parseInt    (p.getProperty("SoloInstanceTimeLimit",     "30").trim());
			COOLDOWN_HOURS      = Long.parseLong      (p.getProperty("SoloInstanceCooldownHours", "24").trim());
			REWARD_ITEM_ID      = Integer.parseInt    (p.getProperty("SoloInstanceRewardItemId",  "10639").trim());
			REWARD_COUNT        = Integer.parseInt    (p.getProperty("SoloInstanceRewardCount",   "500").trim());
			SPAWN_X             = Integer.parseInt    (p.getProperty("SoloInstanceSpawnX",        "42660").trim());
			SPAWN_Y             = Integer.parseInt    (p.getProperty("SoloInstanceSpawnY",        "-47996").trim());
			SPAWN_Z             = Integer.parseInt    (p.getProperty("SoloInstanceSpawnZ",        "-727").trim());

			// Cargar cadena de monstruos
			MONSTER_CHAIN.clear();
			final String raw = p.getProperty("SoloInstanceMonsters", "")
				.replace("\\", "").replace("\n", "").replace("\r", "").trim();

			if (!raw.isEmpty())
			{
				for (String entry : raw.split(";"))
				{
					entry = entry.trim();
					if (entry.isEmpty()) continue;
					final String[] parts = entry.split(",");
					if (parts.length >= 4)
					{
						MONSTER_CHAIN.add(new int[]
						{
							Integer.parseInt(parts[0].trim()),
							Integer.parseInt(parts[1].trim()),
							Integer.parseInt(parts[2].trim()),
							Integer.parseInt(parts[3].trim())
						});
					}
				}
			}

			// Defaults si no se configuraron monstruos (zona Kamaloka 73 — Sel Mahum)
			if (MONSTER_CHAIN.isEmpty())
			{
				MONSTER_CHAIN.add(new int[]{ 22786, 42800, -48100, -727 });
				MONSTER_CHAIN.add(new int[]{ 22787, 42900, -48200, -727 });
				MONSTER_CHAIN.add(new int[]{ 22788, 43000, -48300, -727 });
				MONSTER_CHAIN.add(new int[]{ 22775, 43100, -48400, -727 });
				MONSTER_CHAIN.add(new int[]{ 22776, 43200, -48500, -727 });
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("SoloInstance: Error al cargar configuracion: " + e.getMessage());
		}
	}

	// ---------------------------------------------------------------------------
	// Persistencia de cooldowns (sobrevive reinicios del servidor)
	// ---------------------------------------------------------------------------
	private void loadCooldowns()
	{
		final File file = new File(COOLDOWN_FILE);
		if (!file.exists())
		{
			return;
		}
		try (FileInputStream fis = new FileInputStream(file))
		{
			final Properties p = new Properties();
			p.load(fis);
			final long now        = System.currentTimeMillis();
			final long cooldownMs = COOLDOWN_HOURS * 3_600_000L;

			final Iterator<Map.Entry<Object, Object>> it = p.entrySet().iterator();
			while (it.hasNext())
			{
				final Map.Entry<Object, Object> entry = it.next();
				try
				{
					final int  objId     = Integer.parseInt(entry.getKey().toString());
					final long timestamp = Long.parseLong(entry.getValue().toString());

					// Solo cargar si el cooldown aun no expiro
					if ((timestamp + cooldownMs) > now)
					{
						COOLDOWN_MAP.put(objId, timestamp);
					}
					// Si ya expiro, simplemente no se carga (se limpia del archivo al guardar)
				}
				catch (Exception ignore)
				{
				}
			}
			LOGGER.info("SoloInstance: " + COOLDOWN_MAP.size() + " cooldown(s) activos cargados desde disco.");
		}
		catch (Exception e)
		{
			LOGGER.warning("SoloInstance: Error al cargar cooldowns: " + e.getMessage());
		}
	}

	private void saveCooldowns()
	{
		try
		{
			new File("./config/Custom").mkdirs();
			final Properties p = new Properties();
			final long now        = System.currentTimeMillis();
			final long cooldownMs = COOLDOWN_HOURS * 3_600_000L;

			// Solo guardar cooldowns que aun no expiraron
			for (Map.Entry<Integer, Long> entry : COOLDOWN_MAP.entrySet())
			{
				if ((entry.getValue() + cooldownMs) > now)
				{
					p.setProperty(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
				}
			}

			try (FileOutputStream fos = new FileOutputStream(COOLDOWN_FILE))
			{
				p.store(fos, "SoloInstance cooldowns — generado automaticamente, no editar");
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("SoloInstance: Error al guardar cooldowns: " + e.getMessage());
		}
	}

	// ---------------------------------------------------------------------------
	// Dialogo principal del NPC
	// ---------------------------------------------------------------------------
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (!ENABLED) return null;
		sendMainHtml(player, npc);
		return null;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (!ENABLED) return null;
		sendMainHtml(player, npc);
		return null;
	}

	private void sendMainHtml(Player player, Npc npc)
	{
		final long now        = System.currentTimeMillis();
		final long cooldownMs = COOLDOWN_HOURS * 3_600_000L;
		final long lastEnter  = COOLDOWN_MAP.getOrDefault(player.getObjectId(), 0L);
		final long remaining  = (lastEnter + cooldownMs) - now;
		final boolean premium = PremiumManager.getInstance().getPremiumExpiration(player.getAccountName()) > System.currentTimeMillis();

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<center><br>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
		sb.append("<font name=\"hs15\" color=\"CDB67F\">Instancia Solo</font><br1>");
		sb.append("<font color=\"B09878\">Zona de prueba individual</font><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=240 height=1><br><br>");

		// Info de la instancia
		sb.append("<table width=240 border=0>");
		sb.append("<tr><td align=center><font color=\"FFFFFF\">Limite: <font color=\"LEVEL\">")
			.append(TIME_LIMIT_MINUTES).append(" minutos</font></font></td></tr>");
		sb.append("<tr><td align=center><font color=\"FFFFFF\">Monstruos: <font color=\"LEVEL\">")
			.append(MONSTER_CHAIN.size()).append(" en cadena</font></font></td></tr>");
		sb.append("<tr><td align=center><font color=\"FFFFFF\">Recompensa: <font color=\"LEVEL\">")
			.append(premium ? REWARD_COUNT * 2 : REWARD_COUNT)
			.append(" x item</font></font></td></tr>");
		if (premium)
		{
			sb.append("<tr><td align=center><font color=\"93FFA8\">★ Eres Premium — Recompensa x2!</font></td></tr>");
		}
		sb.append("<tr><td align=center><font color=\"B09878\">Cooldown: ").append(COOLDOWN_HOURS).append(" horas</font></td></tr>");
		sb.append("</table><br>");

		sb.append("<img src=\"L2UI.SquareGray\" width=240 height=1><br><br>");

		if (ACTIVE_INSTANCES.containsKey(player.getObjectId()))
		{
			// Ya tiene instancia activa
			sb.append("<font color=\"FF9900\">Tienes una instancia en curso!</font><br><br>");
			sb.append("<button value=\"  Regresar a la Instancia  \" action=\"bypass -h Script SoloInstance rejoin\"");
			sb.append(" width=210 height=30 back=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Info\"/>");
		}
		else if (remaining > 0)
		{
			// En cooldown
			final long h = remaining / 3_600_000L;
			final long m = (remaining % 3_600_000L) / 60_000L;
			final long s = (remaining % 60_000L) / 1000L;
			sb.append("<font color=\"FF6060\">Ya completaste tu instancia hoy.</font><br>");
			sb.append("<font color=\"B09878\">Disponible en: <font color=\"LEVEL\">");
			if (h > 0) sb.append(h).append("h ");
			sb.append(m).append("m ").append(s).append("s</font></font><br>");
		}
		else
		{
			// Disponible para entrar
			sb.append("<font color=\"93FFA8\">La instancia esta disponible.</font><br><br>");
			sb.append("<button value=\"  Ingresar a la Instancia  \" action=\"bypass -h Script SoloInstance enter\"");
			sb.append(" width=210 height=30 back=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Info\"/>");
		}

		sb.append("<br></center></body></html>");

		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setHtml(sb.toString());
		player.sendPacket(msg);
	}

	// ---------------------------------------------------------------------------
	// Eventos / Bypasses
	// ---------------------------------------------------------------------------
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!ENABLED || player == null) return null;

		switch (event)
		{
			case "enter":
				handleEnterRequest(player, npc);
				break;
			case "confirm_enter":
				startInstance(player);
				break;
			case "rejoin":
				handleRejoin(player);
				break;
			case "back":
				sendMainHtml(player, npc);
				break;
		}
		return null;
	}

	// ---------------------------------------------------------------------------
	// Solicitud de entrada — verificaciones previas
	// ---------------------------------------------------------------------------
	private void handleEnterRequest(Player player, Npc npc)
	{
		// Ya tiene instancia activa
		if (ACTIVE_INSTANCES.containsKey(player.getObjectId()))
		{
			handleRejoin(player);
			return;
		}

		// Cooldown activo
		final long now        = System.currentTimeMillis();
		final long cooldownMs = COOLDOWN_HOURS * 3_600_000L;
		final long lastEnter  = COOLDOWN_MAP.getOrDefault(player.getObjectId(), 0L);
		if ((lastEnter + cooldownMs) > now)
		{
			sendMainHtml(player, npc);
			return;
		}

		// Verificaciones de estado del jugador
		if (player.isInCombat())
		{
			player.sendMessage("[Instancia] No puedes entrar en combate.");
			return;
		}
		if (player.getInstanceId() > 0)
		{
			player.sendMessage("[Instancia] Ya estas dentro de otra instancia.");
			return;
		}
		if (player.isInOlympiadMode())
		{
			player.sendMessage("[Instancia] No puedes entrar durante la Olimpiada.");
			return;
		}

		// Mostrar confirmacion con advertencia de buffs
		sendConfirmHtml(player, npc);
	}

	private void sendConfirmHtml(Player player, Npc npc)
	{
		final boolean premium = PremiumManager.getInstance().getPremiumExpiration(player.getAccountName()) > System.currentTimeMillis();
		final int rewardAmt   = premium ? REWARD_COUNT * 2 : REWARD_COUNT;

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center><br>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
		sb.append("<font name=\"hs15\" color=\"CDB67F\">Instancia Solo</font><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=240 height=1><br><br>");

		sb.append("<font color=\"FF9900\">— Advertencia antes de entrar —</font><br><br>");
		sb.append("<table width=240 border=0>");
		sb.append("<tr><td align=center><font color=\"FFFFFF\">Ingresa con todos tus</font></td></tr>");
		sb.append("<tr><td align=center><font color=\"LEVEL\">BUFFS activos</font></td></tr>");
		sb.append("<tr><td align=center><font color=\"FFFFFF\">Los buffs NO se aplicaran dentro.</font></td></tr>");
		sb.append("<tr><td height=8></td></tr>");
		sb.append("<tr><td align=center><font color=\"B09878\">Tiempo limite: <font color=\"FF6060\">").append(TIME_LIMIT_MINUTES).append(" minutos</font></font></td></tr>");
		sb.append("<tr><td align=center><font color=\"B09878\">Monstruos a eliminar: <font color=\"LEVEL\">").append(MONSTER_CHAIN.size()).append(" (en cadena)</font></font></td></tr>");
		sb.append("<tr><td align=center><font color=\"B09878\">Recompensa: <font color=\"LEVEL\">").append(rewardAmt).append(" unidades</font></font></td></tr>");
		if (premium)
		{
			sb.append("<tr><td align=center><font color=\"93FFA8\">★ Bonus Premium aplicado</font></td></tr>");
		}
		sb.append("</table><br>");

		sb.append("<img src=\"L2UI.SquareGray\" width=240 height=1><br><br>");
		sb.append("<table width=240><tr>");
		sb.append("<td align=center><button value=\"  Entrar  \" action=\"bypass -h Script SoloInstance confirm_enter\"");
		sb.append(" width=110 height=30 back=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Info\"/></td>");
		sb.append("<td align=center><button value=\"  Cancelar  \" action=\"bypass -h Script SoloInstance back\"");
		sb.append(" width=110 height=30 back=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Info\"/></td>");
		sb.append("</tr></table>");
		sb.append("<br></center></body></html>");

		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setHtml(sb.toString());
		player.sendPacket(msg);
	}

	private void handleRejoin(Player player)
	{
		final ActiveInstance ai = ACTIVE_INSTANCES.get(player.getObjectId());
		if (ai == null)
		{
			player.sendMessage("[Instancia] No tienes una instancia activa.");
			return;
		}
		player.teleToLocation(new Location(SPAWN_X, SPAWN_Y, SPAWN_Z), ai.world.getInstanceId(), 50);
		player.sendMessage("[Instancia] Regresaste a tu instancia.");
	}

	// ---------------------------------------------------------------------------
	// Inicio de instancia
	// ---------------------------------------------------------------------------
	private void startInstance(Player player)
	{
		// Re-verificar por si acaso
		if (ACTIVE_INSTANCES.containsKey(player.getObjectId()))
		{
			handleRejoin(player);
			return;
		}

		// Crear instancia usando el template SoloInstance (id=9001)
		final InstanceWorld world = new InstanceWorld();
		world.setInstance(InstanceManager.getInstance().createDynamicInstance(INSTANCE_TEMPLATE_ID));
		InstanceManager.getInstance().addWorld(world);
		world.addAllowed(player);

		// Ajustar duracion al config
		final Instance inst = InstanceManager.getInstance().getInstance(world.getInstanceId());
		if (inst == null)
		{
			player.sendMessage("[Instancia] Error al crear la instancia. Contacta un GM.");
			LOGGER.warning("SoloInstance: Instancia creada pero no encontrada para " + player.getName());
			return;
		}
		inst.setDuration((TIME_LIMIT_MINUTES + 5) * 60_000);
		inst.setEmptyDestroyTime(60_000);

		// Registrar instancia activa
		final ActiveInstance ai = new ActiveInstance(world, player.getObjectId());
		ACTIVE_INSTANCES.put(player.getObjectId(), ai);

		// Registrar cooldown desde el momento de entrada y persistir en disco
		COOLDOWN_MAP.put(player.getObjectId(), System.currentTimeMillis());
		saveCooldowns();

		// Teleportar al jugador dentro
		player.teleToLocation(new Location(SPAWN_X, SPAWN_Y, SPAWN_Z), inst.getId(), 50);

		// Mensajes de bienvenida
		player.sendMessage("==============================================");
		player.sendMessage("  INSTANCIA SOLO — Aden Chronicles");
		player.sendMessage("  Tiempo: " + TIME_LIMIT_MINUTES + " minutos.");
		player.sendMessage("  Debes eliminar " + MONSTER_CHAIN.size() + " monstruos en orden.");
		player.sendMessage("==============================================");
		player.sendPacket(new ExShowScreenMessage(
			"Instancia Solo — Iniciada!", ExShowScreenMessage.TOP_CENTER, 5000, 0, true, true));

		// Timer de timeout
		ai.timeoutTask = ThreadPool.schedule(
			() -> onTimeout(player.getObjectId()),
			(long) TIME_LIMIT_MINUTES * 60_000L
		);

		// Primer monstruo tras 3 segundos (para que la zona cargue)
		ThreadPool.schedule(() -> spawnNextMonster(player.getObjectId()), 3000L);
	}

	// ---------------------------------------------------------------------------
	// Cadena de monstruos
	// ---------------------------------------------------------------------------
	private void spawnNextMonster(int playerObjId)
	{
		final ActiveInstance ai = ACTIVE_INSTANCES.get(playerObjId);
		if (ai == null) return;

		final Player player = findPlayer(playerObjId);
		if ((player == null) || (player.isOnlineInt() != 1))
		{
			cleanupInstance(playerObjId, false);
			return;
		}

		final int idx = ai.currentMonsterIndex;
		if (idx >= MONSTER_CHAIN.size())
		{
			onInstanceComplete(playerObjId);
			return;
		}

		final int[] data      = MONSTER_CHAIN.get(idx);
		final int   monsterId = data[0];
		final int   mx        = data[1];
		final int   my        = data[2];
		final int   mz        = data[3];

		final Npc monster = addSpawn(monsterId, mx, my, mz, 0, false, 0, false, ai.world.getInstanceId());
		if (monster == null)
		{
			LOGGER.warning("SoloInstance: No se pudo spawnear monsterId=" + monsterId + " para " + player.getName());
			player.sendMessage("[Instancia] Error al spawnear monstruo " + (idx + 1) + ". Contacta un GM.");
			cleanupInstance(playerObjId, false);
			return;
		}

		ai.currentMonster = monster;

		// Listener de muerte ligado a este monstruo
		final ConsumerEventListener dl = new ConsumerEventListener(
			monster, EventType.ON_CREATURE_DEATH,
			(OnCreatureDeath ev) ->
			{
				if (ev.getTarget() == monster)
				{
					onMonsterDeath(playerObjId, monster);
				}
			},
			this
		);
		monster.addListener(dl);
		ai.deathListener = dl;

		// Notificar al jugador
		player.sendMessage("[Instancia] Monstruo " + (idx + 1) + "/" + MONSTER_CHAIN.size() + " ha aparecido!");
		player.sendPacket(new ExShowScreenMessage(
			"Monstruo " + (idx + 1) + " de " + MONSTER_CHAIN.size(),
			ExShowScreenMessage.TOP_CENTER, 3000, 0, true, false));
	}

	private void onMonsterDeath(int playerObjId, Npc deadMonster)
	{
		final ActiveInstance ai = ACTIVE_INSTANCES.get(playerObjId);
		if (ai == null) return;
		if (ai.currentMonster != deadMonster) return;

		// Desregistrar listener del monstruo muerto
		if (ai.deathListener != null)
		{
			ai.deathListener.unregisterMe();
			ai.deathListener = null;
		}
		ai.currentMonster = null;
		ai.currentMonsterIndex++;

		if (ai.currentMonsterIndex >= MONSTER_CHAIN.size())
		{
			// Ultimo monstruo eliminado
			onInstanceComplete(playerObjId);
		}
		else
		{
			final Player player = findPlayer(playerObjId);
			if (player != null)
			{
				player.sendMessage("[Instancia] Monstruo eliminado! Siguiente en 3 segundos...");
			}
			ThreadPool.schedule(() -> spawnNextMonster(playerObjId), 3000L);
		}
	}

	// ---------------------------------------------------------------------------
	// Instancia completada con exito
	// ---------------------------------------------------------------------------
	private void onInstanceComplete(int playerObjId)
	{
		final ActiveInstance ai = ACTIVE_INSTANCES.get(playerObjId);
		if (ai == null) return;

		// Cancelar timeout
		if (ai.timeoutTask != null)
		{
			ai.timeoutTask.cancel(false);
			ai.timeoutTask = null;
		}

		final Player player = findPlayer(playerObjId);

		// Calcular recompensa (x2 si es premium)
		int reward = REWARD_COUNT;
		boolean isPremium = false;
		if (player != null)
		{
			isPremium = PremiumManager.getInstance().getPremiumExpiration(player.getAccountName()) > System.currentTimeMillis();
			if (isPremium) reward *= 2;
		}

		final int finalReward = reward;
		final boolean wasPremium = isPremium;

		if ((player != null) && (player.isOnlineInt() == 1))
		{
			player.addItem(ItemProcessType.REWARD, REWARD_ITEM_ID, finalReward, null, true);

			player.sendMessage("==============================================");
			player.sendMessage("  INSTANCIA COMPLETADA!");
			player.sendMessage("  Recompensa: " + finalReward + " unidades.");
			if (wasPremium)
			{
				player.sendMessage("  (Bonus Premium x2 aplicado)");
			}
			player.sendMessage("==============================================");
			player.sendPacket(new ExShowScreenMessage(
				"Instancia Completada! +" + finalReward, ExShowScreenMessage.TOP_CENTER, 7000, 0, true, true));

			// Teleportar a Giran tras 5 segundos
			ThreadPool.schedule(() ->
			{
				if (player.isOnlineInt() == 1)
				{
					player.teleToLocation(EXIT_LOC, 0, 50);
				}
				cleanupInstance(playerObjId, true);
			}, 5000L);
		}
		else
		{
			cleanupInstance(playerObjId, true);
		}
	}

	// ---------------------------------------------------------------------------
	// Timeout (tiempo agotado)
	// ---------------------------------------------------------------------------
	private void onTimeout(int playerObjId)
	{
		final ActiveInstance ai = ACTIVE_INSTANCES.get(playerObjId);
		if (ai == null) return;

		final Player player = findPlayer(playerObjId);
		if ((player != null) && (player.isOnlineInt() == 1))
		{
			player.sendMessage("[Instancia] Tiempo agotado! Fuiste expulsado de la instancia.");
			player.sendPacket(new ExShowScreenMessage(
				"Tiempo Agotado!", ExShowScreenMessage.TOP_CENTER, 5000, 0, true, true));
			player.teleToLocation(EXIT_LOC, 0, 50);
		}

		// Si timeout, quitar cooldown para que pueda reintentar
		cleanupInstance(playerObjId, false);
	}

	// ---------------------------------------------------------------------------
	// Limpieza de instancia
	// ---------------------------------------------------------------------------
	private void cleanupInstance(int playerObjId, boolean completed)
	{
		final ActiveInstance ai = ACTIVE_INSTANCES.remove(playerObjId);
		if (ai == null) return;

		// Cancelar timeout pendiente
		if (ai.timeoutTask != null)
		{
			ai.timeoutTask.cancel(false);
			ai.timeoutTask = null;
		}

		// Desregistrar listener de monstruo actual
		if (ai.deathListener != null)
		{
			ai.deathListener.unregisterMe();
			ai.deathListener = null;
		}

		// Destruir instancia
		final Instance inst = InstanceManager.getInstance().getInstance(ai.world.getInstanceId());
		if (inst != null)
		{
			inst.setDuration(10_000);
			inst.setEmptyDestroyTime(0);
		}

		// Si no completó con exito, remover cooldown para permitir reintento
		if (!completed)
		{
			COOLDOWN_MAP.remove(playerObjId);
		}

		// Persistir estado actualizado en disco
		saveCooldowns();
	}

	// ---------------------------------------------------------------------------
	// Utilidades
	// ---------------------------------------------------------------------------
	private Player findPlayer(int objectId)
	{
		for (Player p : World.getInstance().getPlayers())
		{
			if (p.getObjectId() == objectId)
			{
				return p;
			}
		}
		return null;
	}

	// ---------------------------------------------------------------------------
	// Main — carga automatica por el engine
	// ---------------------------------------------------------------------------
	public static void main(String[] args)
	{
		new SoloInstance();
	}
}
