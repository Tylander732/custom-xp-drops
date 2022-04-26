package com.customXpDrops;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.awt.*;

@Slf4j
public class XpDropOverlay extends Overlay {
    protected static final int RED_HIT_SPLAT_SPRITE_ID = 1359;
    protected static final float FRAMES_PER_SECOND = 50;
    protected static final String pattern = "###,###,###";
    protected static final DecimalFormat xpFormatter = new DecimalFormat(pattern);
    protected static final ArrayList<XpDropInFlight> xpDropsInFlight = new ArrayList<>();
    protected static final BufferedImage[] STAT_ICONS = new BufferedImage[Skill.values().length - 1];

    //TODO: What do the order of these indices and priority matter?
    protected static final int[] SKILL_INDICES = new int[] {10, 0, 2, 4, 6, 1, 3, 5, 16, 15, 17, 12, 20, 14, 13, 7, 11, 8, 9, 18, 19, 22, 21};
    protected static final int[] SKILL_PRIORITY = new int[] {1, 5, 2, 6, 3, 7, 4, 15, 17, 18, 0, 16, 11, 14, 13, 9, 8, 10, 19, 20, 12, 22, 21};
    protected static BufferedImage FAKE_SKILL_ICON;
    protected static BufferedImage HITSPLAT_ICON;
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

    //Constructor for XpDropOverlay, default to top right and to appear layer above widgets
    @Inject
    protected XpDropOverlay(CustomXpDropsPlugin plugin, XpDropsConfig config) {
        this.plugin = plugin;
        this.config = config;
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPosition(OverlayPosition.TOP_RIGHT);
    }

    //Loop through all the skills in the game to get their icons to be used
    protected void initIcons() {
        for(int i = 0; i < STAT_ICONS.length; i++) {
            STAT_ICONS[i] = plugin.getSkillIcon(Skill.values()[i]);
        }
        FAKE_SKILL_ICON = plugin.getIcon(423, 11);
        HITSPLAT_ICON = plugin.getIcon(RED_HIT_SPLAT_SPRITE_ID, 0);
    }

    //TODO: Documentation
    protected void handleFont(Graphics2D graphics) {
        if(font != null) {
            graphics.setFont(font);
            if(useRunescapeFont) {
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            }
        }
    }

    protected void lazyInit() {
        if(firstRender) {
            firstRender = false;
            initIcons();
        }
        if(lastFrameTime <= 0) {
            lastFrameTime = System.currentTimeMillis() - 20;
        }
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        lazyInit();
        update();

        if(config.attachToPlayer()) {
            setPosition(OverlayPosition.DYNAMIC);
            setLayer(OverlayLayer.ABOVE_WIDGETS);

            if(client.getLocalPlayer() == null) {
                return null;
            }

            drawAttachedXpDrops(graphics);
        }
        else {
            setLayer(OverlayLayer.ABOVE_WIDGETS);
            setPosition(OverlayPosition.TOP_RIGHT);

            drawXpDrops(graphics);

            FontMetrics fontMetrics = graphics.getFontMetrics();

            int width = fontMetrics.stringWidth(pattern);

            int height = fontMetrics.getHeight();
            height += Math.abs(config.framesPerDrop() * config.yPixelsPerSecond() / FRAMES_PER_SECOND);

            lastFrameTime = System.currentTimeMillis();
            return new Dimension(width, height);
        }

        lastFrameTime = System.currentTimeMillis();
        return null;
    }

    protected Point getCanvasTextLocation(Graphics2D graphics, Actor actor){
        //TODO: what is the value of 140 representing?
        int zOffset = Math.min(actor.getLogicalHeight(), 140);
        return actor.getCanvasTextLocation(graphics, "x", zOffset);
    }

    //TODO: Learn more about the graphics class
    protected void drawAttachedXpDrops(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        handleFont(graphics);

        for(XpDropInFlight xpDropInFlight : xpDropsInFlight) {
            if (xpDropInFlight.frame < 0) {
                continue;
            }
            String text = getDropText(xpDropInFlight);

            //Attach xp drops to the player
            Actor target = client.getLocalPlayer();

            Point point = getCanvasTextLocation(graphics, target);
            float xStart = xpDropInFlight.xOffset;
            float yStart = xpDropInFlight.yOffset;

            //String width of xp Drop helps determine x location
            int x = (int) (xStart + point.getX() - (graphics.getFontMetrics().stringWidth(text) / 2.0f));
            int y = (int) (yStart + point.getY());

            Color color = getColor(xpDropInFlight);
            Color backgroundColor = new Color(0,0,0, (int)xpDropInFlight.alpha);
            Color color1 = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)xpDropInFlight.alpha);
            graphics.setColor(backgroundColor);
            graphics.drawString(text, x + 1, y + 1);
            graphics.setColor(color1);
            graphics.drawString(text, x, y);

            int imageX = x - 2;
            int imageY = y - graphics.getFontMetrics().getMaxAscent();
            drawIcons(graphics, xpDropInFlight.icons, imageX, imageY, xpDropInFlight.alpha);
        }
    }

    protected void drawXpDrops(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        handleFont(graphics);

        int width = graphics.getFontMetrics().stringWidth(pattern);
        int height = graphics.getFontMetrics().getHeight();
        int totalHeight = height + (int) Math.abs(config.framesPerDrop() * config.yPixelsPerSecond() / FRAMES_PER_SECOND);

        for(XpDropInFlight xpDropInFlight : xpDropsInFlight) {
            if (xpDropInFlight.frame < 0) {
                continue;
            }
            String text = getDropText(xpDropInFlight);

            float xStart = xpDropInFlight.xOffset;
            float yStart = xpDropInFlight.yOffset;

            int textY;
            if(config.yDirection() == XpDropsConfig.VerticalDirection.DOWN) {
                textY = (int) (yStart + graphics.getFontMetrics().getMaxAscent());
            } else {
                textY = (int) (totalHeight + yStart + graphics.getFontMetrics().getMaxAscent() - graphics.getFontMetrics().getHeight());
            }

            int textX = (int) (width + xStart - graphics.getFontMetrics().stringWidth(text));
            drawText(graphics, text, textX, textY, xpDropInFlight);
            int imageX = textX - 2;
            int imageY = textY - graphics.getFontMetrics().getMaxAscent();
            drawIcons(graphics, xpDropInFlight.icons, imageX, imageY, xpDropInFlight.alpha);
        }
    }

    protected void drawText(Graphics2D graphics, String text, int textX, int textY, XpDropInFlight xpDropInFlight) {
        Color _color = getColor(xpDropInFlight);
        Color backgroundColor = new Color(0,0,0, (int) xpDropInFlight.alpha);
        Color color = new Color(_color.getRed(), _color.getGreen(), _color.getBlue(), (int) xpDropInFlight.alpha);
        graphics.setColor(backgroundColor);
        graphics.drawString(text, textX + 1, textY + 1);
        graphics.setColor(color);
        graphics.drawString(text, textX, textY);
    }

    protected int drawIcons(Graphics2D graphics, int icons, int x, int y, float alpha) {
        int width = 0;
        int iconSize = graphics.getFontMetrics().getHeight();
        if(config.showIcons()) {
            for(int i = SKILL_INDICES.length - 1; i >= 0; i--) {
                //TODO: learn about this assignment
                int icon = (icons >> i) & 0x1;
                if(icon == 0x1) {
                    int index = SKILL_INDICES[i];
                    BufferedImage image = STAT_ICONS[index];
                    int _iconSize = Math.max(iconSize, 18);
                    int iconWidth = image.getWidth() * _iconSize / 25;
                    int iconHeight = image.getHeight() * _iconSize / 25;
                    Dimension dimension = drawIcon(graphics, image, x, y, iconWidth, iconHeight, alpha / 0xff);
                    width += dimension.getWidth() + 2;
                }
            }
            int hitsplatIcon = (icons >> 24) & 0x1;
            if(hitsplatIcon == 0x1) {
                BufferedImage image = HITSPLAT_ICON;
                int _iconSize = Math.max(iconSize - 4, 14);
                Dimension dimension = drawIcon(graphics, image, x, y, _iconSize, _iconSize, alpha / 0xff);
            }
        }
        return width;
    }

    private Dimension drawIcon(Graphics2D graphics, BufferedImage image, int x, int y, int width, int height, float alpha) {
        int yOffset = graphics.getFontMetrics().getHeight() / 2 - height / 2;

        Composite composite = graphics.getComposite();
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        graphics.drawImage(image, x, y + yOffset, width, height, null);
        graphics.setComposite(composite);
        return new Dimension(width, height);
    }

    protected Color getColor(XpDropInFlight xpDropInFlight) {
        switch (xpDropInFlight.style) {
            case DEFAULT:
                return config.xpDropColor();
            case MELEE:
            case RANGE:
            case MAGE:
                return config.xpDropColorWhenPraying();
        }
        return Color.WHITE;
    }

    protected String getDropText(XpDropInFlight xpDropInFlight) {
        String text = xpFormatter.format(xpDropInFlight.amount);

        text = config.xpDropPrefix() + text;

        return text;
    }

    private void update() {
        updateFont();
        updateDrops();
        pollDrops();
    }

    //Pull data from the config fields to set the font for drops
    //Will use default runescape font if none other are specified by the name
    private void updateFont() {
        //only perform anything within this function if any settings related to the font have changed
        if(!lastFont.equals(config.fontName()) || lastFontSize != config.fontSize() || lastFontStyle != config.fontStyle()) {
            lastFont = config.fontName();
            lastFontSize = config.fontSize();
            lastFontStyle = config.fontStyle();

            //use runescape font as default
            if(config.fontName().equals("")) {
                if (config.fontSize() < 16)
                {
                    font = FontManager.getRunescapeSmallFont();
                }
                else if (config.fontStyle() == XpDropsConfig.FontStyle.BOLD || config.fontStyle() == XpDropsConfig.FontStyle.BOLD_ITALICS)
                {
                    font = FontManager.getRunescapeBoldFont();
                }
                else
                {
                    font = FontManager.getRunescapeFont();
                }

                if (config.fontSize() > 16)
                {
                    font = font.deriveFont((float)config.fontSize());
                }

                if (config.fontStyle() == XpDropsConfig.FontStyle.BOLD)
                {
                    font = font.deriveFont(Font.BOLD);
                }
                if (config.fontStyle() == XpDropsConfig.FontStyle.ITALICS)
                {
                    font = font.deriveFont(Font.ITALIC);
                }
                if (config.fontStyle() == XpDropsConfig.FontStyle.BOLD_ITALICS)
                {
                    font = font.deriveFont(Font.ITALIC | Font.BOLD);
                }

                useRunescapeFont = true;
                return;
            }

            int style = Font.PLAIN;
            switch (config.fontStyle()) {
                case BOLD:
                    style = Font.BOLD;
                    break;
                case ITALICS:
                    style = Font.ITALIC;
                    break;
                case BOLD_ITALICS:
                    style = Font.BOLD | Font.ITALIC;
                    break;
            }

            font = new Font(config.fontName(), style, config.fontSize());
            useRunescapeFont = false;
        }
    }

    private void updateDrops() {
        //if the drop has been in frame longer than specified in the config, remove it
        xpDropsInFlight.removeIf(xpDropInFlight -> xpDropInFlight.frame > config.framesPerDrop());

        //int yModifier = config.yDirection() == XpDropsConfig.VerticalDirection.UP ? -1 : 1;

        float frameTime = System.currentTimeMillis() - lastFrameTime;
        float frameTimeModifier = frameTime / CONSTANT_FRAME_TIME;

        for(XpDropInFlight xpDropInFlight : xpDropsInFlight) {
            xpDropInFlight.frame += frameTimeModifier;
        }

        if(config.fadeOut()) {
            int threshold = (int) (0.66f * config.framesPerDrop());
            int delta = config.framesPerDrop() - threshold;
            for(XpDropInFlight xpDropInFlight : xpDropsInFlight) {
                if(xpDropInFlight.frame > threshold) {
                    int point = (int) xpDropInFlight.frame - threshold;
                    float fade = point / (float) delta;
                    xpDropInFlight.alpha = Math.max(0, 0xff - fade * 0xff);
                }
            }
        }
    }

    //TODO: Write documentation
    private void pollDrops() {
        float lastFrame = 0;
        if(xpDropsInFlight.size() > 0) {
            XpDropInFlight xpDropInFlight = xpDropsInFlight.get(xpDropsInFlight.size() - 1);
            lastFrame = xpDropInFlight.frame;
            //TODO: lastframe -= config.groupedDelay();
        }

        ArrayList<XpDropInFlight> drops = new ArrayList<>();

        XpDropStyle style = XpDropStyle.DEFAULT;

        int totalHit = 0;
        Actor target = null;

        if(config.isGrouped()) {
            int amount = 0;
            int icons = 0;

            XpDrop xpDrop = plugin.getQueue().poll();
            while(xpDrop != null) {
                amount += xpDrop.getExperience();
                //TODO: What is happening in the assignment of this icon
                icons |= 1 << SKILL_PRIORITY[xpDrop.getSkill().ordinal()];
                if(xpDrop.getStyle() != XpDropStyle.DEFAULT) {
                    style = xpDrop.getStyle();
                }

                if(xpDrop.fake) {
                    //TODO: What is this assignment?
                    icons |= 1 << 23;
                }

                xpDrop = plugin.getQueue().poll();
            }

            if(amount > 0) {
                //TODO: Is this the correct assignment for hit?
                int hit = totalHit;
                XpDropInFlight xpDropInFlight = new XpDropInFlight(icons, amount, style, 0, 0, 0xff, 0, hit, target);
                drops.add(xpDropInFlight);
            }
        }
        else {
            XpDrop xpDrop = plugin.getQueue().poll();
            while(xpDrop != null) {
                int icons = 1 << SKILL_PRIORITY[xpDrop.getSkill().ordinal()];
                int amount = xpDrop.getExperience();
                if(xpDrop.getStyle() != XpDropStyle.DEFAULT) {
                    style = xpDrop.getStyle();
                }

                if(xpDrop.fake) {
                    icons |= 1 << 23;
                }

                XpDropInFlight xpDropInFlight = new XpDropInFlight(icons, amount, style, 0, 0, 0xff, 0, 0, xpDrop.attachedActor);
                drops.add(xpDropInFlight);

                xpDrop = plugin.getQueue().poll();
            }
        }

        for(XpDropInFlight drop : drops) {
            drop.setStyle(style);
            xpDropsInFlight.add(drop);
        }
    }
}
