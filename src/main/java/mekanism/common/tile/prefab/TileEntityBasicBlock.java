package mekanism.common.tile.prefab;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import mekanism.api.Coord4D;
import mekanism.api.Range4D;
import mekanism.common.Mekanism;
import mekanism.common.base.IChunkLoadHandler;
import mekanism.common.base.ITileComponent;
import mekanism.common.base.ITileNetwork;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig.general;
import mekanism.common.frequency.Frequency;
import mekanism.common.frequency.FrequencyManager;
import mekanism.common.frequency.IFrequencyHandler;
import mekanism.common.network.PacketDataRequest.DataRequestMessage;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.security.ISecurityTile;
import mekanism.common.util.MekanismUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Optional.Interface;

@Interface(iface = "ic2.api.tile.IWrenchable", modid = "IC2")
public abstract class TileEntityBasicBlock extends TileEntity implements ITileNetwork, IChunkLoadHandler, IFrequencyHandler, ITickable
{
	/** The direction this block is facing. */
	public EnumFacing facing = EnumFacing.NORTH;

	public EnumFacing clientFacing = facing;

	/** The players currently using this block. */
	public HashSet<EntityPlayer> playersUsing = new HashSet<>();

	/** A timer used to send packets to clients. */
	public int ticker;

	public boolean redstone = false;
	public boolean redstoneLastTick = false;

	public boolean doAutoSync = true;

	public List<ITileComponent> components = new ArrayList<>();

	@Override
	public void update()
	{
		if(!world.isRemote && general.destroyDisabledBlocks)
		{
			MachineType type = BlockStateMachine.MachineType.get(getBlockType(), getBlockMetadata());
			
			if(type != null && !type.isEnabled())
			{
				Mekanism.logger.info("[Mekanism] Destroying machine of type '" + type.blockName + "' at coords " + Coord4D.get(this) + " as according to config.");
				world.setBlockToAir(getPos());
				return;
			}
		}
		
		for(ITileComponent component : components)
		{
			component.tick();
		}

		onUpdate();

		if(!world.isRemote)
		{
			if(doAutoSync && playersUsing.size() > 0)
			{
				for(EntityPlayer player : playersUsing)
				{
					Mekanism.packetHandler.sendTo(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList<>())), (EntityPlayerMP)player);
				}
			}
		}

		ticker++;
		redstoneLastTick = redstone;
	}
	
	@Override
	public void updateContainingBlockInfo()
	{
		super.updateContainingBlockInfo();
		
		onAdded();
	}
	
	@Override
	public void onChunkLoad()
	{
		markDirty();
	}

	public void open(EntityPlayer player)
	{
		playersUsing.add(player);
	}

	public void close(EntityPlayer player)
	{
		playersUsing.remove(player);
	}

	@Override
	public void handlePacketData(ByteBuf dataStream)
	{
		if(FMLCommonHandler.instance().getEffectiveSide().isClient())
		{
			facing = EnumFacing.getFront(dataStream.readInt());
			redstone = dataStream.readBoolean();
	
			if(clientFacing != facing)
			{
				MekanismUtils.updateBlock(world, getPos());
				world.notifyNeighborsOfStateChange(getPos(), world.getBlockState(getPos()).getBlock(), true);
				clientFacing = facing;
			}
	
			for(ITileComponent component : components)
			{
				component.read(dataStream);
			}
		}
	}

	@Override
	public ArrayList<Object> getNetworkedData(ArrayList<Object> data)
	{
		data.add(facing == null ? -1 : facing.ordinal());
		data.add(redstone);

		for(ITileComponent component : components)
		{
			component.write(data);
		}

		return data;
	}
	
	@Override
	public void invalidate()
	{
		super.invalidate();
		
		for(ITileComponent component : components)
		{
			component.invalidate();
		}
	}

	@Override
	public void validate()
	{
		super.validate();

		if(world.isRemote)
		{
			Mekanism.packetHandler.sendToServer(new DataRequestMessage(Coord4D.get(this)));
		}
	}

	/**
	 * Update call for machines. Use instead of updateEntity -- it's called every tick.
	 */
	public abstract void onUpdate();

	@Override
	public void readFromNBT(NBTTagCompound nbtTags)
	{
		super.readFromNBT(nbtTags);

		if(nbtTags.hasKey("facing"))
		{
			facing = EnumFacing.getFront(nbtTags.getInteger("facing"));
		}
		
		redstone = nbtTags.getBoolean("redstone");

		for(ITileComponent component : components)
		{
			component.read(nbtTags);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbtTags)
	{
		super.writeToNBT(nbtTags);

		if(facing != null)
		{
			nbtTags.setInteger("facing", facing.ordinal());
		}
		
		nbtTags.setBoolean("redstone", redstone);

		for(ITileComponent component : components)
		{
			component.write(nbtTags);
		}
		
		return nbtTags;
	}
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing)
	{
		return capability == Capabilities.TILE_NETWORK_CAPABILITY || super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing)
	{
		if(capability == Capabilities.TILE_NETWORK_CAPABILITY)
			return (T)this;
		return super.getCapability(capability, facing);
	}

	public void setFacing(short direction)
	{
		if(canSetFacing(direction))
		{
			facing = EnumFacing.getFront(direction);
		}

		if(!(facing == clientFacing || world.isRemote))
		{
			Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList<>())), new Range4D(Coord4D.get(this)));
			markDirty();
			clientFacing = facing;
		}
	}

	/**
	 * Whether or not this block's orientation can be changed to a specific direction. True by default.
	 * @param facing - facing to check
	 * @return if the block's orientation can be changed
	 */
	public boolean canSetFacing(int facing)
	{
		return true;
	}

	public boolean isPowered()
	{
		return redstone;
	}

	public boolean wasPowered()
	{
		return redstoneLastTick;
	}
	
	public void onPowerChange() {}

	public void onNeighborChange(Block block)
	{
		if(!world.isRemote)
		{
			updatePower();
		}
	}
	
	private void updatePower()
	{
		boolean power = world.isBlockIndirectlyGettingPowered(getPos()) > 0;

		if(redstone != power)
		{
			redstone = power;
			Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList<>())), new Range4D(Coord4D.get(this)));
		
			onPowerChange();
		}
	}
	
	/**
	 * Called when block is placed in world
	 */
	public void onAdded() 
	{
		updatePower();
	}
	
	@Override
	public Frequency getFrequency(FrequencyManager manager)
	{
		if(manager == Mekanism.securityFrequencies && this instanceof ISecurityTile)
		{
			return ((ISecurityTile)this).getSecurity().getFrequency();
		}
		
		return null;
	}

	@Override
	public void handleUpdateTag(NBTTagCompound tag) 
	{
		// The super implementation of handleUpdateTag is to call this readFromNBT. But, the given TagCompound
		// only has x/y/z/id data, so our readFromNBT will set a bunch of default values which are wrong.
		// So simply call the super's readFromNBT, to let Forge do whatever it wants, but don't treat this like
		// a full NBT object, don't pass it to our custom read methods.
		super.readFromNBT(tag);
	}
}
