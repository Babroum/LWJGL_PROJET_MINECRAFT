package demo.LAN;

import java.net.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

// en gros c'est les gars connectés

public class ClientHandler implements Runnable{
    private Socket socket;
    private GameServer server;
    private int id;
    private PrintWriter out;
    private BufferedReader in;

    /**
     * Crée un handler de client avec un socket et un server pour envoyer les données du server au clients
     * @param socket permet d'envoyer et recevoir des données
     * @param server c'est la ou on va chercher les données
     * @throws IOException
     */
    public ClientHandler(Socket socket, GameServer server) throws IOException{
        this.socket = socket;
        this.server = server;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public int getId(){
        return this.id;
    }

    public void setId(int id){
        this.id = id;
    }

    /**
     * envoie un message avec le socket
     * @param message
     */
    public void send(String message){
        this.out.println(message);
    }

    /**
     * écouteur pour les messages
     */
    @Override
    public void run(){
        String message;
        try{
            while ((message = in.readLine()) != null){
                server.broadcast(message, this);
            }
        }
        catch (IOException e){
            server.remove(this);
        }
    }
}