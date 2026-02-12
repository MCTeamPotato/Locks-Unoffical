package melonslise.locks.client.gui;

import melonslise.locks.Locks;
import melonslise.locks.common.container.KeyRingContainer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

@Environment(EnvType.CLIENT)
public class KeyRingScreen extends AbstractContainerScreen<KeyRingContainer>
{
	public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft","textures/gui/container/generic_54.png");

	public KeyRingScreen(KeyRingContainer cont, Inventory inv, Component title)
	{
		super(cont, inv, title);
		this.imageHeight = 114 + cont.rows * 18;
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	public void render(GuiGraphics mtx, int mouseX, int mouseY, float partialTick)
	{
		this.renderBackground(mtx,mouseX, mouseY, partialTick);
		super.render(mtx, mouseX, mouseY, partialTick);
		this.renderTooltip(mtx, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics mtx, float partialTick, int mouseX, int mouseY)
	{
		int rows = this.getMenu().rows;
		//this.minecraft.getTextureManager().bindForSetup(TEXTURE);
		int cornerX = (this.width - this.imageWidth) / 2;
		int cornerY = (this.height - this.imageHeight) / 2;
		mtx.blit(TEXTURE, cornerX, cornerY, 0, 0, this.imageWidth, rows * 18 + 17);
		mtx.blit(TEXTURE, cornerX, cornerY + rows * 18 + 17, 0, 126, this.imageWidth, 96);
	}
}
