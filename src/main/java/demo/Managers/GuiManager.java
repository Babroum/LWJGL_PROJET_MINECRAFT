package demo.Managers;

import demo.Gui.WidgetGui;

public class GuiManager {
    public static WidgetGui currentScreen;

    public static void update(float mouseX, float mouseY, boolean mousePressed){
        if (GuiManager.currentScreen != null){
            GuiManager.currentScreen.updateAll(mouseX, mouseY, mousePressed);
        }
    }

    public static void render(){
        if (GuiManager.currentScreen != null){
            GuiManager.currentScreen.renderAll();
        }
    }

    public static void openScreen(WidgetGui screen) {
        currentScreen = screen;
    }

    public static void closeScreen() {
        currentScreen = null;
    }
}
