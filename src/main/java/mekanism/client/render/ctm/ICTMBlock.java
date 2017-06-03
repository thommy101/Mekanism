package mekanism.client.render.ctm;

import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;

public interface ICTMBlock<T extends Enum & IStringSerializable>
{
	CTMData getCTMData(IBlockState state);
	
	PropertyEnum<? extends T> getTypeProperty();
	
	String getOverrideTexture(IBlockState state, EnumFacing side);
}
