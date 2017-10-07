package mekanism.client.gui;

import java.io.IOException;
import java.util.ArrayList;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.client.gui.element.GuiEnergyInfo;
import mekanism.client.gui.element.GuiPowerBar;
import mekanism.client.gui.element.GuiRedstoneControl;
import mekanism.client.gui.element.GuiSecurityTab;
import mekanism.client.gui.element.GuiSlot;
import mekanism.client.gui.element.GuiSlot.SlotOverlay;
import mekanism.client.gui.element.GuiSlot.SlotType;
import mekanism.client.gui.element.GuiUpgradeTab;
import mekanism.client.gui.element.GuiVisualsTab;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.sound.SoundHandler;
import mekanism.common.Mekanism;
import mekanism.common.content.miner.ThreadMinerSearch.State;
import mekanism.common.inventory.container.ContainerDigitalMiner;
import mekanism.common.network.PacketDigitalMinerGui.DigitalMinerGuiMessage;
import mekanism.common.network.PacketDigitalMinerGui.MinerGuiPacket;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.tile.TileEntityDigitalMiner;
import mekanism.common.util.LangUtils;
import mekanism.common.util.ListUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

@SideOnly(Side.CLIENT)
public class GuiDigitalMiner extends GuiMekanism
{
	public TileEntityDigitalMiner tileEntity;

	public GuiButton startButton;
	public GuiButton stopButton;
	public GuiButton configButton;

	public GuiDigitalMiner(InventoryPlayer inventory, TileEntityDigitalMiner tentity)
	{
		super(tentity, new ContainerDigitalMiner(inventory, tentity));
		tileEntity = tentity;

		guiElements.add(new GuiRedstoneControl(this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiDigitalMiner.png")));
		guiElements.add(new GuiSecurityTab(this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiDigitalMiner.png")));
		guiElements.add(new GuiUpgradeTab(this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiDigitalMiner.png")));
		guiElements.add(new GuiPowerBar(this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiDigitalMiner.png"), 163, 23));
		guiElements.add(new GuiVisualsTab(this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiDigitalMiner.png")));
		guiElements.add(new GuiEnergyInfo(() ->
        {
            String multiplier = MekanismUtils.getEnergyDisplay(tileEntity.getPerTick());
            return ListUtils.asList(LangUtils.localize("gui.mekanism.using") + ": " + multiplier + "/t", LangUtils.localize("gui.mekanism.needed") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getMaxEnergy()-tileEntity.getEnergy()));
        }, this, MekanismUtils.getResource(ResourceType.GUI, "GuiDigitalMiner.png")));

		guiElements.add(new GuiSlot(SlotType.NORMAL, this, MekanismUtils.getResource(ResourceType.GUI, "GuiDigitalMiner.png"), 151, 5).with(SlotOverlay.POWER));
		guiElements.add(new GuiSlot(SlotType.NORMAL, this, MekanismUtils.getResource(ResourceType.GUI, "GuiDigitalMiner.png"), 143, 26));

		ySize+=64;
	}

	@Override
	public void initGui()
	{
		super.initGui();

		int guiWidth = (width - xSize) / 2;
		int guiHeight = (height - ySize) / 2;

		buttonList.clear();
		startButton = new GuiButton(0, guiWidth + 69, guiHeight + 17, 60, 20, LangUtils.localize("gui.mekanism.start"));

		if(tileEntity.searcher.state != State.IDLE && tileEntity.running)
		{
			startButton.enabled = false;
		}

		stopButton = new GuiButton(1, guiWidth + 69, guiHeight + 37, 60, 20, LangUtils.localize("gui.mekanism.stop"));

		if(tileEntity.searcher.state == State.IDLE || !tileEntity.running)
		{
			stopButton.enabled = false;
		}

		configButton = new GuiButton(2, guiWidth + 69, guiHeight + 57, 60, 20, LangUtils.localize("gui.mekanism.config"));

		if(tileEntity.searcher.state != State.IDLE)
		{
			configButton.enabled = false;
		}

		buttonList.add(startButton);
		buttonList.add(stopButton);
		buttonList.add(configButton);
	}

	@Override
	protected void actionPerformed(GuiButton guibutton) throws IOException
	{
		super.actionPerformed(guibutton);

		if(guibutton.id == 0)
		{
			ArrayList<Object> data = new ArrayList<>();
			data.add(3);

			Mekanism.packetHandler.sendToServer(new TileEntityMessage(Coord4D.get(tileEntity), data));
		}
		else if(guibutton.id == 1)
		{
			ArrayList<Object> data = new ArrayList<>();
			data.add(4);

			Mekanism.packetHandler.sendToServer(new TileEntityMessage(Coord4D.get(tileEntity), data));
		}
		else if(guibutton.id == 2)
		{
			Mekanism.packetHandler.sendToServer(new DigitalMinerGuiMessage(MinerGuiPacket.SERVER, Coord4D.get(tileEntity), 0, 0, 0));
		}
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();

		if(tileEntity.searcher.state != State.IDLE && tileEntity.running)
		{
			startButton.enabled = false;
		}
		else {
			startButton.enabled = true;
		}

		if(tileEntity.searcher.state == State.IDLE || !tileEntity.running)
		{
			stopButton.enabled = false;
		}
		else {
			stopButton.enabled = true;
		}

		if(tileEntity.searcher.state != State.IDLE)
		{
			configButton.enabled = false;
		}
		else {
			configButton.enabled = true;
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
	{
		int xAxis = (mouseX - (width - xSize) / 2);
		int yAxis = (mouseY - (height - ySize) / 2);

		fontRenderer.drawString(tileEntity.getName(), 69, 6, 0x404040);
		fontRenderer.drawString(LangUtils.localize("container.inventory"), 8, (ySize - 96) + 2, 0x404040);

		fontRenderer.drawString(tileEntity.running ? LangUtils.localize("gui.mekanism.digitalMiner.running") : LangUtils.localize("gui.mekanism.idle"), 9, 10, 0x00CD00);
		fontRenderer.drawString(tileEntity.searcher.state.desc, 9, 19, 0x00CD00);

		fontRenderer.drawString(LangUtils.localize("gui.mekanism.eject") + ": " + LangUtils.localize("gui.mekanism." + (tileEntity.doEject ? "on" : "off")), 9, 30, 0x00CD00);
		fontRenderer.drawString(LangUtils.localize("gui.mekanism.digitalMiner.pull") + ": " + LangUtils.localize("gui.mekanism." + (tileEntity.doPull ? "on" : "off")), 9, 39, 0x00CD00);
		fontRenderer.drawString(LangUtils.localize("gui.mekanism.digitalMiner.silk") + ": " + LangUtils.localize("gui.mekanism." + (tileEntity.silkTouch ? "on" : "off")), 9, 48, 0x00CD00);

		fontRenderer.drawString(LangUtils.localize("gui.mekanism.digitalMiner.toMine") + ":", 9, 59, 0x00CD00);
		fontRenderer.drawString("" + tileEntity.clientToMine, 9, 68, 0x00CD00);

		if(!tileEntity.missingStack.isEmpty())
		{
			GlStateManager.pushMatrix();
			GL11.glColor4f(1, 1, 1, 1);
			RenderHelper.enableGUIStandardItemLighting();
			GL11.glEnable(GL12.GL_RESCALE_NORMAL);

			mc.getTextureManager().bindTexture(MekanismRenderer.getBlocksTexture());
			
			drawTexturedRectFromIcon(144, 27, MekanismRenderer.getColorIcon(EnumColor.DARK_RED), 16, 16);
			itemRender.renderItemAndEffectIntoGUI(tileEntity.missingStack, 144, 27);
			
			RenderHelper.disableStandardItemLighting();
			GlStateManager.popMatrix();
		}
		else {
			mc.getTextureManager().bindTexture(MekanismUtils.getResource(ResourceType.GUI_ELEMENT, "GuiSlot.png"));
			drawTexturedModalRect(143, 26, SlotOverlay.CHECK.textureX, SlotOverlay.CHECK.textureY, 18, 18);
		}

		if(xAxis >= 164 && xAxis <= 168 && yAxis >= 25 && yAxis <= 77)
		{
			drawHoveringText(MekanismUtils.getEnergyDisplay(tileEntity.getEnergy(), tileEntity.getMaxEnergy()), xAxis, yAxis);
		}

		if(xAxis >= 147 && xAxis <= 161 && yAxis >= 47 && yAxis <= 61)
		{
			drawHoveringText(LangUtils.localize("gui.mekanism.autoEject"), xAxis, yAxis);
		}

		if(xAxis >= 147 && xAxis <= 161 && yAxis >= 63 && yAxis <= 77)
		{
			drawHoveringText(LangUtils.localize("gui.mekanism.digitalMiner.autoPull"), xAxis, yAxis);
		}

		if(xAxis >= 144 && xAxis <= 160 && yAxis >= 27 && yAxis <= 43)
		{
			if(!tileEntity.missingStack.isEmpty())
			{
				drawHoveringText(LangUtils.localize("gui.mekanism.digitalMiner.missingBlock"), xAxis, yAxis);
			}
			else {
				drawHoveringText(LangUtils.localize("gui.mekanism.well"), xAxis, yAxis);
			}
		}

		if(xAxis >= 131 && xAxis <= 145 && yAxis >= 47 && yAxis <= 61)
		{
			drawHoveringText(LangUtils.localize("gui.mekanism.digitalMiner.reset"), xAxis, yAxis);
		}

		if(xAxis >= 131 && xAxis <= 145 && yAxis >= 63 && yAxis <= 77)
		{
			drawHoveringText(LangUtils.localize("gui.mekanism.digitalMiner.silkTouch"), xAxis, yAxis);
		}

		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY)
	{
		mc.renderEngine.bindTexture(MekanismUtils.getResource(ResourceType.GUI, "GuiDigitalMiner.png"));
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		int guiWidth = (width - xSize) / 2;
		int guiHeight = (height - ySize) / 2;
		drawTexturedModalRect(guiWidth, guiHeight, 0, 0, xSize, ySize);

		int xAxis = mouseX - guiWidth;
		int yAxis = mouseY - guiHeight;

		int displayInt;

		displayInt = tileEntity.getScaledEnergyLevel(52);
		drawTexturedModalRect(guiWidth + 164, guiHeight + 25 + 52 - displayInt, 176, 52 - displayInt, 4, displayInt);

		if(xAxis >= 147 && xAxis <= 161 && yAxis >= 47 && yAxis <= 61)
		{
			drawTexturedModalRect(guiWidth + 147, guiHeight + 47, 176 + 4, 0, 14, 14);
		}
		else {
			drawTexturedModalRect(guiWidth + 147, guiHeight + 47, 176 + 4, 14, 14, 14);
		}

		if(xAxis >= 147 && xAxis <= 161 && yAxis >= 63 && yAxis <= 77)
		{
			drawTexturedModalRect(guiWidth + 147, guiHeight + 63, 176 + 4 + 14, 0, 14, 14);
		}
		else {
			drawTexturedModalRect(guiWidth + 147, guiHeight + 63, 176 + 4 + 14, 14, 14, 14);
		}

		if(xAxis >= 131 && xAxis <= 145 && yAxis >= 47 && yAxis <= 61)
		{
			drawTexturedModalRect(guiWidth + 131, guiHeight + 47, 176 + 4 + 28, 0, 14, 14);
		}
		else {
			drawTexturedModalRect(guiWidth + 131, guiHeight + 47, 176 + 4 + 28, 14, 14, 14);
		}

		if(xAxis >= 131 && xAxis <= 145 && yAxis >= 63 && yAxis <= 77)
		{
			drawTexturedModalRect(guiWidth + 131, guiHeight + 63, 176 + 4 + 42, 0, 14, 14);
		}
		else {
			drawTexturedModalRect(guiWidth + 131, guiHeight + 63, 176 + 4 + 42, 14, 14, 14);
		}

		super.drawGuiContainerBackgroundLayer(partialTick, mouseX, mouseY);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException
	{
		super.mouseClicked(mouseX, mouseY, button);

		if(button == 0)
		{
			int xAxis = (mouseX - (width - xSize) / 2);
			int yAxis = (mouseY - (height - ySize) / 2);

			if(xAxis >= 147 && xAxis <= 161 && yAxis >= 47 && yAxis <= 61)
			{
				SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);

				ArrayList<Object> data = new ArrayList<>();
				data.add(0);

				Mekanism.packetHandler.sendToServer(new TileEntityMessage(Coord4D.get(tileEntity), data));
			}

			if(xAxis >= 147 && xAxis <= 161 && yAxis >= 63 && yAxis <= 77)
			{
				SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);

				ArrayList<Object> data = new ArrayList<>();
				data.add(1);

				Mekanism.packetHandler.sendToServer(new TileEntityMessage(Coord4D.get(tileEntity), data));
			}

			if(xAxis >= 131 && xAxis <= 145 && yAxis >= 47 && yAxis <= 61)
			{
				SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);

				ArrayList<Object> data = new ArrayList<>();
				data.add(5);

				Mekanism.packetHandler.sendToServer(new TileEntityMessage(Coord4D.get(tileEntity), data));
			}

			if(xAxis >= 131 && xAxis <= 145 && yAxis >= 63 && yAxis <= 77)
			{
				SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);

				ArrayList<Object> data = new ArrayList<>();
				data.add(9);

				Mekanism.packetHandler.sendToServer(new TileEntityMessage(Coord4D.get(tileEntity), data));
			}
		}
	}
}
