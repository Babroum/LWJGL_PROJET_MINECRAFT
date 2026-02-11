package demo.Math;

import demo.Exceptions.ZeroDivisionError;
import demo.Types.Vector3f;
import demo.Types.Vector4f;

/**
 * Méthodes d'opérations sur vecteurs
*/
public class VectorOperation {

    /**
     * Fait la soustraction entre deux vecteurs
     * @param vec1 Vector3f
     * @param vec2 Vector3f
     * @return Vector3f
     */
    public static Vector3f sub(Vector3f vec1, Vector3f vec2){
        return new Vector3f(
            vec1.getValue(0) - vec2.getValue(0),
            vec1.getValue(1) - vec2.getValue(1),
            vec1.getValue(2) - vec2.getValue(2)
        );
    }
    /**
     * Fait la soustraction entre deux vecteurs
     * @param vec1 Vector4f
     * @param vec2 Vector4f
     * @return Vector4f
     */
    public static Vector4f sub(Vector4f vec1, Vector4f vec2){
        return new Vector4f(
            vec1.getValue(0) - vec2.getValue(0),
            vec1.getValue(1) - vec2.getValue(1),
            vec1.getValue(2) - vec2.getValue(2),
            vec1.getValue(3) - vec2.getValue(3)
        );
    }

    /**
     * Fait l'addition entre deux vecteurs
     * @param vec1 Vector3f
     * @param vec2 Vector3f
     * @return Vector3f
     */
    public static Vector3f add(Vector3f vec1, Vector3f vec2){
        return new Vector3f(
            vec1.getValue(0) + vec2.getValue(0),
            vec1.getValue(1) + vec2.getValue(1),
            vec1.getValue(2) + vec2.getValue(2)
        );
    }
    /**
     * Fait l'addition entre deux vecteurs
     * @param vec1 Vector4f
     * @param vec2 Vector4f
     * @return Vector4f
     */
    public static Vector4f add(Vector4f vec1, Vector4f vec2){
        return new Vector4f(
            vec1.getValue(0) + vec2.getValue(0),
            vec1.getValue(1) + vec2.getValue(1),
            vec1.getValue(2) + vec2.getValue(2),
            vec1.getValue(3) + vec2.getValue(3)
        );
    }

    /**
     * Fait la division entre un vecteur et un float
     * @param vec1 Vector3f
     * @param vec2 Vector3f
     * @return Vector3f
     */
    public static Vector3f div(Vector3f vec1, float f) throws ZeroDivisionError{
        if (f == 0){
            throw new ZeroDivisionError();
        }
        return new Vector3f(
            vec1.getValue(0) / f,
            vec1.getValue(1) / f,
            vec1.getValue(2) / f
        );
    }

    /**
     * Calcule la magnitude d'un vecteur
     * @param vec Vector3f
     * @return float
     */
    public static float magnitude(Vector3f vec){
        return (float)Math.sqrt(Math.pow(vec.getValue(0), 2) + 
                                Math.pow(vec.getValue(1), 2) + 
                                Math.pow(vec.getValue(3), 2));
    }
    /**
     * Calcule la magnitude d'un vecteur
     * @param vec Vector4f
     * @return float
     */
    public static float magnitude(Vector4f vec){
        return (float)Math.sqrt(Math.pow(vec.getValue(0), 2) + 
                                Math.pow(vec.getValue(1), 2) +
                                Math.pow(vec.getValue(2), 2) + 
                                Math.pow(vec.getValue(3), 2));
    }

    /**
     * Normalise un vecteur
     * @param vec Vector3f
     * @return Vector3f
     * @throws ZeroDivisionError
     */
    public static Vector3f normalize(Vector3f vec) throws ZeroDivisionError{
        float mag = VectorOperation.magnitude(vec);
        return VectorOperation.div(vec, mag);
    }

    /**
     * Produit vectoriel de deux vecteurs
     * @param vec1 Vector3f
     * @param vec2 Vector3f
     * @return Vector3f
     */
    public static Vector3f cross(Vector3f vec1, Vector3f vec2){
        return new Vector3f(
            vec1.getValue(1) * vec2.getValue(2) - vec1.getValue(2) * vec2.getValue(1),
            vec1.getValue(2) * vec2.getValue(0) - vec1.getValue(0) * vec2.getValue(2),
            vec1.getValue(0) * vec2.getValue(1) - vec1.getValue(1) * vec2.getValue(0)
        );
    }

    /**
     * Produit scalaire de deux vecteurs
     * @param vec1 Vector3f
     * @param vec2 Vector3f
     * @return float
     */
    public static float dot(Vector3f vec1, Vector3f vec2){
        return vec1.getValue(0) * vec2.getValue(0) +
               vec1.getValue(1) * vec2.getValue(1) +
               vec1.getValue(2) * vec2.getValue(2);

    }
}
