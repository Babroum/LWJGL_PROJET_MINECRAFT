package demo.Exceptions;

public class IncorrectColorValue extends RuntimeException{
    public IncorrectColorValue(){
        super();
    }
    public IncorrectColorValue(String message){
        super(message);
    }
}
