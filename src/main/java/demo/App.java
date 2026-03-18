package demo;

import java.io.IOException;
import java.util.Scanner;

import demo.LAN.GameClient;
import demo.LAN.GameServer;
import demo.Managers.BenchmarkManager;
import demo.Managers.ChunksManager;
import demo.Rendering.SceneGraph;
import demo.Rendering.Window;

public class App 
{   
    public static void main( String[] args ) throws Exception
    {
        Scanner scanner = new Scanner(System.in);
        System.out.println("1 : Héberger ou 2 : Rejoindre");
        int choix = Integer.parseInt(scanner.nextLine());
        if (choix == 1){
            GameServer server = new GameServer();
            Thread serverThread = new Thread(() -> {try{server.start();}catch (IOException e){}});
            serverThread.setDaemon(true);
            serverThread.start();
            GameClient.connect("127.0.0.1", 5555);
        }
        else if (choix == 2){
            System.out.println("IP du server : ");
            String ip = scanner.nextLine();
            GameClient.connect(ip, 5555);
        }else if (choix == 3){
            GameClient.connect("127.0.0.1", 5555);
        }
        scanner.close();
        // création de la fenêtre 
        new Window(1200,800, choix == 1 ? "Server" : "Client");
        // donne le draw en paramètre
        // l'exécutera après l'initialisation d'opengl
        Window.startWindow(() -> draw(Window.getSceneGraph()));        
    }

    public static void draw(SceneGraph scene){
        BenchmarkManager.getInstance().start(); // <- démarre ici

        WorldGenerator generator = new WorldGenerator(4949849L); // seed
        // thread de génération
        Thread generationThread = new Thread(() -> {
            //ChunksManager.createChunks(-24, 24, -24, 24);  // 2304 chunks 
            //ChunksManager.createChunks(-16, 16, -16, 16); // 1024 chunks
            //ChunksManager.createChunks(-10, 10, -10, 10); // 400 chunks
            ChunksManager.createChunks(-2, 2, -2, 2); // 16 chunks
            ChunksManager.generateChunks(generator);
            ChunksManager.finalizeChunks();

            GameLoop.isReady = true;
            for (String msg : GameClient.pendingMessages){
                GameClient.handleMessage(msg);
            }
            GameClient.pendingMessages.clear();
        }); 
        
        // daemon pour que le thread se ferme à la fermeture de la jvm
        generationThread.setDaemon(true);
        generationThread.start();
    }
}
