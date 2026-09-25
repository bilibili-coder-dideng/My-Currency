package org.dideng.my_currency.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.dideng.my_currency.util.Money;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 店主离线期间的营业款暂存处，上线一次性结清，绝不拖欠
public class PendingPayouts extends SavedData {
    private static final String NAME = "my_currency_pending_payouts";

    private final Map<UUID, Long> debts = new HashMap<>();

    public static PendingPayouts get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new Factory<>(PendingPayouts::new, PendingPayouts::load), NAME);
    }

    public void credit(UUID player, long amount) {
        // 离线营业的流水也不许把 long 撑爆，账本封顶即止
        debts.merge(player, amount, Money::saturatedAdd);
        setDirty();
    }

    // 连本带利（并不存在利）一次取走
    public long collect(UUID player) {
        Long amount = debts.remove(player);
        if (amount != null) {
            setDirty();
        }
        return amount == null ? 0L : amount;
    }

    private static PendingPayouts load(CompoundTag tag, HolderLookup.Provider registries) {
        PendingPayouts data = new PendingPayouts();
        for (int i = 0; i < tag.getList("Debts", ListTag.TAG_COMPOUND).size(); i++) {
            CompoundTag entry = tag.getList("Debts", ListTag.TAG_COMPOUND).getCompound(i);
            data.debts.put(entry.getUUID("U"), entry.getLong("A"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        debts.forEach((id, amount) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("U", id);
            entry.putLong("A", amount);
            list.add(entry);
        });
        tag.put("Debts", list);
        return tag;
    }
}
