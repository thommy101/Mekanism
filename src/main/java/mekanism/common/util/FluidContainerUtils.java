package mekanism.common.util;

import mekanism.common.tile.prefab.TileEntityContainerBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.ItemHandlerHelper;

public final class FluidContainerUtils 
{
	public static boolean isFluidContainer(ItemStack stack)
	{
		return !stack.isEmpty() && stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
	}
	
	public static FluidStack extractFluid(FluidTank tileTank, TileEntityContainerBlock tile, int slotID)
	{
		return extractFluid(tileTank, tile, slotID, FluidChecker.check(tileTank.getFluid()));
	}
	
	public static FluidStack extractFluid(FluidTank tileTank, TileEntityContainerBlock tile, int slotID, FluidChecker checker)
	{
		FluidStack ret = extractFluid(tileTank.getCapacity()-tileTank.getFluidAmount(), tile.inventory.get(slotID), checker);
		tile.inventory.set(slotID, FluidUtil.getFluidHandler(tile.inventory.get(slotID)).getContainer());
		return ret;
	}
	
	public static FluidStack extractFluid(int needed, ItemStack container, FluidChecker checker)
	{
		IFluidHandlerItem handler = FluidUtil.getFluidHandler(container);
		
		if(handler == null || FluidUtil.getFluidContained(container) == null)
		{
			return null;
		}
		
		if(checker != null && !checker.isValid(FluidUtil.getFluidContained(container).getFluid()))
		{
			return null;
		}

        return handler.drain(needed, true);
	}
	
	public static int insertFluid(FluidTank tileTank, ItemStack container)
	{
		return insertFluid(tileTank.getFluid(), container);
	}
	
	public static int insertFluid(FluidStack fluid, ItemStack container)
	{
		IFluidHandler handler = FluidUtil.getFluidHandler(container);
		
		if(fluid == null)
		{
			return 0;
		}
		
		return handler.fill(fluid, true);
	}
	
	public static void handleContainerItemFill(TileEntityContainerBlock tileEntity, FluidTank tank, int inSlot, int outSlot)
	{
		tank.setFluid(handleContainerItemFill(tileEntity, tileEntity.inventory, tank.getFluid(), inSlot, outSlot));
	}
	
	public static FluidStack handleContainerItemFill(TileEntity tileEntity, NonNullList<ItemStack> inventory, FluidStack stack, int inSlot, int outSlot)
	{
		if(stack != null)
		{
			ItemStack inputCopy = StackUtils.size(inventory.get(inSlot).copy(), 1);
			
			int drained = insertFluid(stack, inputCopy);
			inputCopy = FluidUtil.getFluidHandler(inputCopy).getContainer();
			
			if(!inventory.get(outSlot).isEmpty() && (!ItemHandlerHelper.canItemStacksStack(inventory.get(outSlot), inputCopy) || inventory.get(outSlot).getCount() == inventory.get(outSlot).getMaxStackSize()))
			{
				return stack;
			}
			
			stack.amount -= drained;
			
			if(inventory.get(outSlot).isEmpty())
			{
				inventory.set(outSlot, inputCopy);
			}
			else if(ItemHandlerHelper.canItemStacksStack(inventory.get(outSlot), inputCopy))
			{
				inventory.get(outSlot).grow(1);
			}
			
			inventory.get(inSlot).shrink(1);
			
			tileEntity.markDirty();
		}
		
		return stack;
	}
	
	public static void handleContainerItemEmpty(TileEntityContainerBlock tileEntity, FluidTank tank, int inSlot, int outSlot)
	{
		handleContainerItemEmpty(tileEntity, tank, inSlot, outSlot, null);
	}
	
	public static void handleContainerItemEmpty(TileEntityContainerBlock tileEntity, FluidTank tank, int inSlot, int outSlot, FluidChecker checker)
	{
		tank.setFluid(handleContainerItemEmpty(tileEntity, tileEntity.inventory, tank.getFluid(), tank.getCapacity()-tank.getFluidAmount(), inSlot, outSlot, checker));
	}
	
	public static FluidStack handleContainerItemEmpty(TileEntity tileEntity, NonNullList<ItemStack> inventory, FluidStack stored, int needed, int inSlot, int outSlot, final FluidChecker checker)
	{
		final Fluid storedFinal = stored != null ? stored.getFluid() : null;
		final ItemStack input = StackUtils.size(inventory.get(inSlot).copy(), 1);
		
		FluidStack ret = extractFluid(needed, input, new FluidChecker() {
			@Override
			public boolean isValid(Fluid f)
			{
				return (checker == null || checker.isValid(f)) && (storedFinal == null || storedFinal == f);
			}
		});
		
		ItemStack inputCopy = FluidUtil.getFluidHandler(input).getContainer();
		
		if(FluidUtil.getFluidContained(inputCopy) == null && !inputCopy.isEmpty())
		{
			if(!inventory.get(outSlot).isEmpty() && (!ItemHandlerHelper.canItemStacksStack(inventory.get(outSlot), inputCopy) || inventory.get(outSlot).getCount() == inventory.get(outSlot).getMaxStackSize()))
			{
				return stored;
			}
		}
		
		if(ret != null)
		{
			if(stored == null)
			{
				stored = ret;
			}
			else {
				stored.amount += ret.amount;
			}
			
			needed -= ret.amount;
			
			tileEntity.markDirty();
		}
		
		if(FluidUtil.getFluidContained(inputCopy) == null || needed == 0)
		{
			if(!inputCopy.isEmpty())
			{
				if(inventory.get(outSlot).isEmpty())
				{
					inventory.set(outSlot, inputCopy);
				}
				else if(ItemHandlerHelper.canItemStacksStack(inventory.get(outSlot), inputCopy))
				{
					inventory.get(outSlot).grow(1);
				}
			}
			
			inventory.get(inSlot).shrink(1);
			
			tileEntity.markDirty();
		}
		else {
			inventory.set(inSlot, inputCopy);
		}
		
		return stored;
	}
	
	public static void handleContainerItem(TileEntityContainerBlock tileEntity, ContainerEditMode editMode, FluidTank tank, int inSlot, int outSlot)
	{
		handleContainerItem(tileEntity, editMode, tank, inSlot, outSlot, null);
	}
	
	public static void handleContainerItem(TileEntityContainerBlock tileEntity, ContainerEditMode editMode, FluidTank tank, int inSlot, int outSlot, FluidChecker checker)
	{
		tank.setFluid(handleContainerItem(tileEntity, tileEntity.inventory, editMode, tank.getFluid(), tank.getCapacity()-tank.getFluidAmount(), inSlot, outSlot, checker));
	}
	
	public static FluidStack handleContainerItem(TileEntity tileEntity, NonNullList<ItemStack> inventory, ContainerEditMode editMode, FluidStack stack, int needed, int inSlot, int outSlot, final FluidChecker checker)
	{
		FluidStack fluidStack = FluidUtil.getFluidContained(inventory.get(inSlot));
		
		if(editMode == ContainerEditMode.FILL || (editMode == ContainerEditMode.BOTH && fluidStack == null))
		{
			return handleContainerItemFill(tileEntity, inventory, stack, inSlot, outSlot);
		}
		else if(editMode == ContainerEditMode.EMPTY || (editMode == ContainerEditMode.BOTH && fluidStack != null))
		{
			return handleContainerItemEmpty(tileEntity, inventory, stack, needed, inSlot, outSlot, checker);
		}
		
		return stack;
	}
	
	public enum ContainerEditMode
	{
		BOTH("fluidedit.both"),
		FILL("fluidedit.fill"),
		EMPTY("fluidedit.empty");
		
		private String display;

		public String getDisplay()
		{
			return LangUtils.localize(display);
		}

		ContainerEditMode(String s)
		{
			display = s;
		}
	}
	
	public static class FluidChecker
	{
		public boolean isValid(Fluid f)
		{
			return true;
		}
		
		public static FluidChecker check(FluidStack fluid)
		{
			final Fluid type = fluid != null ? fluid.getFluid() : null;
			
			return new FluidChecker() {
				@Override
				public boolean isValid(Fluid f)
				{
					return type == null || type == f;
				}
			};
		}
		
		public static FluidChecker check(final Fluid type)
		{
			return new FluidChecker() {
				@Override
				public boolean isValid(Fluid f)
				{
					return type == null || type == f;
				}
			};
		}
	}
}
