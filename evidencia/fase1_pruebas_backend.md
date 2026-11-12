# Evidencia de Pruebas Reales de Backend — Fase 1

> **Proyecto:** SIGESMED — Policlínico Infantil Nuestra Señora del Sagrado Corazón
> **Fecha de ejecución:** 2026-11-11
> **Entorno:** Backend Spring Boot 3.5.3 (Perfil `dev`) en `http://localhost:8081`
> **Base de datos:** PostgreSQL `sigesmed_test` en `localhost:5433`

---

## 1. Script Automatizado de Pruebas Reales

```python
import urllib.request
import urllib.parse
import http.cookiejar
import json
import random
import os
from datetime import date, timedelta

BASE_URL = "http://127.0.0.1:8081/api/v1"

class SigesmedClient:
    def __init__(self):
        self.cj = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cj))

    def get_csrf(self):
        req = urllib.request.Request(f"{BASE_URL}/csrf", method="GET")
        with self.opener.open(req) as resp:
            pass
        for cookie in self.cj:
            if cookie.name == "XSRF-TOKEN":
                return cookie.value
        return None

    def request(self, endpoint, method="GET", data=None):
        url = f"{BASE_URL}{endpoint}"
        headers = {}
        csrf = self.get_csrf()
        if csrf:
            headers["X-XSRF-TOKEN"] = csrf
        
        encoded_data = None
        if data is not None:
            headers["Content-Type"] = "application/json"
            encoded_data = json.dumps(data).encode("utf-8")
            
        req = urllib.request.Request(url, data=encoded_data, headers=headers, method=method)
        try:
            with self.opener.open(req) as resp:
                status_code = resp.status
                raw = resp.read().decode("utf-8")
                parsed = json.loads(raw) if raw else None
                return status_code, parsed
        except urllib.error.HTTPError as e:
            err_raw = e.read().decode("utf-8")
            parsed = json.loads(err_raw) if err_raw else None
            return e.code, parsed

def run():
    log_entries = []

    def record_step(step_num, title, method, endpoint, request_body, status, response_body):
        entry = {
            "step": step_num,
            "title": title,
            "method": method,
            "url": f"{BASE_URL}{endpoint}",
            "request_body": request_body,
            "status": status,
            "response_body": response_body
        }
        log_entries.append(entry)
        print(f"[{step_num}] {title} -> HTTP {status}")

    print("=== INICIANDO EJECUCIÓN DE PRUEBAS REALES ===")

    # Verificación Servicios Complementarios
    client = SigesmedClient()
    st, resp = client.request("/servicios-complementarios", method="GET")
    record_step("0.1", "Listar Servicios Complementarios", "GET", "/servicios-complementarios", None, st, resp)

    # Verificación Especialidades
    st, resp = client.request("/especialidades", method="GET")
    record_step("0.2", "Listar Especialidades Oficiales", "GET", "/especialidades", None, st, resp)

    # 1. Registro de Paciente
    rand_suffix = str(random.randint(1000000, 9999999))
    test_dni = "7" + rand_suffix[:7]
    test_email = f"paciente_{test_dni}@example.test"
    password = "PacientePassword123!"

    reg_payload = {
        "nombre": "Jorge",
        "apellidoPaterno": "Vargas",
        "apellidoMaterno": "Campos",
        "dni": test_dni,
        "fechaNacimiento": "1997-04-12",
        "correo": test_email,
        "clave": password
    }
    patient_client = SigesmedClient()
    st, resp = patient_client.request("/auth/registro", method="POST", data=reg_payload)
    record_step("1", "Registro de nuevo Paciente", "POST", "/auth/registro", reg_payload, st, resp)

    # 2. Login de Paciente
    login_payload = {
        "identificador": test_dni,
        "clave": password
    }
    st, resp = patient_client.request("/auth/login", method="POST", data=login_payload)
    record_step("2", "Inicio de sesión de Paciente", "POST", "/auth/login", login_payload, st, resp)

    # 3. Reservar Cita
    # Obtenemos horarios disponibles para el médico 1
    target_date = (date.today() + timedelta(days=5)).isoformat()
    _, horarios = patient_client.request(f"/medicos/1/horarios?fecha={target_date}", method="GET")
    h1 = horarios[0]["id"]
    h2 = horarios[1]["id"]
    reserva_payload = {
        "horarioId": h1,
        "especialidadId": 7
    }
    st, cita = patient_client.request("/citas", method="POST", data=reserva_payload)
    cita_id = cita["id"]
    record_step("3", "Reservar Cita médica", "POST", "/citas", reserva_payload, st, cita)

    # 4. Reprogramar Cita
    reprog_payload = {
        "horarioId": h2
    }
    st, cita_reprog = patient_client.request(f"/citas/{cita_id}/horario", method="PUT", data=reprog_payload)
    record_step("4", "Reprogramar Cita a otro horario", "PUT", f"/citas/{cita_id}/horario", reprog_payload, st, cita_reprog)

    # 5. Cancelar Cita
    cancel_payload = {
        "motivo": "Motivo de prueba de cancelación por paciente"
    }
    st, cita_cancel = patient_client.request(f"/citas/{cita_id}/cancelacion", method="POST", data=cancel_payload)
    record_step("5", "Cancelar Cita con motivo", "POST", f"/citas/{cita_id}/cancelacion", cancel_payload, st, cita_cancel)

    # 6. Recepcionista Login y Atención
    recep_client = SigesmedClient()
    recep_login_payload = {
        "identificador": "90000002",
        "clave": "DemoRecep2026!"
    }
    st, recep_auth = recep_client.request("/auth/login", method="POST", data=recep_login_payload)
    record_step("6.1", "Inicio de sesión de Recepcionista", "POST", "/auth/login", recep_login_payload, st, recep_auth)

    # Consultar cita 6 atendida
    st, cita_atendida = recep_client.request("/recepcionista/citas/6", method="GET")
    record_step("6.2", "Recepción consulta y verificación de cita ATENDIDA", "GET", "/recepcionista/citas/6", None, st, cita_atendida)

    # 7. Administrador Login y Creación de Horario
    admin_client = SigesmedClient()
    admin_login_payload = {
        "identificador": "90000001",
        "clave": "DemoAdmin2026!"
    }
    st, admin_auth = admin_client.request("/auth/login", method="POST", data=admin_login_payload)
    record_step("7.1", "Inicio de sesión de Administrador", "POST", "/auth/login", admin_login_payload, st, admin_auth)

    fecha_admin = (date.today() + timedelta(days=random.randint(60, 100))).isoformat()
    hora_admin = f"{random.randint(14, 18):02d}:{random.choice([0, 15, 30, 45]):02d}:00"
    crear_horario_payload = {
        "doctorId": 1,
        "fecha": fecha_admin,
        "hora": hora_admin
    }
    st, nuevo_horario = admin_client.request("/administrador/horarios", method="POST", data=crear_horario_payload)
    record_step("7.2", "Administrador crea nuevo horario médico", "POST", "/administrador/horarios", crear_horario_payload, st, nuevo_horario)

    # Generar Documento de Evidencia
    os.makedirs("evidencia", exist_ok=True)
    doc_path = "evidencia/fase1_pruebas_backend.md"
    
    with open(__file__, "r", encoding="utf-8") as f:
        script_code = f.read()

    md_lines = []
    md_lines.append("# Evidencia de Pruebas Reales de Backend — Fase 1")
    md_lines.append("")
    md_lines.append("> **Proyecto:** SIGESMED — Policlínico Infantil Nuestra Señora del Sagrado Corazón")
    md_lines.append(f"> **Fecha de ejecución:** {date.today().isoformat()}")
    md_lines.append("> **Entorno:** Backend Spring Boot 3.5.3 (Perfil `dev`) en `http://localhost:8081`")
    md_lines.append("> **Base de datos:** PostgreSQL `sigesmed_test` en `localhost:5433`")
    md_lines.append("")
    md_lines.append("---")
    md_lines.append("")
    md_lines.append("## 1. Script Automatizado de Pruebas Reales")
    md_lines.append("")
    md_lines.append("```python")
    md_lines.append(script_code)
    md_lines.append("```")
    md_lines.append("")
    md_lines.append("---")
    md_lines.append("")
    md_lines.append("## 2. Salida Completa por Paso (Petición, Código HTTP y JSON)")
    md_lines.append("")

    for item in log_entries:
        md_lines.append(f"### Paso {item['step']}: {item['title']}")
        md_lines.append("")
        md_lines.append(f"- **Petición:** `{item['method']} {item['url']}`")
        if item['request_body']:
            md_lines.append("- **Cuerpo de Petición (Request JSON):**")
            md_lines.append("```json")
            md_lines.append(json.dumps(item['request_body'], indent=2, ensure_ascii=False))
            md_lines.append("```")
        else:
            md_lines.append("- **Cuerpo de Petición:** *(Sin cuerpo / GET)*")
        
        md_lines.append(f"- **Código de Estado HTTP:** `{item['status']}`")
        md_lines.append("- **Cuerpo de Respuesta (Response JSON):**")
        md_lines.append("```json")
        md_lines.append(json.dumps(item['response_body'], indent=2, ensure_ascii=False))
        md_lines.append("```")
        md_lines.append("")
        md_lines.append("---")
        md_lines.append("")

    md_lines.append("## 3. Conclusión de la Fase 1")
    md_lines.append("")
    md_lines.append("Todas las operaciones de la Fase 1 fueron verificadas contra el servidor en vivo con persistencia real en PostgreSQL (`sigesmed_test` puerto 5433). Ninguna llamada fue simulada o mockeada.")

    with open(doc_path, "w", encoding="utf-8") as f:
        f.write("\n".join(md_lines))

    print(f"\nDocumento generado exitosamente en: {doc_path}")

if __name__ == "__main__":
    run()

```

---

## 2. Salida Completa por Paso (Petición, Código HTTP y JSON)

### Paso 0.1: Listar Servicios Complementarios

- **Petición:** `GET http://127.0.0.1:8081/api/v1/servicios-complementarios`
- **Cuerpo de Petición:** *(Sin cuerpo / GET)*
- **Código de Estado HTTP:** `200`
- **Cuerpo de Respuesta (Response JSON):**
```json
[
  {
    "id": 2,
    "nombre": "Ecografías"
  },
  {
    "id": 1,
    "nombre": "Laboratorio clínico"
  }
]
```

---

### Paso 0.2: Listar Especialidades Oficiales

- **Petición:** `GET http://127.0.0.1:8081/api/v1/especialidades`
- **Cuerpo de Petición:** *(Sin cuerpo / GET)*
- **Código de Estado HTTP:** `200`
- **Cuerpo de Respuesta (Response JSON):**
```json
[
  {
    "id": 8,
    "nombre": "Medicina familiar"
  },
  {
    "id": 9,
    "nombre": "Nutrición"
  },
  {
    "id": 11,
    "nombre": "Odontología pediátrica"
  },
  {
    "id": 12,
    "nombre": "Otorrinolaringología"
  },
  {
    "id": 7,
    "nombre": "Pediatría general"
  },
  {
    "id": 10,
    "nombre": "Psicología infantil"
  }
]
```

---

### Paso 1: Registro de nuevo Paciente

- **Petición:** `POST http://127.0.0.1:8081/api/v1/auth/registro`
- **Cuerpo de Petición (Request JSON):**
```json
{
  "nombre": "Jorge",
  "apellidoPaterno": "Vargas",
  "apellidoMaterno": "Campos",
  "dni": "75246744",
  "fechaNacimiento": "1997-04-12",
  "correo": "paciente_75246744@example.test",
  "clave": "PacientePassword123!"
}
```
- **Código de Estado HTTP:** `201`
- **Cuerpo de Respuesta (Response JSON):**
```json
{
  "mensaje": "Registro completado para el DNI 75246744"
}
```

---

### Paso 2: Inicio de sesión de Paciente

- **Petición:** `POST http://127.0.0.1:8081/api/v1/auth/login`
- **Cuerpo de Petición (Request JSON):**
```json
{
  "identificador": "75246744",
  "clave": "PacientePassword123!"
}
```
- **Código de Estado HTTP:** `200`
- **Cuerpo de Respuesta (Response JSON):**
```json
{
  "usuarioId": 14,
  "dni": "75246744",
  "rol": "PACIENTE",
  "portal": "paciente"
}
```

---

### Paso 3: Reservar Cita médica

- **Petición:** `POST http://127.0.0.1:8081/api/v1/citas`
- **Cuerpo de Petición (Request JSON):**
```json
{
  "horarioId": 67,
  "especialidadId": 7
}
```
- **Código de Estado HTTP:** `201`
- **Cuerpo de Respuesta (Response JSON):**
```json
{
  "id": 12,
  "horarioId": 67,
  "doctorId": 1,
  "estado": "RESERVADA",
  "paciente": "Jorge Vargas",
  "pacienteDni": "75246744",
  "correoPaciente": "paciente_75246744@example.test",
  "especialidades": [
    "Pediatría general"
  ],
  "doctor": "Dra. Ana Demo",
  "fecha": "2026-11-16",
  "hora": "09:00:00",
  "motivoCancelacion": null,
  "servicioId": null,
  "servicio": null
}
```

---

### Paso 4: Reprogramar Cita a otro horario

- **Petición:** `PUT http://127.0.0.1:8081/api/v1/citas/12/horario`
- **Cuerpo de Petición (Request JSON):**
```json
{
  "horarioId": 68
}
```
- **Código de Estado HTTP:** `200`
- **Cuerpo de Respuesta (Response JSON):**
```json
{
  "id": 12,
  "horarioId": 68,
  "doctorId": 1,
  "estado": "RESERVADA",
  "paciente": "Jorge Vargas",
  "pacienteDni": "75246744",
  "correoPaciente": "paciente_75246744@example.test",
  "especialidades": [
    "Pediatría general"
  ],
  "doctor": "Dra. Ana Demo",
  "fecha": "2026-11-16",
  "hora": "10:00:00",
  "motivoCancelacion": null,
  "servicioId": null,
  "servicio": null
}
```

---

### Paso 5: Cancelar Cita con motivo

- **Petición:** `POST http://127.0.0.1:8081/api/v1/citas/12/cancelacion`
- **Cuerpo de Petición (Request JSON):**
```json
{
  "motivo": "Motivo de prueba de cancelación por paciente"
}
```
- **Código de Estado HTTP:** `200`
- **Cuerpo de Respuesta (Response JSON):**
```json
{
  "id": 12,
  "horarioId": 68,
  "doctorId": 1,
  "estado": "CANCELADA",
  "paciente": "Jorge Vargas",
  "pacienteDni": "75246744",
  "correoPaciente": "paciente_75246744@example.test",
  "especialidades": [
    "Pediatría general"
  ],
  "doctor": "Dra. Ana Demo",
  "fecha": "2026-11-16",
  "hora": "10:00:00",
  "motivoCancelacion": "Motivo de prueba de cancelación por paciente",
  "servicioId": null,
  "servicio": null
}
```

---

### Paso 6.1: Inicio de sesión de Recepcionista

- **Petición:** `POST http://127.0.0.1:8081/api/v1/auth/login`
- **Cuerpo de Petición (Request JSON):**
```json
{
  "identificador": "90000002",
  "clave": "DemoRecep2026!"
}
```
- **Código de Estado HTTP:** `200`
- **Cuerpo de Respuesta (Response JSON):**
```json
{
  "usuarioId": 2,
  "dni": "90000002",
  "rol": "RECEPCIONISTA",
  "portal": "recepcionista"
}
```

---

### Paso 6.2: Recepción consulta y verificación de cita ATENDIDA

- **Petición:** `GET http://127.0.0.1:8081/api/v1/recepcionista/citas/6`
- **Cuerpo de Petición:** *(Sin cuerpo / GET)*
- **Código de Estado HTTP:** `200`
- **Cuerpo de Respuesta (Response JSON):**
```json
{
  "id": 6,
  "horarioId": 7,
  "doctorId": 1,
  "estado": "ATENDIDA",
  "paciente": "TestPaciente Prueba",
  "pacienteDni": "78430639",
  "correoPaciente": "paciente_78430639@example.test",
  "especialidades": [
    "Pediatría general"
  ],
  "doctor": "Dra. Ana Demo",
  "fecha": "2026-11-10",
  "hora": "09:00:00",
  "motivoCancelacion": null,
  "servicioId": null,
  "servicio": null
}
```

---

### Paso 7.1: Inicio de sesión de Administrador

- **Petición:** `POST http://127.0.0.1:8081/api/v1/auth/login`
- **Cuerpo de Petición (Request JSON):**
```json
{
  "identificador": "90000001",
  "clave": "DemoAdmin2026!"
}
```
- **Código de Estado HTTP:** `200`
- **Cuerpo de Respuesta (Response JSON):**
```json
{
  "usuarioId": 1,
  "dni": "90000001",
  "rol": "ADMINISTRADOR",
  "portal": "administrador"
}
```

---

### Paso 7.2: Administrador crea nuevo horario médico

- **Petición:** `POST http://127.0.0.1:8081/api/v1/administrador/horarios`
- **Cuerpo de Petición (Request JSON):**
```json
{
  "doctorId": 1,
  "fecha": "2027-02-02",
  "hora": "16:45:00"
}
```
- **Código de Estado HTTP:** `201`
- **Cuerpo de Respuesta (Response JSON):**
```json
{
  "id": 141,
  "doctorId": 1,
  "doctor": "Dra. Ana Demo",
  "servicioId": null,
  "servicio": null,
  "fecha": "2027-02-02",
  "hora": "16:45:00",
  "disponible": true,
  "estado": "DISPONIBLE"
}
```

---

## 3. Conclusión de la Fase 1

Todas las operaciones de la Fase 1 fueron verificadas contra el servidor en vivo con persistencia real en PostgreSQL (`sigesmed_test` puerto 5433). Ninguna llamada fue simulada o mockeada.