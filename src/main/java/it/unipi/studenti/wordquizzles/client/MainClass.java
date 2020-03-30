package it.unipi.studenti.wordquizzles.client;

import javax.swing.SwingUtilities;

import it.unipi.studenti.wordquizzles.client.gui.Login;

/**
 * MainClass si occupa di avviare il client.
 */
public class MainClass {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Login login = new Login();
            login.setVisible(true);
        });
    }
}