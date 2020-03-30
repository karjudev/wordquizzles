package it.unipi.studenti.wordquizzles.server.services;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import org.mindrot.jbcrypt.BCrypt;

import it.unipi.studenti.wordquizzles.server.BijectiveConcurrentMap;
import it.unipi.studenti.wordquizzles.shared.Configuration;
import it.unipi.studenti.wordquizzles.shared.wqp.WQPException;

/**
 * AccountService è il singleton che si occupa di gestire le informazioni sugli
 * utenti della piattaforma e di identificare gli utenti attualmente attivi.
 */
public class AccountService {

    /**
     * User è la classe che mantiene le informazioni di un utente.
     */
    private class User {

        // Nome dell'utente (non viene serializzato per evitare la ridondanza)
        private String username;

        // Punteggio dell'utente
        private Integer points;

        // Hash della password
        private String passwordHash;

        // Lista di username degli amici dell'utente
        private List<String> friendsUsernames;

        /**
         * Crea un nuovo utente.
         * @param username Nome dell'utente.
         * @param password Password dell'utente.
         */
        public User(String username, String password) {
            this.username = username;
            points = 0;
            friendsUsernames = Collections.synchronizedList(new ArrayList<>());
            passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        }

        /**
         * @return Nome dell'utente corrente.
         */
        public String getUsername() {
            return username;
        }

        /**
         * @return Numero di punti dell'utente.
         */
        public int getPoints() {
            int gotPoints = 0;
            synchronized (points) {
                gotPoints = points;
            }
            return gotPoints;
        }

        /**
         * Incrementa i punti dell'utente di un certo incremento
         * @param increment Numero di punti da aggiungere al totale dell'utente.
         */
        public void incrementPoints(int increment) {
            synchronized (points) {
                points += increment;
            }
        }

        /**
         * @return Lista di amici dell'utente.
         */
        public List<String> getFriendsUsernames() {
            return friendsUsernames;
        }

        /**
         * @param friendUsername Username dell'amico di cui si vuole verificare l'amicizia con l'utente corrente.
         * @return Flag che indica se l'utente corrente e quello passato sono amici.
         */
        public boolean isFriend(String friendUsername) {
            return friendsUsernames.contains(friendUsername);
        }

        /**
         * Aggiunge un amico alla lista dell'utente.
         * @param friendUsername Username dell'utente da aggiungere come amico.
         */
        public void addFriend(String friendUsername) {
            friendsUsernames.add(friendUsername);
        }

        /**
         * Confronta l'hash della password passata con quello memorizzato.
         * @param password Password da verificare.
         * @return Flag che indica se i due hash sono uguali.
         */
        public boolean checkPassword(String password) {
            return BCrypt.checkpw(password, passwordHash);
        }

        /**
         * Controlla che lo username dell'utente non sia quello passato.
         * @param username Username da controllare.
         * @return Flag che indica se lo username passato è uguale a quello dell'utente.
         */
        public boolean is(String username) {
            return this.username.equals(username);
        }
    }

    // Mappa che associa username a utenti
    private ConcurrentMap<String, User> users;

    // Mappa che associa username a chiavi
    private BijectiveConcurrentMap<SelectionKey, String> loggedInUsers;

    // Oggetto che serializza la mappa degli utenti
    private Gson gson;

    // Tipo della mappa degli utenti
    private Type usersMapType;

    // Unica istanza di AccountService che può essere presente nel sistema
    private static AccountService instance;

    /**
     * Crea l'account service, se possibile deserializzando il file.
     */
    private AccountService() {
        usersMapType = new TypeToken<ConcurrentHashMap<String, User>>(){}.getType();
        gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        loggedInUsers = new BijectiveConcurrentMap<>();
        users = deserialize();
    }

    /**
     * @return Unica possibile istanza di AccountService che può essere allocata nel sistema.
     */
    public static synchronized AccountService getInstance() {
        if (instance == null)
            instance = new AccountService();
        return instance;
    }

    /**
     * Serializza tutta la struttura dati.
     */
    private synchronized void serialize() {
        String json = gson.toJson(users);
        try (Writer writer = new FileWriter(Configuration.USERS_FILENAME)) {
            writer.write(json);
            System.out.println("[ACCOUNTS] Successifully serialized file");
        } catch (IOException e) {
            System.err.printf("[ACCOUNTS] Unable to serialize file: %s\n", e.getMessage());
        }
    }

    /**
     * Deserializza il JSON contenente le informazioni dal file di salvataggio.
     * @return Mappa contenente le informazioni sugli account utente.
     */
    private ConcurrentHashMap<String, User> deserialize() {
        ConcurrentHashMap<String, User> map;
        try (JsonReader reader = new JsonReader(new FileReader(Configuration.USERS_FILENAME))) {
            map = gson.fromJson(reader, usersMapType);
        } catch (FileNotFoundException e) {
            System.err.println("[ACCOUNTS] File not found, falling back to the empty map");
            map = new ConcurrentHashMap<>();
        } catch (IOException e) {
            System.err.println("[ACCOUNTS] Unable to read file, falling back to the empty map");
            map = new ConcurrentHashMap<>();
        }
        return map;
    }

    /**
     * Registra un utente nel sistema.
     * 
     * @param username Username dell'utente.
     * @param password Password associata allo username.
     * @throws WQPException Se l'utente è già inserito nel sistema oppure se lo username è troppo lungo.
     */
    public void register(String username, String password) throws WQPException {
        // Se lo username è troppo lungo esce
        if (username.length() >= Configuration.USERNAME_MAX_LENGTH)
            throw new WQPException("Username too long");
        // Struttura dati che rappresenta l'utente
        User user = new User(username, password);
        // Prova ad inserire l'utente
        User previousUser = users.putIfAbsent(username, user);
        // Se l'utente esiste già lancia una eccezione
        if (previousUser != null)
            throw new WQPException("Username already registered");
        // Salva i cambiamenti su file
        serialize();
    }

    /**
     * Restituisce un utente nel sistema.
     * @param username Nome dell'utente da recuperare.
     * @return Struttura dati dell'utente nel sistema.
     * @throws WQPException Se l'utente con lo username passato non è stato trovato.
     */
    private User getUser(String username) throws WQPException {
        User user = users.get(username);
        if (user == null)
            throw new WQPException("User not found");
        return user;
    }

    /**
     * Restituisce un utente loggato all'interno del sistema.
     * @param key Chiave che identifica la connessione con l'utente.
     * @return Struttura dati dell'utente nel sistema.
     * @throws WQPException Se l'utente con la chiave passata non è loggato o non esiste.
     */
    private User getLoggedUser(SelectionKey key) throws WQPException {
        String username = loggedInUsers.getByKey(key);
        if (username == null)
            throw new WQPException("User is not logged in");
        return getUser(username);
    }

    /**
     * Esegue il login dell'utente.
     * 
     * @param username Username che identifica l'utente.
     * @param password Password associata allo username.
     * @param key      Chiave che identifica la connessione da cui proviene l'utente.
     * @throws WQPException Se username e password non sono corrette.
     */
    public void login(String username, String password, SelectionKey key) throws WQPException {
        // Struttura dati che identifica l'utente
        User user = getUser(username);
        // Se la password non è corretta esce
        if (!user.checkPassword(password))
            throw new WQPException("Incorrect password");
        // Salva la selection key
        boolean inserted = loggedInUsers.putIfAbsent(key, username);
        if (!inserted)
            throw new WQPException("User already logged in");
    }

    /**
     * Esegue il logout dell'utente data la chiave.
     * @param key Chiave che identifica la connessione dell'utente.
     * @throws WQPException Se l'utente non era precedentemente loggato.
     * @return Username associato alla chiave.
     */
    public String logout(SelectionKey key) throws WQPException {
        // Username dell'utente
        String username = loggedInUsers.removeByKey(key);
        // Se lo username non è stato trovato esce
        if (username == null)
            throw new WQPException("User is not logged in");
        return username;
    }

    /**
     * Restituisce lo username dell'utente loggato.
     * 
     * @param key Chiave dell'utente attualmente loggato
     * @return Username dell'utente loggato.
     * @throws WQPException Se non è possibile reperire l'utente identificato dalla chiave.
     */
    public String getUsername(SelectionKey key) throws WQPException {
        User user = getLoggedUser(key);
        return user.getUsername();
    }

    /**
     * Restituisce la chiave associata ad un utente.
     * @param username Username di cui si vuole estrarre la chiave.
     * @return Chiave associata all'utente loggato.
     * @throws WQPException Se la chiave non è stata trovata.
     */
    public SelectionKey getSelectionKey(String username) throws WQPException {
        SelectionKey key = loggedInUsers.getByValue(username);
        if (key == null)
            throw new WQPException("User not found");
        return key;
    }

    /**
     * @param key Chiave che identifica la connessione dell'utente.
     * @return Punti dell'utente dallo username passato.
     * @throws WQPException Se l'utente non è connesso o non è stato trovato.
     */
    public int getPoints(SelectionKey key) throws WQPException {
        User user = getLoggedUser(key);
        return user.getPoints();
    }

    /**
     * Incrementa i punti di un utente del parametro passato.
     * @param key Chiave che identifica l'utente.
     * @param increment Incremento di punti da aggiungere all'utente.
     * @throws WQPException Se l'utente non è connesso o non è stato trovato.
     */
    public void incrementPoints(SelectionKey key, int increment) throws WQPException {
        // Incrementa i punti degli utenti
        User user = getLoggedUser(key);
        user.incrementPoints(increment);
        // Salva i cambiamenti
        serialize();
    }

    /**
     * Aggiunge un amico per l'utente loggato con la chiave passata
     * @param key Chiave dell'utente a cui aggiungere l'amico.
     * @param friendUsername Username dell'amico da aggiungere.
     * @throws WQPException Se l'utente non è attualmente loggato, se non esiste nel sistema, se l'amico non è stato trovato
     */
    public void addFriend(SelectionKey key, String friendUsername) throws WQPException {
        // Struttura dati dell'utente corrente
        User user = getLoggedUser(key);
        // Se l'utente sta cercando di aggiungersi da solo esce
        if (user.is(friendUsername))
            throw new WQPException("You can't add yourself as a friend");
        // Se l'amico è già nella lista esce
        if (user.isFriend(friendUsername))
            throw new WQPException("Users are already friends");
        // Struttura dati dell'utente amico
        User friendUser = getUser(friendUsername);
        // Aggiunge l'associazione nell'utente loggato
        user.addFriend(friendUsername);
        // Aggiunge l'associazione nell'utente amico
        friendUser.addFriend(user.getUsername());
        // Salva i cambiamenti
        serialize();
    }

    /**
     * @param key Chiave che identifica la connessione dell'utente del quale si vuole ottenere la lista degli amici.
     * @return Lista degli username degli amici dell'utente passato.
     * @throws WQPException Se l'utente non è connesso o non è stato trovato.
     */
    public List<String> getFriendsUsernames(SelectionKey key) throws WQPException {
        User user = getLoggedUser(key);
        return user.getFriendsUsernames();
    }

    /**
     * Verifica che l'utente dalla chiave passata sia amico dell'utente dallo username passato.
     * @param key Chiave dell'utente loggato.
     * @param friendUsername Amico dell'utente da verificare.
     * @return Flag che indica se i due utenti sono amici.
     * @throws WQPException Se non è stato trovato l'utente dalla chiave passata.
     */
    public boolean isFriend(SelectionKey key, String friendUsername) throws WQPException {
        User user = getLoggedUser(key);
        return user.isFriend(friendUsername);
    }

    /**
     * Restituisce la classifica dei punteggi dell'utente corrente con tutti i propri amici.
     * @param key
     * @return
     * @throws WQPException
     */
    public Map<String, Integer> getRanking(SelectionKey key) throws WQPException {
        // Utente corrente
        User user = getLoggedUser(key);
        // Mappa che contiene i punteggi
        Map<String, Integer> ranking = new HashMap<>();
        // Riga dell'utente corrente
        ranking.put(user.getUsername(), user.getPoints());
        // Scorre la lista degli amici
        User friendUser;
        for (String friendUsername : user.getFriendsUsernames()) {
            friendUser = getUser(friendUsername);
            ranking.put(friendUsername, friendUser.getPoints());
        }
        return ranking;
    }
}