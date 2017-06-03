package mekanism.common.inventory.container;

import mekanism.common.inventory.slot.SlotEnergy.SlotDischarge;
import mekanism.common.inventory.slot.SlotOutput;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.tile.TileEntityPRC;
import mekanism.common.util.ChargeUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerPRC extends Container
{
	private TileEntityPRC tileEntity;

	public ContainerPRC(InventoryPlayer inventory, TileEntityPRC tentity)
	{
		tileEntity = tentity;
		addSlotToContainer(new Slot(tentity, 0, 54, 35));
		addSlotToContainer(new SlotDischarge(tentity, 1, 141, 19));
		addSlotToContainer(new SlotOutput(tentity, 2, 116, 35));
		
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

			if(slotID == 2)
			{
				if(!mergeItemStack(slotStack, 3, inventorySlots.size(), true))
				{
					return ItemStack.EMPTY;
				}
			}
			else if(ChargeUtils.canBeDischarged(slotStack))
			{
				if(slotID != 1)
				{
					if(!mergeItemStack(slotStack, 1, 2, false))
					{
						return ItemStack.EMPTY;
					}
				}
				else if(slotID == 1)
				{
					if(!mergeItemStack(slotStack, 3, inventorySlots.size(), true))
					{
						return ItemStack.EMPTY;
					}
				}
			}
			else if(RecipeHandler.isInPressurizedRecipe(slotStack))
			{
				if(slotID != 0)
				{
					if(!mergeItemStack(slotStack, 0, 1, false))
					{
						return ItemStack.EMPTY;
					}
				}
				else {
					if(!mergeItemStack(slotStack, 3, inventorySlots.size(), true))
					{
						return ItemStack.EMPTY;
					}
				}
			}
			else {
				if(slotID >= 3 && slotID <= 29)
				{
					if(!mergeItemStack(slotStack, 30, inventorySlots.size(), false))
					{
						return ItemStack.EMPTY;
					}
				}
				else if(slotID > 29)
				{
					if(!mergeItemStack(slotStack, 3, 29, false))
					{
						return ItemStack.EMPTY;
					}
				}
				else {
					if(!mergeItemStack(slotStack, 3, inventorySlots.size(), true))
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
