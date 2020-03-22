/*
 * -------------------------------------------------------------------------------------------------------
 * Class: OreDictHandler
 * This class is part of Metallurgy 4 Reforged
 * Complete source code is available at: https://github.com/Davoleo/Metallurgy-4-Reforged
 * This code is licensed under GNU GPLv3
 * Authors: ItHurtsLikeHell & Davoleo
 * Copyright (c) 2020.
 * --------------------------------------------------------------------------------------------------------
 */

package it.hurts.metallurgy_reforged.handler;

import it.hurts.metallurgy_reforged.item.ModItems;
import net.minecraftforge.oredict.OreDictionary;

public class OreDictHandler {

	public static void init()
	{


		//Additional oreDict values
		OreDictionary.registerOre("globTar", ModItems.tar);
	}

}
