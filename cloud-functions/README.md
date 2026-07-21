# Verificación de licencia de app de pago (Play Integrity + servidor)

Esta Cloud Function (`verifyAppLicense`) es la **única protección robusta** para una app
de PAGO: decide en el servidor si la cuenta compró la app, leyendo el
`appLicensingVerdict` del token de Play Integrity. Un cliente parcheado no puede
falsificarlo.

## Requisitos previos (una sola vez)

1. **Plan Blaze** en Firebase (Cloud Functions lo requiere; tiene capa gratis amplia).
2. En **Google Cloud Console** del proyecto Firebase:
   - Habilita la **Play Integrity API**.
   - Da a la cuenta de servicio de las funciones el rol para llamarla
     (normalmente `App Engine default service account` ya sirve; si no, concede
     acceso a la Play Integrity API).
3. En **Google Play Console → tu app → Integridad de Play (Play Integrity API)**:
   - Vincula el proyecto de Google Cloud (el mismo del Firebase de la app).
   - Deja la respuesta de licencia habilitada (`appLicensingVerdict`).

## Desplegar

```bash
# En la carpeta de tu app (o copia estos archivos a tu proyecto Firebase):
firebase login
firebase use TU_PROJECT_ID          # el projectId Firebase de ESTA app
# Copia index.js y package.json a la carpeta functions/ del proyecto:
cd functions
npm install
firebase deploy --only functions:verifyAppLicense
```

- Cambia `PACKAGE_NAME` en `index.js` por el applicationId de cada app
  (o define la variable de entorno `APP_PACKAGE_NAME` al desplegar).
- Región por defecto: `us-central1` (coincide con el cliente `MintLicenseVerifier`).
  Si la cambias, pásala también en el cliente.

## Activar el bloqueo en la app

En `MainActivity` (app de pago):
```kotlin
enableAntiPiracy = true,
requireValidLicense = true,   // exige veredicto LICENSED del servidor
base64LicenseKey = "<tu clave RSA de Play Console>",  // para firmar compras premium
```

## Notas de comportamiento
- Si el servidor devuelve `UNKNOWN`/`UNEVALUATED` (error o red), la app **NO** bloquea
  (fail-open) para no dejar fuera a un comprador legítimo por un fallo transitorio.
- Solo bloquea con `UNLICENSED` confirmado por el servidor.
- Prueba con una cuenta que **no** haya comprado la app (o el track de prueba con
  cuentas de test) para verificar el bloqueo.
