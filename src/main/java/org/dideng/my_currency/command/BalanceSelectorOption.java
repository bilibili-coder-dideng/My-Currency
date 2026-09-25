package org.dideng.my_currency.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.dideng.my_currency.attachment.ModAttachments;

import java.util.function.LongPredicate;
import java.util.regex.Pattern;

// @a[balance=100..] 原版 range 风味：双点带等号，单点耍严格
public class BalanceSelectorOption {

    public static final DynamicCommandExceptionType ERROR_BALANCE_FORMAT =
            new DynamicCommandExceptionType(o -> Component.translatable("argument.my_currency.balance.invalid", o));

    // 数字 + 一或两个点 + 数字，点的数量决定等号归谁
    private static final Pattern RANGE = Pattern.compile("^(\\d*)(\\.{1,2})(\\d*)$");

    // vanilla 的选项表晚注册会抢不到座，得等它 bootStrap 完，由 ModCommands 叫号
    private static boolean registered;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        EntitySelectorOptions.register("balance", BalanceSelectorOption::handle,
                parser -> true,
                Component.translatable("argument.my_currency.options.balance.description"));
    }

    private static void handle(EntitySelectorParser parser) throws CommandSyntaxException {
        StringReader reader = parser.getReader();
        int start = reader.getCursor();
        while (reader.canRead() && reader.peek() != ',' && reader.peek() != ']') {
            reader.skip();
        }
        String token = reader.getString().substring(start, reader.getCursor()).trim();

        LongPredicate test = parse(token, reader, start);
        // 非人生物没有银行账户，余额一律视为 0 但不参与玩家筛选
        parser.addPredicate(entity -> entity instanceof Player player
                && test.test(player.getData(ModAttachments.BALANCE.get())));
    }

    // 12 精确 / 12.. ≥ / 12. > / ..12 ≤ / .12 <，12..20 还能夹个区间
    private static LongPredicate parse(String token, StringReader reader, int start)
            throws CommandSyntaxException {
        if (token.indexOf('.') < 0) {
            long value = readNumber(token, reader, start);
            return b -> b == value;
        }

        var dot = RANGE.matcher(token);
        if (!dot.matches() || (dot.group(1).isEmpty() && dot.group(3).isEmpty())) {
            throw error(reader, start, token);
        }
        String left = dot.group(1);
        String right = dot.group(3);
        // 双点闭区间讲人情，单点开区间讲规矩
        boolean inclusive = dot.group(2).length() == 2;

        LongPredicate test = b -> true;
        if (!left.isEmpty()) {
            long min = readNumber(left, reader, start);
            test = test.and(inclusive ? b -> b >= min : b -> b > min);
        }
        if (!right.isEmpty()) {
            long max = readNumber(right, reader, start);
            test = test.and(inclusive ? b -> b <= max : b -> b < max);
        }
        return test;
    }

    private static long readNumber(String raw, StringReader reader, int start) throws CommandSyntaxException {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw error(reader, start, raw);
        }
    }

    private static CommandSyntaxException error(StringReader reader, int start, String token) {
        reader.setCursor(start);
        return ERROR_BALANCE_FORMAT.createWithContext(reader, token);
    }
}
