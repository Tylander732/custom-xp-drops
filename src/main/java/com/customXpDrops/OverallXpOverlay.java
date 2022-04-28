package com.customXpDrops;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.text.DecimalFormat;

public class OverallXpOverlay extends Overlay {

    protected static final float FRAMES_PER_SECOND = 50;
    protected static final String pattern = "###,###,###";
    protected static final DecimalFormat xpFormatter = new DecimalFormat(pattern);
    protected static final float CONSTANT_FRAME_TIME = 1000.0f / FRAMES_PER_SECOND;

    protected CustomXpDropsPlugin plugin;
    protected XpDropsConfig config;

    protected String lastFont = "";
    protected int lastFontSize = 0;
    protected boolean useRunescapeFont = true;
    protected XpDropsConfig.FontStyle lastFontStyle = XpDropsConfig.FontStyle.DEFAULT;
    protected Font font = null;
    protected boolean firstRender = true;
    protected long lastFrameTime = 0;

    @Inject
    private Client client;

    @Inject
    protected OverallXpOverlay(CustomXpDropsPlugin plugin, XpDropsConfig config) {
        this.plugin = plugin;
        this.config = config;
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPosition(OverlayPosition.TOP_RIGHT);
    }

    /**
     * Font provided by config menu item
     * @param graphics
     */
    protected void handleFont(Graphics2D graphics) {
        if(font != null) {
            graphics.setFont(font);
            if(useRunescapeFont) {
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            }
        }
    }

    protected void lazyInit() {
        
    }

    //TODO: Remove default osrs XP display widget function

    //TODO: Get overall XP to display within rendered area

    //TODO: Add config modifications for Overall XP - Size, fade out delay

    //TODO: Update() function for continuously grabbing new Overall XP Value

    @Override
    public Dimension render(Graphics2D graphics) {
        return null;
    }
}
