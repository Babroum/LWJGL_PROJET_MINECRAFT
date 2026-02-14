package demo;

import demo.Geometry.Clone3D;
import demo.Geometry.Cube;
import demo.Types.Color4f;

public class App 
{   
    
    public static void main( String[] args )
    {
        Window window = new Window(1600,800);
        draw(window.getSceneGraph());
    }

    public static void draw(SceneGraph scene){        
        Cube cube1 = new Cube();
        Clone3D cube2 = new Clone3D(cube1); // replaced Cube cube2 = new Cube(); everything else works the same
        
        scene.root.addChild(cube1);
        scene.root.addChild(cube2);
        
        cube1.setColor(Color4f.BLUE);
        cube1.x = -1f;
        cube1.z = -2f;
        cube1.angleX = 0f;
        cube1.angleY = 0f;

        cube2.setColor(Color4f.RED);        
        cube2.x = 1f;
        cube2.z = -2f;
        cube2.angleX = 0f;
        cube2.angleY = 0f;
    }  
    

    /*
    mvn clean                                                                                   
    mvn package
    mvn exec:java
    */
}
