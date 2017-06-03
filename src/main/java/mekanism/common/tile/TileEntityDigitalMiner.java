package mekanism.common.tile;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import mekanism.api.Chunk3D;
import mekanism.api.Coord4D;
import mekanism.api.Range4D;
import mekanism.common.HashList;
import mekanism.common.Mekanism;
import mekanism.common.Upgrade;
import mekanism.common.base.IActiveState;
import mekanism.common.base.IAdvancedBoundingBlock;
import mekanism.common.base.IRedstoneControl;
import mekanism.common.base.ISustainedData;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.chunkloading.IChunkLoader;
import mekanism.common.config.MekanismConfig.usage;
import mekanism.common.content.miner.MItemStackFilter;
import mekanism.common.content.miner.MOreDictFilter;
import mekanism.common.content.miner.MinerFilter;
import mekanism.common.content.miner.ThreadMinerSearch;
import mekanism.common.content.miner.ThreadMinerSearch.State;
import mekanism.common.content.transporter.InvStack;
import mekanism.common.content.transporter.TransitRequest;
import mekanism.common.content.transporter.TransitRequest.TransitResponse;
import mekanism.common.inventory.container.ContainerFilter;
import mekanism.common.inventory.container.ContainerNull;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.tile.component.TileComponentChunkLoader;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.prefab.TileEntityElectricBlock;
import mekanism.common.util.CapabilityUtils;
import mekanism.common.util.ChargeUtils;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MinerUtils;
import mekanism.common.util.TransporterUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityDigitalMiner extends TileEntityElectricBlock implements IUpgradeTile, IRedstoneControl, IActiveState, ISustainedData, IChunkLoader, IAdvancedBoundingBlock
{
	public static int[] EJECT_INV;

	public Map<Chunk3D, BitSet> oresToMine = new HashMap<>();
	public Map<Integer, MinerFilter> replaceMap = new HashMap<>();

	public HashList<MinerFilter> filters = new HashList<>();

	public ThreadMinerSearch searcher = new ThreadMinerSearch(this);

	public final double BASE_ENERGY_USAGE = usage.digitalMinerUsage;

	public double energyUsage = usage.digitalMinerUsage;

	public int radius;

	public boolean inverse;

	public int minY = 0;
	public int maxY = 60;

	public boolean doEject = false;
	public boolean doPull = false;
	
	public ItemStack missingStack = ItemStack.EMPTY;
	
	public int BASE_DELAY = 80;

	public int delay;

	public int delayLength = BASE_DELAY;

	public int clientToMine;

	public boolean isActive;
	public boolean clientActive;

	public boolean silkTouch;

	public boolean running;

	public double prevEnergy;

	public int delayTicks;

	public boolean initCalc = false;

	public int numPowering;
	
	public boolean clientRendering = false;

	/** This machine's current RedstoneControl type. */
	public RedstoneControl controlType = RedstoneControl.DISABLED;

	public TileComponentUpgrade upgradeComponent = new TileComponentUpgrade(this, 28);
	public TileComponentSecurity securityComponent = new TileComponentSecurity(this);
	public TileComponentChunkLoader chunkLoaderComponent = new TileComponentChunkLoader(this);

	public TileEntityDigitalMiner()
	{
		super("DigitalMiner", BlockStateMachine.MachineType.DIGITAL_MINER.baseEnergy);
		inventory = NonNullList.withSize(29, ItemStack.EMPTY);
		radius = 10;
		
		upgradeComponent.setSupported(Upgrade.ANCHOR);
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();

		if(getActive())
		{
			for(EntityPlayer player : (HashSet<EntityPlayer>)playersUsing.clone())
			{
				if(player.openContainer instanceof ContainerNull || player.openContainer instanceof ContainerFilter)
				{
					player.closeScreen();
				}
			}
		}

		if(!world.isRemote)
		{
			if(!initCalc)
			{
				if(searcher.state == State.FINISHED)
				{
					boolean prevRunning = running;
					
					reset();
					start();
					
					running = prevRunning;
				}

				initCalc = true;
			}

			ChargeUtils.discharge(27, this);

			if(MekanismUtils.canFunction(this) && running && getEnergy() >= getPerTick() && searcher.state == State.FINISHED && oresToMine.size() > 0)
			{
				setActive(true);

				if(delay > 0)
				{
					delay--;
				}

				setEnergy(getEnergy()-getPerTick());

				if(delay == 0)
				{
					Set<Chunk3D> toRemove = new HashSet<>();
					boolean did = false;
					
					for(Chunk3D chunk : oresToMine.keySet())
					{
						BitSet set = oresToMine.get(chunk);
						int next = 0;
	
						while(true)
						{
							int index = set.nextSetBit(next);
							Coord4D coord = getCoordFromIndex(index);
	
							if(index == -1)
							{
								toRemove.add(chunk);
								break;
							}
	
							if(!coord.exists(world))
							{
								set.clear(index);
								
								if(set.cardinality() == 0)
								{
									toRemove.add(chunk);
								}
								
								next = index + 1;
								continue;
							}

							IBlockState state = coord.getBlockState(world);
							Block block = state.getBlock();
							int meta = block.getMetaFromState(state);
	
							if(block == null || coord.isAirBlock(world))
							{
								set.clear(index);
								
								if(set.cardinality() == 0)
								{
									toRemove.add(chunk);
								}
								
								next = index + 1;
								continue;
							}
	
							boolean hasFilter = false;
	
							for(MinerFilter filter : filters)
							{
								if(filter.canFilter(new ItemStack(block, 1, meta)))
								{
									hasFilter = true;
									break;
								}
							}
	
							if(inverse == hasFilter)
							{
								set.clear(index);
								
								if(set.cardinality() == 0)
								{
									toRemove.add(chunk);
									break;
								}
								
								next = index + 1;
								continue;
							}
	
							List<ItemStack> drops = MinerUtils.getDrops(world, coord, silkTouch);
	
							if(canInsert(drops) && setReplace(coord, index))
							{
								did = true;
								add(drops);
								set.clear(index);
								
								if(set.cardinality() == 0)
								{
									toRemove.add(chunk);
								}
	
								world.playEvent(null, 2001, coord.getPos(), Block.getStateId(state));
	
								missingStack = ItemStack.EMPTY;
							}
	
							break;
						}
						
						if(did)
						{
							break;
						}
					}
					
					for(Chunk3D chunk : toRemove)
					{
						oresToMine.remove(chunk);
					}
					
					delay = getDelay();
				}
			}
			else {
				if(prevEnergy >= getEnergy())
				{
					setActive(false);
				}
			}
			
			TransitRequest ejectMap = getEjectItemMap();

			if(doEject && delayTicks == 0 && !ejectMap.isEmpty() && getEjectInv() != null && getEjectTile() != null)
			{
				if(CapabilityUtils.hasCapability(getEjectInv(), Capabilities.LOGISTICAL_TRANSPORTER_CAPABILITY, facing.getOpposite()))
				{
					TransitResponse response = TransporterUtils.insert(getEjectTile(), CapabilityUtils.getCapability(getEjectInv(), Capabilities.LOGISTICAL_TRANSPORTER_CAPABILITY, facing.getOpposite()), ejectMap, null, true, 0);

					if(!response.isEmpty())
					{
						response.getInvStack(this, facing.getOpposite()).use();
					}
				}
				else {
					TransitResponse response = InventoryUtils.putStackInInventory(getEjectInv(), ejectMap, facing.getOpposite(), false);

					if(!response.isEmpty())
					{
						response.getInvStack(this, facing.getOpposite()).use();
					}
				}

				delayTicks = 10;
			}
			else if(delayTicks > 0)
			{
				delayTicks--;
			}

			if(playersUsing.size() > 0)
			{
				for(EntityPlayer player : playersUsing)
				{
					Mekanism.packetHandler.sendTo(new TileEntityMessage(Coord4D.get(this), getSmallPacket(new ArrayList<>())), (EntityPlayerMP)player);
				}
			}

			prevEnergy = getEnergy();
		}
	}

	public double getPerTick()
	{
		double ret = energyUsage;

		if(silkTouch)
		{
			ret *= 6F;
		}

		int baseRad = Math.max(radius-10, 0);
		ret *= (1 + ((float)baseRad/22F));

		int baseHeight = Math.max((maxY-minY)-60, 0);
		ret *= (1 + ((float)baseHeight/195F));

		return ret;
	}

	public int getDelay()
	{
		return delayLength;
	}

	/*
	 * returns false if unsuccessful
	 */
	public boolean setReplace(Coord4D obj, int index)
	{
		IBlockState state = obj.getBlockState(world);
		Block block = state.getBlock();
		
		EntityPlayer dummy = Mekanism.proxy.getDummyPlayer((WorldServer)world, obj.xCoord, obj.yCoord, obj.zCoord).get();
		BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, obj.getPos(), state, dummy);
		MinecraftForge.EVENT_BUS.post(event);
		
		if(!event.isCanceled())
		{
			ItemStack stack = getReplace(index);
			 
			if(!stack.isEmpty())
			{
				world.setBlockState(obj.getPos(), Block.getBlockFromItem(stack.getItem()).getStateFromMeta(stack.getItemDamage()), 3);

				IBlockState s = obj.getBlockState(world);
				if(s.getBlock() instanceof BlockBush && !((BlockBush)s.getBlock()).canBlockStay(world, obj.getPos(), s))
				{
					s.getBlock().dropBlockAsItem(world, obj.getPos(), s, 1);
					world.setBlockToAir(obj.getPos());
				}
				
				return true;
			}
			else {
				MinerFilter filter = replaceMap.get(index);

				if(filter == null || (filter.replaceStack.isEmpty() || !filter.requireStack))
				{
					world.setBlockToAir(obj.getPos());
					
					return true;
				}
				
				missingStack = filter.replaceStack;
				
				return false;
			}
		}
		
		return false;
	}

	public ItemStack getReplace(int index)
	{
		MinerFilter filter = replaceMap.get(index);
		
		if(filter == null || filter.replaceStack.isEmpty())
		{
			return ItemStack.EMPTY;
		}

		for(int i = 0; i < 27; i++)
		{
			if(!inventory.get(i).isEmpty() && inventory.get(i).isItemEqual(filter.replaceStack))
			{
				inventory.get(i).shrink(1);

				return MekanismUtils.size(filter.replaceStack, 1);
			}
		}

		if(doPull && getPullInv() != null)
		{
			InvStack stack = InventoryUtils.takeDefinedItem(getPullInv(), EnumFacing.UP, filter.replaceStack.copy(), 1, 1);

			if(stack != null)
			{
				stack.use();
				return MekanismUtils.size(filter.replaceStack, 1);
			}
		}

		return ItemStack.EMPTY;
	}

	public NonNullList<ItemStack> copy(NonNullList<ItemStack> stacks)
	{
		NonNullList<ItemStack> toReturn = NonNullList.withSize(stacks.size(), ItemStack.EMPTY);

		for(int i = 0; i < stacks.size(); i++)
		{
			toReturn.set(i, !stacks.get(i).isEmpty() ? stacks.get(i).copy() : ItemStack.EMPTY);
		}

		return toReturn;
	}

	public TransitRequest getEjectItemMap()
	{
		TransitRequest request = new TransitRequest();
		
		for(int i = 27-1; i >= 0; i--)
		{
			ItemStack stack = inventory.get(i);

			if(!stack.isEmpty())
			{
				if(isReplaceStack(stack))
				{
					continue;
				}

				if(!request.hasType(stack))
				{
					request.setItem(stack, i);
				}
			}
		}

		return request;
	}

	public boolean canInsert(List<ItemStack> stacks)
	{
		if(stacks.isEmpty())
		{
			return true;
		}

		NonNullList<ItemStack> testInv = copy(inventory);

		int added = 0;

		stacks:
		for(ItemStack stack : stacks)
		{
			stack = stack.copy();
			
			if(stack.isEmpty() || stack.getItem() == null)
			{
				continue;
			}
			
			for(int i = 0; i < 27; i++)
			{
				if(!testInv.get(i).isEmpty() && testInv.get(i).getItem() == null)
				{
					testInv.set(i, ItemStack.EMPTY);
				}
				
				if(testInv.get(i).isEmpty())
				{
					testInv.set(i, stack);
					added++;

					continue stacks;
				}
				else if(testInv.get(i).isItemEqual(stack) && testInv.get(i).getCount()+stack.getCount() <= stack.getMaxStackSize())
				{
					testInv.get(i).grow(stack.getCount());
					added++;

					continue stacks;
				}
			}
		}

		if(added == stacks.size())
		{
			return true;
		}

		return false;
	}

	public TileEntity getPullInv()
	{
		return Coord4D.get(this).translate(0, 2, 0).getTileEntity(world);
	}

	public TileEntity getEjectInv()
	{
		EnumFacing side = facing.getOpposite();

		return world.getTileEntity(getPos().up().offset(side, 2));
	}

	public void add(List<ItemStack> stacks)
	{
		if(stacks.isEmpty())
		{
			return;
		}

		stacks:
		for(ItemStack stack : stacks)
		{
			for(int i = 0; i < 27; i++)
			{
				if(inventory.get(i).isEmpty())
				{
					inventory.set(i, stack);

					continue stacks;
				}
				else if(inventory.get(i).isItemEqual(stack) && inventory.get(i).getCount()+stack.getCount() <= stack.getMaxStackSize())
				{
					inventory.get(i).grow(stack.getCount());

					continue stacks;
				}
			}
		}
	}

	public void start()
	{
		if(searcher.state == State.IDLE)
		{
			searcher.start();
		}

		running = true;

		MekanismUtils.saveChunk(this);
	}

	public void stop()
	{
		if(searcher.state == State.SEARCHING)
		{
			searcher.interrupt();
			reset();

			return;
		}
		else if(searcher.state == State.FINISHED)
		{
			running = false;
		}

		MekanismUtils.saveChunk(this);
	}

	public void reset()
	{
		searcher = new ThreadMinerSearch(this);
		running = false;
		oresToMine.clear();
		replaceMap.clear();
		missingStack = ItemStack.EMPTY;
		setActive(false);

		MekanismUtils.saveChunk(this);
	}
	
	public boolean isReplaceStack(ItemStack stack)
	{
		for(MinerFilter filter : filters)
		{
			if(!filter.replaceStack.isEmpty() && filter.replaceStack.isItemEqual(stack))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public int getSize()
	{
		int size = 0;
		
		for(Chunk3D chunk : oresToMine.keySet())
		{
			size += oresToMine.get(chunk).cardinality();
		}
		
		return size;
	}

	@Override
	public void openInventory(EntityPlayer player)
	{
		super.openInventory(player);

		if(!world.isRemote)
		{
			Mekanism.packetHandler.sendTo(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList())), (EntityPlayerMP)player);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbtTags)
	{
		super.readFromNBT(nbtTags);

		radius = nbtTags.getInteger("radius");
		minY = nbtTags.getInteger("minY");
		maxY = nbtTags.getInteger("maxY");
		doEject = nbtTags.getBoolean("doEject");
		doPull = nbtTags.getBoolean("doPull");
		isActive = nbtTags.getBoolean("isActive");
		running = nbtTags.getBoolean("running");
		delay = nbtTags.getInteger("delay");
		silkTouch = nbtTags.getBoolean("silkTouch");
		numPowering = nbtTags.getInteger("numPowering");
		searcher.state = State.values()[nbtTags.getInteger("state")];
		controlType = RedstoneControl.values()[nbtTags.getInteger("controlType")];
		inverse = nbtTags.getBoolean("inverse");

		if(nbtTags.hasKey("filters"))
		{
			NBTTagList tagList = nbtTags.getTagList("filters", NBT.TAG_COMPOUND);

			for(int i = 0; i < tagList.tagCount(); i++)
			{
				filters.add(MinerFilter.readFromNBT(tagList.getCompoundTagAt(i)));
			}
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbtTags)
	{
		super.writeToNBT(nbtTags);

		if(searcher.state == State.SEARCHING)
		{
			reset();
		}

		nbtTags.setInteger("radius", radius);
		nbtTags.setInteger("minY", minY);
		nbtTags.setInteger("maxY", maxY);
		nbtTags.setBoolean("doEject", doEject);
		nbtTags.setBoolean("doPull", doPull);
		nbtTags.setBoolean("isActive", isActive);
		nbtTags.setBoolean("running", running);
		nbtTags.setInteger("delay", delay);
		nbtTags.setBoolean("silkTouch", silkTouch);
		nbtTags.setInteger("numPowering", numPowering);
		nbtTags.setInteger("state", searcher.state.ordinal());
		nbtTags.setInteger("controlType", controlType.ordinal());
		nbtTags.setBoolean("inverse", inverse);

		NBTTagList filterTags = new NBTTagList();

		for(MinerFilter filter : filters)
		{
			filterTags.appendTag(filter.write(new NBTTagCompound()));
		}

		if(filterTags.tagCount() != 0)
		{
			nbtTags.setTag("filters", filterTags);
		}
		
		return nbtTags;
	}

	@Override
	public void handlePacketData(ByteBuf dataStream)
	{
		if(FMLCommonHandler.instance().getEffectiveSide().isServer())
		{
			int type = dataStream.readInt();

			switch(type)
			{
				case 0:
					doEject = !doEject;
					break;
				case 1:
					doPull = !doPull;
					break;
				case 3:
					start();
					break;
				case 4:
					stop();
					break;
				case 5:
					reset();
					break;
				case 6:
					radius = dataStream.readInt();
					break;
				case 7:
					minY = dataStream.readInt();
					break;
				case 8:
					maxY = dataStream.readInt();
					break;
				case 9:
					silkTouch = !silkTouch;
					break;
				case 10:
					inverse = !inverse;
					break;
				case 11:
				{
					// Move filter up
					int filterIndex = dataStream.readInt();
					filters.swap(filterIndex, filterIndex - 1);
					
					for(EntityPlayer player : playersUsing) 
					{
						openInventory(player);
					}
					
					break;
				}
				case 12:
				{
					// Move filter down
					int filterIndex = dataStream.readInt();
					filters.swap(filterIndex, filterIndex + 1);
					
					for(EntityPlayer player : playersUsing)
					{
						openInventory(player);
					}
					
					break;
				}
			}
			
			MekanismUtils.saveChunk(this);

			for(EntityPlayer player : playersUsing)
			{
				Mekanism.packetHandler.sendTo(new TileEntityMessage(Coord4D.get(this), getGenericPacket(new ArrayList<>())), (EntityPlayerMP)player);
			}

			return;
		}

		super.handlePacketData(dataStream);

		if(FMLCommonHandler.instance().getEffectiveSide().isClient())
		{
			int type = dataStream.readInt();
	
			if(type == 0)
			{
				radius = dataStream.readInt();
				minY = dataStream.readInt();
				maxY = dataStream.readInt();
				doEject = dataStream.readBoolean();
				doPull = dataStream.readBoolean();
				clientActive = dataStream.readBoolean();
				running = dataStream.readBoolean();
				silkTouch = dataStream.readBoolean();
				numPowering = dataStream.readInt();
				searcher.state = State.values()[dataStream.readInt()];
				clientToMine = dataStream.readInt();
				controlType = RedstoneControl.values()[dataStream.readInt()];
				inverse = dataStream.readBoolean();
				
				if(dataStream.readBoolean())
				{
					missingStack = new ItemStack(Item.getItemById(dataStream.readInt()), 1, dataStream.readInt());
				}
				else {
					missingStack = ItemStack.EMPTY;
				}
	
				filters.clear();
	
				int amount = dataStream.readInt();
	
				for(int i = 0; i < amount; i++)
				{
					filters.add(MinerFilter.readFromPacket(dataStream));
				}
			}
			else if(type == 1)
			{
				radius = dataStream.readInt();
				minY = dataStream.readInt();
				maxY = dataStream.readInt();
				doEject = dataStream.readBoolean();
				doPull = dataStream.readBoolean();
				clientActive = dataStream.readBoolean();
				running = dataStream.readBoolean();
				silkTouch = dataStream.readBoolean();
				numPowering = dataStream.readInt();
				searcher.state = State.values()[dataStream.readInt()];
				clientToMine = dataStream.readInt();
				controlType = RedstoneControl.values()[dataStream.readInt()];
				inverse = dataStream.readBoolean();
				
				if(dataStream.readBoolean())
				{
					missingStack = new ItemStack(Item.getItemById(dataStream.readInt()), 1, dataStream.readInt());
				}
				else {
					missingStack = ItemStack.EMPTY;
				}
			}
			else if(type == 2)
			{
				filters.clear();
	
				int amount = dataStream.readInt();
	
				for(int i = 0; i < amount; i++)
				{
					filters.add(MinerFilter.readFromPacket(dataStream));
				}
			}
			else if(type == 3)
			{
				clientActive = dataStream.readBoolean();
				running = dataStream.readBoolean();
				clientToMine = dataStream.readInt();
				
				if(dataStream.readBoolean())
				{
					missingStack = new ItemStack(Item.getItemById(dataStream.readInt()), 1, dataStream.readInt());
				}
				else {
					missingStack = ItemStack.EMPTY;
				}
			}
			
			if(clientActive != isActive)
			{
				isActive = clientActive;
				MekanismUtils.updateBlock(world, getPos());
			}
		}
	}

	@Override
	public ArrayList<Object> getNetworkedData(ArrayList<Object> data)
	{
		super.getNetworkedData(data);

		data.add(0);

		data.add(radius);
		data.add(minY);
		data.add(maxY);
		data.add(doEject);
		data.add(doPull);
		data.add(isActive);
		data.add(running);
		data.add(silkTouch);
		data.add(numPowering);
		data.add(searcher.state.ordinal());
		
		if(searcher.state == State.SEARCHING)
		{
			data.add(searcher.found);
		}
		else {
			data.add(getSize());
		}

		data.add(controlType.ordinal());
		data.add(inverse);
		
		if(!missingStack.isEmpty())
		{
			data.add(true);
			data.add(MekanismUtils.getID(missingStack));
			data.add(missingStack.getItemDamage());
		}
		else {
			data.add(false);
		}

		data.add(filters.size());

		for(MinerFilter filter : filters)
		{
			filter.write(data);
		}

		return data;
	}

	public ArrayList<Object> getSmallPacket(ArrayList<Object> data)
	{
		super.getNetworkedData(data);

		data.add(3);

		data.add(isActive);
		data.add(running);

		if(searcher.state == State.SEARCHING)
		{
			data.add(searcher.found);
		}
		else {
			data.add(getSize());
		}
		
		if(!missingStack.isEmpty())
		{
			data.add(true);
			data.add(MekanismUtils.getID(missingStack));
			data.add(missingStack.getItemDamage());
		}
		else {
			data.add(false);
		}

		return data;
	}

	public ArrayList<Object> getGenericPacket(ArrayList<Object> data)
	{
		super.getNetworkedData(data);

		data.add(1);

		data.add(radius);
		data.add(minY);
		data.add(maxY);
		data.add(doEject);
		data.add(doPull);
		data.add(isActive);
		data.add(running);
		data.add(silkTouch);
		data.add(numPowering);
		data.add(searcher.state.ordinal());

		if(searcher.state == State.SEARCHING)
		{
			data.add(searcher.found);
		}
		else {
			data.add(getSize());
		}

		data.add(controlType.ordinal());
		data.add(inverse);
		
		if(!missingStack.isEmpty())
		{
			data.add(true);
			data.add(MekanismUtils.getID(missingStack));
			data.add(missingStack.getItemDamage());
		}
		else {
			data.add(false);
		}

		return data;
	}

	public ArrayList getFilterPacket(ArrayList<Object> data)
	{
		super.getNetworkedData(data);

		data.add(2);

		data.add(filters.size());

		for(MinerFilter filter : filters)
		{
			filter.write(data);
		}

		return data;
	}

	public int getTotalSize()
	{
		return getDiameter()*getDiameter()*(maxY-minY+1);
	}

	public int getDiameter()
	{
		return (radius*2)+1;
	}

	public Coord4D getStartingCoord()
	{
		return new Coord4D(getPos().getX()-radius, minY, getPos().getZ()-radius, world.provider.getDimension());
	}

	public Coord4D getCoordFromIndex(int index)
	{
		int diameter = getDiameter();
		Coord4D start = getStartingCoord();

		int x = start.xCoord+index%diameter;
		int y = start.yCoord+(index/diameter/diameter);
		int z = start.zCoord+(index/diameter)%diameter;

		return new Coord4D(x, y, z, world.provider.getDimension());
	}

	@Override
	public boolean isPowered()
	{
		return redstone || numPowering > 0;
	}

	@Override
	public boolean canPulse()
	{
		return false;
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
		MekanismUtils.saveChunk(this);
	}

	@Override
	public TileComponentUpgrade getComponent()
	{
		return upgradeComponent;
	}

	@Override
	public void setActive(boolean active)
	{
		isActive = active;

		if(clientActive != active)
		{
			Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList<>())), new Range4D(Coord4D.get(this)));

			clientActive = active;
		}
	}

	@Override
	public boolean getActive()
	{
		return isActive;
	}

	@Override
	public boolean renderUpdate()
	{
		return false;
	}

	@Override
	public boolean lightUpdate()
	{
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox()
	{
		return INFINITE_EXTENT_AABB;
	}

	@Override
	public void onPlace()
	{
		for(int x = -1; x <= +1; x++)
		{
			for(int y = 0; y <= +1; y++)
			{
				for(int z = -1; z <= +1; z++)
				{
					if(x == 0 && y == 0 && z == 0)
					{
						continue;
					}

					BlockPos pos1 = getPos().add(x, y, z);
					MekanismUtils.makeAdvancedBoundingBlock(world, pos1, Coord4D.get(this));
		            world.notifyNeighborsOfStateChange(pos1, getBlockType(), true);
				}
			}
		}
	}

	@Override
	public boolean canSetFacing(int side)
	{
		return side != 0 && side != 1;
	}

	@Override
	public void onBreak()
	{
		for(int x = -1; x <= +1; x++)
		{
			for(int y = 0; y <= +1; y++)
			{
				for(int z = -1; z <= +1; z++)
				{
					world.setBlockToAir(getPos().add(x, y, z));
				}
			}
		}
	}

	@Override
	public int[] getSlotsForFace(EnumFacing side)
	{
		return InventoryUtils.EMPTY;
	}

	public TileEntity getEjectTile()
	{
		EnumFacing side = facing.getOpposite();
		return world.getTileEntity(getPos().up().offset(side));
	}

	@Override
	public int[] getBoundSlots(BlockPos location, EnumFacing side)
	{
		EnumFacing dir = facing.getOpposite();

		BlockPos pull = getPos().up();
		BlockPos eject = pull.offset(dir);
		
		if((location.equals(eject) && side == dir) || (location.equals(pull) && side == EnumFacing.UP))
		{
			if(EJECT_INV == null)
			{
				EJECT_INV = new int[27];

				for(int i = 0; i < EJECT_INV.length; i++)
				{
					EJECT_INV[i] = i;
				}
			}

			return EJECT_INV;
		}

		return InventoryUtils.EMPTY;
	}

	@Override
	public boolean canBoundInsert(BlockPos location, int i, ItemStack itemstack)
	{
		EnumFacing side = facing.getOpposite();

		BlockPos pull = getPos().up();
		BlockPos eject = pull.offset(side);

		if(location.equals(eject))
		{
			return false;
		}
		else if(location.equals(pull))
		{
			if(!itemstack.isEmpty() && isReplaceStack(itemstack))
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean canBoundExtract(BlockPos location, int i, ItemStack itemstack, EnumFacing dir)
	{
		EnumFacing side = facing.getOpposite();

		BlockPos pull = getPos().up();
		BlockPos eject = pull.offset(side);

		if(location.equals(eject))
		{
			if(!itemstack.isEmpty() && isReplaceStack(itemstack))
			{
				return false;
			}

			return true;
		}
		else if(location.equals(pull))
		{
			return false;
		}

		return false;
	}

	@Override
	public void onPower()
	{
		numPowering++;
	}

	@Override
	public void onNoPower()
	{
		numPowering--;
	}

	public String[] methods = {"setRadius", "setMin", "setMax", "addFilter", "removeFilter", "addOreFilter", "removeOreFilter", "reset", "start", "stop", "getToMine"};

	@Override
	public String[] getMethods()
	{
		return methods;
	}

	@Override
	public Object[] invoke(int method, Object[] arguments) throws Exception
	{
		if(method == 0)
		{
			if(arguments.length != 1 || !(arguments[0] instanceof Double))
			{
				return new Object[] {"Invalid parameters."};
			}
			
			radius = ((Double)arguments[0]).intValue();
		}
		else if(method == 1)
		{
			if(arguments.length != 1 || !(arguments[0] instanceof Double))
			{
				return new Object[] {"Invalid parameters."};
			}
			
			minY = ((Double)arguments[0]).intValue();
		}
		else if(method == 2)
		{
			if(arguments.length != 1 || !(arguments[0] instanceof Double))
			{
				return new Object[] {"Invalid parameters."};
			}
			
			maxY = ((Double)arguments[0]).intValue();
		}
		else if(method == 3)
		{
			if(arguments.length < 1 || !(arguments[0] instanceof Double))
			{
				return new Object[] {"Invalid parameters."};
			}
			
			int id = ((Double)arguments[0]).intValue();
			int meta = 0;

			if(arguments.length > 1)
			{
				if(arguments[1] instanceof Double)
				{
					meta = ((Double)arguments[1]).intValue();
				}
			}

			filters.add(new MItemStackFilter(new ItemStack(Item.getItemById(id), 1, meta)));
			
			return new Object[] {"Added filter."};
		}
		else if(method == 4)
		{
			if(arguments.length < 1 || !(arguments[0] instanceof Double))
			{
				return new Object[] {"Invalid parameters."};
			}
			
			int id = ((Double)arguments[0]).intValue();
			Iterator<MinerFilter> iter = filters.iterator();

			while(iter.hasNext())
			{
				MinerFilter filter = iter.next();

				if(filter instanceof MItemStackFilter)
				{
					if(MekanismUtils.getID(((MItemStackFilter)filter).itemType) == id)
					{
						iter.remove();
						return new Object[] {"Removed filter."};
					}
				}
			}
			
			return new Object[] {"Couldn't find filter."};
		}
		else if(method == 5)
		{
			if(arguments.length < 1 || !(arguments[0] instanceof String))
			{
				return new Object[] {"Invalid parameters."};
			}
			
			String ore = (String)arguments[0];
			MOreDictFilter filter = new MOreDictFilter();

			filter.oreDictName = ore;
			filters.add(filter);
			
			return new Object[] {"Added filter."};
		}
		else if(method == 6)
		{
			if(arguments.length < 1 || !(arguments[0] instanceof String))
			{
				return new Object[] {"Invalid parameters."};
			}
			
			String ore = (String)arguments[0];
			Iterator<MinerFilter> iter = filters.iterator();

			while(iter.hasNext())
			{
				MinerFilter filter = iter.next();

				if(filter instanceof MOreDictFilter)
				{
					if(((MOreDictFilter)filter).oreDictName.equals(ore))
					{
						iter.remove();
						return new Object[] {"Removed filter."};
					}
				}
			}
			
			return new Object[] {"Couldn't find filter."};
		}
		else if(method == 7)
		{
			reset();
			return new Object[] {"Reset miner."};
		}
		else if(method == 8)
		{
			start();
			return new Object[] {"Started miner."};
		}
		else if(method == 9)
		{
			stop();
			return new Object[] {"Stopped miner."};
		}
		else if(method == 10)
		{
			return new Object[] {searcher != null ? searcher.found : 0};
		}

		for(EntityPlayer player : playersUsing)
		{
			Mekanism.packetHandler.sendTo(new TileEntityMessage(Coord4D.get(this), getGenericPacket(new ArrayList<>())), (EntityPlayerMP)player);
		}
		
		return null;
	}

	@Override
	public NBTTagCompound getConfigurationData(NBTTagCompound nbtTags)
	{
		nbtTags.setInteger("radius", radius);
		nbtTags.setInteger("minY", minY);
		nbtTags.setInteger("maxY", maxY);
		nbtTags.setBoolean("doEject", doEject);
		nbtTags.setBoolean("doPull", doPull);
		nbtTags.setBoolean("silkTouch", silkTouch);
		nbtTags.setBoolean("inverse", inverse);

		NBTTagList filterTags = new NBTTagList();

		for(MinerFilter filter : filters)
		{
			filterTags.appendTag(filter.write(new NBTTagCompound()));
		}

		if(filterTags.tagCount() != 0)
		{
			nbtTags.setTag("filters", filterTags);
		}
		
		return nbtTags;
	}

	@Override
	public void setConfigurationData(NBTTagCompound nbtTags)
	{
		radius = nbtTags.getInteger("radius");
		minY = nbtTags.getInteger("minY");
		maxY = nbtTags.getInteger("maxY");
		doEject = nbtTags.getBoolean("doEject");
		doPull = nbtTags.getBoolean("doPull");
		silkTouch = nbtTags.getBoolean("silkTouch");
		inverse = nbtTags.getBoolean("inverse");

		if(nbtTags.hasKey("filters"))
		{
			NBTTagList tagList = nbtTags.getTagList("filters", NBT.TAG_COMPOUND);

			for(int i = 0; i < tagList.tagCount(); i++)
			{
				filters.add(MinerFilter.readFromNBT(tagList.getCompoundTagAt(i)));
			}
		}
	}

	@Override
	public String getDataType()
	{
		return getBlockType().getUnlocalizedName() + "." + fullName + ".name";
	}
	
	public void writeSustainedData(ItemStack itemStack) 
	{
		ItemDataUtils.setBoolean(itemStack, "hasMinerConfig", true);

		ItemDataUtils.setInt(itemStack, "radius", radius);
		ItemDataUtils.setInt(itemStack, "minY", minY);
		ItemDataUtils.setInt(itemStack, "maxY", maxY);
		ItemDataUtils.setBoolean(itemStack, "doEject", doEject);
		ItemDataUtils.setBoolean(itemStack, "doPull", doPull);
		ItemDataUtils.setBoolean(itemStack, "silkTouch", silkTouch);
		ItemDataUtils.setBoolean(itemStack, "inverse", inverse);

		NBTTagList filterTags = new NBTTagList();

		for(MinerFilter filter : filters)
		{
			filterTags.appendTag(filter.write(new NBTTagCompound()));
		}

		if(filterTags.tagCount() != 0)
		{
			ItemDataUtils.setList(itemStack, "filters", filterTags);
		}
	}

	@Override
	public void readSustainedData(ItemStack itemStack)
	{
		if(ItemDataUtils.hasData(itemStack, "hasMinerConfig"))
		{
			radius = ItemDataUtils.getInt(itemStack, "radius");
			minY = ItemDataUtils.getInt(itemStack, "minY");
			maxY = ItemDataUtils.getInt(itemStack, "maxY");
			doEject = ItemDataUtils.getBoolean(itemStack, "doEject");
			doPull = ItemDataUtils.getBoolean(itemStack, "doPull");
			silkTouch = ItemDataUtils.getBoolean(itemStack, "silkTouch");
			inverse = ItemDataUtils.getBoolean(itemStack, "inverse");

			if(ItemDataUtils.hasData(itemStack, "filters"))
			{
				NBTTagList tagList = ItemDataUtils.getList(itemStack, "filters");

				for(int i = 0; i < tagList.tagCount(); i++)
				{
					filters.add(MinerFilter.readFromNBT(tagList.getCompoundTagAt(i)));
				}
			}
		}
	}

	@Override
	public void recalculateUpgradables(Upgrade upgrade)
	{
		super.recalculateUpgradables(upgrade);

		switch(upgrade)
		{
			case SPEED:
				delayLength = MekanismUtils.getTicks(this, BASE_DELAY);
			case ENERGY:
				energyUsage = MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_USAGE);
				maxEnergy = MekanismUtils.getMaxEnergy(this, BASE_MAX_ENERGY);
				setEnergy(Math.min(getMaxEnergy(), getEnergy()));
			default:
				break;
		}
	}
	
	@Override
	public boolean canBoundReceiveEnergy(BlockPos coord, EnumFacing side)
	{
		EnumFacing left = MekanismUtils.getLeft(facing);
		EnumFacing right = MekanismUtils.getRight(facing);
		
		if(coord.equals(getPos().offset(left)))
		{
			return side == left;
		}
		else if(coord.equals(getPos().offset(right)))
		{
			return side == right;
		}
		
		return false;
	}
	
	@Override
	public EnumSet<EnumFacing> getConsumingSides()
	{
		return EnumSet.of(MekanismUtils.getLeft(facing), MekanismUtils.getRight(facing), EnumFacing.DOWN);
	}

	@Override
	public TileComponentSecurity getSecurity() 
	{
		return securityComponent;
	}
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing side)
	{
		return capability == Capabilities.CONFIG_CARD_CAPABILITY || capability == Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY 
				|| super.hasCapability(capability, side);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing side)
	{
		if(capability == Capabilities.CONFIG_CARD_CAPABILITY || capability == Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY)
		{
			return (T)this;
		}
		
		return super.getCapability(capability, side);
	}
	
	@Override
	public TileComponentChunkLoader getChunkLoader()
	{
		return chunkLoaderComponent;
	}
	
	@Override
	public Set<ChunkPos> getChunkSet()
	{
		return new Range4D(Coord4D.get(this)).expandFromCenter(radius).getIntersectingChunks().stream().map(Chunk3D::getPos).collect(Collectors.toSet());
	}
}
