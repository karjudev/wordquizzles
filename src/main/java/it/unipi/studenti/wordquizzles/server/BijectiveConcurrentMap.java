package it.unipi.studenti.wordquizzles.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * BijectiveConcurrentMap implementa una mappa da K a V e da V a K tramite operazioni atomiche su due ConcurrentHashMap.
 */
public class BijectiveConcurrentMap<K, V> {

    // Mappa da chiavi a valori
    private ConcurrentMap<K, V> keysToValues;

    // Mappa da valori a chiavi
    private ConcurrentMap<V, K> valuesToKeys;

    /**
     * Inizializza la struttura dati.
     */
    public BijectiveConcurrentMap() {
        keysToValues = new ConcurrentHashMap<>();
        valuesToKeys = new ConcurrentHashMap<>();
    }

    /**
     * Restituisce il valore associato alla chiave passata.
     * @param key Chiave associata al valore cercato.
     * @return Valore associato a key oppure null.
     */
    public V getByKey(K key) {
        V value = keysToValues.get(key);
        return value;
    }

    /**
     * Restituisce la chiave che identifica il valore passato.
     * @param value Valore associato alla chiave cercata.
     * @return Chiave che identifica value oppure null.
     */
    public K getByValue(V value) {
        K key = valuesToKeys.get(value);
        return key;
    }

    /**
     * Associa nella mappa key a value e viceversa. Se una delle due associazioni è già presente l'operazione fallisce.
     * @param key Chiave che identifica il valore.
     * @param value Valore associato alla chiave.
     * @return Flag che indica se l'operazione è andata a buon fine.
     */
    public boolean putIfAbsent(K key, V value) throws IllegalStateException {
        // Tenta di inserire l'associazione key -> value
        V previousValue = keysToValues.putIfAbsent(key, value);
        // Se l'associazione era già presente fallisce
        if (previousValue != null)
            return false;
        // Tenta di inserire l'associazione value -> key
        K previousKey = valuesToKeys.putIfAbsent(value, key);
        // Se l'associazione era già presente cancella l'associazione key -> value e fallisce
        if (previousKey != null) {
            keysToValues.remove(key);
            return false;
        }
        return true;
    }

    /**
     * Rimuove la coppia identificata dalla chiave.
     * @param key Chiave di cui rimuovere occorrenza.
     * @return Valore precedentemente associato alla chiave.
     */
    public V removeByKey(K key) {
        // Valore precedentemente associato alla chiave
        V value = keysToValues.remove(key);
        // Se il valore è null ha terminato
        if (value == null)
            return null;
        // Chiave precedentemente associato al valore
        valuesToKeys.remove(value);
        return value;
    }

    public K removeByValue(V value) {
        // Chiave precedentemente associata al valore
        K key = valuesToKeys.remove(value);
        // Se il valore è null ha terminato
        if (key == null)
            return null;
        // Rimuove il valore precedentemente associato alla chiave
        keysToValues.remove(key);
        return key;
    }
}