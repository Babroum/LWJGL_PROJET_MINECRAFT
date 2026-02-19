package demo;

import demo.Enum.Textures;
import demo.Geometry.Cube;
import demo.Rendering.SceneGraph;
import demo.Rendering.Window;
import demo.Types.Color4f;

public class App 
{   
    
    public static void main( String[] args )
    {
        new Window(1920,1080);
        
        Window.startWindow(() -> draw(Window.getSceneGraph()));        
    }

    public static void draw(SceneGraph scene){
        Cube cube1 = new Cube(Textures.DIRT);        
        Cube cube2 = new Cube(Textures.GRASS);
        Cube cube21 = new Cube(Textures.STONE);
        
        cube2.addChild(cube21);
        scene.root.addChild(cube1);
        scene.root.addChild(cube2);

        cube1.x = 0f;
        cube1.z = 0f;

        cube2.x = 0f;
        cube2.z = 1f;
        
        cube21.x = 2f;
        cube21.z = 0f;
    }  
    

    /*
    export JAVA_HOME=/home/linuxbrew/.linuxbrew/opt/openjdk@21
    mvn clean                                                                                   
    mvn package
    mvn exec:java
    */
}
