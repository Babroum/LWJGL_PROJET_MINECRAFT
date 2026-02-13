package demo.Exceptions;

/**
 * Erreur retournée si une couleur est incorrecte
 */
public class IncorrectColorValue extends RuntimeException{
    public IncorrectColorValue(){
        super();
    }
    public IncorrectColorValue(String message){
        super(message);
    }
}
