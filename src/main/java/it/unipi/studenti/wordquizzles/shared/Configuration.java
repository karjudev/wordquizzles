package it.unipi.studenti.wordquizzles.shared;

/**
 * Configuration contiene come costanti statiche tutti i valori necessari al funzionamento sia del client che del server.
 */
public class Configuration {

    // Porta usata dalle connessioni su protocollo WQP (TCP + UDP)
    public static int WQP_PORT = 8080;

    // Porta usata dalle connessioni su protocollo RMI
    public static int RMI_PORT = 8081;

    // Nome dell'host del server
    public static String SERVER_HOSTNAME = "localhost";

    // TTL di un invito prima di attendere il nuovo invito
    public static int INVITATION_TTL = 10;

    // Punti guadagnati ad ogni risposta giusta
    public static int CORRECT_ANSWER_POINTS = 1;

    // Punti bonus per chi vince la partita
    public static int BONUS_POINTS = 3;

    // Numero di tentativi di invio/ricezione da fare prima di lanciare una eccezione
    public static int RETRIES = 3;

    // Massimo numero di caratteri disponibili per uno username
    public static int USERNAME_MAX_LENGTH = 255;

    // Timeout che il client deve attendere
    public static int TIMEOUT = 10000;

    // Durata di una challenge in millisecondi
    public static long CHALLENGE_TIME = 90000;

    // Nome del file in cui sono contenute le informazioni sugli account
    public static String USERS_FILENAME = "users.json";

    // Nome del file in cui sono memorizzate le parole per le sfide
    public static String WORDS_FILENAME = "words.txt";

    // Numero di parole inviate per ogni partita
    public static int WORDS_PER_MATCH = 5;

    // URL del servizio remoto
    public static String BASE_URL = "https://api.mymemory.translated.net/get?langpair=it|en&q=";
}