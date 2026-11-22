package net.iann.legendaryallays.client.wheel;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.iann.legendaryallays.registry.ModItems;
import net.iann.legendaryallays.network.LuteCommandPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/** A HUD overlay leaves keyboard movement under normal game control. */
public final class CommandWheel {
    private static final WheelCommand[] MAIN_COMMANDS = {
        WheelCommand.BIND, WheelCommand.UNBIND, WheelCommand.COLLECTION,
        WheelCommand.FILTER_ADD, WheelCommand.FILTER_CLEAR
    };
    private static final WheelCommand[] COLLECTION_COMMANDS = {
        WheelCommand.COLLECT_DROPPED, WheelCommand.COLLECT_NEARBY,
        WheelCommand.COLLECT_EVERYWHERE, WheelCommand.IDLE
    };
    private final Minecraft mc = Minecraft.getInstance();
    private boolean active;
    private boolean collectionMenu;
    private boolean swallowLeft;
    private boolean swallowRight;

    private boolean canOpen() {
        return mc.player != null && mc.level != null && mc.player.isAlive()
                && !mc.player.isSpectator() && mc.screen == null && mc.getOverlay() == null
                && mc.isWindowActive() && mc.player.getMainHandItem().is(ModItems.LUTE.get());
    }

    @SubscribeEvent
    public void mouseButton(InputEvent.MouseButton.Pre event) {
        int button = event.getButton();
        boolean pressed = event.getAction() == GLFW.GLFW_PRESS;
        if ((button == 0 && swallowLeft) || (button == 1 && swallowRight)) {
            event.setCanceled(true);
            if (!pressed) {
                if (button == 0) swallowLeft = false;
                else swallowRight = false;
            }
            return;
        }
        if (active) {
            event.setCanceled(true);
            if (!pressed) return;
            if (button == 0) {
                swallowLeft = true;
                int selected = selection();
                if (!canOpen()) {
                    close(true);
                } else if (selected < 0) {
                    back();
                } else {
                    WheelCommand command = commands()[selected];
                    if (command == WheelCommand.COLLECTION) {
                        collectionMenu = true;
                    } else {
                        PacketDistributor.sendToServer(new LuteCommandPayload(command.serverCommand()));
                        close(true);
                    }
                }
            } else if (button == 1) {
                swallowRight = true;
                back();
            }
        } else if (button == 1 && pressed && canOpen()) {
            event.setCanceled(true);
            swallowRight = true;
            active = true;
            collectionMenu = false;
            clearActions();
            if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
            mc.mouseHandler.releaseMouse();
        }
    }

    @SubscribeEvent
    public void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (active || (event.isAttack() && swallowLeft) || (event.isUseItem() && swallowRight)) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public void tick(ClientTickEvent.Pre event) {
        if (active && (!canOpen() || mc.mouseHandler.isMouseGrabbed())) {
            close(mc.screen == null && mc.getOverlay() == null && mc.isWindowActive());
        }
        if (active || swallowLeft || swallowRight) clearActions();
        // A release can be missed if the window loses focus.
        long window = mc.getWindow().getWindow();
        if (GLFW.glfwGetMouseButton(window, 0) == GLFW.GLFW_RELEASE) swallowLeft = false;
        if (GLFW.glfwGetMouseButton(window, 1) == GLFW.GLFW_RELEASE) swallowRight = false;
    }

    @SubscribeEvent
    public void openingScreen(ScreenEvent.Opening event) {
        if (!active) return;
        boolean escape = event.getNewScreen() instanceof PauseScreen;
        if (escape && collectionMenu) {
            event.setCanceled(true);
            back();
            return;
        }
        close(false);
        if (escape) {
            event.setCanceled(true);
            mc.execute(() -> {
                if (mc.screen == null && mc.isWindowActive()) mc.mouseHandler.grabMouse();
            });
        }
    }

    private WheelCommand[] commands() {
        return collectionMenu ? COLLECTION_COMMANDS : MAIN_COMMANDS;
    }

    private void back() {
        if (collectionMenu) {
            collectionMenu = false;
            clearActions();
        } else {
            close(true);
        }
    }

    private void close(boolean captureMouse) {
        active = false;
        collectionMenu = false;
        if (captureMouse) mc.mouseHandler.grabMouse();
        clearActions();
    }

    private void clearActions() {
        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);
        mc.options.keyPickItem.setDown(false);
        while (mc.options.keyAttack.consumeClick()) {}
        while (mc.options.keyUse.consumeClick()) {}
        while (mc.options.keyPickItem.consumeClick()) {}
    }

    private int selection() {
        var window = mc.getWindow();
        double scale = WheelLayout.scale(window.getGuiScaledWidth(), window.getGuiScaledHeight());
        double x = mc.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
        double y = mc.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
        return WheelLayout.selection((x - window.getGuiScaledWidth() / 2.0) / scale,
                (y - window.getGuiScaledHeight() / 2.0) / scale, commands().length);
    }

    public void render(GuiGraphics gui, DeltaTracker delta) {
        if (!active || !canOpen()) return;
        WheelCommand[] commands = commands();
        double sectorAngle = WheelLayout.step(commands.length);
        int selected = selection();
        float scale = (float) WheelLayout.scale(gui.guiWidth(), gui.guiHeight());
        gui.pose().pushPose();
        gui.pose().translate(gui.guiWidth() / 2.0, gui.guiHeight() / 2.0, 0);
        gui.pose().scale(scale, scale, 1);
        gui.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = gui.pose().last().pose();
        for (int i = 0; i < commands.length; i++) {
            double start = -Math.PI / 2 + (i - 0.5) * sectorAngle + 0.025;
            double end = start + sectorAngle - 0.05;
            int color = i == selected ? (commands[i].color & 0xFFFFFF) | 0xC0000000 : 0xD0182435;
            for (int step = 0; step < 16; step++) {
                double a = start + (end - start) * step / 16;
                double b = start + (end - start) * (step + 1) / 16;
                vertex(buffer, matrix, a, WheelLayout.INNER, color);
                vertex(buffer, matrix, b, WheelLayout.INNER, color);
                vertex(buffer, matrix, b, WheelLayout.OUTER, color);
                vertex(buffer, matrix, a, WheelLayout.OUTER, color);
            }
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
        for (int i = 0; i < commands.length; i++) {
            double angle = -Math.PI / 2 + i * sectorAngle;
            int x = (int) (Math.cos(angle) * 64);
            int y = (int) (Math.sin(angle) * 64);
            gui.renderItem(commands[i].icon, x - 8, y - 10);
            gui.drawCenteredString(mc.font, Integer.toString(i + 1), x, y + 9, commands[i].color);
        }
        gui.drawCenteredString(mc.font, Component.translatable(collectionMenu ? "wheel.legendaryallays.collection" : "wheel.legendaryallays.title"), 0, -125, 0xFFE9F4FF);
        Component label = Component.translatable(selected < 0 ? "wheel.legendaryallays.choose" : commands[selected].key);
        gui.drawCenteredString(mc.font, label, 0, -111, selected < 0 ? 0xFFBBCBDD : commands[selected].color);
        gui.drawCenteredString(mc.font, Component.translatable("wheel.legendaryallays.confirm"), 0, 104, 0xFFE9F4FF);
        gui.drawCenteredString(mc.font, Component.translatable(collectionMenu
                ? "wheel.legendaryallays.back" : "wheel.legendaryallays.cancel"), 0, 117, 0xFFBBCBDD);
        gui.drawCenteredString(mc.font, Component.translatable(collectionMenu
                ? "wheel.legendaryallays.center_back" : "wheel.legendaryallays.center_close"), 0, -4, 0xFFBBCBDD);
        gui.pose().popPose();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, double angle, double radius, int color) {
        buffer.addVertex(matrix, (float) (Math.cos(angle) * radius), (float) (Math.sin(angle) * radius), 0).setColor(color);
    }
}
