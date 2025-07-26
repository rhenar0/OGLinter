package fr.blueproject.oglinter.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class IgnoreListManager {
    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<String>>(){}.getType();

    private static final Path FILE_PATH = Path.of("config", "oglinter", "ignore_words.json");
    private static final Set<String> defaultIgnoreWords = Set.of("horyel", "chaor", "sylkabe", "rhenar");

    private static Set<String> ignoreWords = new HashSet<>();

    public static void load() {
        try {
            if (!Files.exists(FILE_PATH)) {
                save(defaultIgnoreWords);
            }

            Set<String> userWords = new HashSet<>(GSON.fromJson(Files.readString(FILE_PATH), LIST_TYPE));
            ignoreWords = new HashSet<>(defaultIgnoreWords);
            ignoreWords.addAll(userWords);

            if (!userWords.containsAll(defaultIgnoreWords)) {
                Set<String> merged = new HashSet<>(userWords);
                merged.addAll(defaultIgnoreWords);
                save(merged);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void save(Set<String> words) {
        try {
            Files.createDirectories(FILE_PATH.getParent());
            Files.writeString(FILE_PATH, GSON.toJson(new ArrayList<>(words)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean addWord(String word) {
        String lower = word.toLowerCase(Locale.ROOT);
        if (ignoreWords.contains(lower)) return false;
        ignoreWords.add(lower);
        save(ignoreWords);
        return true;
    }

    public static boolean removeWord(String word) {
        String lower = word.toLowerCase(Locale.ROOT);
        if (!ignoreWords.contains(lower)) return false;
        ignoreWords.remove(lower);
        save(ignoreWords);
        return true;
    }

    public static Set<String> getIgnoreWords() {
        return new TreeSet<>(ignoreWords);
    }

    public static boolean shouldIgnore(String word) {
        return ignoreWords.contains(word.toLowerCase(Locale.ROOT));
    }
}
