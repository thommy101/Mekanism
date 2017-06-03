package mekanism.common.tile.prefab;

import java.util.EnumSet;

import mekanism.api.IConfigCardAccess;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.base.IElectricMachine;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.integration.computer.IComputerIntegration;
import mekanism.common.recipe.inputs.MachineInput;
import mekanism.common.recipe.machines.MachineRecipe;
import mekanism.common.recipe.outputs.MachineOutput;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;

public abstract class TileEntityBasicMachine<INPUT extends MachineInput<INPUT>, OUTPUT extends MachineOutput<OUTPUT>, RECIPE extends MachineRecipe<INPUT, OUTPUT, RECIPE>> extends TileEntityOperationalMachine implements IElectricMachine<INPUT, OUTPUT, RECIPE>, IComputerIntegration, ISideConfiguration, IConfigCardAccess
{
	public ResourceLocation guiLocation;

	public RECIPE cachedRecipe = null;

	public TileComponentEjector ejectorComponent;
	public TileComponentConfig configComponent;

	/**
	 * The foundation of all machines - a simple tile entity with a facing, active state, initialized state, sound effect, and animated texture.
	 * @param soundPath - location of the sound effect
	 * @param name - full name of this machine
	 * @param location - GUI texture path of this machine
	 * @param baseEnergyUsage - the energy this machine consumes every tick in it's active state
	 * @param baseTicksRequired - how many ticks it takes to run a cycle
	 * @param maxEnergy - how much energy this machine can store
	 */
	public TileEntityBasicMachine(String soundPath, String name, double maxEnergy, double baseEnergyUsage, int upgradeSlot, int baseTicksRequired, ResourceLocation location)
	{
		super("machine." + soundPath, name, maxEnergy, baseEnergyUsage, upgradeSlot, baseTicksRequired);
		
		guiLocation = location;
	}
	
	@Override
	public EnumSet<EnumFacing> getConsumingSides()
	{
		return configComponent.getSidesForData(TransmissionType.ENERGY, facing, 1);
	}

	@Override
	public int[] getSlotsForFace(EnumFacing side)
	{
		return configComponent.getOutput(TransmissionType.ITEM, side, facing).availableSlots;
	}

	@Override
	public TileComponentConfig getConfig()
	{
		return configComponent;
	}

	@Override
	public EnumFacing getOrientation()
	{
		return facing;
	}
	
	@Override
	public TileComponentEjector getEjector()
	{
		return ejectorComponent;
	}
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing side)
	{
		return capability == Capabilities.CONFIG_CARD_CAPABILITY || super.hasCapability(capability, side);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing side)
	{
		if(capability == Capabilities.CONFIG_CARD_CAPABILITY)
		{
			return (T)this;
		}
		
		return super.getCapability(capability, side);
	}
}
