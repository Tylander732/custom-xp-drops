package com.customXpDrops;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.SpritePixels;
import net.runelite.api.events.FakeXpDrop;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import static net.runelite.api.ScriptID.XPDROPS_SETDROPSIZE;

@PluginDescriptor(
    name = "Custom XP Drops",
    description = "Tyreths custom XP Drops plugin, runelite app practice"
)

@Slf4j
public class CustomXpDropsPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private XpDropOverlay xpDropOverlay;

    @Inject
    private XpDropsConfig config;

    @Inject
    private ClientThread clientThread;

    @Inject
    private XpDropDamageCalculator xpDropDamageCalculator;

    @Provides
    XpDropsConfig provideConfig(ConfigManager configManager) {
        return (XpDropsConfig)configManager.getConfig(XpDropsConfig.class);
    }

    @Getter
    private final PriorityQueue<XpDrop> queue = new PriorityQueue<>(this::skillPriorityComparator);
    @Getter
    private final ArrayDeque<Hit> hitBuffer = new ArrayDeque<>();
    private static final int[] previous_exp = new int[Skill.values().length - 1];
    private static final int[] SKILL_ICON_ORDINAL_ICONS = new int[]{197, 199, 198, 203, 200, 201, 202, 212, 214, 208,
            211, 213, 207, 210, 209, 205, 204, 206, 216, 217, 215, 220, 221};
    private int lastOpponentId = -1;
    private boolean lastOpponentIsPlayer = false;
    private Actor lastOpponent;

    //If multiple XP Drops happen at once, create a priority based on array for order in which they appear
    int skillPriorityComparator(XpDrop x1, XpDrop x2) {
        int priority1 = XpDropOverlay.SKILL_PRIORITY[x1.getSkill().ordinal()];
        int priority2 = XpDropOverlay.SKILL_PRIORITY[x2.getSkill().ordinal()];
        return Integer.compare(priority1, priority2);
    }

    @Override
    protected void startUp() {
        if(client.getGameState() == GameState.LOGGED_IN) {
            //If we're logged into the game client, gather the current XP across all the skills
            //Then use the array of xps we gathered and overwrite our previous_exp array with all those values
            clientThread.invokeLater(() -> {
                int[] xps = client.getSkillExperiences();
                System.arraycopy(xps, 0, previous_exp, 0, previous_exp.length);
            });
        }
        //If unable to gather current exp in skills, set them all to 0 for now
        else {
            Arrays.fill(previous_exp, 0);
        }

        queue.clear();
        overlayManager.add(xpDropOverlay);

        //creates a map of all NPC's (by id) that give bonus xp
        xpDropDamageCalculator.populateMap();
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(xpDropOverlay);
    }

    /**
     * Set the opponent variables to either an NPC or the player the user is fighting against
     * @param event
     */
    @Subscribe
    public void onInteractingChanged(InteractingChanged event) {
        if(event.getSource() != client.getLocalPlayer()){
            return;
        }

        Actor opponent = event.getTarget();
        lastOpponent = opponent;

        //check if opponent is of object type NPC
        if(opponent instanceof NPC) {
            NPC npc = (NPC) opponent;

            lastOpponentId = npc.getId();
            lastOpponentIsPlayer = false;
        }

        if(opponent instanceof Player) {
            lastOpponentId = opponent.getCombatLevel();
            lastOpponentIsPlayer = true;
        } else {
            lastOpponentId = -1;
        }
    }

    /**
     * This function hides the default OSRS XpDrops when the custom Xp drops are turned on
     * If the script being ran has the ID of the Default XP Drops, hide them
     * @param scriptPreFired
     */
    @Subscribe
    public void onScriptPreFired(ScriptPreFired scriptPreFired) {
        if(scriptPreFired.getScriptId() == XPDROPS_SETDROPSIZE) {
            final int[] intStack = client.getIntStack();
            final int intStackSize = client.getIntStackSize();
            final int widgetId = intStack[intStackSize - 4];

            final Widget xpDrop = client.getWidget(widgetId);
            if(xpDrop != null) {
                xpDrop.setHidden(true);
            }
        }
    }

    /**
     * When logging in or when hopping worlds, reset the previous_exp array to 0,
     * so it can be refilled once the user has logged in
     * @param gameStateChanged
     */
    @Subscribe
    protected void onGameStateChanged(GameStateChanged gameStateChanged) {
        if(gameStateChanged.getGameState() == GameState.LOGIN_SCREEN || gameStateChanged.getGameState() == GameState.HOPPING) {
            Arrays.fill(previous_exp, 0);
        }
    }

    /**
     * Generate a FakeXp drop even if not XP Was gained.
     * if a skill has 200m xp in it, do not create an xp drop
     * @param event
     */
    @Subscribe
    protected void onFakeXpDrop(FakeXpDrop event) {
        int currentXp = event.getXp();

        //if the xp for a skill is at 200m, no xp drops
        if(event.getXp() >= 200000000) {
            return;
        }

        if(event.getSkill() == Skill.HITPOINTS) {
            int hit = xpDropDamageCalculator.calculateHitOnNpc(lastOpponentId, currentXp, lastOpponentIsPlayer);
            hitBuffer.add(new Hit(hit, lastOpponent));
        }

        XpDrop xpDrop = new XpDrop(event.getSkill(), currentXp, matchPrayerStyle(event.getSkill()), true, lastOpponent);
        queue.add(xpDrop);
    }

    /**
     * Function triggers when Experience, level, or boosted level of a skill changes
     * Queue up a xp drop if the currentXp is greater than the previous xp
     * set previousXp for the skill that gained xp to the new value
     * @param event
     */
    @Subscribe
    protected void onStatChanged(StatChanged event) {
        int currentXp = event.getXp();
        int previousXp = previous_exp[event.getSkill().ordinal()];
        if(previousXp > 0 && currentXp - previousXp > 0) {
            if(event.getSkill() == Skill.HITPOINTS) {
                int hit = xpDropDamageCalculator.calculateHitOnNpc(lastOpponentId, currentXp - previousXp, lastOpponentIsPlayer);
                hitBuffer.add(new Hit(hit, lastOpponent));
            }

            XpDrop xpDrop = new XpDrop(event.getSkill(), currentXp - previousXp, matchPrayerStyle(event.getSkill()), false, lastOpponent);
            queue.add(xpDrop);
        }
        previous_exp[event.getSkill().ordinal()] = event.getXp();
    }

    protected BufferedImage getSkillIcon(Skill skill) {
        int index = skill.ordinal();
        int icon = SKILL_ICON_ORDINAL_ICONS[index];
        return getIcon(icon, 0);
    }

    protected BufferedImage getIcon(int icon, int spriteIndex) {
        if(client == null) {
            return null;
        }
        SpritePixels[] pixels = client.getSprites(client.getIndexSprites(), icon, 0);
        if(pixels != null && pixels.length >= spriteIndex + 1 && pixels[spriteIndex] != null) {
            return pixels[spriteIndex].toBufferedImage();
        }
        return null;
    }

    private XpDropStyle getActivePrayerType() {
        for (XpPrayer prayer : XpPrayer.values()) {
            if(client.isPrayerActive(prayer.getPrayer())) {
                return prayer.getType();
            }
        }
        return null;
    }

    protected XpDropStyle matchPrayerStyle(Skill skill) {
        XpDropStyle style = XpDropStyle.DEFAULT;
        XpDropStyle active = getActivePrayerType();
        switch (skill) {
            case MAGIC:
                if(active == XpDropStyle.MAGE) {
                    style = active;
                } break;
            case RANGED:
                if(active == XpDropStyle.RANGE) {
                    style = active;
                } break;
            case ATTACK:
            case STRENGTH:
            case DEFENCE:
                if(active == XpDropStyle.MELEE) {
                    style = active;
                } break;
        }
        return style;
    }
}