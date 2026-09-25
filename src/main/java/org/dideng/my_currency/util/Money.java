package org.dideng.my_currency.util;

// long 加法的安全带：首富撑不爆，负翁穿不透
public final class Money {
    private Money() {
    }

    public static long saturatedAdd(long a, long b) {
        long sum = a + b;
        // 异号相加永不溢出；同号相加结果却变号，就是翻车了，按方向钳到端点
        if ((a ^ b) >= 0 && (a ^ sum) < 0) {
            return a >= 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
        return sum;
    }
}
