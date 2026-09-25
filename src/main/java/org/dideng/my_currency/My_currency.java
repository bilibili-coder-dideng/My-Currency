package org.dideng.my_currency;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.dideng.my_currency.attachment.ModAttachments;
import org.dideng.my_currency.block.entity.ModBlockEntities;
import org.dideng.my_currency.command.BalanceSelectorOption;
import org.dideng.my_currency.menu.ModMenuTypes;
import org.slf4j.Logger;

@Mod(My_currency.MODID)
public class My_currency {
    public static final String MODID = "my_currency";
    private static final Logger LOGGER = LogUtils.getLogger();

    public My_currency(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);

        // 票子、钱包、银行网点统统装车
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModAttachments.ATTACHMENTS.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModCreativeModeTabs.TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 选择器扩展不是线程安全的 map，排队主线程插队
        event.enqueueWork(BalanceSelectorOption::register);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
