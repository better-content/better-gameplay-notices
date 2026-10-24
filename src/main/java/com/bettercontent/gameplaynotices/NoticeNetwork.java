package com.bettercontent.gameplaynotices;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

final class NoticeNetwork {
    private static final String VERSION = "1";
    static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BetterGameplayNotices.MOD_ID, "notices"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private NoticeNetwork() {}

    static void register() {
        CHANNEL.messageBuilder(GameplayNotice.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(NoticeNetwork::encode)
                .decoder(NoticeNetwork::decode)
                .consumerMainThread((notice, context) -> {
                    context.get().enqueueWork(() -> NoticeClient.receive(notice));
                    context.get().setPacketHandled(true);
                }).add();
    }

    private static void encode(GameplayNotice notice, FriendlyByteBuf buffer) {
        buffer.writeUtf(notice.identity(), 160);
        buffer.writeEnum(notice.theme());
        buffer.writeComponent(notice.title());
        buffer.writeComponent(notice.hint());
        buffer.writeInt(notice.accentRgb());
    }

    private static GameplayNotice decode(FriendlyByteBuf buffer) {
        return new GameplayNotice(buffer.readUtf(160), buffer.readEnum(NoticeTheme.class),
                buffer.readComponent(), buffer.readComponent(), buffer.readInt());
    }
}
