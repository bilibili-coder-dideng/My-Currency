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
import org.dideng.my_currency.menu.PaymentTerminalMenu;
import org.dideng.my_currency.network.ModPayloads;
import org.lwjgl.glfw.GLFW;

// 付款键谁都能拍，改价小灶只有 OP 看得到
public class PaymentTerminalScreen extends AbstractContainerScreen<PaymentTerminalMenu> {
    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "textures/gui/payment_terminal.png");

    private EditBox priceBox;

    public PaymentTerminalScreen(PaymentTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = menu.admin ? 116 : 76;
        this.titleLabelY = -10000;
        this.inventoryLabelY = -10000;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        addRenderableWidget(Button.builder(payLabel(),
                        b -> this.minecraft.getConnection().send(new ModPayloads.TerminalPay(this.menu.pos)))
                .bounds(x + 10, y + 52, 156, 18)
                .build()).active = this.menu.price > 0;

        if (this.menu.admin) {
            priceBox = new EditBox(this.font, x + 20, y + 74, 136, 18,
                    Component.translatable("gui.my_currency.terminal.set"));
            priceBox.setMaxLength(12);
            priceBox.setFilter(text -> text.isEmpty() || text.matches("\\d{1,12}"));
            if (this.menu.price > 0) {
                priceBox.setValue(Long.toString(this.menu.price));
            }
            addRenderableWidget(priceBox);

            addRenderableWidget(Button.builder(Component.translatable("gui.my_currency.terminal.set"),
                            b -> submitPrice())
                    .bounds(x + 10, y + 94, 156, 18).build());
        }
    }

    private Component payLabel() {
        return this.menu.price > 0
                ? Component.translatable("gui.my_currency.terminal.pay", this.menu.price)
                : Component.translatable("gui.my_currency.terminal.unset");
    }

    private void submitPrice() {
        long price;
        try {
            price = Long.parseLong(priceBox.getValue());
        } catch (NumberFormatException e) {
            return;
        }
        if (price <= 0) {
            return;
        }
        this.minecraft.getConnection().send(new ModPayloads.TerminalSetPrice(this.menu.pos, price));
        this.menu.price = price;
        this.minecraft.player.closeContainer();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.menu.admin && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            submitPrice();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BG, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, this.imageWidth, 116);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        long balance = this.minecraft.player.getData(ModAttachments.BALANCE.get());
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.my_currency.bank.balance", balance),
                this.imageWidth / 2, 19, 0x6CFF8A);

        Component priceText = this.menu.price > 0
                ? Component.translatable("gui.my_currency.terminal.price", this.menu.price)
                : Component.translatable("gui.my_currency.terminal.unset");
        graphics.drawCenteredString(this.font, priceText, this.imageWidth / 2, 40,
                this.menu.price > 0 ? 0xFFD24F : 0xFF6C6C);
    }
}
