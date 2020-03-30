package it.unipi.studenti.wordquizzles.shared;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import it.unipi.studenti.wordquizzles.server.services.AccountService;
import it.unipi.studenti.wordquizzles.shared.wqp.WQPException;

/**
 * StaticUtilities è la classe che mantiene dei metodi statici utili a varie
 * componenti del server e del client.
 */
public class StaticUtilities {

    /**
     * Data una stringa restituisce un buffer che contiene la lunghezza e la stringa
     * stessa.
     * 
     * @param message Stringa da bufferizzare.
     * @return Buffer che contiene la lunghezza della stringa e la stringa stessa.
     */
    public static ByteBuffer bufferizeString(String message) {
        // Array di byte contenente i caratteri
        byte[] messageArray = message.getBytes(StandardCharsets.UTF_8);
        // Buffer contenente il messaggio
        ByteBuffer buffer = ByteBuffer.allocate(messageArray.length + Integer.BYTES);
        buffer.putInt(messageArray.length);
        buffer.put(messageArray);
        buffer.flip();
        return buffer;
    }

    /**
     * Dato un buffer restituisce una stringa con il contenuto del buffer codificato
     * in UTF-8.
     * 
     * @param buffer Buffer contenente dei caratteri.
     * @return Stringa con i dati del buffer codificati in UTF-8.
     */
    public static String stringifyBuffer(ByteBuffer buffer) {
        String message = new String(buffer.array(), StandardCharsets.UTF_8).trim();
        return message;
    }

    /**
     * Concatena una serie di stringhe attraverso un delimitatore.
     * 
     * @param delimiter Delimitatore che sta tra una stringa e l'altra.
     * @param args      Argomenti da concatenare.
     * @return Stringa con gli argomenti concatenati in una stringa sola.
     */
    public static String concat(String delimiter, String... args) {
        StringBuilder builder = new StringBuilder();
        for (String arg : args) {
            if (arg == null)
                continue;
            builder.append(arg);
            builder.append(delimiter);
        }
        builder.trimToSize();
        return builder.toString();
    }

    /**
     * Rimuove tutti i riferimenti alla chiave se il client si disconnette.
     * 
     * @param key Chiave dell'utente che si è disconnesso.
     * @throws IOException Se non è possibile estrarre le informazioni del client.
     */
    public static void removeKey(SelectionKey key) throws IOException {
        // Client associato alla chiave
        SocketChannel client = (SocketChannel) key.channel();
        System.out.printf("[SERVER] %s closed connection\n", client.getRemoteAddress());
        // Cancella la chiave nel selettore
        key.cancel();
        // Chiude la connessione
        client.close();
    }

    /**
     * Esegue il logout di un utente.
     * 
     * @param key Chiave dell'utente loggato.
     * @return Username dell'utente che ha eseguito il logout.
     * @throws WQPException Se l'utente non è stato trovato.
     */
    public static String logoutUser(SelectionKey key) throws WQPException {
        // Servizio di gestione degli account
        AccountService accountService = AccountService.getInstance();
        // Username dell'utente loggato
        String username = accountService.logout(key);
        return username;
    }

}