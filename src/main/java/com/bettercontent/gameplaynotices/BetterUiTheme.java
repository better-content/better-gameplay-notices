package com.bettercontent.gameplaynotices;

/** Client-only, shared journal palette. The journal UI is dark mode only. */
public final class BetterUiTheme {
    private BetterUiTheme() {}

    /** The journal palette has a single mode, and it is dark. */
    public static boolean dark() { return true; }

    /** Always the dark variant; the light variant only remains in source history. */
    public static int color(int light, int night) { return night; }
}
