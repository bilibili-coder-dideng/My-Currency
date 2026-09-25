package org.dideng.my_currency.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dideng.my_currency.ModBlocks;
import org.dideng.my_currency.ModItems;
import org.dideng.my_currency.My_currency;
import org.dideng.my_currency.attachment.ModAttachments;
import org.dideng.my_currency.block.entity.CashRegisterBlockEntity;
import org.dideng.my_currency.block.entity.PaymentTerminalBlockEntity;
import org.dideng.my_currency.block.entity.ShopBlockEntity;
import org.dideng.my_currency.data.PendingPayouts;
import org.dideng.my_currency.util.Money;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 金融街保安队：ATM 归管理员管，收款机和终端各归各的店主
@EventBusSubscriber(modid = My_currency.MODID)
public class ModGameEvents {

    // 开户礼，数字写死，想改改这一处
    private static final long STARTER_BALANCE = 180L;

    private static boolean isNotAdmin(Player player) {
        return !player.hasPermissions(2);
    }

    // 货币没收提示的节流表，不然每 tick 念一遍经
    private static final Map<UUID, Integer> LAST_WARN = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.isCreative() || player.hasPermissions(2)) {
            return;
        }
        boolean confiscated = false;
        Inventory inventory = player.getInventory();
        // 主背包、盔甲、副手挨个搜，钞票无处可藏
        for (List<ItemStack> section : List.of(inventory.items, inventory.armor, inventory.offhand)) {
            for (int i = 0; i < section.size(); i++) {
                if (ModItems.valueOf(section.get(i).getItem()) > 0) {
                    section.set(i, ItemStack.EMPTY);
                    confiscated = true;
                }
            }
        }
        // 光标上捏着的也算
        if (ModItems.valueOf(player.containerMenu.getCarried().getItem()) > 0) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
            confiscated = true;
        }
        if (confiscated) {
            int now = player.tickCount;
            Integer last = LAST_WARN.get(player.getUUID());
            if (last == null || now - last > 30) {
                LAST_WARN.put(player.getUUID(), now);
                player.displayClientMessage(
                        Component.translatable("message.my_currency.currency_admin_only"), true);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_WARN.remove(event.getEntity().getUUID());
    }

    // 每人 180 启动资金，限领一次；店主离线期间卖的货，上线也当场结清
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.getData(ModAttachments.STARTER_GRANTED.get())) {
            long balance = Money.saturatedAdd(player.getData(ModAttachments.BALANCE.get()), STARTER_BALANCE);
            player.setData(ModAttachments.BALANCE.get(), balance);
            player.setData(ModAttachments.STARTER_GRANTED.get(), true);
            player.displayClientMessage(
                    Component.translatable("message.my_currency.starter_bonus", STARTER_BALANCE, balance), false);
        }
        long payout = PendingPayouts.get(player.server).collect(player.getUUID());
        if (payout > 0) {
            long balance = Money.saturatedAdd(player.getData(ModAttachments.BALANCE.get()), payout);
            player.setData(ModAttachments.BALANCE.get(), balance);
            player.displayClientMessage(
                    Component.translatable("message.my_currency.shop_pending", payout, balance), false);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getState().is(ModBlocks.BANK_MACHINE.get()) && isNotAdmin(event.getPlayer())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(
                    Component.translatable("message.my_currency.admin_only"), true);
            return;
        }
        if (event.getState().is(ModBlocks.CASH_REGISTER.get())
                && !canTouchRegister(event.getPlayer(), event)) {
            event.setCanceled(true);
            String shop = event.getLevel().getBlockEntity(event.getPos()) instanceof CashRegisterBlockEntity register
                    ? register.getOwnerName() : "?";
            event.getPlayer().displayClientMessage(
                    Component.translatable("message.my_currency.register_protected", shop), true);
        }
        if (event.getState().is(ModBlocks.PAYMENT_TERMINAL.get())
                && !canTouchTerminal(event.getPlayer(), event)) {
            event.setCanceled(true);
            String owner = event.getLevel().getBlockEntity(event.getPos()) instanceof PaymentTerminalBlockEntity terminal
                    ? terminal.getOwnerName() : "?";
            event.getPlayer().displayClientMessage(
                    Component.translatable("message.my_currency.terminal_protected", owner), true);
        }
        if (event.getState().is(ModBlocks.SHOP.get())
                && !canTouchShop(event.getPlayer(), event)) {
            event.setCanceled(true);
            String owner = event.getLevel().getBlockEntity(event.getPos()) instanceof ShopBlockEntity shop
                    ? shop.getOwnerName() : "?";
            event.getPlayer().displayClientMessage(
                    Component.translatable("message.my_currency.shop_protected", owner), true);
        }
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        // 连裂纹都不许有，劝退要趁早
        var pos = event.getPos();
        var state = event.getLevel().getBlockState(pos);
        var player = event.getEntity();
        if (state.is(ModBlocks.BANK_MACHINE.get()) && isNotAdmin(player)) {
            event.setCanceled(true);
        } else if (state.is(ModBlocks.CASH_REGISTER.get()) && !player.hasPermissions(2)
                && event.getLevel().getBlockEntity(pos) instanceof CashRegisterBlockEntity register
                && (register.getOwner() == null || !register.getOwner().equals(player.getUUID()))) {
            event.setCanceled(true);
        } else if (state.is(ModBlocks.PAYMENT_TERMINAL.get()) && !player.hasPermissions(2)
                && event.getLevel().getBlockEntity(pos) instanceof PaymentTerminalBlockEntity terminal
                && (terminal.getOwner() == null || !terminal.getOwner().equals(player.getUUID()))) {
            event.setCanceled(true);
        } else if (state.is(ModBlocks.SHOP.get()) && !player.hasPermissions(2)
                && event.getLevel().getBlockEntity(pos) instanceof ShopBlockEntity shop
                && (shop.getOwner() == null || !shop.getOwner().equals(player.getUUID()))) {
            event.setCanceled(true);
        }
    }

    private static boolean canTouchRegister(Player player, BlockEvent.BreakEvent event) {
        if (player.hasPermissions(2)) {
            return true;
        }
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof CashRegisterBlockEntity register) {
            UUID owner = register.getOwner();
            return owner != null && owner.equals(player.getUUID());
        }
        return false;
    }

    private static boolean canTouchTerminal(Player player, BlockEvent.BreakEvent event) {
        if (player.hasPermissions(2)) {
            return true;
        }
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof PaymentTerminalBlockEntity terminal) {
            UUID owner = terminal.getOwner();
            return owner != null && owner.equals(player.getUUID());
        }
        return false;
    }

    private static boolean canTouchShop(Player player, BlockEvent.BreakEvent event) {
        if (player.hasPermissions(2)) {
            return true;
        }
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof ShopBlockEntity shop) {
            UUID owner = shop.getOwner();
            return owner != null && owner.equals(player.getUUID());
        }
        return false;
    }
}
