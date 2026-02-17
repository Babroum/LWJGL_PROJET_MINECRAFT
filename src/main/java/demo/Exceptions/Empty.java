package demo.Exceptions;

/**
 * Erreur retournée si quelque chose est vide
 */
public class Empty extends RuntimeException{
    public Empty(){
        super();
    }
    public Empty(String message){
        super(message);
    }
}
