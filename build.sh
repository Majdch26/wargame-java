#!/bin/sh
# Compile le jeu et crée wargame.jar (Java 17 ou plus).
#   ./build.sh          compile et crée wargame.jar
#   ./build.sh test     lance les tests
#   ./build.sh simu     fait jouer l'IA contre elle-même (200 parties par niveau)
#   ./build.sh doc      génère la Javadoc dans doc/
set -e
cd "$(dirname "$0")"

rm -rf build
mkdir -p build
javac -encoding UTF-8 -d build src/wargame/*.java
cp -r src/wargame/images src/wargame/sound.wav build/wargame/
jar cfe wargame.jar wargame.FenetreJeu -C build .
echo "wargame.jar créé : java -jar wargame.jar"

case "$1" in
    test) java -cp wargame.jar wargame.TestWargame ;;
    simu) java -cp wargame.jar wargame.Simulation 200 ;;
    doc)  javadoc -quiet -Xdoclint:none -encoding UTF-8 -charset UTF-8 -docencoding UTF-8 -d doc -sourcepath src wargame
          echo "Javadoc : doc/index.html" ;;
esac
