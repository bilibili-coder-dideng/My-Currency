package org.dideng.my_currency.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.dideng.my_currency.block.entity.ShopBlockEntity;
import org.jetbrains.annotations.Nullable;

// 店主版 28 格货架操作台，顾客版只有一个购买键
public class ShopMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    public final boolean ownerMode;
    public ItemStack product = ItemStack.EMPTY;
    public long price;
    public boolean infinite;
    public int stock;

    @Nullable
    private final ContainerLevelAccess access;

    public ShopMenu(int containerId, BlockPos pos, boolean ownerMode, @Nullable ContainerLevelAccess access) {
        super(ModMenuTypes.SHOP_MENU.get(), containerId);
        this.pos = pos;
        this.ownerMode = ownerMode;
        this.access = access;
    }

    // 服务端挂真货架，坐标和 shop_owner.png 上的坑位死磕对齐
    public void bindOwnerInventory(ShopBlockEntity shop, Inventory inventory) {
        bindSlots(shop.items, inventory);
    }

    // 客户端货架是个临时替身，开店瞬间靠全量同步包灵魂附体
    private void bindSlots(IItemHandler shopItems, Inventory inventory) {
        addSlot(new SlotItemHandler(shopItems, ShopBlockEntity.SAMPLE_SLOT, 8, 38));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(shopItems, 1 + row * 9 + col, 8 + col * 18, 64 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 126 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 182));
        }
    }

    public static ShopMenu client(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean ownerMode = buf.readBoolean();
        ItemStack product = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        long price = buf.readLong();
        boolean infinite = buf.readBoolean();
        int stock = buf.readInt();
        ShopMenu menu = new ShopMenu(containerId, pos, ownerMode, null);
        menu.product = product;
        menu.price = price;
        menu.infinite = infinite;
        menu.stock = stock;
        if (ownerMode) {
            menu.bindSlots(new ItemStackHandler(ShopBlockEntity.TOTAL_SLOTS), inventory);
        }
        return menu;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!ownerMode) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < ShopBlockEntity.TOTAL_SLOTS) {
            // 货架 → 玩家背包
            if (!moveItemStackTo(stack, ShopBlockEntity.TOTAL_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 背包 → 库存槽，样品槽不参与快捷补货
            if (!moveItemStackTo(stack, 1, ShopBlockEntity.TOTAL_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return access == null || access.evaluate((level, pos) ->
                player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }
}
