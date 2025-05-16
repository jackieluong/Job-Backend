
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
