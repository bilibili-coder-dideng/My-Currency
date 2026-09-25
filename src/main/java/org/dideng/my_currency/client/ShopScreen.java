package org.dideng.my_currency.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.dideng.my_currency.My_currency;
import org.dideng.my_currency.attachment.ModAttachments;
import org.dideng.my_currency.menu.ShopMenu;
import org.dideng.my_currency.network.ModPayloads;
import org.lwjgl.glfw.GLFW;

// 店主摆货定价，顾客掏钱拿货，同个柜台两副面孔
public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private static final ResourceLocation OWNER_BG =
            ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "textures/gui/shop_owner.png");
    private static final ResourceLocation CUSTOMER_BG =
            ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "textures/gui/shop_customer.png");

    private EditBox priceBox;
    private Button buyButton;
    private Button modeButton;

    public ShopScreen(ShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = menu.ownerMode ? 204 : 130;
        this.titleLabelY = -10000;
        this.inventoryLabelY = -10000;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        if (this.menu.ownerMode) {
            priceBox = new EditBox(this.font, x + 30, y + 40, 94, 16,
                    Component.translatable("gui.my_currency.shop.price_hint"));
            priceBox.setMaxLength(12);
            priceBox.setFilter(text -> text.isEmpty() || text.matches("\\d{1,12}"));
            if (this.menu.price > 0) {
                priceBox.setValue(Long.toString(this.menu.price));
            }
            addRenderableWidget(priceBox);

            modeButton = addRenderableWidget(Button.builder(modeLabel(),
                            b -> this.minecraft.getConnection().send(
                                    new ModPayloads.ShopSetMode(this.menu.pos, !this.menu.infinite)))
                    .bounds(x + 126, y + 39, 40, 18).build());
        } else {
            buyButton = addRenderableWidget(Button.builder(buyLabel(),
                            b -> this.minecraft.getConnection().send(new ModPayloads.ShopBuy(this.menu.pos)))
                    .bounds(x + 10, y + 80, 156, 18).build());
        }
    }

    private Component modeLabel() {
        return Component.translatable(this.menu.infinite
                ? "gui.my_currency.shop.mode_infinite"
                : "gui.my_currency.shop.mode_stock");
    }

    private Component buyLabel() {
        return Component.translatable("gui.my_currency.shop.buy", this.menu.price);
    }

    private void submitPrice() {
        if (priceBox == null) {
            return;
        }
        try {
            long price = Long.parseLong(priceBox.getValue());
            if (price > 0) {
                this.minecraft.getConnection().send(new ModPayloads.ShopSetPrice(this.menu.pos, price));
            }
        } catch (NumberFormatException ignored) {
            // 数字框都拦过了，真能进来算它本事
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.menu.ownerMode && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            submitPrice();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation bg = this.menu.ownerMode ? OWNER_BG : CUSTOMER_BG;
        int h = this.menu.ownerMode ? 204 : 130;
        graphics.blit(bg, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, h, this.imageWidth, h);

        if (!this.menu.ownerMode && !this.menu.product.isEmpty()) {
            // 物品坐进贴图上的展示槽坑里，偏移 1px 是槽位祖传规矩
            graphics.renderItem(this.menu.product, this.leftPos + 80, this.topPos + 43);
            graphics.renderItemDecorations(this.font, this.menu.product,
                    this.leftPos + 80, this.topPos + 43);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        boolean hasProduct = !this.menu.product.isEmpty();
        Component name = hasProduct ? this.menu.product.getHoverName()
                : Component.translatable("gui.my_currency.shop.no_product");
        graphics.drawCenteredString(this.font, name, this.imageWidth / 2, 14,
                hasProduct ? 0xFFFFFF : 0xFF6C6C);

        Component detail;
        if (this.menu.price <= 0) {
            detail = Component.translatable("gui.my_currency.shop.price_hint")
                    .append(": --");
        } else {
            Component stockText = this.menu.infinite
                    ? Component.translatable("gui.my_currency.shop.infinite_stock")
                    : Component.literal(Integer.toString(this.menu.stock));
            detail = Component.translatable("gui.my_currency.shop.detail", this.menu.price, stockText);
        }
        graphics.drawCenteredString(this.font, detail, this.imageWidth / 2, 23, 0x6CFF8A);

        if (!this.menu.ownerMode) {
            long balance = this.minecraft.player.getData(ModAttachments.BALANCE.get());
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.my_currency.bank.balance", balance),
                    this.imageWidth / 2, 108, 0xFFD24F);
        }
    }

    @Override
    protected void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        // 行情随时可能被服务端刷新，按钮长相每帧跟着菜单走
        if (modeButton != null) {
            modeButton.setMessage(modeLabel());
        }
        if (buyButton != null) {
            buyButton.setMessage(buyLabel());
            buyButton.active = !this.menu.product.isEmpty() && this.menu.price > 0
                    && (this.menu.infinite || this.menu.stock > 0);
        }
    }
}
