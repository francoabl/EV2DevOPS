# Despliegue en AWS EKS (Kubernetes) — Innovatech EP3

Orquestación de la app (Front + 2 Backends + MySQL) en **Amazon EKS**, con
manifiestos de Kubernetes, **HPA** (autoscaling) y pipeline **CI/CD** que despliega
con `kubectl`. Adaptado a **AWS Academy Learner Lab** (usa `LabRole`).

## Arquitectura

```
Internet
   │
   ▼
Service "front" (type LoadBalancer) → ELB público
   │
   ▼  (nginx del front enruta por path)
   ├─ /*                → estáticos del SPA
   ├─ /api-ventas/*     → Service back-ventas:8080
   └─ /api-despachos/*  → Service back-despachos:8080

Namespace "innova" (EKS / Kubernetes)
   ├─ Deployment front           (1 réplica)
   ├─ Deployment back-ventas      + HPA (1→4, CPU 50%)
   ├─ Deployment back-despachos   + HPA (1→4, CPU 50%)
   └─ Deployment mysql  → Service "mysql:3306" (DNS interno de K8s)

Secret "mysql-secret"  → password de la BD (no va en el código)
```

En K8s el descubrimiento de servicios es **nativo**: `mysql`, `back-ventas` y
`back-despachos` se resuelven por nombre. No hace falta Cloud Map ni NLB.

---

## Requisitos (instalar una vez)

| Herramienta | Para qué | Instalar en Windows |
|-------------|----------|---------------------|
| **AWS CLI v2** | hablar con AWS | ya la tienes |
| **kubectl** | operar el clúster | ya la tienes (Docker Desktop) |
| **eksctl** | crear el clúster | `winget install Weaveworks.eksctl` (o descarga de github.com/eksctl-io/eksctl/releases) |

Verifica: `eksctl version` y `kubectl version --client`.

---

## Paso 1 — Crear el clúster EKS

> ⚠️ Inicia el **Learner Lab** (botón verde) antes. El clúster tarda **~15-20 min**.
> Verifica tu nº de cuenta y, si difiere de `765529694569`, edítalo en
> `eks/cluster-config.yaml`:
> ```
> aws sts get-caller-identity --query Account --output text
> ```

```bash
eksctl create cluster -f eks/cluster-config.yaml
```

Esto crea el control plane + 2 nodos `t3.medium`, usando `LabRole` (sin crear IAM).
Al terminar, `eksctl` configura tu `kubectl` automáticamente. Verifica:

```bash
kubectl get nodes        # debes ver 2 nodos en estado Ready
```

<details>
<summary><b>Plan B: si eksctl falla en el lab — crear por consola</b></summary>

1. Consola → **EKS → Add cluster → Create**. Nombre `innova-eks`, rol de clúster = **LabRole**, subredes por defecto.
2. Cuando esté `Active` (~10 min): pestaña **Compute → Add node group**. Rol del nodo = **LabRole**, tipo `t3.medium`, 2 nodos.
3. En tu PC: `aws eks update-kubeconfig --region us-east-1 --name innova-eks`
</details>

---

## Paso 2 — Instalar metrics-server (necesario para el HPA)

EKS **no** trae metrics-server, y sin él el HPA no puede leer la CPU:

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

Verifica (espera ~1 min): `kubectl top nodes` debe mostrar CPU/memoria.

---

## Paso 3 — Configurar los GitHub Secrets

En GitHub → **Settings → Secrets and variables → Actions**:

| Secret | Valor |
|--------|-------|
| `AWS_ACCESS_KEY_ID` | del Learner Lab (AWS Details → AWS CLI) |
| `AWS_SECRET_ACCESS_KEY` | idem |
| `AWS_SESSION_TOKEN` | idem (caduca al cerrar el lab) |
| `AWS_REGION` | `us-east-1` |
| `EKS_CLUSTER_NAME` | `innova-eks` |
| `EKS_NAMESPACE` | `innova` |

---

## Paso 4 — Desplegar

**Automático (pipeline):** haz `push` a la rama `main`. El workflow
`deploy-eks.yml` construye las imágenes, las sube a ECR y aplica todos los
manifiestos con `kubectl`.

**Manual (desde tu PC, opcional):**
```bash
NS=innova
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/mysql-secret.yaml -n $NS
kubectl apply -f k8s/mysql-deployment.yaml -f k8s/mysql-service.yaml -n $NS
kubectl apply -f k8s/ventas-deployment.yaml -f k8s/ventas-service.yaml -n $NS
kubectl apply -f k8s/despachos-deployment.yaml -f k8s/despachos-service.yaml -n $NS
kubectl apply -f k8s/front-deployment.yaml -f k8s/front-service.yaml -n $NS
kubectl apply -f k8s/ventas-hpa.yaml -f k8s/despachos-hpa.yaml -n $NS
```

---

## Paso 5 — Obtener la URL y validar

```bash
kubectl get svc front -n innova
# Copia el EXTERNAL-IP (un dominio ...elb.amazonaws.com). Tarda ~2 min en aparecer.
```

Abre `http://<EXTERNAL-IP>/` (¡con **http**!). Pruebas:
```bash
URL=http://<EXTERNAL-IP>
curl -i $URL/                          # Front
curl -i $URL/api-ventas/api/v1/ventas  # Backend ventas (4 órdenes por el seeder)
```

---

## Paso 6 — Demostrar el HPA (autoscaling, IE3)

En una terminal, observa el HPA y las réplicas en vivo:
```bash
kubectl get hpa -n innova -w
```
En otra, genera carga (5 min) contra la URL pública:
```bash
C:\Users\Franco\tools\hey.exe -z 5m -c 250 http://<EXTERNAL-IP>/api-ventas/api/v1/ventas
```
Verás el HPA subir el `TARGETS` sobre 50% y los `REPLICAS` de back-ventas pasar de **1 → 2/3/4**. Captura:
```bash
kubectl get hpa -n innova
kubectl get pods -n innova        # varios pods de back-ventas
kubectl top pods -n innova        # CPU por pod
```

---

## Logs (IE6) — el equivalente a CloudWatch

```bash
kubectl logs -l app=back-ventas -n innova --tail=100 -f   # logs en vivo
kubectl describe pod <pod> -n innova                       # eventos del pod
```

---

## Limpieza (¡IMPORTANTE para no gastar créditos!)

EKS cobra el control plane (~$0.10/h) + los nodos. Cuando termines:
```bash
eksctl delete cluster -f eks/cluster-config.yaml
```

---

## Solución de problemas

| Síntoma | Causa / Fix |
|---------|-------------|
| `kubectl get nodes` vacío o NotReady | nodos aún uniéndose; espera. Revisa que el node group use LabRole. |
| HPA muestra `<unknown>` en TARGETS | falta metrics-server (Paso 2) o aún no recolecta (~1 min). |
| Pod en `ImagePullBackOff` | la imagen aún no está en ECR, o el tag no coincide. Re-corre el pipeline. |
| Front en `CrashLoopBackOff` | arrancó antes que los Services de backend; se recupera solo al reintentar. |
| `front` sin EXTERNAL-IP | el ELB tarda ~2 min; si nunca aparece, revisa permisos de ELB en el lab. |
| Pipeline `ExpiredToken` | credenciales del lab caducadas; actualiza los 3 secrets de AWS. |

## Mapeo con la rúbrica

| IE | Dónde |
|----|-------|
| IE1 Clúster EKS + nodos + IAM | `eks/cluster-config.yaml` (LabRole) |
| IE2 Front+Back desde ECR | `k8s/*-deployment.yaml` + Services |
| IE3 Autoscaling | `k8s/*-hpa.yaml` (HPA CPU 50%) + metrics-server |
| IE4 Pipeline CI/CD | `.github/workflows/deploy-eks.yml` |
| IE5 Secrets | `k8s/mysql-secret.yaml` + GitHub Secrets |
| IE6 Logs/métricas | `kubectl logs` / `kubectl top` |
| IE7 Validación Front→Back | Service LoadBalancer + nginx routing |
