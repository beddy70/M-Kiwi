# PCB HAT Raspberry Pi pour Minitel

> ⚠️ **PROTOTYPE NON TESTÉ** - Ce design est en cours de développement et n'a pas encore été validé. Utilisez-le à vos risques et périls.

Ce document décrit la conception d'un HAT (Hardware Attached on Top) pour Raspberry Pi dédié au projet M-Kiwi Minitel.

## Fonctionnalités

- **Alimentation** : Régulateur 5V pour alimenter le RPi via GPIO
- **UART Minitel** : Connexion série TX/RX vers prise DIN
- **3 boutons poussoirs** : Entrées GPIO configurables
- **2 LEDs** : Sorties GPIO pour indicateurs status
- **Écran OLED** : SSD1306 128x64 I2C pour affichage local

## Attribution des GPIO

| Composant | GPIO | Pin Header | Fonction |
|-----------|------|------------|----------|
| **Alimentation** |
| 5V (régulateur) | - | Pin 2 ou 4 | Alimentation RPi |
| GND | - | Pin 6, 9, 14... | Masse commune |
| **UART Minitel** |
| TX | GPIO14 | Pin 8 | TX → Minitel RX |
| RX | GPIO15 | Pin 10 | RX ← Minitel TX |
| GND | - | Pin 6 | Masse Minitel |
| **Boutons (INPUT)** |
| BTN1 | GPIO17 | Pin 11 | Bouton poussoir 1 |
| BTN2 | GPIO27 | Pin 13 | Bouton poussoir 2 |
| BTN3 | GPIO22 | Pin 15 | Bouton poussoir 3 |
| **LEDs (OUTPUT)** |
| LED1 | GPIO23 | Pin 16 | LED status 1 |
| LED2 | GPIO24 | Pin 18 | LED status 2 |
| **Écran OLED I2C** |
| SDA | GPIO2 | Pin 3 | I2C Data |
| SCL | GPIO3 | Pin 5 | I2C Clock |
| VCC | - | 3.3V (Pin 1) | Alimentation écran |
| GND | - | Pin 9 | Masse écran |

## Schéma de principe

```
                    ┌─────────────────────────────────────────────────┐
                    │              HAT Raspberry Pi                   │
                    │                                                 │
   Alim externe     │  ┌──────────┐                                   │
   7-12V DC ────────┼──┤ Régulateur├──► 5V vers GPIO Pin 2/4          │
                    │  │  AMS1117 │                                   │
                    │  └──────────┘                                   │
                    │                                                 │
                    │  ┌─────────────────────────────────────────┐    │
                    │  │         Connecteur GPIO 40 pins         │    │
                    │  └─────────────────────────────────────────┘    │
                    │                                                 │
                    │  ┌────────────┐   ┌─────────────────────────┐   │
                    │  │ DIN 5 pins │   │  BTN1  BTN2  BTN3       │   │
                    │  │  Minitel   │   │   ○     ○     ○         │   │
                    │  │    ┌─┐     │   │                         │   │
                    │  │   /   \    │   │  LED1  LED2             │   │
                    │  │  │1 2 3│   │   │   ●     ●               │   │
                    │  │   \4 5/    │   │                         │   │
                    │  │    └─┘     │   │  ┌──────────────────┐   │   │
                    │  └────────────┘   │  │  OLED SSD1306    │   │   │
                    │                   │  │  128x64 I2C      │   │   │
                    │                   │  └──────────────────┘   │   │
                    │                   └─────────────────────────┘   │
                    └─────────────────────────────────────────────────┘
```

## Connecteur DIN 5 broches (prise péri-informatique Minitel)

```
      DIN 5 broches (vue face soudure)
           ┌───────┐
          /  1   2  \
         │     ●     │
          \  3   4  /
           └───5───┘

Pin DIN │ Signal    │ Connexion RPi
--------|-----------|---------------
   1    │ RX (←)    │ GPIO14/TX (Pin 8)
   2    │ GND       │ GND (Pin 6)
   3    │ TX (→)    │ GPIO15/RX (Pin 10)
   4    │ Alim 8-12V│ NON CONNECTÉ ⚠️
   5    │ GND       │ GND (Pin 6)
```

**⚠️ Attention** : Ne jamais connecter la broche 4 du DIN (8-12V) au Raspberry Pi !

## Schéma électronique

### Alimentation

```
  DC Jack 7-12V ──┬──[D1 1N4007]──┤IN  AMS1117  OUT├──┬──► 5V (Pin 2/4)
                  │               │      GND       │  │
                 ─┴─ C1 100µF     └───────┬────────┘ ─┴─ C2 10µF
                  │                       │           │
  GND ────────────┴───────────────────────┴───────────┴──► GND
```

### UART Minitel

```
  Pin 8  (GPIO14/TX) ─────────────────────► DIN Pin 1 (Minitel RX)
  Pin 10 (GPIO15/RX) ◄───────────────────── DIN Pin 3 (Minitel TX)
  GND (Pin 6) ────────────────────────────► DIN Pin 2 & 5 (GND)
```

**Protection optionnelle RX (5V → 3.3V) :**

```
  DIN Pin 3 (TX Minitel) ───[R1 1kΩ]───┬───► GPIO15/RX (Pin 10)
                                       │
                                      [R2 2kΩ]
                                       │
                                      GND
```

### Boutons (avec pull-up interne RPi)

```
  GPIO17 (Pin 11) ───[BTN1]───► GND
  GPIO27 (Pin 13) ───[BTN2]───► GND  
  GPIO22 (Pin 15) ───[BTN3]───► GND
```

### LEDs

```
  GPIO23 (Pin 16) ───[R 330Ω]───[LED1]───► GND
  GPIO24 (Pin 18) ───[R 330Ω]───[LED2]───► GND
```

### Écran OLED I2C

```
  3.3V (Pin 1) ──────► VCC (OLED)
  GND (Pin 9) ───────► GND (OLED)
  GPIO2/SDA (Pin 3) ─► SDA (OLED)
  GPIO3/SCL (Pin 5) ─► SCL (OLED)
```

## Schéma électronique complet

```
                                    ┌─────────────────────────────────────────────────────────────────┐
                                    │                    HAT MINITEL v1.0                             │
                                    │                                                                 │
    ┌─────────┐                     │  ┌─────────────────────────────────────────────────────────┐    │
    │ DC Jack │                     │  │              RASPBERRY PI GPIO HEADER                   │    │
    │ 7-12V   │                     │  │  ┌─────────────────────────────────────────────────┐    │    │
    │  ┌───┐  │                     │  │  │ 1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 ... │    │    │
    │  │ + ├──┼─────┐               │  │  │3V3 5V SDA 5V SCL GND .. TX GND RX B1 .. B2 GND B3│    │    │
    │  │   │  │     │               │  │  │ ●  ●  ●  ●  ●  ●  ●  ●  ●  ●  ●  ●  ●  ●  ●     │    │    │
    │  │ - ├──┼──┐  │               │  │  │ ○  ○  ○  ○  ○  ○  ○  ○  ○  ○  ○  ○  ○  ○  ○     │    │    │
    │  └───┘  │  │  │               │  │  │16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 ... │    │    │
    └─────────┘  │  │               │  │  │L1 .. L2 ...                                     │    │    │
                 │  │               │  │  └─────────────────────────────────────────────────┘    │    │
                 │  │               │  └─────────────────────────────────────────────────────────┘    │
                 │  │               │                                                                 │
                 │  │  ┌────────────┼─────────────────────────────────────────────────────────────┐   │
                 │  │  │            │         POWER SUPPLY SECTION                                │   │
                 │  │  │            │                                                             │   │
                 │  │  │    D1      │         U1 AMS1117-5.0                                      │   │
                 │  └──┼──►|────────┼────┬────┬─────┬────┬──────────────────► 5V (Pin 2,4)        │   │
                 │     │  1N4007    │    │    │ IN  │OUT │                                        │   │
                 │     │            │   ─┴─   │     │    │   ─┴─                                  │   │
                 │     │            │   ─┬─   │ GND │    │   ─┬─                                  │   │
                 │     │            │  C1│    └──┬──┘    │  C2│                                   │   │
                 │     │            │100µF│       │      │ 10µF                                   │   │
                 └─────┼────────────┼────┴───────┴──────┴────┴─────────────► GND                  │   │
                       │            │                                                             │   │
                       └────────────┼─────────────────────────────────────────────────────────────┘   │
                                    │                                                                 │
                       ┌────────────┼─────────────────────────────────────────────────────────────┐   │
                       │            │         UART MINITEL SECTION                                │   │
                       │            │                                                             │   │
                       │            │    Pin 8 (TX) ────────────────────────► DIN Pin 1 (RX)      │   │
                       │            │                                                             │   │
                       │            │    Pin 10 (RX) ◄──[R1 1k]──┬──────────── DIN Pin 3 (TX)     │   │
                       │            │                            │                                │   │
                       │            │                          [R2 2k]  (protection 5V→3.3V)      │   │
                       │            │                            │                                │   │
                       │            │    Pin 6 (GND) ────────────┴──────────► DIN Pin 2,5 (GND)   │   │
                       │            │                                                             │   │
                       │            │         ┌─────────────┐                                     │   │
                       │            │         │  DIN 5 pins │                                     │   │
                       │            │         │    ┌───┐    │                                     │   │
                       │            │         │   / 1 2 \   │  1: RX (←RPi TX)                    │   │
                       │            │         │  │   ●   │  │  2: GND                             │   │
                       │            │         │   \ 3 4 /   │  3: TX (→RPi RX)                    │   │
                       │            │         │    └─5─┘    │  4: NC (8-12V!)                     │   │
                       │            │         │             │  5: GND                             │   │
                       │            │         └─────────────┘                                     │   │
                       └────────────┼─────────────────────────────────────────────────────────────┘   │
                                    │                                                                 │
                       ┌────────────┼─────────────────────────────────────────────────────────────┐   │
                       │            │         BUTTONS SECTION (Active LOW, internal pull-up)      │   │
                       │            │                                                             │   │
                       │            │    Pin 11 (GPIO17) ────[BTN1]──┬──► GND                     │   │
                       │            │                                │                            │   │
                       │            │    Pin 13 (GPIO27) ────[BTN2]──┤                            │   │
                       │            │                                │                            │   │
                       │            │    Pin 15 (GPIO22) ────[BTN3]──┘                            │   │
                       │            │                                                             │   │
                       │            │         [BTN1]    [BTN2]    [BTN3]                          │   │
                       │            │           ○         ○         ○                             │   │
                       └────────────┼─────────────────────────────────────────────────────────────┘   │
                                    │                                                                 │
                       ┌────────────┼─────────────────────────────────────────────────────────────┐   │
                       │            │         LEDS SECTION                                        │   │
                       │            │                                                             │   │
                       │            │    Pin 16 (GPIO23) ───[R3 330Ω]───►|───┬──► GND             │   │
                       │            │                                  LED1  │                    │   │
                       │            │    Pin 18 (GPIO24) ───[R4 330Ω]───►|───┘                    │   │
                       │            │                                  LED2                       │   │
                       │            │                                                             │   │
                       │            │              (●) LED1    (●) LED2                           │   │
                       └────────────┼─────────────────────────────────────────────────────────────┘   │
                                    │                                                                 │
                       ┌────────────┼─────────────────────────────────────────────────────────────┐   │
                       │            │         OLED I2C SECTION                                    │   │
                       │            │                                                             │   │
                       │            │    Pin 1 (3.3V) ──────────────────────► OLED VCC            │   │
                       │            │    Pin 3 (SDA)  ──────────────────────► OLED SDA            │   │
                       │            │    Pin 5 (SCL)  ──────────────────────► OLED SCL            │   │
                       │            │    Pin 9 (GND)  ──────────────────────► OLED GND            │   │
                       │            │                                                             │   │
                       │            │         ┌──────────────────────┐                            │   │
                       │            │         │    OLED SSD1306      │                            │   │
                       │            │         │      128x64          │                            │   │
                       │            │         │  ┌──────────────┐    │                            │   │
                       │            │         │  │              │    │                            │   │
                       │            │         │  │   M-KIWI     │    │                            │   │
                       │            │         │  │   STATUS     │    │                            │   │
                       │            │         │  │              │    │                            │   │
                       │            │         │  └──────────────┘    │                            │   │
                       │            │         │ VCC GND SCL SDA      │                            │   │
                       │            │         └──┬───┬───┬───┬───────┘                            │   │
                       │            │            │   │   │   │                                    │   │
                       └────────────┼────────────┴───┴───┴───┴────────────────────────────────────┘   │
                                    │                                                                 │
                                    │    ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐                           │
                                    │    │ M2.5│  │ M2.5│  │ M2.5│  │ M2.5│  ← Trous de fixation      │
                                    │    └─────┘  └─────┘  └─────┘  └─────┘                           │
                                    │                                                                 │
                                    │                    65mm x 56mm                                  │
                                    └─────────────────────────────────────────────────────────────────┘
```

## Bill of Materials (BOM)

| Qté | Composant | Référence | Description |
|-----|-----------|-----------|-------------|
| 1 | Régulateur | AMS1117-5.0 | Régulateur LDO 5V 1A |
| 1 | Diode | 1N4007 | Protection inversion polarité |
| 1 | Condensateur | 100µF/25V | Électrolytique, entrée régulateur |
| 1 | Condensateur | 10µF/10V | Électrolytique, sortie régulateur |
| 1 | Jack DC | 5.5x2.1mm | Embase alimentation |
| 1 | Connecteur DIN | 5 broches 180° | Prise péri-informatique Minitel |
| 1 | Header GPIO | 2x20 femelle | Connecteur Raspberry Pi |
| 3 | Bouton | 6x6mm tactile | Boutons poussoirs |
| 2 | LED | 3mm ou 5mm | Indicateurs status |
| 2 | Résistance | 330Ω | Limitation courant LEDs |
| 1 | Connecteur | 4 pins femelle | Pour écran OLED |
| 1 | Résistance | 1kΩ | Protection RX (optionnel) |
| 1 | Résistance | 2kΩ | Protection RX (optionnel) |

## Dimensions PCB

Format HAT Raspberry Pi standard :
- **65mm x 56mm**
- Trous de fixation : 4x M2.5 aux coins
- Espacement GPIO : 2.54mm

## Configuration logicielle

### Activation UART

```bash
# /boot/config.txt
enable_uart=1
dtoverlay=disable-bt

# Désactiver getty
sudo systemctl disable serial-getty@ttyAMA0.service
```

### Activation I2C (pour OLED)

```bash
sudo raspi-config
# Interface Options → I2C → Enable

# Vérifier la détection
sudo i2cdetect -y 1
# L'écran SSD1306 apparaît généralement à l'adresse 0x3C
```

### Test des GPIO (Python)

```python
import RPi.GPIO as GPIO
import time

# Configuration
GPIO.setmode(GPIO.BCM)
GPIO.setup(17, GPIO.IN, pull_up_down=GPIO.PUD_UP)  # BTN1
GPIO.setup(27, GPIO.IN, pull_up_down=GPIO.PUD_UP)  # BTN2
GPIO.setup(22, GPIO.IN, pull_up_down=GPIO.PUD_UP)  # BTN3
GPIO.setup(23, GPIO.OUT)  # LED1
GPIO.setup(24, GPIO.OUT)  # LED2

# Test LEDs
GPIO.output(23, GPIO.HIGH)
time.sleep(1)
GPIO.output(23, GPIO.LOW)

# Lecture boutons
while True:
    if GPIO.input(17) == GPIO.LOW:
        print("BTN1 pressé")
    time.sleep(0.1)
```

## Fichiers de conception

- `pcb/minitel_hat.kicad_pro` - Projet KiCad 8.0
- `pcb/minitel_hat.kicad_sch` - Schéma électronique
- `pcb/minitel_hat.kicad_pcb` - Layout PCB (65x56mm, format HAT)
- `pcb/gerber/` - Fichiers Gerber pour fabrication (à générer depuis KiCad)
- `pcb/README.md` - Instructions d'utilisation des fichiers KiCad

---

**Auteur** : Eddy BRIERE  
**Projet** : M-Kiwi Minitel Server  
**Version** : 1.0 (prototype)
