package com.bettercontent.gameplaynotices;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Client-only, shared journal palette. The setting belongs to the installation, not a world. */
public final class BetterUiTheme {
    private static Boolean dark;
    private BetterUiTheme() {}

    public static boolean dark() {
        if (dark == null) load();
        return dark;
    }

    public static int color(int light, int night) { return dark() ? night : light; }

    public static String label() { return dark() ? "Light mode" : "Dark mode"; }

    public static void toggle() {
        dark = !dark();
        try {
            Path path = path();
            Files.createDirectories(path.getParent());
            Files.writeString(path, dark ? "dark\n" : "light\n", StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // The live choice remains usable if this installation cannot save settings.
        }
    }

    private static void load() {
        try {
            dark = Files.readString(path(), StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT).equals("dark");
        } catch (IOException ignored) {
            dark = false;
        }
    }

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config/better-content-ui.theme");
    }
}
