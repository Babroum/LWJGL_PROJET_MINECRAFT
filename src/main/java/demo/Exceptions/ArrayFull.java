package demo.Exceptions;

/**
 * Erreur retournée si un tableau est plein
 */
public class ArrayFull extends RuntimeException{
    public ArrayFull(){
        super();
    }
    public ArrayFull(String message){
        super(message);
    }
}
