package mekanism.common.tile;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.IConfigurable;
import mekanism.common.Upgrade;
import mekanism.common.base.FluidHandlerWrapper;
import mekanism.common.base.IFluidHandlerWrapper;
import mekanism.common.base.IRedstoneControl;
import mekanism.common.base.ISustainedTank;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig.general;
import mekanism.common.config.MekanismConfig.usage;
import mekanism.common.integration.computer.IComputerIntegration;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.prefab.TileEntityElectricBlock;
import mekanism.common.util.ChargeUtils;
import mekanism.common.util.FluidContainerUtils;
import mekanism.common.util.FluidContainerUtils.FluidChecker;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.PipeUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class TileEntityFluidicPlenisher extends TileEntityElectricBlock implements IComputerIntegration, IConfigurable, IFluidHandlerWrapper, ISustainedTank, IUpgradeTile, IRedstoneControl, ISecurityTile
{
	public Set<Coord4D> activeNodes = new LinkedHashSet<>();
	public Set<Coord4D> usedNodes = new HashSet<>();
	
	public boolean finishedCalc = false;
	
	public FluidTank fluidTank = new FluidTank(10000);
	
	/** How much energy this machine consumes per-tick. */
	public double BASE_ENERGY_PER_TICK = usage.fluidicPlenisherUsage;

	public double energyPerTick = BASE_ENERGY_PER_TICK;

	/** How many ticks it takes to run an operation. */
	public int BASE_TICKS_REQUIRED = 20;

	public int ticksRequired = BASE_TICKS_REQUIRED;
	
	/** How many ticks this machine has been operating for. */
	public int operatingTicks;
	
	public RedstoneControl controlType = RedstoneControl.DISABLED;
	
	private static EnumSet<EnumFacing> dirs = EnumSet.complementOf(EnumSet.of(EnumFacing.UP));
	
	public TileComponentUpgrade upgradeComponent = new TileComponentUpgrade(this, 3);
	public TileComponentSecurity securityComponent = new TileComponentSecurity(this);
	
	public TileEntityFluidicPlenisher()
	{
		super("FluidicPlenisher", MachineType.FLUIDIC_PLENISHER.baseEnergy);
		inventory = NonNullList.withSize(4, ItemStack.EMPTY);
	}
	
	@Override
	public void onUpdate()
	{
		if(!world.isRemote)
		{
			ChargeUtils.discharge(2, this);
			
			if(FluidContainerUtils.isFluidContainer(inventory.get(0)))
			{
				FluidContainerUtils.handleContainerItemEmpty(this, fluidTank, 0, 1, new FluidChecker() {
					@Override
					public boolean isValid(Fluid f)
					{
						return f.canBePlacedInWorld();
					}
				});
			}
			
			if(MekanismUtils.canFunction(this) && getEnergy() >= energyPerTick && fluidTank.getFluid() != null && fluidTank.getFluid().getFluid().canBePlacedInWorld())
			{
				if(!finishedCalc)
				{
					setEnergy(getEnergy() - energyPerTick);
				}
				
				if((operatingTicks + 1) < ticksRequired)
				{
					operatingTicks++;
				} 
				else {
					if(!finishedCalc)
					{
						doPlenish();
					}
					else {
						Coord4D below = Coord4D.get(this).offset(EnumFacing.DOWN);
						
						if(canReplace(below, false, false) && fluidTank.getFluidAmount() >= Fluid.BUCKET_VOLUME)
						{
							if(fluidTank.getFluid().getFluid().canBePlacedInWorld())
							{
								world.setBlockState(below.getPos(), MekanismUtils.getFlowingBlock(fluidTank.getFluid().getFluid()).getDefaultState(), 3);
								
								setEnergy(getEnergy() - energyPerTick);
								fluidTank.drain(Fluid.BUCKET_VOLUME, true);
							}
						}
					}
					
					operatingTicks = 0;
				}
			}
		}
	}
	
	private void doPlenish()
	{
		if(usedNodes.size() >= general.maxPlenisherNodes)
		{
			finishedCalc = true;
			return;
		}
		
		if(activeNodes.isEmpty())
		{
			if(usedNodes.isEmpty())
			{
				Coord4D below = Coord4D.get(this).offset(EnumFacing.DOWN);
				
				if(!canReplace(below, true, true))
				{
					finishedCalc = true;
					return;
				}

				activeNodes.add(below);
			}
			else {
				finishedCalc = true;
				return;
			}
		}
		
		Set<Coord4D> toRemove = new HashSet<>();
		
		for(Coord4D coord : activeNodes)
		{
			if(coord.exists(world))
			{
				if(canReplace(coord, true, false))
				{
					world.setBlockState(coord.getPos(), MekanismUtils.getFlowingBlock(fluidTank.getFluid().getFluid()).getDefaultState(), 3);

					fluidTank.drain(Fluid.BUCKET_VOLUME, true);
				}
				
				for(EnumFacing dir : dirs)
				{
					Coord4D sideCoord = coord.offset(dir);
					
					if(sideCoord.exists(world) && canReplace(sideCoord, true, true))
					{
						activeNodes.add(sideCoord);
					}
				}
				
				toRemove.add(coord);
				break;
			}
			else {
				toRemove.add(coord);
			}
		}

		usedNodes.addAll(toRemove);
		activeNodes.removeAll(toRemove);
	}
	
	public boolean canReplace(Coord4D coord, boolean checkNodes, boolean isPathfinding)
	{
		if(checkNodes && usedNodes.contains(coord))
		{
			return false;
		}
		
		if(coord.isAirBlock(world) || MekanismUtils.isDeadFluid(world, coord))
		{
			return true;
		}
		
		if(MekanismUtils.isFluid(world, coord))
		{
			return isPathfinding;
		}
		
		return coord.getBlock(world).isReplaceable(world, coord.getPos());
	}
	
	@Override
	public void handlePacketData(ByteBuf dataStream)
	{
		super.handlePacketData(dataStream);
		
		if(FMLCommonHandler.instance().getEffectiveSide().isClient())
		{
			finishedCalc = dataStream.readBoolean();
			controlType = RedstoneControl.values()[dataStream.readInt()];
	
			if(dataStream.readInt() == 1)
			{
				fluidTank.setFluid(new FluidStack(FluidRegistry.getFluid(ByteBufUtils.readUTF8String(dataStream)), dataStream.readInt()));
			}
			else {
				fluidTank.setFluid(null);
			}
		}
	}

	@Override
	public ArrayList<Object> getNetworkedData(ArrayList<Object> data)
	{
		super.getNetworkedData(data);
		
		data.add(finishedCalc);
		data.add(controlType.ordinal());

		if(fluidTank.getFluid() != null)
		{
			data.add(1);
			data.add(FluidRegistry.getFluidName(fluidTank.getFluid()));
			data.add(fluidTank.getFluid().amount);
		}
		else {
			data.add(0);
		}

		return data;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbtTags)
	{
		super.writeToNBT(nbtTags);
		
		nbtTags.setInteger("operatingTicks", operatingTicks);
		nbtTags.setBoolean("finishedCalc", finishedCalc);
		nbtTags.setInteger("controlType", controlType.ordinal());

		if(fluidTank.getFluid() != null)
		{
			nbtTags.setTag("fluidTank", fluidTank.writeToNBT(new NBTTagCompound()));
		}
		
		NBTTagList activeList = new NBTTagList();

		for(Coord4D wrapper : activeNodes)
		{
			NBTTagCompound tagCompound = new NBTTagCompound();
			wrapper.write(tagCompound);
			activeList.appendTag(tagCompound);
		}

		if(activeList.tagCount() != 0)
		{
			nbtTags.setTag("activeNodes", activeList);
		}

		NBTTagList usedList = new NBTTagList();

		for(Coord4D obj : usedNodes)
		{
			activeList.appendTag(obj.write(new NBTTagCompound()));
		}

		if(activeList.tagCount() != 0)
		{
			nbtTags.setTag("usedNodes", usedList);
		}
		
		return nbtTags;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbtTags)
	{
		super.readFromNBT(nbtTags);
		
		operatingTicks = nbtTags.getInteger("operatingTicks");
		finishedCalc = nbtTags.getBoolean("finishedCalc");
		controlType = RedstoneControl.values()[nbtTags.getInteger("controlType")];

		if(nbtTags.hasKey("fluidTank"))
		{
			fluidTank.readFromNBT(nbtTags.getCompoundTag("fluidTank"));
		}
		
		if(nbtTags.hasKey("activeNodes"))
		{
			NBTTagList tagList = nbtTags.getTagList("activeNodes", NBT.TAG_COMPOUND);

			for(int i = 0; i < tagList.tagCount(); i++)
			{
				activeNodes.add(Coord4D.read(tagList.getCompoundTagAt(i)));
			}
		}

		if(nbtTags.hasKey("usedNodes"))
		{
			NBTTagList tagList = nbtTags.getTagList("usedNodes", NBT.TAG_COMPOUND);

			for(int i = 0; i < tagList.tagCount(); i++)
			{
				usedNodes.add(Coord4D.read(tagList.getCompoundTagAt(i)));
			}
		}
	}

	@Override
	public boolean isItemValidForSlot(int slotID, ItemStack itemstack)
	{
		if(slotID == 1)
		{
			return false;
		}
		else if(slotID == 0)
		{
			return FluidUtil.getFluidContained(itemstack) != null && FluidUtil.getFluidContained(itemstack).getFluid().canBePlacedInWorld();
		}
		else if(slotID == 2)
		{
			return ChargeUtils.canBeDischarged(itemstack);
		}

		return false;
	}

	@Override
	public boolean canExtractItem(int slotID, ItemStack itemstack, EnumFacing side)
	{
		if(slotID == 2)
		{
			return ChargeUtils.canBeOutputted(itemstack, false);
		}
		else if(slotID == 1)
		{
			return true;
		}

		return false;
	}

	@Override
	public boolean sideIsConsumer(EnumFacing side)
	{
		return facing.getOpposite() == side;
	}

	@Override
	public boolean canSetFacing(int side)
	{
		return side != 0 && side != 1;
	}

	@Override
	public int[] getSlotsForFace(EnumFacing side)
	{
		if(side == EnumFacing.UP)
		{
			return new int[] {0};
		}
		else if(side == EnumFacing.DOWN)
		{
			return new int[] {1};
		}
		else {
			return new int[] {2};
		}
	}

	@Override
	public FluidTankInfo[] getTankInfo(EnumFacing direction)
	{
		if(direction == EnumFacing.UP)
		{
			return new FluidTankInfo[] {fluidTank.getInfo()};
		}

		return PipeUtils.EMPTY;
	}

	@Override
	public void setFluidStack(FluidStack fluidStack, Object... data)
	{
		fluidTank.setFluid(fluidStack);
	}

	@Override
	public FluidStack getFluidStack(Object... data)
	{
		return fluidTank.getFluid();
	}

	@Override
	public boolean hasTank(Object... data)
	{
		return true;
	}

	@Override
	public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain)
	{
		if(fluidTank.getFluid() != null && fluidTank.getFluid().getFluid() == resource.getFluid() && from == EnumFacing.UP)
		{
			return drain(from, resource.amount, doDrain);
		}

		return null;
	}

	@Override
	public int fill(EnumFacing from, FluidStack resource, boolean doFill)
	{
		if(from == EnumFacing.UP && resource.getFluid().canBePlacedInWorld())
		{
			return fluidTank.fill(resource, true);
		}
		
		return 0;
	}

	@Override
	public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain)
	{
		return null;
	}

	@Override
	public boolean canFill(EnumFacing from, Fluid fluid)
	{
		return from == EnumFacing.UP && fluid.canBePlacedInWorld();
	}

	@Override
	public boolean canDrain(EnumFacing from, Fluid fluid)
	{
		return false;
	}
	
	@Override
	public EnumActionResult onSneakRightClick(EntityPlayer player, EnumFacing side)
	{
		activeNodes.clear();
		usedNodes.clear();
		finishedCalc = false;
		
		player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + "[Mekanism] " + EnumColor.GREY + LangUtils.localize("tooltip.mekanism.configurator.plenisherReset")));

		return EnumActionResult.SUCCESS;
	}

	@Override
	public EnumActionResult onRightClick(EntityPlayer player, EnumFacing side)
	{
		return EnumActionResult.PASS;
	}
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing side)
	{
		return capability == Capabilities.CONFIGURABLE_CAPABILITY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY 
				|| super.hasCapability(capability, side);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing side)
	{
		if(capability == Capabilities.CONFIGURABLE_CAPABILITY)
		{
			return (T)this;
		}
		
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
		{
			return (T)new FluidHandlerWrapper(this, side);
		}
		
		return super.getCapability(capability, side);
	}

	private static final String[] methods = new String[] {"reset"};

	@Override
	public String[] getMethods()
	{
		return methods;
	}

	@Override
	public Object[] invoke(int method, Object[] arguments) throws Exception
    {
        switch(method)
        {
            case 0:
                activeNodes.clear();
                usedNodes.clear();
                finishedCalc = false;

                return new Object[]{"Plenisher calculation reset."};
            default:
                throw new NoSuchMethodException();
        }
    }
	
	@Override
	public TileComponentUpgrade getComponent() 
	{
		return upgradeComponent;
	}
	
	@Override
	public void recalculateUpgradables(Upgrade upgrade)
	{
		super.recalculateUpgradables(upgrade);

		switch(upgrade)
		{
			case SPEED:
				ticksRequired = MekanismUtils.getTicks(this, BASE_TICKS_REQUIRED);
			case ENERGY:
				energyPerTick = MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_PER_TICK);
				maxEnergy = MekanismUtils.getMaxEnergy(this, BASE_MAX_ENERGY);
				setEnergy(Math.min(getMaxEnergy(), getEnergy()));
			default:
				break;
		}
	}
	
	@Override
	public RedstoneControl getControlType()
	{
		return controlType;
	}

	@Override
	public void setControlType(RedstoneControl type)
	{
		controlType = type;
	}

	@Override
	public boolean canPulse()
	{
		return false;
	}
	
	@Override
	public TileComponentSecurity getSecurity()
	{
		return securityComponent;
	}
}
