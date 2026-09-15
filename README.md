# Weekly Report Generator - Setup Guide

This README contains only the steps needed to set up and run the application locally.

## 1. Prerequisites

Install these first:

- Docker Desktop
- Java JDK 21
- Apache Maven
- Node.js 20 or newer
- npm

Check your installed versions:

```powershell
java -version
mvn -v
node -v
npm -v
docker --version
```

If `mvn -v` does not work, install Maven first.

### Install Maven On Windows

Option 1: install with `winget`:

```powershell
winget install Apache.Maven
```

Close PowerShell, open it again, and check:

```powershell
mvn -v
```

Option 2: install manually:

1. Download Apache Maven from the official Maven download page.
2. Extract it, for example to:

```text
C:\Program Files\Apache\maven
```

3. Add Maven's `bin` folder to your Windows `Path` environment variable:

```text
C:\Program Files\Apache\maven\bin
```

4. Open a new PowerShell terminal and check:

```powershell
mvn -v
```

If Java is installed but Maven cannot detect it, set `JAVA_HOME`:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
```

Change the path if the JDK is installed somewhere else.

## 2. Start MySQL

Start the MySQL Docker container and please makesure to keep the docker desktop application open while entering the below commands:

```powershell
docker compose up -d mysql
```

Check that it is running:

```powershell
docker ps
```

You should see a container named:

```text
wrg-mysql
```

Database details:

```text
Host: localhost
Port: 3306
Database: wrg
Username: wrg
Password: wrg_password
```

## 3. Start The Backend (To start with AI chatbot please refer part 6)

Open a new PowerShell terminal inside the backend folder and makesure the mysql docker container is running before starting the backend:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173,http://127.0.0.1:5173,http://127.0.0.1:5174"
mvn spring-boot:run
```

The backend runs at:

```text
http://localhost:8080
```

API base URL:

```text
http://localhost:8080/api
```

Keep this terminal open while using the app.

## 4. Start The Frontend

Open another PowerShell terminal inside the frontend folder:

```powershell
npm install
npm run dev
```

Vite will show a local URL, usually:

```text
http://127.0.0.1:5173/
```

If port `5173` is busy, Vite may use `5174`. Open the URL printed in the terminal.

Keep this terminal open while using the app.

## 5. Login Details

Seed data is created automatically when the backend starts.

All seeded users use this password:

```text
password123
```

Default accounts:

| Role        | Email               | Password      |
| ----------- | ------------------- | ------------- |
| Admin       | `admin@wrg.local`   | `password123` |
| Manager     | `manager@wrg.local` | `password123` |
| Team member | `aria@wrg.local`    | `password123` |
| Team member | `ben@wrg.local`     | `password123` |
| Team member | `chen@wrg.local`    | `password123` |
| Team member | `dina@wrg.local`    | `password123` |

## 6. Optional: Enable AI Chat With OpenRouter

If you want the AI chat assistant to work, set your OpenRouter key before starting the backend with opening a Powershell/terminal inside the backend folder:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:OPENROUTER_API_KEY="your_openrouter_key_here"
$env:OPENROUTER_MODEL="openrouter/free"
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173,http://127.0.0.1:5173,http://127.0.0.1:5174"
mvn spring-boot:run
```

For the testing use the OPENROUTER_API_KEY as "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"

If you do not set an API key, the app still runs normally. Only the AI chat will show a setup message.

## 7. Optional: Access The Database

Open MySQL inside the Docker container:

```powershell
docker exec -it wrg-mysql mysql -uwrg -pwrg_password wrg
```

## 8. Optional: Reset The Database

This deletes all local MySQL data and recreates a fresh database with opening a powershell/terminal inside the root folder.

```powershell
docker compose down -v
docker compose up -d mysql
```

After that, restart the backend. Seed users and sample data will be created again.

## 9. Run Checks

Backend tests with opening powershell/terminal inside the backend folder:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
mvn test
```

Frontend build with opening powershell/terminal inside the frontend folder:

```powershell
npm run build
```

## 10. Common Issues

### Frontend Uses Port 5174

That is okay. Open the URL printed by Vite.

If backend requests fail, restart the backend with:

```powershell
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173,http://127.0.0.1:5173,http://127.0.0.1:5174"
```

### Backend Cannot Connect To MySQL

Make sure Docker Desktop is running, then run powershell/terminal inside the root folder:

```powershell
docker compose up -d mysql
docker ps
```

### Maven Command Not Found

Install Maven and make sure Maven's `bin` folder is added to your Windows `Path`.

Check Maven:

```powershell
mvn -v
```

If the command still fails, close PowerShell and open a new terminal so Windows reloads the updated `Path`.

### Wrong Java Version

The backend needs Java 21:

```powershell
java -version
```

Then set:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
```
