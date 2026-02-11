package demo.Types;

import demo.Exceptions.IncorrectIndex;

/**
 * Un vecteur composé de 4 paramètres de type float
 */
public class Vector4f {

    private float[] values = new float[4];  

    public Vector4f(float x, float y, float z, float w){
        this.values[0] = x;
        this.values[1] = y;
        this.values[2] = z;
        this.values[3] = w;
    }
    public Vector4f(float x, float y, float z){
        this(x, y, z, 0);
    }
    public Vector4f(){
        this(0, 0, 0);
    }

    public float getValue(int index){
        if (index < 0 || index >= 4){
            throw new IncorrectIndex();
        }
        return this.values[index];
    }
}
