/*
 * -------------------------------------------------------------------------------------------------------
 * Class: ItemBase
 * This class is part of Metallurgy 4 Reforged
 * Complete source code is available at: https://github.com/Davoleo/Metallurgy-4-Reforged
 * This code is licensed under GNU GPLv3
 * Authors: ItHurtsLikeHell & Davoleo
 * Copyright (c) 2019.
 * --------------------------------------------------------------------------------------------------------
 */

package it.hurts.metallurgy_reforged.item;

import it.hurts.metallurgy_reforged.util.IHasModel;
import it.hurts.metallurgy_reforged.util.ItemUtils;
import it.hurts.metallurgy_reforged.util.MetallurgyTabs;
import net.minecraft.item.Item;

import javax.annotation.Nonnull;

public class ItemBase extends Item implements IHasModel {

	public ItemBase(String name)
    {
		ItemUtils.initItem(this, name, MetallurgyTabs.tabSpecial);
	}

	@Nonnull
	@Override
	public String getCategory()
	{
		return "";
	}
}
