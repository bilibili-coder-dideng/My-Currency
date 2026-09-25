package org.dideng.my_currency.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.dideng.my_currency.menu.TransferMenu;

import java.util.List;

// 掌中转账终端，点一下就是一笔人情
public class TransferDeviceItem extends Item {
    public TransferDeviceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.consume(stack);
        }
        // 名单只收活人，自己除外
        List<String> names = serverPlayer.server.getPlayerList().getPlayers().stream()
                .filter(other -> other != serverPlayer)
                .map(other -> other.getGameProfile().getName())
                .toList();
        if (names.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.my_currency.transfer_nobody"), true);
            return InteractionResultHolder.fail(stack);
        }
        serverPlayer.openMenu(new SimpleMenuProvider(
                        (id, inventory, p) -> new TransferMenu(id, names),
                        Component.translatable("item.my_currency.transfer_device")),
                buf -> {
                    buf.writeVarInt(names.size());
                    for (String name : names) {
                        buf.writeUtf(name, 16);
                    }
                });
        return InteractionResultHolder.success(stack);
    }
}
