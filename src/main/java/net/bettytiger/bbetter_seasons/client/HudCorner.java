package net.bettytiger.bbetter_seasons.client;

/** Screen corner the date HUD anchors to. */
public enum HudCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT;

    public boolean isTop() {
        return this == TOP_LEFT || this == TOP_RIGHT;
    }

    public boolean isLeft() {
        return this == TOP_LEFT || this == BOTTOM_LEFT;
    }
}
