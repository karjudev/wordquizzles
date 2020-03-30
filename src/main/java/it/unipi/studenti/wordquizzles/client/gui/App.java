package it.unipi.studenti.wordquizzles.client.gui;

import java.util.List;
import java.util.Map;

import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import it.unipi.studenti.wordquizzles.client.drivers.TCPDriver;
import it.unipi.studenti.wordquizzles.client.drivers.UDPDriver;
import it.unipi.studenti.wordquizzles.client.gui.components.BottomBox;
import it.unipi.studenti.wordquizzles.client.gui.components.ChallengeHandler;
import it.unipi.studenti.wordquizzles.client.gui.components.FriendsList;
import it.unipi.studenti.wordquizzles.client.gui.components.RankingTable;

/**
 * App è la finestra e il punto di ingresso principale dell'applicazione.
 */
public class App extends JFrame {

    /**
     * ID generato automaticamente.
     */
    private static final long serialVersionUID = -8576965997874728730L;

    // Componente che invia i comandi al server tramite TCP
    private TCPDriver tcpDriver;

    // Componente che invia i comandi al server tramite UDP
    private UDPDriver udpDriver;

    // Lista degli amici
    private FriendsList friendsList;

    // Gestione della partita
    private ChallengeHandler challengeHandler;

    // Tabella contenente la classifica
    private RankingTable rankingTable;

    // Barra inferiore contenente username e punti
    private BottomBox bottomBox;

    /**
     * Inizializza l'app passandogli i riferimenti alle classi necessarie a inviare
     * comandi al server.
     * 
     * @param tcpDriver Invia comandi al server tramite TCP.
     * @param udpDriver invia comandi al server tramite UDP.
     */
    public App(TCPDriver tcpDriver, UDPDriver udpDriver) {
        // Titolo della finestra
        super("Word Quizzles");
        // Imposta i driver
        this.tcpDriver = tcpDriver;
        this.udpDriver = udpDriver;
        // Pannello che contiene i controlli
        JPanel panel = new JPanel(new BorderLayout());
        // Lista degli amici
        friendsList = new FriendsList(tcpDriver);
        panel.add(friendsList, BorderLayout.WEST);
        // Gestione della partita
        challengeHandler = new ChallengeHandler(tcpDriver);
        panel.add(challengeHandler, BorderLayout.CENTER);
        // Tabella contenente la classifica
        rankingTable = new RankingTable(tcpDriver);
        panel.add(rankingTable, BorderLayout.EAST);
        // Barra inferiore
        bottomBox = new BottomBox();
        panel.add(bottomBox, BorderLayout.SOUTH);
        // Aggiunge un listener che viene invocato all'uscita
        registerCloseOperations();
        // Aggiunge il pannello alla finestra
        add(panel);
        // Imposta la finestra come non ridimensionabile
        setResizable(false);
        // Imposta la dimensione della finestra
        setSize(1000, 500);
        // Fa apparire la finestra al centro dello schermo
        setLocationRelativeTo(null);
    }

    /**
     * Registra le operazioni che avvengono quando viene chiusa la finestra.
     */
    private void registerCloseOperations() {
        App app = this;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Invia la richiesta di logout
                try {
                    tcpDriver.logoutRequest();
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(app, e1.getMessage(), "Errore di logout", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    /**
     * Visualizza una finestra di dialogo contenente un errore.
     * 
     * @param message Messaggio di errore da visualizzare.
     */
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Errore", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Chiude l'applicazione terminando tutte le dipendenze.
     */
    public void close() {
        try {
            tcpDriver.close();
            udpDriver.close();
            this.dispose();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Errore di chiusura", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Invia tutte le richieste necessarie a reperire le informazioni per il primo
     * avvio.
     * 
     * @param username Nome dell'utente che ha appena effettuato il login e che deve
     *                 caricare le informazioni.
     */
    public void setup(String username) {
        // Setta lo username
        bottomBox.setUsername(username);
        // Richiede le informazioni dell'account
        try {
            tcpDriver.pointsRequest();
            tcpDriver.friendsListRequest();
            tcpDriver.rankingRequest();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Errore di setup", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Aggiunge un amico alla lista degli amici e richiede di scaricare di nuovo la
     * lista degli amici.
     * 
     * @param friendUsername Username dell'amico appena aggiunto.
     */
    public void addFriend(String friendUsername) {
        friendsList.addFriend(friendUsername);
    }

    /**
     * Aggiorna la lista degli amici dell'utente.
     * 
     * @param friends Lista degli amici.
     */
    public void setFriends(List<String> friends) {
        friendsList.setFriends(friends);
    }

    /**
     * Imposta la classifica passata.
     * 
     * @param ranking Classifica sotto forma di mappa che associa gli username ai
     *                punteggi.
     */
    public void setRanking(Map<String, Integer> ranking) {
        rankingTable.setRanking(ranking);
    }

    /**
     * Imposta i punti dell'utente.
     * 
     * @param points Punti dell'utente corrente.
     */
    public void setPoints(int points) {
        bottomBox.setPoints(points);
    }

    /**
     * Visualizza un messaggio che notifica che la challenge è stata rifiutata.
     */
    public void challengeRefused() {
        JOptionPane.showMessageDialog(this, "Sfida rifiutata", "Sfida", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Abilita i controlli dell'applicazione per partecipare alla sfida.
     */
    public void setupChallenge() {
        challengeHandler.setupChallenge();
    }

    /**
     * Imposta la parola da tradurre nella finestra e pulisce il campo di testo.
     * 
     * @param word Parola da mostrare all'utente.
     */
    public void setWord(String word) {
        challengeHandler.setWord(word);
    }

    /**
     * Mostra all'utente una finestra di dialogo con un report e poi disabilita i
     * controlli della partita.
     * 
     * @param report Report da mostrare all'utente.
     */
    public void showReport(String report) {
        challengeHandler.showReport(report);
    }

    /**
     * Notifica un invito e richiede all'utente di accettarlo.
     * 
     * @param username Nome dell'utente che ha inviato l'invito.
     */
    public void notifyInvitation(String username) {
        // Messaggio mostrato all'utente
        String message = String.format(
                "Invito da %s. Vuoi accettarlo?\nNel caso la risposta non arrivi la sfida non sarà inizializzata.",
                username);
        // Visualizza una finestra di dialogo e richiede conferma
        int response = JOptionPane.showConfirmDialog(this, message, "Invito", JOptionPane.YES_NO_OPTION);
        // Se la risposta è stata positiva invia una conferma e gestisce la sfida
        if (response == JOptionPane.YES_OPTION) {
            acceptInvitation(username);
            setupChallenge();
        }
    }

    /**
     * Invia la conferma di sfida al server.
     * 
     * @param username Username dell'utente che ha inviato la sfida.
     */
    private void acceptInvitation(String username) {
        try {
            udpDriver.acceptInvitation(username);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Errore di risposta all'invito", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Blocca la possibilità di inserire input da parte dell'utente.
     */
	public void stopChallenge() {
        challengeHandler.stopChallenge();
	}

    
}