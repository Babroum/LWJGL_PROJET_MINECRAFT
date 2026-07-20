package demo.Annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour signifier l'appartenance au thread OpenGL
 */
// La rétention permet de dire à quel moment les informations concernant l'annotation seront conservées
// Source permet de dire que l'annotation est seulement retenue dans le code source et oubliée pendant la compilation, on l'utilise dans le cas où l'annotation ne serait plus utile après la compilation
// Vu que l'annotation est seulement décorative, on s'en fout de la garder à la compilation
@Retention(RetentionPolicy.SOURCE) 

// Permet de préciser les entités sur lesquelles l'annotation sera utilisable, ici les méthodes et constructeurs
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR}) 

// Permet d'avoir une documentation de l'annotation dans la JavaDoc
@Documented 
// On créer notre propre annotation
public @interface OpenGLThread {
    // Permet juste d'indiquer quelle méthode tourne dans le thread, c'est comme un post-it
}
