package demo.LAN;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

import org.joml.Vector3f;

import demo.Managers.PlayersManager;
import demo.Enum.BlocksType;

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
                break;

            case "POS":
                int id = Integer.parseInt(parts[1]);
                float x = (float)Math.floor(Float.parseFloat(parts[2]));
                float y = (float)Math.floor(Float.parseFloat(parts[3]));
                float z = (float)Math.floor(Float.parseFloat(parts[4]));
                PlayersManager.otherPlayers.put(id, new Vector3f(x, y, z));
                break;

            case "PLACE":                
                System.out.println(message);
                int type = Integer.parseInt(parts[1]);
                int px = Integer.parseInt(parts[2]);
                int py = Integer.parseInt(parts[3]);
                int pz = Integer.parseInt(parts[4]);
                PlayersManager.player.placeBlock(BlocksType.fromId(type),px, py, pz);

            case "BREAK":                
                System.out.println(message);
                int bx = Integer.parseInt(parts[1]);
                int by = Integer.parseInt(parts[2]);
                int bz = Integer.parseInt(parts[3]);
                PlayersManager.player.breakBlock(bx, by, bz);
                break;
            
            default:
                break;
        }
    }

    public static void send(String message){
        GameClient.out.println(message);
    }
}