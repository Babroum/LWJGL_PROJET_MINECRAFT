package demo.LAN;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameServer {
    private static final int PORT = 5555;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private int nextId = 1;
    private List<String> log = new CopyOnWriteArrayList<>();

    /**
     * pour instancier un client coté serveur et le mettre dans la listes des clients connectés
     * @throws IOException
     */
    public void start() throws IOException{
        ServerSocket serverSocket;
        try{
            serverSocket = new ServerSocket(PORT);
            while (true){
                Socket clientSocket = serverSocket.accept();
                ClientHandler client = new ClientHandler(clientSocket, this);
                client.setId(nextId++);
                client.send("MYID:" + client.getId());
                String connectedClients = "PLAYERS:";
                for (ClientHandler connectedClient : this.clients){
                        connectedClients += connectedClient.getId() + ":";
                }
                // envoyer le log du serveur (à changer)
                client.send(connectedClients);
                try{
                    Thread.sleep(5000);
                }
                catch (InterruptedException ie){}
                
                for (String message : log){
                    client.send(message);
                    try{
                    Thread.sleep(10);
                }
                catch (InterruptedException ie){}
                }
                clients.add(client);
                this.broadcast("JOIN:" + client.getId(), client);
                Thread t = new Thread(client);
                t.setDaemon(true);
                t.start();
        }
        }
        catch (IOException e){
            System.err.println("Peut pas écouter le port : " + PORT);
            System.exit(1);
        }
    }

    /**
     * envoie le message a tout le monde dans le serveur
     * @param message 
     * @param sender
     */
    public void broadcast(String message, ClientHandler sender){
        String[] parts = message.split(":");
        if (parts[0].equals("BREAK") || parts[0].equals("PLACE")){
            this.log(message);
        }
        for (ClientHandler client : this.clients){
            if (client.getId() != sender.getId()){
                client.send(message);
            }
        }
    }

    /**
     * pour enlever un client de la liste des connexions
     * @param client
     */
    public void remove(ClientHandler client){
        this.clients.remove(client);
        this.broadcast("LEAVE:" + client.getId(), client);
    }

    public void log(String message){
        this.log.add(message);
    }
}