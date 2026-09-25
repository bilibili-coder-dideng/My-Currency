package org.dideng.my_currency.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.dideng.my_currency.block.entity.ModBlockEntities;
import org.dideng.my_currency.block.entity.PaymentTerminalBlockEntity;
import org.dideng.my_currency.menu.PaymentTerminalMenu;
import org.jetbrains.annotations.Nullable;

// 给钱就给信号，六亲不认只认余额
@SuppressWarnings("NullableProblems")
public class PaymentTerminalBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public PaymentTerminalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // 收费终端持证上岗，拒绝 null 编外人员
    public static final MapCodec<PaymentTerminalBlock> CODEC = simpleCodec(PaymentTerminalBlock::new);

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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PaymentTerminalBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof PaymentTerminalBlockEntity terminal && placer instanceof Player player) {
            // 落子即认主，日后拆不拆得了全看这行
            terminal.setOwner(player.getUUID(), player.getGameProfile().getName());
            terminal.setChanged();
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.PAYMENT_TERMINAL.get(),
                        PaymentTerminalBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PaymentTerminalBlockEntity terminal)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        boolean admin = player.hasPermissions(2);
        MenuProvider provider = new SimpleMenuProvider(
                (id, inventory, p) -> new PaymentTerminalMenu(id, pos,
                        terminal.getPrice(), admin, ContainerLevelAccess.create(level, pos)),
                Component.translatable("block.my_currency.payment_terminal"));
        serverPlayer.openMenu(provider, buf -> {
            buf.writeBlockPos(pos);
            buf.writeLong(terminal.getPrice());
            buf.writeBoolean(admin);
        });
        return InteractionResult.CONSUME;
    }

    // 六面满强度 15，红石客官里边请
    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof PaymentTerminalBlockEntity terminal && terminal.isActive() ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    protected void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // 拆机器前记得断电，别让邻居继续空欢喜
        if (!oldState.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof PaymentTerminalBlockEntity terminal
                && terminal.isActive()) {
            level.updateNeighborsAt(pos, oldState.getBlock());
        }
        super.onRemove(oldState, level, pos, newState, movedByPiston);
    }
}
