package demo;

import demo.Math.MatrixOperation;
import demo.Math.VectorOperation;
import demo.Types.Matrix4vf;
import demo.Types.Vector3f;
import demo.Types.Vector4f;

// https://www.3dgep.com/understanding-the-view-matrix/#Introduction

/**
 * Camera
 */
public class Camera {  

    /*
      ⎡rightx, upx, forwardx, positionx⎤
      ⎢righty, upy, forwardy, positiony⎥
      ⎢rightz, upz, forwardz, positionz⎥
      ⎣   0  ,  0 ,     0   ,     1    ⎦
    */
                                            
    public Matrix4vf mat = new Matrix4vf();

    public Camera(){
        
    }
    
    /**
     * Donne la viewMatrix de la camera
     * @param eye
     * @param target
     * @param up
     * @return
     */
    // param : cameraPos, cameraPos + cameraFront, cameraUp
    public Matrix4vf lookAt(Vector3f eye, Vector3f target, Vector3f up){

        Vector3f zaxis = VectorOperation.normalize(VectorOperation.sub(eye, target)); // "forward" vecteur
        Vector3f xaxis = VectorOperation.normalize(VectorOperation.cross(up, zaxis)); // "right" vecteur
        Vector3f yaxis = VectorOperation.cross(zaxis, xaxis);                         // "up" vecteur

        // Créer une viewMatrix 4x4 grace aux vecteur "forward", "right" et "up" et la position des yeux "eye"
        Matrix4vf viewMatrix = new Matrix4vf(
            new Vector4f(        xaxis.getValue(0),    yaxis.getValue(0),         zaxis.getValue(0), 0), 

            new Vector4f(        xaxis.getValue(1),    yaxis.getValue(1),         zaxis.getValue(1), 0),

            new Vector4f(        xaxis.getValue(2),    yaxis.getValue(2),         zaxis.getValue(2), 0),

            new Vector4f(-VectorOperation.dot(xaxis, eye), -VectorOperation.dot(yaxis, eye), -VectorOperation.dot(zaxis, eye), 1)
        );

        return viewMatrix;
    }

}
