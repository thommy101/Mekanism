package mekanism.common.network;

import io.netty.buffer.ByteBuf;
import mekanism.api.MekanismConfig.general;
import mekanism.api.MekanismConfig.machines;
import mekanism.api.MekanismConfig.usage;
import mekanism.api.util.UnitDisplayUtils.EnergyType;
import mekanism.common.Mekanism;
import mekanism.common.Tier;
import mekanism.common.base.IModule;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.network.PacketConfigSync.ConfigSyncMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketConfigSync implements IMessageHandler<ConfigSyncMessage, IMessage>
{
	@Override
	public IMessage onMessage(ConfigSyncMessage message, MessageContext context) 
	{
		return null;
	}
	
	public static class ConfigSyncMessage implements IMessage
	{
		public ConfigSyncMessage() {}
		
		@Override
		public void toBytes(ByteBuf dataStream)
		{
			dataStream.writeBoolean(general.controlCircuitOreDict);
			dataStream.writeBoolean(general.logPackets);
			dataStream.writeBoolean(general.dynamicTankEasterEgg);
			dataStream.writeBoolean(general.voiceServerEnabled);
			dataStream.writeBoolean(general.cardboardSpawners);
			dataStream.writeBoolean(general.spawnBabySkeletons);
			dataStream.writeInt(general.obsidianTNTDelay);
			dataStream.writeInt(general.obsidianTNTBlastRadius);
			dataStream.writeInt(general.UPDATE_DELAY);
			dataStream.writeDouble(general.FROM_IC2);
			dataStream.writeDouble(general.TO_IC2);
			dataStream.writeDouble(general.FROM_RF);
			dataStream.writeDouble(general.TO_RF);
			dataStream.writeDouble(general.FROM_TESLA);
			dataStream.writeDouble(general.TO_TESLA);
			dataStream.writeDouble(general.FROM_H2);
			dataStream.writeInt(general.ETHENE_BURN_TIME);
			dataStream.writeDouble(general.ENERGY_PER_REDSTONE);
			dataStream.writeDouble(general.DISASSEMBLER_USAGE);
			dataStream.writeInt(general.VOICE_PORT);
			dataStream.writeInt(general.maxUpgradeMultiplier);
			dataStream.writeInt(general.energyUnit.ordinal());
			dataStream.writeDouble(general.minerSilkMultiplier);
			dataStream.writeBoolean(general.blacklistIC2);
			dataStream.writeBoolean(general.blacklistRF);
			dataStream.writeBoolean(general.blacklistTesla);
			dataStream.writeDouble(general.armoredJetpackDamageRatio);
			dataStream.writeInt(general.armoredJetpackDamageMax);
			dataStream.writeBoolean(general.aestheticWorldDamage);
			dataStream.writeBoolean(general.opsBypassRestrictions);
			dataStream.writeDouble(general.thermalEvaporationSpeed);
			dataStream.writeInt(general.maxJetpackGas);
			dataStream.writeInt(general.maxScubaGas);
			dataStream.writeInt(general.maxFlamethrowerGas);
			dataStream.writeInt(general.maxPumpRange);
			dataStream.writeBoolean(general.pumpWaterSources);
			dataStream.writeInt(general.maxPlenisherNodes);
			dataStream.writeDouble(general.evaporationHeatDissipation);
			dataStream.writeDouble(general.evaporationTempMultiplier);
			dataStream.writeDouble(general.evaporationSolarMultiplier);
			dataStream.writeDouble(general.evaporationMaxTemp);
			dataStream.writeDouble(general.energyPerHeat);
			dataStream.writeDouble(general.maxEnergyPerSteam);
			dataStream.writeDouble(general.superheatingHeatTransfer);
			dataStream.writeDouble(general.heatPerFuelTick);
			dataStream.writeBoolean(general.allowTransmitterAlloyUpgrade);
			dataStream.writeBoolean(general.allowChunkloading);
			dataStream.writeBoolean(general.allowProtection);
			
			for(MachineType type : BlockStateMachine.MachineType.getValidMachines())
			{
				dataStream.writeBoolean(machines.isEnabled(type.machineName));
			}
	
			dataStream.writeDouble(usage.enrichmentChamberUsage);
			dataStream.writeDouble(usage.osmiumCompressorUsage);
			dataStream.writeDouble(usage.combinerUsage);
			dataStream.writeDouble(usage.crusherUsage);
			dataStream.writeDouble(usage.factoryUsage);
			dataStream.writeDouble(usage.metallurgicInfuserUsage);
			dataStream.writeDouble(usage.purificationChamberUsage);
			dataStream.writeDouble(usage.energizedSmelterUsage);
			dataStream.writeDouble(usage.digitalMinerUsage);
			dataStream.writeDouble(usage.electricPumpUsage);
			dataStream.writeDouble(usage.rotaryCondensentratorUsage);
			dataStream.writeDouble(usage.oxidationChamberUsage);
			dataStream.writeDouble(usage.chemicalInfuserUsage);
			dataStream.writeDouble(usage.chemicalInjectionChamberUsage);
			dataStream.writeDouble(usage.precisionSawmillUsage);
			dataStream.writeDouble(usage.chemicalDissolutionChamberUsage);
			dataStream.writeDouble(usage.chemicalWasherUsage);
			dataStream.writeDouble(usage.chemicalCrystallizerUsage);
			dataStream.writeDouble(usage.seismicVibratorUsage);
			dataStream.writeDouble(usage.fluidicPlenisherUsage);
			dataStream.writeDouble(usage.gasCentrifugeUsage);
			dataStream.writeDouble(usage.heavyWaterElectrolysisUsage);
			dataStream.writeDouble(usage.formulaicAssemblicatorUsage);
			
			Tier.writeConfig(dataStream);
	
			try {
				for(IModule module : Mekanism.modulesLoaded)
				{
					module.writeConfig(dataStream);
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	
		@Override
		public void fromBytes(ByteBuf dataStream)
		{
			general.controlCircuitOreDict = dataStream.readBoolean();
			general.logPackets = dataStream.readBoolean();
			general.dynamicTankEasterEgg = dataStream.readBoolean();
			general.voiceServerEnabled = dataStream.readBoolean();
			general.cardboardSpawners = dataStream.readBoolean();
			general.spawnBabySkeletons = dataStream.readBoolean();
			general.obsidianTNTDelay = dataStream.readInt();
			general.obsidianTNTBlastRadius = dataStream.readInt();
			general.UPDATE_DELAY = dataStream.readInt();
			general.FROM_IC2 = dataStream.readDouble();
			general.TO_IC2 = dataStream.readDouble();
			general.FROM_RF = dataStream.readDouble();
			general.TO_RF = dataStream.readDouble();
			general.FROM_TESLA = dataStream.readDouble();
			general.TO_TESLA = dataStream.readDouble();
			general.FROM_H2 = dataStream.readDouble();
			general.ETHENE_BURN_TIME = dataStream.readInt();
			general.ENERGY_PER_REDSTONE = dataStream.readDouble();
			general.DISASSEMBLER_USAGE = dataStream.readDouble();
			general.VOICE_PORT = dataStream.readInt();
			general.maxUpgradeMultiplier = dataStream.readInt();
			general.energyUnit = EnergyType.values()[dataStream.readInt()];
			general.minerSilkMultiplier = dataStream.readDouble();
			general.blacklistIC2 = dataStream.readBoolean();
			general.blacklistRF = dataStream.readBoolean();
			general.blacklistTesla = dataStream.readBoolean();
			general.armoredJetpackDamageRatio = dataStream.readDouble();
			general.armoredJetpackDamageMax = dataStream.readInt();
			general.aestheticWorldDamage = dataStream.readBoolean();
			general.opsBypassRestrictions = dataStream.readBoolean();
			general.thermalEvaporationSpeed = dataStream.readDouble();
			general.maxJetpackGas = dataStream.readInt();
			general.maxScubaGas = dataStream.readInt();
			general.maxFlamethrowerGas = dataStream.readInt();
			general.maxPumpRange = dataStream.readInt();
			general.pumpWaterSources = dataStream.readBoolean();
			general.maxPlenisherNodes = dataStream.readInt();
			general.evaporationHeatDissipation = dataStream.readDouble();
			general.evaporationTempMultiplier = dataStream.readDouble();
			general.evaporationSolarMultiplier = dataStream.readDouble();
			general.evaporationMaxTemp = dataStream.readDouble();
			general.energyPerHeat = dataStream.readDouble();
			general.maxEnergyPerSteam = dataStream.readDouble();
			general.superheatingHeatTransfer = dataStream.readDouble();
			general.heatPerFuelTick = dataStream.readDouble();
			general.allowTransmitterAlloyUpgrade = dataStream.readBoolean();
			general.allowChunkloading = dataStream.readBoolean();
			general.allowProtection = dataStream.readBoolean();
			
			for(MachineType type : BlockStateMachine.MachineType.getValidMachines())
			{
				machines.setEntry(type.machineName, dataStream.readBoolean());
			}
	
			usage.enrichmentChamberUsage = dataStream.readDouble();
			usage.osmiumCompressorUsage = dataStream.readDouble();
			usage.combinerUsage = dataStream.readDouble();
			usage.crusherUsage = dataStream.readDouble();
			usage.factoryUsage = dataStream.readDouble();
			usage.metallurgicInfuserUsage = dataStream.readDouble();
			usage.purificationChamberUsage = dataStream.readDouble();
			usage.energizedSmelterUsage = dataStream.readDouble();
			usage.digitalMinerUsage = dataStream.readDouble();
			usage.electricPumpUsage = dataStream.readDouble();
			usage.rotaryCondensentratorUsage = dataStream.readDouble();
			usage.oxidationChamberUsage = dataStream.readDouble();
			usage.chemicalInfuserUsage = dataStream.readDouble();
			usage.chemicalInjectionChamberUsage = dataStream.readDouble();
			usage.precisionSawmillUsage = dataStream.readDouble();
			usage.chemicalDissolutionChamberUsage = dataStream.readDouble();
			usage.chemicalWasherUsage = dataStream.readDouble();
			usage.chemicalCrystallizerUsage = dataStream.readDouble();
			usage.seismicVibratorUsage = dataStream.readDouble();
			usage.fluidicPlenisherUsage = dataStream.readDouble();
			usage.gasCentrifugeUsage = dataStream.readDouble();
			usage.heavyWaterElectrolysisUsage = dataStream.readDouble();
			usage.formulaicAssemblicatorUsage = dataStream.readDouble();
			
			Tier.readConfig(dataStream);
	
			try {
				for(IModule module : Mekanism.modulesLoaded)
				{
					module.readConfig(dataStream);
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
	
			Mekanism.proxy.onConfigSync(true);
		}
	}
}
