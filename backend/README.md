```xml
<readme>

# 🧱 Lebens-Rhythmus Backend — Run Guide &amp; Port 8080 Fix  
&gt; Developer reference for **lebens-rhythmus/backend**

---

## 🚀 1. Start the Backend Application

### ▶️ Option 1 — Run Locally (Gradle)
**Windows / Linux**
```bash
./gradlew bootRun 
```
> Make sure Docker (if used) and your database containers are already running.

---

### ▶️ Option 2 — Build and Run with Docker
**Build the image:**
```bash
docker build -t lr-backend .
```

**Run the container:**
```bash
docker run -d -p 8080:8080 --name lr-backend-container lr-backend
```

**Check running containers:**
```bash
docker ps
```

---

## ⚙️ 2. Port 8080 Is Already in Use — How to Fix

### 🔍 Step 1 — Check Which Process Uses Port 8080

#### 🪟 Windows (PowerShell)
```powershell
Get-NetTCPConnection -LocalPort 8080
Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess
```

#### 🐧 Linux (bash)
```bash
sudo lsof -i :8080
```

---

### 🔪 Step 2 — Stop the Process

#### 🪟 Windows
```powershell
Stop-Process -Id <PID> -Force
```

#### 🐧 Linux
```bash
sudo kill -9 <PID>
```

---

### 🧭 Step 3 — (Alternative) Change the Port in Spring Boot

**application.properties**
```properties
server.port=8081
```
or **application.yml**
```yaml
server:
  port: 8081
```

> 💡 Tip: You can also override the port temporarily when starting the app:
> ```bash
> ./gradlew bootRun --args='--server.port=8081'
> ```

---

## 🔄 4. Restart Docker and Containers

#### 🪟 Windows
```powershell
wsl --shutdown
taskkill /IM "Docker Desktop.exe" /F
start-process "C:\Program Files\Docker\Docker\Docker Desktop.exe"
docker compose up --build -d
```

#### 🐧 Linux
```bash
sudo systemctl restart docker
docker compose up --build -d
```

---

## 📘 5. Common Gradle Commands

| Action                | Command             |
|-----------------------|---------------------|
| Build project         | `./gradlew build`   |
| Run app               | `./gradlew bootRun` |
| Clean build directory | `./gradlew clean`   |
| Build and run tests   | `./gradlew test`    |

---

## 🧩 6. Docker Container Management

| Action            | Command                             |
|-------------------|-------------------------------------|
| Check containers  | `docker ps`                         |
| Stop container    | `docker stop lr-backend-container`  |
| Remove container  | `docker rm -f lr-backend-container` |
| Rebuild & restart | `docker compose up --build -d`      |

---

## 🧠 Troubleshooting

| Issue                                                  | Possible Fix                                                |
|--------------------------------------------------------|-------------------------------------------------------------|
| ❌ `error during connect: ... dockerDesktopLinuxEngine` | Restart Docker Desktop or run `wsl --shutdown`              |
| ❌ Port 8080 in use                                     | Follow steps in section 2                                   |
| ❌ `gradlew` not executable (Linux)                     | Run `chmod +x gradlew`                                      |
| ❌ Out of memory                                        | Increase Docker resource limits in **Settings → Resources** |

---

## 📎 Notes

* Default Spring Boot port: **8080**
* Default container name: **lr-backend-container**
* Recommended JDK: **Java 21+**
* Always verify the Docker daemon is running before the container build.

---

© Lebens_Rhythmus Backend Team — Internal Developer Guide
]]>
</readme>
```