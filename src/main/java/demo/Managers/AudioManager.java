package demo.Managers;

import org.lwjgl.openal.*;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryUtil;

import demo.Others.FileLoader;

import java.nio.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.stb.STBVorbis.*;

// MERCI CLAUDE CAR C INCOMPREHENSIBLE OPENAL

public class AudioManager {

    private static long device;   // représente la carte son physique
    private static long context;  // environnement audio (comme un contexte OpenGL)
    private static ALCapabilities caps;

    // Stocke les buffers audio (données PCM brutes) par nom
    // ex: "music" -> buffer OpenAL contenant les données du fichier OGG
    private static Map<String, Integer> buffers = new HashMap<>();

    // Stocke les sources audio (les "joueurs") par nom
    // une source = un objet 3D dans l'espace qui émet un son
    // ex: "music" -> source OpenAL qui joue le buffer "music"
    private static Map<String, Integer> sources = new HashMap<>();

    private static List<Integer> tempSources = new ArrayList<>();

    /**
     * Initialise OpenAL.
     * Doit être appelé UNE SEULE FOIS avant tout chargement de son,
     * après GLFW mais avant la boucle de rendu.
     */
    public static void init() {
        // Ouvre le device audio par défaut (carte son du système)
        device = alcOpenDevice((ByteBuffer) null);

        // Récupère les capacités du device (extensions supportées etc.)
        ALCCapabilities deviceCaps = ALC.createCapabilities(device);

        // Crée un contexte audio et le rend actif
        // (comme alcMakeContextCurrent en OpenGL)
        context = alcCreateContext(device, (IntBuffer) null);
        alcMakeContextCurrent(context);

        // Initialise les capacités AL avec memCallocPointer
        // IMPORTANT : sans MemoryUtil::memCallocPointer le son ne fonctionne pas
        caps = AL.createCapabilities(deviceCaps, MemoryUtil::memCallocPointer);

        // Volume global de l'écouteur (le "joueur")
        // 1.0f = volume normal, 2.0f = double, etc.
        alListenerf(AL_GAIN, 1.0f);

        //System.out.println("AudioManager initialisé");
    }

    /**
     * Charge un fichier OGG depuis le classpath et le stocke sous un nom.
     * 
     * Le chargement se fait en 3 étapes :
     * 1. Lire le fichier OGG en ByteBuffer
     * 2. Décoder le OGG en données PCM brutes (ShortBuffer) via STB Vorbis
     * 3. Envoyer les données PCM dans un buffer OpenAL
     * 
     * @param name  nom du son pour le retrouver plus tard (ex: "music", "jump")
     * @param path  chemin depuis le classpath (ex: "/audio/music.ogg")
     */
    public static void loadSound(String name, String path) {
        try (STBVorbisInfo info = STBVorbisInfo.malloc()) {

            // Lire le fichier OGG depuis le classpath en ByteBuffer
            ByteBuffer vorbis = FileLoader.loadFileFromClasspathBB(path);

            // Ouvrir le décodeur STB Vorbis sur les données du fichier
            IntBuffer error = MemoryUtil.memAllocInt(1);
            long decoder = stb_vorbis_open_memory(vorbis, error, null);
            if (decoder == MemoryUtil.NULL) {
                System.out.println("Erreur chargement OGG : " + name + " (code " + error.get(0) + ")");
                MemoryUtil.memFree(error);
                MemoryUtil.memFree(vorbis);
                return;
            }

            // Lire les infos du fichier OGG (channels, sample rate)
            // channels   : 1 = mono, 2 = stéréo
            // sample_rate: nombre d'échantillons par seconde (ex: 44100)
            stb_vorbis_get_info(decoder, info);
            int channels = info.channels();

            // Allouer un ShortBuffer de la bonne taille pour recevoir les données PCM
            // taille = nombre d'échantillons total * nombre de channels
            ShortBuffer pcm = MemoryUtil.memAllocShort(stb_vorbis_stream_length_in_samples(decoder) * channels);

            // Décoder tout le fichier OGG en PCM dans le ShortBuffer
            stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);

            // Fermer le décodeur STB (on n'en a plus besoin)
            stb_vorbis_close(decoder);
            MemoryUtil.memFree(error);
            MemoryUtil.memFree(vorbis);

            // Choisir le format OpenAL selon le nombre de channels
            // MONO16   = 1 channel,  16 bits par échantillon
            // STEREO16 = 2 channels, 16 bits par échantillon
            int format = channels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;

            // Créer un buffer OpenAL et y envoyer les données PCM
            // Le buffer = les données audio stockées côté GPU/driver audio
            int buffer = alGenBuffers();
            alBufferData(buffer, format, pcm, info.sample_rate());

            MemoryUtil.memFree(pcm);

            // Créer une source OpenAL et la lier au buffer
            // La source = l'objet qui "joue" le son dans l'espace 3D
            int source = alGenSources();
            alSourcei(source, AL_BUFFER, buffer); // lie le buffer à la source
            alSourcef(source, AL_GAIN, 1.0f);     // volume par défaut

            alSourcei(source, AL_SOURCE_RELATIVE, AL_TRUE);
            alSource3f(source, AL_POSITION, 0f, 0f, 0f);

            // Stocker buffer et source pour les retrouver par nom
            buffers.put(name, buffer);
            sources.put(name, source);
            
            //System.out.println("Son chargé : " + name + " (" + channels + "ch, " + info.sample_rate() + "Hz)");

        } catch (Exception e) {
            System.out.println("Erreur chargement son '" + name + "' : " + e.getMessage());
        }
    }

    /**
     * Joue un son UNE FOIS depuis le début.
     * Idéal pour les effets sonores (saut, tir, collision...).
     * Si le son est déjà en cours, il repart depuis le début.
     * @param name nom du son chargé avec loadSound()
     */
    public static void play(String name) {
        Integer source = sources.get(name);
        if (source == null) {
            System.out.println("Son introuvable : " + name);
            return;
        }
        System.out.println("Playing : " + name + " source ID : " + source);
        alSourcei(source, AL_LOOPING, AL_FALSE); // pas de boucle
        alSourceStop(source);  // remet à zéro si déjà en cours
        alSourcePlay(source);  // joue le son

        int error = alGetError();
        if (error != AL_NO_ERROR) {
            System.out.println("Erreur OpenAL play : " + error);
        }
    }

    public static void playOnce(String name) {
        Integer buffer = buffers.get(name);
        if (buffer == null) {
            System.out.println("Son introuvable : " + name);
            return;
        }
        
        // Crée une nouvelle source temporaire à chaque appel
        int source = alGenSources();
        alSourcei(source, AL_BUFFER, buffer);
        alSourcei(source, AL_LOOPING, AL_FALSE);
        alSourcei(source, AL_SOURCE_RELATIVE, AL_TRUE);
        alSource3f(source, AL_POSITION, 0f, 0f, 0f);
        alSourcef(source, AL_GAIN, 1.0f);
        alSourcePlay(source);
        
        // Stocke la source temporaire pour la nettoyer plus tard
        tempSources.add(source);
    }

    /**
     * Joue un son EN BOUCLE indéfiniment.
     * Idéal pour la musique de fond ou les sons ambiants.
     * @param name nom du son chargé avec loadSound()
     */
    public static void playLoop(String name) {
        Integer source = sources.get(name);
        if (source == null) {
            System.out.println("Son introuvable : " + name);
            return;
        }
        alSourcei(source, AL_LOOPING, AL_TRUE); // active la boucle
        alSourcePlay(source);
    }

    /**
     * Met en pause un son.
     * Peut être repris avec resume().
     * @param name nom du son
     */
    public static void pause(String name) {
        Integer source = sources.get(name);
        if (source == null) return;
        alSourcePause(source);
    }

    /**
     * Reprend un son mis en pause.
     * @param name nom du son
     */
    public static void resume(String name) {
        Integer source = sources.get(name);
        if (source == null) return;
        alSourcePlay(source);
    }

    /**
     * Stoppe un son complètement.
     * Contrairement à pause(), remet la lecture au début.
     * @param name nom du son
     */
    public static void stop(String name) {
        Integer source = sources.get(name);
        if (source == null) return;
        alSourceStop(source);
    }

    /**
     * Change le volume d'un son.
     * @param name   nom du son
     * @param volume 0.0 = muet, 1.0 = normal, 2.0 = double, etc.
     */
    public static void setVolume(String name, float volume) {
        Integer source = sources.get(name);
        if (source == null) return;
        volume = Math.max(0.0f, volume);
        // Vérifier que la source est valide
        if (!alIsSource(source)) {
            System.out.println("ID de source invalide : " + source);
            return;
        }
        
        alSourcef(source, AL_GAIN, volume);
        
        // Vérifier que ça a bien été appliqué
        //float check = alGetSourcef(source, AL_GAIN);
        //System.out.println("Gain appliqué : " + check);
        
        // Vérifier les erreurs OpenAL
        int error = alGetError();
        if (error != AL_NO_ERROR) {
            System.out.println("Erreur OpenAL : " + error);
        }
    }

    /**
     * Retourne le volume d'un son.
     * @param name nom du son
     * @return float
     */
    public static float getVolume(String name) {
        Integer source = sources.get(name);
        if (source == null) return 0.0f;
        return alGetSourcef(source, AL_GAIN);
    }

    /**
     * Change le volume global de tous les sons.
     * Affecte toutes les sources sans exception.
     * @param volume 0.0 = muet, 1.0 = normal, 2.0 = double, etc.
     */
    public static void setMasterVolume(float volume) {
        volume = Math.max(0.0f, volume);
        alListenerf(AL_GAIN, volume);
        System.out.println("Master volume : " + alGetListenerf(AL_GAIN));
    }

    /**
     * Retourne le volume global de tous les sons.
     * @return float
     */
    public static float getMasterVolume() {
        return alGetListenerf(AL_GAIN);
    }

    /**
     * Vérifie si un son est en cours de lecture.
     * @param name nom du son
     * @return true si le son joue, false sinon
     */
    public static boolean isPlaying(String name) {
        Integer source = sources.get(name);
        if (source == null) return false;
        return alGetSourcei(source, AL_SOURCE_STATE) == AL_PLAYING;
    }

    public static void cleanupTempSources() {
        tempSources.removeIf(source -> {
            if (alGetSourcei(source, AL_SOURCE_STATE) != AL_PLAYING) {
                alDeleteSources(source);
                return true;
            }
            return false;
        });
    }

    /**
     * Libère toutes les ressources OpenAL.
     * Doit être appelé à la fermeture de l'application.
     */
    public static void cleanup() {
        // Stopper et supprimer toutes les sources
        for (int source : sources.values()) {
            alSourceStop(source);
            alDeleteSources(source);
        }
        // Supprimer tous les buffers
        for (int buffer : buffers.values()) {
            alDeleteBuffers(buffer);
        }
        // Libérer le contexte et fermer le device
        MemoryUtil.memFree(caps.getAddressBuffer());
        alcDestroyContext(context);
        alcCloseDevice(device);

        //System.out.println("AudioManager nettoyé");
    }
}