# 📦 Compilation avec NetBeans et ZXing

## 🔧 Configuration NetBeans

### 1. Ajouter les JAR ZXing au Projet

1. **Clic droit** sur votre projet `Minitel-Serveur`
2. **Properties** → **Libraries**
3. **Add JAR/Folder**
4. Sélectionner :
   - `lib/zxing-core-3.5.1.jar`
   - `lib/zxing-javase-3.5.1.jar`
5. **OK**

### 2. Build et Run

1. **Clean and Build** (Shift+F11)
2. **Run Project** (F6)

## 🚀 Résultat Attendu

```
🔍 DEBUG ZXing QR:
   Screen: 80x72 pixels
   QR ZXing: 21x21 modules
   QR scalé: 42x42 pixels
   Position: (19, 15)
📊 Pixels dessinés: 1764, ignorés: 0
✅ QR Code ZXing SCANNABLE pour: "https://eddy-briere.com"
```

## 📱 Test iPhone

Le QR Code généré devrait maintenant être **100% scannable** par votre iPhone !

## ⚠️ Si Erreur de Compilation

Si NetBeans ne trouve pas ZXing, utiliser le fallback automatique vers `generateCenteredImprovedQR()`.
