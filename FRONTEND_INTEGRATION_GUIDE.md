# 🚀 Plan de Integración: Frontend + Backend + Blockchain

## 📊 Arquitectura Actual

```
┌─────────────────────────────────────────────────────────────┐
│  Vercel (Frontend Deployment)                               │
│  https://siladocs-frontend-3jq3mq13z-...vercel.app          │
│                                                              │
│  React/Next.js Application                                  │
│  - Login/Register                                           │
│  - Gestión de Carreras, Cursos, Planes                     │
│  - Upload de Sílabos                                        │
│  - Ver Historial de Documentos                             │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP/HTTPS
                           │ API Calls
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  Azure Web App (Backend)                                    │
│  https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01...   │
│                                                              │
│  Spring Boot Application                                    │
│  - Autenticación JWT                                        │
│  - CRUD de Carreras, Cursos, Planes                        │
│  - Upload & almacenamiento de archivos (MinIO)             │
│  - Integración con Blockchain                              │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP
                           │ FABRIC_API_URL
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  Azure VM (Blockchain Infrastructure)                      │
│  20.38.34.192                                               │
│                                                              │
│  ✅ Hyperledger Fabric Network (7051, 5984)                │
│  ✅ Fabric Middleware (8000)                               │
│  ✅ PostgreSQL (5432)                                      │
│  ✅ MinIO (9000/9001)                                      │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔧 PASO 1: Actualizar Backend CORS

**Problema:** Backend CORS solo permite `http://localhost:3000`
**Solución:** Actualizar para aceptar Vercel

### Opción A: En Azure Portal (Rápido)

1. Ve a Azure Portal → Busca tu Web App
2. En el menú izquierdo: **Configuration** → **Application settings**
3. Busca o crea variable: `CORS_ALLOWED_ORIGINS`
4. Valor: `https://siladocs-frontend-3jq3mq13z-luis-zarates-projects.vercel.app`
5. Click **Save** y **Restart**

### Opción B: Editar código (Permanente)

**Archivo:** `src/main/java/com/siladocs/security/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        
        // CORS Configuration
        http.cors(cors -> cors.configurationSource(request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",  // Development
                "https://siladocs-frontend-3jq3mq13z-luis-zarates-projects.vercel.app"  // Production
            ));
            config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(Collections.singletonList("*"));
            config.setAllowCredentials(true);
            return config;
        }));
        
        // ... resto de configuración
    }
}
```

**Después de cambiar, hacer commit y push:**

```bash
git add src/main/java/com/siladocs/security/SecurityConfig.java
git commit -m "Update CORS to allow Vercel frontend domain"
git push origin main
```

---

## 🌐 PASO 2: Configurar Frontend con URL del Backend

### Para React/Next.js

**Crear archivo:** `.env.local` (desarrollo) y `.env.production` (producción)

#### `.env.local` (Desarrollo Local)
```env
REACT_APP_API_URL=http://localhost:8080
REACT_APP_FABRIC_URL=http://localhost:8000
```

#### `.env.production` (Vercel)
```env
REACT_APP_API_URL=https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net
REACT_APP_FABRIC_URL=http://20.38.34.192:8000
```

### En Vercel Dashboard

1. Ve a tu proyecto en Vercel: https://vercel.com/dashboard
2. Selecciona **siladocs-frontend**
3. Click en **Settings**
4. **Environment Variables**
5. Añade:

| Nombre | Value | Environments |
|--------|-------|---|
| `REACT_APP_API_URL` | `https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net` | Production |
| `REACT_APP_FABRIC_URL` | `http://20.38.34.192:8000` | Production |

6. **Save** y **Redeploy**

---

## 🔌 PASO 3: Crear Cliente HTTP en Frontend

**Ejemplo para React/TypeScript:**

**Archivo:** `src/utils/api.ts`

```typescript
import axios, { AxiosInstance } from 'axios';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

// Crear instancia de axios
const apiClient: AxiosInstance = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor para agregar JWT en cada request
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken'); // o sessionStorage
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Interceptor para manejar 401 (token expirado)
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expirado o inválido
      localStorage.removeItem('authToken');
      window.location.href = '/login'; // Redirigir a login
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

---

## 🔐 PASO 4: Implementar Autenticación

**Archivo:** `src/services/auth.ts`

```typescript
import apiClient from '../utils/api';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  role: string;
  institutionId: string;
}

export const authService = {
  // Login
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await apiClient.post('/auth/login', credentials);
    const { token } = response.data;
    localStorage.setItem('authToken', token); // Guardar token
    return response.data;
  },

  // Register
  async register(data: RegisterRequest): Promise<LoginResponse> {
    const response = await apiClient.post('/auth/register', data);
    const { token } = response.data;
    localStorage.setItem('authToken', token);
    return response.data;
  },

  // Validate Access Code
  async validateCode(code: string): Promise<{ message: string; institutionName: string }> {
    const response = await apiClient.get(`/auth/validate-code?code=${code}`);
    return response.data;
  },

  // Logout
  logout(): void {
    localStorage.removeItem('authToken');
    window.location.href = '/login';
  },

  // Get current user
  async getCurrentUser() {
    const response = await apiClient.get('/auth/me');
    return response.data;
  },

  // Change password
  async changePassword(currentPassword: string, newPassword: string) {
    const response = await apiClient.post('/auth/change-password', {
      currentPassword,
      newPassword,
    });
    return response.data;
  },
};
```

---

## 📚 PASO 5: Implementar Servicios de API

**Ejemplo:** `src/services/courses.ts`

```typescript
import apiClient from '../utils/api';

export interface Course {
  id: number;
  code: string;
  name: string;
  faculty: string;
  curriculumId: number;
  year: number;
  status: string;
}

export const courseService = {
  // Obtener todos los cursos
  async getAllCourses(): Promise<Course[]> {
    const response = await apiClient.get('/api/courses');
    return response.data;
  },

  // Obtener curso por ID
  async getCourseById(id: number): Promise<Course> {
    const response = await apiClient.get(`/api/courses/${id}`);
    return response.data;
  },

  // Crear curso
  async createCourse(course: Omit<Course, 'id'>): Promise<Course> {
    const response = await apiClient.post('/api/courses', course);
    return response.data;
  },

  // Actualizar curso
  async updateCourse(id: number, course: Partial<Course>): Promise<Course> {
    const response = await apiClient.put(`/api/courses/${id}`, course);
    return response.data;
  },

  // Eliminar curso
  async deleteCourse(id: number): Promise<void> {
    await apiClient.delete(`/api/courses/${id}`);
  },
};
```

---

## 📄 PASO 6: Upload de Sílabos (Blockchain Integration)

**Archivo:** `src/services/syllabi.ts`

```typescript
import apiClient from '../utils/api';

export interface SyllabusResponse {
  syllabusId: number;
  courseId: number;
  version: number;
  fileHash: string;        // SHA-256 (64 hex chars)
  fileUrl: string;          // MinIO URL
  fabricTxId: string;        // ⭐ BLOCKCHAIN TRANSACTION ID
  status: 'create' | 'update' | 'delete';
  createdAt: string;
}

export const syllabusService = {
  // Upload sílabo
  async uploadSyllabus(
    courseId: number,
    file: File,
    action: 'create' | 'update' = 'create'
  ): Promise<SyllabusResponse> {
    const formData = new FormData();
    formData.append('courseId', courseId.toString());
    formData.append('action', action);
    formData.append('file', file);

    const response = await apiClient.post('/api/syllabi/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    // ⭐ VERIFICAR QUE BLOCKCHAIN FUNCIONÓ
    if (!response.data.fabricTxId) {
      throw new Error('Blockchain registration failed - no transaction ID');
    }

    return response.data;
  },

  // Obtener historial de sílabo
  async getSyllabusHistory(syllabusId: number) {
    const response = await apiClient.get(`/api/syllabi/${syllabusId}/history`);
    return response.data;
  },
};
```

**Uso en componente React:**

```jsx
import { syllabusService } from '../services/syllabi';

export function SyllabusUploadForm() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleUpload = async (courseId: number, file: File) => {
    try {
      setLoading(true);
      const result = await syllabusService.uploadSyllabus(courseId, file);
      
      // ✅ BLOCKCHAIN EXITOSO
      console.log('Blockchain TX ID:', result.fabricTxId);
      console.log('File URL:', result.fileUrl);
      
      alert('Sílabo subido exitosamente a blockchain');
    } catch (err) {
      setError(err.message);
      // ❌ BLOCKCHAIN FALLÓ
      console.error('Blockchain error:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={(e) => {
      e.preventDefault();
      const file = /* obtener archivo del form */;
      handleUpload(1, file);
    }}>
      {/* formulario aquí */}
    </form>
  );
}
```

---

## 🧪 PASO 7: Testing de Integración

### Test 1: Verificar que Frontend se conecta al Backend

```bash
# Desde Vercel, en browser console:
fetch('https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net/health')
  .then(r => r.json())
  .then(d => console.log('Backend Status:', d))
  .catch(e => console.error('Error:', e))
```

**Resultado esperado:**
```json
{
  "status": "UP",
  "application": "SilaDocs Backend",
  "fabric_available": true
}
```

### Test 2: Autenticación
```javascript
// En browser console
const creds = {
  email: 'admin@siladocs.com',
  password: 'password123'
};

fetch('https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(creds)
})
  .then(r => r.json())
  .then(d => {
    console.log('Token:', d.token);
    localStorage.setItem('authToken', d.token);
  })
```

### Test 3: Endpoint autenticado
```javascript
const token = localStorage.getItem('authToken');

fetch('https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net/api/courses', {
  headers: { 'Authorization': `Bearer ${token}` }
})
  .then(r => r.json())
  .then(d => console.log('Cursos:', d))
```

---

## ✅ CHECKLIST DE INTEGRACIÓN

- [ ] **Backend CORS actualizado** para aceptar Vercel
- [ ] **Environment variables** en Vercel configuradas
- [ ] **API Client HTTP** creado en frontend con interceptores
- [ ] **Autenticación JWT** implementada (guardar/limpiar token)
- [ ] **Servicios de API** creados (auth, courses, syllabi, etc)
- [ ] **Upload de sílabos** funcionando (verificar `fabricTxId`)
- [ ] **Manejo de errores** 401 (redirigir a login)
- [ ] **Tests en browser** confirman conectividad

---

## 🔗 URLs de Referencia

| Componente | URL |
|-----------|-----|
| **Frontend** | https://siladocs-frontend-3jq3mq13z-luis-zarates-projects.vercel.app |
| **Backend API** | https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net |
| **Backend Swagger** | https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net/swagger-ui/index.html |
| **Fabric Middleware** | http://20.38.34.192:8000 |
| **Vercel Settings** | https://vercel.com/dashboard |

---

## 🚀 IMPLEMENTACIÓN

**Orden recomendado:**

1. ✅ Actualizar CORS en backend
2. ✅ Configurar env vars en Vercel
3. ✅ Crear cliente HTTP en frontend
4. ✅ Implementar autenticación
5. ✅ Implementar servicios de API
6. ✅ Testear en browser console
7. ✅ Integrar con componentes React
8. ✅ Deploy a Vercel

**¿Listo para empezar?** 🚀
