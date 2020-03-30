package it.unipi.studenti.wordquizzles.client.gui.components;

import java.awt.GridLayout;
import java.io.IOException;

import javax.swing.Timer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import it.unipi.studenti.wordquizzles.client.drivers.TCPDriver;
import it.unipi.studenti.wordquizzles.shared.Configuration;

/**
 * ChallengeHandler è il componente che mostra le parole arrivate dal server e
 * richiede l'inserimento delle traduzioni.
 */
public class ChallengeHandler extends JPanel {

    /**
     * ID generato automaticamente.
     */
    private static final long serialVersionUID = -251575630786648286L;

    // Driver che permette di scrivere sul socket le parole inviate.
    private TCPDriver driver;

    // Etichetta contenente la parola da tradurre
    private JLabel wordLabel;

    // Campo di testo contenente la traduzione
    private JTextField submissionText;

    // Pulsante che permette di inviare i dati
    private JButton submitButton;

    // Timer che controlla se la challenge è stata inizializzata
    private Timer challengeTimer;

    // Flag che indica se è in corso una sfida
    private boolean challenge;

    /**
     * Inizializza il componente passandogli il driver.
     * 
     * @param driver Driver che permette di scrivere sul socket.
     */
    public ChallengeHandler(TCPDriver driver) {
        super(new GridLayout(3, 1));
        this.driver = driver;
        challenge = false;

        // Inizializza il timer che controlla se la challenge è iniziata
        challengeTimer = new Timer(Configuration.TIMEOUT, e -> checkChallengeStarted());
        challengeTimer.setRepeats(false);

        wordLabel = new JLabel("In attesa della sfida");
        wordLabel.setVerticalAlignment(JLabel.CENTER);
        wordLabel.setHorizontalAlignment(JLabel.CENTER);
        add(wordLabel);
        
        submissionText = new JTextField();
        submissionText.setEditable(false);
        add(submissionText);
        // Permette di inviare la parola premendo invio sulla casella
        submissionText.addActionListener(e -> submit());

        // Pulsante che permette di sottomettere la parola
        submitButton = new JButton("Invia");
        submitButton.addActionListener(e -> submit());
        submitButton.setEnabled(false);
        add(submitButton);

        // Imposta la dimensione perché prenda i tre quinti dello schermo
        setSize(300, 200);
    }

    /**
     * Se la sfida non è partita entro il timeout disattiva i controlli e stampa un messaggio.
     */
    private void checkChallengeStarted() {
        // Se la challenge è stata inizializzata va tutto bene
        if (challenge)
            return;
        // Stampa un messaggio di errore
        JOptionPane.showMessageDialog(this, "La parola non è arrivata entro il timeout", "Errore durante la sfida", JOptionPane.ERROR_MESSAGE);
        // Disattiva i controlli
        stopChallenge();
    }

    /**
     * Invia una parola al server.
     */
    private void submit() {
        try {
            // Stringa che si vuole tradurre
            String word = wordLabel.getText();
            // Stringa inserita dall'utente
            String submission = submissionText.getText();
            // Invia la parola al server
            driver.submit(word, submission);
            // Cancella il testo contenuto nella casella
            submissionText.setText(null);
            // Disattiva il pulsante
            submitButton.setEnabled(false);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Errore di invio", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Imposta la parola arrivata dal server.
     * 
     * @param word Parola arrivata dal server.
     */
    public void setWord(String word) {
        // Setta il flag a vero se non è già stato fatto
        challenge = true;
        // Imposta l'etichetta
        wordLabel.setText(word);
        // Abilita la casella di testo
        submissionText.setEditable(true);
        // Abilita il pulsante
        submitButton.setEnabled(true);
    }

    /**
     * Visualizza il report che indica la fine della partita.
     * 
     * @param report Report di fine partita.
     */
    public void showReport(String report) {
        // Blocca la challenge
        stopChallenge();
        // Mostra il report
        JOptionPane.showMessageDialog(this, report, "Report", JOptionPane.INFORMATION_MESSAGE);
        try {
            // Chiede di ricaricare la classifica
            driver.rankingRequest();
            // Chiede di riavere i punti
            driver.pointsRequest();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Errore di invio", JOptionPane.ERROR_MESSAGE);

        }
    }

    /**
     * Verifica che sia in corso una sfida.
     * 
     * @return Flag che indica se la sfida è in corso.
     */
    public boolean isChallenge() {
        return challenge;
    }

    /**
     * Abilita la possibilità di scrivere nella casella di testo.
     */
	public void setupChallenge() {
        // Abilita il timer che controlla se la challenge è iniziata o c'è stato un problema
        challengeTimer.start();
    }
    
    /**
     * Disabilita i controlli che permettono di gestire la challenge.
     */
    public void stopChallenge() {
        challenge = false;
        submissionText.setEditable(false);
        submitButton.setEnabled(false);
        wordLabel.setText("In attesa della sfida");
    }
}