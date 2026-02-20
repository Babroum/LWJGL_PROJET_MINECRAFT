package demo.Exceptions;

/**
 * Erreur retournée si une coordonnées est incorrecte
 */
public class IncorrectCoordinates extends RuntimeException{
    public IncorrectCoordinates(){
        super();
    }
    public IncorrectCoordinates(String message){
        super(message);
    }
}
