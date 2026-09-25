package org.dideng.my_currency.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.dideng.my_currency.My_currency;
import org.dideng.my_currency.menu.ModMenuTypes;

// 柜台窗口的装修队，仅客户端上岗
@EventBusSubscriber(modid = My_currency.MODID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.BANK_MENU.get(), BankScreen::new);
        event.register(ModMenuTypes.REGISTER_MENU.get(), CashRegisterScreen::new);
        event.register(ModMenuTypes.TRANSFER_MENU.get(), TransferScreen::new);
        event.register(ModMenuTypes.TERMINAL_MENU.get(), PaymentTerminalScreen::new);
        event.register(ModMenuTypes.SHOP_MENU.get(), ShopScreen::new);
    }
}
