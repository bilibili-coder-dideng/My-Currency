package org.dideng.my_currency.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// 没格子的通讯录，名单和选中的人都在客户端手里
public class TransferMenu extends AbstractContainerMenu {
    public final List<String> players;
    public int selected;

    public TransferMenu(int containerId, List<String> players) {
        super(ModMenuTypes.TRANSFER_MENU.get(), containerId);
        this.players = List.copyOf(players);
    }

    public static TransferMenu client(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<String> names = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            names.add(buf.readUtf());
        }
        return new TransferMenu(containerId, names);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    // 揣在兜里的机器，走哪都能用
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
