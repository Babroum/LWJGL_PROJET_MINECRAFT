package demo;

import demo.Enum.Textures;
import demo.Geometry.Clone3D;
import demo.Geometry.Cube;
import demo.Rendering.SceneGraph;
import demo.Rendering.Window;

public class App 
{   
    
    public static void main( String[] args )
    {
        new Window(1700,860);
        
        Window.startWindow(() -> draw(Window.getSceneGraph()));        
    }

    public static void draw(SceneGraph scene){
        Cube cube1 = new Cube();        
        Cube cube2 = new Cube(Textures.MAX);

        /*
        for (int i = 2; i < 50; i++){
            for(int j = 2; j < 50; j++){
                Clone3D tmp = new Clone3D(cube2);
                scene.root.addChild(tmp);
                tmp.x = i;
                tmp.y = j;
            }
        }*/
        
        scene.root.addChild(cube1);
        scene.root.addChild(cube2);

        cube1.x = 0;
        cube1.z = 0;

        cube2.x = 1;
        cube2.z = 1;
        cube2.y = 1;
    }  
    

    /*
    export JAVA_HOME=/home/linuxbrew/.linuxbrew/opt/openjdk@21
    mvn clean                                                                                   
    mvn package
    mvn exec:java
    */
}
