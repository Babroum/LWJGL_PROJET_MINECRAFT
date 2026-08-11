package demo.Gui;

/**
 * 
 * GuiFactory
 */
public class GuiFactory {

    /**
     * Constructeur d'un bouton
     * @param imageAliasNormal Alias de l'image quand le bouton n'est pas affecté
     * @param imageAliasHover Alias de l'image quand le bouton est survolé
     * @param imageAliasClick Alias de l'image quand le bouton est cliqué
     * @param x Position relative en x du bouton
     * @param y Position relative en y du bouton
     * @param width Largeur du bouton
     * @param height Hauteur du bouton
     * @param onClick CallBack du bouton
     */
    public static ButtonGui createButton(String imageAliasNormal, String imageAliasHover, String imageAliasClick, int x, int y, int width, int height, Runnable onClick){
        return new ButtonGui(imageAliasNormal, imageAliasHover, imageAliasClick, x, y, width, height, onClick);
    }
}
