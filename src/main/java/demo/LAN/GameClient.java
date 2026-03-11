package demo.LAN;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class GameClient{
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    public static int myId;

    public void connect(String ip, int port) throws IOException{
        this.socket = new Socket(ip, port);
        this.out = new PrintWriter(this.socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        new Thread(this::run).start();
    }

    public void run(){
        String message;
        try{
            while ((message = in.readLine()) != null){
                handleMessage(message);
            }
        }
        catch (IOException e){
            System.out.println("Déconnecté du serveur");
        }
    }

    public void handleMessage(String message){
        String[] parts = message.split(":");
        switch (parts[0]) {
            case "MYID":
                GameClient.myId = Integer.parseInt(parts[1]);
                break;
            default:
                break;
        }
    }
}