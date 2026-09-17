package demo.Gui;

import demo.Game.GameSession;
import demo.Managers.GuiManager;
import demo.Rendering.Window;

/**
 * 
 * GuiFactory
 */
public class GuiFactory {

    /** Échelle globale du GUI*/
    public static float guiScale = 1.0f;

    /** Espacement entre les widgets */
    public static int widgetPadding = 20;

    /**
     * Constructeur d'un bouton
     * @param imageAliasNormal Alias de l'image quand le bouton n'est pas affecté
     * @param imageAliasHover Alias de l'image quand le bouton est survolé
     * @param imageAliasClick Alias de l'image quand le bouton est cliqué
     * @param text Texte sur le bouton
     * @param x Position relative en x du bouton
     * @param y Position relative en y du bouton
     * @param width Largeur du bouton
     * @param height Hauteur du bouton
     * @param onClick CallBack du bouton
     */
    public static ButtonGui createButton(String imageAliasNormal, String imageAliasHover, String imageAliasClick, String text, int x, int y, int width, int height, Runnable onClick){
        return new ButtonGui(imageAliasNormal, imageAliasHover, imageAliasClick, text, x, y, width, height, onClick);
    }

    /**
     * Créer le menu d'accueil
     * @return Le widget
     */
    public static ContainerGui createHomeMenu(){
        ContainerGui root = new ContainerGui(0, 0, Window.width, Window.height);

        BackgroundGui background = new BackgroundGui("background.home", 0, 0, Window.width, Window.height);
        root.addChild(background);

        ContainerGui menu = new ContainerGui(0, 0, (int)(Window.width / 3.2f), Window.height);
        root.addChild(menu);

        int buttonWidth  = (int)(200 * guiScale);
        int buttonHeight = (int)(50  * guiScale);
        int spacing = (int)(100 * guiScale); // l'espacement entre les boutons doit aussi suivre guiScale

        int buttonCount = 2; // play + quit
        int totalGroupHeight = buttonCount * buttonHeight + (buttonCount - 1) * spacing;

        int startY = (menu.height - totalGroupHeight) / 2; // centre le groupe entier verticalement dans menu.height

        ButtonGui playButton = new ButtonGui("gray.button.rectangle.normal",
                                            "gray.button.rectangle.hover",
                                            "gray.button.rectangle.pressed",
                                            "Play",
                                            center(menu.width, buttonWidth),
                                            startY,
                                            buttonWidth,
                                            buttonHeight,
                                            () -> {
                                                GameSession.start();
                                                GuiManager.closeScreen();
                                            });
        menu.addChild(playButton);

        ButtonGui quitButton = new ButtonGui("gray.button.rectangle.normal",
                                            "gray.button.rectangle.hover",
                                            "gray.button.rectangle.pressed",
                                            "Quit",
                                            center(menu.width, buttonWidth),
                                            playButton.y + buttonHeight + spacing, // même spacing que celui utilisé dans totalGroupHeight
                                            buttonWidth,
                                            buttonHeight,
                                            () -> {
                                                GuiManager.closeScreen();
                                                Window.exitWindow();
                                            });           
        menu.addChild(quitButton);
        return root;
    }

    public static ContainerGui createPauseMenu(){
        ContainerGui root = new ContainerGui(0, 0, Window.width, Window.height);

        ButtonGui quitButton = new ButtonGui("gray.button.rectangle.normal",
                                             "gray.button.rectangle.hover",
                                             "gray.button.rectangle.pressed",
                                             "Menu",
                                             (root.width/2) - 100, 
                                             (root.height/2) + 25,
                                             200,
                                             50,
                                             () -> {
                                                GameSession.stop();
                                                GuiManager.openScreen(() -> createHomeMenu());
                                            });
        root.addChild(quitButton);

        return root;
    }

    /**
     * Centre un élément par rapport à un autre
     * @param container L'élément à centrer
     * @param widget L'élément référence
     * @return L'emplacement en pixel de l'élément centré
     */
    public static int center(int container, int widget) {
        return (container - widget) / 2;
    }
}
