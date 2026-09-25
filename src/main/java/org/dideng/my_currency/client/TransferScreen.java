package org.dideng.my_currency.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.dideng.my_currency.My_currency;
import org.dideng.my_currency.attachment.ModAttachments;
import org.dideng.my_currency.menu.TransferMenu;
import org.dideng.my_currency.network.ModPayloads;
import org.lwjgl.glfw.GLFW;

// 上半屏看余额，中间选人，底下敲数，钱就去隔壁口袋了
public class TransferScreen extends AbstractContainerScreen<TransferMenu> {
    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "textures/gui/transfer.png");

    private EditBox amountBox;
    private Button targetButton;

    public TransferScreen(TransferMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 100;
        this.titleLabelY = -10000;
        this.inventoryLabelY = -10000;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        targetButton = addRenderableWidget(Button.builder(targetLabel(), b -> {
                    this.menu.selected = (this.menu.selected + 1) % this.menu.players.size();
                    b.setMessage(targetLabel());
                })
                .bounds(x + 10, y + 38, 156, 18).build());

        amountBox = new EditBox(this.font, x + 20, y + 58, 136, 18,
                Component.translatable("gui.my_currency.transfer.amount"));
        amountBox.setMaxLength(12);
        amountBox.setFilter(text -> text.isEmpty() || text.matches("\\d{1,12}"));
        addRenderableWidget(amountBox);
        setInitialFocus(amountBox);

        addRenderableWidget(Button.builder(Component.translatable("gui.my_currency.transfer.confirm"),
                        b -> confirm())
                .bounds(x + 10, y + 78, 156, 18).build());
    }

    private Component targetLabel() {
        String name = this.menu.players.get(this.menu.selected);
        return Component.translatable("gui.my_currency.transfer.to", name);
    }

    private void confirm() {
        long amount;
        try {
            amount = Long.parseLong(amountBox.getValue());
        } catch (NumberFormatException e) {
            return;
        }
        if (amount <= 0) {
            return;
        }
        this.minecraft.getConnection().send(
                new ModPayloads.Transfer(this.menu.players.get(this.menu.selected), amount));
        this.minecraft.player.closeContainer();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BG, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        long balance = this.minecraft.player.getData(ModAttachments.BALANCE.get());
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.my_currency.bank.balance", balance),
                this.imageWidth / 2, 19, 0x6CFF8A);
    }
}
