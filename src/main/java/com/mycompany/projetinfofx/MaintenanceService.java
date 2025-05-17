/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.projetinfofx;

/**
 *
 * @author titou
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class MaintenanceService {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("ddMMyyyyHH:mm");

    public static Map<String, Float> calculerFiabilite(String path) throws Exception {
        Map<String, Float> fiab = new TreeMap<>();
        Map<String, LocalDateTime> lastDown = new HashMap<>();
        Map<String, Float> downTime = new HashMap<>();
        LocalDateTime startObs = null, endObs = null;

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";"); // date;heure;machine;A/D;operator;cause
                String dt = parts[0] + parts[1];
                LocalDateTime t = LocalDateTime.parse(dt, DATE_FMT);
                if (startObs == null || t.isBefore(startObs)) startObs = t;
                if (endObs == null || t.isAfter(endObs)) endObs = t;
                String machine = parts[2];
                String type = parts[3];

                fiab.putIfAbsent(machine, 0f);
                downTime.putIfAbsent(machine, 0f);

                if ("A".equals(type)) {
                    lastDown.put(machine, t);
                } else if ("D".equals(type) && lastDown.containsKey(machine)) {
                    float dtHours = java.time.Duration.between(lastDown.get(machine), t).toMinutes() / 60f;
                    downTime.put(machine, downTime.get(machine) + dtHours);
                    lastDown.remove(machine);
                }
            }
        }
        float obsHours = java.time.Duration.between(startObs, endObs).toMinutes() / 60f;
        for (String m : fiab.keySet()) {
            float dt = downTime.getOrDefault(m, 0f);
            fiab.put(m, ((obsHours - dt) / obsHours) * 100f);
        }
        return fiab;
    }
}
