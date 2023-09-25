package fr.atesab.bo4hash;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MainCsvEventReader {

    record EventData(String name, long timeStampStart, long timeStampEnd, String platform) {
    }

    public static void main(String[] args) throws IOException {
        Path csv = Path.of("H:\\Vuze Downloads\\GSC\\Default Project\\docs\\notes\\game_events.csv");
        Path output = Path.of("H:\\Vuze Downloads\\GSC\\Default Project\\docs\\game_events.md");

        List<EventData> data = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(csv)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(",");
                if (split.length != EventData.class.getRecordComponents().length) {
                    continue; // ignore lines
                }
                data.add(new EventData(split[0], Long.parseLong(split[1]), Long.parseLong(split[2]), split[3]));
            }
        }

        Set<String> names = new TreeSet<>();

        data.forEach(ed -> names.add(ed.name()));

        Map<String, String> mapping = new HashMap<>();

        mapping.put("10_and_20_no_dupe_crate_bundle", "10 and 20 no dupe crates bundle");
        mapping.put("10_no_dupe_crate_bundle", "10 no dupe crates bundle");
        mapping.put("5_and_5_no_dupe_crate_bundle", "5 and 5 no dupe crates bundle");
        mapping.put("bribe_offer_holidays_2019", "Bride offer holidays 2019");
        mapping.put("bribe_offer_launch", "Bride offer launcher");
        mapping.put("early_season_4_loot_drop", "Early season 4");
        mapping.put("fourth_of_july_event_stream", "4th July stream");
        mapping.put("free_pick_weapon_bribe_may_2020", "Free weapon bribe may 2020");
        mapping.put("global_2wxp_mp_client", "Weapon 2XP (Multiplayer/client)");
        mapping.put("global_2wxp_mp_server", "Weapon 2XP (Multiplayer/server)");
        mapping.put("global_2wxp_zm_client", "Weapon 2XP (Zombie/client)");
        mapping.put("global_2wxp_zm_server", "Weapon 2XP (Zombie/server)");
        mapping.put("global_2x_merits_wz_client", "2XP (Blackout/client)");
        mapping.put("global_2x_merits_wz_server", "2XP (Blackout/server)");
        mapping.put("global_2xnp_zm_client", "2 Nebulium plasma (Zombie/client)");
        mapping.put("global_2xnp_zm_server", "2 Nebulium plasma (Zombie/server)");
        mapping.put("playlist_2xp_mp_client_long_term", "Long term 2XP (Multiplayer/client)");
        mapping.put("playlist_2xp_mp_server_long_term", "Long term 2XP (Multiplayer/server)");
        mapping.put("global_2xp_mp_client", "2XP (Multiplayer/client)");
        mapping.put("global_2xp_mp_server", "2XP (Multiplayer/server)");
        mapping.put("global_2xp_zm_client", "2XP (Zombie/client)");
        mapping.put("global_2xp_zm_server", "2XP (Zombie/server)");
        mapping.put("global_2xtier_mp_client", "2 tier XP (Multiplayer/client)");
        mapping.put("global_2xtier_mp_server", "2 tier XP (Multiplayer/server)");
        mapping.put("global_2xtier_wz_client", "2 tier XP (Blackout/client)");
        mapping.put("global_2xtier_wz_server", "2 tier XP (Blackout/server)");

        mapping.put("season_2_bonus_stream", "Barbarians");
        mapping.put("season_2_stream", "Operation Absolute Zero");
        mapping.put("season_3_starter_pack", "Operation Grand Heist Starter pack");
        mapping.put("season_3_stream", "Operation Grand Heist");
        mapping.put("season_4_starter_pack", "Operation Spectre Rising Starter pack");
        mapping.put("season_4_stream", "Operation Spectre Rising");
        mapping.put("season_5_stream", "Season 5");
        mapping.put("season_6_stream", "Season 6");
        mapping.put("season_7_stream", "Season 7");
        mapping.put("season_8_stream", "Season 8");
        mapping.put("season_9", "Season 9");
        mapping.put("summer_break_bundle", "Summer break bundle");
        mapping.put("zm_lab_150_np_discount", "Zombie 150 Nebulium plasma discount");
        mapping.put("zm_lab_titanium_treble_slot_1", "Zombie titanium");
        mapping.put("zm_lab_tungsten_tripler_slot_2", "Zombie tungsten");
        mapping.put("half_off_nd_crate", "Half price no dupe crate");
        mapping.put("half_off_pick_weapon_bribes", "Half price pick weapon bribe");

        //mapping.put("digital_refresh_v3", "");
        //mapping.put("labor_day_event_stream", "");
        //mapping.put("playlist_2xp_mp_server_nuketown", "");
        //mapping.put("playlist_2xp_mp_client_nuketown", "");
        //mapping.put("reserve_completion_meter", "");
        //mapping.put("reserves_drop_12", "");
        //mapping.put("sunset_features", "");


        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");

        try (BufferedWriter writer = Files.newBufferedWriter(output)) {

            writer.write("""
                    [Black ops 4 (T8) information...](index.md)
                    
                    # Black Ops 4 - Events
                    
                    **Table of content**
                    - [Names](#names)
                    - [Events](#events)
                    
                    All the dates are with the format YEAR/MONTH/DAY.
                    
                    Data from the file [1e99cad95d3d02d7.csv](notes/game_events.csv). (last update: %s)
                    
                    ## Names
                    
                    | Name | Description |
                    | ---- | ----------- |
                    """.formatted(format.format(System.currentTimeMillis())));


            for (String name : names) {
                writer.write("| `" + name + "` | " + mapping.getOrDefault(name, "?") + " |\n");
            }
            writer.write("\n## Events\n\n");
            writer.write("""
                    nb: Platform = Xbox seems to link the PC and Xbox dates.
                    
                    | Name | Description | Start | End | Platform |
                    | ---- | ----------- | ----- | --- | -------- |
                    """);

            data.sort(Comparator.comparingLong(EventData::timeStampStart));

            for (EventData datum : data) {
                writer.write(
                        "| `" + datum.name + "` | "
                                + mapping.getOrDefault(datum.name, "?") + " | "
                                + format.format(new Date(datum.timeStampStart * 1000)) + " | "
                                + format.format(new Date(datum.timeStampEnd * 1000)) + " | "
                        + datum.platform + " |\n"
                );
            }

            writer.flush();
        }
    }
}
