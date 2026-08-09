package com.adaycounter.client;

import com.adaycounter.AestheticDayCounter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = AestheticDayCounter.MODID, value = Dist.CLIENT)
public final class ClientDayCounterHandler {

    // --- Tunables -----------------------------------------------------

    private static final int CHARS_PER_SECOND = 6;      // reveal speed
    private static final int HOLD_MS = 2500;              // how long text stays fully visible
    private static final int FADE_MS = 700;                // fade-out duration after the hold
    private static final int MILESTONE_INTERVAL = 100;     // special styling every N days
    private static final int START_DELAY_MS = 500;          // pause before entry

    // --- State ----------------------------------------------------------

    private static long lastSeenDay = Long.MIN_VALUE; // uninitialized sentinel
    private static String activeText = null;
    private static long triggeredAtMs = -1;
    private static int lastRevealedChars = -1;
    private static boolean isMilestone = false;
    private static boolean wasScreenOpen = false;
    private static long screenOpenedAtMs = -1;

    private ClientDayCounterHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
        
            lastSeenDay = Long.MIN_VALUE;
            return;
        }
        if (mc.isPaused()) {
            return;
        }

        if (mc.screen != null) {
            return;
        }

        long dayTime = mc.level.getDayTime();
        long currentDay = Math.floorDiv(dayTime, 24000L); // 0-indexed, matches vanilla day 0

        if (currentDay != lastSeenDay) {
            lastSeenDay = currentDay;
            long displayDay = currentDay + 1;
            triggerDisplay(displayDay);
        }
    }

    private static void triggerDisplay(long displayDay) {
        activeText = "-|  DAY " + displayDay + "  |-";
        triggeredAtMs = System.currentTimeMillis();
        lastRevealedChars = -1;
        isMilestone = (displayDay % MILESTONE_INTERVAL == 0);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.screen != null) {
            if (!wasScreenOpen) {
                wasScreenOpen = true;
                screenOpenedAtMs = System.currentTimeMillis();
            }
            return; // don't render or play sound while any menu is open
        }
        if (wasScreenOpen) {
            wasScreenOpen = false;
            long pausedDuration = System.currentTimeMillis() - screenOpenedAtMs;
            triggeredAtMs += pausedDuration; // shift the whole timeline forward by however long the menu was open
        }

        if (activeText == null) {
            return;
        }
        

        long elapsed = System.currentTimeMillis() - triggeredAtMs;

        if (elapsed < START_DELAY_MS) {
            return; // 
        }
        elapsed -= START_DELAY_MS; //    

        int totalChars = activeText.length();
        int revealMs = 1000 / CHARS_PER_SECOND;
        int revealedChars = Math.min(totalChars, (int) (elapsed / revealMs));

        
        if (revealedChars > lastRevealedChars && revealedChars < totalChars) {
            mc.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            SoundEvents.UI_BUTTON_CLICK.value(), 1.6F, 0.6F));
        }
        lastRevealedChars = revealedChars;

        long fullyVisibleAt = (long) totalChars * revealMs;
        long fadeStartAt = fullyVisibleAt + HOLD_MS;
        long fadeEndAt = fadeStartAt + FADE_MS;

        if (elapsed > fadeEndAt) {
            activeText = null;
            return;
        }

        float alpha = 1.0F;
        if (elapsed > fadeStartAt) {
            alpha = 1.0F - ((float) (elapsed - fadeStartAt) / FADE_MS);
            alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        }

        String shown = activeText.substring(0, revealedChars);

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();

        int color = isMilestone ? 0xFFD700 : 0xFFFFFF; // gold for milestones, white otherwise
        int alphaInt = (int) (alpha * 255);
        if (alphaInt < 4) {
      
            return;
        }
        int argb = (alphaInt << 24) | (color & 0xFFFFFF);

        Component text = Component.literal(shown);
        int textWidth = font.width(text);
        int x = (screenW - textWidth) / 2;
        int y = screenH - 55;

        // Slightly larger scale for milestone days?
        float scale = isMilestone ? 1.5F : 1.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, argb, true);
        graphics.pose().popPose();
    }
}
