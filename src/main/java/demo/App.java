package demo;

import demo.Enum.Textures;
import demo.Geometry.Cube;
import demo.Rendering.SceneGraph;
import demo.Rendering.Window;

public class App 
{   
    
    public static void main( String[] args )
    {
        new Window(1920,1080);
        
        Window.startWindow(() -> draw(Window.getSceneGraph()));        
    }

    public static void draw(SceneGraph scene){        
        Cube cube1 = new Cube(Textures.GRASS);
        Cube cube2 = new Cube(Textures.COBBLESTONE);
        
        cube1.addChild(cube2);
        scene.root.addChild(cube1);

        cube1.x = 0f;
        cube1.z = 0f;
        
        cube2.x = 1f;
        cube2.z = 0f;
    }  
    

    /*
    export JAVA_HOME=/home/linuxbrew/.linuxbrew/opt/openjdk@21
    mvn clean                                                                                   
    mvn package
    mvn exec:java
    */
}
