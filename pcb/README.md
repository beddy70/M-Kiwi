# PCB HAT Minitel - Fichiers KiCad

> ⚠️ **PROTOTYPE NON TESTÉ** - Ces fichiers sont en cours de développement.

## Fichiers

| Fichier | Description |
|---------|-------------|
| `minitel_hat.kicad_pro` | Projet KiCad 8.0 |
| `minitel_hat.kicad_sch` | Schéma électronique |
| `minitel_hat.kicad_pcb` | Layout PCB (65x56mm, format HAT) |
| `gerber/` | Fichiers Gerber pour fabrication |

## Ouverture

1. Installer [KiCad 8.0](https://www.kicad.org/download/)
2. Ouvrir `minitel_hat.kicad_pro`

## Étapes pour finaliser

1. **Schéma** : Ajouter les symboles des composants depuis les librairies KiCad
2. **PCB** : Placer les empreintes (footprints) des composants
3. **Routage** : Router les pistes (auto-router ou manuel)
4. **DRC** : Vérifier les règles de conception
5. **Gerber** : Exporter via File → Plot → Gerber

## Fabrication

Services recommandés :
- [JLCPCB](https://jlcpcb.com) - ~$2 pour 5 PCB
- [PCBWay](https://www.pcbway.com)
- [Aisler](https://aisler.net) (Europe)

## Spécifications PCB

- **Dimensions** : 65mm x 56mm
- **Couches** : 2 (F.Cu, B.Cu)
- **Épaisseur** : 1.6mm
- **Finition** : HASL ou ENIG
- **Couleur** : Vert (ou au choix)

## Composants à commander

Voir la BOM complète dans [docs/PCB_HAT.md](../docs/PCB_HAT.md)
