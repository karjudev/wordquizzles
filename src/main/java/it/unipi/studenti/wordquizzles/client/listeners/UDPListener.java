package it.unipi.studenti.wordquizzles.client.listeners;

import java.io.IOException;

import it.unipi.studenti.wordquizzles.client.clients.UDPClient;
import it.unipi.studenti.wordquizzles.client.gui.App;

/**
 * UDPListener è il thread che si occupa di attendere gli inviti via UDP.
 */
public class UDPListener extends Thread {

    // Client che riceve stringhe via UDP
    private UDPClient client;

    // Applicazione a cui inviare gli aggiornamenti
    private App app;

    /**
     * Inizializza il thread passandogli i parametri necessari.
     * 
     * @param client Client da cui ricevere stringhe arrivate via UDP.
     * @param app Applicazione a cui inviare notifiche perché modifichi l'interfaccia.
     */
    public UDPListener(UDPClient client, App app) {
        this.client = client;
        this.app = app;
    }

    @Override
    public void run() {
        // Loop di attesa delle stringhe
        try {
            System.out.println("[UDP] Listener started");
            while (!Thread.interrupted()) {
                // Stringa arrivata come invito
                String username = client.receive();
                // Notifica l'invito
                app.notifyInvitation(username);
            }
        } catch (IOException e) {
            System.err.printf("[UDP] Unable to receive inviations: %s\n", e.getMessage());
        }
    }
}