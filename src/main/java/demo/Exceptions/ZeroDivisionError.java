package demo.Exceptions;

/**
 * Erreur retournée s'il y a une division par 0
 */
public class ZeroDivisionError extends RuntimeException{
    public ZeroDivisionError(){
        super();
    }
    public ZeroDivisionError(String message){
        super(message);
    }
}

