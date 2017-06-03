package mekanism.common.capabilities;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public class ItemCapabilityWrapper implements ICapabilityProvider
{
	private List<ItemCapability> capabilities = new ArrayList<>();
	
	protected final ItemStack itemStack;
	
	public ItemCapabilityWrapper(ItemStack stack, ItemCapability... caps)
	{
		itemStack = stack;
		
		for(ItemCapability c : caps)
		{
			c.wrapper = this;
			capabilities.add(c);
		}
	}
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) 
	{
		for(ItemCapability cap : capabilities)
		{
			if(cap.canProcess(capability))
			{
				return true;
			}
		}
		
		return false;
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing)
	{
		for(ItemCapability cap : capabilities)
		{
			if(cap.canProcess(capability))
			{
				return (T)cap;
			}
		}
		
		return null;
	}
	
	public static abstract class ItemCapability
	{
		private ItemCapabilityWrapper wrapper;
		
		public abstract boolean canProcess(Capability capability);
		
		public ItemStack getStack()
		{
			return wrapper.itemStack;
		}
	}
}
