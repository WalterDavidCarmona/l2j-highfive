/*
 * This file is part of the L2J Mobius project.
 * Modified for DressMe visual appearance system - L2 Zona Zero
 */
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.custom.dressme.DressMeManager;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.model.actor.instance.Decoy;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class CharInfo extends ServerPacket
{
	private static final int[] PAPERDOLL_ORDER = new int[] { 0, 1, 5, 7, 10, 6, 11, 12, 23, 5, 2, 3, 16, 15, 17, 18, 19, 20, 21, 22, 24 };

	private final Player _player;
	private final Clan _clan;
	private int _objId;
	private int _x;
	private int _y;
	private int _z;
	private int _heading;
	private final int _mAtkSpd;
	private final int _pAtkSpd;
	private final int _runSpd;
	private final int _walkSpd;
	private final int _swimRunSpd;
	private final int _swimWalkSpd;
	private final int _flyRunSpd;
	private final int _flyWalkSpd;
	private final double _moveMultiplier;
	private int _vehicleId = 0;
	private final boolean _gmSeeInvis;

	public CharInfo(Player player, boolean gmSeeInvis)
	{
		_player = player;
		_objId = player.getObjectId();
		_clan = player.getClan();
		if ((_player.getVehicle() != null) && (_player.getInVehiclePosition() != null))
		{
			_x = _player.getInVehiclePosition().getX();
			_y = _player.getInVehiclePosition().getY();
			_z = _player.getInVehiclePosition().getZ();
			_vehicleId = _player.getVehicle().getObjectId();
		}
		else
		{
			_x = _player.getX();
			_y = _player.getY();
			_z = _player.getZ();
		}
		_heading = _player.getHeading();
		_mAtkSpd = _player.getMAtkSpd();
		_pAtkSpd = (int) _player.getPAtkSpd();
		_moveMultiplier = player.getMovementSpeedMultiplier();
		_runSpd = (int) Math.round(player.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) Math.round(player.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = (int) Math.round(player.getSwimRunSpeed() / _moveMultiplier);
		_swimWalkSpd = (int) Math.round(player.getSwimWalkSpeed() / _moveMultiplier);
		_flyRunSpd = player.isFlying() ? _runSpd : 0;
		_flyWalkSpd = player.isFlying() ? _walkSpd : 0;
		_gmSeeInvis = gmSeeInvis;
	}

	public CharInfo(Decoy decoy, boolean gmSeeInvis)
	{
		this(decoy.asPlayer(), gmSeeInvis);
		_objId = decoy.getObjectId();
		_x = decoy.getX();
		_y = decoy.getY();
		_z = decoy.getZ();
		_heading = decoy.getHeading();
	}

	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.CHAR_INFO.writeId(this, buffer);
		buffer.writeInt(_x);
		buffer.writeInt(_y);
		buffer.writeInt(_z);
		buffer.writeInt(_vehicleId);
		buffer.writeInt(_objId);
		final PlayerAppearance appearance = _player.getAppearance();
		buffer.writeString(appearance.getVisibleName());
		buffer.writeInt(_player.getRace().ordinal());
		buffer.writeInt(appearance.isFemale());
		buffer.writeInt(_player.getBaseClass());

		// ── DressMe: visual item IDs ───────────────────────────────
		final int pid = _player.getObjectId();
		for (int slot : getPaperdollOrder())
		{
			int visualId = DressMeManager.getInstance().getVisualId(pid, slot);
			buffer.writeInt(visualId > 0 ? visualId : _player.getInventory().getPaperdollItemDisplayId(slot));
		}

		// Augmentation IDs (always use real item augments)
		for (int slot : getPaperdollOrder())
		{
			buffer.writeInt(_player.getInventory().getPaperdollAugmentationId(slot));
		}

		buffer.writeInt(_player.getInventory().getTalismanSlots());
		buffer.writeInt(_player.getInventory().canEquipCloak());
		buffer.writeInt((int) _player.getPvpFlag());
		buffer.writeInt(_player.getKarma());
		buffer.writeInt(_mAtkSpd);
		buffer.writeInt(_pAtkSpd);
		buffer.writeInt(0);
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
		buffer.writeString(_gmSeeInvis ? "Invisible" : appearance.getVisibleTitle());
		if (!_player.isCursedWeaponEquipped())
		{
			buffer.writeInt(_player.getClanId());
			buffer.writeInt(_player.getClanCrestId());
			buffer.writeInt(_player.getAllyId());
			buffer.writeInt(_player.getAllyCrestId());
		}
		else
		{
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
		}
		buffer.writeByte(!_player.isSitting());
		buffer.writeByte(_player.isRunning());
		buffer.writeByte(_player.isInCombat());
		buffer.writeByte(!_player.isInOlympiadMode() && _player.isAlikeDead());
		buffer.writeByte(!_gmSeeInvis && _player.isInvisible());
		buffer.writeByte(_player.getMountType().ordinal());
		buffer.writeByte(_player.getPrivateStoreType().getId());
		buffer.writeShort(_player.getCubics().size());
		for (int cubicId : _player.getCubics().keySet())
		{
			buffer.writeShort(cubicId);
		}
		buffer.writeByte(_player.isInPartyMatchRoom());
		buffer.writeInt(_gmSeeInvis ? _player.getAbnormalVisualEffects() | AbnormalVisualEffect.STEALTH.getMask() : _player.getAbnormalVisualEffects());
		buffer.writeByte(_player.isInsideZone(ZoneId.WATER) ? 1 : (_player.isFlyingMounted() ? 2 : 0));
		buffer.writeShort(_player.getRecomHave());
		buffer.writeInt(_player.getMountNpcId() + 1000000);
		buffer.writeInt(_player.getPlayerClass().getId());
		buffer.writeInt(0);
		buffer.writeByte(_player.isMounted() ? 0 : _player.getEnchantEffect());
		buffer.writeByte(_player.getTeam().getId());
		buffer.writeInt(_player.getClanCrestLargeId());
		buffer.writeByte(_player.isNoble());
		buffer.writeByte(_player.isHero() || (_player.isGM() && GeneralConfig.GM_HERO_AURA));
		buffer.writeByte(_player.isFishing());
		buffer.writeInt(_player.getFishX());
		buffer.writeInt(_player.getFishY());
		buffer.writeInt(_player.getFishZ());
		buffer.writeInt(appearance.getNameColor());
		buffer.writeInt(_heading);
		buffer.writeInt(_player.getPledgeClass());
		buffer.writeInt(_player.getPledgeType());
		buffer.writeInt(appearance.getTitleColor());
		buffer.writeInt(_player.isCursedWeaponEquipped() ? CursedWeaponsManager.getInstance().getLevel(_player.getCursedWeaponEquippedId()) : 0);
		buffer.writeInt(_clan != null ? _clan.getReputationScore() : 0);
		buffer.writeInt(_player.getTransformationDisplayId());
		buffer.writeInt(_player.getAgathionId());
		buffer.writeInt(1);
		buffer.writeInt(_player.getAbnormalVisualEffectSpecial());
	}

	@Override
	public int[] getPaperdollOrder()
	{
		return PAPERDOLL_ORDER;
	}
}
