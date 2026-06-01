/*
 * NPC: Maestro de Reputacion (ID 50020)
 * Intercambia 1000 unidades del item 10639 por 100 puntos de reputacion de clan.
 * Solo el lider de clan puede realizar el intercambio.
 */
package custom.ReputationMaster;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Maestro de Reputacion de Clan
 * Intercambia item 10639 x1000 -> 100 puntos de reputacion de clan
 * Requiere: lider de clan
 */
public class ReputationMaster extends Script
{
	// NPC
	private static final int NPC_ID = 50020;

	// Intercambio
	private static final int  ITEM_ID      = 10639;
	private static final long ITEM_COST    = 1000;
	private static final int  REP_REWARD   = 100;

	private ReputationMaster()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		// Sin clan
		if (player.getClan() == null)
		{
			return "50020-noclan.htm";
		}
		// No es lider
		if (!player.isClanLeader())
		{
			return "50020-noleader.htm";
		}
		return "50020.htm";
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "exchange":
			{
				// Validaciones
				if (player.getClan() == null)
				{
					return "50020-noclan.htm";
				}
				if (!player.isClanLeader())
				{
					return "50020-noleader.htm";
				}
				if (getQuestItemsCount(player, ITEM_ID) < ITEM_COST)
				{
					return "50020-noitems.htm";
				}

				// Realizar intercambio
				player.destroyItemByItemId(ItemProcessType.FEE, ITEM_ID, ITEM_COST, npc, true);
				player.getClan().addReputationScore(REP_REWARD);

				// Mensaje del sistema: "Su clan obtuvo X puntos de reputacion"
				player.sendMessage("Tu clan ha recibido +" + REP_REWARD + " puntos de reputacion.");
				player.sendPacket(QuestSound.ITEMSOUND_QUEST_FINISH.getPacket());

				return "50020-ok.htm";
			}
		}
		return null;
	}

	public static void main(String[] args)
	{
		new ReputationMaster();
	}
}
