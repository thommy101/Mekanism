package mekanism.generators.common.tile.reactor;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import mekanism.api.Coord4D;
import mekanism.common.tile.prefab.TileEntityElectricBlock;
import mekanism.generators.common.FusionReactor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;

public abstract class TileEntityReactorBlock extends TileEntityElectricBlock
{
	public FusionReactor fusionReactor;
	
	public boolean attempted;
	
	public boolean changed;

	public TileEntityReactorBlock()
	{
		super("ReactorBlock", 0);
		inventory = NonNullList.withSize(0, ItemStack.EMPTY);
	}
	
	public abstract boolean isFrame();

	public TileEntityReactorBlock(String name, double maxEnergy)
	{
		super(name, maxEnergy);
	}

	public void setReactor(FusionReactor reactor)
	{
		if(reactor != fusionReactor)
		{
			changed = true;
		}
		
		fusionReactor = reactor;
	}

	public FusionReactor getReactor()
	{
		return fusionReactor;
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
		
		if(getReactor() != null)
		{
			getReactor().formMultiblock(false);
		}
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();
		
		if(changed)
		{
			changed = false;
		}
		
		if(!world.isRemote && ticker == 5 && !attempted && (getReactor() == null || !getReactor().isFormed()))
		{
			updateController();
		}
		
		attempted = false;
	}

	@Override
	public EnumSet<EnumFacing> getOutputtingSides()
	{
		return EnumSet.noneOf(EnumFacing.class);
	}

	@Override
	public EnumSet<EnumFacing> getConsumingSides()
	{
		return EnumSet.noneOf(EnumFacing.class);
	}
	
	@Override
	public void onChunkUnload()
	{
		super.onChunkUnload();

		if(!(this instanceof TileEntityReactorController) && getReactor() != null)
		{
			getReactor().formMultiblock(true);
		}
	}
	
	@Override
	public void onAdded()
	{
		super.onAdded();

		if(!world.isRemote)
		{
			if(getReactor() != null)
			{
				getReactor().formMultiblock(false);
			}
			else {
				updateController();
			}
		}
	}
	
	public void updateController()
	{
		if(!(this instanceof TileEntityReactorController))
		{
			TileEntityReactorController found = new ControllerFinder().find();
			
			if(found != null && (found.getReactor() == null || !found.getReactor().isFormed()))
			{
				found.formMultiblock(false);
			}
		}
	}
	
	public class ControllerFinder
	{
		public TileEntityReactorController found;
		
		public Set<Coord4D> iterated = new HashSet<>();
		
		public void loop(Coord4D pos)
		{
			if(iterated.size() > 512 || found != null)
			{
				return;
			}
			
			iterated.add(pos);
			
			for(EnumFacing side : EnumFacing.VALUES)
			{
				Coord4D coord = pos.offset(side);
				
				if(!iterated.contains(coord) && coord.getTileEntity(world) instanceof TileEntityReactorBlock)
				{
					((TileEntityReactorBlock)coord.getTileEntity(world)).attempted = true;
					
					if(coord.getTileEntity(world) instanceof TileEntityReactorController)
					{
						found = (TileEntityReactorController)coord.getTileEntity(world);
						return;
					}
					
					loop(coord);
				}
			}
		}
		
		public TileEntityReactorController find()
		{
			loop(Coord4D.get(TileEntityReactorBlock.this));
			
			return found;
		}
	}
}
