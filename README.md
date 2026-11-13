# Users Management — Spring Boot, Arquitectura Hexagonal y DDD

Aplicación de gestión de usuarios construida con Java 17 y Spring Boot. La API REST es el punto de entrada activo. El código de la antigua CLI se conserva como adaptador inactivo y no posee un contenedor de dependencias independiente.

Spring es el único *composition root*: `Main` inicia el contexto y las dependencias se resuelven mediante configuración y component scanning de Spring.

## Verificación

```bash
./mvnw clean test
./mvnw clean package
```

En Windows se puede utilizar `mvnw.cmd`.

## Foto de perfil e integraciones cloud (Guías 9, 10 y 11)

| Integración | Guía | Uso en el servicio | Variables de entorno |
|---|---|---|---|
| Backblaze B2 (bucket privado, API S3) | 9 | Guarda las fotos de perfil. La base de datos solo guarda la *key* del objeto (`profile_photo_key`). | `B2_ENABLED`, `B2_ENDPOINT`, `B2_REGION`, `B2_BUCKET`, `B2_KEY_ID`, `B2_APPLICATION_KEY` |
| Valkey en Aiven (caché distribuida) | 10 | Cache-aside de `GET /api/users/{id}` (TTL 300 s) y de las imágenes (TTL 3600 s). Se invalida en cada escritura. | `VALKEY_ENABLED`, `VALKEY_URI` |
| OpenSearch en Aiven | 11 | Índice `users` (1 shard, 0 réplicas) para búsqueda por nombre/correo con tolerancia a errores y filtros por rol/estado. | `OPENSEARCH_ENABLED`, `OPENSEARCH_URL` (o `OPENSEARCH_USERNAME`/`OPENSEARCH_PASSWORD`) |

Cada integración se activa con su variable `*_ENABLED=true`. Si está desactivada, el servicio arranca igual: la caché se omite, la búsqueda y las fotos responden `503` con un mensaje claro. Copie `.env.example` a `.env` para Docker Compose; en Render defina las mismas variables en *Environment*. Nunca suba las credenciales al repositorio.

Configuración del lado de cada proveedor (según las guías):

- **B2:** bucket **privado**, cifrado SSE-B2, *Object Lock* deshabilitado, ciclo de vida *Keep only the last version*. Application Key restringida a ese bucket con *Read and Write* (no use la Master Key). Endpoint: el que muestra el bucket, por ejemplo `https://s3.us-east-005.backblazeb2.com`, y región `us-east-005`.
- **Valkey:** plan Free, política de expulsión `allkeys-lru`. `VALKEY_URI` es la *Service URI* (`rediss://default:...@host:puerto`).
- **OpenSearch:** plan Free. `OPENSEARCH_URL` es la *Service URI* de la pestaña **OpenSearch** (puerto del motor, no el 443 de Dashboards).

### Endpoints nuevos

| Método y ruta | Acceso | Descripción |
|---|---|---|
| `POST /api/users/{id}/profile-photo` | El propio usuario o ADMIN | `multipart/form-data`, campo `file`. JPEG, PNG o WEBP hasta 2 MB (se valida el contenido real del archivo). Reemplaza y borra la foto anterior. |
| `GET /api/users/{id}/profile-photo` | El propio usuario, ADMIN o REVIEWER | Devuelve la imagen con su `Content-Type`. |
| `DELETE /api/users/{id}/profile-photo` | El propio usuario o ADMIN | Quita la foto (idempotente). |
| `GET /api/users/search?q=&role=&status=&size=` | ADMIN o REVIEWER | Búsqueda en OpenSearch. `size` entre 1 y 50 (por defecto 20). |
| `POST /api/users/search/reindex` | ADMIN | Reconstruye el índice desde la base de datos. Ejecútelo una vez al activar OpenSearch para indexar los usuarios existentes. |

Las respuestas de usuario incluyen `profilePhotoUrl` (ruta de la API, o `null` si no tiene foto). El bucket es privado, por eso la imagen se sirve a través de la API y no con una URL pública de B2.

### Prueba rápida con Postman

1. `POST /api/auth/login` con `admin@example.com` / `Admin1234!` y copie el `accessToken`.
2. En las siguientes solicitudes use *Authorization → Bearer Token*.
3. `POST /api/users/00000000-0000-0000-0000-000000000001/profile-photo` → *Body → form-data*, clave `file` de tipo *File* con una imagen.
4. `GET` a la misma ruta para ver la imagen. Verifique el objeto en *Browse Files* del bucket (con candado: privado).
5. `POST /api/users/search/reindex` y luego `GET /api/users/search?q=admin`.
6. En Redis Insight verá las claves `users-service:user:<id>` y `users-service:photo:<key>` con su TTL.
