/*
 * Show Drop - Visualizador de Drop para Jugadores
 * Aden Chronicles - Custom Script
 *
 * Permite a los jugadores hacer Shift+Click sobre un monstruo atacable
 * para ver su tabla de drop/spoil con porcentajes y cantidades reales
 * (incluyendo multiplicadores del servidor y bonificaciones del personaje).
 *
 * Config : config/Custom/ShowDrop.ini
 * Trigger: Shift+Click sobre cualquier monstruo atacable con drops.
 */
package custom.ShowDrop;

import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.npc.DropType;
import org.l2jmobius.gameserver.model.actor.holders.npc.DropGroupHolder;
import org.l2jmobius.gameserver.model.actor.holders.npc.DropHolder;
import org.l2jmobius.gameserver.model.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.util.HtmlUtil;

public class ShowDrop extends Script implements IBypassHandler
{
	private static final Logger LOGGER = Logger.getLogger(ShowDrop.class.getName());

	// -------------------------------------------------------------------------
	// Config — public so NpcShiftClick can read ENABLED without coupling
	// -------------------------------------------------------------------------
	public static boolean ENABLED = true;
	public static boolean SHOW_SPOIL = true;
	public static int ITEMS_PER_PAGE = 10;

	private static final String[] COMMANDS =
	{
		"showdrop"
	};

	// -------------------------------------------------------------------------
	// Constructor
	// -------------------------------------------------------------------------
	private ShowDrop()
	{
		loadConfig();
		if (ENABLED)
		{
			BypassHandler.getInstance().registerHandler(this);
			LOGGER.info("ShowDrop: Sistema habilitado. Shift+Click en monstruos para ver drops.");
		}
		else
		{
			LOGGER.info("ShowDrop: Sistema deshabilitado por config.");
		}
	}

	// -------------------------------------------------------------------------
	// Config Loader
	// -------------------------------------------------------------------------
	private static void loadConfig()
	{
		try (InputStream is = new FileInputStream("./config/Custom/ShowDrop.ini"))
		{
			final Properties props = new Properties();
			props.load(is);

			ENABLED = Boolean.parseBoolean(props.getProperty("ShowDropEnabled", "True").trim());
			SHOW_SPOIL = Boolean.parseBoolean(props.getProperty("ShowDropSpoilEnabled", "True").trim());
			ITEMS_PER_PAGE = Integer.parseInt(props.getProperty("ShowDropItemsPerPage", "10").trim());

			if (ITEMS_PER_PAGE < 1)
			{
				ITEMS_PER_PAGE = 1;
			}
			else if (ITEMS_PER_PAGE > 30)
			{
				ITEMS_PER_PAGE = 30;
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("ShowDrop: No se pudo cargar ShowDrop.ini, usando valores por defecto. Error: " + e.getMessage());
		}
	}

	// -------------------------------------------------------------------------
	// IBypassHandler
	// Formato de bypass: showdrop DROP <npcObjectId> [pagina]
	//                    showdrop SPOIL <npcObjectId> [pagina]
	// -------------------------------------------------------------------------
	@Override
	public boolean onCommand(String command, Player player, Creature bypassOrigin)
	{
		if (!ENABLED)
		{
			return false;
		}

		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken(); // consume "showdrop"

		if (!st.hasMoreTokens())
		{
			return false;
		}

		final String typeStr = st.nextToken(); // DROP or SPOIL
		if (!st.hasMoreTokens())
		{
			return false;
		}

		final int objId;
		try
		{
			objId = Integer.parseInt(st.nextToken());
		}
		catch (NumberFormatException e)
		{
			return false;
		}

		final int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;

		final WorldObject obj = World.getInstance().findObject(objId);
		if (!(obj instanceof Npc))
		{
			player.sendMessage("[ShowDrop] El monstruo ya no esta disponible.");
			return false;
		}

		final DropType dropType;
		try
		{
			dropType = Enum.valueOf(DropType.class, typeStr);
		}
		catch (IllegalArgumentException e)
		{
			return false;
		}

		sendNpcDropList(player, (Npc) obj, dropType, page);
		return true;
	}

	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}

	// -------------------------------------------------------------------------
	// Entry point — llamado desde NpcShiftClick cuando un jugador hace
	// Shift+Click sobre un monstruo atacable con drops.
	// -------------------------------------------------------------------------
	public static void sendDropView(Player player, Npc npc)
	{
		sendNpcDropList(player, npc, DropType.DROP, 0);
	}

	// -------------------------------------------------------------------------
	// Drop list builder — construye y envia el HTML de drops al jugador
	// -------------------------------------------------------------------------
	private static void sendNpcDropList(Player player, Npc npc, DropType dropType, int pageValue)
	{
		// Recopilar la lista de drops segun el tipo solicitado
		List<DropHolder> dropList = null;

		if (dropType == DropType.SPOIL)
		{
			final List<DropHolder> spoilList = npc.getTemplate().getSpoilList();
			if (spoilList != null)
			{
				dropList = new ArrayList<>(spoilList);
			}
		}
		else
		{
			// Drops directos
			final List<DropHolder> drops = npc.getTemplate().getDropList();
			if (drops != null)
			{
				dropList = new ArrayList<>(drops);
			}

			// Drops de grupo (chance maestra * chance individual)
			final List<DropGroupHolder> dropGroups = npc.getTemplate().getDropGroups();
			if (dropGroups != null)
			{
				if (dropList == null)
				{
					dropList = new ArrayList<>();
				}
				for (DropGroupHolder dropGroup : dropGroups)
				{
					final double groupChance = dropGroup.getChance() / 100.0;
					for (DropHolder dropHolder : dropGroup.getDropList())
					{
						dropList.add(new DropHolder(dropHolder.getDropType(), dropHolder.getItemId(), dropHolder.getMin(), dropHolder.getMax(), dropHolder.getChance() * groupChance));
					}
				}
			}
		}

		if ((dropList == null) || dropList.isEmpty())
		{
			return;
		}

		// Ordenar por itemId para presentacion consistente
		Collections.sort(dropList, (d1, d2) -> Integer.compare(d1.getItemId(), d2.getItemId()));

		// Calcular paginas
		int pages = dropList.size() / ITEMS_PER_PAGE;
		if ((ITEMS_PER_PAGE * pages) < dropList.size())
		{
			pages++;
		}

		// Botones de paginacion
		final StringBuilder pagesSb = new StringBuilder();
		if (pages > 1)
		{
			pagesSb.append("<table><tr>");
			for (int i = 0; i < pages; i++)
			{
				pagesSb.append("<td align=center><button value=\"").append(i + 1).append("\" width=22 height=20 action=\"bypass showdrop ").append(dropType).append(" ").append(npc.getObjectId()).append(" ").append(i).append("\" back=\"L2UI_CT1.Button_DF_Calculator_Down\" fore=\"L2UI_CT1.Button_DF_Calculator\"></td>");
			}
			pagesSb.append("</tr></table>");
		}

		// Ajustar pagina actual
		int page = pageValue;
		if (page >= pages)
		{
			page = pages - 1;
		}

		final int start = page > 0 ? page * ITEMS_PER_PAGE : 0;
		int end = (page * ITEMS_PER_PAGE) + ITEMS_PER_PAGE;
		if (end > dropList.size())
		{
			end = dropList.size();
		}

		// Multiplicadores de tasa del jugador
		final DecimalFormat amountFormat = new DecimalFormat("#,###");
		final DecimalFormat chanceFormat = new DecimalFormat("0.00##");
		final PlayerStat stat = player.getStat();
		final double dropAdenaBonus = stat.getBonusDropAdenaMultiplier();
		final double dropAmountBonus = stat.getBonusDropAmountMultiplier();
		final double dropRateBonus = stat.getBonusDropRateMultiplier();
		final double spoilRateBonus = stat.getBonusSpoilRateMultiplier();

		// Construir columnas izquierda/derecha (layout 2 columnas)
		final StringBuilder leftSb = new StringBuilder();
		final StringBuilder rightSb = new StringBuilder();
		int leftHeight = 0;
		int rightHeight = 0;
		String limitReachedMsg = "";

		for (int i = start; i < end; i++)
		{
			final DropHolder dropItem = dropList.get(i);
			final ItemTemplate item = ItemData.getInstance().getTemplate(dropItem.getItemId());
			if (item == null)
			{
				continue;
			}

			// Calcular multiplicadores de tasa reales
			double rateChance = 1.0;
			double rateAmount = 1.0;

			if (dropType == DropType.SPOIL)
			{
				rateChance = RatesConfig.RATE_SPOIL_DROP_CHANCE_MULTIPLIER;
				rateAmount = RatesConfig.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER;
				if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
				{
					rateChance *= PremiumSystemConfig.PREMIUM_RATE_SPOIL_CHANCE;
					rateAmount *= PremiumSystemConfig.PREMIUM_RATE_SPOIL_AMOUNT;
				}
				rateChance *= spoilRateBonus;
			}
			else
			{
				// Chance
				if (RatesConfig.RATE_DROP_CHANCE_BY_ID.get(dropItem.getItemId()) != null)
				{
					rateChance *= RatesConfig.RATE_DROP_CHANCE_BY_ID.get(dropItem.getItemId());
					if ((dropItem.getItemId() == Inventory.ADENA_ID) && (rateChance > 100))
					{
						rateChance = 100;
					}
				}
				else if (item.hasExImmediateEffect())
				{
					rateChance *= RatesConfig.RATE_HERB_DROP_CHANCE_MULTIPLIER;
				}
				else if (npc.isRaid())
				{
					rateChance *= RatesConfig.RATE_RAID_DROP_CHANCE_MULTIPLIER;
				}
				else
				{
					rateChance *= RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER;
				}

				// Amount
				if (RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(dropItem.getItemId()) != null)
				{
					rateAmount *= RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(dropItem.getItemId());
				}
				else if (item.hasExImmediateEffect())
				{
					rateAmount *= RatesConfig.RATE_HERB_DROP_AMOUNT_MULTIPLIER;
				}
				else if (npc.isRaid())
				{
					rateAmount *= RatesConfig.RATE_RAID_DROP_AMOUNT_MULTIPLIER;
				}
				else
				{
					rateAmount *= RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER;
				}

				// Premium rates
				if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
				{
					if (PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(dropItem.getItemId()) != null)
					{
						rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(dropItem.getItemId());
					}
					else if (!item.hasExImmediateEffect() && !npc.isRaid())
					{
						rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE;
					}

					if (PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(dropItem.getItemId()) != null)
					{
						rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(dropItem.getItemId());
					}
					else if (!item.hasExImmediateEffect() && !npc.isRaid())
					{
						rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT;
					}
				}

				// Bonificaciones del jugador
				rateAmount *= dropAmountBonus;
				if (item.getId() == Inventory.ADENA_ID)
				{
					rateAmount *= dropAdenaBonus;
				}
				rateChance *= dropRateBonus;
			}

			// No mostrar items con chance 0
			if (rateChance == 0d)
			{
				continue;
			}

			// Cantidad final con multiplicadores
			final long min = (long) (dropItem.getMin() * rateAmount);
			final long max = (long) (dropItem.getMax() * rateAmount);

			// Construir bloque de item
			final StringBuilder sb = new StringBuilder();
			sb.append("<table width=332 cellpadding=2 cellspacing=0 background=\"L2UI_CT1.Windows.Windows_DF_TooltipBG\">");
			sb.append("<tr><td width=32 valign=top>");
			sb.append("<img src=\"").append(item.getIcon() == null ? "icon.etc_question_mark_i00" : item.getIcon()).append("\" width=32 height=32>");
			sb.append("</td><td fixwidth=300 align=center><font name=\"hs9\" color=\"CD9000\">");
			sb.append(item.getName());
			sb.append("</font></td></tr>");
			sb.append("<tr><td width=32></td><td width=300><table width=295 cellpadding=0 cellspacing=0>");
			sb.append("<tr><td width=70 align=right valign=top><font color=\"LEVEL\">Amount: </font></td>");
			sb.append("<td width=225 align=center>");
			if (min == max)
			{
				sb.append(amountFormat.format(min));
			}
			else
			{
				sb.append(amountFormat.format(min)).append(" - ").append(amountFormat.format(max));
			}
			sb.append("</td></tr>");
			sb.append("<tr><td width=70 align=right valign=top><font color=\"LEVEL\">Chance: </font></td>");
			sb.append("<td width=225 align=center>");
			sb.append(chanceFormat.format(Math.min(dropItem.getChance() * rateChance, 100.0)));
			sb.append("%</td></tr></table></td></tr>");
			sb.append("<tr><td width=32></td><td width=300>&nbsp;</td></tr></table>");

			// Distribuir en columnas respetando el limite de HTML
			final int height = 64;
			if ((sb.length() + leftSb.length() + rightSb.length()) < 16000)
			{
				if (leftHeight >= (rightHeight + height))
				{
					rightSb.append(sb);
					rightHeight += height;
				}
				else
				{
					leftSb.append(sb);
					leftHeight += height;
				}
			}
			else
			{
				limitReachedMsg = "<br><center><font color=FF6666>Demasiados drops, no se pudieron mostrar todos.</font></center>";
				break;
			}
		}

		// Botones Drop / Spoil (toggle entre los dos tipos)
		final StringBuilder buttonSb = new StringBuilder();
		final boolean hasDrop = (npc.getTemplate().getDropList() != null) || (npc.getTemplate().getDropGroups() != null);
		final boolean hasSpoil = SHOW_SPOIL && (npc.getTemplate().getSpoilList() != null);

		buttonSb.append("<table width=295 cellpadding=0 cellspacing=2><tr>");
		if (hasDrop)
		{
			if (dropType == DropType.DROP)
			{
				buttonSb.append("<td align=center><button value=\"[ Drop ]\" width=110 height=25 action=\"bypass showdrop DROP ").append(npc.getObjectId()).append("\" back=\"L2UI_CT1.Button_DF_Calculator_Down\" fore=\"L2UI_CT1.Button_DF_Calculator\"></td>");
			}
			else
			{
				buttonSb.append("<td align=center><button value=\"Drop\" width=110 height=25 action=\"bypass showdrop DROP ").append(npc.getObjectId()).append("\" back=\"L2UI_CT1.Button_DF_Calculator_Down\" fore=\"L2UI_CT1.Button_DF_Calculator\"></td>");
			}
		}
		if (hasSpoil)
		{
			if (dropType == DropType.SPOIL)
			{
				buttonSb.append("<td align=center><button value=\"[ Spoil ]\" width=110 height=25 action=\"bypass showdrop SPOIL ").append(npc.getObjectId()).append("\" back=\"L2UI_CT1.Button_DF_Calculator_Down\" fore=\"L2UI_CT1.Button_DF_Calculator\"></td>");
			}
			else
			{
				buttonSb.append("<td align=center><button value=\"Spoil\" width=110 height=25 action=\"bypass showdrop SPOIL ").append(npc.getObjectId()).append("\" back=\"L2UI_CT1.Button_DF_Calculator_Down\" fore=\"L2UI_CT1.Button_DF_Calculator\"></td>");
			}
		}
		buttonSb.append("</tr></table>");

		// Indicador de pagina actual
		final String pageInfo = pages > 1 ? "<font color=808080>Pagina " + (page + 1) + " / " + pages + "</font><br1>" : "";

		// Armar HTML completo
		final StringBuilder htmlSb = new StringBuilder();
		htmlSb.append("<html><head><title>Drop: ").append(npc.getName()).append("</title></head><body><center>");
		htmlSb.append("<font color=AA8800 name=\"hs9\">").append(npc.getName()).append("</font><br>");
		htmlSb.append(buttonSb);
		htmlSb.append("<br1>");
		htmlSb.append(pageInfo);
		htmlSb.append(pagesSb);
		if ((leftSb.length() == 0) && (rightSb.length() == 0))
		{
			htmlSb.append("<br><font color=808080>No hay drops para mostrar.</font>");
		}
		else
		{
			htmlSb.append("<table><tr><td>").append(leftSb).append("</td><td>").append(rightSb).append("</td></tr></table>");
		}
		htmlSb.append(limitReachedMsg);
		if (pages > 1)
		{
			htmlSb.append("<br1>").append(pagesSb);
		}
		htmlSb.append("</center></body></html>");

		HtmlUtil.sendCBHtml(player, htmlSb.toString());
	}

	// -------------------------------------------------------------------------
	// Bootstrap
	// -------------------------------------------------------------------------
	public static void main(String[] args)
	{
		new ShowDrop();
	}
}
