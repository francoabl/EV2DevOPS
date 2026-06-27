# 🚀 Guía rápida: dejar la demo lista antes de presentar

> **Objetivo:** tener el clúster EKS funcionando y la app accesible para mostrarla **en vivo**.
> **Si no alcanzas el tiempo o algo falla → no hay drama: presentas con los GIF y capturas.** La pauta acepta evidencias prácticas.

## ⏱️ Aviso de tiempo (IMPORTANTE)
Crear el clúster tarda **~15-20 min** por sí solo. Con 30 min vas **justo**. Si puedes, **empieza 40-45 min antes**. Lo más lento es el `eksctl create cluster`: lánzalo **lo primero** y mientras tanto haces lo demás.

---

## Antes de empezar (1 vez por PC)
Verifica que estén instalados:
```powershell
aws --version        # AWS CLI v2
kubectl version --client
eksctl version
```
Si falta eksctl: `winget install eksctl.eksctl`

---

## Paso 1 · Iniciar el Learner Lab y credenciales (2 min)
1. Inicia el **Learner Lab** (botón verde).
2. **AWS Details → AWS CLI → Show** y copia el bloque de credenciales.
3. Pégalo en el archivo de credenciales:
   ```powershell
   notepad %UserProfile%\.aws\credentials
   ```
   (reemplaza todo, guarda)
4. Verifica:
   ```powershell
   aws sts get-caller-identity --query Account --output text
   ```
   Debe devolver el número de cuenta. Si el número **NO** es `765529694569`, edítalo en `eks/cluster-config.yaml`.

## Paso 2 · Crear el clúster (LANZAR PRIMERO · ~15-20 min)
```powershell
cd C:\Users\Franco\Desktop\innova
eksctl create cluster -f eks/cluster-config.yaml
```
Déjalo corriendo. Al terminar:
```powershell
kubectl get nodes      # 2 nodos en estado Ready
```

## Paso 3 · metrics-server (para el HPA · 2 min)
```powershell
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl top nodes      # espera ~1 min hasta que muestre CPU/MEM
```

## Paso 4 · Desplegar la app (3-4 min)
**Opción rápida (manual, recomendada para la demo):**
```powershell
$NS="innova"
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/mysql-secret.yaml -n $NS
kubectl apply -f k8s/mysql-deployment.yaml -f k8s/mysql-service.yaml -n $NS
kubectl apply -f k8s/ventas-deployment.yaml -f k8s/ventas-service.yaml -n $NS
kubectl apply -f k8s/despachos-deployment.yaml -f k8s/despachos-service.yaml -n $NS
kubectl apply -f k8s/front-deployment.yaml -f k8s/front-service.yaml -n $NS
kubectl apply -f k8s/ventas-hpa.yaml -f k8s/despachos-hpa.yaml -n $NS
```
> Las imágenes ya están en ECR; los manifiestos las referencian.

**Opción CI/CD (para mostrar el pipeline en vivo):** Actions → *CI/CD Innovatech EKS* → **Run workflow** (recuerda tener los secrets `AWS_*`, `EKS_CLUSTER_NAME=innova-eks`, `EKS_NAMESPACE=innova` actualizados).

## Paso 5 · Obtener la URL pública (2 min)
```powershell
kubectl get svc front -n innova
```
Copia el **EXTERNAL-IP** (`...elb.amazonaws.com`). Tarda ~2 min en aparecer. Ábrelo con **http://** (no https).

## Paso 6 · Verificar que todo responde
```powershell
kubectl get pods -n innova     # todos Running 1/1
```
Abre en el navegador: `http://<EXTERNAL-IP>/` → genera un despacho de prueba.

---

## 🎬 Comandos listos para la DEMO en vivo
Deja **dos terminales** abiertas y estos comandos a mano:

**Terminal A — estado y logs:**
```powershell
kubectl get pods -n innova
kubectl logs -l app=back-ventas -n innova --tail=30        # logs (IE9)
kubectl delete pod -l app=back-ventas -n innova            # self-healing: K8s lo recrea solo
```

**Terminal B — autoscaling en vivo:**
```powershell
kubectl get hpa -n innova -w
# en otra ventana, generar carga:
C:\Users\Franco\tools\hey.exe -z 3m -c 250 http://<EXTERNAL-IP>/api-ventas/api/v1/ventas
```
Muestra cómo `back-ventas` sube de 1 → 4 réplicas y luego baja (scale-in).

---

## 🧹 Al terminar (¡no olvidar!)
```powershell
eksctl delete cluster -f eks/cluster-config.yaml
```
Borra el clúster para no gastar créditos. Luego cierra el lab.

---

## Plan B (sin demo en vivo)
Si no alcanzas o algo falla:
1. Presenta con la presentación + los **GIF** y capturas (ya cubren front, pipeline, HPA y scale-in).
2. Explica con seguridad apoyándote en el **GUION.md**.
3. No pasa nada: la pauta acepta evidencias prácticas como respaldo.
