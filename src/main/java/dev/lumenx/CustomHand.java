package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Custom hand: свободное положение / поворот / размер руки и 20+ вариантов удара.
 * Вызывается из HeldItemRendererMixin. Если стиль удара = Off, удар рисует модуль Swing или ваниль.
 */
public final class CustomHand {
    static final String[] STYLES = {
            "Off", "Smooth", "Slide", "Block", "Spin", "Poke", "Whip", "Tap", "Hammer", "Wave",
            "Dash", "Stab", "Drop", "Punch", "Wiggle", "Spiral", "Chop", "Uppercut", "Pendulum",
            "Flip", "Corkscrew", "No swing"
    };

    public static final Module M = new Module("Custom hand",
            "Move, rotate and scale your hand, pick a swing style", Category.VISUALS, false, true);
    // основная рука (id как раньше, чтобы сохранились ваши значения)
    static final Setting MAIN_ON = M.add(Setting.bool("main_on", "Main hand", true));
    static final Setting X      = M.add(Setting.num("x", "Main: Offset X", 0, -1, 1, 0.05));
    static final Setting Y      = M.add(Setting.num("y", "Main: Offset Y", 0, -1, 1, 0.05));
    static final Setting Z      = M.add(Setting.num("z", "Main: Offset Z", 0, -1, 1, 0.05));
    static final Setting ROT_X  = M.add(Setting.num("rot_x", "Main: Rotate X", 0, -180, 180, 5));
    static final Setting ROT_Y  = M.add(Setting.num("rot_y", "Main: Rotate Y", 0, -180, 180, 5));
    static final Setting ROT_Z  = M.add(Setting.num("rot_z", "Main: Rotate Z", 0, -180, 180, 5));
    static final Setting SCALE  = M.add(Setting.num("scale", "Main: Scale", 1, 0.3, 1.8, 0.05));
    // вторая рука — свои отдельные значения, по умолчанию выключена (ваниль)
    static final Setting OFF_ON = M.add(Setting.bool("off_on", "Off hand", false));
    static final Setting OX     = M.add(Setting.num("off_x", "Off: Offset X", 0, -1, 1, 0.05));
    static final Setting OY     = M.add(Setting.num("off_y", "Off: Offset Y", 0, -1, 1, 0.05));
    static final Setting OZ     = M.add(Setting.num("off_z", "Off: Offset Z", 0, -1, 1, 0.05));
    static final Setting OROT_X = M.add(Setting.num("off_rot_x", "Off: Rotate X", 0, -180, 180, 5));
    static final Setting OROT_Y = M.add(Setting.num("off_rot_y", "Off: Rotate Y", 0, -180, 180, 5));
    static final Setting OROT_Z = M.add(Setting.num("off_rot_z", "Off: Rotate Z", 0, -180, 180, 5));
    static final Setting OSCALE = M.add(Setting.num("off_scale", "Off: Scale", 1, 0.3, 1.8, 0.05));
    static final Setting NO_DIP = M.add(Setting.bool("no_dip", "No hit dip", true));
    static final Setting STYLE  = M.add(Setting.mode("style", "Swing style", 0, STYLES));
    static final Setting POWER  = M.add(Setting.num("power", "Swing strength", 1, 0.2, 2, 0.1));
    static final Setting SPEED  = M.add(Setting.num("speed", "Swing speed", 1, 1, 3, 0.25));

    private CustomHand() { }

    private static boolean isMain(Arm arm) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return arm == mc.options.getMainArm().getValue();
    }

    /** Включена ли кастомизация для этой руки. */
    private static boolean active(Arm arm) {
        return isMain(arm) ? MAIN_ON.bool : OFF_ON.bool;
    }

    // ---------- маленькие помощники ----------
    private static void rx(MatrixStack m, float d) { if (d != 0f) m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(d)); }
    private static void ry(MatrixStack m, float d) { if (d != 0f) m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(d)); }
    private static void rz(MatrixStack m, float d) { if (d != 0f) m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(d)); }
    private static void tr(MatrixStack m, float x, float y, float z) { m.translate(x, y, z); }
    private static float seg(float p, float a, float b) { return MathHelper.clamp((p - a) / (b - a), 0f, 1f); }
    private static float ease(float t) { return t * t * (3f - 2f * t); }

    /** После ванильного equip-смещения: добавляем своё положение руки. */
    public static void applyOffset(MatrixStack m, Arm arm, float equipProgress) {
        if (!M.enabled || !active(arm)) return;
        // Ваниль опускает руку после каждого удара (перезарядка) и прячет анимацию —
        // компенсируем это смещение (оно чисто вертикальное).
        if (NO_DIP.bool) m.translate(0f, 0.6f * equipProgress, 0f);

        boolean main = isMain(arm);
        float side = arm == Arm.RIGHT ? 1f : -1f;
        float x  = (float) (main ? X.num : OX.num);
        float y  = (float) (main ? Y.num : OY.num);
        float z  = (float) (main ? Z.num : OZ.num);
        float rx = (float) (main ? ROT_X.num : OROT_X.num);
        float ry = (float) (main ? ROT_Y.num : OROT_Y.num);
        float rz = (float) (main ? ROT_Z.num : OROT_Z.num);
        float s  = (float) (main ? SCALE.num : OSCALE.num);

        m.translate(side * x, y, z);
        rx(m, rx);
        ry(m, side * ry);
        rz(m, side * rz);
        if (s != 1f) m.scale(s, s, s);
    }

    /** @return true, если удар нарисован нами и ванильную/Lumen-анимацию надо отменить. */
    public static boolean applySwing(MatrixStack m, Arm arm, float rawP) {
        if (!M.enabled || STYLE.mode == 0 || !active(arm)) return false;
        float i = arm == Arm.RIGHT ? 1f : -1f;
        float k = (float) POWER.num;
        float pi = (float) Math.PI;

        // 1) Убираем ванильный сдвиг руки при ударе: он применяется до equip-смещения,
        //    а оба смещения — чистые переводы, поэтому компенсация точная.
        float vs = MathHelper.sin(MathHelper.sqrt(rawP) * pi);
        m.translate(i * 0.4f * vs,
                -0.2f * MathHelper.sin(MathHelper.sqrt(rawP) * pi * 2f),
                0.2f * MathHelper.sin(rawP * pi));

        // 2) Скорость: ускоряем анимацию, после конца рука стоит в покое
        float p = Math.min(1f, rawP * (float) SPEED.num);

        float sp = MathHelper.sin(p * pi);                    // 0 -> 1 -> 0
        float s2 = MathHelper.sin(MathHelper.sqrt(p) * pi);   // резкий старт
        float sq = MathHelper.sin(p * p * pi);                // мягкий старт
        float sw = MathHelper.sin(p * 2f * pi);               // туда-обратно
        float sm = ease(p);                                   // 0 -> 1 плавно

        String name = STYLES[Math.max(0, Math.min(STYLES.length - 1, STYLE.mode))];
        switch (name) {
            case "Smooth" -> {
                ry(m, i * (45f - 12f * sq * k));
                rz(m, i * -14f * s2 * k);
                rx(m, -55f * s2 * k);
                ry(m, i * -45f);
            }
            case "Slide" -> tr(m, 0f, 0f, -0.65f * sp * k);
            case "Block" -> {
                tr(m, i * -0.30f * sp * k, 0.10f * sp * k, -0.05f * sp * k);
                rz(m, i * 30f * sp * k);
                rx(m, -20f * sp * k);
                ry(m, i * -25f * sp * k);
            }
            case "Spin" -> {
                tr(m, 0f, 0f, -0.2f * sp * k);
                ry(m, i * 360f * sm);
            }
            case "Poke" -> {
                tr(m, 0f, 0f, -1.0f * s2 * k);
                rx(m, -10f * s2 * k);
            }
            case "Whip" -> {
                ry(m, i * 45f);
                rx(m, -35f * s2 * k);
                rz(m, i * -95f * sp * k);
                ry(m, i * -45f);
            }
            case "Tap" -> {
                float t = sp * sp;
                tr(m, 0f, -0.12f * t * k, -0.18f * t * k);
                rx(m, -40f * t * k);
            }
            case "Hammer" -> {
                // подъём -> удар -> возврат
                float ang = 55f * ease(seg(p, 0f, 0.3f))
                        - 155f * ease(seg(p, 0.3f, 0.5f))
                        + 100f * ease(seg(p, 0.5f, 1f));
                rx(m, ang * k);
            }
            case "Wave" -> {
                tr(m, i * 0.12f * sw * k, 0f, 0f);
                rz(m, i * 35f * sw * k);
                rx(m, -20f * MathHelper.sin(4f * pi * p) * k);
            }
            case "Dash" -> {
                tr(m, i * -0.4f * sp * k, -0.25f * sp * k, -0.7f * sp * k);
                rx(m, -20f * sp * k);
            }
            case "Stab" -> {
                tr(m, 0f, 0f, -0.7f * sp * k);
                rx(m, -85f * sp * k);
            }
            case "Drop" -> {
                tr(m, 0f, -0.75f * sp * k, 0f);
                rx(m, 25f * sp * k);
            }
            case "Punch" -> {
                tr(m, i * -0.12f * s2 * k, 0.05f * s2 * k, -0.85f * s2 * k);
                rz(m, i * 8f * s2 * k);
                rx(m, -6f * s2 * k);
            }
            case "Wiggle" -> {
                rz(m, i * 18f * MathHelper.sin(6f * pi * p) * sp * k);
                rx(m, 9f * MathHelper.sin(8f * pi * p) * sp * k);
            }
            case "Spiral" -> {
                tr(m, 0f, 0f, -0.35f * sp * k);
                ry(m, i * 360f * sm);
                rx(m, -30f * sp * k);
            }
            case "Chop" -> {
                ry(m, i * 45f);
                rx(m, -110f * s2 * k);
                ry(m, i * -45f);
                tr(m, 0f, 0f, -0.12f * sp * k);
            }
            case "Uppercut" -> {
                ry(m, i * 45f);
                rx(m, 75f * s2 * k);
                rz(m, i * -25f * sp * k);
                ry(m, i * -45f);
                tr(m, 0f, -0.15f * sp * k, 0f);
            }
            case "Pendulum" -> {
                rz(m, i * -55f * sw * k);
                ry(m, i * 15f * sw * k);
            }
            case "Flip" -> rx(m, -360f * sm);
            case "Corkscrew" -> {
                tr(m, 0f, 0f, -0.25f * sp * k);
                rz(m, i * 360f * sm);
            }
            default -> { /* No swing: просто без анимации */ }
        }
        return true;
    }
}
