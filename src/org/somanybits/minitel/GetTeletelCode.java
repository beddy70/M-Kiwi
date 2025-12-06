/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Générateur de codes Videotex.
 * <p>
 * Classe utilitaire statique qui génère les séquences d'échappement Videotex
 * sous forme de tableaux de bytes. Ces codes peuvent ensuite être envoyés
 * au Minitel via {@link MinitelConnection}.
 * </p>
 * 
 * <h2>Fonctions principales</h2>
 * <ul>
 *   <li>{@link #setCursor(int, int)} - Positionner le curseur</li>
 *   <li>{@link #setTextColor(int)} - Couleur du texte</li>
 *   <li>{@link #setBackgroundColor(int)} - Couleur de fond</li>
 *   <li>{@link #clearScreen()} - Effacer l'écran</li>
 *   <li>{@link #beep()} - Émettre un bip sonore</li>
 * </ul>
 * 
 * <h2>Exemple d'utilisation</h2>
 * <pre>{@code
 * byte[] codes = GetTeletelCode.setCursor(10, 5);
 * codes = concat(codes, GetTeletelCode.setTextColor(COLOR_RED));
 * codes = concat(codes, GetTeletelCode.writeString("Bonjour!"));
 * connection.writeBytes(codes);
 * }</pre>
 * 
 * @author Eddy Briere
 * @version 0.3
 * @see Teletel
 * @see MinitelConnection
 */
public final class GetTeletelCode {

    // Constantes reprises de Teletel
    public static final int PAGE_WIDTH = 40;
    public static final int PAGE_HEIGHT = 24;
    
    // Protection ligne 0 - désactivée par défaut (ligne 0 interdite)
    private static boolean lineZeroEnabled = false;

    public static final int COLOR_BLACK = 0x00;
    public static final int COLOR_RED = 0x01;
    public static final int COLOR_GREEN = 0x02;
    public static final int COLOR_YELLOW = 0x03;
    public static final int COLOR_BLUE = 0x04;
    public static final int COLOR_MAGENTA = 0x05;
    public static final int COLOR_CYAN = 0x06;
    public static final int COLOR_WHITE = 0x07;

    public static final int MODE_TEXT = 0x01;
    public static final int MODE_SEMI_GRAPH = 0x00;

    public static final byte MODE_VIDEOTEXT = 0x00;
    public static final byte MODE_MIXTE = 0x01;
    public static final byte MODE_STANDARD = 0x02;

    // Constructeur privé - classe utilitaire
    private GetTeletelCode() {
    }

    /**
     * Génère les codes pour écrire une chaîne de caractères
     */
    public static byte[] writeString(String text) {
        if (text == null) return new byte[0];
        return text.getBytes();
    }

    /**
     * Génère les codes pour définir la couleur du texte
     */
    public static byte[] setTextColor(int color) {
        return new byte[] {
            (byte) 0x1b,
            (byte) (0x40 + (byte) color)
        };
    }

    /**
     * Génère les codes pour définir la couleur de fond
     */
    public static byte[] setBGColor(int color) {
        return new byte[] {
            (byte) 0x1b,
            (byte) (0x50 + (byte) color)
        };
    }

    /**
     * Génère les codes pour positionner le curseur.
     * Par défaut, la ligne 0 est protégée et le curseur sera placé en ligne 1.
     * Utilisez {@link #enableLineZero(boolean)} pour autoriser l'écriture en ligne 0.
     * 
     * @param x Position horizontale (0-39)
     * @param y Position verticale (0-24, mais 0 est protégée par défaut)
     * @return Séquence de bytes pour positionner le curseur
     */
    public static byte[] setCursor(int x, int y) {
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        
        // Protection ligne 0 : si désactivée, forcer y >= 1
        if (!lineZeroEnabled && y == 0) {
            y = 1;
            System.out.println("⚠️ Ligne 0 protégée - curseur déplacé en ligne 1");
        }
        
        return new byte[] {
            (byte) 0x1f,
            (byte) (y + 0x40),
            (byte) (x + 0x40 + 1)
        };
    }
    
    /**
     * Active ou désactive l'écriture sur la ligne 0.
     * La ligne 0 du Minitel est la ligne de status système.
     * Y écrire peut causer des problèmes d'affichage.
     * 
     * @param enabled true pour autoriser l'écriture en ligne 0, false pour l'interdire
     */
    public static void enableLineZero(boolean enabled) {
        lineZeroEnabled = enabled;
        System.out.println("📺 Ligne 0 " + (enabled ? "activée" : "protégée"));
    }
    
    /**
     * Vérifie si l'écriture sur la ligne 0 est autorisée.
     * @return true si la ligne 0 est accessible, false sinon
     */
    public static boolean isLineZeroEnabled() {
        return lineZeroEnabled;
    }

    /**
     * Génère les codes pour positionner le curseur à l'origine
     */
    public static byte[] setCursorHome() {
        return new byte[] {
            (byte) 0x1e
        };
    }

    /**
     * Génère les codes pour activer/désactiver l'inversion vidéo
     * @param inverse true pour activer, false pour désactiver
     */
    public static byte[] setInverse(boolean inverse) {
        return new byte[] {
            (byte) 0x1b,
            (byte) (inverse ? 0x5d : 0x5c)  // ESC ] = inverse on, ESC \ = inverse off
        };
    }

    /**
     * Génère les codes pour déplacer le curseur (format ANSI)
     */
    public static byte[] moveCursorXY(int x, int y) {
        List<Byte> codes = new ArrayList<>();
        
        codes.add((byte) 0x1B);
        codes.add((byte) 0x5B);
        
        // Ajouter les bytes pour y (Pr)
        addByteP(codes, y);
        
        codes.add((byte) 0x3B);
        
        // Ajouter les bytes pour x (Pc)
        addByteP(codes, x);
        
        codes.add((byte) 0x48);
        
        // Convertir List<Byte> en byte[]
        byte[] result = new byte[codes.size()];
        for (int i = 0; i < codes.size(); i++) {
            result[i] = codes.get(i);
        }
        return result;
    }

    /**
     * Génère les codes pour effacer la ligne zéro
     */
    public static byte[] clearLineZero() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(setCursor(0, 0));
            baos.write(writeString(" ".repeat(40)));
        } catch (IOException e) {
            // Ne devrait pas arriver avec ByteArrayOutputStream
        }
        return baos.toByteArray();
    }

    /**
     * Génère les codes pour effacer l'écran (mode VIDEOTEXT)
     * Utilise FF (0x0C) qui est plus compatible que les séquences CSI
     */
    public static byte[] clear() {
        return new byte[] {
            (byte) 0x0c  // FF - Form Feed, efface l'écran en mode Videotex
        };
    }

    /**
     * Génère les codes pour effacer l'écran selon le mode
     */
    public static byte[] clear(int screenmode) {
        if (screenmode != MODE_VIDEOTEXT) {
            return new byte[] {
                (byte) 0x0c
            };
        } else {
            return new byte[] {
                (byte) 0x1b, (byte) 0x5B, (byte) 0x32, (byte) 0x4A,
                (byte) 0x1b, (byte) 0x5B, (byte) 0x48
            };
        }
    }

    /**
     * @param flag
     * @return 
     * @return  
     */
    public static byte[] setEcho(boolean flag) {
        // Séquence PRO2 pour activer/désactiver l'écho clavier
        // ESC PRO2 START/STOP RCPT_CLAVIER
        return new byte[] {
            (byte) 0x1b,      // ESC
            (byte) 0x3a,      // PRO2 (0x3A, pas 0x3B qui est PRO3)
            (byte) (flag ? 0x61 : 0x60), // START (0x61) ou STOP (0x60)
            (byte) 0x58       // RCPT_CLAVIER
        };
    }

    /**
     * Génère les codes pour activer/désactiver le clignotement
     */
    public static byte[] setBlink(boolean flag) {
        return new byte[] {
            (byte) 0x1b,
            (byte) (flag ? 0x48 : 0x49)
        };
    }

    /**
     * Génère les codes pour définir le mode (texte/semi-graphique)
     */
    public static byte[] setMode(int mode) {
        if (mode != MODE_TEXT && mode != MODE_SEMI_GRAPH) {
            mode = MODE_TEXT;
        }
        switch (mode) {
            case MODE_TEXT:
                return new byte[] {
                    (byte) 0x0f
                };
            case MODE_SEMI_GRAPH:
                return new byte[] {
                    (byte) 0x0e
                };
            default:
                break;
        }
        return new byte[] {
            (byte) (0x0e + mode)
        };
    }

    /**
     * Génère les codes pour définir le mode d'écran
     */
    public static byte[] setScreenMode(int screenmode) {
        switch (screenmode) {
            case MODE_MIXTE:
                return new byte[] {
                    (byte) 0x1B, (byte) 0x3A, (byte) 0x32, (byte) 0x7D
                };
            case MODE_STANDARD:
                return new byte[] {
                    (byte) 0x1B, (byte) 0x3A, (byte) 0x31, (byte) 0x7D
                };
            case MODE_VIDEOTEXT:
            default:
                return new byte[] {
                    (byte) 0x1B, (byte) 0x3A, (byte) 0x32, (byte) 0x7E
                };
        }
    }

    /**
     * Obtient la couleur de gris selon le niveau
     */
    public static int getGrey(int level) {
        level &= 0x7;
        
        switch (level) {
            case 0: return COLOR_BLACK;   // 0%
            case 1: return COLOR_BLUE;    // 40%
            case 2: return COLOR_RED;     // 50%
            case 3: return COLOR_MAGENTA; // 60%
            case 4: return COLOR_GREEN;   // 70%
            case 5: return COLOR_CYAN;    // 80%
            case 6: return COLOR_YELLOW;  // 90%
            case 7: return COLOR_WHITE;   // 100%
            default: return COLOR_BLACK;
        }
    }

    /**
     * Méthode utilitaire pour ajouter un entier sous forme de bytes (équivalent writeByteP)
     */
    private static void addByteP(List<Byte> codes, int value) {
        String valueStr = String.valueOf(value);
        for (char c : valueStr.toCharArray()) {
            codes.add((byte) c);
        }
    }

    /**
     * Combine plusieurs tableaux de bytes en un seul
     */
    public static byte[] combine(byte[]... arrays) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            for (byte[] array : arrays) {
                if (array != null) {
                    baos.write(array);
                }
            }
        } catch (IOException e) {
            // Ne devrait pas arriver avec ByteArrayOutputStream
        }
        return baos.toByteArray();
    }

    /**
     * Convertit un tableau de bytes en chaîne hexadécimale pour debug
     */
    public static String toHexString(byte[] bytes) {
        if (bytes == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }
    
    /**
     * Génère les codes pour afficher/masquer le curseur
     * @param show true pour afficher, false pour masquer
     */
    public static byte[] showCursor(boolean show) {
        // CON (0x11) = afficher curseur, COFF (0x14) = masquer curseur
        return new byte[] {
            (byte) (show ? 0x11 : 0x14)
        };
    }
    
    /**
     * Génère le code pour émettre un bip sonore (BEL)
     */
    public static byte[] beep() {
        return new byte[] { (byte) 0x07 };  // BEL
    }
}
