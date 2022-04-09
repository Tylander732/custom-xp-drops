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

    private final Gson GSON;

    @Inject
    protected XpDropDamageCalculator(Gson gson) {
        this.GSON = gson;
    }

    public void populateMap() {
        XP_BONUS_MAPPING.clear();
        XP_BONUS_MAPPING.putAll(getNpcsWithXpBonus());
    }


    //TODO: Spend more time figuring out what's going on in this function
    //Why are calculations happening this way?
    public int calculateHitOnNpc(int id, int hpXpDiff, boolean isPlayer, double configModifier) {

        //What determines the configModifier?
        if(Math.abs(configModifier) < 1e-6) {
            configModifier = 1e-6;
        }

        double modifier = 1.0;

        if(isPlayer) {
            modifier = Math.min(1.125d, 1 + Math.floor(id / 20.0d) / 40.0d);
        } else if (XP_BONUS_MAPPING.containsKey(id)) {
            modifier = XP_BONUS_MAPPING.get(id);
        }

        if(modifier < 1e-6) {
            return 0;
        }

        return (int) Math.round((hpXpDiff * (3.0d / 4.0d)) / modifier / configModifier);
    }

    //Read through the JSON file and gather any Npcs that have bonus xp
    private HashMap<Integer, Double> getNpcsWithXpBonus() {
        HashMap<Integer, Double> map = new HashMap<>();
        try {
            try (InputStream resource = XpDropDamageCalculator.class.getResourceAsStream((NPC_JSON_FILE))) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8));
                Object jsonResult = GSON.fromJson(reader, Map.class);
                try {
                    //Json file format is id [String, double]
                    //Cast JsonResult to a Map
                    Map<String, LinkedTreeMap<String, Double>> stringLinkedTreeMapMap = (Map<String, LinkedTreeMap<String, Double>>) jsonResult;
                    for(String id : stringLinkedTreeMapMap.keySet()) {
                        LinkedTreeMap<String, Double> result = stringLinkedTreeMapMap.get(id);

                        //for each key in the keyset, gather the bonus xp for each npc
                        //and get the percentage of bonus xp
                        for(String key : result.keySet()) {
                            Double xpBonus = result.get(key);
                            xpBonus = (xpBonus + 100) / 100.0d;
                            map.put(Integer.parseInt(id), xpBonus);
                        }
                    }
                } catch (ClassCastException castException) {
                    log.warn("Invalid Json.");
                }
            }
        }  catch (IOException e) {
            log.warn("Couldn't open JSON file");
        }
        return map;
    }
}
