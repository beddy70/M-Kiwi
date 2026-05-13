# Package `org.somanybits.minitel.hardware`

Ce package regroupe les pilotes bas niveau du Shield M-Kiwi : LEDs, boutons-poussoirs et afficheur OLED.
Tous les composants sont conçus pour le **Raspberry Pi 2 Model B (BCM2836)** sous **Raspberry Pi OS Bookworm (Linux 6.x)**.

## Sommaire

- [Prérequis matériels](#prérequis-matériels)
- [GPIOLed — Contrôle des LEDs](#gpioled--contrôle-des-leds)
- [GPIOButton — Boutons-poussoirs](#gpiobutton--boutons-poussoirs)
- [OLEDDisplay — Pilote SSD1306 bas niveau](#oleddisplay--pilote-ssd1306-bas-niveau)
- [OLEDClient — Afficheur OLED haut niveau](#oledclient--afficheur-oled-haut-niveau)
- [Exemple combiné](#exemple-combiné)

---

## Prérequis matériels

### Brochage Shield M-Kiwi

| Composant | GPIO BCM | Pin Header | Notes |
|-----------|----------|------------|-------|
| LED 0     | GPIO 5   | Pin 29     | active-high, R 330Ω vers GND |
| LED 1     | GPIO 6   | Pin 31     | active-high, R 330Ω vers GND |
| LED 2     | GPIO 12  | Pin 32     | active-high, R 330Ω vers GND |
| LED 3     | GPIO 13  | Pin 33     | active-high, R 330Ω vers GND |
| BTN 0     | GPIO 20  | Pin 38     | active-low, pull-up interne  |
| BTN 1     | GPIO 21  | Pin 40     | active-low, pull-up interne  |
| BTN 2     | GPIO 26  | Pin 37     | active-low, pull-up interne  |
| OLED SDA  | GPIO 2   | Pin 3      | I2C bus 1                    |
| OLED SCL  | GPIO 3   | Pin 5      | I2C bus 1                    |

### Logiciel requis

```bash
# Droits sysfs GPIO (reconnexion requise après)
sudo usermod -aG gpio $USER

# Pull-up boutons
sudo apt-get install raspi-gpio

# I2C pour l'OLED
sudo raspi-config   # Interface Options -> I2C -> Enable
```

### Mécanisme GPIO (Linux 6.x)

Sur Linux 6.x, les GPIO ne s'exportent plus avec leur numéro BCM brut.
Il faut lire la base dans `/sys/class/gpio/gpiochip512/base` (valeur typique : 512)
et additionner le numéro BCM : `sysfs_num = base + bcm_pin`.

Les classes `GPIOLed` et `GPIOButton` effectuent ce calcul automatiquement.

---

## GPIOLed — Contrôle des LEDs

**Fichier :** `GPIOLed.java`

Contrôle 4 LEDs via sysfs GPIO. Chaque démarrage effectue un unexport/re-export
propre pour garantir un état initial sain.

### API

| Méthode | Description |
|---------|-------------|
| `boolean init()` | Initialise le sysfs GPIO. Retourne `false` si indisponible. |
| `boolean isAvailable()` | Vrai si l'init a réussi. |
| `void set(int index, boolean on)` | Allume (`true`) ou éteint (`false`) la LED `index` (0-3). |
| `void toggle(int index)` | Inverse l'état de la LED `index`. |
| `void allOn()` | Allume les 4 LEDs. |
| `void allOff()` | Éteint les 4 LEDs. |
| `boolean getState(int index)` | Retourne l'état courant de la LED. |
| `void close()` | Éteint toutes les LEDs et libère les ressources. |

### Exemple — clignotement

```java
GPIOLed leds = new GPIOLed();
if (!leds.init()) {
    System.out.println("LEDs non disponibles");
    return;
}

// Allumer toutes les LEDs
leds.allOn();
Thread.sleep(500);

// Éteindre une par une
for (int i = 0; i < GPIOLed.COUNT; i++) {
    leds.set(i, false);
    Thread.sleep(200);
}

// Basculer la LED 2
leds.toggle(2);

leds.close();
```

### Exemple — animation chenillard

```java
GPIOLed leds = new GPIOLed();
if (!leds.init()) return;

// Aller-retour
for (int i = 0; i < GPIOLed.COUNT; i++) {
    leds.set(i, true);
    Thread.sleep(150);
    leds.set(i, false);
}
for (int i = GPIOLed.COUNT - 2; i >= 0; i--) {
    leds.set(i, true);
    Thread.sleep(150);
    leds.set(i, false);
}

leds.close();
```

### Exemple — heartbeat (thread daemon)

```java
GPIOLed leds = new GPIOLed();
if (!leds.init()) return;

Thread heartbeat = new Thread(() -> {
    try {
        while (!Thread.currentThread().isInterrupted()) {
            leds.set(3, true);
            Thread.sleep(100);
            leds.set(3, false);
            Thread.sleep(900);
        }
    } catch (InterruptedException ignored) {}
}, "heartbeat");
heartbeat.setDaemon(true);
heartbeat.start();
```

---

## GPIOButton — Boutons-poussoirs

**Fichier :** `GPIOButton.java`

Capture les appuis et relâchements de 3 boutons via un thread de polling à 10 ms.
Les pull-up internes sont activés automatiquement via `raspi-gpio set <BCM> ip pu`.

### API

| Méthode | Description |
|---------|-------------|
| `void setListener(Listener l)` | Définit le listener (avant ou après `init()`). |
| `boolean init()` | Configure le sysfs GPIO et démarre le polling. |
| `boolean isAvailable()` | Vrai si l'init a réussi. |
| `boolean isPressed(int index)` | Lit l'état instantané du bouton (sans événement). |
| `void close()` | Arrête le polling et libère les ressources. |

### Interface Listener

```java
public interface Listener {
    void onPressed(int index);   // front descendant HIGH→LOW
    void onReleased(int index);  // front montant  LOW→HIGH
}
```

### Exemple — affichage des événements

```java
GPIOButton buttons = new GPIOButton();

buttons.setListener(new GPIOButton.Listener() {
    @Override
    public void onPressed(int index) {
        System.out.println("BTN " + index
            + " (GPIO " + GPIOButton.GPIO_PINS[index] + ") pressé");
    }
    @Override
    public void onReleased(int index) {
        System.out.println("BTN " + index + " relâché");
    }
});

if (buttons.init()) {
    System.out.println("En attente de pression... (Ctrl-C pour quitter)");
    Thread.sleep(30_000);
    buttons.close();
}
```

### Exemple — lecture instantanée (sans listener)

```java
GPIOButton buttons = new GPIOButton();
buttons.init();

// Polling manuel dans la boucle principale
while (true) {
    if (buttons.isPressed(0)) System.out.println("BTN 0 tenu");
    if (buttons.isPressed(1)) System.out.println("BTN 1 tenu");
    Thread.sleep(50);
}
```

---

## OLEDDisplay — Pilote SSD1306 bas niveau

**Fichier :** `OLEDDisplay.java`

Pilote direct du SSD1306 via I2C (Pi4J v2 / linuxfs). Utilise un buffer interne de
1024 octets avec envoi unique (`flush()`). La police intégrée est la 5×7 Adafruit GFX
(ASCII 0x20–0x7E), soit 21 caractères × 8 lignes sur un écran 128×64.

### API

| Méthode | Description |
|---------|-------------|
| `boolean init()` | Initialise Pi4J v2 et le SSD1306. |
| `boolean isAvailable()` | Vrai si l'écran répond. |
| `void clear()` | Efface le buffer interne (pas l'écran). |
| `void flush()` | Envoie le buffer vers l'écran (1033 octets I2C). |
| `void drawChar(char c, int col, int row)` | Dessine un caractère (col 0-20, row 0-7). |
| `void drawText(String text, int col, int row)` | Dessine une chaîne depuis `col` sur `row`. |
| `void displayLines(String[] lines)` | Efface et affiche jusqu'à 8 lignes. |
| `void clearScreen()` | Efface l'écran (clear + flush). |
| `void close()` | Libère Pi4J. |

**Constantes utiles :**

```java
OLEDDisplay.CHARS_PER_LINE  // 21 — caractères par ligne
OLEDDisplay.MAX_LINES       // 8  — lignes disponibles
OLEDDisplay.DEFAULT_ADDRESS // 0x3C
```

### Exemple — texte multi-lignes

```java
OLEDDisplay oled = new OLEDDisplay();
if (!oled.init()) {
    System.out.println("OLED non disponible");
    return;
}

oled.displayLines(new String[]{
    "M-Kiwi v0.7",
    "localhost:8080",
    "",
    "-------------------",
    "J1: js0",
    "J2: ---"
});

Thread.sleep(5000);
oled.clearScreen();
oled.close();
```

### Exemple — dessin caractère par caractère

```java
OLEDDisplay oled = new OLEDDisplay();
oled.init();

oled.clear();
oled.drawText("Hello, Minitel!", 0, 0);
oled.drawText("Ligne 2", 3, 2);          // décalé de 3 colonnes
oled.drawChar('*', 10, 4);
oled.flush();  // un seul transfert I2C
```

---

## OLEDClient — Afficheur OLED haut niveau

**Fichier :** `OLEDClient.java`

Gestionnaire OLED orienté événements, intégré dans `MinitelClient`.
Toutes les mises à jour sont sérialisées dans un unique thread daemon `oled-client`
— aucun risque de conflit I2C ni de blocage des threads appelants.

### Mise en page fixe (6 lignes)

```
┌─────────────────────┐
│ M-Kiwi v0.7.3       │  ligne 0 : version
│ localhost:8080       │  ligne 1 : serveur:port
│ /jeux/tetris.vtml   │  ligne 2 : URL courante
│ ---------------------│  ligne 3 : séparateur
│ J1: js0       B:3   │  ligne 4 : joystick 0 + bouton
│ J2: ---             │  ligne 5 : joystick 1
└─────────────────────┘
```

### API

| Méthode | Description |
|---------|-------------|
| `OLEDClient(String version, String server, int port)` | Constructeur. |
| `boolean init()` | Initialise l'écran et affiche le premier rendu. |
| `boolean isAvailable()` | Vrai si l'écran répond. |
| `void onNavigate(String url)` | Appelé à chaque changement de page VTML. |
| `void onJoystick(int idx, String device, boolean connected)` | Connexion/déconnexion joystick. |
| `void onButton(int idx, int button)` | Appui bouton joystick (affiché 2 s). |
| `void close()` | Arrête le thread de rendu et libère l'écran. |

### Exemple — intégration dans un client

```java
OLEDClient oled = new OLEDClient("0.7.3", "localhost", 8080);
if (oled.init()) {
    // Navigation VTML
    oled.onNavigate("/accueil.vtml");

    // Joystick branché sur /dev/input/js0
    oled.onJoystick(0, "/dev/input/js0", true);

    // Appui bouton 5 du joystick 0 (affiché 2 s puis effacé)
    oled.onButton(0, 5);

    Thread.sleep(3000);
    oled.close();
}
```

---

## Exemple combiné

Démo complète shield M-Kiwi : toutes les LEDs allumées au démarrage,
bouton 0 = sélection LED, bouton 1 = éteint, bouton 2 = allume,
OLED affiche l'état.

```java
GPIOLed     leds    = new GPIOLed();
GPIOButton  buttons = new GPIOButton();
OLEDDisplay oled    = new OLEDDisplay();

// Init OLED
boolean oledOk = oled.init();

// Init LEDs
if (!leds.init()) {
    System.out.println("LEDs indisponibles");
    return;
}

// Sélection courante (partagée avec le listener)
final int[] selected = {0};

// Init boutons
buttons.setListener(new GPIOButton.Listener() {
    @Override
    public void onPressed(int index) {
        switch (index) {
            case 0 -> {
                // Cycle LED sélectionnée
                selected[0] = (selected[0] + 1) % GPIOLed.COUNT;
                System.out.println("LED selectionnee : " + selected[0]);
            }
            case 1 -> {
                // Éteindre LED sélectionnée
                leds.set(selected[0], false);
                System.out.println("LED " + selected[0] + " OFF");
            }
            case 2 -> {
                // Allumer LED sélectionnée
                leds.set(selected[0], true);
                System.out.println("LED " + selected[0] + " ON");
            }
        }
        // Rafraîchir l'OLED
        if (oledOk) {
            oled.clear();
            oled.drawText("Shield M-Kiwi", 0, 0);
            oled.drawText("Sel: LED " + selected[0], 0, 2);
            String[] states = new String[GPIOLed.COUNT];
            for (int i = 0; i < GPIOLed.COUNT; i++)
                states[i] = "LED" + i + ": " + (leds.getState(i) ? "ON " : "OFF");
            for (int i = 0; i < states.length; i++)
                oled.drawText(states[i], 0, 4 + i / 2);
            oled.flush();
        }
    }

    @Override
    public void onReleased(int index) {}
});

buttons.init();

// Toutes les LEDs ON au démarrage
leds.allOn();

System.out.println("Demo en cours — Ctrl-C pour quitter");
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    leds.close();
    buttons.close();
    if (oledOk) { oled.clearScreen(); oled.close(); }
}));

// Attente indéfinie (le polling tourne dans son thread)
Thread.currentThread().join();
```
