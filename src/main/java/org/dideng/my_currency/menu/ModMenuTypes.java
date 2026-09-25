package org.dideng.my_currency.menu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.dideng.my_currency.My_currency;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, My_currency.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<BankMenu>> BANK_MENU =
            MENUS.register("bank", () -> IMenuTypeExtension.create(BankMenu::client));

    public static final DeferredHolder<MenuType<?>, MenuType<CashRegisterMenu>> REGISTER_MENU =
            MENUS.register("cash_register", () -> IMenuTypeExtension.create(CashRegisterMenu::client));

    public static final DeferredHolder<MenuType<?>, MenuType<TransferMenu>> TRANSFER_MENU =
            MENUS.register("transfer", () -> IMenuTypeExtension.create(TransferMenu::client));

    public static final DeferredHolder<MenuType<?>, MenuType<PaymentTerminalMenu>> TERMINAL_MENU =
            MENUS.register("payment_terminal", () -> IMenuTypeExtension.create(PaymentTerminalMenu::client));

    public static final DeferredHolder<MenuType<?>, MenuType<ShopMenu>> SHOP_MENU =
            MENUS.register("shop", () -> IMenuTypeExtension.create(ShopMenu::client));
}
