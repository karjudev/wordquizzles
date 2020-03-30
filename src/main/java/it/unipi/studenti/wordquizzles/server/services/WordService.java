package it.unipi.studenti.wordquizzles.server.services;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import it.unipi.studenti.wordquizzles.shared.Configuration;

/**
 * WordService è il singleton che si occupa di reperire le parole dal servizio
 * REST remoto.
 */
public class WordService {

    // Lista delle parole disponibili
    private List<String> words;

    // Unica istanza del servizio presente nel sistema
    private static WordService wordService;

    /**
     * Istanzia il servizio leggendo le parole dal file.
     * 
     * @throws IOException Se non è possibile leggere le parole dal file.
     */
    private WordService() throws IOException {
        words = Collections.synchronizedList(new ArrayList<>());
        try (BufferedReader reader = new BufferedReader(new FileReader(Configuration.WORDS_FILENAME))) {
            String word = reader.readLine();
            while (word != null) {
                words.add(word);
                word = reader.readLine();
            }
            System.out.printf("[WORDS] Successifully read from file the available words: %s\n", words);
        }
    }

    /**
     * Traduce una parola interrogando il servizio remoto.
     * 
     * @param word Parola da tradurre.
     * @return Traduzione della parola.
     * @throws IOException Se non è possibile leggere dall'URL.
     */
    private static String translate(String word) throws IOException {
        // URL della risorsa desiderata
        URL url = new URL(Configuration.BASE_URL + word);
        // Parola da tradurre
        String translation;
        // Legge dall'URL ed elabora il JSON
        try (JsonReader reader = new JsonReader(new BufferedReader(new InputStreamReader(url.openStream())))) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            // Prende il campo desiderato
            translation = jsonObject.get("responseData").getAsJsonObject().get("translatedText").getAsString();
        }
        System.out.printf("[WORDS] %s translated into %s\n", word, translation);
        return translation;
    }

    /**
     * Estrae casualmente un numero prefissato di parole dal file, richiede al
     * servizio remoto le traduzioni e le restituisce all'utente.
     * 
     * @return Mappa che associa ad ogni parola la traduzione.
     * @throws IOException Se non è possibile leggere le parole o le traduzioni.
     */
    public static synchronized Map<String, String> getWords() throws IOException {
        // Se questa è la prima invocazione del metodo inizializza la classe
        if (wordService == null)
            wordService = new WordService();
        // Randomizza l'array di parole per estrarre le parole necessarie al match in ordine casuale
        Collections.shuffle(wordService.words);
        // Estrae il sottoinsieme necessario al match
        List<String> matchWords = wordService.words.subList(0, Configuration.WORDS_PER_MATCH);
        // Mappa delle traduzioni
        Map<String, String> translations = new HashMap<>(Configuration.WORDS_PER_MATCH);
        // Per ogni parola scarica la traduzione
        for (String word : matchWords) {
            String translation = translate(word);
            translations.put(word, translation);
        }
        return translations;
    }
}