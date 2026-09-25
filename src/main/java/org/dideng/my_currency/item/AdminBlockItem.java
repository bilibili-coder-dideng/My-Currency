package org.dideng.my_currency.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

// 没有工牌，禁止施工
public class AdminBlockItem extends BlockItem {
    public AdminBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player != null && !player.hasPermissions(2)) {
            if (!context.getLevel().isClientSide) {
                player.displayClientMessage(Component.translatable("message.my_currency.admin_only"), true);
            }
            return InteractionResult.FAIL;
        }
        return super.useOn(context);
    }
}
