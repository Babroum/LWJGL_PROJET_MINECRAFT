package demo.LAN;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class GameClient{
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    public static int myId;

    public static void connect(String ip, int port) throws IOException{
        GameClient.socket = new Socket(ip, port);
        GameClient.out = new PrintWriter(GameClient.socket.getOutputStream(), true);
        GameClient.in = new BufferedReader(new InputStreamReader(GameClient.socket.getInputStream()));
        Thread t = new Thread(GameClient::run);
        t.setDaemon(true);
        t.start();
    }

    public static void run(){
        String message;
        try{
            while ((message = in.readLine()) != null){
                GameClient.handleMessage(message);
            }
        }
        catch (IOException e){
            System.out.println("Déconnecté du serveur");
        }
    }

    private static void handleMessage(String message){
        String[] parts = message.split(":");
        switch (parts[0]) {
            case "MYID":
                GameClient.myId = Integer.parseInt(parts[1]);
                break;
            
            case "TEST":
                System.out.println("Message reçu");

            default:
                break;
        }
    }

    public static void send(String message){
        GameClient.out.println(message);
    }
}