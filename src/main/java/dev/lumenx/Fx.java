package dev.lumenx;

import net.minecraft.client.gui.DrawContext;

/** Мелкие 2D-примитивы для HUD-оверлея (светящиеся точки и линии). */
final class Fx {
    private Fx() { }

    static void fill(DrawContext c, int x1, int y1, int x2, int y2, int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
        if (a == 0) return;
        c.fill(x1, y1, x2, y2, (a << 24) | (rgb & 0xFFFFFF));
    }

    /** Точка с мягким свечением (три слоя с разной прозрачностью). */
    static void glowDot(DrawContext c, float x, float y, int rgb, float alpha, int r) {
        int ix = Math.round(x), iy = Math.round(y);
        fill(c, ix - r - 2, iy - r - 2, ix + r + 3, iy + r + 3, rgb, alpha * 0.10f);
        fill(c, ix - r - 1, iy - r - 1, ix + r + 2, iy + r + 2, rgb, alpha * 0.22f);
        fill(c, ix - r, iy - r, ix + r + 1, iy + r + 1, rgb, alpha * 0.90f);
    }

    /** Светящаяся линия из точек с шагом ~2 px. */
    static void glowLine(DrawContext c, float x1, float y1, float x2, float y2,
                         int rgb, float alpha, int r) {
        float dx = x2 - x1, dy = y2 - y1;
        int n = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / 2f));
        if (n > 200) return; // защита от «улетевших» сегментов у края камеры
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            glowDot(c, x1 + dx * t, y1 + dy * t, rgb, alpha, r);
        }
    }

    /** Тонкая линия из точек 2x2 без свечения (дёшево, для кругов и разметки). */
    static void thinLine(DrawContext c, float x1, float y1, float x2, float y2, int rgb, float alpha) {
        float dx = x2 - x1, dy = y2 - y1;
        int n = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / 1.5f));
        if (n > 300) return;
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            int x = Math.round(x1 + dx * t), y = Math.round(y1 + dy * t);
            fill(c, x, y, x + 2, y + 2, rgb, alpha);
        }
    }
}
