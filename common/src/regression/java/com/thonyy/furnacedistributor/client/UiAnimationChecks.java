package com.thonyy.furnacedistributor.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Exercises elapsed time and the real GUI extraction API without a window/GPU. */
public final class UiAnimationChecks {
    private static int checks;
    private static int wraps;
    static final Font FONT = new Font(new Font.Provider() {
        @Override public net.minecraft.client.gui.GlyphSource glyphs(net.minecraft.network.chat.FontDescription description) { return null; }
        @Override public net.minecraft.client.gui.font.glyphs.EffectGlyph effect() { return null; }
    }) {
        private final net.minecraft.client.StringSplitter splitter = new net.minecraft.client.StringSplitter((point, style) -> 6);
        @Override public List<FormattedCharSequence> split(FormattedText text, int width) {
            wraps++;
            return splitter.splitLines(text, width, Style.EMPTY).stream()
                    .map(net.minecraft.locale.Language.getInstance()::getVisualOrder).toList();
        }
        @Override public int width(FormattedCharSequence text) { return (int) splitter.stringWidth(text); }
    };

    public static int run() throws Exception {
        checks = 0;
        timing();
        extraction();
        return checks;
    }

    private static void timing() {
        UiTransition transition = new UiTransition();
        close(transition.update(1, 0, 180, true), 0, "entry starts transparent");
        close(transition.update(1, 90, 180, true), 0.875f, "ease-out cubic midpoint; target does not restart the clock");
        close(transition.value(180), 1, "entry duration");
        close(transition.update(0, 200, 240, false), 1, "exit begins at visible endpoint");
        close(transition.value(320), 0.5f, "ease-in-out cubic exit midpoint");
        close(transition.update(1, 320, 180, true), 0.5f, "interrupted exit reverses continuously");
        close(transition.value(410), 0.9375f, "reversed entry eases from actual opacity");
        close(transition.update(0, 410, 240, false), 0.9375f, "interrupted entry reverses continuously");
        close(transition.value(650), 0, "exit duration after reversal");
        for (int fps : new int[]{5, 30, 60, 144, 240}) {
            transition.snap(0);
            transition.update(1, 1000, 180, true);
            for (int elapsed = 0; elapsed < 180; elapsed += Math.max(1, 1000 / fps)) {
                float t = elapsed / 180f;
                close(transition.update(1, 1000 + elapsed, 180, true), 1 - (1 - t) * (1 - t) * (1 - t),
                        "elapsed-time entry at " + fps + " FPS");
            }
            close(transition.value(1180), 1, "same duration at every FPS");
        }
        for (int i = 0; i < 30; i++) {
            long now = 2000 + i * 20;
            float before = transition.value(now);
            close(transition.update(i % 2, now, 180, true), before, "rapid reversals do not jump");
        }
        close(transition.value(60000), 1, "large frame gap completes instead of sticking");
        close(transition.update(0, 60001, 0, false), 0, "disabled exit is immediate");
        close(transition.update(1, 60001, 0, true), 1, "disabled entry is immediate");
        transition.snap(0);
        close(transition.value(60002), 0, "suspension clears temporal state");
    }

    private static void extraction() throws Exception {
        Class<?> row = Class.forName(SelectionHud.class.getName() + "$Row");
        var constructor = row.getDeclaredConstructor(Component.class, int.class);
        constructor.setAccessible(true);
        Method panel = SelectionHud.class.getDeclaredMethod("panel", GuiGraphicsExtractor.class, Font.class,
                List.class, List.class, long.class, boolean.class);
        panel.setAccessible(true);
        var base = List.of(constructor.newInstance(Component.literal("8 furnaces").withStyle(style -> style.withBold(true)), 0xFFFF55),
                constructor.newInstance(Component.literal("R select / C collect"), 0xCCCCCC));
        var tips = List.of(constructor.newInstance(Component.literal("TIP"), 0x55FFFF),
                constructor.newInstance(Component.literal("Press [G] here"), 0xCCCCCC));
        var ticks = SelectionFeedback.class.getDeclaredField("tipTicks");
        ticks.setAccessible(true);
        SelectionHud.reset();
        try {
            check(frame(panel, base, List.of(), 0, true, 360, 240).texts.isEmpty(), "zero-alpha elements are not submitted");
            Frame halfway = frame(panel, base, List.of(), 90, true, 360, 240);
            close(halfway.texts.getFirst().pose.m21(), 0.75f, "six-pixel entry slide follows eased opacity");
            check(alpha(halfway.texts.getFirst()) == ARGB.alpha(ARGB.multiplyAlpha(0xFFFFFFFF, 0.875f)), "text uses actual ARGB alpha");
            check(halfway.rectangles.getFirst().col1() == ARGB.multiplyAlpha(0xB0202020, 0.875f), "backdrop preserves its own base alpha");
            check(ARGB.alpha(halfway.rectangles.get(1).col1()) == alpha(halfway.texts.getFirst()), "stripe and text fade consistently");
            int wrapped = wraps;
            Frame visible = frame(panel, base, List.of(), 180, true, 360, 240);
            check(wraps == wrapped, "unchanged content reuses wrapped text");
            close(visible.texts.getFirst().pose.m21(), 0, "final HUD position is unchanged");
            check(alpha(visible.texts.getFirst()) == 255, "entry ends fully opaque");
            close(halfway.texts.getFirst().pose.m21(), 0.75f, "submitted pose is immutable across later extractions");
            frame(panel, List.of(), List.of(), 200, true, 360, 240);
            Frame exiting = frame(panel, List.of(), List.of(), 320, true, 360, 240);
            var changed = List.of(constructor.newInstance(Component.literal("64 furnaces"), 0xCC88FF));
            Frame reversed = frame(panel, changed, List.of(), 320, true, 360, 240);
            check(alpha(reversed.texts.getFirst()) == alpha(exiting.texts.getFirst()), "HUD reversal preserves submitted opacity");
            check(text(reversed.texts.getFirst()).equals("64 furnaces"), "count changes immediately to the exact new value");
            var backdrop = reversed.rectangles.getFirst();
            check(Math.max(backdrop.x0(), backdrop.x1()) >= FONT.width((FormattedCharSequence) textField(reversed.texts.getFirst())) + 16,
                    "background immediately accommodates an increased exact count");
            frame(panel, changed, List.of(), 500, true, 360, 240);
            frame(panel, List.of(), List.of(), 510, true, 360, 240);
            check(frame(panel, List.of(), List.of(), 750, true, 360, 240).texts.isEmpty(), "completed exit submits nothing");
            var held = SelectionHud.class.getDeclaredField("heldRows");
            held.setAccessible(true);
            check(((List<?>) held.get(null)).isEmpty(), "completed exit releases content");
            SelectionHud.reset();
            ticks.setInt(null, 100);
            frame(panel, base, tips, 1000, true, 360, 240);
            Frame tipVisible = frame(panel, base, tips, 1180, true, 360, 240);
            check(tipVisible.texts.size() == 4 && alpha(tipVisible.texts.get(2)) == 255, "tip enters inside the same panel");
            check(tipVisible.texts.get(2).scissor.height() <= tipVisible.texts.getFirst().scissor.height(),
                    "tip reveal is clipped to its shared backdrop");
            ticks.setInt(null, 5);
            frame(panel, base, tips, 5750, true, 360, 240);
            Frame tipExit = frame(panel, base, tips, 5875, true, 360, 240);
            check(alpha(tipExit.texts.getFirst()) == 255 && alpha(tipExit.texts.get(2)) >= 127
                    && alpha(tipExit.texts.get(2)) <= 128, "tip fades independently during its final 250 ms");
            close(tipExit.texts.get(2).pose.m21(), 1.5f, "tip slide remains subtle");
            ticks.setInt(null, 0);
            check(frame(panel, base, List.of(), 6000, true, 360, 240).texts.size() == 2, "expired/invalid tip leaves no stale text");
            ticks.setInt(null, 100);
            frame(panel, base, tips, 6010, true, 360, 240);
            frame(panel, base, tips, 6200, true, 360, 240);
            check(frame(panel, base, List.of(), 6201, true, 360, 240).texts.size() == 2, "collecting hides even an opaque tip immediately");
            SelectionHud.reset();
            ticks.setInt(null, 5);
            Frame disabled = frame(panel, base, tips, 0, false, 160, 240);
            for (GuiTextRenderState text : disabled.texts) {
                check(alpha(text) == 255, "disabled animations submit fully opaque final states, including tips");
                close(text.pose.m21(), 0, "disabled animations have no slide");
            }
            wrapped = wraps;
            Frame resized = frame(panel, base, tips, 1, true, 360, 240);
            check(wraps > wrapped, "GUI scale/resolution change invalidates wrapping");
            backdrop = resized.rectangles.getFirst();
            check(Math.max(backdrop.x0(), backdrop.x1()) <= 356 && Math.max(backdrop.y0(), backdrop.y1()) <= 236,
                    "resized panel stays within current GUI viewport");
            check(frame(panel, List.of(), List.of(), 2, false, 360, 240).texts.isEmpty(), "disabled HUD disappears immediately");
            SelectionHud.reset();
            check(frame(panel, base, List.of(), 60000, true, 360, 240).texts.isEmpty(), "resuming a cleared session starts a fresh entry");
        } finally {
            ticks.setInt(null, 0);
            SelectionHud.reset();
        }
    }

    private static Frame frame(Method panel, List<?> rows, List<?> tips, long now, boolean animated, int width, int height) throws Exception {
        Frame state = new Frame();
        // Vanilla's constructor requires GPU atlases; initialize just the real extraction fields used here.
        var access = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        access.setAccessible(true);
        Graphics graphics = (Graphics) ((sun.misc.Unsafe) access.get(null)).allocateInstance(Graphics.class);
        graphics.width = width;
        graphics.height = height;
        set(graphics, "pose", new org.joml.Matrix3x2fStack(16));
        set(graphics, "guiRenderState", state);
        var scissors = Class.forName(GuiGraphicsExtractor.class.getName() + "$ScissorStack")
                .getDeclaredConstructor(net.minecraft.client.gui.navigation.ScreenRectangle.class);
        scissors.setAccessible(true);
        set(graphics, "scissorStack", scissors.newInstance(new net.minecraft.client.gui.navigation.ScreenRectangle(0, 0, width, height)));
        panel.invoke(null, graphics, FONT, rows, tips, now, animated);
        close(graphics.pose().m21(), 0, "extraction restores the matrix stack");
        check(graphics.containsPointInScissor(0, 0), "extraction restores the scissor stack");
        return state;
    }

    private static void set(Graphics graphics, String name, Object value) throws Exception {
        var field = GuiGraphicsExtractor.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(graphics, value);
    }

    private static final class Graphics extends GuiGraphicsExtractor {
        int width, height;
        private Graphics() { super(null, null, 0, 0); }
        @Override public int guiWidth() { return width; }
        @Override public int guiHeight() { return height; }
    }

    private static int alpha(GuiTextRenderState text) throws Exception {
        var field = GuiTextRenderState.class.getDeclaredField("color");
        field.setAccessible(true);
        return ARGB.alpha(field.getInt(text));
    }

    private static String text(GuiTextRenderState text) throws Exception {
        StringBuilder contents = new StringBuilder();
        ((FormattedCharSequence) textField(text)).accept((index, style, point) -> { contents.appendCodePoint(point); return true; });
        return contents.toString();
    }

    private static Object textField(GuiTextRenderState text) throws Exception {
        var field = GuiTextRenderState.class.getDeclaredField("text");
        field.setAccessible(true);
        return field.get(text);
    }

    private static final class Frame extends GuiRenderState {
        final List<GuiTextRenderState> texts = new ArrayList<>();
        final List<ColoredRectangleRenderState> rectangles = new ArrayList<>();
        @Override public void addText(GuiTextRenderState text) { texts.add(text); }
        @Override public void addGuiElement(GuiElementRenderState element) { rectangles.add((ColoredRectangleRenderState) element); }
    }

    private static void close(float value, float expected, String message) { check(Math.abs(value - expected) < 0.0001f, message); }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private UiAnimationChecks() { }
}
