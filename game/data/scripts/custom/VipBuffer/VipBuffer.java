/*
 * VIP Buffer - NPC buffeador exclusivo para cuentas Premium.
 * Adaptado para L2J Mobius CT2.6 HighFive - L2 Zona Zero
 *
 * Acceso: requiere cuenta Premium activa (hasPremiumStatus).
 * Restricciones: no funciona en PvP flag ni con Karma.
 *
 * NPC ID: 6001  --  //spawn 6001  para spawnearlo en el mundo.
 */
package custom.VipBuffer;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * VIP Buffer NPC - Solo para jugadores con cuenta Premium.
 * @author Adaptado para L2 Zona Zero
 */
public class VipBuffer extends Script
{
	private static final int NPC_ID = 6001;

	private VipBuffer()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (!player.hasPremiumStatus())
		{
			return "<html><head><body>Solo los jugadores con cuenta <font color=\"FFDF00\">Premium</font> pueden usar este servicio.<br>Consulta con un Administrador para obtener acceso.</body></html>";
		}
		if (player.getPvpFlag() > 0)
		{
			return "<html><head><body>No puedes usar este servicio mientras estas <font color=\"FF4444\">en PvP Flag</font>.<br>Espera a que desaparezca la bandera e intentalo de nuevo.</body></html>";
		}
		if (player.getKarma() > 0)
		{
			return "<html><head><body>No puedes usar este servicio mientras tienes <font color=\"FF0000\">Karma</font>.</body></html>";
		}
		return "1.htm";
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!player.hasPremiumStatus())
		{
			return "<html><head><body>Solo los jugadores con cuenta <font color=\"FFDF00\">Premium</font> pueden usar este servicio.</body></html>";
		}

		if (event.endsWith(".htm"))
		{
			return event;
		}

		final int[] info = getSkillInfo(event);
		if (info != null)
		{
			final Skill skill = SkillData.getInstance().getSkill(info[0], info[1]);
			if (skill != null)
			{
				skill.applyEffects(player, player);
			}
			return getReturnPage(event);
		}

		return "1.htm";
	}

	/**
	 * Retorna [skillId, skillLevel] para el evento dado, o null si no coincide.
	 */
	private int[] getSkillInfo(String event)
	{
		switch (event)
		{
			// ---- Resistencias elementales ----
			case "1":  return new int[]{1182, 330}; // Resist Aqua +30
			case "2":  return new int[]{1191, 330}; // Resist Fire +30
			case "3":  return new int[]{1189, 330}; // Resist Wind +30
			case "4":  return new int[]{1393, 130}; // Unholy Resistance +30
			case "5":  return new int[]{1392, 130}; // Holy Resistance +30
			case "6":  return new int[]{1033, 330}; // Resist Poison +30
			case "7":  return new int[]{1032, 330}; // Invigor +30
			case "8":  return new int[]{1238, 330}; // Freezing Skin +30
			case "9":  return new int[]{1232, 330}; // Blazing Skin +30
			case "10": return new int[]{1259, 330}; // Resist Shock +30
			case "24": return new int[]{1548, 330}; // Resist Earth +30
			// ---- Mejoras de combate ----
			case "11": return new int[]{1443, 130}; // Dark Weapon +30
			case "12": return new int[]{1416, 115}; // Pa'agrio's Fist +15
			case "13": return new int[]{1442, 130}; // Protection From Darkness +30
			case "14": return new int[]{1047, 130}; // Mana Regeneration +30
			case "15": return new int[]{1397, 130}; // Clarity Physical +30
			case "16": return new int[]{1397, 230}; // Clarity Magic +30
			case "22": return new int[]{982,    3}; // Combat Aura Lv3
			// ---- Buffs especiales (requiere Premium) ----
			case "17": return new int[]{1374,   1}; // Heroic Valor
			case "18": return new int[]{1355, 315}; // Prophecy of Water +15
			case "19": return new int[]{1356, 315}; // Prophecy of Fire +15
			case "20": return new int[]{1357, 315}; // Prophecy of Wind +15
			case "21": return new int[]{1363, 315}; // Chant of Victory +15
			case "23": return new int[]{396,    1}; // Heroic Berserker
		}
		return null;
	}

	/**
	 * Determina a que menu volver segun el buff aplicado.
	 * Buffs especiales (17-23) regresan a pagina 3; el resto a pagina 2.
	 */
	private String getReturnPage(String event)
	{
		switch (event)
		{
			case "17": case "18": case "19":
			case "20": case "21": case "23":
				return "3.htm";
			default:
				return "2.htm";
		}
	}

	public static void main(String[] args)
	{
		new VipBuffer();
	}
}
