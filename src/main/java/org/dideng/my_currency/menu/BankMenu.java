package org.dideng.my_currency.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

// 没有金库格子的菜单：钱只是数字，数字只是幻觉
public class BankMenu extends AbstractContainerMenu {
    @Nullable
    private final ContainerLevelAccess access;

    public BankMenu(int containerId, Inventory inventory, @Nullable ContainerLevelAccess access) {
        super(ModMenuTypes.BANK_MENU.get(), containerId);
        this.access = access;

        // Y坐标必须和 bank.png 上的槽位坑严格一一对应
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 100 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 158));
        }
    }

    // 余额走附件自动同步，数据包里不夹带私货了
    public static BankMenu client(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new BankMenu(containerId, inventory, null);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // 银行不收来路不明的快捷操作，存款请走按钮
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return access == null || access.evaluate((level, pos) ->
                player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }
}
