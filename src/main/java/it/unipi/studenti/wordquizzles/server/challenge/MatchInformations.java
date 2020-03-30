package it.unipi.studenti.wordquizzles.server.challenge;

import java.nio.channels.SelectionKey;

import it.unipi.studenti.wordquizzles.shared.Configuration;

/**
 * MatchInformations è la classe che mantiene le informazioni necessarie ad un
 * giocatore per giocare una partita.
 */
public class MatchInformations {

    // Chiave dell'utente nel selettore principale
    private SelectionKey key;
    
    // Indice della prossima parola da tradurre
    private int index;

    // Username dell'utente
    private String username;

    // Punti dell'utente
    private int points;

    // Numero di parole indovinate
    private int guessed;

    // Numero di parole non indovinate
    private int notGuessed;

    /**
     * Istanzia la classe per un utente.
     * 
     * @param key Chiave dell'utente nel selettore principale.
     * @param username Username dell'utente di cui si mantengono le informazioni.
     */
    public MatchInformations(SelectionKey key, String username) {
        this.key = key;
        this.username = username;
        index = 0;
        points = 0;
        guessed = 0;
        notGuessed = 0;
    }

    /**
     * Restituisce l'indice della prossima parola da inviare al client.
     * 
     * @return Indice della prossima parola da inviare al client oppure -1.
     */
    private int increment() {
        // Incrementa il numero di parole
        index++;
        // Se ha superato l'indice dell'ultima parola restituisce -1
        if (index >= Configuration.WORDS_PER_MATCH)
            return -1;
        return index;
    }

    /**
     * Incrementa i punti e il numero di parole indovinate. Dopodiché restituisce l'indice della prossima parola.
     * @return Indice della prossima parola da inviare al client oppure -1.
     */
    public int correct() {
        // Incrementa i punti
        points += Configuration.CORRECT_ANSWER_POINTS;
        // Incrementa il numero di parole
        guessed++;
        return increment();
    }

    /**
     * Incrementa il numero di parole non indovinate. Dopodiché restituisce l'indice della prossima parola.
     * @return Indice della prossima parola da inviare al client oppure -1.
     */
    public int incorrect() {
        // Incrementa il numero di parole sbagliate
        notGuessed++;
        return increment();
    }

    /**
     * Incrementa i punti di un certo bonus.
     */
    public void bonus() {
        points += Configuration.BONUS_POINTS;
    }

    /**
     * @return Punti dell'utente.
     */
    public int getPoints() {
        return points;
    }

    /**
     * @return Chiave che identifica l'utente nel selettore principale.
     */
    public SelectionKey getKey() {
        return key;
    }

    /**
     * Restituisce un report contenente le informazioni e i punteggi.
     * @return Report con le informazioni dell'utente.
     */
	public String toString() {
        // Numero di parole sbagliate
        int missed = Configuration.WORDS_PER_MATCH - guessed - notGuessed;
		return String.format("L'utente %s ha indovinato %d parole, sbagliato %d traduzioni e non risposto a %d. Ha ottenuto %d punti.", username, guessed, notGuessed, missed, points);
	}

}