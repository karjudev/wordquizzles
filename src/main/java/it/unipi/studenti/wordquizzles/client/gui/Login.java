package it.unipi.studenti.wordquizzles.client.gui;

import java.awt.GridLayout;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import it.unipi.studenti.wordquizzles.client.clients.RMIClient;
import it.unipi.studenti.wordquizzles.client.clients.TCPClient;
import it.unipi.studenti.wordquizzles.client.clients.UDPClient;
import it.unipi.studenti.wordquizzles.client.drivers.TCPDriver;
import it.unipi.studenti.wordquizzles.client.drivers.UDPDriver;
import it.unipi.studenti.wordquizzles.client.listeners.TCPListener;
import it.unipi.studenti.wordquizzles.client.listeners.UDPListener;
import it.unipi.studenti.wordquizzles.shared.Configuration;
import it.unipi.studenti.wordquizzles.shared.wqp.WQPException;

/**
 * Login è il form che si occupa di provvedere al login dell'utente e all'avvio
 * dell'applicazione vera e propria.
 */
public class Login extends JFrame {

    /**
     * ID generato automaticamente.
     */
    private static final long serialVersionUID = 9158622471590089767L;

    // Campo di testo contenente il nome dell'host del server
    private JTextField hostNameField;

    // Campo di testo contenente lo username dell'utente
    private JTextField usernameField;

    // Campo contenente la password
    private JPasswordField passwordField;

    /**
     * Inizializza il componente passando i client necessari all'interazione sulla rete.
     * 
     * @param tcpClient Client TCP.
     * @param udpClient Client UDP.
     */
    public Login() {
        // Imposta il titolo alla finestra
        super("Login");
        // Quando la finestra viene chiusa bisogna uscire immediatamente
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Pannello che contiene i controlli (layout da 4 righe e 2 colonne)
        JPanel panel = new JPanel(new GridLayout(4, 2));
        // Controlli per il nome del server
        JLabel hostNameLabel = new JLabel("Server");
        panel.add(hostNameLabel);
        hostNameField = new JTextField(Configuration.SERVER_HOSTNAME);
        panel.add(hostNameField);
        // Controlli per lo username
        JLabel usernameLabel = new JLabel("Username");
        panel.add(usernameLabel);
        usernameField = new JTextField();
        panel.add(usernameField);
        // Controlli per la password
        JLabel passwordLabel = new JLabel("Password");
        panel.add(passwordLabel);
        passwordField = new JPasswordField();
        panel.add(passwordField);
        // Pulsante di login
        JButton loginButton = new JButton("Login");
        panel.add(loginButton);
        // Funzione di login
        loginButton.addActionListener(e -> login());
        // Pulsante di registrazione
        JButton registerButton = new JButton("Registrati");
        panel.add(registerButton);
        // Funzione di registrazione
        registerButton.addActionListener(e -> register());
        // Imposta la finestra come non ridimensionabile
        setResizable(false);
        // Imposta la dimensione della finestra
        setSize(250, 250);
        // Fa apparire la finestra al centro dello schermo
        setLocationRelativeTo(null);
        // Aggiunge il pannello con tutti i controlli alla finestra corrente
        add(panel);
    }

    /**
     * Registra l'utente inserito dal client nel sistema.
     */
    private void register() {
        // Indirizzo del server
        String hostName = hostNameField.getText();
        // Username dell'utente
        String username = usernameField.getText();
        // Password dell'utente
        String password = new String(passwordField.getPassword());
        try {
            // Oggetto remoto che consente di registrare l'utente
            RMIClient client = new RMIClient(hostName);
            // Registra l'utente
            client.registerUser(username, password);
            // Esegue il login
            login();
        } catch (RemoteException | WQPException | NotBoundException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Errore di registrazione", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Esegue il login di un utente nel sistema. Se questo ha successo avvia la finestra dell'applicazione.
     */
    private void login() {
        TCPClient tcpClient = null;
        UDPClient udpClient = null;
        try {
            // Indirizzo del server
            SocketAddress address = new InetSocketAddress(hostNameField.getText(), Configuration.WQP_PORT);
            // Client che comunicano con il server
            tcpClient = new TCPClient(address);
            udpClient = new UDPClient(tcpClient.getLocalAddress());
            // Driver che scrivono sul server
            TCPDriver tcpDriver = new TCPDriver(tcpClient);
            UDPDriver udpDriver = new UDPDriver(udpClient);
            // Applicazione grafica che visualizza le informazioni
            App app = new App(tcpDriver, udpDriver);
            // Listener che attendono e notificano l'arrivo di pacchetti
            TCPListener tcpListener = new TCPListener(tcpClient, app);
            UDPListener udpListener = new UDPListener(udpClient, app);
            // Avvia i due ascoltatori
            tcpListener.start();
            udpListener.start();
            // Username dell'utente
            String username = usernameField.getText();
            // Password dell'utente
            String password = new String(passwordField.getPassword());
            // Richiede il login dell'utente
            tcpDriver.loginRequest(username, password);
            // Mostra la finestra principale
            app.setVisible(true);
            // Chiude la finestra corrente
            this.dispose();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Errore di istanziamento dei client", JOptionPane.ERROR_MESSAGE);
            try {
                if (tcpClient != null)
                    tcpClient.close();
                if (udpClient != null)
                    udpClient.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}