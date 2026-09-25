package org.dideng.my_currency.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

// 终端没格子，一个价签、一个付款键，管理员多一个改价小灶
public class PaymentTerminalMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    public long price;
    public final boolean admin;
    @Nullable
    private final ContainerLevelAccess access;

    public PaymentTerminalMenu(int containerId, BlockPos pos, long price, boolean admin,
                               @Nullable ContainerLevelAccess access) {
        super(ModMenuTypes.TERMINAL_MENU.get(), containerId);
        this.pos = pos;
        this.price = price;
        this.admin = admin;
        this.access = access;
    }

    public static PaymentTerminalMenu client(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        long price = buf.readLong();
        boolean admin = buf.readBoolean();
        return new PaymentTerminalMenu(containerId, pos, price, admin, null);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return access == null || access.evaluate((level, pos) ->
                player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }
}
