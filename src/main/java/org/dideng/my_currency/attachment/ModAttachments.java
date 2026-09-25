package org.dideng.my_currency.attachment;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.dideng.my_currency.My_currency;

import java.util.function.Supplier;

// 玩家随身账户，死了钱还在，银行的承诺
public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, My_currency.MODID);

    // 长这样还得亲自教网络部怎么读 long
    private static final StreamCodec<RegistryFriendlyByteBuf, Long> LONG_SYNC = StreamCodec.of(
            RegistryFriendlyByteBuf::writeLong,
            RegistryFriendlyByteBuf::readLong);

    public static final Supplier<AttachmentType<Long>> BALANCE = ATTACHMENTS.register("balance",
            () -> AttachmentType.builder(() -> 0L)
                    .serialize(Codec.LONG)
                    .sync(LONG_SYNC)
                    .copyOnDeath()
                    .build());

    // 开局启动资金有没有领过，防止把服务器领破产
    public static final Supplier<AttachmentType<Boolean>> STARTER_GRANTED = ATTACHMENTS.register("starter_granted",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .build());
}
