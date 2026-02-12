package demo;

import demo.Types.Color4f;

public class App 
{
    //mvn package pour ajouter les modif 
    //mvn exec:java pour executer 
    
    public static void main( String[] args )
    {
        Window window = new Window(1600,800);
        draw(window.getSceneGraph());
    }

    public static void draw(SceneGraph scene){        
        Cube cube1 = new Cube();
        Cube cube2 = new Cube();       
        
        scene.root.addChild(cube1);
        scene.root.addChild(cube2);
        
        cube1.setColor(Color4f.BLUE);
        cube1.x = -1f;
        cube1.z = -2f;
        cube1.angleX = 22.5f;
        cube1.angleY = 22.5f;

        cube2.setColor(Color4f.RED);        
        cube2.x = 1f;
        cube2.z = -2f;
        cube2.angleX = 22.5f;
        cube2.angleY = 22.5f;
    }

    /*
    cd "...\LWJGL_PROJET_MINECRAFT"
    mvn clean                                                                                   
    mvn package
    mvn exec:java
    */
}
