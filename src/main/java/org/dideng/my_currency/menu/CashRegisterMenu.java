package org.dideng.my_currency.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

// 收款机没格子，交易全靠余额扣来扣去
public class CashRegisterMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    public long cash;
    public final boolean ownerView;
    public final String ownerName;
    @Nullable
    private final ContainerLevelAccess access;

    public CashRegisterMenu(int containerId, BlockPos pos, long cash, String ownerName,
                            boolean ownerView, @Nullable ContainerLevelAccess access) {
        super(ModMenuTypes.REGISTER_MENU.get(), containerId);
        this.pos = pos;
        this.cash = cash;
        this.ownerName = ownerName;
        this.ownerView = ownerView;
        this.access = access;
    }

    // 客户端照单收货：坐标、营收、身份、店名，读写顺序必须拜把子
    public static CashRegisterMenu client(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        long cash = buf.readLong();
        boolean ownerView = buf.readBoolean();
        String ownerName = buf.readUtf();
        return new CashRegisterMenu(containerId, pos, cash, ownerName, ownerView, null);
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
