/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ai.areas.Gracia;

import java.util.logging.Level;
import java.util.logging.Logger;

import ai.areas.Gracia.AI.EnergySeeds;
import ai.areas.Gracia.AI.Lindvior;
import ai.areas.Gracia.AI.Maguen;
import ai.areas.Gracia.AI.StarStones;
import ai.areas.Gracia.AI.NPC.AbyssGaze.AbyssGaze;
import ai.areas.Gracia.AI.NPC.DestroyedTumors.DestroyedTumors;
import ai.areas.Gracia.AI.NPC.EkimusMouth.EkimusMouth;
import ai.areas.Gracia.AI.NPC.FortuneTelling.FortuneTelling;
import ai.areas.Gracia.AI.NPC.GeneralDilios.GeneralDilios;
import ai.areas.Gracia.AI.NPC.Klemis.Klemis;
import ai.areas.Gracia.AI.NPC.Lekon.Lekon;
import ai.areas.Gracia.AI.NPC.Nemo.Nemo;
import ai.areas.Gracia.AI.NPC.Nottingale.Nottingale;
import ai.areas.Gracia.AI.NPC.Seyo.Seyo;
import ai.areas.Gracia.AI.NPC.ZealotOfShilen.ZealotOfShilen;
import ai.areas.Gracia.AI.SeedOfAnnihilation.SeedOfAnnihilation;
import ai.areas.Gracia.instances.HallOfErosionAttack.HallOfErosionAttack;
import ai.areas.Gracia.instances.HallOfErosionDefence.HallOfErosionDefence;
import ai.areas.Gracia.instances.HallOfSufferingAttack.HallOfSufferingAttack;
import ai.areas.Gracia.instances.HallOfSufferingDefence.HallOfSufferingDefence;
import ai.areas.Gracia.instances.HeartInfinityAttack.HeartInfinityAttack;
import ai.areas.Gracia.instances.HeartInfinityDefence.HeartInfinityDefence;
import ai.areas.Gracia.instances.SecretArea.SecretArea;
import ai.areas.Gracia.instances.SeedOfDestruction.SeedOfDestruction;
import ai.areas.Gracia.instances.SeedOfDestruction.MountedTroops.ChamblainsMountedTroop;
import ai.areas.Gracia.instances.SeedOfDestruction.MountedTroops.GreatWarriorsMountedTroop;
import ai.areas.Gracia.instances.SeedOfDestruction.MountedTroops.SoldiersMountedTroop;
import ai.areas.Gracia.instances.SeedOfDestruction.MountedTroops.WarriorsMountedTroop;
import ai.areas.Gracia.vehicles.AirShipGludioGracia.AirShipGludioGracia;
import ai.areas.Gracia.vehicles.KeucereusNorthController.KeucereusNorthController;
import ai.areas.Gracia.vehicles.KeucereusSouthController.KeucereusSouthController;
import ai.areas.Gracia.vehicles.SoDController.SoDController;
import ai.areas.Gracia.vehicles.SoIController.SoIController;

/**
 * Gracia class-loader.
 * @author Mobius
 */
public class GraciaLoader
{
	private static final Logger LOGGER = Logger.getLogger(GraciaLoader.class.getName());
	
	private static final Class<?>[] SCRIPTS =
	{
		// AIs
		EnergySeeds.class,
		Lindvior.class,
		Maguen.class,
		StarStones.class,
		DestroyedTumors.class,
		
		// NPCs
		AbyssGaze.class,
		EkimusMouth.class,
		FortuneTelling.class,
		GeneralDilios.class,
		Klemis.class,
		Lekon.class,
		Nemo.class,
		Nottingale.class,
		Seyo.class,
		ZealotOfShilen.class,
		
		// Seed of Annihilation
		SeedOfAnnihilation.class,
		
		// Instances
		SecretArea.class,
		SeedOfDestruction.class,
		ChamblainsMountedTroop.class,
		GreatWarriorsMountedTroop.class,
		SoldiersMountedTroop.class,
		WarriorsMountedTroop.class,
		HallOfErosionAttack.class,
		HallOfErosionDefence.class,
		HallOfSufferingAttack.class,
		HallOfSufferingDefence.class,
		HeartInfinityAttack.class,
		HeartInfinityDefence.class,
		
		// Vehicles
		AirShipGludioGracia.class,
		KeucereusNorthController.class,
		KeucereusSouthController.class,
		SoIController.class,
		SoDController.class,
	};
	
	public static void main(String[] args)
	{
		LOGGER.info(GraciaLoader.class.getSimpleName() + ": Loading Gracia related scripts.");
		for (Class<?> script : SCRIPTS)
		{
			try
			{
				script.getDeclaredConstructor().newInstance();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, GraciaLoader.class.getSimpleName() + ": Failed loading " + script.getSimpleName() + ":", e);
			}
		}
	}
}
