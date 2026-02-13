package demo.Exceptions;

/**
 * Erreur retournée si un indice est incorrect
 */
public class IncorrectIndex extends RuntimeException{
    public IncorrectIndex(){
        super();
    }
    public IncorrectIndex(String message){
        super(message);
    }
}
