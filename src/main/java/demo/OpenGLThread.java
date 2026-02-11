package demo;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// la retention permet de dire a quel moment les informations concernant l'annotation seront conservées
// Source permet de dire que l'annotation est seulement retenu dans le code source et oublié pendant la compilation on l'utilise dans le cas ou l'annotation n'est plus utile après la compilation
// Vu que l'annotation est seulement décorative on s'en fout de la garder a la compilation
@Retention(RetentionPolicy.SOURCE) 

// permet de préciser les entités sur lesquelles l'annotation sera utilisable, ici les méthodes
@Target(ElementType.METHOD) 

// permet d'avoir une documentation de l'annotation dans la JavaDoc
@Documented 
// on creér notre propre annotation
public @interface OpenGLThread {
    // permet juste d'indiquer quel méthode tourne dans le thread, c'est comme un post-it
}
