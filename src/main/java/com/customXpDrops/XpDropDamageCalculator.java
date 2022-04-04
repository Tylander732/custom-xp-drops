package com.customXpDrops;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class XpDropDamageCalculator {
    //Some enemies in the game give different XP values for damage when hit, such as bosses. Calculate the XP Drop if an opponent gives bonus xp
    private static final String NPC_JSON_FILE = "npc.min.json";
    private static final HashMap<Integer, Double> XP_BONUS_MAPPING = new HashMap<>();

    //TODO: What is this GSON file for?
    private final Gson GSON;

    @Inject
    protected XpDropDamageCalculator(Gson gson) {
        this.GSON = gson;
    }
}
