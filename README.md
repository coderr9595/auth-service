# 🔐 Auth Service

Spring Boot Authentication Service using PostgreSQL and JWT, containerized with Docker.

---

## ✨ Features

- ✅ REST API with **Spring Boot 3.2.5** & **Java 21**
- 🐘 PostgreSQL integration with auto-init via Docker
- 🔐 JWT-based token authentication
- 🐳 Full Docker & Docker Compose setup

---

## 🚀 Setup Instructions

### 🖥️ Run Locally (Without Docker)

1. **Configure database and JWT properties** in `src/main/resources/application.properties`:

    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/auth_db
    spring.datasource.username=postgres
    spring.datasource.password=your_password

    jwt.secret=your_jwt_secret_key
    jwt.expirationMs=3600000
    ```

2. **Build and run the app:**

    ```bash
    mvn clean package
    java -jar target/auth-service-0.0.1-SNAPSHOT.jar
    ```

---

### 🐳 Run with Docker

1. Ensure the following files are inside the `script/` folder:
    - `Dockerfile`
    - `docker-compose.yml`
    - `docker-entrypoint-initdb.d/init.sql`

2. Run the application:

    ```bash
    cd script
    docker-compose up --build
    ```

3. Access the service at:

    ```
    http://localhost:8080
    ```

---

## 🧰 Docker Compose Overview

- **Postgres**: Initializes `auth_db` and user credentials using environment variables.
- **Auth Service**: Built from the project root and connects to the PostgreSQL container.

---

## ♻️ Resetting the Database

If you want to reinitialize the database from scratch:

```bash
docker-compose down
docker volume rm auth-service_pgdata
docker-compose up --build
```

## 📬 Contact
For questions or feedback:

Email: piyushlumarpk2130@gmail.com