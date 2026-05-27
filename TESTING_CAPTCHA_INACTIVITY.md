# Prueba de CAPTCHA e Inactividad

## 1. Prueba del CAPTCHA Matemático

### Paso 1.1: Abrir Modal de Login
1. Abre http://localhost/ en tu navegador
2. Haz clic en el botón "🗡️ INICIAR SESIÓN"
3. Deberías ver el modal de login con:
   - Campo "Usuario"
   - Campo "Contraseña"
   - **Nueva sección CAPTCHA**: "¿Cuánto es X + Y?" con input y botón 🔄

### Paso 1.2: Verificar Generación de Pregunta
1. En el paso anterior, debería haber una pregunta matemática aleatoria
2. Haz clic en el botón 🔄 (refrescar)
3. La pregunta debería cambiar a otra suma diferente
4. El campo de respuesta debería vaciarse

### Paso 1.3: Respuesta Incorrecta
1. En el campo de respuesta, escribe un número incorrecto (ej: si la pregunta es "5 + 3 = ?", escribe "10")
2. Haz clic en "INICIAR SESIÓN"
3. Deberías ver un error en rojo: **"Respuesta incorrecta, intenta de nuevo"**
4. Una nueva pregunta debería generarse automáticamente

### Paso 1.4: Respuesta Correcta
1. Observa la pregunta actual (ej: "7 + 8 = ?")
2. Escribe la respuesta correcta (ej: "15")
3. Ingresa tu usuario y contraseña válidos
4. Haz clic en "INICIAR SESIÓN"
5. Deberías **ingresar exitosamente** y cerrar el modal

---

## 2. Prueba del Sistema de Inactividad (10 minutos)

### Paso 2.1: Verificar Que InactivityManager Inicia Después de Login
1. Completar login exitosamente (Paso 1.4)
2. Verificar que:
   - No hay modal de advertencia visible
   - El usuario está en el panel principal
   - Puedes navegar normalmente

### Paso 2.2: Verificar la Advertencia de Inactividad (método rápido con DevTools)
1. Después de hacer login, abre DevTools (F12)
2. Ve a la consola (Console)
3. Ejecuta este código para **acelerar el timer a 20 segundos** para pruebas:

```javascript
// Para testing: crear un InactivityManager con 20 segundos timeout
if (window.inactivityManager) window.inactivityManager.stop();
window.inactivityManager = new InactivityManager(20/60, 16/60); // 20 seg timeout, 16 seg warning
window.inactivityManager.start();
console.log("InactivityManager iniciado para testing (20 seg timeout)");
```

4. Espera 16 segundos sin mover el mouse ni escribir nada
5. Después de 16 segundos, debería aparecer un modal: **"⏰ Sesión por vencer"**
6. El modal mostrará: "Tu sesión vence en 4 segundos por inactividad"
7. Hay un contador que desciende: 4, 3, 2, 1, 0

### Paso 2.3: Extender Sesión desde la Advertencia
1. Cuando aparezca el modal de advertencia (Paso 2.2)
2. Haz clic en **"Extender sesión"**
3. El modal debería cerrarse
4. Deberías continuar logueado normalmente
5. El timer de inactividad se resetea y comienza de nuevo

### Paso 2.4: Cerrar Sesión Desde la Advertencia
1. Ejecuta de nuevo el código del Paso 2.2 para re-inicializar el timer rápido
2. Espera a que aparezca el modal de advertencia
3. Haz clic en **"Salir"**
4. Deberías:
   - Ver un toast: "Tu sesión expiró por inactividad"
   - Ser redirigido a la página de inicio (home)
   - El modal de login debería estar disponible

### Paso 2.5: Logout Automático (sin intervención)
1. Ejecuta de nuevo el código del Paso 2.2
2. **NO hagas nada**: no muevas el mouse, no escribas, no hagas clic
3. Después de 20 segundos, deberías ser redirigido automáticamente a la página de inicio
4. Deberías ver el toast: "Tu sesión expiró por inactividad"

### Paso 2.6: Verificar que Actividad Resetea el Timer
1. Ejecuta el código del Paso 2.2
2. Espera 10 segundos (sin hacer nada)
3. Luego **mueve el mouse o haz clic en cualquier lugar**
4. El timer debería resetarse
5. Espera 16 segundos nuevos sin actividad
6. El modal de advertencia debería aparecer nuevamente

### Paso 2.7: Detener InactivityManager al Hacer Logout
1. Haz login normalmente (con CAPTCHA correcto)
2. Haz clic en el botón de logout (en el panel)
3. Deberías ver el toast: "Sesión cerrada correctamente"
4. El InactivityManager debería detenerse (ya no contarás inactividad)
5. Abre el modal de login de nuevo
6. Ejecuta en la consola:
```javascript
console.log("InactivityManager active:", window.inactivityManager?.isActive || false);
```
7. Deberías ver: `false` (porque fue detenido en logout)

---

## 3. Casos Especiales a Verificar

### Caso 3.1: Abrir Modal de Login Múltiples Veces
1. Abre el modal de login
2. Ciérralo (clic en la X o fuera del modal)
3. Abrelo de nuevo
4. Una nueva pregunta CAPTCHA debería generarse

### Caso 3.2: CAPTCHA No Afecta otros Endpoints
1. Después de hacer login, verifica que:
   - El API de rankings funciona: `/api/rankings/pvp`
   - El API de tienda funciona: `/api/shop/items`
   - Los endpoints protegidos funcionan normalmente
2. El CAPTCHA solo está en el formulario de login, no afecta otros endpoints

### Caso 3.3: Error de Login No Interrumpe CAPTCHA
1. Abre el modal de login
2. Genera una pregunta CAPTCHA (debería haber una)
3. Responde correctamente al CAPTCHA
4. Ingresa usuario y contraseña **INCORRECTOS**
5. Debería aparecer error: "Usuario o contraseña inválidos"
6. El CAPTCHA se debería refrescar automáticamente (nueva pregunta)

---

## 4. Verificación en DevTools

### Verificar que InactivityManager existe:
```javascript
console.log(window.inactivityManager);
```
Debería mostrar un objeto con propiedades: `timeoutMs`, `warningMs`, `isActive`, métodos `start()`, `stop()`, `resetTimer()`

### Verificar que CAPTCHA variables existen:
```javascript
console.log("Current CAPTCHA answer:", currentCaptchaAnswer);
console.log("Expected type: number");
```

### Verificar Event Listeners (en la consola):
```javascript
// Los event listeners están attachados a estos eventos:
// mousemove, keydown, click, touchstart
console.log("Activity monitoring is active");
```

---

## 5. Prueba en Móvil (si es posible)

1. Abre la página en un navegador móvil
2. Verifica que el CAPTCHA se vea correctamente en pantalla móvil
3. Responde el CAPTCHA
4. Haz login
5. Verifica que la advertencia de inactividad aparezca después de no tocar la pantalla por 8+ minutos

---

## Resumen de Cambios Implementados

✅ **CAPTCHA Matemático**
- Suma de dos números (1-20) generada aleatoriamente
- Validación en el frontend antes de permitir login
- Botón para refrescar pregunta
- Nuevo error mostrado si respuesta es incorrecta
- Nueva pregunta generada automáticamente si error

✅ **Sistema de Inactividad**
- InactivityManager: clase que monitorea actividad del usuario
- Timeout de 10 minutos sin actividad
- Advertencia a los 8 minutos
- Modal con countdown timer
- Eventos monitoreados: mousemove, keydown, click, touchstart
- Integración con login/logout
- Auto-logout si no hay respuesta a la advertencia

✅ **CSS Styling**
- Contenedor CAPTCHA con fondo coloreado
- Botón de refresh estilizado
- Modal de inactividad con diseño distintivo
- Countdown timer visible

---

## Notas de Seguridad

⚠️ **IMPORTANTE:**
- El CAPTCHA es simple y está en el frontend (para privacidad, sin APIs externas)
- No es invulnerable contra bots sofisticados, pero previene automatización básica
- La inactividad es manejada completamente en el frontend
- Los timers se resetean con cualquier interacción del usuario
- No se requiere base de datos adicional para el sistema de inactividad

