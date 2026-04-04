# Ouro Backend - Con Verificación de Email

Backend para plataforma de terapia Ouro desarrollado con Spring Boot, incluyendo sistema completo de verificación de email.

## ✨ Características Principales

- ✅ **Registro de usuarios** con validación
- ✅ **Verificación de email obligatoria** con token único
- ✅ **Email de bienvenida automático**
- ✅ **Reenvío de email de verificación**
- ✅ **Expiración de tokens** (24 horas)
- ✅ **Validación** para crear perfiles de terapeuta (requiere email verificado)
- ✅ **Sistema de emails** para coordinaciones de citas
- ✅ **Sin Lombok** - Getters/Setters manuales

## 🛠 Tecnologías

- Java 17
- Spring Boot 3.2.0
- Spring Data JPA
- MySQL 8
- Spring Mail
- BCrypt para passwords
- Maven

## ⚙️ Configuración

### 1. Base de Datos

Crea la base de datos en MySQL:
```sql
CREATE DATABASE ouro;
```

Edita `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ouro
spring.datasource.username=tu_usuario
spring.datasource.password=tu_contraseña
```

### 2. Configuración de Email (Gmail)

Para usar Gmail necesitas una **contraseña de aplicación**:

1. Ve a tu cuenta de Google
2. Seguridad → Verificación en dos pasos (actívala si no está activa)
3. Contraseñas de aplicaciones
4. Genera una nueva contraseña para "Correo"
5. Copia la contraseña generada

Configura en `application.properties`:
```properties
spring.mail.username=tu-email@gmail.com
spring.mail.password=tu-contraseña-de-aplicacion
app.frontend.url=http://localhost:3000
```

## 🚀 Ejecutar la Aplicación

```bash
mvn clean install
mvn spring-boot:run
```

La aplicación estará disponible en `http://localhost:8080`

## 📧 Flujo de Verificación de Email

### 1. Registro de Usuario

**Endpoint:** `POST /api/users/register`

```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "usuario@example.com",
    "fullName": "Juan Pérez",
    "phone": "+54911123456",
    "password": "password123"
  }'
```

**Respuesta:**
```json
{
  "success": true,
  "message": "Usuario registrado exitosamente. Por favor verifica tu email.",
  "user": {
    "id": 1,
    "email": "usuario@example.com",
    "fullName": "Juan Pérez",
    "emailVerified": false
  }
}
```

**Qué pasa:**
1. Se crea el usuario con `emailVerified = false`
2. Se genera un token único de verificación
3. Se envía automáticamente un email con el link de verificación
4. El link expira en 24 horas

### 2. Verificar Email

El usuario recibe un email con un botón que apunta a:
```
http://localhost:3000/verify-email?token=abc123...
```

Tu frontend debe llamar al endpoint:

**Endpoint:** `GET /api/users/verify-email?token=abc123...`

```bash
curl -X GET "http://localhost:8080/api/users/verify-email?token=abc123..."
```

**Respuesta exitosa:**
```json
{
  "success": true,
  "message": "Email verificado exitosamente",
  "user": {
    "id": 1,
    "email": "usuario@example.com",
    "emailVerified": true
  }
}
```

### 3. Reenviar Email de Verificación

**Endpoint:** `POST /api/users/resend-verification?email=usuario@example.com`

```bash
curl -X POST "http://localhost:8080/api/users/resend-verification?email=usuario@example.com"
```

### 4. Crear Perfil de Terapeuta (Requiere Email Verificado)

**Endpoint:** `POST /api/therapists`

**Si el email NO está verificado:**
```json
{
  "success": false,
  "message": "Debe verificar su email antes de crear un perfil de terapeuta",
  "requiresEmailVerification": true
}
```

## 🔑 Endpoints Principales

### Users
- `POST /api/users/register` - Registrar usuario (envía email automático)
- `GET /api/users/verify-email?token=xxx` - Verificar email
- `POST /api/users/resend-verification?email=xxx` - Reenviar email
- `GET /api/users` - Listar todos
- `GET /api/users/{id}` - Obtener por ID
- `PUT /api/users/{id}` - Actualizar
- `DELETE /api/users/{id}` - Eliminar

### Therapists
- `POST /api/therapists` - Crear terapeuta (valida email verificado)
- `GET /api/therapists` - Listar todos
- `GET /api/therapists?specialty={specialty}` - Filtrar por especialidad
- `GET /api/therapists/{id}` - Obtener por ID
- `PUT /api/therapists/{id}` - Actualizar
- `DELETE /api/therapists/{id}` - Eliminar

### Clients
- `POST /api/clients` - Crear cliente
- `GET /api/clients` - Listar todos
- `GET /api/clients/{id}` - Obtener por ID
- `PUT /api/clients/{id}` - Actualizar
- `DELETE /api/clients/{id}` - Eliminar

### Email
- `POST /api/email/send` - Enviar email personalizado
- `POST /api/email/appointment-confirmation` - Confirmación de cita
- `POST /api/email/appointment-reminder` - Recordatorio

## 💾 Cambios en la Base de Datos

Se agregaron estos campos a la tabla `user`:

```sql
ALTER TABLE user 
ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN verification_token VARCHAR(255),
ADD COLUMN verification_token_expiry DATETIME(6);
```

Spring Boot los creará automáticamente al ejecutar.

## 🔒 Validaciones de Seguridad

1. **Token único:** UUID v4 generado para cada verificación
2. **Expiración:** 24 horas automáticas
3. **Password encriptado:** BCrypt
4. **Emails únicos:** No se permiten duplicados
5. **Validación obligatoria:** Los terapeutas DEBEN tener email verificado

## 📝 Notas Importantes

- Al cambiar el email de un usuario, se marca como no verificado y se envía nuevo email
- Los tokens se limpian de la BD después de verificar
- Si un token expira, se puede pedir uno nuevo con `/resend-verification`
- El frontend debe manejar el parámetro `?token=xxx` de la URL

## 🚦 Próximos Pasos - React

Ahora que tienes el backend con verificación, el frontend React necesitará:
1. Página de registro que llame a `/api/users/register`
2. Página de verificación que lea el token de la URL y llame a `/api/users/verify-email`
3. Componente para reenviar email de verificación
4. Manejo de estados de verificación en la UI
