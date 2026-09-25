package org.dideng.my_currency;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.dideng.my_currency.block.BankMachineBlock;
import org.dideng.my_currency.block.CashRegisterBlock;
import org.dideng.my_currency.block.PaymentTerminalBlock;
import org.dideng.my_currency.block.ShopBlock;
import org.dideng.my_currency.item.AdminBlockItem;

// 不动产业务部
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(My_currency.MODID);

    // 铁身板，扛得住苦力怕的讨债
    public static final DeferredBlock<BankMachineBlock> BANK_MACHINE = BLOCKS.registerBlock("bank_machine",
            BankMachineBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .pushReaction(PushReaction.BLOCK)
                    .noOcclusion());

    public static final DeferredItem<BlockItem> BANK_MACHINE_ITEM = ModItems.ITEMS.registerItem("bank_machine",
            props -> new AdminBlockItem(BANK_MACHINE.get(), props), new Item.Properties());

    // 平民版收银台，谁都能开个店，活塞推不动
    public static final DeferredBlock<CashRegisterBlock> CASH_REGISTER = BLOCKS.registerBlock("cash_register",
            CashRegisterBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()
                    .pushReaction(PushReaction.BLOCK));

    public static final DeferredItem<BlockItem> CASH_REGISTER_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("cash_register", CASH_REGISTER);

    // 给钱就通电的红石终端，人人可摆，只有改价权留给管理员
    public static final DeferredBlock<PaymentTerminalBlock> PAYMENT_TERMINAL = BLOCKS.registerBlock("payment_terminal",
            PaymentTerminalBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .pushReaction(PushReaction.BLOCK));

    public static final DeferredItem<BlockItem> PAYMENT_TERMINAL_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("payment_terminal", PAYMENT_TERMINAL);

    // 玩家自营小店，摆货定价一条龙
    public static final DeferredBlock<ShopBlock> SHOP = BLOCKS.registerBlock("shop",
            ShopBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()
                    .pushReaction(PushReaction.BLOCK));

    public static final DeferredItem<BlockItem> SHOP_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("shop", SHOP);
}
