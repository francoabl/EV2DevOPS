# Despliegue en AWS ECS Fargate (EP3 - Innovatech)

Orquestación de la aplicación (Front + 2 Backends + MySQL) en **AWS ECS Fargate**
mediante **CloudFormation** (Infrastructure as Code) y un pipeline **CI/CD con
GitHub Actions**. Compatible con **AWS Academy Learner Lab**.

## Arquitectura

```
Internet
   │
   ▼
Application Load Balancer (público, :80)  ── routing por path ──┐
   ├─ /*                → Front  (nginx :80)
   ├─ /api-ventas/*     → back-ventas    (:8080)
   └─ /api-despachos/*  → back-despachos (:8080)

ECS Cluster "innova-cluster" (Fargate)
   ├─ innova-front        (autoscaling CPU 50%, 1→4 tareas)
   ├─ innova-ventas       (autoscaling CPU 50%, 1→4 tareas)
   ├─ innova-despachos    (autoscaling CPU 50%, 1→4 tareas)
   └─ innova-mysql        → DNS interno: mysql.innova.local:3306 (Cloud Map)

VPC 10.20.0.0/16 · 2 subnets públicas (multi-AZ) · 2 Security Groups
CloudWatch Logs (/ecs/innova/*) · Container Insights · ECR
IAM: usa LabRole (no se crean roles → compatible con Academy)
```

### Decisiones de diseño (para la defensa)

| Tema | Decisión | Por qué |
|------|----------|---------|
| Orquestador | **ECS Fargate** | Sin gestionar nodos/EC2; más simple que EKS y suficiente para la rúbrica. |
| IAM | **LabRole** existente | Learner Lab **no permite crear roles IAM**; crear uno haría fallar el stack. |
| Comunicación Front→Back | **Routing por path en el ALB** | El navegador llama `/api-ventas/*` y `/api-despachos/*` al mismo origen; el ALB enruta. Cumple "balanceo con ALB". |
| Base de datos | **MySQL como servicio ECS** + Cloud Map DNS | Gratis y autocontenido en el lab. Los backends la alcanzan por `mysql.innova.local`. |
| Red | Subnets **públicas** con IP pública | Fargate descarga imágenes de ECR/DockerHub sin NAT Gateway (evita costo y restricciones del lab). |
| Autoscaling | **Target Tracking CPU 50%** | Escala out al superar 50% de CPU; valor estándar equilibrado. |

> ⚠️ **Persistencia**: el MySQL del clúster **no persiste** datos entre reinicios
> (sin volumen EFS). Para datos persistentes, migrar a **RDS MySQL** y apuntar
> `SPRING_DATASOURCE_URL` al endpoint de RDS. Para la demo, `ddl-auto=update`
> recrea el esquema automáticamente.

## Requisitos previos

1. **AWS Academy Learner Lab** iniciado (botón *Start Lab* en verde).
2. Repositorio en GitHub con la rama `deploy`.
3. Secrets configurados en GitHub (Settings → Secrets and variables → Actions):

| Secret | Dónde sacarlo |
|--------|---------------|
| `AWS_ACCESS_KEY_ID` | Learner Lab → *AWS Details* → *AWS CLI* |
| `AWS_SECRET_ACCESS_KEY` | idem |
| `AWS_SESSION_TOKEN` | idem (las credenciales del lab son **temporales**, caducan al cerrar el lab) |
| `AWS_REGION` | normalmente `us-east-1` |

> 🔑 Las credenciales del Learner Lab **cambian cada vez que reinicias el lab**.
> Si el pipeline falla con `ExpiredToken`, actualiza los 3 secrets.

## Despliegue automático (pipeline)

Cada `push` a la rama `deploy` ejecuta `.github/workflows/deploy-ecs.yml`:

1. **Build & push** de las 3 imágenes a ECR (tag = SHA corto del commit).
2. **`aws cloudformation deploy`** crea/actualiza el stack `innova-ecs`.
3. Imprime los **Outputs** (URL pública del ALB).

También se puede lanzar manualmente desde **Actions → CI/CD ECS → Run workflow**.

## Despliegue manual (desde tu PC, opcional)

```bash
# 1) Login a ECR y crear repos
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
REGION=us-east-1
REGISTRY=$ACCOUNT.dkr.ecr.$REGION.amazonaws.com
aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin $REGISTRY
for r in innova/front innova/ventas innova/despachos; do
  aws ecr create-repository --repository-name $r 2>/dev/null || true
done

# 2) Build & push
docker build -t $REGISTRY/innova/front:manual ./front_despacho && docker push $REGISTRY/innova/front:manual
docker build -t $REGISTRY/innova/ventas:manual ./back-Ventas_SpringBoot/Springboot-API-REST && docker push $REGISTRY/innova/ventas:manual
docker build -t $REGISTRY/innova/despachos:manual ./back-Despachos_SpringBoot/Springboot-API-REST-DESPACHO && docker push $REGISTRY/innova/despachos:manual

# 3) Deploy del stack
aws cloudformation deploy \
  --stack-name innova-ecs \
  --template-file infra/ecs.yml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
    ImageFront=$REGISTRY/innova/front:manual \
    ImageVentas=$REGISTRY/innova/ventas:manual \
    ImageDespachos=$REGISTRY/innova/despachos:manual

# 4) Obtener la URL pública
aws cloudformation describe-stacks --stack-name innova-ecs \
  --query "Stacks[0].Outputs" --output table
```

## Validación funcional (IE7)

```bash
ALB=$(aws cloudformation describe-stacks --stack-name innova-ecs \
  --query "Stacks[0].Outputs[?OutputKey=='AppUrl'].OutputValue" --output text)

curl -i $ALB/                              # Frontend (200)
curl -i $ALB/api-ventas/v3/api-docs        # Backend ventas
curl -i $ALB/api-despachos/v3/api-docs     # Backend despachos
```

## Logs y métricas (IE6)

```bash
# Logs en vivo de un servicio
aws logs tail /ecs/innova/ventas --follow

# Estado de los servicios
aws ecs describe-services --cluster innova-cluster \
  --services innova-front innova-ventas innova-despachos innova-mysql \
  --query "services[].{Name:serviceName,Running:runningCount,Desired:desiredCount}" --output table
```
Métricas de CPU/memoria: **CloudWatch → Container Insights → innova-cluster**.

## Demostrar autoscaling (IE3)

Generar carga sobre un endpoint para superar el 50% de CPU y ver subir las tareas:
```bash
# Ejemplo simple de carga (requiere 'hey' o usar Apache Bench 'ab')
hey -z 3m -c 50 $ALB/api-ventas/api/v1/ventas
# Observar el escalado:
watch -n 10 'aws ecs describe-services --cluster innova-cluster --services innova-ventas \
  --query "services[0].{Running:runningCount,Desired:desiredCount}"'
```

## Limpieza (para no gastar créditos)

```bash
aws cloudformation delete-stack --stack-name innova-ecs
```
> Borra todo (cluster, ALB, VPC, servicios). Los repos de ECR y sus imágenes
> permanecen; elimínalos aparte si quieres.

## Mapeo con la rúbrica EP3

| Indicador | Dónde se cumple |
|-----------|-----------------|
| **IE1** Clúster + VPC/subnets/SG + IAM | `infra/ecs.yml` (VPC, subnets, SG, Cluster, LabRole) |
| **IE2** Despliegue Front+Back desde ECR | Task Definitions + Services + ALB |
| **IE3** Autoscaling | `ScalableTarget*` + `ScalingPolicy*` (Target Tracking 50%) |
| **IE4** Pipeline CI/CD build→push→deploy | `.github/workflows/deploy-ecs.yml` |
| **IE5** Secrets/credenciales | GitHub Secrets (no hay credenciales en el código) |
| **IE6** Logs y métricas | CloudWatch Logs + Container Insights |
| **IE7** Validación Front→Back | ALB path routing + sección de validación |
