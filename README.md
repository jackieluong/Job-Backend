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
### 5. Add extension to PostgreSQL database
##### After starting the containers, you'll need to execute SQL scripts to enable full-text and fuzzy search capabilities.
The scripts will:
- Enable unaccent and pg_trgm extensions
- Create search indexes for jobs table
- Set up triggers for search vector updates and follower counting

#### You can also find the SQL scripts in the `postgres.sql` file.
<details>
<summary>View SQL script (click to expand)</summary>

```bash
-- enable unaccent extension, crucial for full-text search in vietnamese words
CREATE EXTENSION IF NOT EXISTS unaccent;

-- enable pg_trgm extension, crucial for our fuzzy search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Create indexes:

-- index for full-text search on column: searchVector, crucial for our full-text search performance
-- basically we use GIN index for arrays column, not B-Tree index. You can read about Postgres Gin Index in https://www.postgresql.org/docs/current/gin.html
CREATE INDEX job_search_index ON public.jobs USING gin (search_vector)

-- index for fuzzy search on column: name, which is job title, crucial for our fuzzy search performance
CREATE INDEX idx_jobs_name_trgm ON public.jobs USING gin (name gin_trgm_ops)

-- Create functions, triggers

-- Trigger function:
CREATE OR REPLACE FUNCTION update_search_vector()
RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('pg_catalog.simple', unaccent(coalesce(NEW.name, ''))), 'A') ||
        setweight(to_tsvector('pg_catalog.simple', unaccent(coalesce(NEW.description, ''))), 'B');
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update search vector on insert or update on jobs table
CREATE TRIGGER search_vector_update
    BEFORE INSERT OR UPDATE ON jobs
    FOR EACH ROW EXECUTE FUNCTION update_search_vector();


-- Trigger function
CREATE OR REPLACE FUNCTION add_follow_insert()
RETURNS TRIGGER AS $$
BEGIN
UPDATE companies
SET num_of_followers = num_of_followers + 1
WHERE id = NEW.company_id;  -- assuming `company_id` is the foreign key
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update column num_of_followers
-- on companies table on insert to company_follow table
CREATE TRIGGER after_company_follow_insert
    AFTER INSERT ON company_follow
    FOR EACH ROW
    EXECUTE FUNCTION add_follow_insert();


```
</details>



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


