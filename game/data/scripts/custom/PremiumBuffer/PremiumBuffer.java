/*
 * PremiumBuffer NPC - Buffer premium con enchant +15/+30 y sistema de esquemas.
 *
 * Skills cargados desde:
 *   game/data/PremiumBufferSkills15.xml  (todos los buffs al nivel +15)
 *   game/data/PremiumBufferSkills30.xml  (todos los buffs al nivel +30)
 *
 * Esquemas guardados en:
 *   game/config/Custom/PremiumBufferSchemes.properties
 *
 * Config:
 *   game/config/Custom/PremiumBuffer.ini
 *
 * HTML estilo identico al NPC SchemeBuffer (50008).
 */
package custom.PremiumBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
	private static int     NPC_ID               = 50028;
	private static final int SCHEME_BUFFER_NPC  = 50008;
	private static int     MAX_SCHEMES          = 4;
	private static int     ENCHANT_LOW          = 15;
	private static int     ENCHANT_HIGH         = 30;
	private static int     COST_ITEM_ID         = 10639;
	private static int     COST_CHAR            = 5;
	private static int     COST_PET             = 3;
	private static boolean ENABLED              = true;

	// ── Skills por categoria para cada nivel de enchant ──────────────────────
	// Clave de categoria → lista de {skillId, level}
	private static final Map<String, List<int[]>> SKILLS_15 = new LinkedHashMap<>();
	private static final Map<String, List<int[]>> SKILLS_30 = new LinkedHashMap<>();

	// Orden de categorias para mostrar (mismo que SchemeBuffer)
	private static final String[] CATEGORIES = { "Buffs", "Resist", "Songs", "Dances", "Chants", "Special" };
	// Nombres en español para los titulos
	private static final Map<String, String> CAT_LABEL = new LinkedHashMap<>();
	static
	{
		CAT_LABEL.put("Buffs",   "Buffs");
		CAT_LABEL.put("Resist",  "Resistencias");
		CAT_LABEL.put("Songs",   "Canciones");
		CAT_LABEL.put("Dances",  "Danzas");
		CAT_LABEL.put("Chants",  "Canticos");
		CAT_LABEL.put("Special", "Especiales");
	}

	// ── Esquemas por jugador ────────────────────────────────────────────────
	// Clave: "{playerObjId}_{schemeIdx}"  Valor: set de "skillId:level"
	private static final Map<String, LinkedHashSet<String>> SCHEMES = new ConcurrentHashMap<>();
	private static final String SCHEMES_FILE = "config/Custom/PremiumBufferSchemes.properties";

	// ── Constructor ────────────────────────────────────────────────────────────
	private PremiumBuffer()
	{
		loadConfig();
		loadSkillsXml("data/PremiumBufferSkills15.xml", SKILLS_15);
		loadSkillsXml("data/PremiumBufferSkills30.xml", SKILLS_30);
		loadSchemes();

		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
		addTalkId(SCHEME_BUFFER_NPC);

		int total15 = SKILLS_15.values().stream().mapToInt(List::size).sum();
		int total30 = SKILLS_30.values().stream().mapToInt(List::size).sum();
		LOGGER.info("PremiumBuffer: NPC=" + NPC_ID
			+ " | Skills +15=" + total15 + "  +30=" + total30
			+ " | MaxEsquemas=" + MAX_SCHEMES
			+ " | Costo=" + COST_CHAR + "x" + COST_ITEM_ID + "(char) "
			+ COST_PET + "x" + COST_ITEM_ID + "(pet)");
	}

	// ── Eventos NPC ────────────────────────────────────────────────────────────
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (!ENABLED) return null;
		sendHtml(npc, player, buildEnchantChoicePage(npc));
		return null;
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!ENABLED || player == null) return null;

		final String[] p = event.split("_");

		switch (p[0])
		{
			case "choice":
				sendHtml(npc, player, buildEnchantChoicePage(npc));
				break;

			case "enchant":
				if (p.length >= 2)
				{
					if (!checkPremium(npc, player)) break;
					final int enchant = safeInt(p[1], ENCHANT_LOW);
					sendHtml(npc, player, buildSchemeListPage(npc, player, enchant));
				}
				break;

			case "cat":
				// cat_{enchant}_{category}  → muestra grilla de skills de esa categoria para agregar
				if (p.length >= 3 && checkPremium(npc, player))
				{
					// No usamos navegacion por categoria individual; el editor muestra todo
					final int enchant = safeInt(p[1], ENCHANT_LOW);
					sendHtml(npc, player, buildSchemeListPage(npc, player, enchant));
				}
				break;

			case "edit":
				// edit_{schemeIdx}_{enchant}
				if (p.length >= 3 && checkPremium(npc, player))
				{
					final int idx     = safeInt(p[1], 1);
					final int enchant = safeInt(p[2], ENCHANT_LOW);
					sendHtml(npc, player, buildEditorPage(npc, player, idx, enchant));
				}
				break;

			case "editcat":
				// editcat_{schemeIdx}_{enchant}_{category}
				if (p.length >= 4 && checkPremium(npc, player))
				{
					final int idx      = safeInt(p[1], 1);
					final int enchant  = safeInt(p[2], ENCHANT_LOW);
					final String cat   = p[3];
					sendHtml(npc, player, buildEditorCategoryPage(npc, player, idx, enchant, cat));
				}
				break;

			case "toggle":
				// toggle_{schemeIdx}_{enchant}_{category}_{skillId}_{skillLevel}
				if (p.length >= 6 && checkPremium(npc, player))
				{
					final int    idx     = safeInt(p[1], 1);
					final int    enchant = safeInt(p[2], ENCHANT_LOW);
					final String cat     = p[3];
					final int    skillId = safeInt(p[4], 0);
					final int    skillLv = safeInt(p[5], 1);
					toggleSkill(player, idx, skillId, skillLv);
					sendHtml(npc, player, buildEditorCategoryPage(npc, player, idx, enchant, cat));
				}
				break;

			case "apply":
				// apply_{schemeIdx}_{enchant}_{target}
				if (p.length >= 4 && checkPremium(npc, player))
				{
					final int    idx     = safeInt(p[1], 1);
					final int    enchant = safeInt(p[2], ENCHANT_LOW);
					final String target  = p[3];
					applyScheme(npc, player, idx, enchant, target);
					sendHtml(npc, player, buildSchemeListPage(npc, player, enchant));
				}
				break;

			case "clear":
				// clear_{schemeIdx}_{enchant}
				if (p.length >= 3 && checkPremium(npc, player))
				{
					final int idx     = safeInt(p[1], 1);
					final int enchant = safeInt(p[2], ENCHANT_LOW);
					clearScheme(player, idx);
					sendHtml(npc, player, buildEditorPage(npc, player, idx, enchant));
				}
				break;

			case "heal":
				if (checkPremium(npc, player))
				{
					healPlayer(player);
					sendHtml(npc, player, buildEnchantChoicePage(npc));
				}
				break;
		}
		return null;
	}

	// ── Paginas HTML (estilo identico a 50008.htm) ─────────────────────────────

	/** Pagina inicial: eleccion de enchant (+15 o +30). */
	private String buildEnchantChoicePage(Npc npc)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		appendHeader(sb, "Buffer Premium", "Esquemas con Enchant");
		appendSeparator(sb);

		appendCategoryTitle(sb, "Selecciona el nivel");
		sb.append("<br1>");
		sb.append("<table width=270><tr>");
		sb.append("<td align=center>");
		appendBtn(sb, "Buffs +" + ENCHANT_LOW, "bypass -h Script PremiumBuffer enchant_" + ENCHANT_LOW, 128, 21);
		sb.append("</td><td align=center>");
		appendBtn(sb, "Buffs +" + ENCHANT_HIGH, "bypass -h Script PremiumBuffer enchant_" + ENCHANT_HIGH, 128, 21);
		sb.append("</td></tr></table><br1>");

		appendSeparator(sb);
		appendCategoryTitle(sb, "Otros Servicios");
		sb.append("<br1>");
		appendBtn(sb, "Curar (HP / MP / CP)", "bypass -h Script PremiumBuffer heal", 200, 21);
		sb.append("<br>");
		appendSeparator(sb);
		sb.append("</center></body></html>");
		return sb.toString();
	}

	/** Pagina de lista de esquemas para un nivel de enchant. */
	private String buildSchemeListPage(Npc npc, Player player, int enchant)
	{
		final long   balance  = player.getInventory().getInventoryItemCount(COST_ITEM_ID, -1);
		final String itemName = getItemName(COST_ITEM_ID);

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		appendHeader(sb, "Buffer Premium +" + enchant, "Balance: " + balance + " " + itemName);
		appendSeparator(sb);

		for (int i = 1; i <= MAX_SCHEMES; i++)
		{
			final int count = getScheme(player, i).size();
			appendCategoryTitle(sb, "Esquema " + i + " - " + count + " skills");
			sb.append("<br1>");

			// Botones aplicar
			final boolean canChar = balance >= COST_CHAR;
			final boolean canPet  = (player.getSummon() != null) && (balance >= COST_PET);

			sb.append("<table width=270><tr>");
			sb.append("<td align=center>");
			appendBtn(sb, "Aplicar Personaje (" + COST_CHAR + "x)",
				"bypass -h Script PremiumBuffer apply_" + i + "_" + enchant + "_char", 128, 21);
			sb.append("</td><td align=center>");
			appendBtn(sb, "Aplicar Pet (" + COST_PET + "x)",
				"bypass -h Script PremiumBuffer apply_" + i + "_" + enchant + "_pet", 128, 21);
			sb.append("</td></tr></table><br1>");

			appendBtn(sb, "Editar Esquema " + i,
				"bypass -h Script PremiumBuffer edit_" + i + "_" + enchant, 200, 21);
			sb.append("<br1>");
			appendSeparator(sb);
		}

		// Cambiar nivel de enchant
		final int other = (enchant == ENCHANT_LOW) ? ENCHANT_HIGH : ENCHANT_LOW;
		appendCategoryTitle(sb, "Cambiar nivel");
		sb.append("<br1>");
		sb.append("<table width=270><tr>");
		sb.append("<td align=center>");
		appendBtn(sb, "Ver +" + other,
			"bypass -h Script PremiumBuffer enchant_" + other, 128, 21);
		sb.append("</td><td align=center>");
		appendBtn(sb, "Volver",
			"bypass -h Script PremiumBuffer choice", 128, 21);
		sb.append("</td></tr></table><br>");
		appendSeparator(sb);
		sb.append("</center></body></html>");
		return sb.toString();
	}

	/** Pagina del editor: muestra botones de categoria para editar el esquema. */
	private String buildEditorPage(Npc npc, Player player, int schemeIdx, int enchant)
	{
		final Map<String, List<int[]>> skills = (enchant <= ENCHANT_LOW) ? SKILLS_15 : SKILLS_30;
		final int count = getScheme(player, schemeIdx).size();

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		appendHeader(sb, "Esquema " + schemeIdx + " +" + enchant,
			count + " skills seleccionados");
		appendSeparator(sb);

		appendCategoryTitle(sb, "Buffs por Categoria");
		sb.append("<br1>");
		sb.append("<table width=270>");

		// 2 botones por fila
		String[] cats = CATEGORIES;
		for (int i = 0; i < cats.length; i += 2)
		{
			sb.append("<tr>");
			sb.append("<td align=center>");
			appendBtn(sb, CAT_LABEL.getOrDefault(cats[i], cats[i]),
				"bypass -h Script PremiumBuffer editcat_" + schemeIdx + "_" + enchant + "_" + cats[i],
				128, 21);
			sb.append("</td>");
			if (i + 1 < cats.length)
			{
				sb.append("<td align=center>");
				appendBtn(sb, CAT_LABEL.getOrDefault(cats[i+1], cats[i+1]),
					"bypass -h Script PremiumBuffer editcat_" + schemeIdx + "_" + enchant + "_" + cats[i+1],
					128, 21);
				sb.append("</td>");
			}
			sb.append("</tr>");
		}
		sb.append("</table><br1>");
		appendSeparator(sb);

		appendCategoryTitle(sb, "Acciones");
		sb.append("<br1>");
		appendBtn(sb, "Limpiar Esquema " + schemeIdx,
			"bypass -h Script PremiumBuffer clear_" + schemeIdx + "_" + enchant, 200, 21);
		sb.append("<br1>");
		appendBtn(sb, "Volver a Esquemas +" + enchant,
			"bypass -h Script PremiumBuffer enchant_" + enchant, 200, 21);
		sb.append("<br>");
		appendSeparator(sb);
		sb.append("</center></body></html>");
		return sb.toString();
	}

	/** Pagina del editor por categoria: iconos de skills con toggle. */
	private String buildEditorCategoryPage(Npc npc, Player player, int schemeIdx, int enchant, String category)
	{
		final Map<String, List<int[]>> skillMap = (enchant <= ENCHANT_LOW) ? SKILLS_15 : SKILLS_30;
		final List<int[]> catSkills = skillMap.getOrDefault(category, new ArrayList<>());
		final LinkedHashSet<String> scheme = getScheme(player, schemeIdx);
		final String catLabel = CAT_LABEL.getOrDefault(category, category);

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		appendHeader(sb, catLabel + " +" + enchant,
			"Esquema " + schemeIdx + " - " + scheme.size() + " skills");
		appendSeparator(sb);

		// Grilla de iconos: 8 por fila, identica al SchemeBuffer
		sb.append("<table border=0 cellspacing=2 cellpadding=0 width=270>");
		int col = 0;
		for (int[] s : catSkills)
		{
			final int skillId  = s[0];
			final int skillLvl = s[1];
			final boolean sel  = scheme.contains(skillId + ":" + skillLvl);

			if (col == 0) sb.append("<tr>");

			final String overlay = sel
				? "back=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm_Down\""
				: "back=\"\" fore=\"\"";
			sb.append("<td width=32 height=32 background=\"icon.skill").append(skillId).append("\">");
			sb.append("<button value=\" \"")
				.append(" action=\"bypass -h Script PremiumBuffer toggle_")
				.append(schemeIdx).append("_").append(enchant).append("_")
				.append(category).append("_").append(skillId).append("_").append(skillLvl).append("\"")
				.append(" width=30 height=30 ").append(overlay).append(">");
			sb.append("</td>");

			col++;
			if (col >= 8) { sb.append("</tr>"); col = 0; }
		}
		if (col > 0) sb.append("</tr>");
		sb.append("</table><br1>");

		appendSeparator(sb);
		appendCategoryTitle(sb, "Navegacion");
		sb.append("<br1>");
		sb.append("<table width=270><tr>");
		sb.append("<td align=center>");
		appendBtn(sb, "Volver a Categorias",
			"bypass -h Script PremiumBuffer edit_" + schemeIdx + "_" + enchant, 128, 21);
		sb.append("</td><td align=center>");
		appendBtn(sb, "Volver a Esquemas",
			"bypass -h Script PremiumBuffer enchant_" + enchant, 128, 21);
		sb.append("</td></tr></table><br>");
		appendSeparator(sb);
		sb.append("</center></body></html>");
		return sb.toString();
	}

	// ── Helpers HTML estilo 50008 ───────────────────────────────────────────────

	private static void appendHeader(StringBuilder sb, String title, String subtitle)
	{
		sb.append("<table width=270 bgcolor=111111>");
		sb.append("<tr><td align=center height=6></td></tr>");
		sb.append("<tr><td align=center><font color=\"LEVEL\">").append(title).append("</font></td></tr>");
		sb.append("<tr><td align=center><font color=\"808080\">").append(subtitle).append("</font></td></tr>");
		sb.append("<tr><td align=center height=6></td></tr>");
		sb.append("</table>");
	}

	private static void appendSeparator(StringBuilder sb)
	{
		sb.append("<img src=\"L2UI.SquareGray\" width=270 height=1>");
	}

	private static void appendCategoryTitle(StringBuilder sb, String title)
	{
		sb.append("<table width=270>");
		sb.append("<tr><td align=center><font color=\"ffffa0\">").append(title).append("</font></td></tr>");
		sb.append("</table>");
	}

	private static void appendBtn(StringBuilder sb, String label, String action, int w, int h)
	{
		sb.append("<button value=\"").append(label).append("\"")
			.append(" action=\"").append(action).append("\"")
			.append(" width=").append(w).append(" height=").append(h)
			.append(" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
	}

	// ── Logica de esquemas ─────────────────────────────────────────────────────

	private static LinkedHashSet<String> getScheme(Player player, int idx)
	{
		return SCHEMES.computeIfAbsent(player.getObjectId() + "_" + idx, k -> new LinkedHashSet<>());
	}

	private static void toggleSkill(Player player, int schemeIdx, int skillId, int skillLvl)
	{
		final String key   = player.getObjectId() + "_" + schemeIdx;
		final String entry = skillId + ":" + skillLvl;
		final LinkedHashSet<String> scheme = SCHEMES.computeIfAbsent(key, k -> new LinkedHashSet<>());
		if (!scheme.remove(entry)) scheme.add(entry);
		saveSchemes();
	}

	private static void clearScheme(Player player, int schemeIdx)
	{
		SCHEMES.put(player.getObjectId() + "_" + schemeIdx, new LinkedHashSet<>());
		saveSchemes();
	}

	// ── Aplicacion de buffs ────────────────────────────────────────────────────

	private static void applyScheme(Npc npc, Player player, int schemeIdx, int enchant, String target)
	{
		final LinkedHashSet<String> scheme = getScheme(player, schemeIdx);
		if (scheme.isEmpty())
		{
			player.sendMessage("[Buffer Premium] El Esquema " + schemeIdx + " esta vacio. Editalo primero.");
			return;
		}

		final boolean forChar = target.equals("char");
		final boolean forPet  = target.equals("pet");
		final int cost        = forChar ? COST_CHAR : COST_PET;

		if (forPet && player.getSummon() == null)
		{
			player.sendMessage("[Buffer Premium] No tienes un pet activo.");
			return;
		}

		final long balance = player.getInventory().getInventoryItemCount(COST_ITEM_ID, -1);
		if (balance < cost)
		{
			player.sendMessage("[Buffer Premium] Necesitas " + cost + " x " + getItemName(COST_ITEM_ID)
				+ " (tienes " + balance + ").");
			return;
		}

		player.destroyItemByItemId(ItemProcessType.FEE, COST_ITEM_ID, cost, npc, true);

		final List<Creature> targets = new ArrayList<>();
		if (forChar) targets.add(player);
		if (forPet)  targets.add(player.getSummon());

		int applied = 0;
		for (String entry : scheme)
		{
			final String[] parts = entry.split(":");
			if (parts.length < 2) continue;
			final Skill skill = SkillData.getInstance().getSkill(
				Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
			if (skill == null) continue;
			for (Creature t : targets)
			{
				skill.applyEffects(player, t);
				player.sendPacket(new MagicSkillUse(player, t,
					skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
			}
			applied++;
		}

		final String tgtLabel = forChar ? "personaje" : "pet";
		player.sendMessage("[Buffer Premium] +" + enchant + " | Esquema " + schemeIdx
			+ " aplicado a " + tgtLabel + " (" + applied + " skills).");
	}

	// ── Heal ──────────────────────────────────────────────────────────────────

	private static void healPlayer(Player player)
	{
		player.setCurrentHp(player.getMaxHp());
		player.setCurrentMp(player.getMaxMp());
		player.setCurrentCp(player.getMaxCp());
		player.updateUserInfo();
		final Summon pet = player.getSummon();
		if (pet != null) { pet.setCurrentHp(pet.getMaxHp()); pet.setCurrentMp(pet.getMaxMp()); }
		player.sendMessage("[Buffer Premium] HP / MP / CP restaurados.");
	}

	// ── Check Premium ─────────────────────────────────────────────────────────

	private static boolean hasPremium(Player player)
	{
		return PremiumManager.getInstance().getPremiumExpiration(player.getAccountName()) > System.currentTimeMillis();
	}

	private static boolean checkPremium(Npc npc, Player player)
	{
		if (hasPremium(player)) return true;

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		appendHeader(sb, "Buffer Premium", "Acceso Denegado");
		appendSeparator(sb);
		appendCategoryTitle(sb, "Solo Premium");
		sb.append("<br1><font color=\"808080\">Necesitas estado Premium activo</font><br1>");
		sb.append("<font color=\"808080\">para usar el Buffer Premium.</font><br1>");
		appendSeparator(sb);
		sb.append("</center></body></html>");
		sendHtml(npc, player, sb.toString());
		return false;
	}

	private static void sendHtml(Npc npc, Player player, String html)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0);
		msg.setHtml(html);
		player.sendPacket(msg);
	}

	// ── Carga de XML ──────────────────────────────────────────────────────────

	private static void loadSkillsXml(String path, Map<String, List<int[]>> target)
	{
		target.clear();
		final File f = new File(path);
		if (!f.exists())
		{
			LOGGER.warning("PremiumBuffer: XML no encontrado: " + path);
			return;
		}
		try
		{
			final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
			doc.getDocumentElement().normalize();
			final NodeList cats = doc.getElementsByTagName("category");
			for (int i = 0; i < cats.getLength(); i++)
			{
				final Element cat  = (Element) cats.item(i);
				final String type  = cat.getAttribute("type");
				final List<int[]> list = new ArrayList<>();
				final NodeList buffs = cat.getElementsByTagName("buff");
				for (int j = 0; j < buffs.getLength(); j++)
				{
					final Element buff = (Element) buffs.item(j);
					final int id  = Integer.parseInt(buff.getAttribute("id"));
					final int lvl = Integer.parseInt(buff.getAttribute("level"));
					list.add(new int[]{id, lvl});
				}
				target.put(type, list);
			}
			LOGGER.info("PremiumBuffer: Cargado " + path + " ("
				+ target.values().stream().mapToInt(List::size).sum() + " skills)");
		}
		catch (Exception e)
		{
			LOGGER.warning("PremiumBuffer: Error leyendo " + path + ": " + e.getMessage());
		}
	}

	// ── Persistencia de esquemas ──────────────────────────────────────────────

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
					for (String e : val.split(","))
						if (!e.isBlank()) set.add(e.trim());
				SCHEMES.put(key, set);
			}
			LOGGER.info("PremiumBuffer: " + SCHEMES.size() + " esquemas cargados.");
		}
		catch (Exception e) { LOGGER.warning("PremiumBuffer: Error cargando esquemas: " + e.getMessage()); }
	}

	private static void saveSchemes()
	{
		try
		{
			final File f = new File(SCHEMES_FILE);
			f.getParentFile().mkdirs();
			final Properties props = new Properties();
			for (Map.Entry<String, LinkedHashSet<String>> e : SCHEMES.entrySet())
				props.setProperty(e.getKey(), String.join(",", e.getValue()));
			try (FileOutputStream out = new FileOutputStream(f))
			{
				props.store(out, "PremiumBuffer Schemes");
			}
		}
		catch (Exception e) { LOGGER.warning("PremiumBuffer: Error guardando esquemas: " + e.getMessage()); }
	}

	// ── Carga de config ────────────────────────────────────────────────────────

	private static void loadConfig()
	{
		final File f = new File("config/Custom/PremiumBuffer.ini");
		if (!f.exists()) return;
		try (InputStream in = new FileInputStream(f))
		{
			final Properties p = new Properties();
			p.load(in);
			ENABLED      = Boolean.parseBoolean(p.getProperty("PremiumBuffer_Enabled",      "true").trim());
			NPC_ID       = Integer.parseInt(p.getProperty("PremiumBuffer_NpcId",             "50028").trim());
			MAX_SCHEMES  = Integer.parseInt(p.getProperty("PremiumBuffer_MaxSchemes",         "4").trim());
			ENCHANT_LOW  = Integer.parseInt(p.getProperty("PremiumBuffer_EnchantLow",         "15").trim());
			ENCHANT_HIGH = Integer.parseInt(p.getProperty("PremiumBuffer_EnchantHigh",        "30").trim());
			COST_ITEM_ID = Integer.parseInt(p.getProperty("PremiumBuffer_CostItemId",         "10639").trim());
			COST_CHAR    = Integer.parseInt(p.getProperty("PremiumBuffer_CostCharAmount",     "5").trim());
			COST_PET     = Integer.parseInt(p.getProperty("PremiumBuffer_CostPetAmount",      "3").trim());
		}
		catch (Exception e) { LOGGER.warning("PremiumBuffer: Error leyendo config: " + e.getMessage()); }
	}

	// ── Utilidades ────────────────────────────────────────────────────────────

	private static int safeInt(String s, int def)
	{
		try { return Integer.parseInt(s); } catch (Exception e) { return def; }
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
