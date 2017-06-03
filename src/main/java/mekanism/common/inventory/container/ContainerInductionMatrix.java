package mekanism.common.inventory.container;

import ic2.api.item.IElectricItem;
import mekanism.common.inventory.slot.SlotEnergy.SlotCharge;
import mekanism.common.inventory.slot.SlotEnergy.SlotDischarge;
import mekanism.common.tile.TileEntityInductionCasing;
import mekanism.common.util.ChargeUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerInductionMatrix extends Container
{
	private TileEntityInductionCasing tileEntity;

	public ContainerInductionMatrix(InventoryPlayer inventory, TileEntityInductionCasing tentity)
	{
		tileEntity = tentity;
		addSlotToContainer(new SlotCharge(tentity, 0, 146, 20));
		addSlotToContainer(new SlotDischarge(tentity, 1, 146, 51));
		
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

			if(ChargeUtils.canBeCharged(slotStack) || ChargeUtils.canBeDischarged(slotStack))
			{
				if(slotStack.getItem() == Items.REDSTONE)
				{
					if(slotID != 1)
					{
						if(!mergeItemStack(slotStack, 1, 2, false))
						{
							return ItemStack.EMPTY;
						}
					}
					else {
						if(!mergeItemStack(slotStack, 2, inventorySlots.size(), true))
						{
							return ItemStack.EMPTY;
						}
					}
				}
				else {
					if(slotID != 1 && slotID != 0)
					{
						if(ChargeUtils.canBeDischarged(slotStack))
						{
							if(!mergeItemStack(slotStack, 1, 2, false))
							{
								if(canTransfer(slotStack))
								{
									if(!mergeItemStack(slotStack, 0, 1, false))
									{
										return ItemStack.EMPTY;
									}
								}
							}
						}
						else if(canTransfer(slotStack))
						{
							if(!mergeItemStack(slotStack, 0, 1, false))
							{
								return ItemStack.EMPTY;
							}
						}
					}
					else if(slotID == 1)
					{
						if(canTransfer(slotStack))
						{
							if(!mergeItemStack(slotStack, 0, 1, false))
							{
								if(!mergeItemStack(slotStack, 2, inventorySlots.size(), true))
								{
									return ItemStack.EMPTY;
								}
							}
						}
						else {
							if(!mergeItemStack(slotStack, 2, inventorySlots.size(), true))
							{
								return ItemStack.EMPTY;
							}
						}
					}
					else if(slotID == 0)
					{
						if(!mergeItemStack(slotStack, 2, inventorySlots.size(), true))
						{
							return ItemStack.EMPTY;
						}
					}
				}
			}
			else {
				if(slotID >= 2 && slotID <= 28)
				{
					if(!mergeItemStack(slotStack, 29, inventorySlots.size(), false))
					{
						return ItemStack.EMPTY;
					}
				}
				else if(slotID > 28)
				{
					if(!mergeItemStack(slotStack, 2, 28, false))
					{
						return ItemStack.EMPTY;
					}
				}
				else {
					if(!mergeItemStack(slotStack, 2, inventorySlots.size(), true))
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
	
	private boolean canTransfer(ItemStack slotStack)
	{
		return MekanismUtils.useIC2() && slotStack.getItem() instanceof IElectricItem;
	}
}
