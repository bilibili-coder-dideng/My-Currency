package org.dideng.my_currency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.dideng.my_currency.My_currency;
import org.dideng.my_currency.attachment.ModAttachments;
import org.dideng.my_currency.util.Money;

import java.util.Collection;

// /balance 查账增删三件套，OP 专属印钞机与抽水机
@EventBusSubscriber(modid = My_currency.MODID)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("balance")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.literal("get")
                                .executes(ctx -> query(ctx.getSource(),
                                        EntityArgument.getPlayers(ctx, "targets"))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                        .executes(ctx -> adjust(ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                LongArgumentType.getLong(ctx, "amount")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                        .executes(ctx -> adjust(ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                -LongArgumentType.getLong(ctx, "amount")))))));
    }

    private static int query(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            long balance = player.getData(ModAttachments.BALANCE.get());
            source.sendSuccess(() -> Component.translatable(
                    "commands.my_currency.balance.get", player.getName(), balance), false);
        }
        return targets.size();
    }

    private static int adjust(CommandSourceStack source, Collection<ServerPlayer> targets, long delta) {
        for (ServerPlayer player : targets) {
            long before = player.getData(ModAttachments.BALANCE.get());
            // 扣款不许扣成负数，印钞不许印爆 long
            long after = Math.max(0L, Money.saturatedAdd(before, delta));
            player.setData(ModAttachments.BALANCE.get(), after);

            boolean adding = delta > 0;
            long amount = Math.abs(delta);
            source.sendSuccess(() -> Component.translatable(
                    adding ? "commands.my_currency.balance.add.success"
                           : "commands.my_currency.balance.remove.success",
                    player.getName(), amount, after), true);

            // 当事人也有知情权
            player.displayClientMessage(Component.translatable(
                    adding ? "message.my_currency.balance.added"
                           : "message.my_currency.balance.removed",
                    amount, after), false);
        }
        return targets.size();
    }
}
