package mekanism.client;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import mekanism.api.Coord4D;
import mekanism.common.MekanismSounds;
import mekanism.common.config.MekanismConfig.general;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SparkleAnimation
{
	public TileEntity pointer;

	public Random random = new Random();

	public Set<Coord4D> iteratedNodes = new HashSet<>();
	
	public INodeChecker nodeChecker;

	public SparkleAnimation(TileEntity tileEntity, INodeChecker checker)
	{
		pointer = tileEntity;
		nodeChecker = checker;
	}

	public void run()
	{
		try {
			if(general.dynamicTankEasterEgg)
			{
				pointer.getWorld().playSound(null, pointer.getPos().getX(), pointer.getPos().getY(), pointer.getPos().getZ(), MekanismSounds.CJ_EASTER_EGG, SoundCategory.BLOCKS, 1F, 1F);
			}

			loop(pointer);
		} catch(Exception e) {}
		
		try {
			new Thread() {
				@Override
				public void run()
				{
					World world = pointer.getWorld();
					
					for(Coord4D coord : iteratedNodes)
					{
						for(int i = 0; i < 6; i++)
						{
							world.spawnParticle(EnumParticleTypes.REDSTONE, coord.xCoord + random.nextDouble(), coord.yCoord + -.01, coord.zCoord + random.nextDouble(), 0, 0, 0);
							world.spawnParticle(EnumParticleTypes.REDSTONE, coord.xCoord + random.nextDouble(), coord.yCoord + 1.01, coord.zCoord + random.nextDouble(), 0, 0, 0);
							world.spawnParticle(EnumParticleTypes.REDSTONE, coord.xCoord + random.nextDouble(), coord.yCoord + random.nextDouble(), coord.zCoord + -.01, 0, 0, 0);
							world.spawnParticle(EnumParticleTypes.REDSTONE, coord.xCoord + random.nextDouble(), coord.yCoord + random.nextDouble(), coord.zCoord + 1.01, 0, 0, 0);
							world.spawnParticle(EnumParticleTypes.REDSTONE, coord.xCoord + -.01, coord.yCoord + random.nextDouble(), coord.zCoord + random.nextDouble(), 0, 0, 0);
							world.spawnParticle(EnumParticleTypes.REDSTONE, coord.xCoord + 1.01, coord.yCoord + random.nextDouble(), coord.zCoord + random.nextDouble(), 0, 0, 0);
						}
					}
				}
			}.start();
		} catch(Exception e) {}
	}

	public void loop(TileEntity tileEntity)
	{
		iteratedNodes.add(Coord4D.get(tileEntity));

		for(EnumFacing side : EnumFacing.VALUES)
		{
			Coord4D coord = Coord4D.get(tileEntity).offset(side);
			
			if(coord.exists(pointer.getWorld()))
			{
				TileEntity tile = coord.getTileEntity(pointer.getWorld());
	
				if(tile != null && isNode(tile) && !iteratedNodes.contains(coord))
				{
					loop(tile);
				}
			}
		}
	}
	
	public boolean isNode(TileEntity tile)
	{
		return nodeChecker.isNode(tile);
	}
	
	public interface INodeChecker
	{
		boolean isNode(TileEntity tile);
	}
}
