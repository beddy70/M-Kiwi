/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.server.mmodules;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;
import org.somanybits.minitel.server.ModelMModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 * @author eddy
 */
public class ServerScore1 extends ModelMModule {

    public static final String GAME_ID = "gameid";
    public static final String GAME_NAME = "gamename";
    public static final String FIELDS_LIST = "fields";
    public static final String SIZE_RECORD = "maxrecord";
    public static final String MODE = "mode";
    public static final String VALUES = "values";

    private final ObjectMapper mapper = new ObjectMapper(new JsonFactory());

    public ServerScore1(HashMap params, HttpExchange ex, Path docRoot) {
        super(params, ex, docRoot);
    }

    @Override
    public String getResponse() {

        String response = "";
        String mode = params.get(MODE);

        String method = ex.getRequestMethod();
        String game_id = null;

        if ("GET".equals(method) || "HEAD".equals(method)) {

            switch (mode) {
                case "create" -> {
                    try {
                        String game_name = params.get(GAME_NAME);

                        // Création de l'iD du jeu
                        if (game_name != null) {
                            game_id = game_name + "_" + UUID.randomUUID().toString();
                        }

                        // Création du fichier json
                        File outFile = new File(game_id + ".json");
                        // 3. Création du JsonGenerator

                        params.remove(MODE);
                        params.put(VALUES, "");

                        // Construction de l'objet JSON
                        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

                        mapper.writeValue(outFile, params);
                        System.out.println("✅ JSON écrit dans : " + outFile.getAbsolutePath());

                        response = game_id;
                    } catch (IOException ex) {
                        System.getLogger(ServerScore1.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                    }
                    response = game_id;
                }

                case "read" -> {

                    try {
                        ObjectMapper mapper1 = new ObjectMapper();

                        File file_score = new File(params.get(GAME_ID) + ".json");
                        JsonNode root = mapper1.readTree(file_score);

                        // 1. Extraction des listes de champs
                        List<String> origFields = Arrays.asList(root.path(FIELDS_LIST).asText().split(","));
                        List<String> reqFields = Arrays.asList(params.get(FIELDS_LIST).split(","));

                        // 2. Découpage des enregistrements bruts
                        String[] rawRecords = root.path(VALUES).asText().split("&");

                        // 3. Construction d'une liste de maps champ→valeur
                        List<Map<String, String>> records = new ArrayList<>(rawRecords.length);
                        for (String rec : rawRecords) {
                            String[] vals = rec.split(",");
                            Map<String, String> map = new HashMap<>();
                            for (int i = 0; i < origFields.size() && i < vals.length; i++) {
                                map.put(origFields.get(i), vals[i]);
                            }
                            records.add(map);
                        }

                        // 4. Tri par le premier champ demandé (descendant)
                        String sortKey = reqFields.get(0);
                        records.sort((m1, m2) -> {
                            String v1 = m1.getOrDefault(sortKey, "");
                            String v2 = m2.getOrDefault(sortKey, "");
                            // Essayer numérique
                            try {
                                return Long.compare(Long.parseLong(v2), Long.parseLong(v1));
                            } catch (NumberFormatException e) {
                                // fallback lexicographique
                                return v2.compareTo(v1);
                            }
                        });

                        // 5. Reconstruction des chaînes réordonnées
                        List<String> output = new ArrayList<>(records.size());
                        for (Map<String, String> rec : records) {
                            List<String> row = new ArrayList<>(reqFields.size());
                            for (String key : reqFields) {
                                row.add(rec.getOrDefault(key, ""));
                            }
                            output.add(String.join(",", row));
                        }

                        response = String.join("&", output);
                    } catch (IOException ex2) {
                        System.getLogger(ServerScore1.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex2);
                    }

                }

                case "write" -> {

                    try {
                        // 1. Chargement du fichier JSON
                        File fileScore = new File(params.get(GAME_ID) + ".json");

                        JsonNode root = mapper.readTree(fileScore);
                        if (!(root instanceof ObjectNode)) {
                            throw new IllegalStateException("Le JSON racine doit être un objet");
                        }
                        ObjectNode obj = (ObjectNode) root;

                        // 2. Extraction des paramètres existants
                        String rawValues = obj.path(VALUES).asText("");           // "Hugo,10000&Eddy,9500&…"
                        int maxRecords = Integer.parseInt(obj.path(SIZE_RECORD).asText("0")); // 10
                        String newRecord = params.get(VALUES);                    // ex : "Alice,10200"
                        String[] newParts = newRecord.split(",", 2);
                        long newScore = Long.parseLong(newParts[1]);

                        // 3. Découpage en liste
                        List<String> records = rawValues.isEmpty()
                                ? new ArrayList<>()
                                : new ArrayList<>(Arrays.asList(rawValues.split("&")));

                        // 4. Si on a de la place, ajoute direct
                        if (records.size() < maxRecords) {
                            records.add(newRecord);
                        } else {
                            // 5. Recherche du plus petit score
                            int idxMin = 0;
                            long minScore = Long.MAX_VALUE;
                            for (int i = 0; i < records.size(); i++) {
                                String[] parts = records.get(i).split(",", 2);
                                long sc = Long.parseLong(parts[1]);
                                if (sc < minScore) {
                                    minScore = sc;
                                    idxMin = i;
                                }
                            }
                            // 6. Remplace uniquement si le nouveau score est supérieur
                            if (newScore > minScore) {
                                records.set(idxMin, newRecord);
                            }
                            // sinon on ne modifie pas la liste
                        }

                        // 7. Tri décroissant par le champ "score"
                        records.sort((r1, r2) -> {
                            long s1 = Long.parseLong(r1.split(",", 2)[1]);
                            long s2 = Long.parseLong(r2.split(",", 2)[1]);
                            return Long.compare(s2, s1);
                        });

                        // 8. Réinjection et écriture
                        obj.put(VALUES, String.join("&", records));
                        mapper
                                .enable(SerializationFeature.INDENT_OUTPUT)
                                .writerWithDefaultPrettyPrinter()
                                .writeValue(fileScore, obj);
                    } catch (IOException ex) {
                        System.getLogger(ServerScore1.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                    }

                }

                case "top1" -> {
                    // Retourne le meilleur score (premier de la liste triée)
                    try {
                        File fileScore = new File(params.get(GAME_ID) + ".json");
                        System.out.println("🏆 top1: lecture de " + fileScore.getAbsolutePath());
                        JsonNode root = mapper.readTree(fileScore);
                        
                        // 1. Extraction des listes de champs
                        List<String> origFields = Arrays.asList(root.path(FIELDS_LIST).asText().split(","));
                        String reqFieldsParam = params.get(FIELDS_LIST);
                        // Si fields non fourni, utiliser les champs d'origine
                        List<String> reqFields = (reqFieldsParam != null) 
                            ? Arrays.asList(reqFieldsParam.split(","))
                            : origFields;
                        
                        System.out.println("🏆 top1: origFields=" + origFields + ", reqFields=" + reqFields);
                        
                        String rawValues = root.path(VALUES).asText("");
                        System.out.println("🏆 top1: rawValues=" + rawValues);
                        
                        if (!rawValues.isEmpty()) {
                            String[] rawRecords = rawValues.split("&");
                            
                            // 2. Construction d'une liste de maps champ→valeur
                            List<Map<String, String>> records = new ArrayList<>(rawRecords.length);
                            for (String rec : rawRecords) {
                                String[] vals = rec.split(",");
                                Map<String, String> map = new HashMap<>();
                                for (int i = 0; i < origFields.size() && i < vals.length; i++) {
                                    map.put(origFields.get(i), vals[i]);
                                }
                                records.add(map);
                            }
                            
                            System.out.println("🏆 top1: " + records.size() + " records parsés");
                            
                            // 3. Tri par le premier champ demandé (descendant)
                            String sortKey = reqFields.get(0);
                            records.sort((m1, m2) -> {
                                String v1 = m1.getOrDefault(sortKey, "");
                                String v2 = m2.getOrDefault(sortKey, "");
                                try {
                                    return Long.compare(Long.parseLong(v2), Long.parseLong(v1));
                                } catch (NumberFormatException e) {
                                    return v2.compareTo(v1);
                                }
                            });
                            
                            // 4. Retourner le premier (meilleur score)
                            if (!records.isEmpty()) {
                                Map<String, String> best = records.get(0);
                                List<String> row = new ArrayList<>(reqFields.size());
                                for (String key : reqFields) {
                                    row.add(best.getOrDefault(key, ""));
                                }
                                response = String.join(",", row);
                                System.out.println("🏆 top1: response=" + response);
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("🏆 top1 ERROR: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }

                case "top10" -> {
                    // Retourne le 10ème meilleur score (ou le dernier si moins de 10)
                    try {
                        File fileScore = new File(params.get(GAME_ID) + ".json");
                        System.out.println("🏆 top10: lecture de " + fileScore.getAbsolutePath());
                        JsonNode root = mapper.readTree(fileScore);
                        
                        // 1. Extraction des listes de champs
                        List<String> origFields = Arrays.asList(root.path(FIELDS_LIST).asText().split(","));
                        String reqFieldsParam = params.get(FIELDS_LIST);
                        // Si fields non fourni, utiliser les champs d'origine
                        List<String> reqFields = (reqFieldsParam != null) 
                            ? Arrays.asList(reqFieldsParam.split(","))
                            : origFields;
                        
                        String rawValues = root.path(VALUES).asText("");
                        if (!rawValues.isEmpty()) {
                            String[] rawRecords = rawValues.split("&");
                            
                            // 2. Construction d'une liste de maps champ→valeur
                            List<Map<String, String>> records = new ArrayList<>(rawRecords.length);
                            for (String rec : rawRecords) {
                                String[] vals = rec.split(",");
                                Map<String, String> map = new HashMap<>();
                                for (int i = 0; i < origFields.size() && i < vals.length; i++) {
                                    map.put(origFields.get(i), vals[i]);
                                }
                                records.add(map);
                            }
                            
                            // 3. Tri par le premier champ demandé (descendant)
                            String sortKey = reqFields.get(0);
                            records.sort((m1, m2) -> {
                                String v1 = m1.getOrDefault(sortKey, "");
                                String v2 = m2.getOrDefault(sortKey, "");
                                try {
                                    return Long.compare(Long.parseLong(v2), Long.parseLong(v1));
                                } catch (NumberFormatException e) {
                                    return v2.compareTo(v1);
                                }
                            });
                            
                            // 4. Retourner le 10ème (ou le dernier si moins de 10)
                            if (!records.isEmpty()) {
                                int index = Math.min(9, records.size() - 1);
                                Map<String, String> tenth = records.get(index);
                                List<String> row = new ArrayList<>(reqFields.size());
                                for (String key : reqFields) {
                                    row.add(tenth.getOrDefault(key, ""));
                                }
                                response = String.join(",", row);
                                System.out.println("🏆 top10: response=" + response);
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("🏆 top10 ERROR: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }

            }

        }

        return response;
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getContentType() {
        return "Content-Type, text/plain; charset=UTF-8";
    }

}
