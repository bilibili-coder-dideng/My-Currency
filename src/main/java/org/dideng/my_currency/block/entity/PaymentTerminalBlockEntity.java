package org.dideng.my_currency.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// 拍钱就通电两秒，红石世界的自动售货机心脏
public class PaymentTerminalBlockEntity extends BlockEntity {
    // 一次付款亮两秒，40 tick 的快乐
    public static final int PULSE_TICKS = 40;

    private long price;
    private int pulse;
    @Nullable
    private UUID owner;
    private String ownerName = "";

    public PaymentTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PAYMENT_TERMINAL.get(), pos, state);
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

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = Math.max(0L, price);
        setChanged();
    }

    public boolean isActive() {
        return pulse > 0;
    }

    // 钱到账，信号起来；重复付款只会续杯，不会把邻居闪坏
    public void activate() {
        boolean wasInactive = pulse == 0;
        pulse = PULSE_TICKS;
        setChanged();
        // 先通电再喊邻居来摸，不然人家扑个空
        if (level != null && wasInactive) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PaymentTerminalBlockEntity terminal) {
        if (terminal.pulse > 0) {
            terminal.pulse--;
            if (terminal.pulse == 0) {
                level.updateNeighborsAt(pos, state.getBlock());
                terminal.setChanged();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Price", price);
        tag.putInt("Pulse", pulse);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putString("OwnerName", ownerName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        price = tag.getLong("Price");
        pulse = tag.getInt("Pulse");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
    }
}
