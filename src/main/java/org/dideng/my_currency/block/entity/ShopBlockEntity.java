package org.dideng.my_currency.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.dideng.my_currency.ModItems;
import org.dideng.my_currency.attachment.ModAttachments;
import org.dideng.my_currency.data.PendingPayouts;
import org.dideng.my_currency.menu.ShopMenu;
import org.dideng.my_currency.network.ModPayloads;
import org.dideng.my_currency.util.Money;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// 一格样品、二十七格库存，童叟无欺
public class ShopBlockEntity extends BlockEntity {
    public static final int SAMPLE_SLOT = 0;
    public static final int STOCK_SLOTS = 27;
    public static final int TOTAL_SLOTS = 1 + STOCK_SLOTS;

    private long price;
    private boolean infinite;
    @Nullable
    private UUID owner;
    private String ownerName = "";

    public final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SAMPLE_SLOT) {
                // 拿钱卖钱属于空手套白狼，本店拒绝开展洗钱业务
                return ModItems.valueOf(stack.getItem()) <= 0;
            }
            // 无限模式不收实物，实物补货必须和样品一模一样（附魔 NBT 都得对）
            if (infinite) {
                return false;
            }
            ItemStack sample = getStackInSlot(SAMPLE_SLOT);
            return !sample.isEmpty() && ItemStack.isSameItemSameComponents(sample, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            pushStateToViewers();
        }
    };

    // 店主摆货换样品的瞬间，所有开着这家店窗口的玩家立刻看到新行情
    private void pushStateToViewers() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ModPayloads.ShopState state = new ModPayloads.ShopState(
                getBlockPos(), getProduct(), price, infinite, getStockCount());
        for (ServerPlayer viewer : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (viewer.containerMenu instanceof ShopMenu menu && menu.pos.equals(getBlockPos())) {
                viewer.connection.send(state);
            }
        }
    }

    public ShopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHOP.get(), pos, state);
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = Math.max(0L, price);
        setChanged();
    }

    public boolean isInfinite() {
        return infinite;
    }

    public void setInfinite(boolean infinite) {
        this.infinite = infinite;
        setChanged();
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwner(@Nullable UUID id, String name) {
        this.owner = id;
        this.ownerName = name;
        setChanged();
    }

    public ItemStack getProduct() {
        return items.getStackInSlot(SAMPLE_SLOT);
    }

    // 只数和样品同款的，换过样品的旧库存不会被冒认awa
    
    public int getStockCount() {
        ItemStack product = getProduct();
        if (product.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 1; i < TOTAL_SLOTS; i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(product, stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public enum SellResult { OK, NO_PRODUCT, NO_PRICE, NO_MONEY, SOLD_OUT, INVENTORY_FULL, SELF }

    // 一手交钱一手交货，任何一步翻车都原地回滚
    public SellResult sellOne(ServerPlayer buyer) {
        ItemStack product = getProduct();
        if (product.isEmpty() || ModItems.valueOf(product.getItem()) > 0) {
            return SellResult.NO_PRODUCT;
        }
        if (price <= 0) {
            return SellResult.NO_PRICE;
        }
        if (owner != null && owner.equals(buyer.getUUID())) {
            return SellResult.SELF;
        }
        long balance = buyer.getData(ModAttachments.BALANCE.get());
        if (balance < price) {
            return SellResult.NO_MONEY;
        }

        ItemStack sold;
        int fromSlot = -1;
        if (infinite) {
            sold = product.copyWithCount(1);
        } else {
            for (int i = 1; i < TOTAL_SLOTS; i++) {
                ItemStack stack = items.getStackInSlot(i);
                if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(product, stack)) {
                    fromSlot = i;
                    break;
                }
            }
            if (fromSlot < 0) {
                return SellResult.SOLD_OUT;
            }
            sold = items.extractItem(fromSlot, 1, false);
            if (sold.isEmpty()) {
                return SellResult.SOLD_OUT;
            }
        }

        long oldBalance = balance;
        buyer.setData(ModAttachments.BALANCE.get(), balance - price);
        if (!buyer.getInventory().add(sold)) {
            // 背包塞不下：钱退回去，货摆回货架，就当什么都没发生
            buyer.setData(ModAttachments.BALANCE.get(), oldBalance);
            if (!infinite && fromSlot >= 0) {
                items.insertItem(fromSlot, sold, false);
            }
            return SellResult.INVENTORY_FULL;
        }

        payOwner(buyer, price);
        setChanged();
        return SellResult.OK;
    }

    private void payOwner(ServerPlayer buyer, long amount) {
        if (owner == null || level == null) {
            return;
        }
        ServerPlayer shopOwner = buyer.server.getPlayerList().getPlayer(owner);
        if (shopOwner != null) {
            long current = shopOwner.getData(ModAttachments.BALANCE.get());
            shopOwner.setData(ModAttachments.BALANCE.get(), Money.saturatedAdd(current, amount));
            shopOwner.displayClientMessage(
                    Component.translatable("message.my_currency.shop_sold",
                            buyer.getGameProfile().getName(), amount), true);
        } else {
            // 店主翘班也不耽误营业，先记账，上线连本带利（没有利）结清
            PendingPayouts.get(buyer.server).credit(owner, amount);
        }
    }

    // 拆店大甩卖：样品和库存全吐出来，一分不吞
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Price", price);
        tag.putBoolean("Infinite", infinite);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putString("OwnerName", ownerName);
        tag.put("Stock", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        price = tag.getLong("Price");
        infinite = tag.getBoolean("Infinite");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        if (tag.contains("Stock")) {
            items.deserializeNBT(registries, tag.getCompound("Stock"));
        }
    }
}
