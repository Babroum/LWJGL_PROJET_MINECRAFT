package demo.Math;

import demo.Types.Matrix4vf;
import demo.Types.Vector4f;

/**
 * Méthodes d'opérations sur matrices
*/
public class MatrixOperation {
    /**
     * Pre-multiplie un vecteur par une matrice à droite
     * @param mat
     * @param vec
     * @return un vecteur4f
     */
    // prend x et le multiplie par la 1er valeur de la 1er colonne 
    // puis y multiplier avec la 1er valeur de la 2eme colonne
    // ...
    // et fait la somme 
    // puis prend x et le multiplie par 2eme valeur de la 1er colonne
    // etc ...
    public static Vector4f leftVectorMatrixMultiplication(Matrix4vf mat, Vector4f vec){
        return new Vector4f(
            mat.getColumn(0).getValue(0) * vec.getValue(0) +
             mat.getColumn(1).getValue(0) * vec.getValue(1) +
              mat.getColumn(2).getValue(0) * vec.getValue(2) +
               mat.getColumn(3).getValue(0) * vec.getValue(3),
            mat.getColumn(0).getValue(1) * vec.getValue(0) +
             mat.getColumn(1).getValue(1) * vec.getValue(1) +
              mat.getColumn(2).getValue(1) * vec.getValue(2) +
               mat.getColumn(3).getValue(1) * vec.getValue(3),
            mat.getColumn(0).getValue(2) * vec.getValue(0) +
             mat.getColumn(1).getValue(2) * vec.getValue(1) +
              mat.getColumn(2).getValue(2) * vec.getValue(2) +
               mat.getColumn(3).getValue(2) * vec.getValue(3),
            mat.getColumn(0).getValue(3) * vec.getValue(0) +
             mat.getColumn(1).getValue(3) * vec.getValue(1) +
              mat.getColumn(2).getValue(3) * vec.getValue(2) +
               mat.getColumn(3).getValue(3) * vec.getValue(3)
        );
    }

    /**
     * Post-multiplie un vecteur par une matrice à droite
     * @param mat
     * @param vec
     * @return un vecteur4f
     */
    // prend x et le multiplie par la 1er valeur de la 1er colonne 
    // puis y multiplier avec la 2eme valeur de la 1er colonne
    // ...
    // et fait la somme 
    // puis prend x et le multiplie par 1er valeur de la 2eme colonne
    // etc ...
    public static Vector4f rightVectorMatrixMultiplication(Vector4f vec, Matrix4vf mat){
        return new Vector4f(
            vec.getValue(0) * mat.getColumn(0).getValue(0) +
             vec.getValue(1) * mat.getColumn(0).getValue(1) +
              vec.getValue(2)* mat.getColumn(0).getValue(2) + 
               vec.getValue(3) * mat.getColumn(0).getValue(3),
            vec.getValue(0) * mat.getColumn(1).getValue(0) +
             vec.getValue(1) * mat.getColumn(1).getValue(1) +
              vec.getValue(2)* mat.getColumn(1).getValue(2) + 
               vec.getValue(3) * mat.getColumn(1).getValue(3),
            vec.getValue(0) * mat.getColumn(2).getValue(0) +
             vec.getValue(1) * mat.getColumn(2).getValue(1) +
              vec.getValue(2)* mat.getColumn(2).getValue(2) + 
               vec.getValue(3) * mat.getColumn(2).getValue(3),
            vec.getValue(0) * mat.getColumn(3).getValue(0) +
             vec.getValue(1) * mat.getColumn(3).getValue(1) +
              vec.getValue(2)* mat.getColumn(3).getValue(2) + 
               vec.getValue(3) * mat.getColumn(3).getValue(3)
        );
    }
    /**
     * Multiplie deux matrices entre elles
     * @param mat1
     * @param mat2
     * @return une matrice4vf
     */
    public static Matrix4vf matrixMultiplication(Matrix4vf mat1, Matrix4vf mat2){
        return new Matrix4vf(
            MatrixOperation.leftVectorMatrixMultiplication(mat1, mat2.getColumn(0)),
            MatrixOperation.leftVectorMatrixMultiplication(mat1, mat2.getColumn(1)),
            MatrixOperation.leftVectorMatrixMultiplication(mat1, mat2.getColumn(2)),
            MatrixOperation.leftVectorMatrixMultiplication(mat1, mat2.getColumn(3))
        );
    }
}
