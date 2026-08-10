package demo.Gui;

/**
 * Une image simple
 * Background
 */
public class Background extends Widget{
    /** L'alias de l'image dans "imageAlias.json" */
    public String imageAlias;

    /**
     * Constructeur de l'image
     * @param imageAlias Alias de l'image
     */
    public Background(String imageAlias, int x, int y, int width, int height){
        super(x, y, width, height);
        this.imageAlias = imageAlias;
    }

    /**
     * {@inheritDoc}
     * <br>
     * Ne fait rien, l'image n'a pas de mise à jour
     */
    @Override
    public void update(float mouseX, float mouseY, boolean mousePressed){

    }

    @Override
    public void render(){
        GuiRenderer.drawImage(this.imageAlias, this.getAbsoluteX(), this.getAbsoluteY(), this.width, this.height);
    }
}
