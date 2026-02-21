package demo.Exceptions;

/**
 * Erreur retournée si un tableau est vide
 */
public class ArrayEmpty extends RuntimeException{
    public ArrayEmpty(){
        super();
    }
    public ArrayEmpty(String message){
        super(message);
    }
}
