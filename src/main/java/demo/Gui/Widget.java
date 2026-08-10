package demo.Gui;

import java.util.ArrayList;
import java.util.List;

/**
 * Base simple pour un Widget
 * Widget
 */
public abstract class Widget {
    /** Position en x à l'écran basée sur le parent */
    public int x;
    /** Position en y à l'écran basée sur le parent */
    public int y;
    /** Largeur du widget */
    public int width;
    /** Hauteur du widget */
    public int height;
    /** Enfant du widget */
    public List<Widget> children;
    /** Parent du widget */
    public Widget parent;

    /**
     * Constructeur d'un widget
     * @param x Position relative en x du widget
     * @param y Position relative en y du widget
     * @param width Largeur du widget
     * @param height Hauteur du widget
     */
    public Widget(int x, int y, int width, int height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.children = new ArrayList<Widget>();
    }
    
    /**
     * Mise à jour du widget (logique), exemple : changement de textures basé sur un événement
     * @param mouseX Position de la souris en X
     * @param mouseY Position de la souris en Y
     * @param mousePressed La souris est-elle pressée ?
     */
    public abstract void update(float mouseX, float mouseY, boolean mousePressed);

    /** Mise à jour du widget (graphique), ce que fait OpenGl à chaque frame */
    public abstract void render();

    /** Mise à jour du widget (logique), exemple : changement de textures basé sur un événement, et des enfant du widget de façon récursive
     * @param mouseX Position de la souris en X
     * @param mouseY Position de la souris en Y
     * @param mousePressed La souris est-elle pressée ?
     */
    public void updateAll(float mouseX, float mouseY, boolean mousePressed){
        this.update(mouseX, mouseY, mousePressed);
        for (Widget child : this.children){
            child.updateAll(mouseX, mouseY, mousePressed);
        }
    }

    /** Mise à jour du widget (graphique), ce que fait OpenGL à chaque frame, et des enfants du widget de façon récursive */
    public void renderAll(){
        this.render();
        for (Widget child : this.children){
            child.renderAll();
        }
    }

    /** Ajoute un enfant au widget et définit son parent, qui est le widget actuel */
    public void addChild(Widget child){
        child.parent = this;
        this.children.add(child);
    }

    /** Retire un enfant au widget et supprime ce widget de la variable « parent » de l'enfant */
    public void removeChild(Widget child){
        child.parent = null;
        this.children.remove(child);
    }

    /**
     * Retourne la position absolue du widget en X
     * @return La position X
     */
    public int getAbsoluteX(){
        if (this.parent == null){
            return this.x;
        }else{
            return this.x + this.parent.x;
        }
    }
    
    /**
     * Retourne la position absolue du widget en Z
     * @return La position Z
     */
    public int getAbsoluteY(){
        if (this.parent == null){
            return this.y;
        }else{
            return this.y + this.parent.y;
        }
    }

    /**
     * Vérifie si la souris est dans le widget
     * @param mouseX Position de la souris en x
     * @param mouseY Position de la souris en y
     * @return la souris est-elle dans le widget ?
     */
    public boolean isMouseInWidget(float mouseX, float mouseY){
        if((mouseX > (float) this.getAbsoluteX()) && (mouseX < (float) this.getAbsoluteX() + (float) this.width) 
        && (mouseY > (float) this.getAbsoluteY()) && (mouseY < (float) this.getAbsoluteY() + (float) this.height)){
            return true;
        }else{
            return false;
        }
    }
}
