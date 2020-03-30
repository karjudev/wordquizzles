package it.unipi.studenti.wordquizzles.client.gui.components;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import it.unipi.studenti.wordquizzles.client.drivers.TCPDriver;

/**
 * FriendsList è il componente che mostra la lista degli amici e permette
 * l'aggiunta di un nuovo amico.
 */
public class FriendsList extends JPanel {

    /**
     * ID generato automaticamente.
     */
    private static final long serialVersionUID = 4038002113147564810L;

    // Driver che permette di inviare richieste di amicizia
    private TCPDriver driver;

    // Modello dei dati presenti nella lista
    private DefaultListModel<String> listModel;

    // Lista contenente il modello
    private JList<String> list;

    /**
     * Inizializza il componente passandogli il driver.
     * 
     * @param driver Permette di inviare richieste di amicizia.
     */
    public FriendsList(TCPDriver driver) {
        // Layout del pannello
        super(new BorderLayout());
        this.driver = driver;
        // Modello dei dati
        listModel = new DefaultListModel<>();
        // Lista degli amici
        list = new JList<>(listModel);
        list.setLayoutOrientation(JList.VERTICAL);
        // Aggiunge la lista al pannello in uno scroll pane
        add(new JScrollPane(list), BorderLayout.CENTER);
        // Pulsante che permette di aggiungere un amico
        JButton addFriendButton = new JButton("Aggiungi amico");
        // Funzione che permette di aggiungere un amico
        addFriendButton.addActionListener(e -> friendRequest());
        add(addFriendButton, BorderLayout.SOUTH);
        // Imposta la dimensione perché sia un quinto della finestra
        setSize(100, 500);
    }

    /**
     * Aggiunge un amico.
     */
    private void friendRequest() {
        // Finestra di dialogo che restituisce un nome utente
        String username = JOptionPane.showInputDialog(this, "Inserisci lo username", "Aggiungi amici",
                JOptionPane.OK_OPTION);
        // Se lo username è nullo segnala l'errore ed esce
        if (username == null) {
            JOptionPane.showMessageDialog(this, "Inserisci un nome utente valido", "Errore di inserimento",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Invia una richiesta di amicizia al server
        try {
            driver.friendRequest(username);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Errore di richiesta di amicizia", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Aggiunge un amico alla lista.
     * 
     * @param username Nome utente dell'amico da aggiungere.
     */
    public void addFriend(String username) {
        listModel.addElement(username);
        // Richiede la classifica
        try {
            driver.rankingRequest();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Errore di richiesta di classifica", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Imposta la lista degli amici.
     * 
     * @param friendsList Lista degli amici.
     */
    public void setFriends(List<String> friendsList) {
        // Svuota tutti gli elementi
        listModel.removeAllElements();
        // Aggiunge i nuovi elementi
        for (String friend : friendsList)
            listModel.addElement(friend);
    }
}