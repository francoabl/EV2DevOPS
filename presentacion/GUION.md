# 🎤 Guion de la presentación · EP3 — Innovatech EKS

> **Para los 3 integrantes (Franco, Cristóbal, Máximo).**
> La defensa es **individual**: cada uno debe poder explicar **toda** la solución, no solo "su parte".
> Duración: **10-15 min**. Lenguaje técnico, seguro y claro.
> Estructura por slide: **🗣️ Qué decir** (en voz alta) · **🧠 Clave técnica** (por si preguntan).

---

## Slide 1 · Portada
🗣️ "Buenos días. Somos Franco, Cristóbal y Máximo. Presentamos la **orquestación de la aplicación de Innovatech en Amazon EKS**, con despliegue automatizado y autoescalado."
🧠 Es la EP3 de DevOps: tomamos la app ya contenerizada y la llevamos a un clúster de Kubernetes gestionado por AWS.

## Slide 2 · Contexto y objetivo
🗣️ "La app gestiona **Ventas y Despachos**. El objetivo fue orquestarla en EKS para que sea **escalable, tolerante a fallos y se despliegue sola** desde GitHub."
🧠 Componentes: frontend React (nginx), 2 backends Spring Boot (Ventas y Despachos) y una base de datos MySQL. Antes corría en contenedores sueltos; ahora en un clúster.

## Slide 3 · Arquitectura final
🗣️ "El usuario entra por un **balanceador público (ELB)** que llega al **Frontend**. El frontend, con nginx, **enruta por path** a los dos backends, y **ambos backends** usan la misma base de datos **MySQL**, todo dentro del clúster EKS."
🧠 Flujo de una petición:
- `/` → nginx sirve el SPA (React).
- `/api-ventas/...` → nginx hace proxy al Service `back-ventas:8080`.
- `/api-despachos/...` → proxy al Service `back-despachos:8080`.
- Los backends abren conexión JDBC a `mysql:3306` (nombre de Service, resuelto por el DNS interno de Kubernetes).

## Slide 4 · Clúster EKS
🗣️ "Creamos el clúster con **eksctl**, reutilizando el **LabRole** de AWS Academy. Tiene **2 nodos t3.medium**, Kubernetes **1.32**, y le instalamos **metrics-server**."
🧠 `eksctl create cluster -f eks/cluster-config.yaml`. eksctl crea el VPC, el control plane (gestionado por AWS) y un *managed node group*. metrics-server es un addon que recolecta CPU/memoria de los pods (lo necesita el HPA).

## Slide 5 · Roles IAM y redes
🗣️ "Como Learner Lab no permite crear roles IAM, reutilizamos **LabRole** para el clúster y los nodos. La red es una **VPC con subredes en 2 zonas** y los **Security Groups** los gestiona EKS."
🧠
- **IAM:** LabRole hace de *cluster service role* (control plane) y de *node role* (los EC2 se unen al clúster y leen imágenes de ECR).
- **Redes:** VPC dedicada creada por eksctl, subredes públicas multi-AZ (alta disponibilidad), Security Groups que permiten el tráfico nodo↔nodo, pod↔pod y ELB→pods.

## Slide 6 · Despliegue de servicios
🗣️ "Cada componente es un **Deployment** en Kubernetes. Las imágenes salen de **Amazon ECR**, las **credenciales** de la BD van en un **Secret**, y un **seeder** carga datos iniciales."
🧠
- Cada Deployment define réplicas, imagen y variables de entorno.
- La password de MySQL está en un objeto `Secret` (no en el código) → buena práctica de seguridad.
- El backend de Ventas trae un *seeder*: si la tabla está vacía, inserta 4 órdenes de ejemplo (para que la demo funcione con una BD nueva).

## Slide 7 · Redes y balanceo
🗣️ "El **ELB** entrega el tráfico al **Frontend (nginx)**, y **nginx enruta por path**: `/` al SPA, `/api-ventas` y `/api-despachos` a cada backend."
🧠 El `front` es un Service tipo **LoadBalancer** → AWS crea un ELB público automáticamente. Los backends y MySQL son Services internos (ClusterIP), accesibles solo dentro del clúster por su nombre DNS.

## Slide 8 · Pipeline CI/CD
🗣️ "Con **GitHub Actions**, cada push construye las imágenes, las sube a **ECR** y despliega con **kubectl** al clúster. El run completo es **~4 minutos** y quedó **100% en verde**."
🧠 Etapas: `docker build` → `docker push` a ECR → `aws eks update-kubeconfig` → `kubectl apply` + `kubectl set image` + `kubectl rollout status`. La integración GitHub↔AWS usa las credenciales del lab guardadas como **GitHub Secrets**.

## Slide 9 · Autoscaling con HPA
🗣️ "Configuramos un **Horizontal Pod Autoscaler** con umbral de **50% de CPU**. Bajo carga, la CPU superó el **200%** y el HPA escaló de **1 a 4 réplicas**."
🧠 El HPA lee la CPU vía metrics-server y la compara con el `cpu request` del pod (no con el nodo). Elegimos 50% como umbral equilibrado: reacciona antes de saturar pero sin escalar por picos mínimos. `minReplicas: 1`, `maxReplicas: 4`.

## Slide 10 · Métricas y resultados
🗣️ "Hicimos una prueba de carga de 5 minutos con 250 conexiones: **~260 peticiones/seg**, **100% respuestas exitosas**, y al cesar la carga el HPA hizo **scale-in** de vuelta a 1 réplica."
🧠 Herramienta de carga: `hey`. La disponibilidad 100% (todas 200) demuestra estabilidad bajo estrés. El scale-in tiene una ventana de estabilización (~5 min) para no oscilar.

## Slide 11 · Por qué es resiliente
🗣️ "La solución garantiza los 4 pilares: **escala** (HPA), **alta disponibilidad** (nodos en 2 zonas + réplicas), **tolerancia a fallos** (Kubernetes reinicia pods caídos) y **automatización** (CI/CD y estado deseado)."
🧠 *Self-healing*: si un pod muere, el Deployment lo recrea para mantener el número de réplicas deseado. Los *rolling updates* despliegan versiones nuevas sin cortar el servicio.

## Slide 12 · La aplicación en marcha
🗣️ "Aquí la app funcionando: listamos órdenes de compra desde el backend de Ventas y generamos un despacho que se guarda en MySQL, todo a través de la URL pública."
🧠 (Si hay demo en vivo: abre la URL del ELB, muestra el listado y crea un despacho.) Esto valida la comunicación **Front → Back → BD** de punta a punta.

## Slide 13 · Problemas y soluciones
🗣️ "Tres obstáculos: **Cloud Map bloqueado** en Learner Lab — resuelto con el DNS nativo de Kubernetes; la **versión de EKS** 1.29 sin soporte — subimos a 1.32; y **metrics-server/HTTP** — los ajustamos."
🧠 En el intento previo con ECS, AWS Academy bloqueaba `servicediscovery:CreatePrivateDnsNamespace`. En EKS ese problema desaparece porque Kubernetes ya trae DNS interno (CoreDNS).

## Slide 14 · Lecciones y proyección
🗣️ "Aprendimos que EKS en Learner Lab exige reutilizar LabRole, que el DNS interno simplifica todo y que metrics-server es clave para el HPA. Para producción real: **RDS** para la BD, **HTTPS** y **observabilidad** con CloudWatch."
🧠 Proyección concreta para Innovatech: base de datos gestionada y persistente (RDS), certificado TLS (ACM + Ingress), métricas y alarmas, y más réplicas en varias zonas.

## Slide 15 · Conclusión
🗣️ "En resumen: logramos una app **orquestada, escalable y automatizada** en EKS, con CI/CD funcional y autoscaling demostrado con evidencia. Gracias, quedamos atentos a sus preguntas."

---

# 🧠 Cómo funciona todo (referencia técnica para la defensa)

**¿Qué es EKS?** El servicio de AWS que gestiona un clúster de **Kubernetes**. AWS administra el *control plane*; nosotros ponemos los *nodos* (EC2) donde corren los contenedores (pods).

**¿Qué es un pod / Deployment / Service?**
- **Pod:** la unidad mínima; envuelve uno o más contenedores.
- **Deployment:** mantiene N réplicas de un pod y las recrea si caen (self-healing).
- **Service:** un nombre/IP estable para acceder a esos pods. `ClusterIP` = interno; `LoadBalancer` = expone con un ELB público.

**¿Cómo se comunican los servicios?** Por **DNS interno de Kubernetes**: un backend llama a `mysql:3306` y CoreDNS lo resuelve a la IP del Service. Por eso no necesitamos Cloud Map.

**¿Cómo entra el tráfico?** Internet → **ELB** (creado por el Service `front` tipo LoadBalancer) → pod del frontend → **nginx** enruta por path a los backends.

**¿Cómo escala?** El **HPA** consulta la CPU (vía metrics-server) cada pocos segundos; si supera el 50% del *request*, añade réplicas (hasta 4); si baja, las quita.

**¿Cómo se despliega?** Push a GitHub → **GitHub Actions** construye la imagen, la sube a **ECR** y ejecuta `kubectl` contra el clúster. Versionamos cada imagen con el hash del commit.

**¿Dónde están las credenciales?** Las de AWS, en **GitHub Secrets**; la de la BD, en un **Secret de Kubernetes**. Nunca en el código.

---

# ❓ Preguntas probables del profe (y respuestas cortas)

- **¿Por qué ECS o EKS? ¿Por qué eligieron EKS?** → Porque usa Kubernetes (estándar de la industria) y se gestiona con `kubectl`, como se vio en clase.
- **¿Por qué no usaron `kubectl` en ECS?** → `kubectl` es de Kubernetes/EKS; en ECS el equivalente es AWS CLI + CloudWatch. Por eso, al pedir EKS, todo encaja.
- **¿Cómo garantizan alta disponibilidad?** → Nodos en 2 zonas de disponibilidad y varias réplicas detrás del ELB; si una cae, el resto responde.
- **¿Qué pasa si se cae un pod?** → El Deployment lo recrea automáticamente (self-healing). Lo podemos demostrar borrando un pod.
- **¿Cómo eligieron el umbral del 50%?** → Equilibrio: reacciona antes de saturar, sin escalar por picos breves.
- **¿La base de datos persiste?** → En esta demo no (sin volumen), por eso el seeder recarga datos. En producción usaríamos RDS con almacenamiento persistente.
- **¿Es seguro?** → Credenciales en Secrets (no en el código); acceso solo por el ELB; los backends y la BD no se exponen a internet.
- **¿Por qué solo HTTP?** → El ELB escucha en el puerto 80. Para HTTPS añadiríamos un certificado ACM. No era requisito de la pauta.
- **¿Cuánto tarda un despliegue?** → ~4 minutos el pipeline completo (build + push + deploy).
