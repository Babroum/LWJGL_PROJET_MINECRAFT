package demo.LAN;

import demo.Enum.BlocksType;

public class NetworkEvent{
    public static void sendPosition(float x, float y, float z){
        GameClient.send("POS:" + GameClient.myId + ":" + x + ":" + y + ":" + z);
    }

    public static void sendBreak(int x, int y, int z){
        GameClient.send("BREAK:" + x + ":" + y + ":" + z);
    }

    public static void sendPlace(int blockType, int x, int y, int z){
        GameClient.send("PLACE:" + blockType + ":" + x + ":" + y + ":" + z);
    }
}