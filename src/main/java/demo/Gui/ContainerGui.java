package demo.Gui;

/**
 * Conteneur qui permet de structuré les widgets, fonctionne comme un dossier 
 * ContainerGui
 */
public class ContainerGui extends WidgetGui{
    public ContainerGui(int x, int y, int width, int height){
        super(x, y, width, height);
    }

    /**
     * {@inheritDoc}
     * <br>
     * Ne fait rien
     */
    public void update(float mouseX, float mouseY, boolean mousePressed){}
    
    /**
     * {@inheritDoc}
     * <br>
     * Ne fait rien
     */
    public void render(){}
}
