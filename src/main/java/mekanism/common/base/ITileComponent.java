package mekanism.common.base;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;

import net.minecraft.nbt.NBTTagCompound;

public interface ITileComponent
{
	void tick();

	void read(NBTTagCompound nbtTags);

	void read(ByteBuf dataStream);

	void write(NBTTagCompound nbtTags);

	void write(ArrayList<Object> data);
	
	void invalidate();
}
