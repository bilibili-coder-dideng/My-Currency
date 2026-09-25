package org.dideng.my_currency;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.dideng.my_currency.item.TransferDeviceItem;

import java.util.List;

// 印钞厂，谢绝参观
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(My_currency.MODID);

    // 一叠的饭量，存取款拆分也照这个数来
    public static final int MAX_STACK = 96;

    // 一格96枚，腰缠万贯不是梦
    private static DeferredItem<Item> currency(String name) {
        return ITEMS.registerSimpleItem(name, new Item.Properties().stacksTo(MAX_STACK));
    }

    public static final DeferredItem<Item> YUAN_1 = currency("yuan_1");
    public static final DeferredItem<Item> YUAN_10 = currency("yuan_10");
    public static final DeferredItem<Item> YUAN_100 = currency("yuan_100");
    public static final DeferredItem<Item> YUAN_1000 = currency("yuan_1000");
    public static final DeferredItem<Item> YUAN_10000 = currency("yuan_10000");

    // 一台走天下的掌上银行，揣兜里别弄丢
    public static final DeferredItem<Item> TRANSFER_DEVICE = ITEMS.registerItem("transfer_device",
            TransferDeviceItem::new, new Item.Properties().stacksTo(1));

    // 取款找零专用，大面额排前面
    public static final List<DeferredItem<Item>> BY_VALUE_DESC =
            List.of(YUAN_10000, YUAN_1000, YUAN_100, YUAN_10, YUAN_1);

    // 不是钱就是0，童叟无欺
    public static long valueOf(Item item) {
        if (item == YUAN_1.get()) return 1L;
        if (item == YUAN_10.get()) return 10L;
        if (item == YUAN_100.get()) return 100L;
        if (item == YUAN_1000.get()) return 1000L;
        if (item == YUAN_10000.get()) return 10000L;
        return 0L;
    }
}
