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

    //If multiple XP Drops happen at once, create a priority based on array for order in which they appear
    int skillPriorityComparator(XpDrop x1, XpDrop x2) {
        int priority1 = XpDropOverlay.SKILL_PRIORITY[x1.getSkill().ordinal()];
        int priority2 = XpDropOverlay.SKILL_PRIORITY[x2.getSkill().ordinal()];
        return Integer.compare(priority1, priority2);
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
        //TODO: Does this queue.clear() matter?
        queue.clear();
        overlayManager.add(xpDropOverlay);

        //TODO: xpDropDamageCalculator.populateMap();
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(xpDropOverlay);
    }

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
}