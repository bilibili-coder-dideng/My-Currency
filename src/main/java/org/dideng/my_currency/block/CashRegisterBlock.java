package org.dideng.my_currency.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.dideng.my_currency.block.entity.CashRegisterBlockEntity;
import org.dideng.my_currency.menu.CashRegisterMenu;
import org.jetbrains.annotations.Nullable;

// 开门做生意，童叟无欺；店主查账，顾客掏钱
@SuppressWarnings("NullableProblems")
public class CashRegisterBlock extends BaseEntityBlock {
    // BaseEntityBlock 不带朝向，只能借 HorizontalDirectionalBlock 的模板一用
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;

    public CashRegisterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    // 钱柜也要有合法身份，序列化时才不会当场暴毙
    public static final MapCodec<CashRegisterBlock> CODEC = simpleCodec(CashRegisterBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @Nullable net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CashRegisterBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof CashRegisterBlockEntity register && placer instanceof Player player) {
            register.setOwner(player.getUUID(), player.getGameProfile().getName());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof CashRegisterBlockEntity register)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        // 店主进门直接查账，顾客上门只管掏钱
        boolean ownerView = player.getUUID().equals(register.getOwner());
        MenuProvider provider = new SimpleMenuProvider(
                (id, inventory, p) -> new CashRegisterMenu(id, pos,
                        register.getCash(), register.getOwnerName(), ownerView,
                        ContainerLevelAccess.create(level, pos)),
                Component.translatable("block.my_currency.cash_register"));
        serverPlayer.openMenu(provider, buf -> {
            buf.writeBlockPos(pos);
            buf.writeLong(register.getCash());
            buf.writeBoolean(ownerView);
            buf.writeUtf(register.getOwnerName(), 16);
        });
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // 拆店先清钱柜，票子一个子儿都不能少
        if (!oldState.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof CashRegisterBlockEntity register) {
            register.dropCash(level, pos);
        }
        super.onRemove(oldState, level, pos, newState, movedByPiston);
    }
}
