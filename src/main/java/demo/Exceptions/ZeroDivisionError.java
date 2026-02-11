package demo.Exceptions;

/**
 * Erreur retourner si il y a une division par 0
 */
public class ZeroDivisionError extends RuntimeException{
    public ZeroDivisionError(){
        super();
    }
    public ZeroDivisionError(String message){
        super(message);
    }
}

