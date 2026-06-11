/*
 * Voiced command handler para Battle Royale.
 * .registerbr — registra/desregistra al jugador (redirige al NPC si esta en Giran).
 * .brready    — confirma participacion durante el ready-check de 30 segundos.
 */
package handlers.chat.commands.voiced;

import custom.events.BattleRoyale.BattleRoyale;
import custom.events.BattleRoyale.BattleRoyale.EventState;

import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class BattleRoyaleVoice implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"registerbr",
		"brready"
	};

	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		if (player == null)
		{
			return false;
		}

		if ("registerbr".equalsIgnoreCase(command))
		{
			handleRegister(player);
			return true;
		}

		if ("brready".equalsIgnoreCase(command))
		{
			handleBrReady(player);
			return true;
		}

		return false;
	}

	// =========================================================================
	// .brready — confirmar participacion durante el ready-check
	// =========================================================================
	private void handleBrReady(Player player)
	{
		final EventState state = BattleRoyale.getState();

		if (state != EventState.WAITING)
		{
			player.sendMessage("[Battle Royale] No hay confirmacion pendiente en este momento.");
			return;
		}

		if (!BattleRoyale.isPendingReady(player))
		{
			player.sendMessage("[Battle Royale] No tienes una confirmacion pendiente.");
			return;
		}

		BattleRoyale.getInstance().handleReadyConfirm(player);
	}

	// =========================================================================
	// .registerbr — registro / desregistro
	// =========================================================================
	private void handleRegister(Player player)
	{
		final EventState state = BattleRoyale.getState();

		// Jugador dentro de la arena
		if (BattleRoyale.isInEvent(player))
		{
			if (state == EventState.ACTIVE || state == EventState.WAITING)
			{
				showEventStatusHtml(player);
				return;
			}
			// Esta registrado → desregistrar
			final boolean removed = BattleRoyale.tryUnregister(player);
			if (removed)
			{
				player.sendMessage("[Battle Royale] Registro cancelado.");
			}
			showRegistrationHtml(player);
			return;
		}

		if (state == EventState.INACTIVE)
		{
			player.sendMessage("[Battle Royale] El evento no esta activo. Habla con el Battle Royale Event Manager en Giran.");
			showRegistrationHtml(player);
			return;
		}

		if (state != EventState.REGISTRATION)
		{
			player.sendMessage("[Battle Royale] El registro ya cerro. Espera el proximo evento.");
			showRegistrationHtml(player);
			return;
		}

		// Intentar registrar
		final String error = BattleRoyale.tryRegister(player);
		if (error != null)
		{
			player.sendMessage("[Battle Royale] No puedes registrarte: " + error);
			showRegistrationHtml(player);
			return;
		}

		player.sendMessage("[Battle Royale] Registro exitoso! Espera el inicio del evento.");
		showRegistrationHtml(player);
	}

	private void showRegistrationHtml(Player player)
	{
		final EventState state = BattleRoyale.getState();
		final boolean isRegistered = BattleRoyale.isInEvent(player);
		final int count = BattleRoyale.getRegisteredCount();
		final int min = BattleRoyale.getMinPlayers();
		final int max = BattleRoyale.getMaxPlayers();

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<center>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\"><br>");
		sb.append("<font color=\"LEVEL\">Battle Royale</font><br>");
		sb.append("<font color=\"808080\">Ultimo Jugador en Pie</font><br><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");

		switch (state)
		{
			case REGISTRATION:
			{
				sb.append("<table width=\"270\">");
				sb.append("<tr><td><font color=\"808080\">Estado:</font></td><td><font color=\"00FF00\">Registro Abierto</font></td></tr>");
				sb.append("<tr><td><font color=\"808080\">Inscriptos:</font></td><td><font color=\"LEVEL\">").append(count).append(" / ").append(max).append("</font></td></tr>");
				sb.append("<tr><td><font color=\"808080\">Minimo:</font></td><td><font color=\"LEVEL\">").append(min).append(" jugadores</font></td></tr>");
				sb.append("</table><br>");

				if (isRegistered)
				{
					sb.append("<font color=\"00FF00\">Estas registrado!</font><br><br>");
					sb.append("<font color=\"FF6347\">IMPORTANTE: Buffeate antes de confirmar entrada.</font><br><br>");
					sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
					sb.append("<button value=\"Cancelar registro\" action=\"bypass voice .registerbr\" width=\"200\" height=\"30\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
				}
				else
				{
					sb.append("<font color=\"FF6347\">Zona: Kamaloka — Solo un sobreviviente!</font><br>");
					sb.append("<font color=\"808080\">No hay buffer. Usa tus propios buffs.</font><br>");
					sb.append("<font color=\"808080\">O habla con el Event Manager en Giran.</font><br><br>");
					sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
					sb.append("<button value=\"Registrarse\" action=\"bypass voice .registerbr\" width=\"200\" height=\"30\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
				}
				break;
			}
			case WAITING:
			{
				sb.append("<font color=\"FFAA00\">El evento esta a punto de comenzar!</font><br><br>");
				if (BattleRoyale.isPendingReady(player))
				{
					sb.append("<font color=\"FF6347\">Tienes una confirmacion pendiente!</font><br>");
					sb.append("<font color=\"FFFF00\">Escribe <font color=\"FFFFFF\">.brready</font> para confirmar.</font><br>");
					sb.append("<font color=\"AAAAAA\">O habla con el Event Manager en Giran.</font><br>");
				}
				else
				{
					sb.append("<font color=\"808080\">El registro ya cerro. Espera el proximo evento.</font><br>");
				}
				break;
			}
			case ACTIVE:
			{
				sb.append("<font color=\"FF6347\">El evento ya esta en curso.</font><br><br>");
				sb.append("<font color=\"808080\">Podras participar en el proximo evento.</font><br>");
				break;
			}
			default:
			{
				sb.append("<font color=\"808080\">El evento no esta activo en este momento.</font><br><br>");
				sb.append("El Battle Royale es iniciado manualmente por un GM.<br>");
				sb.append("<font color=\"808080\">Habla con el Event Manager en Giran cuando el registro este abierto.</font><br>");
				break;
			}
		}

		sb.append("<br><img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\">");
		sb.append("</center>");
		sb.append("</body></html>");

		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}

	private void showEventStatusHtml(Player player)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<center>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\"><br>");
		sb.append("<font color=\"LEVEL\">Battle Royale — En Curso</font><br><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
		sb.append("<font color=\"FF6347\">Estas dentro del evento!</font><br><br>");
		sb.append("<font color=\"808080\">Comandos disponibles:</font><br>");
		sb.append("<table width=\"270\">");
		sb.append("<tr><td><font color=\"LEVEL\">.apon</font></td><td><font color=\"808080\">Activar AutoPotion HP</font></td></tr>");
		sb.append("<tr><td><font color=\"LEVEL\">.apoff</font></td><td><font color=\"808080\">Desactivar AutoPotion HP</font></td></tr>");
		sb.append("<tr><td><font color=\"LEVEL\">.potionon</font></td><td><font color=\"808080\">Activar AutoPotion</font></td></tr>");
		sb.append("<tr><td><font color=\"LEVEL\">.potionoff</font></td><td><font color=\"808080\">Desactivar AutoPotion</font></td></tr>");
		sb.append("</table><br>");
		sb.append("<img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\">");
		sb.append("</center>");
		sb.append("</body></html>");

		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}

	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
