# Baseerat – Build Instructions

## مسئلہ جو پہلے آیا تھا
پہلی بلڈ اس لیے فیل ہوئی کہ `gradle-wrapper.jar` پروجیکٹ میں موجود نہیں تھا۔  
اب ورک فلو خود `gradle wrapper` چلا کر jar بناتا ہے، پھر APK بناتا ہے۔

## GitHub سے APK بنانا (سب سے آسان)

1. GitHub پر Repository کھولیں (یا نیا بنائیں)۔
2. اس ZIP کی **تمام فائلیں** repository کے **root** میں رکھیں / اپ لوڈ کریں۔
3. `main` branch پر push کریں۔
4. **Actions** ٹیب کھولیں → `Build APK` ورک فلو چلے گی۔
5. کامیاب ہونے کے بعد **Artifacts** سے `Baseerat-debug-apk` ڈاؤن لوڈ کریں۔

اگر پہلے سے ریپو میں پرانا ورک فلو ہے تو نئی `.github/workflows/build.yml` فائل سے بدل دیں اور دوبارہ push کریں۔

## مقامی (Local) بلڈ

ضروریات: JDK 17 + Android SDK

```bash
cd Baseerat
gradle wrapper --gradle-version 8.9   # پہلی بار
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## فون پر سیٹ اپ

1. APK انسٹال کریں۔
2. Settings → Accessibility → **Baseerat Accessibility** آن کریں۔
3. Display over other apps کی اجازت دیں۔
4. ایپ میں **Show Floating Button** دبائیں۔
