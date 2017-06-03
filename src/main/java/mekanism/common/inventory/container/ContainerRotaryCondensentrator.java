package mekanism.common.inventory.container;

import mekanism.api.gas.IGasItem;
import mekanism.common.inventory.slot.SlotEnergy.SlotDischarge;
import mekanism.common.inventory.slot.SlotOutput;
import mekanism.common.inventory.slot.SlotStorageTank;
import mekanism.common.tile.TileEntityRotaryCondensentrator;
import mekanism.common.util.ChargeUtils;
import mekanism.common.util.FluidContainerUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerRotaryCondensentrator extends Container
{
	private TileEntityRotaryCondensentrator tileEntity;

	public ContainerRotaryCondensentrator(InventoryPlayer inventory, TileEntityRotaryCondensentrator tentity)
	{
		tileEntity = tentity;
		addSlotToContainer(new SlotStorageTank(tentity, 0, 5, 25));
		addSlotToContainer(new SlotStorageTank(tentity, 1, 5, 56));
		addSlotToContainer(new Slot(tentity, 2, 155, 25));
		addSlotToContainer(new SlotOutput(tentity, 3, 155, 56));
		addSlotToContainer(new SlotDischarge(tentity, 4, 155, 5));

		int slotY;

		for(slotY = 0; slotY < 3; slotY++)
		{
			for(int slotX = 0; slotX < 9; slotX++)
			{
				addSlotToContainer(new Slot(inventory, slotX + slotY * 9 + 9, 8 + slotX * 18, 84 + slotY * 18));
			}
		}

		for(slotY = 0; slotY < 9; slotY++)
		{
			addSlotToContainer(new Slot(inventory, slotY, 8 + slotY * 18, 142));
		}

		tileEntity.open(inventory.player);
		tileEntity.openInventory(inventory.player);
	}

	@Override
	public void onContainerClosed(EntityPlayer entityplayer)
	{
		super.onContainerClosed(entityplayer);

		tileEntity.close(entityplayer);
		tileEntity.closeInventory(entityplayer);
	}

	@Override
	public boolean canInteractWith(EntityPlayer entityplayer)
	{
		return tileEntity.isUsableByPlayer(entityplayer);
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slotID)
	{
		ItemStack stack = ItemStack.EMPTY;
		Slot currentSlot = inventorySlots.get(slotID);

		if(currentSlot != null && currentSlot.getHasStack())
		{
			ItemStack slotStack = currentSlot.getStack();
			stack = slotStack.copy();

			if(ChargeUtils.canBeDischarged(slotStack))
			{
				if(slotID != 4)
				{
					if(!mergeItemStack(slotStack, 4, 5, false))
					{
						return ItemStack.EMPTY;
					}
				}
				else {
					if(!mergeItemStack(slotStack, 5, inventorySlots.size(), true))
					{
						return ItemStack.EMPTY;
					}
				}
			}
			else if(FluidContainerUtils.isFluidContainer(slotStack))
			{
				if(slotID != 2 && slotID != 3)
				{
					if(!mergeItemStack(slotStack, 2, 3, false))
					{
						return ItemStack.EMPTY;
					}
				}
				else {
					if(!mergeItemStack(slotStack, 5, inventorySlots.size(), true))
					{
						return ItemStack.EMPTY;
					}
				}
			}
			else if(slotStack.getItem() instanceof IGasItem)
			{
				if(slotID != 0 && slotID != 1)
				{
					if(((IGasItem)slotStack.getItem()).canProvideGas(slotStack, tileEntity.gasTank.getGas() != null ? tileEntity.gasTank.getGas().getGas() : null))
					{
						if(!mergeItemStack(slotStack, 0, 1, false))
						{
							return ItemStack.EMPTY;
						}
					}
					else if(((IGasItem)slotStack.getItem()).canReceiveGas(slotStack, tileEntity.gasTank.getGas() != null ? tileEntity.gasTank.getGas().getGas() : null))
					{
						if(!mergeItemStack(slotStack, 1, 2, false))
						{
							return ItemStack.EMPTY;
						}
					}
				}
				else {
					if(!mergeItemStack(slotStack, 5, inventorySlots.size(), true))
					{
						return ItemStack.EMPTY;
					}
				}
			}
			else {
				if(slotID >= 5 && slotID <= 31)
				{
					if(!mergeItemStack(slotStack, 32, inventorySlots.size(), false))
					{
						return ItemStack.EMPTY;
					}
				}
				else if(slotID > 31)
				{
					if(!mergeItemStack(slotStack, 5, 31, false))
					{
						return ItemStack.EMPTY;
					}
				}
				else {
					if(!mergeItemStack(slotStack, 5, inventorySlots.size(), true))
					{
						return ItemStack.EMPTY;
					}
				}
			}

			if(slotStack.getCount() == 0)
			{
				currentSlot.putStack(ItemStack.EMPTY);
			}
			else {
				currentSlot.onSlotChanged();
			}

			if(slotStack.getCount() == stack.getCount())
			{
				return ItemStack.EMPTY;
			}

			currentSlot.onTake(player, slotStack);
		}

		return stack;
	}
}
