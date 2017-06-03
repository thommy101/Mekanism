package mekanism.common.recipe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mekanism.common.Mekanism;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.RecipeUtils;
import net.minecraft.block.Block;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.oredict.OreDictionary;

public class ShapelessMekanismRecipe implements IRecipe
{
    private ItemStack output = ItemStack.EMPTY;
    private ArrayList<Object> input = new ArrayList<>();

    public ShapelessMekanismRecipe(Block result, Object... recipe){ this(new ItemStack(result), recipe); }
    public ShapelessMekanismRecipe(Item  result, Object... recipe){ this(new ItemStack(result), recipe); }

    public ShapelessMekanismRecipe(ItemStack result, Object... recipe)
    {
        output = result.copy();
        
        for(Object obj : recipe)
        {
            if(obj instanceof ItemStack)
            {
                input.add(((ItemStack)obj).copy());
            }
            else if(obj instanceof Item)
            {
                input.add(new ItemStack((Item)obj));
            }
            else if(obj instanceof Block)
            {
                input.add(new ItemStack((Block)obj));
            }
            else if(obj instanceof String)
            {
                input.add(OreDictionary.getOres((String)obj));
            }
            else {
                String ret = "Invalid shapeless Mekanism recipe: ";
                
                for(Object tmp :  recipe)
                {
                    ret += tmp + ", ";
                }
                
                ret += output;
                throw new RuntimeException(ret);
            }
        }
    }

    @Override
    public int getRecipeSize()
    { 
    	return input.size();
    }

    @Override
    public ItemStack getRecipeOutput()
    { 
    	return output;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv)
    {
        return ForgeHooks.defaultRecipeGetRemainingItems(inv);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv)
    { 
    	return RecipeUtils.getCraftingResult(inv, output.copy());
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(InventoryCrafting inv, World world)
    {
        ArrayList<Object> required = new ArrayList<>(input);

        for(int x = 0; x < inv.getSizeInventory(); x++)
        {
            ItemStack slot = inv.getStackInSlot(x);

            if(!slot.isEmpty())
            {
                boolean inRecipe = false;

                for (Object aRequired : required) {
                    boolean match = false;

                    if (aRequired instanceof ItemStack) {
                        match = RecipeUtils.areItemsEqualForCrafting((ItemStack) aRequired, slot);
                    } else if (aRequired instanceof List) {
                        Iterator<ItemStack> itr = ((List<ItemStack>) aRequired).iterator();

                        while (itr.hasNext() && !match) {
                            match = RecipeUtils.areItemsEqualForCrafting(itr.next(), slot);
                        }
                    }

                    if (match) {
                        inRecipe = true;
                        required.remove(aRequired);

                        break;
                    }
                }

                if(!inRecipe)
                {
                    return false;
                }
            }
        }

        return required.isEmpty();
    }

    public ArrayList<Object> getInput()
    {
        return input;
    }
    
    public static ShapelessMekanismRecipe create(NBTTagCompound nbtTags)
    {
    	if(!nbtTags.hasKey("result") || !nbtTags.hasKey("input"))
    	{
    		Mekanism.logger.error("[Mekanism] Shapeless recipe parse error: missing input or result compound tag.");
    		return null;
    	}
    	
    	ItemStack result = InventoryUtils.loadFromNBT(nbtTags.getCompoundTag("result"));
    	NBTTagList list = nbtTags.getTagList("input", NBT.TAG_COMPOUND);
    	
    	if(result.isEmpty() || list.tagCount() == 0)
    	{
    		Mekanism.logger.error("[Mekanism] Shapeless recipe parse error: invalid result stack or input data list.");
    		return null;
    	}
    	
    	Object[] ret = new Object[list.tagCount()];
    	
    	for(int i = 0; i < list.tagCount(); i++)
    	{
    		NBTTagCompound compound = list.getCompoundTagAt(i);
    		
    		if(compound.hasKey("oredict"))
    		{
    			ret[i] = compound.getString("oredict");
    		}
    		else if(compound.hasKey("itemstack"))
    		{
    			ret[i] = InventoryUtils.loadFromNBT(compound.getCompoundTag("itemstack"));
    		}
    		else {
    			Mekanism.logger.error("[Mekanism] Shapeless recipe parse error: invalid input tag data key.");
    			return null;
    		}
    	}
    	
    	return new ShapelessMekanismRecipe(result, ret);
    }
}
