package it.unipi.studenti.wordquizzles.client.gui.components;

import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * BottomBox è la barra inferiore che mostra lo username dell'utente appena
 * loggato e i punti.
 */
public class BottomBox extends JPanel {

    /**
     * ID generato automaticamente.
     */
    private static final long serialVersionUID = -3840507674346882586L;

    // Etichetta che mostra lo username
    private JLabel usernameLabel;

    // Etichetta che mostra i punti
    private JLabel pointsLabel;

    public BottomBox() {
        // Aggiunge un layout a griglia
        super(new GridLayout(1, 2));
        
        usernameLabel = new JLabel();
        add(usernameLabel);
        pointsLabel = new JLabel();
        add(pointsLabel);
    }

    /**
     * Imposta l'etichetta con lo username.
     * 
     * @param username Username da settare.
     */
    public void setUsername(String username) {
        usernameLabel.setText(username);
    }

    /**
     * Imposta l'etichetta con i punti.
     * 
     * @param points Punti dell'utente.
     */
    public void setPoints(int points) {
        String pointsText = String.format("%d Punti", points);
        pointsLabel.setText(pointsText);
    }
}