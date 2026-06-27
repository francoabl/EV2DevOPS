# Proyecto DevOps — Innovatech Chile (EP3)

Aplicación de gestión de **Ventas** y **Despachos** orquestada en **AWS EKS (Kubernetes)**,
con pipeline **CI/CD (GitHub Actions)** que despliega vía `kubectl` y autoscaling con **HPA**.

## Componentes

| Servicio | Tecnología | Puerto | Ruta pública |
|----------|------------|--------|--------------|
| `front_despacho` | React + Vite + nginx | 80 | `/` |
| `back-Ventas_SpringBoot` | Spring Boot (Java 17) | 8080 | `/api-ventas/*` |
| `back-Despachos_SpringBoot` | Spring Boot (Java 17) | 8080 | `/api-despachos/*` |
| MySQL | mysql:8.0 | 3306 | interno (Service `mysql:3306`) |

## Arquitectura y despliegue

La orquestación está en manifiestos de Kubernetes en **[`k8s/`](k8s/)** y se
despliega con el pipeline **[`.github/workflows/deploy-eks.yml`](.github/workflows/deploy-eks.yml)**.
El clúster se crea con **[`eks/cluster-config.yaml`](eks/cluster-config.yaml)** (eksctl + LabRole).

➡️ **Guía completa (crear clúster, desplegar, HPA, defensa): [`eks/README.md`](eks/README.md)**

> Nota: la versión anterior en ECS/CloudFormation queda como referencia en
> [`infra/`](infra/), pero el despliegue activo es **EKS**.

## Pruebas en local (antes de AWS)

Replica la arquitectura en tu PC con Docker (un `gateway` nginx enruta como el
LoadBalancer):

```bash
docker compose up --build      # primera vez tarda (compila los Spring Boot)
```
Luego abre **http://localhost:8080**. Endpoints de prueba:
- http://localhost:8080/ — Frontend
- http://localhost:8080/api-ventas/v3/api-docs — Backend Ventas
- http://localhost:8080/api-despachos/v3/api-docs — Backend Despachos

Para detener: `docker compose down` (agrega `-v` para borrar también los datos).

## Inicio rápido (EKS)

1. Inicia el **AWS Academy Learner Lab**.
2. Crea el clúster: `eksctl create cluster -f eks/cluster-config.yaml` (~15-20 min).
3. Instala metrics-server (para el HPA) — ver [`eks/README.md`](eks/README.md).
4. Configura los GitHub Secrets: credenciales AWS + `AWS_REGION`, `EKS_CLUSTER_NAME` (`innova-eks`), `EKS_NAMESPACE` (`innova`).
5. Haz `push` a la rama `main` → el pipeline construye, sube a ECR y aplica los manifiestos con `kubectl`.
6. La URL pública es el `EXTERNAL-IP` del Service `front`: `kubectl get svc front -n innova`.
