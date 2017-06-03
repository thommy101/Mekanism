package mekanism.common.inventory.container;

import mekanism.common.base.IUpgradeItem;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.inventory.slot.SlotMachineUpgrade;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerUpgradeManagement extends Container
{
	private IUpgradeTile tileEntity;

	public ContainerUpgradeManagement(InventoryPlayer inventory, IUpgradeTile tile)
	{
		tileEntity = tile;
		addSlotToContainer(new SlotMachineUpgrade((TileEntityContainerBlock)tile, tileEntity.getComponent().getUpgradeSlot(), 154, 7));
		
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

		((TileEntityContainerBlock)tileEntity).open(inventory.player);
		((TileEntityContainerBlock)tileEntity).openInventory(inventory.player);
	}

	@Override
	public void onContainerClosed(EntityPlayer entityplayer)
	{
		super.onContainerClosed(entityplayer);

		((TileEntityContainerBlock)tileEntity).close(entityplayer);
		((TileEntityContainerBlock)tileEntity).closeInventory(entityplayer);
	}

	@Override
	public boolean canInteractWith(EntityPlayer entityplayer)
	{
		return ((TileEntityContainerBlock)tileEntity).isUsableByPlayer(entityplayer);
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

			if(slotStack.getItem() instanceof IUpgradeItem)
			{
				if(slotID != 0)
				{
					if(!mergeItemStack(slotStack, 0, 1, false))
					{
						return ItemStack.EMPTY;
					}
				}
				else {
					if(!mergeItemStack(slotStack, 1, inventorySlots.size(), true))
					{
						return ItemStack.EMPTY;
					}
				}
			}
			else {
				if(slotID >= 1 && slotID <= 27)
				{
					if(!mergeItemStack(slotStack, 28, inventorySlots.size(), false))
					{
						return ItemStack.EMPTY;
					}
				}
				else if(slotID > 27)
				{
					if(!mergeItemStack(slotStack, 1, 27, false))
					{
						return ItemStack.EMPTY;
					}
				}
				else {
					if(!mergeItemStack(slotStack, 1, inventorySlots.size(), true))
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
