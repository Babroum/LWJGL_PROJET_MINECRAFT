# LWJGL-PROJET-MINECRAFT

Un projet graphique 3D utilisant LWJGL (Lightweight Java Game Library) pour créer une application type Minecraft.

## Prérequis

- **Java JDK 11 ou supérieur** - [Télécharger Java](https://www.oracle.com/java/technologies/downloads/)
- **Git** - Pour cloner le projet

⚠️ **Maven n'est pas obligatoire !** Le projet inclut Maven Wrapper (`mvnw.cmd` pour Windows), qui télécharge et utilise Maven automatiquement.

## Installation et démarrage

### 1. Cloner le projet
```bash
git clone <your-repo-url>
cd LWJGL_PROJET_MINECRAFT
```
OU CTRL+SHIFT+P et git clone 

### 2. Compiler et télécharger les dépendances

**⚠️ Les commandes doivent être exécutées à la racine du projet (où se trouve le `pom.xml`)**

```bash
# Sous Windows (PowerShell ou CMD)
mvnw.cmd clean install

# Sous Linux/Mac
./mvnw clean install
```

Maven téléchargera automatiquement toutes les dépendances du `pom.xml` (y compris LWJGL et ses natives).

### 3. Lancer les tests
```bash
# Sous Windows
mvnw.cmd test

# Sous Linux/Mac
./mvnw test
```

### 4. Générer le JAR exécutable
```bash
# Sous Windows
mvnw.cmd package

# Sous Linux/Mac
./mvnw package
```

## Commandes Maven utiles

| Commande | Description |
|----------|-------------|
| `mvnw clean` | Nettoie les fichiers générés |
| `mvnw compile` | Compile le code source |
| `mvnw test` | Lance les tests unitaires |
| `mvnw package` | Crée un JAR exécutable |
| `mvnw clean install` | Compile, teste et installe le projet |
| `mvnw exec:java` | Lance l'application directement |

## Contribution

Pour développer et faire des push :

SUR L'ONGLET GIT DE VSCODE CLIQUER SUR LE LOGO DE BRANCHE ET FAIRE : CREER BRANCH
<img width="530" height="65" alt="image" src="https://github.com/user-attachments/assets/77c5a6d3-66b0-42fb-90d6-577de112a77e" />
IL SUFFIT DE PUSH ET POUR MERGE IL FAUT ALLER SUR GITHUB

1. Créer une branche : `git checkout -b feature/ma-feature`
2. Faire vos modifications
3. Tester : `mvnw test`
4. Commit : `git commit -m "Description des changements"`
5. Push : `git push origin feature/ma-feature`

## Notes importantes

- Le fichier `dependency-reduced-pom.xml` est généré automatiquement par Maven - ne pas le modifier
- Le répertoire `target/` est généré à la compilation - il est ignoré par Git
- Les natives LWJGL sont téléchargées automatiquement et cachées localement

## AVANT DE PUSH UNE FEATURE
Il faut créer une issue avec le label feature, c'est comme une todo list en gros, pour savoir ce que l'on fait et qui le fait
