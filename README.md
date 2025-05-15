# Running the Application with Docker Compose

This guide explains how to run the **job-app** (a Spring Boot application) using **Docker Compose**.
The app requires **Redis** (included), and connects to **PostgreSQL**, **SMTP**, **Cloudinary**, and **Google OAuth**.

---

## Prerequisites

### ✅ Install Docker

* **[Download Docker Desktop](https://www.docker.com/products/docker-desktop)**
* Verify installation:

```bash
docker --version
docker-compose --version
```

---

## 📦 Project Setup

### 1. Clone the Repository

```bash
git clone https://github.com/jackieluong/Job-Backend.git
cd Job-Backend
```

### 2. Verify Required Files

Make sure these files exist in the root of the repository:

```
.
├── docker-compose.yml
├── Dockerfile
├── .env              ← (You will create this)
└── src/main/resources/application.yml
```

---

## 🔐 Configure Environment

### 3. Create and Configure `.env`

* Copy the template:

```bash
cp .env.example .env
```

* Fill in your own values:

```
POSTGRESQL → DB_URL, DB_USER, DB_PASSWORD  
MONGODB → MONGO_URI  
REDIS → REDIS_HOST, REDIS_PORT, REDIS_PASSWORD  
SMTP (Gmail) → SMTP_USER, SMTP_PASSWORD  
Cloudinary → CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET  
Google OAuth → GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET  
```

> ⚠️ Make sure your PostgreSQL are accessible if hosted externally (cloud,..).

---

##  Running the App with Docker Compose

### 4. Build and Start Containers

```bash
docker-compose up -d --build
```

This will:

* Build the Docker image
* Start:

  * Backend Spring Boot app
  * Redis container using `redis:7.0-alpine` 

---

## ✅ Testing the App

### 🔍 Swagger UI:

* [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)

### 🩵 View Logs:

```bash
docker-compose logs app
```

---

## 🚩 Stopping the App

```bash
docker-compose down
```

---

## 🚯 Troubleshooting

### ❌ Build Errors

**Error**:

```
ERROR: failed to solve: ...
```

**Fix**:
Check Dockerfile and project files. Rebuild manually:

```bash
docker-compose build
```

---

### ❌ Redis Connection Errors

**Error**:

```
RedisConnectionException: Unable to connect to redis:6379
```

**Fix**:

* Check if Redis is running:

```bash
docker-compose ps
docker-compose logs redis
```

* Ensure `REDIS_HOST` in `.env` is correct.

---

### ❌ PostgreSQL Connection Errors

**Error**:

```
SQLException: Connection refused
```

**Fix**:

* Check your `.env` for valid `DB_URL`, `DB_USER`, and `DB_PASSWORD`.
* Make sure PostgreSQL is running and accessible.

---

## 📌 Notes

* The `Dockerfile` builds the app from source. If you change code, rebuild:

```bash
docker-compose up -d --build
```

* Make sure your external services are online and credentials in `.env` are correct.

---


