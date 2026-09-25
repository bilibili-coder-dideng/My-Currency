package org.dideng.my_currency.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.dideng.my_currency.My_currency;
import org.dideng.my_currency.attachment.ModAttachments;
import org.dideng.my_currency.menu.CashRegisterMenu;
import org.dideng.my_currency.network.ModPayloads;

// 顾客看到付款键，店主看到提款键，一面两吃
public class CashRegisterScreen extends AbstractContainerScreen<CashRegisterMenu> {
    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "textures/gui/cash_register.png");

    public CashRegisterScreen(CashRegisterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 92;
        this.titleLabelY = -10000;
        this.inventoryLabelY = -10000;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        if (this.menu.ownerView) {
            addRenderableWidget(Button.builder(Component.translatable("gui.my_currency.register.collect"),
                            b -> this.minecraft.getConnection().send(
                                    new ModPayloads.RegisterCollect(this.menu.pos)))
                    .bounds(x + 10, y + 52, 156, 18).build());
        } else {
            // 大面额排第一排，小面额第二排，和取款机一个路数
            int[] rows = {38, 58};
            for (int i = 0; i < 5; i++) {
                int denomination = i;
                int col = i % 3;
                int rowY = rows[i / 3];
                long value = new long[]{10000, 1000, 100, 10, 1}[i];
                addRenderableWidget(Button.builder(Component.translatable("gui.my_currency.register.pay", value),
                                b -> this.minecraft.getConnection().send(
                                        new ModPayloads.RegisterPay(this.menu.pos, denomination)))
                        .bounds(x + 10 + col * 52, y + rowY, 50, 18).build());
            }
        }
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

        if (this.menu.ownerView) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.my_currency.register.stored", this.menu.cash),
                    this.imageWidth / 2, 40, 0xFFD24F);
        } else {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.my_currency.register.owner", this.menu.ownerName),
                    this.imageWidth / 2, 79, 0x373737);
        }
    }
}
