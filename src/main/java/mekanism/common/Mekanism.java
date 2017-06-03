package mekanism.common;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.MekanismAPI;
import mekanism.api.MekanismAPI.BoxBlacklistEvent;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.OreGas;
import mekanism.api.infuse.InfuseObject;
import mekanism.api.infuse.InfuseRegistry;
import mekanism.api.infuse.InfuseType;
import mekanism.api.transmitters.DynamicNetwork.ClientTickUpdate;
import mekanism.api.transmitters.DynamicNetwork.NetworkClientRequest;
import mekanism.api.transmitters.DynamicNetwork.TransmittersAddedEvent;
import mekanism.api.transmitters.TransmitterNetworkRegistry;
import mekanism.client.ClientTickHandler;
import mekanism.common.Tier.BaseTier;
import mekanism.common.Tier.BinTier;
import mekanism.common.Tier.EnergyCubeTier;
import mekanism.common.Tier.FactoryTier;
import mekanism.common.Tier.FluidTankTier;
import mekanism.common.Tier.GasTankTier;
import mekanism.common.Tier.InductionCellTier;
import mekanism.common.Tier.InductionProviderTier;
import mekanism.common.base.IChunkLoadHandler;
import mekanism.common.base.IFactory.RecipeType;
import mekanism.common.base.IModule;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.block.states.BlockStateTransmitter.TransmitterType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.chunkloading.ChunkManager;
import mekanism.common.config.MekanismConfig.general;
import mekanism.common.config.MekanismConfig.usage;
import mekanism.common.config.TypeConfigManager;
import mekanism.common.content.boiler.SynchronizedBoilerData;
import mekanism.common.content.entangloporter.InventoryFrequency;
import mekanism.common.content.matrix.SynchronizedMatrixData;
import mekanism.common.content.tank.SynchronizedTankData;
import mekanism.common.content.transporter.PathfinderCache;
import mekanism.common.content.transporter.TransporterManager;
import mekanism.common.entity.EntityBabySkeleton;
import mekanism.common.entity.EntityBalloon;
import mekanism.common.entity.EntityFlame;
import mekanism.common.entity.EntityObsidianTNT;
import mekanism.common.entity.EntityRobit;
import mekanism.common.frequency.Frequency;
import mekanism.common.frequency.FrequencyManager;
import mekanism.common.integration.IMCHandler;
import mekanism.common.integration.MekanismHooks;
import mekanism.common.integration.OreDictManager;
import mekanism.common.integration.multipart.MultipartMekanism;
import mekanism.common.multiblock.MultiblockManager;
import mekanism.common.network.PacketDataRequest.DataRequestMessage;
import mekanism.common.network.PacketSimpleGui;
import mekanism.common.network.PacketTransmitterUpdate.PacketType;
import mekanism.common.network.PacketTransmitterUpdate.TransmitterUpdateMessage;
import mekanism.common.recipe.BinRecipe;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.ShapedMekanismRecipe;
import mekanism.common.recipe.ShapelessMekanismRecipe;
import mekanism.common.recipe.inputs.ItemStackInput;
import mekanism.common.recipe.machines.SmeltingRecipe;
import mekanism.common.recipe.outputs.ItemStackOutput;
import mekanism.common.security.SecurityFrequency;
import mekanism.common.tile.TileEntityAdvancedBoundingBlock;
import mekanism.common.tile.TileEntityAmbientAccumulator;
import mekanism.common.tile.TileEntityBoundingBlock;
import mekanism.common.tile.TileEntityCardboardBox;
import mekanism.common.tile.TileEntityChemicalInfuser;
import mekanism.common.tile.TileEntityChemicalOxidizer;
import mekanism.common.tile.TileEntityChemicalWasher;
import mekanism.common.tile.TileEntityElectricPump;
import mekanism.common.tile.TileEntityElectrolyticSeparator;
import mekanism.common.tile.TileEntityFluidicPlenisher;
import mekanism.common.tile.TileEntityFuelwoodHeater;
import mekanism.common.tile.TileEntityGlowPanel;
import mekanism.common.tile.TileEntityInductionCasing;
import mekanism.common.tile.TileEntityInductionCell;
import mekanism.common.tile.TileEntityInductionPort;
import mekanism.common.tile.TileEntityInductionProvider;
import mekanism.common.tile.TileEntityLaser;
import mekanism.common.tile.TileEntityLaserAmplifier;
import mekanism.common.tile.TileEntityLaserTractorBeam;
import mekanism.common.tile.TileEntityOredictionificator;
import mekanism.common.tile.TileEntityPressureDisperser;
import mekanism.common.tile.TileEntityRotaryCondensentrator;
import mekanism.common.tile.TileEntityStructuralGlass;
import mekanism.common.tile.TileEntitySuperheatingElement;
import mekanism.common.tile.TileEntityThermalEvaporationBlock;
import mekanism.common.tile.TileEntityThermalEvaporationValve;
import mekanism.common.tile.prefab.TileEntityElectricBlock;
import mekanism.common.transmitters.grid.EnergyNetwork.EnergyTransferEvent;
import mekanism.common.transmitters.grid.FluidNetwork.FluidTransferEvent;
import mekanism.common.transmitters.grid.GasNetwork.GasTransferEvent;
import mekanism.common.util.MekanismUtils;
import mekanism.common.voice.VoiceServerManager;
import mekanism.common.world.GenHandler;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.IFuelHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.RecipeSorter;
import net.minecraftforge.oredict.RecipeSorter.Category;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.authlib.GameProfile;

/**
 * Mekanism - a Minecraft mod
 * @author AidanBrady
 *
 */
@Mod(modid = "mekanism", name = "Mekanism", version = "9.3.1", guiFactory = "mekanism.client.gui.ConfigGuiFactory",
		dependencies = 	"after:mcmultipart;" +
						"after:jei;" +
						"after:buildcraft;" +
						"after:buildcraftapi;" +
						"after:ic2;" +
						"after:cofhcore;" +
						"after:computercraft;" +
						"after:galacticraft api;" +
						"after:metallurgycore")
public class Mekanism
{
	/** Mekanism Packet Pipeline */
	public static PacketHandler packetHandler = new PacketHandler();

	/** Mekanism logger instance */
	public static Logger logger = LogManager.getLogger("Mekanism");
	
	/** Mekanism proxy instance */
	@SidedProxy(clientSide = "mekanism.client.ClientProxy", serverSide = "mekanism.common.CommonProxy")
	public static CommonProxy proxy;
	
    /** Mekanism mod instance */
	@Instance("mekanism")
    public static Mekanism instance;
    
    /** Mekanism hooks instance */
    public static MekanismHooks hooks = new MekanismHooks();
    
    /** Mekanism configuration instance */
    public static Configuration configuration;
    
	/** Mekanism version number */
	public static Version versionNumber = new Version(9, 3, 1);
	
	/** MultiblockManagers for various structrures */
	public static MultiblockManager<SynchronizedTankData> tankManager = new MultiblockManager<>("dynamicTank");
	public static MultiblockManager<SynchronizedMatrixData> matrixManager = new MultiblockManager<>("inductionMatrix");
	public static MultiblockManager<SynchronizedBoilerData> boilerManager = new MultiblockManager<>("thermoelectricBoiler");
	
	/** FrequencyManagers for various networks */
	public static FrequencyManager publicTeleporters = new FrequencyManager(Frequency.class, Frequency.TELEPORTER);
	public static Map<UUID, FrequencyManager> privateTeleporters = new HashMap<>();
	
	public static FrequencyManager publicEntangloporters = new FrequencyManager(InventoryFrequency.class, InventoryFrequency.ENTANGLOPORTER);
	public static Map<UUID, FrequencyManager> privateEntangloporters = new HashMap<>();
	
	public static FrequencyManager securityFrequencies = new FrequencyManager(SecurityFrequency.class, SecurityFrequency.SECURITY);
	
	/** Mekanism creative tab */
	public static CreativeTabMekanism tabMekanism = new CreativeTabMekanism();
	
	/** List of Mekanism modules loaded */
	public static List<IModule> modulesLoaded = new ArrayList<>();
	
	/** The latest version number which is received from the Mekanism server */
	public static String latestVersionNumber;
	
	/** The recent news which is received from the Mekanism server */
	public static String recentNews;
	
	/** The VoiceServer manager for walkie talkies */
	public static VoiceServerManager voiceManager;
	
	/** A list of the usernames of players who have donated to Mekanism. */
	public static List<String> donators = new ArrayList<>();
	
	/** The server's world tick handler. */
	public static CommonWorldTickHandler worldTickHandler = new CommonWorldTickHandler();
	
	/** The Mekanism world generation handler. */
	public static GenHandler genHandler = new GenHandler();
	
	/** The version of ore generation in this version of Mekanism. Increment this every time the default ore generation changes. */
	public static int baseWorldGenVersion = 0;
	
	/** The GameProfile used by the dummy Mekanism player */
	public static GameProfile gameProfile = new GameProfile(UUID.nameUUIDFromBytes("mekanism.common".getBytes()), "[Mekanism]");
	
	public static KeySync keyMap = new KeySync();
	
	public static Set<String> jetpackOn = new HashSet<>();
	public static Set<String> gasmaskOn = new HashSet<>();
	public static Set<String> flamethrowerActive = new HashSet<>();
	
	public static Set<Coord4D> activeVibrators = new HashSet<>();
	
	static {		
		MekanismFluids.register();
	}

	/**
	 * Adds all in-game crafting, smelting and machine recipes.
	 */
	public void addRecipes()
	{
		//Storage Recipes
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 3),
			"***", "***", "***", '*', new ItemStack(Items.COAL, 1, 1)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(Items.COAL, 9, 1),
			"*", '*', new ItemStack(MekanismBlocks.BasicBlock, 1, 3)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 2),
			"***", "***", "***", '*', "ingotRefinedObsidian"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Ingot, 9, 0),
			"*", '*', new ItemStack(MekanismBlocks.BasicBlock, 1, 2)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 4),
			"***", "***", "***", '*', "ingotRefinedGlowstone"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Ingot, 9, 3),
			"*", '*', new ItemStack(MekanismBlocks.BasicBlock, 1, 4)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 0),
			"XXX", "XXX", "XXX", 'X', "ingotOsmium"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Ingot, 9, 1),
			"*", '*', new ItemStack(MekanismBlocks.BasicBlock, 1, 0)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 1),
			"***", "***", "***", '*', "ingotBronze"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Ingot, 9, 2),
			"*", '*', new ItemStack(MekanismBlocks.BasicBlock, 1, 1)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 5),
			"***", "***", "***", '*', "ingotSteel"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Ingot, 9, 4),
			"*", '*', new ItemStack(MekanismBlocks.BasicBlock, 1, 5)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 12),
			"***", "***", "***", '*', "ingotCopper"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Ingot, 9, 5),
			"*", '*', new ItemStack(MekanismBlocks.BasicBlock, 1, 12)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 13),
			"***", "***", "***", '*', "ingotTin"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Ingot, 9, 6),
			"*", '*', new ItemStack(MekanismBlocks.BasicBlock, 1, 13)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.SaltBlock),
			"**", "**", '*', MekanismItems.Salt
		));
		
		//Base Recipes
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.ObsidianTNT, 1),
			"***", "XXX", "***", '*', Blocks.OBSIDIAN, 'X', Blocks.TNT
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.ElectricBow.getUnchargedItem(),
			" AB", "E B", " AB", 'A', MekanismItems.EnrichedAlloy, 'B', Items.STRING, 'E', MekanismItems.EnergyTablet.getUnchargedItem()
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.EnergyTablet.getUnchargedItem(),
			"RCR", "ECE", "RCR", 'C', "ingotGold", 'R', "dustRedstone", 'E', MekanismItems.EnrichedAlloy
		));
		MachineType.ENRICHMENT_CHAMBER.addRecipe(new ShapedMekanismRecipe(MachineType.ENRICHMENT_CHAMBER.getStack(),
			"RCR", "iIi", "RCR", 'i', "ingotIron", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'R', "alloyBasic", 'I', new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		MachineType.OSMIUM_COMPRESSOR.addRecipe(new ShapedMekanismRecipe(MachineType.OSMIUM_COMPRESSOR.getStack(),
			"ECE", "BIB", "ECE", 'E', "alloyAdvanced", 'C', MekanismUtils.getControlCircuit(BaseTier.ADVANCED), 'B', Items.BUCKET, 'I', new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		MachineType.COMBINER.addRecipe(new ShapedMekanismRecipe(MachineType.COMBINER.getStack(),
			"RCR", "SIS", "RCR", 'S', "cobblestone", 'C', MekanismUtils.getControlCircuit(BaseTier.ELITE), 'R', "alloyElite", 'I', new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		MachineType.CRUSHER.addRecipe(new ShapedMekanismRecipe(MachineType.CRUSHER.getStack(),
			"RCR", "LIL", "RCR", 'R', "dustRedstone", 'L', Items.LAVA_BUCKET, 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'I', new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.SpeedUpgrade),
			" G ", "ADA", " G ", 'G', "blockGlass", 'A', MekanismItems.EnrichedAlloy, 'D', "dustOsmium"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.EnergyUpgrade),
			" G ", "ADA", " G ", 'G', "blockGlass", 'A', MekanismItems.EnrichedAlloy, 'D', "dustGold"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.GasUpgrade),
			" G ", "ADA", " G ", 'G', "blockGlass", 'A', MekanismItems.EnrichedAlloy, 'D', "dustIron"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.FilterUpgrade),
			" G ", "ADA", " G ", 'G', "blockGlass", 'A', MekanismItems.EnrichedAlloy, 'D', "dustTin"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.MufflingUpgrade),
			" G ", "ADA", " G ", 'G', "blockGlass", 'A', MekanismItems.EnrichedAlloy, 'D', "dustSteel"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.AnchorUpgrade),
				" G ", "ADA", " G ", 'G', "blockGlass", 'A', MekanismItems.EnrichedAlloy, 'D', "dustDiamond"
			));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.AtomicDisassembler.getUnchargedItem(),
			"AEA", "ACA", " O ", 'A', MekanismItems.EnrichedAlloy, 'E', MekanismItems.EnergyTablet.getUnchargedItem(), 'C', MekanismItems.AtomicAlloy, 'O', "ingotRefinedObsidian"
		));
		MachineType.METALLURGIC_INFUSER.addRecipe(new ShapedMekanismRecipe(MachineType.METALLURGIC_INFUSER.getStack(),
			"IFI", "ROR", "IFI", 'I', "ingotIron", 'F', Blocks.FURNACE, 'R', "dustRedstone", 'O', "ingotOsmium"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.TeleportationCore),
			"LAL", "GDG", "LAL", 'L', new ItemStack(Items.DYE, 1, 4), 'A', MekanismItems.AtomicAlloy, 'G', "ingotGold", 'D', Items.DIAMOND
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.PortableTeleporter.getUnchargedItem(),
			" E ", "CTC", " E ", 'E', MekanismItems.EnergyTablet.getUnchargedItem(), 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'T', MekanismItems.TeleportationCore
		));
		MachineType.TELEPORTER.addRecipe(new ShapedMekanismRecipe(MachineType.TELEPORTER.getStack(),
			"COC", "OTO", "COC", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'O', new ItemStack(MekanismBlocks.BasicBlock, 1, 8), 'T', MekanismItems.TeleportationCore
		));
		MachineType.PURIFICATION_CHAMBER.addRecipe(new ShapedMekanismRecipe(MachineType.PURIFICATION_CHAMBER.getStack(),
			"ECE", "ORO", "ECE", 'C', MekanismUtils.getControlCircuit(BaseTier.ADVANCED), 'E', "alloyAdvanced", 'O', "ingotOsmium", 'R', MachineType.ENRICHMENT_CHAMBER.getStack()
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.Configurator.getUnchargedItem(),
			" L ", "AEA", " S ", 'L', new ItemStack(Items.DYE, 1, 4), 'A', MekanismItems.EnrichedAlloy, 'E', MekanismItems.EnergyTablet.getUnchargedItem(), 'S', Items.STICK
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 9, 7),
			"OOO", "OGO", "OOO", 'O', "ingotRefinedObsidian", 'G', "ingotRefinedGlowstone"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 8), "SGS", "GPG", "SGS", 'S', "ingotSteel", 'P', "ingotOsmium", 'G', "blockGlass"));
		MachineType.ENERGIZED_SMELTER.addRecipe(new ShapedMekanismRecipe(MachineType.ENERGIZED_SMELTER.getStack(),
			"RCR", "GIG", "RCR", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'R', "alloyBasic", 'G', "blockGlass", 'I', new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		MachineType.ELECTRIC_PUMP.addRecipe(new ShapedMekanismRecipe(MachineType.ELECTRIC_PUMP.getStack(),
			" B ", "ECE", "OOO", 'B', Items.BUCKET, 'E', MekanismItems.EnrichedAlloy, 'C', new ItemStack(MekanismBlocks.BasicBlock, 1, 8), 'O', "ingotOsmium"
		));
		MachineType.PERSONAL_CHEST.addRecipe(new ShapedMekanismRecipe(MachineType.PERSONAL_CHEST.getStack(),
			"SGS", "CcC", "SSS", 'S', "ingotSteel", 'G', "blockGlass", 'C', "chestWood", 'c', MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 4, 9),
			" I ", "IBI", " I ", 'I', "ingotSteel", 'B', Items.BUCKET
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 4, 10),
			" I ", "IGI", " I ", 'I', "ingotSteel", 'G', "blockGlass"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 2, 11),
			" I ", "ICI", " I ", 'I', new ItemStack(MekanismBlocks.BasicBlock, 1, 9), 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		MachineType.CHARGEPAD.addRecipe(new ShapedMekanismRecipe(MachineType.CHARGEPAD.getStack(),
			"PPP", "SES", 'P', Blocks.STONE_PRESSURE_PLATE, 'S', "ingotSteel", 'E', MekanismItems.EnergyTablet.getUnchargedItem()
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.Robit.getUnchargedItem(),
			" S ", "ECE", "OIO", 'S', "ingotSteel", 'E', MekanismItems.EnergyTablet.getUnchargedItem(), 'C', MekanismItems.AtomicAlloy, 'O', "ingotRefinedObsidian", 'I', MachineType.PERSONAL_CHEST.getStack()
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.NetworkReader.getUnchargedItem(),
			" G ", "AEA", " I ", 'G', "blockGlass", 'A', MekanismItems.EnrichedAlloy, 'E', MekanismItems.EnergyTablet.getUnchargedItem(), 'I', "ingotSteel"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.WalkieTalkie),
			"  O", "SCS", " S ", 'O', "ingotOsmium", 'S', "ingotSteel", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		MachineType.LOGISTICAL_SORTER.addRecipe(new ShapedMekanismRecipe(MachineType.LOGISTICAL_SORTER.getStack(),
			"IPI", "ICI", "III", 'I', "ingotIron", 'P', Blocks.PISTON, 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		MachineType.DIGITAL_MINER.addRecipe(new ShapedMekanismRecipe(MachineType.DIGITAL_MINER.getStack(),
			"ACA", "SES", "TIT", 'A', MekanismItems.AtomicAlloy, 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'S', MachineType.LOGISTICAL_SORTER.getStack(), 'E', MekanismItems.Robit.getUnchargedItem(),
                'I', new ItemStack(MekanismBlocks.BasicBlock, 1, 8), 'T', MekanismItems.TeleportationCore
		));
		MachineType.ROTARY_CONDENSENTRATOR.addRecipe(new ShapedMekanismRecipe(MachineType.ROTARY_CONDENSENTRATOR.getStack(),
			"GCG", "tEI", "GCG", 'G', "blockGlass", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 't', MekanismUtils.getEmptyGasTank(GasTankTier.BASIC), 'E', MekanismItems.EnergyTablet.getUnchargedItem(),
                'T', new ItemStack(MekanismBlocks.BasicBlock, 1, 9), 'I', MekanismUtils.getEmptyFluidTank(FluidTankTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.Jetpack.getEmptyItem(),
			"SCS", "TGT", " T ", 'S', "ingotSteel", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'T', "ingotTin", 'G', MekanismUtils.getEmptyGasTank(GasTankTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Dictionary),
			"C", "B", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'B', Items.BOOK
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.GasMask),
			" S ", "GCG", "S S", 'S', "ingotSteel", 'G', "blockGlass", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.ScubaTank.getEmptyItem(),
			" C ", "ATA", "SSS", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'A', MekanismItems.EnrichedAlloy, 'S', "ingotSteel"
		));
		MachineType.CHEMICAL_OXIDIZER.addRecipe(new ShapedMekanismRecipe(MachineType.CHEMICAL_OXIDIZER.getStack(),
			"ACA", "ERG", "ACA", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'R', new ItemStack(MekanismBlocks.BasicBlock, 1, 9), 'G', MekanismUtils.getEmptyGasTank(GasTankTier.BASIC), 'E', MachineType.PERSONAL_CHEST.getStack(), 'A', MekanismItems.EnrichedAlloy
		));
		MachineType.CHEMICAL_INFUSER.addRecipe(new ShapedMekanismRecipe(MachineType.CHEMICAL_INFUSER.getStack(),
			"ACA", "GRG", "ACA", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'R', new ItemStack(MekanismBlocks.BasicBlock, 1, 9), 'G', MekanismUtils.getEmptyGasTank(GasTankTier.BASIC), 'A', MekanismItems.EnrichedAlloy
		));
		MachineType.CHEMICAL_INJECTION_CHAMBER.addRecipe(new ShapedMekanismRecipe(MachineType.CHEMICAL_INJECTION_CHAMBER.getStack(),
			"RCR", "GPG", "RCR", 'C', MekanismUtils.getControlCircuit(BaseTier.ELITE), 'R', "alloyElite", 'G', "ingotGold", 'P', MachineType.PURIFICATION_CHAMBER.getStack()
		));
		MachineType.ELECTROLYTIC_SEPARATOR.addRecipe(new ShapedMekanismRecipe(MachineType.ELECTROLYTIC_SEPARATOR.getStack(),
			"IRI", "ECE", "IRI", 'I', "ingotIron", 'R', "dustRedstone", 'E', MekanismItems.EnrichedAlloy, 'C', MekanismItems.ElectrolyticCore
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.ElectrolyticCore),
			"EPE", "IEG", "EPE", 'E', MekanismItems.EnrichedAlloy, 'P', "dustOsmium", 'I', "dustIron", 'G', "dustGold"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.CardboardBox),
			"SS", "SS", 'S', "pulpWood"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(Items.PAPER, 6),
			"SSS", 'S', MekanismItems.Sawdust
		));
		MachineType.PRECISION_SAWMILL.addRecipe(new ShapedMekanismRecipe(MachineType.PRECISION_SAWMILL.getStack(),
			"ICI", "ASA", "ICI", 'I', "ingotIron", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'A', MekanismItems.EnrichedAlloy, 'S', new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 14),
			"CGC", "IBI", "III", 'C', MekanismUtils.getControlCircuit(BaseTier.ADVANCED), 'G', "paneGlass", 'I', new ItemStack(MekanismBlocks.BasicBlock2, 1, 0), 'B', Items.BUCKET
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock, 1, 15),
			" I ", "ICI", " I ", 'I', new ItemStack(MekanismBlocks.BasicBlock2, 1, 0), 'C', MekanismUtils.getControlCircuit(BaseTier.ADVANCED)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock2, 4, 0),
			" S ", "SCS", " S ", 'C', "ingotCopper", 'S', "ingotSteel"
		));
		MachineType.CHEMICAL_DISSOLUTION_CHAMBER.addRecipe(new ShapedMekanismRecipe(MachineType.CHEMICAL_DISSOLUTION_CHAMBER.getStack(),
			"CGC", "EAE", "CGC", 'G', MekanismUtils.getEmptyGasTank(GasTankTier.BASIC), 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'A', MekanismItems.AtomicAlloy, 'E', MekanismItems.EnrichedAlloy
		));
		MachineType.CHEMICAL_WASHER.addRecipe(new ShapedMekanismRecipe(MachineType.CHEMICAL_WASHER.getStack(),
			"CWC", "EIE", "CGC", 'W', Items.BUCKET, 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'E', MekanismItems.EnrichedAlloy, 'G', MekanismUtils.getEmptyGasTank(GasTankTier.BASIC), 'I', new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		MachineType.CHEMICAL_CRYSTALLIZER.addRecipe(new ShapedMekanismRecipe(MachineType.CHEMICAL_CRYSTALLIZER.getStack(),
			"CGC", "ASA", "CGC", 'G', MekanismUtils.getEmptyGasTank(GasTankTier.BASIC), 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'A', MekanismItems.AtomicAlloy, 'S', new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.FreeRunners.getUnchargedItem(),
			"C C", "A A", "T T", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'A', MekanismItems.EnrichedAlloy, 'T', MekanismItems.EnergyTablet.getUnchargedItem()
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.ArmoredJetpack.getEmptyItem(),
			"D D", "BSB", " J ", 'D', "dustDiamond", 'B', "ingotBronze", 'S', "blockSteel", 'J', MekanismItems.Jetpack.getEmptyItem()
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.ConfigurationCard),
			" A ", "ACA", " A ", 'A', MekanismItems.EnrichedAlloy, 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.SeismicReader.getUnchargedItem(),
			"SLS", "STS", "SSS", 'S', "ingotSteel", 'L', new ItemStack(Items.DYE, 1, 4), 'T', MekanismItems.EnergyTablet.getUnchargedItem()
		));
		MachineType.SEISMIC_VIBRATOR.addRecipe(new ShapedMekanismRecipe(MachineType.SEISMIC_VIBRATOR.getStack(),
			"TLT", "CIC", "TTT", 'T', "ingotTin", 'L', new ItemStack(Items.DYE, 1, 4), 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'I', new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		MachineType.PRESSURIZED_REACTION_CHAMBER.addRecipe(new ShapedMekanismRecipe(MachineType.PRESSURIZED_REACTION_CHAMBER.getStack(),
			"TET", "CIC", "GFG", 'S', "ingotSteel", 'E', MekanismItems.EnrichedAlloy, 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'G', MekanismUtils.getEmptyGasTank(GasTankTier.BASIC),
                'I', MachineType.ENRICHMENT_CHAMBER.getStack(), 'F', new ItemStack(MekanismBlocks.BasicBlock, 1, 9)
		));
		MachineType.FLUIDIC_PLENISHER.addRecipe(new ShapedMekanismRecipe(MachineType.FLUIDIC_PLENISHER.getStack(),
			"TTT", "CPC", "TTT", 'P', MachineType.ELECTRIC_PUMP.getStack(), 'T', "ingotTin", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(Blocks.RAIL, 24),
			"O O", "OSO", "O O", 'O', "ingotOsmium", 'S', "stickWood"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.Flamethrower.getEmptyItem(),
			"TTT", "TGS", "BCB", 'T', "ingotTin", 'G', MekanismUtils.getEmptyGasTank(GasTankTier.BASIC), 'S', Items.FLINT_AND_STEEL, 'B', "ingotBronze", 'C', MekanismUtils.getControlCircuit(BaseTier.ADVANCED)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismItems.GaugeDropper.getEmptyItem(),
			" O ", "G G", "GGG", 'O', "ingotOsmium", 'G', "paneGlass"
		));
		MachineType.SOLAR_NEUTRON_ACTIVATOR.addRecipe(new ShapedMekanismRecipe(MachineType.SOLAR_NEUTRON_ACTIVATOR.getStack(),
			"APA", "CSC", "BBB", 'A', "alloyElite", 'S', new ItemStack(MekanismBlocks.BasicBlock, 1, 8), 'P', new ItemStack(MekanismItems.Polyethene, 1, 2), 'B', "ingotBronze", 'C', MekanismUtils.getControlCircuit(BaseTier.ELITE)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock2, 4, 1),
			" S ", "SES", " S ", 'S', "ingotSteel", 'E', MekanismItems.EnergyTablet.getUnchargedItem()
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock2, 2, 2),
			" I ", "ICI", " I ", 'I', new ItemStack(MekanismBlocks.BasicBlock2, 1, 1), 'C', MekanismUtils.getControlCircuit(BaseTier.ELITE)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.TierInstaller, 1, 0), "RCR", "iWi", "RCR", 'R', "alloyBasic", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'i', "ingotIron", 'W', "plankWood"));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.TierInstaller, 1, 1), "ECE", "oWo", "ECE", 'E', "alloyAdvanced", 'C', MekanismUtils.getControlCircuit(BaseTier.ADVANCED), 'o', "ingotOsmium", 'W', "plankWood"));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.TierInstaller, 1, 2), "RCR", "gWg", "RCR", 'R', "alloyElite", 'C', MekanismUtils.getControlCircuit(BaseTier.ELITE), 'g', "ingotGold", 'W', "plankWood"));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.TierInstaller, 1, 3), "RCR", "dWd", "RCR", 'R', "alloyUltimate", 'C', MekanismUtils.getControlCircuit(BaseTier.ULTIMATE), 'd', "gemDiamond", 'W', "plankWood"));
		MachineType.OREDICTIONIFICATOR.addRecipe(new ShapedMekanismRecipe(MachineType.OREDICTIONIFICATOR.getStack(),
			"SGS", "CBC", "SWS", 'S', "ingotSteel", 'G', "paneGlass", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'B', MekanismItems.Dictionary, 'W', "chestWood"
		));
		MachineType.LASER.addRecipe(new ShapedMekanismRecipe(MachineType.LASER.getStack(),
			"RE ", "RCD", "RE ", 'R', "alloyElite", 'E', MekanismItems.EnergyTablet.getUnchargedItem(), 'C', new ItemStack(MekanismBlocks.BasicBlock, 1, 8), 'D', "gemDiamond"
		));
		MachineType.LASER_AMPLIFIER.addRecipe(new ShapedMekanismRecipe(MachineType.LASER_AMPLIFIER.getStack(),
			"SSS", "SED", "SSS", 'S', "ingotSteel", 'E', MekanismUtils.getEnergyCube(EnergyCubeTier.BASIC), 'D', "gemDiamond"
		));
		MachineType.LASER_TRACTOR_BEAM.addRecipe(new ShapedMekanismRecipe(MachineType.LASER_TRACTOR_BEAM.getStack(),
			"C", "F", 'C', MachineType.PERSONAL_CHEST.getStack(), 'F', MachineType.LASER_AMPLIFIER.getStack()
		));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock2, 1, 6),
			"SFS", "FAF", "SFS", 'S', "ingotSteel", 'A', MekanismItems.EnrichedAlloy, 'F', Blocks.IRON_BARS
		));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock2, 4, 7),
			" S ", "SIS", " S ", 'S', "ingotSteel", 'I', "ingotIron"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock2, 2, 8),
			" I ", "ICI", " I ", 'I', new ItemStack(MekanismBlocks.BasicBlock2, 1, 7), 'C', MekanismUtils.getControlCircuit(BaseTier.ADVANCED)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock2, 1, 5),
			"ACA", "CIC", "ACA", 'I', new ItemStack(MekanismBlocks.BasicBlock, 1, 8), 'C', "ingotCopper", 'A', "alloyBasic"
		));
		MachineType.RESISTIVE_HEATER.addRecipe(new ShapedMekanismRecipe(MachineType.RESISTIVE_HEATER.getStack(), "CRC", "RHR", "CEC", 'C', "ingotTin", 'R', "dustRedstone", 'H', new ItemStack(MekanismBlocks.BasicBlock2, 1, 5), 'E', MekanismItems.EnergyTablet.getUnchargedItem()));
		MachineType.QUANTUM_ENTANGLOPORTER.addRecipe(new ShapedMekanismRecipe(MachineType.QUANTUM_ENTANGLOPORTER.getStack(), "OCO", "ATA", "OCO", 'O', "ingotRefinedObsidian", 'C', MekanismUtils.getControlCircuit(BaseTier.ULTIMATE), 'A', "alloyUltimate", 'T', MekanismItems.TeleportationCore));
		MachineType.FORMULAIC_ASSEMBLICATOR.addRecipe(new ShapedMekanismRecipe(MachineType.FORMULAIC_ASSEMBLICATOR.getStack(), "STS", "BIB", "SCS", 'S', "ingotSteel", 'T', Blocks.CRAFTING_TABLE, 'B', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'I', new ItemStack(MekanismBlocks.BasicBlock, 1, 8), 'C', "chestWood"));
		CraftingManager.getInstance().getRecipeList().add(new ShapelessMekanismRecipe(new ItemStack(MekanismItems.CraftingFormula), Items.PAPER, MekanismUtils.getControlCircuit(BaseTier.BASIC)));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.BasicBlock2, 1, 9), "SGS", "CIC", "STS", 'S', "ingotSteel", 'G', "blockGlass", 'C', MekanismUtils.getControlCircuit(BaseTier.ELITE), 'I', new ItemStack(MekanismBlocks.BasicBlock, 1, 8),
				'T', MekanismItems.TeleportationCore));
		MachineType.FUELWOOD_HEATER.addRecipe(new ShapedMekanismRecipe(MachineType.FUELWOOD_HEATER.getStack(), "SCS", "FHF", "SSS", 'S', "ingotSteel", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'F', Blocks.FURNACE, 'H', new ItemStack(MekanismBlocks.BasicBlock, 1, 8)));
		
		//Energy Cube recipes
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getEnergyCube(EnergyCubeTier.BASIC),
			"RTR", "iIi", "RTR", 'R', "alloyBasic", 'i', "ingotIron", 'T', MekanismItems.EnergyTablet.getUnchargedItem(), 'I', new ItemStack(MekanismBlocks.BasicBlock, 1, 8)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getEnergyCube(EnergyCubeTier.ADVANCED),
			"ETE", "oBo", "ETE", 'E', "alloyAdvanced", 'o', "ingotOsmium", 'T', MekanismItems.EnergyTablet.getUnchargedItem(), 'B', MekanismUtils.getEnergyCube(EnergyCubeTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getEnergyCube(EnergyCubeTier.ELITE),
			"RTR", "gAg", "RTR", 'R', "alloyElite", 'g', "ingotGold", 'T', MekanismItems.EnergyTablet.getUnchargedItem(), 'A', MekanismUtils.getEnergyCube(EnergyCubeTier.ADVANCED)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getEnergyCube(EnergyCubeTier.ULTIMATE),
			"ATA", "dEd", "ATA", 'A', "alloyUltimate", 'd', "gemDiamond", 'T', MekanismItems.EnergyTablet.getUnchargedItem(), 'E', MekanismUtils.getEnergyCube(EnergyCubeTier.ELITE)
		));
		
		//Gas Tank Recipes
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getEmptyGasTank(GasTankTier.BASIC), "APA", "P P", "APA", 'P', "ingotOsmium", 'A', "alloyBasic"));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getEmptyGasTank(GasTankTier.ADVANCED), "APA", "PTP", "APA", 'P', "ingotOsmium", 'A', "alloyAdvanced", 'T', MekanismUtils.getEmptyGasTank(GasTankTier.BASIC)));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getEmptyGasTank(GasTankTier.ELITE), "APA", "PTP", "APA", 'P', "ingotOsmium", 'A', "alloyElite", 'T', MekanismUtils.getEmptyGasTank(GasTankTier.ADVANCED)));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getEmptyGasTank(GasTankTier.ULTIMATE), "APA", "PTP", "APA", 'P', "ingotOsmium", 'A', "alloyUltimate", 'T', MekanismUtils.getEmptyGasTank(GasTankTier.ELITE)));
		
		//Fluid Tank Recipes
		MachineType.FLUID_TANK.addRecipe(new ShapedMekanismRecipe(MekanismUtils.getEmptyFluidTank(FluidTankTier.BASIC), "AIA", "I I", "AIA", 'I', "ingotIron", 'A', "alloyBasic"));
		MachineType.FLUID_TANK.addRecipe(new ShapedMekanismRecipe(MekanismUtils.getEmptyFluidTank(FluidTankTier.ADVANCED), "AIA", "ITI", "AIA", 'I', "ingotIron", 'A', "alloyAdvanced", 'T', MekanismUtils.getEmptyFluidTank(FluidTankTier.BASIC)));
		MachineType.FLUID_TANK.addRecipe(new ShapedMekanismRecipe(MekanismUtils.getEmptyFluidTank(FluidTankTier.ELITE), "AIA", "ITI", "AIA", 'I', "ingotIron", 'A', "alloyElite", 'T', MekanismUtils.getEmptyFluidTank(FluidTankTier.ADVANCED)));
		MachineType.FLUID_TANK.addRecipe(new ShapedMekanismRecipe(MekanismUtils.getEmptyFluidTank(FluidTankTier.ULTIMATE), "AIA", "ITI", "AIA", 'I', "ingotIron", 'A', "alloyUltimate", 'T', MekanismUtils.getEmptyFluidTank(FluidTankTier.ELITE)));
		
		//Bin recipes
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getBin(BinTier.BASIC), "SCS", "A A", "SSS", 'S', "cobblestone", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'A', "alloyBasic"));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getBin(BinTier.ADVANCED), "SCS", "ABA", "SSS", 'S', "cobblestone", 'C', MekanismUtils.getControlCircuit(BaseTier.ADVANCED), 'A', "alloyAdvanced", 'B', MekanismUtils.getBin(BinTier.BASIC)));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getBin(BinTier.ELITE), "SCS", "ABA", "SSS", 'S', "cobblestone", 'C', MekanismUtils.getControlCircuit(BaseTier.ELITE), 'A', "alloyElite", 'B', MekanismUtils.getBin(BinTier.ADVANCED)));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getBin(BinTier.ULTIMATE), "SCS", "ABA", "SSS", 'S', "cobblestone", 'C', MekanismUtils.getControlCircuit(BaseTier.ULTIMATE), 'A', "alloyUltimate", 'B', MekanismUtils.getBin(BinTier.ELITE)));
		
		//Induction Cell recipes
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getInductionCell(InductionCellTier.BASIC),
			"LTL", "TET", "LTL", 'T', MekanismItems.EnergyTablet.getUnchargedItem(), 'E', MekanismUtils.getEnergyCube(EnergyCubeTier.BASIC), 'L', "dustLithium"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getInductionCell(InductionCellTier.ADVANCED),
			"TCT", "CEC", "TCT", 'T', MekanismItems.EnergyTablet.getUnchargedItem(), 'E', MekanismUtils.getEnergyCube(EnergyCubeTier.ADVANCED), 'C', MekanismUtils.getInductionCell(InductionCellTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getInductionCell(InductionCellTier.ELITE),
			"TCT", "CEC", "TCT", 'T', MekanismItems.EnergyTablet.getUnchargedItem(), 'E', MekanismUtils.getEnergyCube(EnergyCubeTier.ELITE), 'C', MekanismUtils.getInductionCell(InductionCellTier.ADVANCED)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getInductionCell(InductionCellTier.ULTIMATE),
			"TCT", "CEC", "TCT", 'T', MekanismItems.EnergyTablet.getUnchargedItem(), 'E', MekanismUtils.getEnergyCube(EnergyCubeTier.ULTIMATE), 'C', MekanismUtils.getInductionCell(InductionCellTier.ELITE)
		));
		
		//Induction Provider recipes
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getInductionProvider(InductionProviderTier.BASIC),
			"LCL", "CEC", "LCL", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'E', MekanismUtils.getEnergyCube(EnergyCubeTier.BASIC), 'L', "dustLithium"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getInductionProvider(InductionProviderTier.ADVANCED),
			"CPC", "PEP", "CPC", 'C', MekanismUtils.getControlCircuit(BaseTier.ADVANCED), 'E', MekanismUtils.getEnergyCube(EnergyCubeTier.ADVANCED), 'P', MekanismUtils.getInductionProvider(InductionProviderTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getInductionProvider(InductionProviderTier.ELITE),
			"CPC", "PEP", "CPC", 'C', MekanismUtils.getControlCircuit(BaseTier.ELITE), 'E', MekanismUtils.getEnergyCube(EnergyCubeTier.ELITE), 'P', MekanismUtils.getInductionProvider(InductionProviderTier.ADVANCED)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getInductionProvider(InductionProviderTier.ULTIMATE),
			"CPC", "PEP", "CPC", 'C', MekanismUtils.getControlCircuit(BaseTier.ULTIMATE), 'E', MekanismUtils.getEnergyCube(EnergyCubeTier.ULTIMATE), 'P', MekanismUtils.getInductionProvider(InductionProviderTier.ELITE)
		));
		
		//Circuit recipes
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.ControlCircuit, 1, 1),
			"ECE", 'C', new ItemStack(MekanismItems.ControlCircuit, 1, 0), 'E', "alloyAdvanced"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.ControlCircuit, 1, 2),
			"RCR", 'C', new ItemStack(MekanismItems.ControlCircuit, 1, 1), 'R', "alloyElite"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.ControlCircuit, 1, 3),
			"ACA", 'C', new ItemStack(MekanismItems.ControlCircuit, 1, 2), 'A', "alloyUltimate"
		));

		//Factory recipes
		for(RecipeType type : RecipeType.values())
		{
			MachineType.BASIC_FACTORY.addRecipe(new ShapedMekanismRecipe(MekanismUtils.getFactory(FactoryTier.BASIC, type),
				"RCR", "iOi", "RCR", 'R', "alloyBasic", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC), 'i', "ingotIron", 'O', type.getStack()
			));
			MachineType.ADVANCED_FACTORY.addRecipe(new ShapedMekanismRecipe(MekanismUtils.getFactory(FactoryTier.ADVANCED, type),
				"ECE", "oOo", "ECE", 'E', "alloyAdvanced", 'C', MekanismUtils.getControlCircuit(BaseTier.ADVANCED), 'o', "ingotOsmium", 'O', MekanismUtils.getFactory(FactoryTier.BASIC, type)
			));
			MachineType.ELITE_FACTORY.addRecipe(new ShapedMekanismRecipe(MekanismUtils.getFactory(FactoryTier.ELITE, type),
				"RCR", "gOg", "RCR", 'R', "alloyElite", 'C', MekanismUtils.getControlCircuit(BaseTier.ELITE), 'g', "ingotGold", 'O', MekanismUtils.getFactory(FactoryTier.ADVANCED, type)
			));
		}
		
		//Add the bin recipe system to the CraftingManager
		CraftingManager.getInstance().getRecipeList().add(new BinRecipe());
		
        //Transmitters
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.UNIVERSAL_CABLE, BaseTier.BASIC, 8),
			"SRS", 'S', "ingotSteel", 'R', "dustRedstone"
		));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.UNIVERSAL_CABLE, BaseTier.ADVANCED, 8),
            "TTT", "TET", "TTT", 'E', "alloyAdvanced", 'T', MekanismUtils.getTransmitter(TransmitterType.UNIVERSAL_CABLE, BaseTier.BASIC, 1)
        ));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.UNIVERSAL_CABLE, BaseTier.ELITE, 8),
            "TTT", "TRT", "TTT", 'R', "alloyElite", 'T', MekanismUtils.getTransmitter(TransmitterType.UNIVERSAL_CABLE, BaseTier.ADVANCED, 1)
        ));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.UNIVERSAL_CABLE, BaseTier.ULTIMATE, 8),
            "TTT", "TAT", "TTT", 'A', "alloyUltimate", 'T', MekanismUtils.getTransmitter(TransmitterType.UNIVERSAL_CABLE, BaseTier.ELITE, 1)
        ));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.MECHANICAL_PIPE, BaseTier.BASIC, 8),
            "SBS", 'S', "ingotSteel", 'B', Items.BUCKET
        ));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.MECHANICAL_PIPE, BaseTier.ADVANCED, 8),
			"TTT", "TET", "TTT", 'E', "alloyAdvanced", 'T', MekanismUtils.getTransmitter(TransmitterType.MECHANICAL_PIPE, BaseTier.BASIC, 1)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.MECHANICAL_PIPE, BaseTier.ELITE, 8),
			"TTT", "TRT", "TTT", 'R', "alloyElite", 'T', MekanismUtils.getTransmitter(TransmitterType.MECHANICAL_PIPE, BaseTier.ADVANCED, 1)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.MECHANICAL_PIPE, BaseTier.ULTIMATE, 8),
			"TTT", "TAT", "TTT", 'A', "alloyUltimate", 'T', MekanismUtils.getTransmitter(TransmitterType.MECHANICAL_PIPE, BaseTier.ELITE, 1)
		));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.PRESSURIZED_TUBE, BaseTier.BASIC, 8),
            "SGS", 'S', "ingotSteel", 'G', "blockGlass"
        ));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.PRESSURIZED_TUBE, BaseTier.ADVANCED, 8),
			"TTT", "TET", "TTT", 'E', "alloyAdvanced", 'T', MekanismUtils.getTransmitter(TransmitterType.PRESSURIZED_TUBE, BaseTier.BASIC, 1)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.PRESSURIZED_TUBE, BaseTier.ELITE, 8),
			"TTT", "TRT", "TTT", 'R', "alloyElite", 'T', MekanismUtils.getTransmitter(TransmitterType.PRESSURIZED_TUBE, BaseTier.ADVANCED, 1)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.PRESSURIZED_TUBE, BaseTier.ULTIMATE, 8),
			"TTT", "TAT", "TTT", 'A', "alloyUltimate", 'T', MekanismUtils.getTransmitter(TransmitterType.PRESSURIZED_TUBE, BaseTier.ELITE, 1)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.LOGISTICAL_TRANSPORTER, BaseTier.BASIC, 8),
			"SCS", 'S', "ingotSteel", 'C', MekanismUtils.getControlCircuit(BaseTier.BASIC)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.LOGISTICAL_TRANSPORTER, BaseTier.ADVANCED, 8),
			"TTT", "TET", "TTT", 'E', "alloyAdvanced", 'T', MekanismUtils.getTransmitter(TransmitterType.LOGISTICAL_TRANSPORTER, BaseTier.BASIC, 1)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.LOGISTICAL_TRANSPORTER, BaseTier.ELITE, 8),
			"TTT", "TRT", "TTT", 'R', "alloyElite", 'T', MekanismUtils.getTransmitter(TransmitterType.LOGISTICAL_TRANSPORTER, BaseTier.ADVANCED, 1)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.LOGISTICAL_TRANSPORTER, BaseTier.ULTIMATE, 8),
			"TTT", "TAT", "TTT", 'A', "alloyUltimate", 'T', MekanismUtils.getTransmitter(TransmitterType.LOGISTICAL_TRANSPORTER, BaseTier.ELITE, 1)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.RESTRICTIVE_TRANSPORTER, BaseTier.BASIC, 2),
			"SBS", 'S', "ingotSteel", 'B', Blocks.IRON_BARS
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.DIVERSION_TRANSPORTER, BaseTier.BASIC, 2),
			"RRR", "SBS", "RRR", 'R', "dustRedstone", 'S', "ingotSteel", 'B', Blocks.IRON_BARS
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.THERMODYNAMIC_CONDUCTOR, BaseTier.BASIC, 8),
			"SCS", 'S', "ingotSteel", 'C', "ingotCopper"
		));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.THERMODYNAMIC_CONDUCTOR, BaseTier.ADVANCED, 8),
            "TTT", "TET", "TTT", 'E', "alloyAdvanced", 'T', MekanismUtils.getTransmitter(TransmitterType.THERMODYNAMIC_CONDUCTOR, BaseTier.BASIC, 1)
        ));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.THERMODYNAMIC_CONDUCTOR, BaseTier.ELITE, 8),
            "TTT", "TRT", "TTT", 'R', "alloyElite", 'T', MekanismUtils.getTransmitter(TransmitterType.THERMODYNAMIC_CONDUCTOR, BaseTier.ADVANCED, 1)
        ));
        CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(MekanismUtils.getTransmitter(TransmitterType.THERMODYNAMIC_CONDUCTOR, BaseTier.ULTIMATE, 8),
            "TTT", "TAT", "TTT", 'A', "alloyUltimate", 'T', MekanismUtils.getTransmitter(TransmitterType.THERMODYNAMIC_CONDUCTOR, BaseTier.ELITE, 1)
        ));

		//Plastic stuff
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Polyethene, 1, 1),
			"PP", "PP", 'P', new ItemStack(MekanismItems.Polyethene, 1, 0)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Polyethene, 1, 2),
			"PPP", "P P", "PPP", 'P', new ItemStack(MekanismItems.Polyethene, 1, 0)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismItems.Polyethene, 1, 3),
			"R", "R", 'R', new ItemStack(MekanismItems.Polyethene, 1, 1)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.PlasticBlock, 4, 15),
			"SSS", "S S", "SSS", 'S', new ItemStack(MekanismItems.Polyethene, 1, 2)
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.GlowPanel, 2, 15),
			"PSP", "S S", "GSG", 'P', "paneGlass", 'S', new ItemStack(MekanismItems.Polyethene, 1, 2), 'G', "dustGlowstone"
		));
		CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.PlasticFence, 3, 15),
			"BSB", "BSB", 'B', new ItemStack(MekanismBlocks.PlasticBlock, 1, 15), 'S', new ItemStack(MekanismItems.Polyethene, 1, 3)
		));

		for(int i = 0; i < EnumColor.DYES.length-1; i++)
		{
			CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.PlasticBlock, 4, i),
				"SSS", "SDS", "SSS", 'S', new ItemStack(MekanismItems.Polyethene, 1, 2), 'D', "dye" + EnumColor.DYES[i].dyeName
			));
			CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.GlowPanel, 2, i),
				"PSP", "SDS", "GSG", 'P', "paneGlass", 'S', new ItemStack(MekanismItems.Polyethene, 1, 2), 'D', "dye" + EnumColor.DYES[i].dyeName, 'G', "dustGlowstone"
			));
		}

		for(int i = 0; i < EnumColor.DYES.length; i++)
        {
			CraftingManager.getInstance().getRecipeList().add(new ShapelessMekanismRecipe(new ItemStack(MekanismItems.Balloon, 2, i),
				Items.LEATHER, Items.STRING, "dye" + EnumColor.DYES[i].dyeName
			));

			for(int j = 0; j < EnumColor.DYES.length; j++)
			{
				CraftingManager.getInstance().getRecipeList().add(new ShapelessMekanismRecipe(new ItemStack(MekanismItems.Balloon, 1, i),
					new ItemStack(MekanismItems.Balloon, 1, j), "dye" + EnumColor.DYES[i].dyeName
				));

				CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.PlasticBlock, 4, i),
					" P ", "PDP", " P ", 'P', new ItemStack(MekanismBlocks.PlasticBlock, 1, j), 'D', "dye" + EnumColor.DYES[i].dyeName
				));
				CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.SlickPlasticBlock, 4, i),
					" P ", "PDP", " P ", 'P', new ItemStack(MekanismBlocks.SlickPlasticBlock, 1, j), 'D', "dye" + EnumColor.DYES[i].dyeName
				));
				CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.GlowPlasticBlock, 4, i),
					" P ", "PDP", " P ", 'P', new ItemStack(MekanismBlocks.GlowPlasticBlock, 1, j), 'D', "dye" + EnumColor.DYES[i].dyeName
				));
				CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.ReinforcedPlasticBlock, 4, i),
					" P ", "PDP", " P ", 'P', new ItemStack(MekanismBlocks.ReinforcedPlasticBlock, 1, j), 'D', "dye" + EnumColor.DYES[i].dyeName
				));
				CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.GlowPanel, 4, i),
					" P ", "PDP", " P ", 'P', new ItemStack(MekanismBlocks.GlowPanel, 1, j), 'D', "dye" + EnumColor.DYES[i].dyeName
				));
			}

			CraftingManager.getInstance().getRecipeList().add(new ShapelessMekanismRecipe(new ItemStack(MekanismBlocks.GlowPlasticBlock, 3, i),
				new ItemStack(MekanismBlocks.PlasticBlock, 1, i), new ItemStack(MekanismBlocks.PlasticBlock, 1, i), new ItemStack(MekanismBlocks.PlasticBlock, 1, i), new ItemStack(Items.GLOWSTONE_DUST)
			));
			CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.ReinforcedPlasticBlock, 4, i),
				" P ", "POP", " P ", 'P', new ItemStack(MekanismBlocks.PlasticBlock, 1, i), 'O', "dustOsmium"
			));
			CraftingManager.getInstance().getRecipeList().add(new ShapedMekanismRecipe(new ItemStack(MekanismBlocks.RoadPlasticBlock, 3, i),
				"SSS", "PPP", "SSS", 'S', Blocks.SAND, 'P', new ItemStack(MekanismBlocks.SlickPlasticBlock, 1, i)
			));
        }
	
		//Furnace Recipes
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismBlocks.OreBlock, 1, 0), new ItemStack(MekanismItems.Ingot, 1, 1), 1.0F);
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismBlocks.OreBlock, 1, 1), new ItemStack(MekanismItems.Ingot, 1, 5), 1.0F);
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismBlocks.OreBlock, 1, 2), new ItemStack(MekanismItems.Ingot, 1, 6), 1.0F);
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismItems.Dust, 1, Resource.OSMIUM.ordinal()), new ItemStack(MekanismItems.Ingot, 1, 1), 0.0F);
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismItems.Dust, 1, Resource.IRON.ordinal()), new ItemStack(Items.IRON_INGOT), 0.0F);
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismItems.Dust, 1, Resource.GOLD.ordinal()), new ItemStack(Items.GOLD_INGOT), 0.0F);
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismItems.OtherDust, 1, 1), new ItemStack(MekanismItems.Ingot, 1, 4), 0.0F);
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismItems.Dust, 1, Resource.COPPER.ordinal()), new ItemStack(MekanismItems.Ingot, 1, 5), 0.0F);
		FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(MekanismItems.Dust, 1, Resource.TIN.ordinal()), new ItemStack(MekanismItems.Ingot, 1, 6), 0.0F);
		
		//Enrichment Chamber Recipes
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.REDSTONE_ORE), new ItemStack(Items.REDSTONE, 12));
        RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.OBSIDIAN), new ItemStack(MekanismItems.OtherDust, 2, 6));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Items.COAL, 1, 0), new ItemStack(MekanismItems.CompressedCarbon));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Items.COAL, 1, 1), new ItemStack(MekanismItems.CompressedCarbon));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Items.REDSTONE), new ItemStack(MekanismItems.CompressedRedstone));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.LAPIS_ORE), new ItemStack(Items.DYE, 12, 4));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.COAL_ORE), new ItemStack(Items.COAL, 2));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.DIAMOND_ORE), new ItemStack(Items.DIAMOND, 2));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.EMERALD_ORE), new ItemStack(Items.EMERALD, 2));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.MOSSY_COBBLESTONE), new ItemStack(Blocks.COBBLESTONE));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.STONE), new ItemStack(Blocks.STONEBRICK, 1, 2));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.SAND), new ItemStack(Blocks.GRAVEL));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.GRAVEL), new ItemStack(Blocks.COBBLESTONE));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Items.GUNPOWDER), new ItemStack(Items.FLINT));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.STONEBRICK, 1, 2), new ItemStack(Blocks.STONEBRICK, 1, 0));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.STONEBRICK, 1, 0), new ItemStack(Blocks.STONEBRICK, 1, 3));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.STONEBRICK, 1, 1), new ItemStack(Blocks.STONEBRICK, 1, 0));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.GLOWSTONE), new ItemStack(Items.GLOWSTONE_DUST, 4));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.CLAY), new ItemStack(Items.CLAY_BALL, 4));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(MekanismBlocks.SaltBlock), new ItemStack(MekanismItems.Salt, 4));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Items.DIAMOND), new ItemStack(MekanismItems.CompressedDiamond));
		RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(MekanismItems.Polyethene, 3, 0), new ItemStack(MekanismItems.Polyethene, 1, 2));
		
		for(int i = 0; i < EnumColor.DYES.length; i++)
		{
			RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(MekanismBlocks.PlasticBlock, 1, i), new ItemStack(MekanismBlocks.SlickPlasticBlock, 1, i));
		}
		
		//Combiner recipes
		RecipeHandler.addCombinerRecipe(new ItemStack(Items.REDSTONE, 16), new ItemStack(Blocks.REDSTONE_ORE));
		RecipeHandler.addCombinerRecipe(new ItemStack(Items.DYE, 16, 4), new ItemStack(Blocks.LAPIS_ORE));
		RecipeHandler.addCombinerRecipe(new ItemStack(Items.FLINT), new ItemStack(Blocks.GRAVEL));
		
		//Osmium Compressor Recipes
		RecipeHandler.addOsmiumCompressorRecipe(new ItemStack(Items.GLOWSTONE_DUST), new ItemStack(MekanismItems.Ingot, 1, 3));
		
		//Crusher Recipes
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.DIAMOND), new ItemStack(MekanismItems.OtherDust, 1, 0));
        RecipeHandler.addCrusherRecipe(new ItemStack(Items.IRON_INGOT), new ItemStack(MekanismItems.Dust, 1, Resource.IRON.ordinal()));
        RecipeHandler.addCrusherRecipe(new ItemStack(Items.GOLD_INGOT), new ItemStack(MekanismItems.Dust, 1, Resource.GOLD.ordinal()));
        RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.GRAVEL), new ItemStack(Blocks.SAND));
        RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.STONE), new ItemStack(Blocks.COBBLESTONE));
        RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.COBBLESTONE), new ItemStack(Blocks.GRAVEL));
        RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.STONEBRICK, 1, 2), new ItemStack(Blocks.STONE));
        RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.STONEBRICK, 1, 0), new ItemStack(Blocks.STONEBRICK, 1, 2));
        RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.STONEBRICK, 1, 3), new ItemStack(Blocks.STONEBRICK, 1, 0));
        RecipeHandler.addCrusherRecipe(new ItemStack(Items.FLINT), new ItemStack(Items.GUNPOWDER));
        RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.SANDSTONE), new ItemStack(Blocks.SAND, 2));
        
        for(int i = 0; i < 16; i++)
        {
        	RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.WOOL, 1, i), new ItemStack(Items.STRING, 4));
        }
        
		//BioFuel Crusher Recipes
		RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.TALLGRASS), new ItemStack(MekanismItems.BioFuel, 4));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.REEDS), new ItemStack(MekanismItems.BioFuel, 2));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.WHEAT_SEEDS), new ItemStack(MekanismItems.BioFuel, 2));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.WHEAT), new ItemStack(MekanismItems.BioFuel, 4));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.PUMPKIN_SEEDS), new ItemStack(MekanismItems.BioFuel, 2));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.MELON_SEEDS), new ItemStack(MekanismItems.BioFuel, 2));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.APPLE), new ItemStack(MekanismItems.BioFuel, 4));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.BREAD), new ItemStack(MekanismItems.BioFuel, 4));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.POTATO), new ItemStack(MekanismItems.BioFuel, 4));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.CARROT), new ItemStack(MekanismItems.BioFuel, 4));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.ROTTEN_FLESH), new ItemStack(MekanismItems.BioFuel, 2));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.MELON), new ItemStack(MekanismItems.BioFuel, 4));
		RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.PUMPKIN), new ItemStack(MekanismItems.BioFuel, 6));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.BAKED_POTATO), new ItemStack(MekanismItems.BioFuel, 4));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.POISONOUS_POTATO), new ItemStack(MekanismItems.BioFuel, 4));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.BEETROOT), new ItemStack(MekanismItems.BioFuel, 4));
		RecipeHandler.addCrusherRecipe(new ItemStack(Items.BEETROOT_SEEDS), new ItemStack(MekanismItems.BioFuel, 2));

		//Purification Chamber Recipes
        RecipeHandler.addPurificationChamberRecipe(new ItemStack(Blocks.GRAVEL), new ItemStack(Items.FLINT));
        
        //Chemical Injection Chamber Recipes
        RecipeHandler.addChemicalInjectionChamberRecipe(new ItemStack(Blocks.DIRT), MekanismFluids.Water, new ItemStack(Blocks.CLAY));
        RecipeHandler.addChemicalInjectionChamberRecipe(new ItemStack(Blocks.HARDENED_CLAY), MekanismFluids.Water, new ItemStack(Blocks.CLAY));
        RecipeHandler.addChemicalInjectionChamberRecipe(new ItemStack(Items.BRICK), MekanismFluids.Water, new ItemStack(Items.CLAY_BALL));
        RecipeHandler.addChemicalInjectionChamberRecipe(new ItemStack(Items.GUNPOWDER), MekanismFluids.HydrogenChloride, new ItemStack(MekanismItems.OtherDust, 1, 3));
		
		//Precision Sawmill Recipes
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.LADDER, 3), new ItemStack(Items.STICK, 7));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.TORCH, 4), new ItemStack(Items.STICK), new ItemStack(Items.COAL), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.CHEST), new ItemStack(Blocks.PLANKS, 8));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.TRAPDOOR), new ItemStack(Blocks.PLANKS, 3));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.BOAT), new ItemStack(Blocks.PLANKS, 5));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.BED), new ItemStack(Blocks.PLANKS, 3), new ItemStack(Blocks.WOOL, 3), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.OAK_DOOR), new ItemStack(Blocks.PLANKS, 2, 0));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.SPRUCE_DOOR), new ItemStack(Blocks.PLANKS, 2, 1));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.BIRCH_DOOR), new ItemStack(Blocks.PLANKS, 2, 2));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.JUNGLE_DOOR), new ItemStack(Blocks.PLANKS, 2, 3));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.ACACIA_DOOR), new ItemStack(Blocks.PLANKS, 2, 4));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.DARK_OAK_DOOR), new ItemStack(Blocks.PLANKS, 2, 5));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.JUKEBOX), new ItemStack(Blocks.PLANKS, 8), new ItemStack(Items.DIAMOND), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.BOOKSHELF), new ItemStack(Blocks.PLANKS, 6), new ItemStack(Items.BOOK, 3), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.WOODEN_PRESSURE_PLATE), new ItemStack(Blocks.PLANKS, 2));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.OAK_FENCE), new ItemStack(Items.STICK, 3));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.SPRUCE_FENCE), new ItemStack(Items.STICK, 3));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.BIRCH_FENCE), new ItemStack(Items.STICK, 3));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.JUNGLE_FENCE), new ItemStack(Items.STICK, 3));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.ACACIA_FENCE), new ItemStack(Items.STICK, 3));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.DARK_OAK_FENCE), new ItemStack(Items.STICK, 3));
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.OAK_FENCE_GATE), new ItemStack(Blocks.PLANKS, 2, 0), new ItemStack(Items.STICK, 4), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.SPRUCE_FENCE_GATE), new ItemStack(Blocks.PLANKS, 2, 1), new ItemStack(Items.STICK, 4), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.BIRCH_FENCE_GATE), new ItemStack(Blocks.PLANKS, 2, 2), new ItemStack(Items.STICK, 4), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.JUNGLE_FENCE_GATE), new ItemStack(Blocks.PLANKS, 2, 3), new ItemStack(Items.STICK, 4), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.ACACIA_FENCE_GATE), new ItemStack(Blocks.PLANKS, 2, 4), new ItemStack(Items.STICK, 4), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.DARK_OAK_FENCE_GATE), new ItemStack(Blocks.PLANKS, 2, 5), new ItemStack(Items.STICK, 4), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.NOTEBLOCK), new ItemStack(Blocks.PLANKS, 8), new ItemStack(Items.REDSTONE), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.REDSTONE_TORCH), new ItemStack(Items.STICK), new ItemStack(Items.REDSTONE), 1);
		RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.CRAFTING_TABLE), new ItemStack(Blocks.PLANKS, 4));
		
        //Metallurgic Infuser Recipes
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("CARBON"), 10, new ItemStack(Items.IRON_INGOT), new ItemStack(MekanismItems.EnrichedIron));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("CARBON"), 10, new ItemStack(MekanismItems.EnrichedIron), new ItemStack(MekanismItems.OtherDust, 1, 1));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("REDSTONE"), 10, new ItemStack(Items.IRON_INGOT), new ItemStack(MekanismItems.EnrichedAlloy));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("FUNGI"), 10, new ItemStack(Blocks.DIRT), new ItemStack(Blocks.MYCELIUM));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("BIO"), 10, new ItemStack(Blocks.COBBLESTONE), new ItemStack(Blocks.MOSSY_COBBLESTONE));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("BIO"), 10, new ItemStack(Blocks.STONEBRICK, 1, 0), new ItemStack(Blocks.STONEBRICK, 1, 1));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("BIO"), 10, new ItemStack(Blocks.SAND), new ItemStack(Blocks.DIRT));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("BIO"), 10, new ItemStack(Blocks.DIRT), new ItemStack(Blocks.DIRT, 1, 2));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("DIAMOND"), 10, new ItemStack(MekanismItems.EnrichedAlloy), new ItemStack(MekanismItems.ReinforcedAlloy));
        RecipeHandler.addMetallurgicInfuserRecipe(InfuseRegistry.get("OBSIDIAN"), 10, new ItemStack(MekanismItems.ReinforcedAlloy), new ItemStack(MekanismItems.AtomicAlloy));
        
        //Chemical Infuser Recipes
        RecipeHandler.addChemicalInfuserRecipe(new GasStack(MekanismFluids.Oxygen, 1), new GasStack(MekanismFluids.SulfurDioxide, 2), new GasStack(MekanismFluids.SulfurTrioxide, 2));
		RecipeHandler.addChemicalInfuserRecipe(new GasStack(MekanismFluids.SulfurTrioxide, 1), new GasStack(MekanismFluids.Water, 1), new GasStack(MekanismFluids.SulfuricAcid, 1));
		RecipeHandler.addChemicalInfuserRecipe(new GasStack(MekanismFluids.Hydrogen, 1), new GasStack(MekanismFluids.Chlorine, 1), new GasStack(MekanismFluids.HydrogenChloride, 1));
		RecipeHandler.addChemicalInfuserRecipe(new GasStack(MekanismFluids.Deuterium, 1), new GasStack(MekanismFluids.Tritium, 1), new GasStack(MekanismFluids.FusionFuel, 2));

		//Electrolytic Separator Recipes
		RecipeHandler.addElectrolyticSeparatorRecipe(FluidRegistry.getFluidStack("water", 2), 2 * general.FROM_H2, new GasStack(MekanismFluids.Hydrogen, 2), new GasStack(MekanismFluids.Oxygen, 1));
		RecipeHandler.addElectrolyticSeparatorRecipe(FluidRegistry.getFluidStack("brine", 10), 2 * general.FROM_H2, new GasStack(MekanismFluids.Sodium, 1), new GasStack(MekanismFluids.Chlorine, 1));
		RecipeHandler.addElectrolyticSeparatorRecipe(FluidRegistry.getFluidStack("heavywater", 2), usage.heavyWaterElectrolysisUsage, new GasStack(MekanismFluids.Deuterium, 2), new GasStack(MekanismFluids.Oxygen, 1));
		
		//Thermal Evaporation Plant Recipes
		RecipeHandler.addThermalEvaporationRecipe(FluidRegistry.getFluidStack("water", 10), FluidRegistry.getFluidStack("brine", 1));
		RecipeHandler.addThermalEvaporationRecipe(FluidRegistry.getFluidStack("brine", 10), FluidRegistry.getFluidStack("liquidlithium", 1));
		
		//Chemical Crystallizer Recipes
		RecipeHandler.addChemicalCrystallizerRecipe(new GasStack(MekanismFluids.Lithium, 100), new ItemStack(MekanismItems.OtherDust, 1, 4));
		RecipeHandler.addChemicalCrystallizerRecipe(new GasStack(MekanismFluids.Brine, 15), new ItemStack(MekanismItems.Salt));
		
		//T4 Processing Recipes
		for(Gas gas : GasRegistry.getRegisteredGasses())
		{
			if(gas instanceof OreGas && !((OreGas)gas).isClean())
			{
				OreGas oreGas = (OreGas)gas;
				
				RecipeHandler.addChemicalWasherRecipe(new GasStack(oreGas, 1), new GasStack(oreGas.getCleanGas(), 1));
				RecipeHandler.addChemicalCrystallizerRecipe(new GasStack(oreGas.getCleanGas(), 200), new ItemStack(MekanismItems.Crystal, 1, Resource.getFromName(oreGas.getName()).ordinal()));
			}
		}

		//Pressurized Reaction Chamber Recipes
		RecipeHandler.addPRCRecipe(
				new ItemStack(MekanismItems.BioFuel, 2), new FluidStack(FluidRegistry.WATER, 10), new GasStack(MekanismFluids.Hydrogen, 100),
				new ItemStack(MekanismItems.Substrate), new GasStack(MekanismFluids.Ethene, 100),
				0,
				100
		);
		RecipeHandler.addPRCRecipe(
				new ItemStack(MekanismItems.Substrate), new FluidStack(MekanismFluids.Ethene.getFluid(), 50), new GasStack(MekanismFluids.Oxygen, 10),
				new ItemStack(MekanismItems.Polyethene), new GasStack(MekanismFluids.Oxygen, 5),
				1000,
				60
		);
		RecipeHandler.addPRCRecipe(
				new ItemStack(MekanismItems.Substrate), new FluidStack(FluidRegistry.WATER, 200), new GasStack(MekanismFluids.Ethene, 100),
				new ItemStack(MekanismItems.Substrate, 8), new GasStack(MekanismFluids.Oxygen, 10),
				200,
				400
		);
		
		//Solar Neutron Activator Recipes
		RecipeHandler.addSolarNeutronRecipe(new GasStack(MekanismFluids.Lithium, 1), new GasStack(MekanismFluids.Tritium, 1));

        //Infuse objects
		InfuseRegistry.registerInfuseObject(new ItemStack(MekanismItems.BioFuel), new InfuseObject(InfuseRegistry.get("BIO"), 5));
		InfuseRegistry.registerInfuseObject(new ItemStack(Items.COAL, 1, 0), new InfuseObject(InfuseRegistry.get("CARBON"), 10));
        InfuseRegistry.registerInfuseObject(new ItemStack(Items.COAL, 1, 1), new InfuseObject(InfuseRegistry.get("CARBON"), 20));
        InfuseRegistry.registerInfuseObject(new ItemStack(MekanismItems.CompressedCarbon), new InfuseObject(InfuseRegistry.get("CARBON"), 80));
        InfuseRegistry.registerInfuseObject(new ItemStack(Items.REDSTONE), new InfuseObject(InfuseRegistry.get("REDSTONE"), 10));
        InfuseRegistry.registerInfuseObject(new ItemStack(Blocks.REDSTONE_BLOCK), new InfuseObject(InfuseRegistry.get("REDSTONE"), 90));
        InfuseRegistry.registerInfuseObject(new ItemStack(MekanismItems.CompressedRedstone), new InfuseObject(InfuseRegistry.get("REDSTONE"), 80));
        InfuseRegistry.registerInfuseObject(new ItemStack(Blocks.RED_MUSHROOM), new InfuseObject(InfuseRegistry.get("FUNGI"), 10));
        InfuseRegistry.registerInfuseObject(new ItemStack(Blocks.BROWN_MUSHROOM), new InfuseObject(InfuseRegistry.get("FUNGI"), 10));
        InfuseRegistry.registerInfuseObject(new ItemStack(MekanismItems.CompressedDiamond), new InfuseObject(InfuseRegistry.get("DIAMOND"), 80));
        InfuseRegistry.registerInfuseObject(new ItemStack(MekanismItems.CompressedObsidian), new InfuseObject(InfuseRegistry.get("OBSIDIAN"), 80));
        
        //Fuels
        GameRegistry.registerFuelHandler(new IFuelHandler() {
			@Override
			public int getBurnTime(ItemStack fuel)
			{
				if(fuel.isItemEqual(new ItemStack(MekanismBlocks.BasicBlock, 1, 3)))
				{
					return 200*8*9;
				}
				
				return 0;
			}
		});

		//Fuel Gases
		FuelHandler.addGas(MekanismFluids.Hydrogen, 1, general.FROM_H2);
		
		//RecipeSorter registrations
		RecipeSorter.register("mekanism_shaped", ShapedMekanismRecipe.class, Category.SHAPED, "");
		RecipeSorter.register("mekanism_shapeless", ShapelessMekanismRecipe.class, Category.SHAPELESS, "");
		RecipeSorter.register("bin", BinRecipe.class, Category.SHAPELESS, "");
	}

	/**
	 * Registers specified items with the Ore Dictionary.
	 */
	public void registerOreDict()
	{
		//Add specific items to ore dictionary for recipe usage in other mods.
		OreDictionary.registerOre("universalCable", MekanismUtils.getTransmitter(TransmitterType.UNIVERSAL_CABLE, BaseTier.BASIC, 1));
		OreDictionary.registerOre("battery", MekanismItems.EnergyTablet.getUnchargedItem());
		OreDictionary.registerOre("pulpWood", MekanismItems.Sawdust);
		OreDictionary.registerOre("dustWood", MekanismItems.Sawdust);
		OreDictionary.registerOre("blockSalt", MekanismBlocks.SaltBlock);
		
		//Alloys!
		OreDictionary.registerOre("alloyBasic", new ItemStack(Items.REDSTONE));
		OreDictionary.registerOre("alloyAdvanced", new ItemStack(MekanismItems.EnrichedAlloy));
		OreDictionary.registerOre("alloyElite", new ItemStack(MekanismItems.ReinforcedAlloy));
		OreDictionary.registerOre("alloyUltimate", new ItemStack(MekanismItems.AtomicAlloy));
		
		//GregoriousT?
		OreDictionary.registerOre("itemSalt", MekanismItems.Salt);
		OreDictionary.registerOre("dustSalt", MekanismItems.Salt);
		
		OreDictionary.registerOre("dustDiamond", new ItemStack(MekanismItems.OtherDust, 1, 0));
		OreDictionary.registerOre("dustSteel", new ItemStack(MekanismItems.OtherDust, 1, 1));
		//Lead was once here
		OreDictionary.registerOre("dustSulfur", new ItemStack(MekanismItems.OtherDust, 1, 3));
		OreDictionary.registerOre("dustLithium", new ItemStack(MekanismItems.OtherDust, 1, 4));
		OreDictionary.registerOre("dustRefinedObsidian", new ItemStack(MekanismItems.OtherDust, 1, 5));
		OreDictionary.registerOre("dustObsidian", new ItemStack(MekanismItems.OtherDust, 1, 6));
		
		OreDictionary.registerOre("ingotRefinedObsidian", new ItemStack(MekanismItems.Ingot, 1, 0));
		OreDictionary.registerOre("ingotOsmium", new ItemStack(MekanismItems.Ingot, 1, 1));
		OreDictionary.registerOre("ingotBronze", new ItemStack(MekanismItems.Ingot, 1, 2));
		OreDictionary.registerOre("ingotRefinedGlowstone", new ItemStack(MekanismItems.Ingot, 1, 3));
		OreDictionary.registerOre("ingotSteel", new ItemStack(MekanismItems.Ingot, 1, 4));
		OreDictionary.registerOre("ingotCopper", new ItemStack(MekanismItems.Ingot, 1, 5));
		OreDictionary.registerOre("ingotTin", new ItemStack(MekanismItems.Ingot, 1, 6));
		
		OreDictionary.registerOre("blockOsmium", new ItemStack(MekanismBlocks.BasicBlock, 1, 0));
		OreDictionary.registerOre("blockBronze", new ItemStack(MekanismBlocks.BasicBlock, 1, 1));
		OreDictionary.registerOre("blockRefinedObsidian", new ItemStack(MekanismBlocks.BasicBlock, 1, 2));
		OreDictionary.registerOre("blockCharcoal", new ItemStack(MekanismBlocks.BasicBlock, 1, 3));
		OreDictionary.registerOre("blockRefinedGlowstone", new ItemStack(MekanismBlocks.BasicBlock, 1, 4));
		OreDictionary.registerOre("blockSteel", new ItemStack(MekanismBlocks.BasicBlock, 1, 5));
		OreDictionary.registerOre("blockCopper", new ItemStack(MekanismBlocks.BasicBlock, 1, 12));
		OreDictionary.registerOre("blockTin", new ItemStack(MekanismBlocks.BasicBlock, 1, 13));
		
		for(Resource resource : Resource.values())
		{
			OreDictionary.registerOre("dust" + resource.getName(), new ItemStack(MekanismItems.Dust, 1, resource.ordinal()));
			OreDictionary.registerOre("dustDirty" + resource.getName(), new ItemStack(MekanismItems.DirtyDust, 1, resource.ordinal()));
			OreDictionary.registerOre("clump" + resource.getName(), new ItemStack(MekanismItems.Clump, 1, resource.ordinal()));
			OreDictionary.registerOre("shard" + resource.getName(), new ItemStack(MekanismItems.Shard, 1, resource.ordinal()));
			OreDictionary.registerOre("crystal" + resource.getName(), new ItemStack(MekanismItems.Crystal, 1, resource.ordinal()));
		}
		
		OreDictionary.registerOre("oreOsmium", new ItemStack(MekanismBlocks.OreBlock, 1, 0));
		OreDictionary.registerOre("oreCopper", new ItemStack(MekanismBlocks.OreBlock, 1, 1));
		OreDictionary.registerOre("oreTin", new ItemStack(MekanismBlocks.OreBlock, 1, 2));
		
		if(general.controlCircuitOreDict)
		{
			OreDictionary.registerOre("circuitBasic", new ItemStack(MekanismItems.ControlCircuit, 1, 0));
			OreDictionary.registerOre("circuitAdvanced", new ItemStack(MekanismItems.ControlCircuit, 1, 1));
			OreDictionary.registerOre("circuitElite", new ItemStack(MekanismItems.ControlCircuit, 1, 2));
			OreDictionary.registerOre("circuitUltimate", new ItemStack(MekanismItems.ControlCircuit, 1, 3));
		}

		OreDictionary.registerOre("itemCompressedCarbon", new ItemStack(MekanismItems.CompressedCarbon));
		OreDictionary.registerOre("itemCompressedRedstone", new ItemStack(MekanismItems.CompressedRedstone));
		OreDictionary.registerOre("itemCompressedDiamond", new ItemStack(MekanismItems.CompressedDiamond));
		OreDictionary.registerOre("itemCompressedObsidian", new ItemStack(MekanismItems.CompressedObsidian));

		OreDictionary.registerOre("itemEnrichedAlloy", new ItemStack(MekanismItems.EnrichedAlloy));
		OreDictionary.registerOre("itemBioFuel", new ItemStack(MekanismItems.BioFuel));
	}
	
	/**
	 * Adds and registers all entities and tile entities.
	 */
	public void addEntities()
	{
		//Registrations
		EntityRegistry.registerModEntity(new ResourceLocation("mekanism", "ObsidianTNT"), EntityObsidianTNT.class, "ObsidianTNT", 0, this, 64, 5, true);
		EntityRegistry.registerModEntity(new ResourceLocation("mekanism", "Robit"), EntityRobit.class, "Robit", 1, this, 64, 2, true);
		EntityRegistry.registerModEntity(new ResourceLocation("mekanism", "Balloon"), EntityBalloon.class, "Balloon", 2, this, 64, 1, true);
		EntityRegistry.registerModEntity(new ResourceLocation("mekanism", "BabySkeleton"), EntityBabySkeleton.class, "BabySkeleton", 3, this, 64, 5, true);
		EntityRegistry.registerModEntity(new ResourceLocation("mekanism", "Flame"), EntityFlame.class, "Flame", 4, this, 64, 5, true);
		
		//Tile entities
		GameRegistry.registerTileEntity(TileEntityBoundingBlock.class, "BoundingBlock");
		GameRegistry.registerTileEntity(TileEntityAdvancedBoundingBlock.class, "AdvancedBoundingBlock");
		GameRegistry.registerTileEntity(TileEntityCardboardBox.class, "CardboardBox");
		GameRegistry.registerTileEntity(TileEntityThermalEvaporationValve.class, "ThermalEvaporationValve");
		GameRegistry.registerTileEntity(TileEntityThermalEvaporationBlock.class, "ThermalEvaporationBlock");
		GameRegistry.registerTileEntity(TileEntityPressureDisperser.class, "PressureDisperser");
		GameRegistry.registerTileEntity(TileEntitySuperheatingElement.class, "SuperheatingElement");
		GameRegistry.registerTileEntity(TileEntityLaser.class, "Laser");
		GameRegistry.registerTileEntity(TileEntityAmbientAccumulator.class, "AmbientAccumulator");
		GameRegistry.registerTileEntity(TileEntityInductionCasing.class, "InductionCasing");
		GameRegistry.registerTileEntity(TileEntityInductionPort.class, "InductionPort");
		GameRegistry.registerTileEntity(TileEntityInductionCell.class, "InductionCell");
		GameRegistry.registerTileEntity(TileEntityInductionProvider.class, "InductionProvider");
		GameRegistry.registerTileEntity(TileEntityOredictionificator.class, "Oredictionificator");
		GameRegistry.registerTileEntity(TileEntityStructuralGlass.class, "StructuralGlass");
		GameRegistry.registerTileEntity(TileEntityFuelwoodHeater.class, "FuelwoodHeater");
		GameRegistry.registerTileEntity(TileEntityLaserAmplifier.class, "LaserAmplifier");
		GameRegistry.registerTileEntity(TileEntityLaserTractorBeam.class, "LaserTractorBeam");
		GameRegistry.registerTileEntity(TileEntityChemicalWasher.class, "ChemicalWasher");
		GameRegistry.registerTileEntity(TileEntityElectrolyticSeparator.class, "ElectrolyticSeparator");
		GameRegistry.registerTileEntity(TileEntityChemicalOxidizer.class, "ChemicalOxidizer");
		GameRegistry.registerTileEntity(TileEntityChemicalInfuser.class, "ChemicalInfuser");
		GameRegistry.registerTileEntity(TileEntityRotaryCondensentrator.class, "RotaryCondensentrator");
		GameRegistry.registerTileEntity(TileEntityElectricPump.class, "ElectricPump");
		GameRegistry.registerTileEntity(TileEntityFluidicPlenisher.class, "FluidicPlenisher");
		GameRegistry.registerTileEntity(TileEntityGlowPanel.class, "GlowPanel");

		//Load tile entities that have special renderers.
		proxy.registerSpecialTileEntities();
	}
	
	@EventHandler
	public void serverStarting(FMLServerStartingEvent event)
	{
		if(general.voiceServerEnabled)
		{
			voiceManager.start();
		}
		
		//Load cached furnace recipes
		Recipe.ENERGIZED_SMELTER.get().clear();
		
		for(Object obj : FurnaceRecipes.instance().getSmeltingList().entrySet())
		{
			Map.Entry<ItemStack, ItemStack> entry = (Map.Entry<ItemStack, ItemStack>)obj;
			SmeltingRecipe recipe = new SmeltingRecipe(new ItemStackInput(entry.getKey()), new ItemStackOutput(entry.getValue()));
			Recipe.ENERGIZED_SMELTER.put(recipe);
		}
		
		event.registerServerCommand(new CommandMekanism());
	}
	
	@EventHandler
	public void serverStopping(FMLServerStoppingEvent event)
	{
		if(general.voiceServerEnabled)
		{
			voiceManager.stop();
		}
		
		//Clear all cache data
		jetpackOn.clear();
		gasmaskOn.clear();
		activeVibrators.clear();
		worldTickHandler.resetRegenChunks();
		privateTeleporters.clear();
		privateEntangloporters.clear();
		
		//Reset consistent managers
		MultiblockManager.reset();
		FrequencyManager.reset();
		TransporterManager.reset();
		PathfinderCache.reset();
		TransmitterNetworkRegistry.reset();
	}
	
	@EventHandler
	public void loadComplete(FMLLoadCompleteEvent event)
	{
		new IMCHandler().onIMCEvent(FMLInterModComms.fetchRuntimeMessages(this));
	}
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		File config = event.getSuggestedConfigurationFile();
		
		//Set the mod's configuration
		configuration = new Configuration(config);

        //Register tier information
        Tier.init();

		if(config.getAbsolutePath().contains("voltz"))
		{
			logger.info("Detected Voltz in root directory - hello, fellow user!");
		}
		else if(config.getAbsolutePath().contains("tekkit"))
		{
			logger.info("Detected Tekkit in root directory - hello, fellow user!");
		}
		
		//Register blocks and items
		MekanismItems.register();
		MekanismBlocks.register();

		if(Loader.isModLoaded("mcmultipart")) 
		{
			//Set up multiparts
			new MultipartMekanism();
		} 
		else {
			logger.info("Didn't detect MCMP, ignoring compatibility package");
		}

		Mekanism.proxy.preInit();

		//Register infuses
        InfuseRegistry.registerInfuseType(new InfuseType("CARBON", new ResourceLocation("mekanism:blocks/infuse/Carbon")).setUnlocalizedName("carbon"));
        InfuseRegistry.registerInfuseType(new InfuseType("TIN", new ResourceLocation("mekanism:blocks/infuse/Tin")).setUnlocalizedName("tin"));
        InfuseRegistry.registerInfuseType(new InfuseType("DIAMOND", new ResourceLocation("mekanism:blocks/infuse/Diamond")).setUnlocalizedName("diamond"));
        InfuseRegistry.registerInfuseType(new InfuseType("REDSTONE", new ResourceLocation("mekanism:blocks/infuse/Redstone")).setUnlocalizedName("redstone"));
        InfuseRegistry.registerInfuseType(new InfuseType("FUNGI", new ResourceLocation("mekanism:blocks/infuse/Fungi")).setUnlocalizedName("fungi"));
		InfuseRegistry.registerInfuseType(new InfuseType("BIO", new ResourceLocation("mekanism:blocks/infuse/Bio")).setUnlocalizedName("bio"));
		InfuseRegistry.registerInfuseType(new InfuseType("OBSIDIAN", new ResourceLocation("mekanism:blocks/infuse/Obsidian")).setUnlocalizedName("obsidian"));

		Capabilities.registerCapabilities();
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event) 
	{
		//Register the mod's world generators
		GameRegistry.registerWorldGenerator(genHandler, 1);
		
		//Register the mod's GUI handler
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new CoreGuiHandler());
		
		//Register player tracker
		MinecraftForge.EVENT_BUS.register(new CommonPlayerTracker());
		MinecraftForge.EVENT_BUS.register(new CommonPlayerTickHandler());
		
		//Initialization notification
		logger.info("Version " + versionNumber + " initializing...");
		
		//Get data from server
		new ThreadGetData();
		
		//Register with ForgeChunkManager
		ForgeChunkManager.setForcedChunkLoadingCallback(this, new ChunkManager());
		
		//Register to receive subscribed events
		MinecraftForge.EVENT_BUS.register(this);
		
		//Register this module's GUI handler in the simple packet protocol
		PacketSimpleGui.handlers.add(0, proxy);

		//Set up VoiceServerManager
		if(general.voiceServerEnabled)
		{
			voiceManager = new VoiceServerManager();
		}
		
		//Register with TransmitterNetworkRegistry
		TransmitterNetworkRegistry.initiate();
		
		//Load configuration
		proxy.loadConfiguration();
		proxy.onConfigSync(false);
		
		//Add baby skeleton spawner
		if(general.spawnBabySkeletons)
		{
			for(Biome biome : BiomeProvider.allowedBiomes) 
			{
				if(biome.getSpawnableList(EnumCreatureType.MONSTER) != null && biome.getSpawnableList(EnumCreatureType.MONSTER).size() > 0)
				{
					EntityRegistry.addSpawn(EntityBabySkeleton.class, 40, 1, 3, EnumCreatureType.MONSTER, biome);
				}
			}
		}
		
		//Integrate certain OreDictionary recipes
		registerOreDict();

		//Load this module
		addRecipes();
		addEntities();
		
		//Integrate with Waila
		FMLInterModComms.sendMessage("Waila", "register", "mekanism.common.integration.WailaDataProvider.register");

		//Integrate with OpenComputers
		if(Loader.isModLoaded("OpenComputers"))
		{
			hooks.loadOCDrivers();
		}

		if(Loader.isModLoaded("appliedenergistics2"))
		{
			hooks.registerAE2P2P();
		}

		//Packet registrations
		packetHandler.initialize();

		//Load proxy
		proxy.init();
		
		//Completion notification
		logger.info("Loading complete.");
		
		//Success message
		logger.info("Mod loaded.");
	}	
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		logger.info("Fake player readout: UUID = " + gameProfile.getId().toString() + ", name = " + gameProfile.getName());

		hooks.hook();
		
		MinecraftForge.EVENT_BUS.post(new BoxBlacklistEvent());
		
		OreDictManager.init();
		
		//Update the config-dependent recipes after the recipes have actually been added in the first place
		TypeConfigManager.updateConfigRecipes(MachineType.getValidMachines(), general.machinesManager);
		
		logger.info("Hooking complete.");
	}
	
	@SubscribeEvent
	public void onEnergyTransferred(EnergyTransferEvent event)
	{
		try {
			packetHandler.sendToReceivers(new TransmitterUpdateMessage(PacketType.ENERGY, event.energyNetwork.transmitters.iterator().next().coord(), event.power), event.energyNetwork.getPacketRange());
		} catch(Exception e) {}
	}
	
	@SubscribeEvent
	public void onGasTransferred(GasTransferEvent event)
	{
		try {
			packetHandler.sendToReceivers(new TransmitterUpdateMessage(PacketType.GAS, event.gasNetwork.transmitters.iterator().next().coord(), event.transferType, event.didTransfer), event.gasNetwork.getPacketRange());
		} catch(Exception e) {}
	}
	
	@SubscribeEvent
	public void onLiquidTransferred(FluidTransferEvent event)
	{
		try {
			packetHandler.sendToReceivers(new TransmitterUpdateMessage(PacketType.FLUID, event.fluidNetwork.transmitters.iterator().next().coord(), event.fluidType, event.didTransfer), event.fluidNetwork.getPacketRange());
		} catch(Exception e) {}
	}

	@SubscribeEvent
	public void onTransmittersAddedEvent(TransmittersAddedEvent event)
	{
		try {
			packetHandler.sendToReceivers(new TransmitterUpdateMessage(PacketType.UPDATE, event.network.transmitters.iterator().next().coord(), event.newNetwork, event.newTransmitters), event.network.getPacketRange());
		} catch(Exception e) {}
	}
	
	@SubscribeEvent
	public void onNetworkClientRequest(NetworkClientRequest event)
	{
		try {
			packetHandler.sendToServer(new DataRequestMessage(Coord4D.get(event.tileEntity)));
		} catch(Exception e) {}
	}
	
	@SubscribeEvent
	public void onClientTickUpdate(ClientTickUpdate event)
	{
		try {
			if(event.operation == 0)
			{
				ClientTickHandler.tickingSet.remove(event.network);
			}
			else {
				ClientTickHandler.tickingSet.add(event.network);
			}
		} catch(Exception e) {}
	}
	
	@SubscribeEvent
	public void onBlacklistUpdate(BoxBlacklistEvent event)
	{
		MekanismAPI.addBoxBlacklist(MekanismBlocks.CardboardBox, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(MekanismBlocks.BoundingBlock, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.BEDROCK, 0);
		MekanismAPI.addBoxBlacklist(Blocks.PORTAL, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.END_PORTAL, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.END_PORTAL_FRAME, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.BED, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.OAK_DOOR, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.SPRUCE_DOOR, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.BIRCH_DOOR, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.JUNGLE_DOOR, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.ACACIA_DOOR, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.DARK_OAK_DOOR, OreDictionary.WILDCARD_VALUE);
		MekanismAPI.addBoxBlacklist(Blocks.IRON_DOOR, OreDictionary.WILDCARD_VALUE);
		
		BoxBlacklistParser.load();
	}
	
	@SubscribeEvent
	public synchronized void onChunkLoad(ChunkEvent.Load event)
	{
		if(event.getChunk() != null && !event.getWorld().isRemote)
		{
			Map copy = (Map)((HashMap)event.getChunk().getTileEntityMap()).clone();

			for (Object obj : copy.values()) {
				if (obj instanceof TileEntity) {
					TileEntity tileEntity = (TileEntity) obj;

					if (tileEntity instanceof TileEntityElectricBlock && MekanismUtils.useIC2()) {
						((TileEntityElectricBlock) tileEntity).register();
					} else if (tileEntity instanceof IChunkLoadHandler) {
						((IChunkLoadHandler) tileEntity).onChunkLoad();
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public void chunkSave(ChunkDataEvent.Save event) 
	{
		if(!event.getWorld().isRemote)
		{
			NBTTagCompound nbtTags = event.getData();

			nbtTags.setInteger("MekanismWorldGen", baseWorldGenVersion);
			nbtTags.setInteger("MekanismUserWorldGen", general.userWorldGenVersion);
		}
	}
	
	@SubscribeEvent
	public synchronized void onChunkDataLoad(ChunkDataEvent.Load event)
	{
		if(!event.getWorld().isRemote)
		{
			if(general.enableWorldRegeneration)
			{
				NBTTagCompound loadData = event.getData();
				
				if(loadData.getInteger("MekanismWorldGen") == baseWorldGenVersion && loadData.getInteger("MekanismUserWorldGen") == general.userWorldGenVersion)
				{
					return;
				}
	
				ChunkPos coordPair = event.getChunk().getPos();
				worldTickHandler.addRegenChunk(event.getWorld().provider.getDimension(), coordPair);
			}
		}
	}

	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
	{
		if(event.getModID().equals("mekanism"))
		{
			proxy.loadConfiguration();
			proxy.onConfigSync(false);
		}
	}
}
