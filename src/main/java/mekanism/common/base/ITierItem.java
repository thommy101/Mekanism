package mekanism.common.base;

import mekanism.common.Tier.BaseTier;
import net.minecraft.item.ItemStack;

public interface ITierItem 
{
	BaseTier getBaseTier(ItemStack stack);
	
	void setBaseTier(ItemStack stack, BaseTier tier);
}
