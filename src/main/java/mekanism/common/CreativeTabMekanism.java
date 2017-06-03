package mekanism.common;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class CreativeTabMekanism extends CreativeTabs
{
	public CreativeTabMekanism()
	{
		super("tabMekanism");
	}

	@Override
	public ItemStack getIconItemStack()
	{
		return new ItemStack(MekanismItems.AtomicAlloy);
	}

	@Override
	public ItemStack getTabIconItem() 
	{
		return new ItemStack(MekanismItems.AtomicAlloy);
	}
}
