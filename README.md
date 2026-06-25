# Proyecto DevOps — Innovatech Chile (EP3)

Aplicación de gestión de **Ventas** y **Despachos** orquestada en **AWS ECS Fargate**,
con pipeline **CI/CD (GitHub Actions)** e infraestructura como código (**CloudFormation**).

## Componentes

| Servicio | Tecnología | Puerto | Ruta pública |
|----------|------------|--------|--------------|
| `front_despacho` | React + Vite + nginx | 80 | `/` |
| `back-Ventas_SpringBoot` | Spring Boot (Java 17) | 8080 | `/api-ventas/*` |
| `back-Despachos_SpringBoot` | Spring Boot (Java 17) | 8080 | `/api-despachos/*` |
| MySQL | mysql:8.0 | 3306 | interno (`mysql.innova.local`) |

## Arquitectura y despliegue

Toda la orquestación (clúster ECS, ALB, autoscaling, networking, logs) está definida
en **[`infra/ecs.yml`](infra/ecs.yml)** y se despliega con el pipeline
**[`.github/workflows/deploy-ecs.yml`](.github/workflows/deploy-ecs.yml)**.

➡️ **Guía completa de despliegue, validación y defensa: [`infra/README.md`](infra/README.md)**

## Pruebas en local (antes de AWS)

Replica la arquitectura de ECS en tu PC con Docker (un `gateway` nginx cumple el
rol del ALB):

```bash
docker compose up --build      # primera vez tarda (compila los Spring Boot)
```
Luego abre **http://localhost:8080**. Endpoints de prueba:
- http://localhost:8080/ — Frontend
- http://localhost:8080/api-ventas/v3/api-docs — Backend Ventas
- http://localhost:8080/api-despachos/v3/api-docs — Backend Despachos

Para detener: `docker compose down` (agrega `-v` para borrar también los datos).

## Inicio rápido

1. Inicia el **AWS Academy Learner Lab** y copia las credenciales temporales.
2. Configura los GitHub Secrets: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`,
   `AWS_SESSION_TOKEN`, `AWS_REGION`.
3. Haz `push` a la rama `deploy` → el pipeline construye las imágenes, las sube a
   ECR y despliega el stack en ECS.
4. La URL pública aparece en los *Outputs* del workflow (salida del ALB).
