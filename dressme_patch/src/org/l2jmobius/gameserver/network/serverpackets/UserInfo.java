/*
 * This file is part of the L2J Mobius project.
 * Modified for DressMe visual appearance system - L2 Zona Zero
 */
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Iterator;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.custom.dressme.DressMeManager;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class UserInfo extends ServerPacket
{
	private final Player _player;
	private int _relation;
	private int _airShipHelm;
	private final int _runSpd;
	private final int _walkSpd;
	private final int _swimRunSpd;
	private final int _swimWalkSpd;
	private final int _flyRunSpd;
	private final int _flyWalkSpd;
	private final double _moveMultiplier;

	public UserInfo(Player player)
	{
		_player = player;
		final int _territoryId = TerritoryWarManager.getInstance().getRegisteredTerritoryId(player);
		_relation = _player.isClanLeader() ? 64 : 0;
		if (_player.getSiegeState() == 1)
		{
			_relation = _territoryId == 0 ? (_relation |= 0x180) : (_relation |= 0x1000);
		}
		if (_player.getSiegeState() == 2)
		{
			_relation |= 0x80;
		}
		_airShipHelm = _player.isInAirShip() && _player.getAirShip().isCaptain(_player) ? _player.getAirShip().getHelmItemId() : 0;
		_moveMultiplier = player.getMovementSpeedMultiplier();
		_runSpd = (int) Math.round(player.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) Math.round(player.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = (int) Math.round(player.getSwimRunSpeed() / _moveMultiplier);
		_swimWalkSpd = (int) Math.round(player.getSwimWalkSpeed() / _moveMultiplier);
		_flyRunSpd = player.isFlying() ? _runSpd : 0;
		_flyWalkSpd = player.isFlying() ? _walkSpd : 0;
	}

	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.USER_INFO.writeId(this, buffer);
		buffer.writeInt(_player.getX());
		buffer.writeInt(_player.getY());
		buffer.writeInt(_player.getZ());
		buffer.writeInt(_player.getVehicle() != null ? _player.getVehicle().getObjectId() : 0);
		buffer.writeInt(_player.getObjectId());
		final PlayerAppearance appearance = _player.getAppearance();
		buffer.writeString(appearance.getVisibleName());
		buffer.writeInt(_player.getRace().ordinal());
		buffer.writeInt(appearance.isFemale());
		buffer.writeInt(_player.getBaseClass());
		buffer.writeInt(_player.getLevel());
		buffer.writeLong(_player.getExp());
		buffer.writeDouble((double) ((float) (_player.getExp() - ExperienceData.getInstance().getExpForLevel(_player.getLevel())) / (float) (ExperienceData.getInstance().getExpForLevel(_player.getLevel() + 1) - ExperienceData.getInstance().getExpForLevel(_player.getLevel()))));
		buffer.writeInt(_player.getSTR());
		buffer.writeInt(_player.getDEX());
		buffer.writeInt(_player.getCON());
		buffer.writeInt(_player.getINT());
		buffer.writeInt(_player.getWIT());
		buffer.writeInt(_player.getMEN());
		buffer.writeInt(_player.getMaxHp());
		buffer.writeInt((int) Math.round(_player.getCurrentHp()));
		buffer.writeInt(_player.getMaxMp());
		buffer.writeInt((int) Math.round(_player.getCurrentMp()));
		buffer.writeInt((int) _player.getSp());
		buffer.writeInt(_player.getCurrentLoad());
		buffer.writeInt(_player.getMaxLoad());
		buffer.writeInt(_player.getActiveWeaponItem() != null ? 40 : 20);

		// Object IDs - always real
		for (int slot : getPaperdollOrder())
		{
			buffer.writeInt(_player.getInventory().getPaperdollObjectId(slot));
		}

		// ── DressMe: visual item IDs ───────────────────────────────
		final int pid = _player.getObjectId();
		for (int slot : getPaperdollOrder())
		{
			int visualId = DressMeManager.getInstance().getVisualId(pid, slot);
			buffer.writeInt(visualId > 0 ? visualId : _player.getInventory().getPaperdollItemDisplayId(slot));
		}

		// Augmentation IDs - always real
		for (int slot : getPaperdollOrder())
		{
			buffer.writeInt(_player.getInventory().getPaperdollAugmentationId(slot));
		}

		buffer.writeInt(_player.getInventory().getTalismanSlots());
		buffer.writeInt(_player.getInventory().canEquipCloak());
		buffer.writeInt((int) _player.getPAtk(null));
		buffer.writeInt((int) _player.getPAtkSpd());
		buffer.writeInt((int) _player.getPDef(null));
		buffer.writeInt(_player.getEvasionRate(null));
		buffer.writeInt(_player.getAccuracy());
		buffer.writeInt(_player.getCriticalHit(null, null));
		buffer.writeInt((int) _player.getMAtk(null, null));
		buffer.writeInt(_player.getMAtkSpd());
		buffer.writeInt((int) _player.getPAtkSpd());
		buffer.writeInt((int) _player.getMDef(null, null));
		buffer.writeInt((int) _player.getPvpFlag());
		buffer.writeInt(_player.getKarma());
		buffer.writeInt(_runSpd);
		buffer.writeInt(_walkSpd);
		buffer.writeInt(_swimRunSpd);
		buffer.writeInt(_swimWalkSpd);
		buffer.writeInt(_flyRunSpd);
		buffer.writeInt(_flyWalkSpd);
		buffer.writeInt(_flyRunSpd);
		buffer.writeInt(_flyWalkSpd);
		buffer.writeDouble(_moveMultiplier);
		buffer.writeDouble(_player.getAttackSpeedMultiplier());
		buffer.writeDouble(_player.getCollisionRadius());
		buffer.writeDouble(_player.getCollisionHeight());
		buffer.writeInt((int) appearance.getHairStyle());
		buffer.writeInt((int) appearance.getHairColor());
		buffer.writeInt((int) appearance.getFace());
		buffer.writeInt(_player.isGM());
		String title = _player.getTitle();
		if (_player.isGM() && _player.isInvisible())
		{
			title = "[Invisible]";
		}
		buffer.writeString(title);
		buffer.writeInt(_player.getClanId());
		buffer.writeInt(_player.getClanCrestId());
		buffer.writeInt(_player.getAllyId());
		buffer.writeInt(_player.getAllyCrestId());
		buffer.writeInt(_relation);
		buffer.writeByte(_player.getMountType().ordinal());
		buffer.writeByte(_player.getPrivateStoreType().getId());
		buffer.writeByte(_player.hasDwarvenCraft());
		buffer.writeInt(_player.getPkKills());
		buffer.writeInt(_player.getPvpKills());
		buffer.writeShort(_player.getCubics().size());
		final Iterator<?> iterator = _player.getCubics().keySet().iterator();
		while (iterator.hasNext())
		{
			final int cubicId = (Integer) iterator.next();
			buffer.writeShort(cubicId);
		}
		buffer.writeByte(_player.isInPartyMatchRoom());
		buffer.writeInt(_player.isInvisible() ? _player.getAbnormalVisualEffects() | AbnormalVisualEffect.STEALTH.getMask() : _player.getAbnormalVisualEffects());
		buffer.writeByte(_player.isInsideZone(ZoneId.WATER) ? 1 : (_player.isFlyingMounted() ? 2 : 0));
		buffer.writeInt(_player.getClanPrivileges().getMask());
		buffer.writeShort(_player.getRecomLeft());
		buffer.writeShort(_player.getRecomHave());
		buffer.writeInt(_player.getMountNpcId() > 0 ? _player.getMountNpcId() + 1000000 : 0);
		buffer.writeShort(_player.getInventoryLimit());
		buffer.writeInt(_player.getPlayerClass().getId());
		buffer.writeInt(0);
		buffer.writeInt(_player.getMaxCp());
		buffer.writeInt((int) _player.getCurrentCp());
		buffer.writeByte(_player.isMounted() || _airShipHelm != 0 ? 0 : _player.getEnchantEffect());
		buffer.writeByte(_player.getTeam().getId());
		buffer.writeInt(_player.getClanCrestLargeId());
		buffer.writeByte(_player.isNoble());
		buffer.writeByte(_player.isHero() || (_player.isGM() && GeneralConfig.GM_HERO_AURA));
		buffer.writeByte(_player.isFishing());
		buffer.writeInt(_player.getFishX());
		buffer.writeInt(_player.getFishY());
		buffer.writeInt(_player.getFishZ());
		buffer.writeInt(appearance.getNameColor());
		buffer.writeByte(_player.isRunning());
		buffer.writeInt(_player.getPledgeClass());
		buffer.writeInt(_player.getPledgeType());
		buffer.writeInt(appearance.getTitleColor());
		buffer.writeInt(_player.isCursedWeaponEquipped() ? CursedWeaponsManager.getInstance().getLevel(_player.getCursedWeaponEquippedId()) : 0);
		buffer.writeInt(_player.getTransformationDisplayId());
		final byte attackAttribute = _player.getAttackElement();
		buffer.writeShort((short) attackAttribute);
		buffer.writeShort(_player.getAttackElementValue(attackAttribute));
		buffer.writeShort(_player.getDefenseElementValue((byte) 0));
		buffer.writeShort(_player.getDefenseElementValue((byte) 1));
		buffer.writeShort(_player.getDefenseElementValue((byte) 2));
		buffer.writeShort(_player.getDefenseElementValue((byte) 3));
		buffer.writeShort(_player.getDefenseElementValue((byte) 4));
		buffer.writeShort(_player.getDefenseElementValue((byte) 5));
		buffer.writeInt(_player.getAgathionId());
		buffer.writeInt(_player.getFame());
		buffer.writeInt(_player.isMinimapAllowed());
		buffer.writeInt(_player.getVitalityPoints());
		buffer.writeInt(_player.getAbnormalVisualEffectSpecial());
	}
}
