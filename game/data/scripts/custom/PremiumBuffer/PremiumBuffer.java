/*
 * PremiumBuffer NPC - Buffer premium con enchant +15/+30 y sistema de esquemas.
 *
 * Flujo:
 *   Jugador habla con NPC -> verifica premium -> menu principal
 *   -> elige enchant (+15 o +30) -> selecciona esquema -> aplica (cuesta items)
 *   -> edita esquema (toggle buffs) -> guarda automatico
 *
 * Config : game/config/Custom/PremiumBuffer.ini
 * Esquemas: game/config/Custom/PremiumBufferSchemes.properties (auto-generado)
 */
package custom.PremiumBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.managers.PremiumManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class PremiumBuffer extends Script
{
	private static final Logger LOGGER = Logger.getLogger(PremiumBuffer.class.getName());

	// ── Config ─────────────────────────────────────────────────────────────────
	private static int NPC_ID          = 50028;
	private static int MAX_SCHEMES     = 4;
	private static int ENCHANT_LOW     = 15;
	private static int ENCHANT_HIGH    = 30;
	private static int COST_ITEM_ID    = 10639;
	private static int COST_CHAR       = 5;
	private static int COST_PET        = 3;
	private static boolean ENABLED     = true;

	// ── Persistencia de esquemas ────────────────────────────────────────────────
	// Clave: "{playerObjId}_{schemeIdx}"  Valor: set de "skillId:baseLevel"
	private static final Map<String, LinkedHashSet<String>> SCHEMES = new ConcurrentHashMap<>();
	private static final String SCHEMES_FILE = "config/Custom/PremiumBufferSchemes.properties";

	// ── Lista maestra de skills ─────────────────────────────────────────────────
	// Formato: {skillId, baseLevel}
	// Buffs encantables
	private static final int[][] BUFFS =
	{
		{1068, 3}, // Might
		{1388, 3}, // Greater Might
		{1077, 3}, // Focus
		{1242, 3}, // Death Whisper
		{1086, 2}, // Haste
		{1085, 3}, // Acumen
		{1204, 2}, // Wind Walk
		{1087, 3}, // Agility
		{1040, 3}, // Shield
		{1389, 3}, // Greater Shield
		{1036, 2}, // Magic Barrier
		{1035, 4}, // Mental Shield
		{1044, 3}, // Regeneration
		{1045, 6}, // Blessed Body
		{1048, 6}, // Blessed Soul
		{1243, 6}, // Bless Shield
		{1059, 3}, // Empower
		{1078, 6}, // Concentration
		{1303, 2}, // Wild Magic
		{1268, 4}, // Vampiric Rage
		{1062, 2}, // Berserker Spirit
		{1240, 3}, // Guidance
		{1304, 3}, // Advanced Block
		{1397, 3}, // Clarity
		{1390, 3}, // War Chant
		{1391, 3}, // Earth Chant
		{1043, 1}, // Holy Weapon
		{1542, 1}, // Counter Critical
	};

	// Resistencias encantables
	private static final int[][] RESISTS =
	{
		{1259, 4}, // Resist Shock
		{1032, 3}, // Invigor (Resist Bleed)
		{1033, 3}, // Resist Poison
		{1182, 3}, // Resist Aqua
		{1189, 3}, // Resist Wind
		{1191, 3}, // Resist Fire
		{1548, 3}, // Resist Earth
		{1392, 3}, // Holy Resistance
		{1393, 3}, // Unholy Resistance
		{1352, 1}, // Elemental Protection
		{1353, 1}, // Divine Protection
		{1354, 1}, // Arcane Protection
	};

	// Danzas (nivel fijo 1, sin enchant significativo)
	private static final int[][] DANCES =
	{
		{271, 1}, // Dance of the Warrior
		{272, 1}, // Dance of Inspiration
		{273, 1}, // Dance of the Mystic
		{274, 1}, // Dance of Fire
		{275, 1}, // Dance of Fury
		{276, 1}, // Dance of Concentration
		{277, 1}, // Dance of Light
		{307, 1}, // Dance of Aqua Guard
		{309, 1}, // Dance of Earth Guard
		{310, 1}, // Dance of the Vampire
		{311, 1}, // Dance of Protection
		{365, 1}, // Siren's Dance
		{915, 1}, // Dance of Berserker
		{530, 1}, // Dance of Alignment
		{366, 1}, // Dance of Shadow
	};

	// Canciones (nivel fijo 1)
	private static final int[][] SONGS =
	{
		{264, 1}, // Song of Earth
		{265, 1}, // Song of Life
		{266, 1}, // Song of Water
		{267, 1}, // Song of Warding
		{268, 1}, // Song of Wind
		{269, 1}, // Song of Hunter
		{270, 1}, // Song of Invocation
		{304, 1}, // Song of Vitality
		{305, 1}, // Song of Vengeance
		{306, 1}, // Song of Flame Guard
		{308, 1}, // Song of Storm Guard
		{349, 1}, // Song of Renewal
		{363, 1}, // Song of Meditation
		{364, 1}, // Song of Champion
		{529, 1}, // Song of Elemental
	};

	// Canticos
	private static final int[][] CHANTS =
	{
		{1002, 3}, // Flame Chant
		{1006, 3}, // Chant of Fire
		{1007, 3}, // Chant of Battle
		{1009, 3}, // Chant of Shielding
		{1251, 2}, // Chant of Fury
		{1252, 3}, // Chant of Evasion
		{1253, 3}, // Chant of Rage
		{1284, 3}, // Chant of Revenge
		{1308, 3}, // Chant of Predator
		{1309, 3}, // Chant of Eagle
		{1310, 4}, // Chant of Vampiric
		{1362, 1}, // Chant of Spirit
		{1461, 1}, // Chant of Protection
		{1517, 1}, // Chant of Combat
		{1518, 1}, // Chant of Critical Attack
		{1519, 1}, // Chant of Blood Awakening
		{1535, 1}, // Chant of Movement
		{1549, 1}, // Chant of Elements
	};

	// Especiales / Profecías
	private static final int[][] SPECIAL =
	{
		{1355, 1}, // Prophecy of Water
		{1356, 1}, // Prophecy of Fire
		{1357, 1}, // Prophecy of Wind
		{1363, 1}, // Chant of Victory
		{1413, 1}, // Magnus' Chant
		{1414, 1}, // Victory of Paagrio
		{1323, 1}, // Noblesse Blessing
		{1501, 1}, // Improved Condition
		{1499, 1}, // Improved Combat
		{1504, 1}, // Improved Movement
		{4699, 13}, // Blessing of Queen
		{4700, 13}, // Gift of Queen
		{4702, 13}, // Blessing of Seraphim
		{4703, 13}, // Gift of Seraphim
	};

	// ── Constructor ────────────────────────────────────────────────────────────
	private PremiumBuffer()
	{
		loadConfig();
		loadSchemes();

		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);

		LOGGER.info("PremiumBuffer: NPC=" + NPC_ID
			+ " | MaxEsquemas=" + MAX_SCHEMES
			+ " | Enchant=+" + ENCHANT_LOW + "/+" + ENCHANT_HIGH
			+ " | Costo=" + COST_CHAR + "x" + COST_ITEM_ID + " (char), "
			+ COST_PET + "x" + COST_ITEM_ID + " (pet)");
	}

	// ── Eventos NPC ────────────────────────────────────────────────────────────
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (!ENABLED)
		{
			return null;
		}
		sendHtml(npc, player, buildMainPage(npc, player));
		return null;
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!ENABLED || (player == null))
		{
			return null;
		}

		final String[] p = event.split("_");

		switch (p[0])
		{
			case "main":
				sendHtml(npc, player, buildMainPage(npc, player));
				break;

			case "enchant":
				// enchant_15 o enchant_30
				if (p.length >= 2)
				{
					final int enchant = safeInt(p[1], ENCHANT_LOW);
					sendHtml(npc, player, buildSchemeListPage(npc, player, enchant));
				}
				break;

			case "edit":
				// edit_{schemeIdx}
				if (p.length >= 2 && checkPremium(npc, player))
				{
					final int idx = safeInt(p[1], 1);
					sendHtml(npc, player, buildEditorPage(npc, player, idx));
				}
				break;

			case "toggle":
				// toggle_{schemeIdx}_{skillId}_{skillLevel}
				if (p.length >= 4 && checkPremium(npc, player))
				{
					final int idx       = safeInt(p[1], 1);
					final int skillId   = safeInt(p[2], 0);
					final int skillLvl  = safeInt(p[3], 1);
					toggleSkill(player, idx, skillId, skillLvl);
					sendHtml(npc, player, buildEditorPage(npc, player, idx));
				}
				break;

			case "apply":
				// apply_{schemeIdx}_{enchant}_{target}  target: char | pet | both
				if (p.length >= 4 && checkPremium(npc, player))
				{
					final int idx     = safeInt(p[1], 1);
					final int enchant = safeInt(p[2], ENCHANT_LOW);
					final String tgt  = p[3];
					applyScheme(npc, player, idx, enchant, tgt);
					sendHtml(npc, player, buildSchemeListPage(npc, player, enchant));
				}
				break;

			case "heal":
				if (checkPremium(npc, player))
				{
					healPlayer(player);
					sendHtml(npc, player, buildMainPage(npc, player));
				}
				break;
		}
		return null;
	}

	// ── Paginas HTML ───────────────────────────────────────────────────────────

	private String buildMainPage(Npc npc, Player player)
	{
		final boolean hasPremium = hasPremium(player);
		final String itemName    = getItemName(COST_ITEM_ID);

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=300 background=\"L2UI_CH3.refinewnd_back_Pattern\">");
		sb.append("<tr><td valign=top align=center>");

		// Encabezado
		appendHeader(sb, "Buffer Premium");

		// Estado premium
		sb.append("<table border=0 cellpadding=3 cellspacing=0 width=270 bgcolor=\"0A0A1E\">");
		sb.append("<tr><td align=center>");
		if (hasPremium)
		{
			sb.append("<font color=\"00FF00\">&#9733; PREMIUM ACTIVO &#9733;</font>");
		}
		else
		{
			sb.append("<font color=\"FF4040\">&#10007; SIN PREMIUM</font>");
		}
		sb.append("</td></tr></table>");
		sb.append("<br>");

		if (hasPremium)
		{
			// Separador
			sb.append("<img src=\"L2UI.SquareGray\" width=260 height=1><br><br>");

			// Boton principal: Buffs Premium +15/+30
			sb.append("<table border=0 width=270 cellspacing=4>");
			sb.append("<tr><td align=center>");
			sb.append("<button value=\"Buffs Premium  +" + ENCHANT_LOW + " / +" + ENCHANT_HIGH + "\"");
			sb.append(" action=\"bypass -h Script PremiumBuffer enchant_" + ENCHANT_LOW + "\"");
			sb.append(" width=260 height=34 back=\"L2UI_CT1.OlympiadWnd_DF_GrandTexture\" fore=\"L2UI_CT1.OlympiadWnd_DF_GrandTexture\">");
			sb.append("</td></tr>");

			// Boton heal
			sb.append("<tr><td align=center>");
			sb.append("<button value=\"Curar (HP / MP / CP)\"");
			sb.append(" action=\"bypass -h Script PremiumBuffer heal\"");
			sb.append(" width=260 height=28 back=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Info\">");
			sb.append("</td></tr>");
			sb.append("</table><br>");

			// Info de costo
			sb.append("<img src=\"L2UI.SquareGray\" width=260 height=1><br>");
			sb.append("<table border=0 width=270 cellpadding=2>");
			sb.append("<tr><td align=center><font color=\"7A7060\">Costo por esquema:</font></td></tr>");
			sb.append("<tr><td align=center><font color=\"CDB67F\">Personaje: " + COST_CHAR + " x " + itemName + "</font></td></tr>");
			sb.append("<tr><td align=center><font color=\"CDB67F\">Pet: " + COST_PET + " x " + itemName + "</font></td></tr>");
			sb.append("</table>");
		}
		else
		{
			sb.append("<table border=0 width=270 cellpadding=4>");
			sb.append("<tr><td align=center><font color=\"9A8060\">Necesitas estado Premium</font></td></tr>");
			sb.append("<tr><td align=center><font color=\"7A7060\">para acceder al Buffer Premium.</font></td></tr>");
			sb.append("</table>");
		}

		appendFooter(sb);
		return sb.toString();
	}

	private String buildSchemeListPage(Npc npc, Player player, int enchant)
	{
		final long balance   = player.getInventory().getInventoryItemCount(COST_ITEM_ID, -1);
		final String itemName = getItemName(COST_ITEM_ID);

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=300 background=\"L2UI_CH3.refinewnd_back_Pattern\">");
		sb.append("<tr><td valign=top align=center>");

		appendHeader(sb, "Esquemas  +" + enchant);

		// Balance
		sb.append("<table border=0 width=270 cellpadding=2 bgcolor=\"0A0A1E\">");
		sb.append("<tr><td align=center><font color=\"B09878\">Balance: </font>");
		sb.append("<font color=\"CDB67F\">").append(balance).append(" ").append(itemName).append("</font></td></tr>");
		sb.append("</table><br>");

		// Lista de esquemas
		for (int i = 1; i <= MAX_SCHEMES; i++)
		{
			final Set<String> scheme = getScheme(player, i);
			final int count          = scheme.size();
			final boolean canAffordChar = balance >= COST_CHAR;
			final boolean canAffordPet  = (player.getSummon() != null) && (balance >= COST_PET);

			sb.append("<table border=0 cellpadding=0 cellspacing=0 width=270 bgcolor=\"12121E\">");
			sb.append("<tr><td height=6></td></tr>");

			// Titulo del esquema
			sb.append("<tr><td align=center>");
			sb.append("<font color=\"CDB67F\">Esquema ").append(i).append("</font>");
			sb.append("  <font color=\"696969\">(").append(count).append(" skills)</font>");
			sb.append("</td></tr>");

			sb.append("<tr><td height=4></td></tr>");

			// Botones aplicar
			sb.append("<tr><td align=center>");
			sb.append("<table border=0 cellspacing=3><tr>");

			// Boton personaje
			final String btnCharStyle = canAffordChar
				? "back=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm\""
				: "back=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\"";
			sb.append("<td><button value=\"Personaje (").append(COST_CHAR).append("x)\"")
				.append(" action=\"bypass -h Script PremiumBuffer apply_").append(i).append("_").append(enchant).append("_char\"")
				.append(" width=124 height=26 ").append(btnCharStyle).append("></td>");

			// Boton pet
			final String btnPetStyle = canAffordPet
				? "back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\""
				: "back=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Info_Down\"";
			sb.append("<td><button value=\"Pet (").append(COST_PET).append("x)\"")
				.append(" action=\"bypass -h Script PremiumBuffer apply_").append(i).append("_").append(enchant).append("_pet\"")
				.append(" width=124 height=26 ").append(btnPetStyle).append("></td>");
			sb.append("</tr></table>");
			sb.append("</td></tr>");

			// Boton editar
			sb.append("<tr><td align=center>");
			sb.append("<button value=\"Editar Esquema ").append(i).append("\"")
				.append(" action=\"bypass -h Script PremiumBuffer edit_").append(i).append("\"")
				.append(" width=254 height=24 back=\"L2UI_CT1.OlympiadWnd_DF_Back_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Back\">");
			sb.append("</td></tr>");

			sb.append("<tr><td height=6></td></tr>");
			sb.append("</table>");
			sb.append("<br>");
		}

		// Seleccionar el otro enchant
		final int otherEnchant = (enchant == ENCHANT_LOW) ? ENCHANT_HIGH : ENCHANT_LOW;
		sb.append("<table border=0 width=270 cellspacing=3><tr>");
		sb.append("<td><button value=\"Ver +").append(otherEnchant).append("\"")
			.append(" action=\"bypass -h Script PremiumBuffer enchant_").append(otherEnchant).append("\"")
			.append(" width=124 height=26 back=\"L2UI_CT1.OlympiadWnd_DF_Fight3None_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Fight3None\"></td>");
		sb.append("<td><button value=\"Volver\"")
			.append(" action=\"bypass -h Script PremiumBuffer main\"")
			.append(" width=124 height=26 back=\"L2UI_CT1.OlympiadWnd_DF_Back_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Back\"></td>");
		sb.append("</tr></table>");

		appendFooter(sb);
		return sb.toString();
	}

	private String buildEditorPage(Npc npc, Player player, int schemeIdx)
	{
		final Set<String> scheme = getScheme(player, schemeIdx);

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=300 background=\"L2UI_CH3.refinewnd_back_Pattern\">");
		sb.append("<tr><td valign=top align=center>");

		appendHeader(sb, "Editando: Esquema " + schemeIdx);

		sb.append("<table border=0 cellpadding=2 width=270 bgcolor=\"0A0A1E\">");
		sb.append("<tr><td align=center><font color=\"7A7060\">");
		sb.append(scheme.size()).append(" skills seleccionadas. Click para agregar/quitar.");
		sb.append("</font></td></tr></table><br>");

		// Categoria: Buffs
		appendCategoryGrid(sb, player, schemeIdx, "Buffs", BUFFS, scheme);
		// Categoria: Resistencias
		appendCategoryGrid(sb, player, schemeIdx, "Resistencias", RESISTS, scheme);
		// Categoria: Danzas
		appendCategoryGrid(sb, player, schemeIdx, "Danzas", DANCES, scheme);
		// Categoria: Canciones
		appendCategoryGrid(sb, player, schemeIdx, "Canciones", SONGS, scheme);
		// Categoria: Canticos
		appendCategoryGrid(sb, player, schemeIdx, "Canticos", CHANTS, scheme);
		// Categoria: Especiales
		appendCategoryGrid(sb, player, schemeIdx, "Especiales", SPECIAL, scheme);

		// Botones navegacion
		sb.append("<br><table border=0 width=270 cellspacing=3><tr>");
		sb.append("<td><button value=\"&#10003; Listo\"")
			.append(" action=\"bypass -h Script PremiumBuffer enchant_").append(ENCHANT_LOW).append("\"")
			.append(" width=124 height=28 back=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm\"></td>");
		sb.append("<td><button value=\"Limpiar\"")
			.append(" action=\"bypass -h Script PremiumBuffer cleareditor_").append(schemeIdx).append("\"")
			.append(" width=124 height=28 back=\"L2UI_CT1.OlympiadWnd_DF_Back_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Back\"></td>");
		sb.append("</tr></table>");

		appendFooter(sb);
		return sb.toString();
	}

	/** Renderiza una categoria de buffs en grilla de iconos 8 por fila. */
	private static void appendCategoryGrid(StringBuilder sb, Player player, int schemeIdx, String label, int[][] skills, Set<String> scheme)
	{
		sb.append("<table border=0 cellpadding=1 width=270 bgcolor=\"12121E\">");
		sb.append("<tr><td align=center height=18>");
		sb.append("<font color=\"CDB67F\">").append(label).append("</font>");
		sb.append("</td></tr>");
		sb.append("<tr><td align=center>");
		sb.append("<table border=0 cellspacing=2 cellpadding=0>");

		int col = 0;
		for (int[] s : skills)
		{
			final int skillId  = s[0];
			final int skillLvl = s[1];
			final String key   = skillId + ":" + skillLvl;
			final boolean selected = scheme.contains(key);

			if (col == 0)
			{
				sb.append("<tr>");
			}

			// Seleccionado = boton con overlay dorado, no seleccionado = transparente
			final String backFore = selected
				? "back=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm_Down\""
				: "back=\"\" fore=\"\"";

			sb.append("<td width=32 height=32 background=\"icon.skill").append(skillId).append("\">");
			sb.append("<button value=\" \"")
				.append(" action=\"bypass -h Script PremiumBuffer toggle_").append(schemeIdx)
				.append("_").append(skillId).append("_").append(skillLvl).append("\"")
				.append(" width=30 height=30 ").append(backFore).append(">");
			sb.append("</td>");

			col++;
			if (col >= 8)
			{
				sb.append("</tr>");
				col = 0;
			}
		}

		// Cerrar ultima fila si quedo abierta
		if (col > 0)
		{
			sb.append("</tr>");
		}

		sb.append("</table>");
		sb.append("</td></tr>");
		sb.append("<tr><td height=4></td></tr>");
		sb.append("</table><br>");
	}

	// ── Logica de esquemas ─────────────────────────────────────────────────────

	private static LinkedHashSet<String> getScheme(Player player, int idx)
	{
		final String key = player.getObjectId() + "_" + idx;
		return SCHEMES.computeIfAbsent(key, k -> new LinkedHashSet<>());
	}

	private static void toggleSkill(Player player, int schemeIdx, int skillId, int skillLvl)
	{
		final String key    = player.getObjectId() + "_" + schemeIdx;
		final String entry  = skillId + ":" + skillLvl;
		final LinkedHashSet<String> scheme = SCHEMES.computeIfAbsent(key, k -> new LinkedHashSet<>());

		if (scheme.contains(entry))
		{
			scheme.remove(entry);
		}
		else
		{
			scheme.add(entry);
		}

		saveSchemes();
	}

	private static void clearScheme(Player player, int schemeIdx)
	{
		final String key = player.getObjectId() + "_" + schemeIdx;
		SCHEMES.put(key, new LinkedHashSet<>());
		saveSchemes();
	}

	// ── Aplicacion de buffs ────────────────────────────────────────────────────

	private static void applyScheme(Npc npc, Player player, int schemeIdx, int enchant, String target)
	{
		final Set<String> scheme = getScheme(player, schemeIdx);
		if (scheme.isEmpty())
		{
			player.sendMessage("[Buffer Premium] El Esquema " + schemeIdx + " esta vacio. Editalo primero.");
			return;
		}

		// Verificar costo
		final boolean forChar = target.equals("char") || target.equals("both");
		final boolean forPet  = target.equals("pet")  || target.equals("both");
		int totalCost         = 0;
		if (forChar) totalCost += COST_CHAR;
		if (forPet && player.getSummon() != null) totalCost += COST_PET;

		final long balance = player.getInventory().getInventoryItemCount(COST_ITEM_ID, -1);
		if (balance < totalCost)
		{
			final String itemName = getItemName(COST_ITEM_ID);
			player.sendMessage("[Buffer Premium] Necesitas " + totalCost + " x " + itemName
				+ " (tienes " + balance + ").");
			return;
		}

		// Cobrar
		player.destroyItemByItemId(ItemProcessType.FEE, COST_ITEM_ID, totalCost, npc, true);

		// Construir targets
		final List<Creature> targets = new ArrayList<>();
		if (forChar) targets.add(player);
		if (forPet && player.getSummon() != null) targets.add(player.getSummon());

		// Aplicar cada skill
		int applied = 0;
		for (String entry : scheme)
		{
			final String[] parts = entry.split(":");
			if (parts.length < 2) continue;

			final int skillId   = Integer.parseInt(parts[0]);
			final int baseLevel = Integer.parseInt(parts[1]);

			final Skill skill = resolveSkill(skillId, baseLevel, enchant);
			if (skill == null) continue;

			for (Creature t : targets)
			{
				skill.applyEffects(player, t);
				player.sendPacket(new MagicSkillUse(player, t,
					skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
			}
			applied++;
		}

		final String targetLabel = forChar && forPet ? "personaje y pet"
			: forChar ? "personaje" : "pet";
		player.sendMessage("[Buffer Premium] +" + enchant + " | Esquema " + schemeIdx
			+ " aplicado a " + targetLabel + " (" + applied + " skills).");
	}

	/**
	 * Intenta obtener la version encantada de un skill.
	 * Si no existe al nivel buscado, baja el enchant hasta encontrar uno valido.
	 * Si no hay ninguno encantado, retorna la version base.
	 */
	private static Skill resolveSkill(int skillId, int baseLevel, int enchant)
	{
		// Intentar con enchant solicitado, bajando si no existe
		for (int e = enchant; e >= 1; e--)
		{
			final Skill s = SkillData.getInstance().getSkill(skillId, baseLevel + e);
			if (s != null) return s;
		}
		// Fallback a nivel base
		return SkillData.getInstance().getSkill(skillId, baseLevel);
	}

	// ── Heal ──────────────────────────────────────────────────────────────────

	private static void healPlayer(Player player)
	{
		player.setCurrentHp(player.getMaxHp());
		player.setCurrentMp(player.getMaxMp());
		player.setCurrentCp(player.getMaxCp());
		player.updateUserInfo();

		final Summon pet = player.getSummon();
		if (pet != null)
		{
			pet.setCurrentHp(pet.getMaxHp());
			pet.setCurrentMp(pet.getMaxMp());
		}
		player.sendMessage("[Buffer Premium] HP / MP / CP restaurados.");
	}

	// ── Verificaciones ─────────────────────────────────────────────────────────

	private static boolean hasPremium(Player player)
	{
		return PremiumManager.getInstance().getPremiumExpiration(player.getAccountName()) > System.currentTimeMillis();
	}

	private static boolean checkPremium(Npc npc, Player player)
	{
		if (!hasPremium(player))
		{
			final StringBuilder sb = new StringBuilder();
			sb.append("<html><body>");
			sb.append("<table border=0 cellpadding=0 cellspacing=0 width=300 background=\"L2UI_CH3.refinewnd_back_Pattern\">");
			sb.append("<tr><td valign=top align=center>");
			appendHeader(sb, "Buffer Premium");
			sb.append("<table border=0 width=270 cellpadding=6 bgcolor=\"0A0A1E\">");
			sb.append("<tr><td align=center><font color=\"FF4040\">Acceso Denegado</font></td></tr>");
			sb.append("<tr><td align=center><font color=\"9A8060\">El Buffer Premium es exclusivo</font></td></tr>");
			sb.append("<tr><td align=center><font color=\"9A8060\">para jugadores con Premium activo.</font></td></tr>");
			sb.append("</table><br>");
			sb.append("<button value=\"Volver\" action=\"bypass -h Script PremiumBuffer main\"");
			sb.append(" width=200 height=28 back=\"L2UI_CT1.OlympiadWnd_DF_Back_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Back\">");
			appendFooter(sb);
			sendHtml(npc, player, sb.toString());
			return false;
		}
		return true;
	}

	// ── Helpers HTML ──────────────────────────────────────────────────────────

	private static void appendHeader(StringBuilder sb, String title)
	{
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=290>");
		sb.append("<tr><td width=270 height=90 background=\"L2UI_CT1.OlympiadWnd_DF_GrandTexture\"></td></tr>");
		sb.append("</table>");
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=290>");
		sb.append("<tr><td align=center>");
		sb.append("<font name=\"hs13\" color=\"CDB67F\">").append(title).append("</font><br1>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=28>");
		sb.append("</td></tr></table><br>");
	}

	private static void appendFooter(StringBuilder sb)
	{
		sb.append("<br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=260 height=1><br>");
		sb.append("<table border=0 width=270 cellpadding=2>");
		sb.append("<tr><td align=center><font color=\"696969\">L2 Zona Zero &#183; Premium</font></td></tr>");
		sb.append("</table><br>");
		sb.append("</td></tr></table></body></html>");
	}

	private static void sendHtml(Npc npc, Player player, String html)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0);
		msg.setHtml(html);
		player.sendPacket(msg);
	}

	// ── Persistencia ──────────────────────────────────────────────────────────

	private static void loadSchemes()
	{
		final File f = new File(SCHEMES_FILE);
		if (!f.exists()) return;

		try (InputStream in = new FileInputStream(f))
		{
			final Properties props = new Properties();
			props.load(in);
			for (String key : props.stringPropertyNames())
			{
				final String val = props.getProperty(key, "").trim();
				final LinkedHashSet<String> set = new LinkedHashSet<>();
				if (!val.isEmpty())
				{
					for (String entry : val.split(","))
					{
						if (!entry.isBlank()) set.add(entry.trim());
					}
				}
				SCHEMES.put(key, set);
			}
			LOGGER.info("PremiumBuffer: " + SCHEMES.size() + " esquemas cargados.");
		}
		catch (Exception e)
		{
			LOGGER.warning("PremiumBuffer: Error cargando esquemas: " + e.getMessage());
		}
	}

	private static void saveSchemes()
	{
		try
		{
			final File f = new File(SCHEMES_FILE);
			f.getParentFile().mkdirs();
			final Properties props = new Properties();
			for (Map.Entry<String, LinkedHashSet<String>> entry : SCHEMES.entrySet())
			{
				props.setProperty(entry.getKey(), String.join(",", entry.getValue()));
			}
			try (FileOutputStream out = new FileOutputStream(f))
			{
				props.store(out, "PremiumBuffer Schemes - no editar manualmente");
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("PremiumBuffer: Error guardando esquemas: " + e.getMessage());
		}
	}

	// ── Configuracion ─────────────────────────────────────────────────────────

	private static void loadConfig()
	{
		final File f = new File("config/Custom/PremiumBuffer.ini");
		if (!f.exists())
		{
			LOGGER.warning("PremiumBuffer: config/Custom/PremiumBuffer.ini no encontrado, usando defaults.");
			return;
		}
		try (InputStream in = new FileInputStream(f))
		{
			final Properties p = new Properties();
			p.load(in);
			ENABLED       = Boolean.parseBoolean(p.getProperty("PremiumBuffer_Enabled",     "true").trim());
			NPC_ID        = Integer.parseInt(p.getProperty("PremiumBuffer_NpcId",           "50028").trim());
			MAX_SCHEMES   = Integer.parseInt(p.getProperty("PremiumBuffer_MaxSchemes",       "4").trim());
			ENCHANT_LOW   = Integer.parseInt(p.getProperty("PremiumBuffer_EnchantLow",       "15").trim());
			ENCHANT_HIGH  = Integer.parseInt(p.getProperty("PremiumBuffer_EnchantHigh",      "30").trim());
			COST_ITEM_ID  = Integer.parseInt(p.getProperty("PremiumBuffer_CostItemId",       "10639").trim());
			COST_CHAR     = Integer.parseInt(p.getProperty("PremiumBuffer_CostCharAmount",   "5").trim());
			COST_PET      = Integer.parseInt(p.getProperty("PremiumBuffer_CostPetAmount",    "3").trim());
		}
		catch (Exception e)
		{
			LOGGER.warning("PremiumBuffer: Error leyendo config: " + e.getMessage());
		}
	}

	// ── Utilidades ────────────────────────────────────────────────────────────

	private static int safeInt(String s, int def)
	{
		try { return Integer.parseInt(s); }
		catch (Exception e) { return def; }
	}

	private static String getItemName(int itemId)
	{
		if (itemId == 57) return "Adena";
		final org.l2jmobius.gameserver.model.item.ItemTemplate tpl =
			ItemData.getInstance().getTemplate(itemId);
		return (tpl != null) ? tpl.getName() : ("Item#" + itemId);
	}

	// ── Entry point ───────────────────────────────────────────────────────────

	public static void main(String[] args)
	{
		new PremiumBuffer();
	}
}
