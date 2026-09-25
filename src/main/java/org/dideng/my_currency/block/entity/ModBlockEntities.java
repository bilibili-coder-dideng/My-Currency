package org.dideng.my_currency.block.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.dideng.my_currency.My_currency;
import org.dideng.my_currency.ModBlocks;

import java.util.function.Supplier;

// 带脑子的方块在此挂号
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, My_currency.MODID);

    public static final Supplier<BlockEntityType<CashRegisterBlockEntity>> CASH_REGISTER =
            BLOCK_ENTITIES.register("cash_register", () -> BlockEntityType.Builder
                    .of(CashRegisterBlockEntity::new, ModBlocks.CASH_REGISTER.get())
                    .build(null));

    public static final Supplier<BlockEntityType<PaymentTerminalBlockEntity>> PAYMENT_TERMINAL =
            BLOCK_ENTITIES.register("payment_terminal", () -> BlockEntityType.Builder
                    .of(PaymentTerminalBlockEntity::new, ModBlocks.PAYMENT_TERMINAL.get())
                    .build(null));

    public static final Supplier<BlockEntityType<ShopBlockEntity>> SHOP =
            BLOCK_ENTITIES.register("shop", () -> BlockEntityType.Builder
                    .of(ShopBlockEntity::new, ModBlocks.SHOP.get())
                    .build(null));
}
