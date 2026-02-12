package demo; 

import java.util.Objects;

//clone d'objets 3D ayant la meme maille pour pas avoir 2 copies de listes
public class Clone3D extends Node3D {
    private Color4i color;
    private final Object3D cloned;
    public Clone3D(final Object3D cloned){
        this.cloned = Objects.requireNonNull(cloned, "cloned must not be null");
    }

    public Color4i getColor(){
        return this.color;
    }

    public void setColor(final Color4i color){
        this.color = Objects.requireNonNull(color, "color must not be null");
    }

    @OpenGLThread
    @Override
    public void drawSpecific(){
        this.color.apply();
        this.cloned.mesh.drawMesh();
    }
}