#!/bin/bash
#
# Minitel-Serveur - Test des joysticks USB
# Usage: ./test_joystick.sh
#

echo "╔════════════════════════════════════════════════════════╗"
echo "║           TESTEUR DE JOYSTICKS USB                     ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""
echo "Quel joystick voulez-vous tester ?"
echo "  0 - Joystick 0 (/dev/input/js0)"
echo "  1 - Joystick 1 (/dev/input/js1)"
echo "  2 - Les deux joysticks"
echo ""
read -p "Votre choix [0/1/2]: " choice

case $choice in
    0)
        echo ""
        echo "Test du joystick 0..."
        java -cp Minitel.jar org.somanybits.minitel.tools.JoystickTester /dev/input/js0
        ;;
    1)
        echo ""
        echo "Test du joystick 1..."
        java -cp Minitel.jar org.somanybits.minitel.tools.JoystickTester /dev/input/js1
        ;;
    2)
        echo ""
        echo "Test des deux joysticks..."
        java -cp Minitel.jar org.somanybits.minitel.tools.JoystickTester /dev/input/js0 /dev/input/js1
        ;;
    *)
        echo ""
        echo "Choix invalide. Test du joystick 0 par défaut..."
        java -cp Minitel.jar org.somanybits.minitel.tools.JoystickTester /dev/input/js0
        ;;
esac
