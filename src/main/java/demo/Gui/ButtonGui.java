package demo.Gui;

/**
 * Un Bouton intéractif qui change dynamiquement d'image par rapport aux actions de la souris sur lui
 * Button
 */
public class ButtonGui extends WidgetGui {
    /** L'alias de l'image en mode normal dans "imageAlias.json" */
    public String imageAliasNormal;
    /** L'alias de l'image en mode hover dans "imageAlias.json" */
    public String imageAliasHover;
    /** L'alias de l'image en mode click dans "imageAlias.json" */
    public String imageAliasClick;
    /** L'alias de l'image à affiché dans "imageAlias.json" */
    public String imageAliasNow;
    /** CallBack à effectué au moment du click */
    public Runnable onClick;
    /** Etat du bouton */
    public boolean isClicked;

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
    public ButtonGui(String imageAliasNormal, String imageAliasHover, String imageAliasClick, int x, int y, int width, int height, Runnable onClick){
        super(x, y, width, height);
        this.imageAliasNormal = imageAliasNormal;
        this.imageAliasHover = imageAliasHover;
        this.imageAliasClick = imageAliasClick;
        this.imageAliasNow = imageAliasNormal;
        this.onClick = onClick;
    }

    /**
     * {@inheritDoc}
     * <br>
     * Modifie l'image du bouton grâce aux interactions de la souris
     */
    @Override
    public void update(float mouseX, float mouseY, boolean mousePressed){
        if(this.isMouseInWidget(mouseX, mouseY) && mousePressed && !isClicked){
            // bouton cliqué
            this.isClicked = true;
            this.imageAliasNow = this.imageAliasClick;
            this.onClick.run(); // appel le callback
        }else if (this.isMouseInWidget(mouseX, mouseY) && !mousePressed){
            // bouton survolé
            this.isClicked = false;
            this.imageAliasNow = this.imageAliasHover;
        }else{
            // bouton normal
            this.imageAliasNow = this.imageAliasNormal;
        }
    }

    @Override
    public void render(){
        GuiRenderer.drawImage(imageAliasNow, this.getAbsoluteX(), this.getAbsoluteY(), this.width, this.height);
    }
}
