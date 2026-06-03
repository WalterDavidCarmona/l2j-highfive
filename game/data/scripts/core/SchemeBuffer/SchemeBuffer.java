package org.l2jmobius.gameserver.model.actor.instance;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.SchemeBufferConfig;
import org.l2jmobius.gameserver.data.SchemeBufferTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.managers.PremiumManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.holders.npc.BuffSkillHolder;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.HtmlUtil;

public class SchemeBuffer extends Npc
{
	private static final int    PAGE_LIMIT           = 6;
	private static final int    SCHEME_NAME_MAX_LENGTH = 14;
	private static final int    TYPES_PER_ROW        = 4;
	private static final int    UI_WIDTH             = 256;
	private static final int    UI_HALF_WIDTH        = 128;
	private static final int    ICON_COL_WIDTH       = 40;
	private static final int    BTN_COL_WIDTH        = 32;
	private static final int    NAME_COL_WIDTH       = 184;
	private static final int    FOOTER_SIDE_WIDTH    = 70;
	private static final int    PAGE_CENTER_WIDTH    = 116;

	/** Nombre exacto de la categoria premium en SchemeBufferSkills.xml */
	private static final String PREMIUM_CATEGORY     = "Premium";

	public SchemeBuffer(NpcTemplate template)
	{
		super(template);
	}

	// ── Verificacion premium ─────────────────────────────────────────────────

	private static boolean hasPremium(Player player)
	{
		return PremiumManager.getInstance().getPremiumExpiration(player.getAccountName()) > System.currentTimeMillis();
	}

	// ── Bypass handler ────────────────────────────────────────────────────────

	@Override
	public void onBypassFeedback(Player player, String commandValue)
	{
		if (player == null || commandValue == null || commandValue.isEmpty())
		{
			return;
		}

		SchemeBufferTable schemeBufferTable = SchemeBufferTable.getInstance();
		SkillData skillData = SkillData.getInstance();
		StringTokenizer tokenizer = new StringTokenizer(commandValue.replace("createscheme ", "createscheme;"), ";");

		if (!tokenizer.hasMoreTokens())
		{
			return;
		}

		String currentCommand = tokenizer.nextToken();

		if (currentCommand.startsWith("menu"))
		{
			showMainMenu(player);
		}
		else if (currentCommand.startsWith("cleanup"))
		{
			player.stopAllEffects();
			Summon summon = player.getSummon();
			if (summon != null)
			{
				summon.stopAllEffects();
			}
			showMainMenu(player);
		}
		else if (currentCommand.startsWith("heal"))
		{
			player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			Summon summon = player.getSummon();
			if (summon != null)
			{
				summon.setCurrentHpMp(summon.getMaxHp(), summon.getMaxMp());
			}
			showMainMenu(player);
		}
		else if (currentCommand.startsWith("support"))
		{
			showGiveBuffsWindow(player);
		}
		else if (currentCommand.startsWith("givebuffs"))
		{
			if (!tokenizer.hasMoreTokens())
			{
				return;
			}

			String schemeName = tokenizer.nextToken();
			if (tokenizer.hasMoreTokens())
			{
				tokenizer.nextToken(); // consume cost token
			}

			Map<String, List<Integer>> playerSchemes = schemeBufferTable.getPlayerSchemes(player.getObjectId());
			if (playerSchemes == null || !playerSchemes.containsKey(schemeName))
			{
				player.sendMessage("Invalid scheme name: " + schemeName + ".");
				showGiveBuffsWindow(player);
				return;
			}

			List<Integer> scheme = schemeBufferTable.getScheme(player.getObjectId(), schemeName);
			int cost = getFee(scheme);
			Creature target = player;
			if (tokenizer.hasMoreTokens() && "pet".equalsIgnoreCase(tokenizer.nextToken()))
			{
				target = player.getSummon();
			}

			if (target == null)
			{
				player.sendMessage("You don't have a pet.");
				return;
			}

			if (cost == 0
				|| (SchemeBufferConfig.BUFFER_ITEM_ID == 57 && player.reduceAdena(ItemProcessType.FEE, (long) cost, this, true))
				|| (SchemeBufferConfig.BUFFER_ITEM_ID != 57 && player.destroyItemByItemId(ItemProcessType.FEE, SchemeBufferConfig.BUFFER_ITEM_ID, (long) cost, player, true)))
			{
				for (int skillId : scheme)
				{
					BuffSkillHolder holder = schemeBufferTable.getAvailableBuff(skillId);
					if (holder == null)
					{
						continue;
					}

					// ── Verificacion categoria Premium ──────────────────────
					// Si el skill pertenece a la categoria Premium,
					// solo aplica si el jugador tiene premium activo.
					if (PREMIUM_CATEGORY.equalsIgnoreCase(holder.getType()) && !hasPremium(player))
					{
						continue;
					}

					Skill skill = skillData.getSkill(skillId, holder.getLevel());
					if (skill == null)
					{
						continue;
					}

					// ── Proteccion anti-pisado por enchant inferior ──────────
					// Si el target ya tiene este skill activo en un nivel IGUAL
					// o SUPERIOR al que se va a aplicar, se omite para no bajar
					// el buff encantado con uno sin encantar.
					BuffInfo existing = target.getEffectList().getBuffInfoBySkillId(skillId);
					if (existing != null && existing.getSkill().getLevel() >= skill.getLevel())
					{
						continue;
					}

					skill.applyEffects(this, target);
				}
			}
		}
		else if (currentCommand.startsWith("editschemes"))
		{
			if (tokenizer.countTokens() < 3)
			{
				return;
			}

			String groupType   = tokenizer.nextToken();
			String schemeName  = tokenizer.nextToken();
			Integer page       = parseUnsignedInt(tokenizer.nextToken());
			if (page == null)
			{
				return;
			}

			// Bloquear edicion de categoria Premium a no-premium
			if (PREMIUM_CATEGORY.equalsIgnoreCase(groupType) && !hasPremium(player))
			{
				player.sendMessage("La categoria Premium es exclusiva para jugadores con Premium activo.");
				showGiveBuffsWindow(player);
				return;
			}

			showEditSchemeWindow(player, groupType, schemeName, page);
		}
		else if (currentCommand.startsWith("skill"))
		{
			if (tokenizer.countTokens() < 4)
			{
				return;
			}

			String groupType   = tokenizer.nextToken();
			String schemeName  = tokenizer.nextToken();
			Integer skillId    = parseUnsignedInt(tokenizer.nextToken());
			Integer page       = parseUnsignedInt(tokenizer.nextToken());

			if (skillId == null || page == null)
			{
				return;
			}

			// Bloquear seleccion de skills Premium a no-premium
			if (PREMIUM_CATEGORY.equalsIgnoreCase(groupType) && !hasPremium(player))
			{
				player.sendMessage("La categoria Premium es exclusiva para jugadores con Premium activo.");
				showGiveBuffsWindow(player);
				return;
			}

			Map<String, List<Integer>> playerSchemes = schemeBufferTable.getPlayerSchemes(player.getObjectId());
			if (playerSchemes == null || !playerSchemes.containsKey(schemeName))
			{
				player.sendMessage("Invalid scheme name: " + schemeName + ".");
				showGiveBuffsWindow(player);
				return;
			}

			List<Integer> schemeSkills = schemeBufferTable.getScheme(player.getObjectId(), schemeName);

			if (currentCommand.startsWith("skillselect") && !schemeName.equalsIgnoreCase("none"))
			{
				Skill skill = skillData.getSkill(skillId, 1);
				if (skill == null)
				{
					showEditSchemeWindow(player, groupType, schemeName, page);
					return;
				}

				if (skill.isDance())
				{
					if (getCountOf(schemeSkills, true) < PlayerConfig.DANCES_MAX_AMOUNT)
					{
						if (!schemeSkills.contains(skillId))
						{
							schemeBufferTable.addSkillToScheme(player.getObjectId(), schemeName, skillId);
						}
					}
					else
					{
						player.sendMessage("This scheme has reached the maximum amount of dances/songs.");
					}
				}
				else if (getCountOf(schemeSkills, false) < player.getStat().getMaxBuffCount())
				{
					if (!schemeSkills.contains(skillId))
					{
						schemeBufferTable.addSkillToScheme(player.getObjectId(), schemeName, skillId);
					}
				}
				else
				{
					player.sendMessage("This scheme has reached the maximum amount of buffs.");
				}
			}
			else if (currentCommand.startsWith("skillunselect"))
			{
				schemeBufferTable.removeSkillFromScheme(player.getObjectId(), schemeName, skillId);
			}

			showEditSchemeWindow(player, groupType, schemeName, page);
		}
		else if (currentCommand.startsWith("createscheme"))
		{
			if (!tokenizer.hasMoreTokens())
			{
				player.sendMessage("Scheme's name must contain up to 14 chars.");
				return;
			}

			String schemeName = tokenizer.nextToken().trim();
			if (schemeName.isEmpty() || schemeName.length() > 14)
			{
				player.sendMessage("Scheme's name must contain up to 14 chars.");
				return;
			}

			String normalized = schemeName.replace(" ", "").replace(".", "").replace(",", "").replace("-", "").replace("+", "").replace("!", "").replace("?", "");
			if (!StringUtil.isAlphaNumeric(normalized))
			{
				player.sendMessage("Please use plain alphanumeric characters.");
				return;
			}

			Map<String, List<Integer>> playerSchemes = schemeBufferTable.getPlayerSchemes(player.getObjectId());
			if (playerSchemes != null)
			{
				if (playerSchemes.size() >= SchemeBufferConfig.BUFFER_MAX_SCHEMES)
				{
					player.sendMessage("Maximum schemes amount is already reached.");
					return;
				}

				if (playerSchemes.containsKey(schemeName))
				{
					player.sendMessage("The scheme name already exists: " + schemeName + ".");
					return;
				}
			}

			schemeBufferTable.setScheme(player.getObjectId(), schemeName, new ArrayList<>());
			showGiveBuffsWindow(player);
		}
		else if (currentCommand.startsWith("deletescheme"))
		{
			if (!tokenizer.hasMoreTokens())
			{
				player.sendMessage("This scheme name is invalid.");
				showGiveBuffsWindow(player);
				return;
			}

			String schemeName = tokenizer.nextToken();
			schemeBufferTable.deleteScheme(player.getObjectId(), schemeName);
			showGiveBuffsWindow(player);
		}
		else if (currentCommand.startsWith("manual"))
		{
			String category = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "Buffs";

			// ── Bloquear categoria Premium a no-premium ──────────────────
			if (PREMIUM_CATEGORY.equalsIgnoreCase(category) && !hasPremium(player))
			{
				player.sendMessage("La categoria Premium es exclusiva para jugadores con Premium activo.");
				showMainMenu(player);
				return;
			}

			int page = 1;
			if (tokenizer.hasMoreTokens())
			{
				Integer parsed = parseUnsignedInt(tokenizer.nextToken());
				if (parsed == null)
				{
					showMainMenu(player);
					return;
				}
				page = parsed;
			}

			String targetType = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "me";
			showManualWindow(player, category, page, targetType);
		}
		else if (currentCommand.startsWith("castbuff"))
		{
			if (tokenizer.countTokens() < 4)
			{
				return;
			}

			Integer skillId    = parseUnsignedInt(tokenizer.nextToken());
			String category    = tokenizer.nextToken();
			Integer page       = parseUnsignedInt(tokenizer.nextToken());
			String targetType  = tokenizer.nextToken();

			if (skillId == null || page == null)
			{
				return;
			}

			// Bloquear castbuff Premium a no-premium
			if (PREMIUM_CATEGORY.equalsIgnoreCase(category) && !hasPremium(player))
			{
				player.sendMessage("La categoria Premium es exclusiva para jugadores con Premium activo.");
				showMainMenu(player);
				return;
			}

			Creature target = "pet".equalsIgnoreCase(targetType) ? player.getSummon() : player;
			if (target == null)
			{
				player.sendMessage("You don't have a pet.");
				showManualWindow(player, category, page, targetType);
				return;
			}

			// Para Premium: el bypass incluye el level especifico como 6to token
			// para distinguir entre multiples entries del mismo skill ID (ej: guerrero vs mago).
			Skill skill = null;
			int targetLevel = -1;
			if (PREMIUM_CATEGORY.equalsIgnoreCase(category) && tokenizer.hasMoreTokens())
			{
				Integer specificLevel = parseUnsignedInt(tokenizer.nextToken());
				if (specificLevel != null)
				{
					targetLevel = specificLevel;
					skill = skillData.getSkill(skillId, specificLevel);
					// Si el nivel premium no existe en SkillData, buscar el nivel mas alto disponible
					if (skill == null)
					{
						for (int lvl = specificLevel - 1; lvl >= 1; lvl--)
						{
							skill = skillData.getSkill(skillId, lvl);
							if (skill != null)
							{
								break;
							}
						}
					}
				}
			}

			if (skill == null)
			{
				// Fallback al holder normal para categorias no-Premium o si no hay nivel especifico
				BuffSkillHolder holder = schemeBufferTable.getAvailableBuff(category, skillId);
				if (holder != null)
				{
					skill = skillData.getSkill(skillId, holder.getLevel());
					if (skill == null)
					{
						skill = skillData.getSkill(skillId, 1);
					}
				}
			}

			if (skill != null)
			{
				// Proteccion anti-pisado: no sobreescribir enchant superior
				BuffInfo existing = target.getEffectList().getBuffInfoBySkillId(skillId);
				if (existing == null || existing.getSkill().getLevel() < skill.getLevel())
				{
					skill.applyEffects(this, target);
				}
			}

			showManualWindow(player, category, page, targetType);
		}
		else if (currentCommand.startsWith("autobuff"))
		{
			String targetType = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "me";
			applyAutoBuff(player, targetType);
		}
		else if (currentCommand.startsWith("reloadskills"))
		{
			// ── Recarga de SchemeBufferSkills.xml en tiempo real ──────────
			// Solo disponible para administradores (accessLevel >= 100).
			if (!player.isGM())
			{
				player.sendMessage("No tienes permisos para recargar los skills del buffer.");
				showMainMenu(player);
				return;
			}

			SchemeBufferTable.getInstance().reloadAvailableBuffs();
			player.sendMessage("[SchemeBuffer] Skills recargados desde SchemeBufferSkills.xml.");
			showMainMenu(player);
		}
		else
		{
			super.onBypassFeedback(player, commandValue);
		}
	}

	// ── HTML path ─────────────────────────────────────────────────────────────

	@Override
	public String getHtmlPath(int npcId, int value)
	{
		String suffix = value == 0 ? Integer.toString(npcId) : npcId + "-" + value;
		return "data/html/mods/SchemeBuffer/" + suffix + ".htm";
	}

	// ── Ventanas ──────────────────────────────────────────────────────────────

	private void showMainMenu(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, getHtmlPath(getId(), 0));
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}

	private void showGiveBuffsWindow(Player player)
	{
		StringBuilder htmlBuilder = new StringBuilder(200);
		Map<String, List<Integer>> playerSchemes = SchemeBufferTable.getInstance().getPlayerSchemes(player.getObjectId());

		if (playerSchemes != null && !playerSchemes.isEmpty())
		{
			for (Map.Entry<String, List<Integer>> scheme : playerSchemes.entrySet())
			{
				int cost = getFee(scheme.getValue());
				htmlBuilder.append("<font color=\"LEVEL\">").append(scheme.getKey())
					.append(" [").append(scheme.getValue().size()).append(" skill(s)]")
					.append(cost > 0 ? " - cost: " + NumberFormat.getInstance(Locale.ENGLISH).format((long) cost) : "")
					.append("</font><br1>");
				htmlBuilder.append("<a action=\"bypass -h npc_%objectId%_givebuffs;").append(scheme.getKey()).append(";").append(cost).append("\">Use on Me</a>&nbsp;|&nbsp;");
				htmlBuilder.append("<a action=\"bypass -h npc_%objectId%_givebuffs;").append(scheme.getKey()).append(";").append(cost).append(";pet\">Use on Pet</a>&nbsp;|&nbsp;");
				htmlBuilder.append("<a action=\"bypass -h npc_%objectId%_editschemes;Buffs;").append(scheme.getKey()).append(";1\">Edit</a>&nbsp;|&nbsp;");
				htmlBuilder.append("<a action=\"bypass -h npc_%objectId%_deletescheme;").append(scheme.getKey()).append("\">Delete</a><br>");
			}
		}
		else
		{
			htmlBuilder.append("<font color=\"LEVEL\">You haven't defined any scheme.</font>");
		}

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, getHtmlPath(getId(), 1));
		html.replace("%schemes%", htmlBuilder.toString());
		html.replace("%max_schemes%", SchemeBufferConfig.BUFFER_MAX_SCHEMES);
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}

	private void showEditSchemeWindow(Player player, String groupType, String schemeName, int page)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		List<Integer> schemeSkills = SchemeBufferTable.getInstance().getScheme(player.getObjectId(), schemeName);
		html.setFile(player, getHtmlPath(getId(), 2));
		html.replace("%schemename%", schemeName);
		int buffCount = getCountOf(schemeSkills, false);
		html.replace("%count%", buffCount + " / " + player.getStat().getMaxBuffCount() + " buffs, " + getCountOf(schemeSkills, true) + " / " + PlayerConfig.DANCES_MAX_AMOUNT + " dances/songs");
		html.replace("%typesframe%", getTypesFrame(groupType, schemeName, player));
		html.replace("%skilllistframe%", getGroupSkillList(player, groupType, schemeName, page));
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}

	private void showManualWindow(Player player, String category, int pageValue, String targetType)
	{
		SchemeBufferTable schemeBufferTable = SchemeBufferTable.getInstance();
		SkillData skillData = SkillData.getInstance();

		// Premium: usa la lista completa que permite duplicados de skill ID
		if (PREMIUM_CATEGORY.equalsIgnoreCase(category))
		{
			showPremiumManualWindow(player, pageValue, targetType);
			return;
		}

		List<Integer> skillIds = schemeBufferTable.getSkillsIdsByType(category);

		if (skillIds.isEmpty())
		{
			player.sendMessage("That category doesn't contain any skills.");
			return;
		}

		int maxPage = HtmlUtil.countPageNumber(skillIds.size(), 6);
		int page = Math.max(1, Math.min(pageValue, maxPage));

		skillIds = skillIds.subList((page - 1) * 6, Math.min(page * 6, skillIds.size()));
		StringBuilder htmlBuilder = new StringBuilder(skillIds.size() * 200);

		htmlBuilder.append("<table width=\"").append(UI_WIDTH).append("\"><tr>");
		htmlBuilder.append("<td width=\"").append(UI_HALF_WIDTH).append("\" align=\"center\">");
		if ("me".equalsIgnoreCase(targetType))
		{
			htmlBuilder.append("<font color=\"LEVEL\">Me</font>");
		}
		else
		{
			htmlBuilder.append("<a action=\"bypass -h npc_").append(getObjectId()).append("_manual;").append(category).append(";").append(page).append(";me\">Me</a>");
		}
		htmlBuilder.append("</td>");

		htmlBuilder.append("<td width=\"").append(UI_HALF_WIDTH).append("\" align=\"center\">");
		if ("pet".equalsIgnoreCase(targetType))
		{
			htmlBuilder.append("<font color=\"LEVEL\">Pet</font>");
		}
		else
		{
			htmlBuilder.append("<a action=\"bypass -h npc_").append(getObjectId()).append("_manual;").append(category).append(";").append(page).append(";pet\">Pet</a>");
		}
		htmlBuilder.append("</td>");
		htmlBuilder.append("</tr></table><br1>");

		int row = 0;
		for (int skillId : skillIds)
		{
			BuffSkillHolder holder = schemeBufferTable.getAvailableBuff(category, skillId);
			if (holder == null)
			{
				continue;
			}

			Skill skill = skillData.getSkill(skillId, holder.getLevel());
			if (skill == null)
			{
				continue;
			}

			htmlBuilder.append(row % 2 == 0 ? "<table width=\"256\" bgcolor=\"000000\"><tr>" : "<table width=\"256\"><tr>");
			htmlBuilder.append("<td height=40 width=").append(ICON_COL_WIDTH).append(" align=center><img src=\"").append(skill.getIcon()).append("\" width=32 height=32></td>");
			htmlBuilder.append("<td width=").append(NAME_COL_WIDTH).append(">").append(skill.getName()).append("<br1>");
			htmlBuilder.append("<font color=\"B09878\">").append(holder.getDescription()).append("</font></td>");
			htmlBuilder.append("<td width=").append(BTN_COL_WIDTH).append(" align=center><button action=\"bypass -h npc_")
				.append(getObjectId()).append("_castbuff;").append(skillId).append(";").append(category).append(";").append(page).append(";").append(targetType)
				.append("\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomin2\" fore=\"L2UI_CH3.mapbutton_zoomin1\"></td>");
			htmlBuilder.append("</tr></table>");
			htmlBuilder.append("<img src=\"L2UI.SquareGray\" width=").append(UI_WIDTH).append(" height=1>");
			row++;
		}

		// Paginacion
		htmlBuilder.append("<br1><img src=\"L2UI.SquareGray\" width=").append(UI_WIDTH).append(" height=1>")
			.append("<table width=\"").append(UI_WIDTH).append("\" bgcolor=000000><tr>");
		if (page > 1)
		{
			htmlBuilder.append("<td align=left width=").append(FOOTER_SIDE_WIDTH).append("><a action=\"bypass -h npc_").append(getObjectId()).append("_manual;").append(category).append(";").append(page - 1).append(";").append(targetType).append("\">Previous</a></td>");
		}
		else
		{
			htmlBuilder.append("<td align=left width=").append(FOOTER_SIDE_WIDTH).append(">Previous</td>");
		}

		htmlBuilder.append("<td align=center width=").append(PAGE_CENTER_WIDTH).append(">Page ").append(page).append("</td>");

		if (page < maxPage)
		{
			htmlBuilder.append("<td align=right width=").append(FOOTER_SIDE_WIDTH).append("><a action=\"bypass -h npc_").append(getObjectId()).append("_manual;").append(category).append(";").append(page + 1).append(";").append(targetType).append("\">Next</a></td>");
		}
		else
		{
			htmlBuilder.append("<td align=right width=").append(FOOTER_SIDE_WIDTH).append(">Next</td>");
		}

		htmlBuilder.append("</tr></table><img src=\"L2UI.SquareGray\" width=").append(UI_WIDTH).append(" height=1>");
		htmlBuilder.append("<br1><center><a action=\"bypass -h npc_").append(getObjectId()).append("_menu\">Back</a></center>");

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, getHtmlPath(getId(), 3));
		html.replace("%category%", category);
		html.replace("%skills%", htmlBuilder.toString());
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}

	// ── Ventana manual Premium (permite duplicados de skill ID) ──────────────

	private void showPremiumManualWindow(Player player, int pageValue, String targetType)
	{
		final SchemeBufferTable schemeBufferTable = SchemeBufferTable.getInstance();
		final SkillData skillData = SkillData.getInstance();
		final List<BuffSkillHolder> allEntries = schemeBufferTable.getPremiumBuffsList();

		if (allEntries.isEmpty())
		{
			player.sendMessage("La categoria Premium no tiene skills configurados.");
			return;
		}

		final int maxPage = HtmlUtil.countPageNumber(allEntries.size(), PAGE_LIMIT);
		final int page    = Math.max(1, Math.min(pageValue, maxPage));
		final List<BuffSkillHolder> pageEntries = allEntries.subList((page - 1) * PAGE_LIMIT, Math.min(page * PAGE_LIMIT, allEntries.size()));

		final StringBuilder html = new StringBuilder(pageEntries.size() * 200);

		// Selector Me / Pet
		html.append("<table width=\"").append(UI_WIDTH).append("\"><tr>");
		html.append("<td width=\"").append(UI_HALF_WIDTH).append("\" align=\"center\">");
		if ("me".equalsIgnoreCase(targetType))
		{
			html.append("<font color=\"LEVEL\">Me</font>");
		}
		else
		{
			html.append("<a action=\"bypass -h npc_").append(getObjectId()).append("_manual;").append(PREMIUM_CATEGORY).append(";").append(page).append(";me\">Me</a>");
		}
		html.append("</td>");
		html.append("<td width=\"").append(UI_HALF_WIDTH).append("\" align=\"center\">");
		if ("pet".equalsIgnoreCase(targetType))
		{
			html.append("<font color=\"LEVEL\">Pet</font>");
		}
		else
		{
			html.append("<a action=\"bypass -h npc_").append(getObjectId()).append("_manual;").append(PREMIUM_CATEGORY).append(";").append(page).append(";pet\">Pet</a>");
		}
		html.append("</td>");
		html.append("</tr></table><br1>");

		int row = 0;
		for (BuffSkillHolder holder : pageEntries)
		{
			// Intentar obtener el skill al nivel premium configurado.
			// Si ese nivel no existe en SkillData (ej: level 130 custom), usar nivel 1
			// unicamente para obtener icono y nombre para mostrar en pantalla.
			// El bypass siempre usa holder.getLevel() para aplicar el nivel correcto.
			Skill skill = skillData.getSkill(holder.getId(), holder.getLevel());
			if (skill == null)
			{
				skill = skillData.getSkill(holder.getId(), 1);
			}
			if (skill == null)
			{
				continue;
			}

			html.append(row % 2 == 0 ? "<table width=\"256\" bgcolor=\"000000\"><tr>" : "<table width=\"256\"><tr>");
			html.append("<td height=40 width=").append(ICON_COL_WIDTH).append(" align=center>")
				.append("<img src=\"").append(skill.getIcon()).append("\" width=32 height=32></td>");
			html.append("<td width=").append(NAME_COL_WIDTH).append(">").append(skill.getName()).append("<br1>")
				.append("<font color=\"B09878\">").append(holder.getDescription()).append("</font></td>");
			// Bypass incluye level especifico como 6to parametro (novedad para Premium)
			html.append("<td width=").append(BTN_COL_WIDTH).append(" align=center>")
				.append("<button action=\"bypass -h npc_").append(getObjectId())
				.append("_castbuff;").append(holder.getId())
				.append(";").append(PREMIUM_CATEGORY)
				.append(";").append(page)
				.append(";").append(targetType)
				.append(";").append(holder.getLevel())   // <-- level especifico
				.append("\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomin2\" fore=\"L2UI_CH3.mapbutton_zoomin1\"></td>");
			html.append("</tr></table>");
			html.append("<img src=\"L2UI.SquareGray\" width=").append(UI_WIDTH).append(" height=1>");
			row++;
		}

		// Paginacion
		html.append("<br1><img src=\"L2UI.SquareGray\" width=").append(UI_WIDTH).append(" height=1>")
			.append("<table width=\"").append(UI_WIDTH).append("\" bgcolor=000000><tr>");
		if (page > 1)
		{
			html.append("<td align=left width=").append(FOOTER_SIDE_WIDTH)
				.append("><a action=\"bypass -h npc_").append(getObjectId())
				.append("_manual;").append(PREMIUM_CATEGORY).append(";").append(page - 1).append(";").append(targetType)
				.append("\">Previous</a></td>");
		}
		else
		{
			html.append("<td align=left width=").append(FOOTER_SIDE_WIDTH).append(">Previous</td>");
		}

		html.append("<td align=center width=").append(PAGE_CENTER_WIDTH).append(">Page ").append(page).append("</td>");

		if (page < maxPage)
		{
			html.append("<td align=right width=").append(FOOTER_SIDE_WIDTH)
				.append("><a action=\"bypass -h npc_").append(getObjectId())
				.append("_manual;").append(PREMIUM_CATEGORY).append(";").append(page + 1).append(";").append(targetType)
				.append("\">Next</a></td>");
		}
		else
		{
			html.append("<td align=right width=").append(FOOTER_SIDE_WIDTH).append(">Next</td>");
		}

		html.append("</tr></table><img src=\"L2UI.SquareGray\" width=").append(UI_WIDTH).append(" height=1>");
		html.append("<br1><center><a action=\"bypass -h npc_").append(getObjectId()).append("_menu\">Back</a></center>");

		final NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
		msg.setFile(player, getHtmlPath(getId(), 3));
		msg.replace("%category%", PREMIUM_CATEGORY);
		msg.replace("%skills%", html.toString());
		msg.replace("%objectId%", getObjectId());
		player.sendPacket(msg);
	}

	// ── Auto buff ─────────────────────────────────────────────────────────────

	private void applyAutoBuff(Player player, String targetType)
	{
		SchemeBufferTable schemeBufferTable = SchemeBufferTable.getInstance();
		SkillData skillData = SkillData.getInstance();
		Creature target = "pet".equalsIgnoreCase(targetType) ? player.getSummon() : player;

		if (target == null)
		{
			player.sendMessage("You don't have a pet.");
			showMainMenu(player);
			return;
		}

		String category = player.isMageClass() ? "MAGE_GROUP" : "FIGHTER_GROUP";
		List<Integer> skillIds = schemeBufferTable.getSkillsIdsByType(category);

		if (skillIds.isEmpty())
		{
			player.sendMessage("Auto buff configuration is not available.");
			showMainMenu(player);
			return;
		}

		for (int skillId : skillIds)
		{
			BuffSkillHolder holder = schemeBufferTable.getAvailableBuff(category, skillId);
			if (holder == null)
			{
				continue;
			}

			Skill skill = skillData.getSkill(skillId, holder.getLevel());
			if (skill == null)
			{
				continue;
			}

			// Proteccion anti-pisado en auto buff tambien
			BuffInfo existing = target.getEffectList().getBuffInfoBySkillId(skillId);
			if (existing != null && existing.getSkill().getLevel() >= skill.getLevel())
			{
				continue;
			}

			skill.applyEffects(this, target);
		}

		player.sendMessage("Auto buff applied successfully!");
		showMainMenu(player);
	}

	// ── Generadores de HTML ───────────────────────────────────────────────────

	private String getGroupSkillList(Player player, String groupType, String schemeName, int pageValue)
	{
		// Premium: usa _premiumBuffsList para mostrar duplicados de skill ID
		if (PREMIUM_CATEGORY.equalsIgnoreCase(groupType))
		{
			return getGroupSkillListPremium(player, schemeName, pageValue);
		}

		SchemeBufferTable schemeBufferTable = SchemeBufferTable.getInstance();
		SkillData skillData = SkillData.getInstance();
		List<Integer> skillIds = schemeBufferTable.getSkillsIdsByType(groupType);

		if (skillIds.isEmpty())
		{
			return "That group doesn't contain any skills.";
		}

		int maxPage = HtmlUtil.countPageNumber(skillIds.size(), 6);
		int page = Math.max(1, Math.min(pageValue, maxPage));

		skillIds = skillIds.subList((page - 1) * 6, Math.min(page * 6, skillIds.size()));
		List<Integer> schemeSkills = schemeBufferTable.getScheme(player.getObjectId(), schemeName);
		StringBuilder htmlBuilder = new StringBuilder(skillIds.size() * 150);
		int row = 0;

		for (int skillId : skillIds)
		{
			BuffSkillHolder holder = schemeBufferTable.getAvailableBuff(groupType, skillId);
			if (holder == null)
			{
				continue;
			}

			Skill skill = skillData.getSkill(skillId, 1);
			if (skill == null)
			{
				continue;
			}

			htmlBuilder.append(row % 2 == 0 ? "<table width=\"256\" bgcolor=\"000000\"><tr>" : "<table width=\"256\"><tr>");
			htmlBuilder.append("<td height=40 width=").append(ICON_COL_WIDTH).append(" align=center><img src=\"").append(skill.getIcon()).append("\" width=32 height=32></td>");
			htmlBuilder.append("<td width=").append(NAME_COL_WIDTH).append(">").append(skill.getName()).append("<br1><font color=\"B09878\">").append(holder.getDescription()).append("</font></td>");

			if (schemeSkills.contains(skillId))
			{
				htmlBuilder.append("<td width=").append(BTN_COL_WIDTH).append(" align=center><button action=\"bypass -h npc_%objectId%_skillunselect;")
					.append(groupType).append(";").append(schemeName).append(";").append(skillId).append(";").append(page)
					.append("\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomout2\" fore=\"L2UI_CH3.mapbutton_zoomout1\"></td>");
			}
			else
			{
				htmlBuilder.append("<td width=").append(BTN_COL_WIDTH).append(" align=center><button action=\"bypass -h npc_%objectId%_skillselect;")
					.append(groupType).append(";").append(schemeName).append(";").append(skillId).append(";").append(page)
					.append("\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomin2\" fore=\"L2UI_CH3.mapbutton_zoomin1\"></td>");
			}

			htmlBuilder.append("</tr></table><img src=\"L2UI.SquareGray\" width=").append(UI_WIDTH).append(" height=1>");
			row++;
		}

		appendPaginacion(htmlBuilder, groupType, schemeName, page, maxPage);
		return htmlBuilder.toString();
	}

	/**
	 * Variante de getGroupSkillList para la categoria Premium.
	 * Itera _premiumBuffsList directamente para mostrar todos los entries
	 * incluyendo duplicados de skill ID con distinto level (ej: guerrero/mago).
	 */
	private String getGroupSkillListPremium(Player player, String schemeName, int pageValue)
	{
		final SchemeBufferTable schemeBufferTable = SchemeBufferTable.getInstance();
		final SkillData skillData = SkillData.getInstance();
		final List<BuffSkillHolder> allEntries = schemeBufferTable.getPremiumBuffsList();

		if (allEntries.isEmpty())
		{
			return "La categoria Premium no tiene skills configurados.";
		}

		final int maxPage = HtmlUtil.countPageNumber(allEntries.size(), 6);
		final int page    = Math.max(1, Math.min(pageValue, maxPage));
		final List<BuffSkillHolder> pageEntries = allEntries.subList((page - 1) * 6, Math.min(page * 6, allEntries.size()));
		final List<Integer> schemeSkills = schemeBufferTable.getScheme(player.getObjectId(), schemeName);
		final StringBuilder html = new StringBuilder(pageEntries.size() * 150);
		int row = 0;

		for (BuffSkillHolder holder : pageEntries)
		{
			// Obtener skill para display; fallback a nivel 1 si el nivel premium no existe
			Skill skill = skillData.getSkill(holder.getId(), holder.getLevel());
			if (skill == null)
			{
				skill = skillData.getSkill(holder.getId(), 1);
			}
			if (skill == null)
			{
				continue;
			}

			html.append(row % 2 == 0 ? "<table width=\"256\" bgcolor=\"000000\"><tr>" : "<table width=\"256\"><tr>");
			html.append("<td height=40 width=").append(ICON_COL_WIDTH)
				.append(" align=center><img src=\"").append(skill.getIcon()).append("\" width=32 height=32></td>");
			html.append("<td width=").append(NAME_COL_WIDTH).append(">")
				.append(skill.getName()).append("<br1>")
				.append("<font color=\"B09878\">").append(holder.getDescription()).append("</font></td>");

			// El skillselect/unselect usa el skillId; agregar/quitar del esquema funciona por ID
			if (schemeSkills.contains(holder.getId()))
			{
				html.append("<td width=").append(BTN_COL_WIDTH)
					.append(" align=center><button action=\"bypass -h npc_%objectId%_skillunselect;")
					.append(PREMIUM_CATEGORY).append(";").append(schemeName)
					.append(";").append(holder.getId()).append(";").append(page)
					.append("\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomout2\" fore=\"L2UI_CH3.mapbutton_zoomout1\"></td>");
			}
			else
			{
				html.append("<td width=").append(BTN_COL_WIDTH)
					.append(" align=center><button action=\"bypass -h npc_%objectId%_skillselect;")
					.append(PREMIUM_CATEGORY).append(";").append(schemeName)
					.append(";").append(holder.getId()).append(";").append(page)
					.append("\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomin2\" fore=\"L2UI_CH3.mapbutton_zoomin1\"></td>");
			}

			html.append("</tr></table><img src=\"L2UI.SquareGray\" width=").append(UI_WIDTH).append(" height=1>");
			row++;
		}

		appendPaginacion(html, PREMIUM_CATEGORY, schemeName, page, maxPage);
		return html.toString();
	}

	/** Helper: agrega la barra de paginacion al HTML. */
	private void appendPaginacion(StringBuilder html, String groupType, String schemeName, int page, int maxPage)
	{
		html.append("<br1><img src=\"L2UI.SquareGray\" width=").append(UI_WIDTH).append(" height=1>")
			.append("<table width=\"").append(UI_WIDTH).append("\" bgcolor=000000><tr>");

		if (page > 1)
		{
			html.append("<td align=left width=").append(FOOTER_SIDE_WIDTH)
				.append("><a action=\"bypass -h npc_").append(getObjectId())
				.append("_editschemes;").append(groupType).append(";").append(schemeName).append(";").append(page - 1)
				.append("\">Previous</a></td>");
		}
		else
		{
			html.append("<td align=left width=").append(FOOTER_SIDE_WIDTH).append(">Previous</td>");
		}

		html.append("<td align=center width=").append(PAGE_CENTER_WIDTH).append(">Page ").append(page).append("</td>");

		if (page < maxPage)
		{
			html.append("<td align=right width=").append(FOOTER_SIDE_WIDTH)
				.append("><a action=\"bypass -h npc_").append(getObjectId())
				.append("_editschemes;").append(groupType).append(";").append(schemeName).append(";").append(page + 1)
				.append("\">Next</a></td>");
		}
		else
		{
			html.append("<td align=right width=").append(FOOTER_SIDE_WIDTH).append(">Next</td>");
		}

		html.append("</tr></table><img src=\"L2UI.SquareGray\" width=").append(UI_WIDTH).append(" height=1>");
	}

	/**
	 * Genera el frame de tipos de skill para el editor de esquemas.
	 * La categoria Premium solo se muestra si el jugador tiene premium activo.
	 */
	private static String getTypesFrame(String groupType, String schemeName, Player player)
	{
		StringBuilder htmlBuilder = new StringBuilder(500);
		htmlBuilder.append("<table>");
		int count = 0;

		for (String type : SchemeBufferTable.getInstance().getSkillTypes())
		{
			// Ocultar categoria Premium a jugadores sin premium
			if (PREMIUM_CATEGORY.equalsIgnoreCase(type) && !hasPremium(player))
			{
				continue;
			}

			if (count == 0)
			{
				htmlBuilder.append("<tr>");
			}

			if (groupType.equalsIgnoreCase(type))
			{
				htmlBuilder.append("<td width=65>").append(type).append("</td>");
			}
			else
			{
				htmlBuilder.append("<td width=65><a action=\"bypass -h npc_%objectId%_editschemes;")
					.append(type).append(";").append(schemeName).append(";1\">").append(type).append("</a></td>");
			}

			count++;
			if (count == TYPES_PER_ROW)
			{
				htmlBuilder.append("</tr>");
				count = 0;
			}
		}

		if (count != 0)
		{
			htmlBuilder.append("</tr>");
		}

		htmlBuilder.append("</table>");
		return htmlBuilder.toString();
	}

	// ── Utilidades ────────────────────────────────────────────────────────────

	private static Integer parseUnsignedInt(String value)
	{
		if (value == null || value.isEmpty())
		{
			return null;
		}

		int result = 0;
		for (int i = 0; i < value.length(); i++)
		{
			char c = value.charAt(i);
			if (c < '0' || c > '9')
			{
				return null;
			}

			int digit = c - 48;
			if (result > (Integer.MAX_VALUE - digit) / 10)
			{
				return null;
			}

			result = result * 10 + digit;
		}

		return result;
	}

	private static int getFee(List<Integer> list)
	{
		if (SchemeBufferConfig.BUFFER_STATIC_BUFF_COST > 0)
		{
			return list.size() * SchemeBufferConfig.BUFFER_STATIC_BUFF_COST;
		}

		int fee = 0;
		for (int skillId : list)
		{
			BuffSkillHolder holder = SchemeBufferTable.getInstance().getAvailableBuff(skillId);
			if (holder != null)
			{
				fee += holder.getPrice();
			}
		}

		return fee;
	}

	private static int getCountOf(List<Integer> skills, boolean dances)
	{
		SkillData skillData = SkillData.getInstance();
		int count = 0;

		for (int skillId : skills)
		{
			Skill skill = skillData.getSkill(skillId, 1);
			if (skill != null && skill.isDance() == dances)
			{
				count++;
			}
		}

		return count;
	}
}
