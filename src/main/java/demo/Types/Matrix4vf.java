package demo.Types;

import demo.Exceptions.IncorrectIndex;

/**
 * Une Matrice de taille 4x4 de type Vector4f
 */
public class Matrix4vf {

    private Vector4f[] vec = new Vector4f[4];  

    /**
     * Créer une matrice de 4 vecteurs
     * @param col1 Vector4f
     * @param col2 Vector4f
     * @param col3 Vector4f
     * @param col4 Vector4f
     */
    public Matrix4vf(Vector4f col1, Vector4f col2, Vector4f col3, Vector4f col4){
        this.vec[0] = col1;
        this.vec[1] = col2;
        this.vec[2] = col3;
        this.vec[3] = col4;
    }
    /**
     * Créer une matrice de 4 vecteurs
     * @param col1 Vector4f   1, 0, 0, 0
     * @param col2 Vector4f   0, 1, 0, 0
     * @param col3 Vector4f   0, 0, 1, 0
     * @param col4 Vector4f   0, 0, 0, 1
     */
    public Matrix4vf(){
        this(new Vector4f(1, 0, 0),
             new Vector4f(0, 1, 0),
             new Vector4f(0, 0, 1),
             new Vector4f(0, 0, 0,1));
    }

    /**
     * Permet d'obtenir une colonne de la matrice
     * @param index int
     * @return Vector4f
     * @throws IncorrectIndex
     */
    public Vector4f getColumn(int index) throws IncorrectIndex{
        if (index < 0 || index >= 4){
            throw new IncorrectIndex();
        }
        return this.vec[index];
    }
}