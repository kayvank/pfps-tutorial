##
## login to remote postgress:
## psql -h 172.23.0.2 -U user-name -d database-name
## \i /home/kayvan/dev/workspaces/workspace-proto/pfps-tutorial/db/sql/categories.sql
##
CREATE TABLE categories (
uuid UUID PRIMARY KEY,
name VARCHAR UNIQUE NOT NULL
);
