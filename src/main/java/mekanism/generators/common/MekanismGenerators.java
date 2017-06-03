package mekanism.generators.common;

import io.netty.buffer.ByteBuf;

import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.infuse.InfuseRegistry;
import mekanism.common.FuelHandler;
import mekanism.common.Mekanism;
import mekanism.common.MekanismBlocks;
import mekanism.common.MekanismFluids;
import mekanism.common.MekanismItems;
import mekanism.common.Tier.BaseTier;
import mekanism.common.Tier.GasTankTier;
import mekanism.common.Version;
import mekanism.common.base.IModule;
import mekanism.common.config.TypeConfigManager;
import mekanism.common.config.MekanismConfig.general;
import mekanism.common.config.MekanismConfig.generators;
import mekanism.common.multiblock.MultiblockManager;
import mekanism.common.network.PacketSimpleGui;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.ShapedMekanismRecipe;
import mekanism.common.util.MekanismUtils;
import mekanism.generators.common.block.states.BlockStateGenerator.GeneratorType;
import mekanism.generators.common.block.states.BlockStateReactor.ReactorBlockType;
import mekanism.generators.common.content.turbine.SynchronizedTurbineData;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.oredict.OreDictionary;
import buildcraft.api.fuels.BuildcraftFuelRegistry;
import buildcraft.api.fuels.IFuel;

@Mod(modid = "mekanismgenerators", name = "MekanismGenerators", version = "9.3.1", dependencies = "required-after:mekanism", guiFactory = "mekanism.generators.client.gui.GeneratorsGuiFactory")
public class MekanismGenerators implements IModule
{
	@SidedProxy(clientSide = "mekanism.generators.client.GeneratorsClientProxy", serverSide = "mekanism.generators.common.GeneratorsCommonProxy")
	public static GeneratorsCommonProxy proxy;
	
	@Instance("mekanismgenerators")
	public static MekanismGenerators instance;
	
	/** MekanismGenerators version number */
	public static Version versionNumber = new Version(9, 3, 1);
	
	public static MultiblockManager<SynchronizedTurbineData> turbineManager = new MultiblockManager<>("industrialTurbine");

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		GeneratorsBlocks.register();
		GeneratorsItems.register();
		
		proxy.preInit();
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		//Add this module to the core list
		Mekanism.modulesLoaded.add(this);
		
		//Register this module's GUI handler in the simple packet protocol
		PacketSimpleGui.handlers.add(1, proxy);
		
		//Set up the GUI handler
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GeneratorsGuiHandler());
		MinecraftForge.EVENT_BUS.register(this);

		//Load the proxy
		proxy.loadConfiguration();
		proxy.registerRegularTileEntities();
		proxy.registerSpecialTileEntities();
		
		addRecipes();
		
		//Finalization
		Mekanism.logger.info("Loaded MekanismGenerators module.");
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		if(FuelHandler.BCPresent() && BuildcraftFuelRegistry.fuel != null)
		{
			for(IFuel s : BuildcraftFuelRegistry.fuel.getFuels())
			{
				if(!(s.getFluid() == null || GasRegistry.containsGas(s.getFluid().getName())))
				{
					GasRegistry.register(new Gas(s.getFluid()));
				}
			}

			BuildcraftFuelRegistry.fuel.addFuel(MekanismFluids.Ethene.getFluid(), (int)(240 * general.TO_RF), 40 * Fluid.BUCKET_VOLUME);
		}
		
		//Update the config-dependent recipes after the recipes have actually been added in the first place
		TypeConfigManager.updateConfigRecipes(GeneratorType.getGeneratorsForConfig(), generators.generatorsManager);
		
		for(ItemStack ore : OreDictionary.getOres("dustGold"))
		{
			RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("CARBON"), 10, MekanismUtils.size(ore, 4), GeneratorsItems.Hohlraum.getEmptyItem());
		}
	}
	
	public void addRecipes()
	{
		GeneratorType.HEAT_GENERATOR.addRecipe(new ShapedMekanismRecipe(GeneratorType.HEAT_GENERATOR.getStack(), "III", "WOW", "CFC", 'I', "ingotIron", 'C', "ingotCopper", 'O', "ingotOsmium", 'F', Blocks.FURNACE, 'W', "plankWood"));
		GeneratorType.SOLAR_GENERATOR.addRecipe(new ShapedMekanismRecipe(GeneratorType.SOLAR_GENERATOR.getStack(), "SSS", "AIA", "PEP", 'S', GeneratorsItems.SolarPanel, 'A', MekanismItems.EnrichedAlloy, 'I', "ingotIron", 'P', "dustOsmium", 'E', MekanismItems.EnergyTablet.getUnchargedItem()));
		GeneratorType.ADVANCED_SOLAR_GENERATOR.addRecipe(new ShapedMekanismRecipe(GeneratorType.ADVANCED_SOLAR_GENERATOR.getStack(), "SES", "SES", "III", 'S', GeneratorType.SOLAR_GENERATOR.getStack(), 'E', MekanismItems.EnrichedAlloy, 'I', "ingotIron"));
		GeneratorType.BIO_GENERATOR.addRecipe(new ShapedMekanismRecipe(GeneratorType.BIO_GENERATOR.getStack(), "RER", "BCB", "NEN", 'R', "dustRedstone", 'E', MekanismItems.EnrichedAlloy, 'B', MekanismItems.BioFuel, 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'N', "ingotIron"));
		GeneratorType.GAS_GENERATOR.addRecipe(new ShapedMekanismRecipe(GeneratorType.GAS_GENERATOR.getStack(), "PEP", "ICI", "PEP", 'P', "ingotOsmium", 'E', MekanismItems.EnrichedAlloy, 'I', new ItemStack(MekanismBlocks.BasicBlock, 1, 8), 'C', MekanismItems.ElectrolyticCore));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(GeneratorsItems.SolarPanel), "GGG", "RAR", "PPP", 'G', "paneGlass", 'R', "dustRedstone", 'A', MekanismItems.EnrichedAlloy, 'P', "ingotOsmium"));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(GeneratorsBlocks.Generator, 1, 6), " O ", "OAO", "ECE", 'O', "ingotOsmium", 'A', MekanismItems.EnrichedAlloy, 'E', MekanismItems.EnergyTablet.getUnchargedItem(), 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC)));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(GeneratorsItems.TurbineBlade), " S ", "SAS", " S ", 'S', "ingotSteel", 'A', MekanismItems.EnrichedAlloy));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(GeneratorsBlocks.Generator, 1, 7), "SAS", "SAS", "SAS", 'S', "ingotSteel", 'A', MekanismItems.EnrichedAlloy));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(GeneratorsBlocks.Generator, 1, 8), "SAS", "CAC", "SAS", 'S', "ingotSteel", 'A', MekanismItems.EnrichedAlloy, 'C', MekanismUtils.getControlCircuit(BaseTier.ADVANCED)));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(GeneratorsBlocks.Generator, 1, 9), "SGS", "GEG", "SGS", 'S', "ingotSteel", 'G', "ingotGold", 'E', MekanismItems.EnergyTablet.getUnchargedItem()));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(GeneratorsBlocks.Generator, 4, 10), " S ", "SOS", " S ", 'S', "ingotSteel", 'O', "ingotOsmium"));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(GeneratorsBlocks.Generator, 2, 11), " I ", "ICI", " I ", 'I', new ItemStack(GeneratorsBlocks.Generator, 1, 10), 'C', MekanismUtils.getControlCircuit(BaseTier.ADVANCED)));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(GeneratorsBlocks.Generator, 2, 12), " I ", "IFI", " I ", 'I', new ItemStack(GeneratorsBlocks.Generator, 1, 10), 'F', Blocks.IRON_BARS));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(GeneratorsBlocks.Generator, 1, 13), "STS", "TBT", "STS", 'S', "ingotSteel", 'T', "ingotTin", 'B', Items.BUCKET));
		
		//Reactor Recipes
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(ReactorBlockType.REACTOR_FRAME.getStack(4), " C ", "CAC", " C ", 'C', new ItemStack(MekanismBlocks.BasicBlock, 1, 8), 'A', "alloyUltimate"));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(ReactorBlockType.REACTOR_PORT.getStack(2), " I ", "ICI", " I ", 'I', ReactorBlockType.REACTOR_FRAME.getStack(1), 'C', MekanismUtils.getControlCircuit(BaseTier.ULTIMATE)));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(ReactorBlockType.REACTOR_GLASS.getStack(4), " I ", "IGI", " I ", 'I', ReactorBlockType.REACTOR_FRAME.getStack(1), 'G', "blockGlass"));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(ReactorBlockType.REACTOR_CONTROLLER.getStack(1), "CGC", "ITI", "III", 'C', MekanismUtils.getControlCircuit(BaseTier.ULTIMATE), 'G', "paneGlass", 'I', ReactorBlockType.REACTOR_FRAME.getStack(1), 'T', MekanismUtils.getEmptyGasTank(GasTankTier.BASIC)));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(ReactorBlockType.LASER_FOCUS_MATRIX.getStack(2), " I ", "ILI", " I ", 'I', ReactorBlockType.REACTOR_GLASS.getStack(1), 'L', "blockRedstone"));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(ReactorBlockType.REACTOR_LOGIC_ADAPTER.getStack(1), " R ", "RFR", " R ", 'R', "dustRedstone", 'F', ReactorBlockType.REACTOR_FRAME.getStack(1)));

		FuelHandler.addGas(MekanismFluids.Ethene, general.ETHENE_BURN_TIME, general.FROM_H2 + generators.bioGeneration * 2 * general.ETHENE_BURN_TIME); //1mB hydrogen + 2*bioFuel/tick*200ticks/100mB * 20x efficiency bonus
	}

	@Override
	public Version getVersion() 
	{
		return versionNumber;
	}

	@Override
	public String getName()
	{
		return "Generators";
	}
	
	@Override
	public void writeConfig(ByteBuf dataStream)
	{
		dataStream.writeDouble(generators.advancedSolarGeneration);
		dataStream.writeDouble(generators.bioGeneration);
		dataStream.writeDouble(generators.heatGeneration);
		dataStream.writeDouble(generators.heatGenerationLava);
		dataStream.writeDouble(generators.heatGenerationNether);
		dataStream.writeDouble(generators.solarGeneration);
		
		dataStream.writeDouble(generators.windGenerationMin);
		dataStream.writeDouble(generators.windGenerationMax);
		
		dataStream.writeInt(generators.windGenerationMinY);
		dataStream.writeInt(generators.windGenerationMaxY);
		
		dataStream.writeInt(generators.turbineBladesPerCoil);
		dataStream.writeDouble(generators.turbineVentGasFlow);
		dataStream.writeDouble(generators.turbineDisperserGasFlow);
		dataStream.writeInt(generators.condenserRate);
		
		for(GeneratorType type : GeneratorType.getGeneratorsForConfig())
		{
			dataStream.writeBoolean(generators.generatorsManager.isEnabled(type.blockName));
		}
	}

	@Override
	public void readConfig(ByteBuf dataStream)
	{
		generators.advancedSolarGeneration = dataStream.readDouble();
		generators.bioGeneration = dataStream.readDouble();
		generators.heatGeneration = dataStream.readDouble();
		generators.heatGenerationLava = dataStream.readDouble();
		generators.heatGenerationNether = dataStream.readDouble();
		generators.solarGeneration = dataStream.readDouble();
		
		generators.windGenerationMin = dataStream.readDouble();
		generators.windGenerationMax = dataStream.readDouble();
		
		generators.windGenerationMinY = dataStream.readInt();
		generators.windGenerationMaxY = dataStream.readInt();
		
		generators.turbineBladesPerCoil = dataStream.readInt();
		generators.turbineVentGasFlow = dataStream.readDouble();
		generators.turbineDisperserGasFlow = dataStream.readDouble();
		generators.condenserRate = dataStream.readInt();
		
		for(GeneratorType type : GeneratorType.getGeneratorsForConfig())
		{
			generators.generatorsManager.setEntry(type.blockName, dataStream.readBoolean());
		}
	}
	
	@Override
	public void resetClient()
	{
		SynchronizedTurbineData.clientRotationMap.clear();
	}

	@SubscribeEvent
	public void onConfigChanged(OnConfigChangedEvent event)
	{
		if(event.getModID().equals("mekanismgenerators"))
		{
			proxy.loadConfiguration();
			TypeConfigManager.updateConfigRecipes(GeneratorType.getGeneratorsForConfig(), generators.generatorsManager);
		}
	}
}
