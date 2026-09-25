package org.dideng.my_currency.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.dideng.my_currency.My_currency;
import org.dideng.my_currency.ModItems;
import org.dideng.my_currency.attachment.ModAttachments;
import org.dideng.my_currency.block.entity.CashRegisterBlockEntity;
import org.dideng.my_currency.block.entity.PaymentTerminalBlockEntity;
import org.dideng.my_currency.block.entity.ShopBlockEntity;
import org.dideng.my_currency.menu.CashRegisterMenu;
import org.dideng.my_currency.menu.ShopMenu;
import org.dideng.my_currency.util.Money;

// ATM 存取、店铺收付、玩家转账，金融命脉全在这几条专线上
@EventBusSubscriber(modid = My_currency.MODID)
public class ModPayloads {

    public record Deposit() implements CustomPacketPayload {
        public static final Deposit INSTANCE = new Deposit();
        public static final Type<Deposit> TYPE =
                new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "deposit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Deposit> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // denomination: 0..4 对应单张面额（大到小），-1 表示全部取出
    public record Withdraw(int denomination) implements CustomPacketPayload {
        public static final Type<Withdraw> TYPE =
                new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "withdraw"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Withdraw> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeInt(p.denomination),
                buf -> new Withdraw(buf.readInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // 顾客往收款机里拍钱，面额索引同 ATM
    public record RegisterPay(BlockPos pos, int denomination) implements CustomPacketPayload {
        public static final Type<RegisterPay> TYPE =
                new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "register_pay"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RegisterPay> CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeBlockPos(p.pos); buf.writeInt(p.denomination); },
                buf -> new RegisterPay(buf.readBlockPos(), buf.readInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // 店主收摊，整锅端走
    public record RegisterCollect(BlockPos pos) implements CustomPacketPayload {
        public static final Type<RegisterCollect> TYPE =
                new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "register_collect"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RegisterCollect> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBlockPos(p.pos),
                buf -> new RegisterCollect(buf.readBlockPos()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // 机内现金有变动，推给正扒在柜台前的人刷新屏幕
    public record RegisterSync(long cash) implements CustomPacketPayload {
        public static final Type<RegisterSync> TYPE =
                new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "register_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RegisterSync> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeLong(p.cash),
                buf -> new RegisterSync(buf.readLong()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // 点对点转账，报上玩家名和数额
    public record Transfer(String target, long amount) implements CustomPacketPayload {
        public static final Type<Transfer> TYPE =
                new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "transfer"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Transfer> CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeUtf(p.target, 16); buf.writeLong(p.amount); },
                buf -> new Transfer(buf.readUtf(), buf.readLong()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // 红石终端：付钱通电
    public record TerminalPay(BlockPos pos) implements CustomPacketPayload {
        public static final Type<TerminalPay> TYPE =
                new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "terminal_pay"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TerminalPay> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBlockPos(p.pos),
                buf -> new TerminalPay(buf.readBlockPos()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // 红石终端：管理员给价签定价
    public record TerminalSetPrice(BlockPos pos, long price) implements CustomPacketPayload {
        public static final Type<TerminalSetPrice> TYPE =
                new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "terminal_set_price"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TerminalSetPrice> CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeBlockPos(p.pos); buf.writeLong(p.price); },
                buf -> new TerminalSetPrice(buf.readBlockPos(), buf.readLong()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // 玩家商店：顾客拍下一件
    public record ShopBuy(BlockPos pos) implements CustomPacketPayload {
        public static final Type<ShopBuy> TYPE =
                new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "shop_buy"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ShopBuy> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBlockPos(p.pos),
                buf -> new ShopBuy(buf.readBlockPos()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // 店主定价
    public record ShopSetPrice(BlockPos pos, long price) implements CustomPacketPayload {
        public static final Type<ShopSetPrice> TYPE =
                new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "shop_set_price"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ShopSetPrice> CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeBlockPos(p.pos); buf.writeLong(p.price); },
                buf -> new ShopSetPrice(buf.readBlockPos(), buf.readLong()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // 店主切实物/无限模式
    public record ShopSetMode(BlockPos pos, boolean infinite) implements CustomPacketPayload {
        public static final Type<ShopSetMode> TYPE =
                new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "shop_set_mode"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ShopSetMode> CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeBlockPos(p.pos); buf.writeBoolean(p.infinite); },
                buf -> new ShopSetMode(buf.readBlockPos(), buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // 柜台行情广播：样品、价格、模式、库存，买家屏幕跟着刷新
    public record ShopState(BlockPos pos, ItemStack product, long price, boolean infinite, int stock)
            implements CustomPacketPayload {
        public static final Type<ShopState> TYPE =
                new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(My_currency.MODID, "shop_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ShopState> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBlockPos(p.pos);
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.product);
                    buf.writeLong(p.price);
                    buf.writeBoolean(p.infinite);
                    buf.writeInt(p.stock);
                },
                buf -> new ShopState(buf.readBlockPos(),
                        ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                        buf.readLong(), buf.readBoolean(), buf.readInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Deposit.TYPE, Deposit.CODEC, ModPayloads::handleDeposit);
        registrar.playToServer(Withdraw.TYPE, Withdraw.CODEC, ModPayloads::handleWithdraw);
        registrar.playToServer(RegisterPay.TYPE, RegisterPay.CODEC, ModPayloads::handleRegisterPay);
        registrar.playToServer(RegisterCollect.TYPE, RegisterCollect.CODEC, ModPayloads::handleRegisterCollect);
        registrar.playToServer(Transfer.TYPE, Transfer.CODEC, ModPayloads::handleTransfer);
        registrar.playToServer(TerminalPay.TYPE, TerminalPay.CODEC, ModPayloads::handleTerminalPay);
        registrar.playToServer(TerminalSetPrice.TYPE, TerminalSetPrice.CODEC, ModPayloads::handleTerminalSetPrice);
        registrar.playToServer(ShopBuy.TYPE, ShopBuy.CODEC, ModPayloads::handleShopBuy);
        registrar.playToServer(ShopSetPrice.TYPE, ShopSetPrice.CODEC, ModPayloads::handleShopSetPrice);
        registrar.playToServer(ShopSetMode.TYPE, ShopSetMode.CODEC, ModPayloads::handleShopSetMode);
        registrar.playToClient(RegisterSync.TYPE, RegisterSync.CODEC, ModPayloads::handleRegisterSync);
        registrar.playToClient(ShopState.TYPE, ShopState.CODEC, ModPayloads::handleShopState);
    }

    private static void handleRegisterSync(RegisterSync payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof CashRegisterMenu menu) {
                menu.cash = payload.cash;
            }
        });
    }

    // 顺手帮店主也刷新一下，万一他正盯着自家账本
    private static void pushRegisterSync(ServerPlayer payer, CashRegisterBlockEntity register) {
        payer.connection.send(new RegisterSync(register.getCash()));
        if (register.getOwner() != null) {
            ServerPlayer owner = payer.server.getPlayerList().getPlayer(register.getOwner());
            if (owner != null && owner != payer) {
                owner.connection.send(new RegisterSync(register.getCash()));
            }
        }
    }

    private static void handleRegisterPay(RegisterPay payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BlockPos pos = payload.pos();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                return;
            }
            BlockEntity blockEntity = player.level().getBlockEntity(pos);
            if (!(blockEntity instanceof CashRegisterBlockEntity register)) {
                return;
            }
            if (player.getUUID().equals(register.getOwner())) {
                player.displayClientMessage(Component.translatable("message.my_currency.cannot_pay_own"), true);
                return;
            }
            int index = payload.denomination();
            if (index < 0 || index >= ModItems.BY_VALUE_DESC.size()) {
                return;
            }
            long value = ModItems.valueOf(ModItems.BY_VALUE_DESC.get(index).get());
            long balance = player.getData(ModAttachments.BALANCE.get());
            if (balance < value) {
                player.displayClientMessage(Component.translatable("message.my_currency.balance_too_low"), true);
                return;
            }
            player.setData(ModAttachments.BALANCE.get(), balance - value);
            register.addCash(value);
            player.displayClientMessage(
                    Component.translatable("message.my_currency.pay_success", value), true);
            pushRegisterSync(player, register);
        });
    }

    private static void handleRegisterCollect(RegisterCollect payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BlockPos pos = payload.pos();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                return;
            }
            if (!(player.level().getBlockEntity(pos) instanceof CashRegisterBlockEntity register)) {
                return;
            }
            if (!player.getUUID().equals(register.getOwner())) {
                return;
            }
            long cash = register.getCash();
            if (cash <= 0) {
                player.displayClientMessage(Component.translatable("message.my_currency.register_empty"), true);
                return;
            }
            register.takeCash();
            long balance = Money.saturatedAdd(player.getData(ModAttachments.BALANCE.get()), cash);
            player.setData(ModAttachments.BALANCE.get(), balance);
            player.displayClientMessage(
                    Component.translatable("message.my_currency.register_collected", cash, balance), true);
            pushRegisterSync(player, register);
        });
    }

    private static void handleTerminalPay(TerminalPay payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BlockPos pos = payload.pos();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                return;
            }
            if (!(player.level().getBlockEntity(pos) instanceof PaymentTerminalBlockEntity terminal)) {
                return;
            }
            long price = terminal.getPrice();
            if (price <= 0) {
                player.displayClientMessage(Component.translatable("message.my_currency.terminal_unset"), true);
                return;
            }
            long balance = player.getData(ModAttachments.BALANCE.get());
            if (balance < price) {
                player.displayClientMessage(Component.translatable("message.my_currency.balance_too_low"), true);
                return;
            }
            player.setData(ModAttachments.BALANCE.get(), balance - price);
            terminal.activate();
            player.displayClientMessage(
                    Component.translatable("message.my_currency.terminal_paid", price), true);
        });
    }

    private static void handleTerminalSetPrice(TerminalSetPrice payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.hasPermissions(2)) {
                return;
            }
            if (payload.price() <= 0) {
                player.displayClientMessage(Component.translatable("message.my_currency.terminal_bad_price"), true);
                return;
            }
            if (player.distanceToSqr(payload.pos().getX() + 0.5, payload.pos().getY() + 0.5,
                    payload.pos().getZ() + 0.5) > 64.0) {
                return;
            }
            if (player.level().getBlockEntity(payload.pos()) instanceof PaymentTerminalBlockEntity terminal) {
                terminal.setPrice(payload.price());
                player.displayClientMessage(
                        Component.translatable("message.my_currency.terminal_price_set", payload.price()), true);
            }
        });
    }

    private static void handleShopState(ShopState payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof ShopMenu menu && menu.pos.equals(payload.pos())) {
                menu.product = payload.product();
                menu.price = payload.price();
                menu.infinite = payload.infinite();
                menu.stock = payload.stock();
            }
        });
    }

    private static void sendShopState(ServerPlayer player, ShopBlockEntity shop) {
        player.connection.send(new ShopState(shop.getBlockPos(), shop.getProduct(),
                shop.getPrice(), shop.isInfinite(), shop.getStockCount()));
    }

    // 凡是正扒在这家店窗口上的，行情一人一份
    private static void broadcastShopState(ServerPlayer source, ShopBlockEntity shop) {
        for (ServerPlayer viewer : source.server.getPlayerList().getPlayers()) {
            if (viewer.containerMenu instanceof ShopMenu menu && menu.pos.equals(shop.getBlockPos())) {
                sendShopState(viewer, shop);
            }
        }
    }

    private static void handleShopBuy(ShopBuy payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BlockPos pos = payload.pos();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                return;
            }
            if (!(player.level().getBlockEntity(pos) instanceof ShopBlockEntity shop)) {
                return;
            }
            ShopBlockEntity.SellResult result = shop.sellOne(player);
            String feedback = switch (result) {
                case OK -> "message.my_currency.shop_bought";
                case NO_PRODUCT -> "message.my_currency.shop_no_product";
                case NO_PRICE -> "message.my_currency.shop_no_price";
                case NO_MONEY -> "message.my_currency.balance_too_low";
                case SOLD_OUT -> "message.my_currency.shop_sold_out";
                case INVENTORY_FULL -> "message.my_currency.shop_inventory_full";
                case SELF -> "message.my_currency.shop_self";
            };
            Object[] args = result == ShopBlockEntity.SellResult.OK
                    ? new Object[]{shop.getPrice(), shop.getProduct().getHoverName()}
                    : new Object[0];
            player.displayClientMessage(Component.translatable(feedback, args), true);
            if (result == ShopBlockEntity.SellResult.OK) {
                broadcastShopState(player, shop);
            }
        });
    }

    private static void handleShopSetPrice(ShopSetPrice payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (payload.price() <= 0) {
                player.displayClientMessage(Component.translatable("message.my_currency.shop_bad_price"), true);
                return;
            }
            withOwnedShop(player, payload.pos(), shop -> {
                shop.setPrice(payload.price());
                broadcastShopState(player, shop);
                player.displayClientMessage(
                        Component.translatable("message.my_currency.shop_price_set", payload.price()), true);
            });
        });
    }

    private static void handleShopSetMode(ShopSetMode payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            withOwnedShop(player, payload.pos(), shop -> {
                shop.setInfinite(payload.infinite());
                broadcastShopState(player, shop);
            });
        });
    }

    // 距离 + 方块 + 店主或 OP，三连验明正身才办事
    private static void withOwnedShop(ServerPlayer player, BlockPos pos, java.util.function.Consumer<ShopBlockEntity> action) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return;
        }
        if (!(player.level().getBlockEntity(pos) instanceof ShopBlockEntity shop)) {
            return;
        }
        boolean owner = shop.getOwner() != null && shop.getOwner().equals(player.getUUID());
        if (!owner && !player.hasPermissions(2)) {
            player.displayClientMessage(Component.translatable("message.my_currency.shop_owner_only"), true);
            return;
        }
        action.accept(shop);
    }

    private static void handleTransfer(Transfer payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            long amount = payload.amount();
            if (amount <= 0) {
                player.displayClientMessage(Component.translatable("message.my_currency.transfer_bad_amount"), true);
                return;
            }
            ServerPlayer target = player.server.getPlayerList().getPlayerByName(payload.target());
            if (target == null) {
                player.displayClientMessage(Component.translatable("message.my_currency.transfer_gone"), true);
                return;
            }
            if (target == player) {
                player.displayClientMessage(Component.translatable("message.my_currency.transfer_self"), true);
                return;
            }
            long balance = player.getData(ModAttachments.BALANCE.get());
            if (balance < amount) {
                player.displayClientMessage(Component.translatable("message.my_currency.balance_too_low"), true);
                return;
            }
            long senderLeft = balance - amount;
            long targetNow = Money.saturatedAdd(target.getData(ModAttachments.BALANCE.get()), amount);
            player.setData(ModAttachments.BALANCE.get(), senderLeft);
            target.setData(ModAttachments.BALANCE.get(), targetNow);
            player.displayClientMessage(Component.translatable(
                    "message.my_currency.transfer_sent", target.getGameProfile().getName(), amount, senderLeft), true);
            target.displayClientMessage(Component.translatable(
                    "message.my_currency.transfer_received", player.getGameProfile().getName(), amount, targetNow), false);
        });
    }

    private static void handleDeposit(Deposit payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Inventory inventory = player.getInventory();
            long deposited = 0;
            // 只扫主背包，盔甲和副手的私房钱不动
            for (int i = 0; i < inventory.items.size(); i++) {
                ItemStack stack = inventory.items.get(i);
                long value = ModItems.valueOf(stack.getItem());
                if (value > 0) {
                    deposited += value * stack.getCount();
                    inventory.items.set(i, ItemStack.EMPTY);
                }
            }
            inventory.setChanged();
            long balance = Money.saturatedAdd(player.getData(ModAttachments.BALANCE.get()), deposited);
            player.setData(ModAttachments.BALANCE.get(), balance);
            if (deposited == 0) {
                player.displayClientMessage(Component.translatable("message.my_currency.no_cash"), true);
            } else {
                player.displayClientMessage(
                        Component.translatable("message.my_currency.deposit_success", deposited, balance), true);
            }
        });
    }

    private static void handleWithdraw(Withdraw payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            long balance = player.getData(ModAttachments.BALANCE.get());
            Inventory inventory = player.getInventory();
            long withdrawn = 0;

            if (payload.denomination() == -1) {
                // 全额提款，大面额优先，一张都塞不进就收摊
                boolean backpackFull = false;
                for (var deferred : ModItems.BY_VALUE_DESC) {
                    long value = ModItems.valueOf(deferred.get());
                    while (!backpackFull && balance - withdrawn >= value) {
                        int amount = (int) Math.min(ModItems.MAX_STACK, (balance - withdrawn) / value);
                        ItemStack stack = new ItemStack(deferred.get(), amount);
                        if (!inventory.add(stack)) {
                            int moved = amount - stack.getCount();
                            if (moved == 0) {
                                backpackFull = true;
                                break;
                            }
                            withdrawn += (long) moved * value;
                            break;
                        }
                        withdrawn += (long) amount * value;
                    }
                }
            } else if (payload.denomination() >= 0 && payload.denomination() < ModItems.BY_VALUE_DESC.size()) {
                var deferred = ModItems.BY_VALUE_DESC.get(payload.denomination());
                long value = ModItems.valueOf(deferred.get());
                if (balance < value) {
                    player.displayClientMessage(Component.translatable("message.my_currency.balance_too_low"), true);
                    return;
                }
                ItemStack stack = new ItemStack(deferred.get(), 1);
                if (!inventory.add(stack)) {
                    player.displayClientMessage(Component.translatable("message.my_currency.inventory_full"), true);
                    return;
                }
                withdrawn = value;
            }

            if (withdrawn > 0) {
                long remaining = balance - withdrawn;
                player.setData(ModAttachments.BALANCE.get(), remaining);
                player.displayClientMessage(
                        Component.translatable("message.my_currency.withdraw_success", withdrawn, remaining), true);
            }
        });
    }
}
