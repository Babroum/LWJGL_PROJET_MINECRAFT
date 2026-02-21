package demo.Annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.SOURCE) 
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR}) 
@Documented 
public @interface ChunkThread {
}
