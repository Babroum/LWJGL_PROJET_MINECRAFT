package demo.Types;

import demo.Exceptions.IncorrectIndex;

/**
 * Un vecteur composé de 3 paramètres de type float
 */
public class Vector3f {
    private float[] values = new float[3];  

    /**
     * Créer un vecteur de 3 float
     * @param x float
     * @param y float
     * @param z float
     */
    public Vector3f(float x, float y, float z){
        this.values[0] = x;
        this.values[1] = y;
        this.values[2] = z;
    }
    /**
     * Créer un vecteur de 3 float à 0
     */
    public Vector3f(){
        this(0, 0, 0);
    }

    /**
     * Permet d'obtenir une valeur du vecteur
     * @param index
     * @return
     */
    public float getValue(int index){
        if (index < 0 || index >= 3){
            throw new IncorrectIndex();
        }
        return this.values[index];
    }
}
