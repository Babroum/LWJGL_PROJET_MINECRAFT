package demo.Gui;

public class FlexBoxGui extends WidgetGui{
    public enum Direction{
        ROW,
        COLUMN
    }
    public enum JustifyContent{
        CENTER,
        START,
        SPACE_BETWEEN
    }

    /** Nombre de widget enfant */
    public int childCount;
    /** Type de rangement des widgets enfants */
    public Direction dir;
    /** Espacement fixe entre chaque widget enfant */
    public int gap;
    /** Disposition des widgets enfants */
    public JustifyContent jc;

    /**
     * Constructeur d'une flexbox
     * @param x Position relative en x du bouton
     * @param y Position relative en y du bouton
     * @param width Largeur du bouton
     * @param height Hauteur du bouton
     * @param dir Type de rangement des widgets enfants
     * @param gap Espacement fixe entre chaque widget enfant
     * @param jc Disposition des widgets enfants
     */
    public FlexBoxGui(int x, int y, int width, int height, Direction dir, int gap, JustifyContent jc){
        super(x, y, width, height);
    }

    @Override
    public void addChild(WidgetGui childToAdd){
        this.childCount++;
        switch (this.dir) {
            case Direction.COLUMN:
                if (childCount == 0) {childToAdd.x = 0; childToAdd.y = 0; return;}

                // on calcule le nouvel emplacement
                int newY = 0;
                int newX = 0;
                for(WidgetGui child : this.children){
                    newY += child.height;
                    if (child.width > newX) {newX = child.width;}
                }

                // mais si il sera en dehors alors on le déplace à côté
                // en COLUMN seul la hauteur de la flexbox est prit en compte la largeur s'adaptera, inversement pour ROW
                if (newY + childToAdd.height > this.height) {
                    newY = 0;
                }else{
                    newX = 0;
                }
                break;
        
            default:
                break;
        }
    }

    /**
     * {@inheritDoc}
     * <br>
     * Ne fait rien
     */
    @Override
    public void update(float mouseX, float mouseY, boolean mousePressed){
    }

    /**
     * {@inheritDoc}
     * <br>
     * Ne fait rien
     */
    @Override
    public void render(){}
}
