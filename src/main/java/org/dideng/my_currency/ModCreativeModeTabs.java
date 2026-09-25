package org.dideng.my_currency;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// 会走路的钱包
public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, My_currency.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CURRENCY = TABS.register("currency", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.my_currency.currency"))
                    .icon(() -> ModItems.YUAN_10000.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.YUAN_1.get());
                        output.accept(ModItems.YUAN_10.get());
                        output.accept(ModItems.YUAN_100.get());
                        output.accept(ModItems.YUAN_1000.get());
                        output.accept(ModItems.YUAN_10000.get());
                        output.accept(ModItems.TRANSFER_DEVICE.get());
                        output.accept(ModBlocks.BANK_MACHINE_ITEM.get());
                        output.accept(ModBlocks.CASH_REGISTER_ITEM.get());
                        output.accept(ModBlocks.PAYMENT_TERMINAL_ITEM.get());
                        output.accept(ModBlocks.SHOP_ITEM.get());
                    })
                    .build());
}
