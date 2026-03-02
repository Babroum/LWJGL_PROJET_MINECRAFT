package demo.Geometry; 

import java.util.Objects;

import demo.Types.Color4f;

/**
 * Classe d'object cloné étant des Object3D <br>
 * Pas utile pour les blocks <br>
 * Peut servir pour les arbres, mobs, ...
 */
// A modifier 
@Deprecated
public class Clone3D extends Node3D {
    private Color4f color = Color4f.WHITE;
    private final Object3D cloned;
    
    /**
     * Créer un clone de l'object passé en paramètre <br>
     * Garde la même texture
     * @param cloned Object3D
     */
    public Clone3D(final Object3D cloned){
        this.cloned = Objects.requireNonNull(cloned, "Le clone ne doit pas être null");
    }

    /**
     * Retourne l'object cloné
     * @return Object3D
     */
    public Object3D getCloned(){
        return this.cloned;
    }
    
    /**
     * Retourne la couleur du clone
     * @return Color4f
     */
    public Color4f getColor(){
        return this.color;
    }

    /**
     * Définit la couleur du texte
     * @param color Color4f
     */
    public void setColor(final Color4f color){
        this.color = Objects.requireNonNull(color, "color must not be null");
    }
}