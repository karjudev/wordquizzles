package it.unipi.studenti.wordquizzles.client.gui.components;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import it.unipi.studenti.wordquizzles.client.drivers.TCPDriver;

/**
 * RankingTable è il componente che mostra la classifica.
 */
public class RankingTable extends JPanel {

    /**
     * ID generato automaticamente.
     */
    private static final long serialVersionUID = 1272509427682959972L;

    // Driver necessario a inoltrare richieste di challenge
    private TCPDriver driver;

    // Tabella contenente la classifica
    private JTable table;

    /**
     * Inizializza il componente dato il driver.
     * 
     * @param driver Classe necessaria a inviare inviti UDP.
     */
    public RankingTable(TCPDriver driver) {
        // Setta il layout
        super(new BorderLayout());
        this.driver = driver;
        // Tabella che contiene il modello
        table = new JTable();
        // Imposta la tabella come non modificabile
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // Aggiunge la tabella in uno scroll pane
        add(new JScrollPane(table), BorderLayout.CENTER);
        // Pulsante di sfida
        JButton challengeButton = new JButton("Sfida l'utente selezionato");
        // Funzione di sfida
        challengeButton.addActionListener(e -> challengeSelectedUser());
        add(challengeButton, BorderLayout.SOUTH);
        // Imposta la dimensione perché sia un quinto dello schermo
        setSize(100, 500);
    }

    /**
     * Sfida l'utente selezionato nella tabella
     */
    private void challengeSelectedUser() {
        // Indice della riga dell'utente selezionato
        int index = table.getSelectedRow();
        // Se non è stata selezionata nessuna riga lo notifica ed esce
        if (index == -1) {
            JOptionPane.showMessageDialog(this, "Seleziona un utente", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Username dell'utente selezionato
        String username = (String) table.getValueAt(index, 0);
        // Richiede la conferma
        String message = String.format("Vuoi sfidare %s?", username);
        int confirm = JOptionPane.showConfirmDialog(this, message, "Sfida", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.OK_OPTION)
            return;
        // Invia la richiesta di sfida
        try {
            driver.challengeRequest(username);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Errore di invio", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Aggiorna la tabella con la classifica data.
     * 
     * @param ranking Mappa contenente la classifica di tutti gli amici dell'utente selezionato.
     */
    public void setRanking(Map<String, Integer> ranking) {
        // Lista di entry della mappa
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(ranking.entrySet());
        // Ordina le entry per valore
        entryList.sort(Map.Entry.comparingByValue());
        // Nomi delle colonne
        String[] columnNames = { "Username", "Punteggio" };
        // Modello dei dati nella tabella
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        // Inserisce i dati nel modello
        for(Map.Entry<String, Integer> entry : entryList) {
            Object[] row = { entry.getKey(), entry.getValue() };
            model.addRow(row);
        }
        // Visualzza il modello nella tabella
        table.setModel(model);
    }
}