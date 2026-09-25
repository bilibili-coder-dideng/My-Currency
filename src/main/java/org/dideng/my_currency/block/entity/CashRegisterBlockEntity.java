package org.dideng.my_currency.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.dideng.my_currency.ModItems;
import org.dideng.my_currency.util.Money;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// 收款机的铁肚子，顾客的票子在这儿排队
public class CashRegisterBlockEntity extends BlockEntity {
    @Nullable
    private UUID owner;
    private String ownerName = "";
    private long cash;

    public CashRegisterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CASH_REGISTER.get(), pos, state);
    }

    public void setOwner(@Nullable UUID id, String name) {
        this.owner = id;
        this.ownerName = name;
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public long getCash() {
        return cash;
    }

    public void addCash(long amount) {
        cash = Money.saturatedAdd(cash, amount);
        setChanged();
    }

    public long takeCash() {
        long taken = cash;
        cash = 0;
        setChanged();
        return taken;
    }

    // 机器被拆，现金当场吐一地，大面额优先
    public void dropCash(Level level, BlockPos pos) {
        long remaining = cash;
        for (var deferred : ModItems.BY_VALUE_DESC) {
            long value = ModItems.valueOf(deferred.get());
            while (remaining >= value) {
                int amount = (int) Math.min(ModItems.MAX_STACK, remaining / value);
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        new ItemStack(deferred.get(), amount));
                remaining -= (long) amount * value;
            }
        }
        cash = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putString("OwnerName", ownerName);
        tag.putLong("Cash", cash);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        cash = tag.getLong("Cash");
    }
}
