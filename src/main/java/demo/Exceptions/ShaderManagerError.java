package demo.Exceptions;

/**
 * Erreur retournée si un shader ne compile pas (code glsl incorrect)
 */
public class ShaderManagerError extends RuntimeException{
    public ShaderManagerError(){
        super();
    }
    public ShaderManagerError(String message){
        super(message);
    }
}

