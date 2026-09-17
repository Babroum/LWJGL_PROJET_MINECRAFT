package demo.Managers;

import java.util.function.Supplier;

import demo.Gui.WidgetGui;

/**
 * S'occupe de contrôler tout les éléments GUI
 * GuiManager
 */
public class GuiManager {
    /**
     * Ecran actuel affiché, options, pause, rien ...
     */
    public static WidgetGui currentScreen;
    public static Supplier<WidgetGui> currentScreenCallBack;

    /**
     * Met à jour tout les éléments Gui
     * @param mouseX Position de la souris en X
     * @param mouseY Position de la souris en Y
     * @param mousePressed La souris est-elle pressée ?
     */
    public static void update(float mouseX, float mouseY, boolean mousePressed){
        if (GuiManager.currentScreen != null){
            GuiManager.currentScreen.updateAll(mouseX, mouseY, mousePressed);
        }
    }

    /**
     * Mise à jour graphique de tout les widgets (OpenGL)
     */
    public static void render(){
        if (GuiManager.currentScreen != null){
            GuiManager.currentScreen.renderAll();
        }
    }

    /**
     * Charge le bon écran Gui
     * @param screen Element Gui
     */
    public static void openScreen(Supplier<WidgetGui> screen) {
        GuiManager.currentScreenCallBack = screen;
        GuiManager.currentScreen = screen.get();
    }

    /**
     * Ferme l'écran Gui actuel
     */
    public static void closeScreen() {
        GuiManager.currentScreenCallBack = null;
        GuiManager.currentScreen = null;
    }

    /**
     * Retourne si il y a un écran affiché
     * @return Un écran est-il affiché ?
     */
    public static boolean hasScreen(){
        return GuiManager.currentScreen != null;
    }
}
