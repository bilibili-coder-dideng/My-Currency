package org.dideng.my_currency.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.dideng.my_currency.My_currency;
import org.dideng.my_currency.attachment.ModAttachments;

import java.util.Locale;

// 右下角常驻小金库读数，抬头就能看到自己有多富有
@EventBusSubscriber(modid = My_currency.MODID, value = Dist.CLIENT)
public class BalanceHudOverlay {
    private static final int EDGE = 4;

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        long balance = mc.player.getData(ModAttachments.BALANCE.get());
        String amount = String.format(Locale.ROOT, "%,d", balance);
        Component text = Component.translatable("gui.my_currency.bank.balance", amount);

        GuiGraphics graphics = event.getGuiGraphics();
        int x = graphics.guiWidth() - mc.font.width(text) - EDGE;
        int y = graphics.guiHeight() - mc.font.lineHeight - EDGE;
        graphics.drawString(mc.font, text, x, y, 0xFFD24F, true);
    }
}
