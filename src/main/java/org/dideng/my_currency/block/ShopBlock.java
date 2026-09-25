package org.dideng.my_currency.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.dideng.my_currency.block.entity.ShopBlockEntity;
import org.dideng.my_currency.menu.ShopMenu;
import org.jetbrains.annotations.Nullable;

// 一格小店，明码标价，店主离线也照常营业
@SuppressWarnings("NullableProblems")
public class ShopBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public ShopBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
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
        return new ShopBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof ShopBlockEntity shop && placer instanceof Player player) {
            shop.setOwner(player.getUUID(), player.getGameProfile().getName());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ShopBlockEntity shop)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        boolean ownerMode = player.hasPermissions(2)
                || (shop.getOwner() != null && shop.getOwner().equals(player.getUUID()));
        MenuProvider provider = new SimpleMenuProvider(
                (id, inventory, p) -> {
                    ShopMenu menu = new ShopMenu(id, pos, ownerMode, ContainerLevelAccess.create(level, pos));
                    if (ownerMode) {
                        menu.bindOwnerInventory(shop, inventory);
                    }
                    return menu;
                },
                Component.translatable("block.my_currency.shop"));
        boolean finalOwnerMode = ownerMode;
        serverPlayer.openMenu(provider, buf -> {
            buf.writeBlockPos(pos);
            buf.writeBoolean(finalOwnerMode);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, shop.getProduct());
            buf.writeLong(shop.getPrice());
            buf.writeBoolean(shop.isInfinite());
            buf.writeInt(shop.getStockCount());
        });
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState,
                            boolean movedByPiston) {
        if (!oldState.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof ShopBlockEntity shop) {
            shop.dropContents(level, pos);
        }
        super.onRemove(oldState, level, pos, newState, movedByPiston);
    }
}
