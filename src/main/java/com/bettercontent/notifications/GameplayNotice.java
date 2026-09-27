package com.bettercontent.notifications;

import java.util.Objects;
import net.minecraft.network.chat.Component;

public record GameplayNotice(String identity, NoticeTheme theme, Component title, Component hint, int accentRgb) {
    public GameplayNotice(String identity, NoticeTheme theme, Component title, Component hint) {
        this(identity, theme, title, hint, theme == NoticeTheme.COMBAT ? 0xC73E42 : 0xC6A15B);
    }

    public GameplayNotice {
        Objects.requireNonNull(identity);
        Objects.requireNonNull(theme);
        Objects.requireNonNull(title);
        Objects.requireNonNull(hint);
        if (identity.isBlank() || identity.length() > 160) {
            throw new IllegalArgumentException("Invalid notice identity");
        }
        if (title.getString().length() > 160 || hint.getString().length() > 160) {
            throw new IllegalArgumentException("Notice text is too long");
        }
        accentRgb &= 0xFFFFFF;
    }
}
