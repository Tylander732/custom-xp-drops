package com.customXpDrops;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

import java.awt.Color;

/**
 * Interface extending config class, contains all config items that I may want to change for my xp drops
 */
@ConfigGroup("CustomXpDrops")
public interface XpDropsConfig extends Config {

    enum FontStyle {
        BOLD("Bold"),
        ITALICS("Italics"),
        BOLD_ITALICS("Bold and italics"),
        DEFAULT("Default");

        String name;
        FontStyle(String name){
            this.name = name;
        }
        public String getName() {
            return name;
        }
    }

    enum VerticalDirection {
        UP,
        DOWN
    }

    @ConfigSection(
            name = "xp drop settings",
            description = "Settings related to xp drops",
            position = 1
    )
    String xp_drop_settings = "xp_drop_settings";

    @ConfigSection(
            name = "font settings",
            description = "Settings related to your font choice",
            position = 2
    )
    String font_settings = "font_settings";

    @ConfigSection(
            name = "Overall Xp Display settings",
            description = "Settings related to the display of Overall XP",
            position = 3
    )
    String overall_xp_settings = "overall_xp_settings";

    @ConfigItem(
            keyName = "grouped",
            name = "Group XP Drops",
            description = "Group XP Drops",
            position = 0,
            section = xp_drop_settings
    )
    default boolean isGrouped() {
        return true;
    }

    @ConfigItem(
            keyName = "groupedDelay",
            name = "XP Drop Delay",
            description = "Amount of frames delay between 2 drops when not grouped",
            position = 1,
            section = xp_drop_settings
    )
    default int groupedDelay() {
        return 20;
    }

    @ConfigItem(
            keyName = "yPixelsPerSecond",
            name = "Vertical speed",
            description = "Amount of pixels the xp drop will move per second in the vertical direction",
            position = 2,
            section = xp_drop_settings
    )
    default int yPixelsPerSecond() {
        return 44;
    }

    @ConfigItem(
            keyName = "yDirection",
            name = "Vertical Direction",
            description = "Direction the drop moves vertically, either UP or DOWN",
            position = 3,
            section = xp_drop_settings
    )
    default VerticalDirection yDirection(){
        return VerticalDirection.UP;
    }

    @ConfigItem(
            keyName = "framesPerDrop",
            name = "Time until fadeout",
            description = "Time until the XP drop disappears",
            position = 4,
            section = xp_drop_settings
    )
    default int framesPerDrop(){
        return 100;
    }

    @ConfigItem(
            keyName = "fadeOut",
            name = "Fade out",
            description = "Should the XP drop fade out",
            position = 5,
            section = xp_drop_settings
    )
    default boolean fadeOut() {
        return true;
    }

    @ConfigItem(
            keyName = "showIcons",
            name = "Show skill icons",
            description = "Show the skill icons next to the XP drop",
            position = 6,
            section = xp_drop_settings
    )
    default boolean showIcons() {
        return true;
    }

    @ConfigItem(
            keyName = "xpDropColor",
            name = "Xp Drop Color",
            description = "Color you want the XP drop to be",
            position = 7,
            section = xp_drop_settings
    )
    default Color xpDropColor() {
        return Color.WHITE;
    }

    @ConfigItem(
            keyName = "xpDropColorWhenPraying",
            name = "XP Drop Color when praying",
            description = "Color of the XP drop when using offensive prayers",
            position = 8,
            section = xp_drop_settings
    )
    default Color xpDropColorWhenPraying() {
        return Color.BLUE;
    }

    @ConfigItem(
            keyName = "attachToPlayer",
            name = "Attach to player",
            description = "Attaches the XP drop location to the player",
            position = 9,
            section = xp_drop_settings
    )
    default boolean attachToPlayer() {
        return false;
    }

    @ConfigItem(
            keyName = "fontName",
            name = "Font Name",
            description = "Name of the font for XP Drops.",
            position = 10,
            section = font_settings
    )
    default String fontName() {
        return "";
    }

    @ConfigItem(
            keyName = "fontSize",
            name = "Font Size",
            description = "Size for your font",
            position = 11,
            section = font_settings
    )
    default int fontSize() {
        return 12;
    }

    @ConfigItem(
            keyName = "fontStyle",
            name = "Font Style",
            description = "Style of font for XP Drops",
            position = 12,
            section = font_settings
    )
    default FontStyle fontStyle() {
        return FontStyle.DEFAULT;
    }

    @ConfigItem(
            keyName = "xpDropPrefix",
            name = "Xp Drop Prefix",
            description = "Custom Prefix for Xp Drops",
            position = 13,
            section = font_settings

    )
    default String xpDropPrefix() {
        return "";
    }

    @ConfigItem(
            keyName = "overallXpFontSize",
            name = "Overall XP Font Size",
            description = "Size of font for the Overall XP overlay",
            position = 14,
            section = overall_xp_settings

    )
    default int overallXpFontSize() {
        return 16;
    }

}

