# TaskFlow – Complete Setup Guide

## Two Ways to Run

- **Option A** → Docker (easiest, one command, no installs needed except Docker)
- **Option B** → Manual (run each piece separately, good for development)

---

## OPTION A – Docker Compose (Recommended)

### Step 1 – Install Docker Desktop

Download and install from: https://www.docker.com/products/docker-desktop/

Verify it works:
```
docker --version
docker compose version
```

### Step 2 – Get the project files

You already have the `taskflow/` folder from the previous step.
Make sure your folder looks like this:

```
taskflow/
├── backend/
├── frontend/
├── docker-compose.yml
└── README.md
```

### Step 3 – Run everything

Open a terminal in the `taskflow/` folder and run:

```
docker compose up --build
```

This will:
- Pull MySQL 8 image
- Build and start the Spring Boot backend
- Build and start the React frontend
- Wire them all together automatically

First run takes ~3-5 minutes (downloading images + building).

### Step 4 – Open the app

| Service   | URL                        |
|-----------|----------------------------|
| Frontend  | http://localhost:3000       |
| API       | http://localhost:8080/api   |
| MySQL     | localhost:3306              |

### Step 5 – Stop the app

```
docker compose down
```

To also wipe the database:
```
docker compose down -v
```

---

## OPTION B – Manual Setup

### Prerequisites to Install

#### 1. Java 17 (JDK)
Download from: https://adoptium.net/ (choose "Temurin 17 LTS")

After install, verify:
```
java -version
# Should show: openjdk version "17.x.x"
```

#### 2. Maven 3.9+
Download from: https://maven.apache.org/download.cgi
Extract it and add to PATH.

Verify:
```
mvn -version
# Should show: Apache Maven 3.9.x
```

macOS shortcut (if you have Homebrew):
```
brew install maven
```

Windows shortcut (if you have Chocolatey):
```
choco install maven
```

#### 3. Node.js 20 LTS
Download from: https://nodejs.org/ (choose "20 LTS")

Verify:
```
node --version    # v20.x.x
npm --version     # 10.x.x
```

#### 4. MySQL 8.0
Download from: https://dev.mysql.com/downloads/mysql/
OR use Docker just for MySQL:
```
docker run -d \
  --name taskflow-db \
  -e MYSQL_ROOT_PASSWORD=rootsecret \
  -e MYSQL_DATABASE=taskflow \
  -e MYSQL_USER=taskflow \
  -e MYSQL_PASSWORD=secret \
  -p 3306:3306 \
  mysql:8.0
```

---

### Step-by-Step Backend Setup

#### Step 1 – Set up the database

If you installed MySQL natively, open MySQL shell:
```
mysql -u root -p
```

Then run these SQL commands:
```sql
CREATE DATABASE IF NOT EXISTS taskflow;
CREATE USER IF NOT EXISTS 'taskflow'@'localhost' IDENTIFIED BY 'secret';
GRANT ALL PRIVILEGES ON taskflow.* TO 'taskflow'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

#### Step 2 – Navigate to backend folder
```
cd taskflow/backend
```

#### Step 3 – Download all Java dependencies
```
mvn dependency:resolve
```

This downloads ~50 libraries (Spring Boot, Security, JPA, JWT, etc.)
Takes 1–2 minutes on first run. Cached for future runs.

#### Step 4 – Build the project
```
mvn clean package -DskipTests
```

You should see: `BUILD SUCCESS`
This creates `target/taskflow-backend-1.0.0.jar`

#### Step 5 – Run the backend

Option A (Maven plugin, for development):
```
mvn spring-boot:run
```

Option B (compiled JAR):
```
java -jar target/taskflow-backend-1.0.0.jar
```

You should see Spring Boot banner and:
```
Started TaskFlowApplication in 3.x seconds
TaskAssignmentEngine started – pool [4/16], queue cap 500
```

Backend is now running at: http://localhost:8080/api

---

### Step-by-Step Frontend Setup

Open a NEW terminal window (keep backend running).

#### Step 1 – Navigate to frontend folder
```
cd taskflow/frontend
```

#### Step 2 – Install Node.js dependencies
```
npm install
```

This installs React, Vite, and other packages into `node_modules/`.
Takes ~30 seconds.

#### Step 3 – Start the dev server
```
npm run dev
```

You should see:
```
  VITE v5.x.x  ready in 300 ms
  ➜  Local:   http://localhost:3000/
```

Open http://localhost:3000 in your browser.

---

### Verify Everything is Working

1. Go to http://localhost:3000
2. Click "Create account" and register
3. You'll be logged in automatically
4. Click "+ New Task" to create a task
5. Watch it auto-assign on the Kanban board within a few seconds
6. Check the Dashboard to see live engine metrics

---

## Environment Variables (Optional Customization)

Create a `.env` file in `taskflow/` (for Docker) or set system env vars (for manual):

```
DB_HOST=localhost
DB_PORT=3306
DB_NAME=taskflow
DB_USER=taskflow
DB_PASS=secret
JWT_SECRET=YourSuperSecretKeyThatIsAtLeast32CharsLong!
```

To change the assignment strategy, edit `application.yml`:
```yaml
taskflow:
  assignment:
    strategy: LEAST_LOADED   # or ROUND_ROBIN or PRIORITY_BASED
```

---

## Troubleshooting

**"Port 3306 already in use"**
Another MySQL instance is running. Either stop it or change the port in application.yml.

**"BUILD FAILURE" in Maven**
Make sure you're using Java 17+:
```
java -version
```
If not, set JAVA_HOME to point to JDK 17.

**"Cannot connect to database"**
Check MySQL is running:
```
mysql -u taskflow -psecret -h localhost taskflow
```

**"Module not found" in frontend**
Delete node_modules and reinstall:
```
rm -rf node_modules
npm install
```

**Backend starts but tasks don't assign**
The assignment engine sweeps every 30 seconds. Wait a moment, or check logs for:
```
Task X assigned to resource Y (load: Z/N)
```
