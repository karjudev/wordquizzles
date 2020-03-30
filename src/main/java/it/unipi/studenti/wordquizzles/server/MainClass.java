package it.unipi.studenti.wordquizzles.server;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutionException;

import it.unipi.studenti.wordquizzles.server.rmi.RegistrationServiceImplementation;
import it.unipi.studenti.wordquizzles.server.wqp.SocketServer;
import it.unipi.studenti.wordquizzles.shared.Configuration;
import it.unipi.studenti.wordquizzles.shared.rmi.RegistrationService;

/**
 * MainClass è il punto di ingresso del server. Istanzia le strutture necessarie
 * e fa partire i thread.
 */
public class MainClass {

    /**
     * Registra l'oggetto remoto sulla porta passata
     * 
     * @param portNumber Porta su cui registrare l'oggetto
     * @throws RemoteException Se c'è un problema nell'esposizione dell'oggetto
     *                         remoto.
     */
    private static void registerRMIService(int portNumber) throws RemoteException {
        RegistrationService server = new RegistrationServiceImplementation();
        RegistrationService stub = (RegistrationService) UnicastRemoteObject.exportObject((RegistrationService) server, 0);

        Registry registry = LocateRegistry.createRegistry(portNumber);
        registry.rebind("RegistrationService", stub);
        System.out.printf("[REGISTER] Register service is started on port %d\n", portNumber);
    }

    public static void main(String[] args) {

        try {
            registerRMIService(Configuration.RMI_PORT);
        } catch (RemoteException e) {
            System.err.println("Unable to serve the remote object");
            e.printStackTrace();
            System.exit(1);
        }
    
        try (SocketServer server = new SocketServer(Configuration.WQP_PORT)) {
            server.start();
        } catch (IOException e) {
            System.err.println("Error in read/write operation");
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("The server has stopped");
            e.printStackTrace();
        } catch (ExecutionException e) {
            System.err.println("The server failed to serve a request");
            e.printStackTrace();
        }
    }
}