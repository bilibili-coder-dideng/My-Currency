package org.dideng.my_currency.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.dideng.my_currency.My_currency;
import org.dideng.my_currency.attachment.ModAttachments;
import org.dideng.my_currency.menu.BankMenu;
import org.dideng.my_currency.network.ModPayloads;

// 蓝皮屏幕配六个红按钮，ATM 那味儿就有了
public class BankScreen extends AbstractContainerScreen<BankMenu> {
    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "textures/gui/bank.png");

    public BankScreen(BankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 180;
        // 库存名和标题都藏好，余额屏自己说话
        this.titleLabelY = -10000;
        this.inventoryLabelY = -10000;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        addRenderableWidget(Button.builder(Component.translatable("gui.my_currency.bank.deposit_all"),
                        b -> this.minecraft.getConnection().send(ModPayloads.Deposit.INSTANCE))
                .bounds(x + 10, y + 38, 156, 16).build());

        // 第一排大面额，第二排小面额加清户
        int[][] buttons = {
                {0, 56}, {1, 56}, {2, 56},
                {3, 76}, {4, 76}, {-1, 76}
        };
        for (int i = 0; i < buttons.length; i++) {
            int denomination = buttons[i][0];
            int rowY = buttons[i][1];
            int col = i % 3;
            Component text = denomination == -1
                    ? Component.translatable("gui.my_currency.bank.withdraw_all")
                    : Component.translatable("gui.my_currency.bank.take",
                            denominationValue(denomination));
            addRenderableWidget(Button.builder(text,
                            b -> this.minecraft.getConnection().send(new ModPayloads.Withdraw(denomination)))
                    .bounds(x + 10 + col * 52, y + rowY, 50, 18).build());
        }
    }

    // 和 ModItems.BY_VALUE_DESC 一个鼻孔出气：0 最大 4 最小
    private static long denominationValue(int index) {
        return new long[]{10000, 1000, 100, 10, 1}[index];
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 贴图不是256见方，必须老实报上真实尺寸，不然MC会把左上角一小块拉满屏
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
