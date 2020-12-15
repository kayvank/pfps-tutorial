##
## login to remote postgress:
## psql -h 172.23.0.2 -U user-name -d database-name
## \i /home/kayvan/dev/workspaces/workspace-proto/pfps-tutorial/db/sql/orders.sql
##

CREATE TABLE orders (
uuid UUID PRIMARY KEY,
user_id UUID NOT NULL,
payment_id UUID UNIQUE NOT NULL,
items JSONB NOT NULL,
total NUMERIC,
CONSTRAINT user_id_fkey FOREIGN KEY (user_id)
REFERENCES users (uuid) MATCH SIMPLE
ON UPDATE NO ACTION ON DELETE NO ACTION
);
