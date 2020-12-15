##
## login to remote postgress:
## psql -h 172.23.0.2 -U user-name -d database-name
## \i /home/kayvan/dev/workspaces/workspace-proto/pfps-tutorial/db/sql/items.sql
##
CREATE TABLE items (
uuid UUID PRIMARY KEY,
name VARCHAR UNIQUE NOT NULL,
description VARCHAR NOT NULL,
price NUMERIC NOT NULL,
brand_id UUID NOT NULL,
category_id UUID NOT NULL,
CONSTRAINT brand_id_fkey FOREIGN KEY (brand_id)
REFERENCES brands (uuid) MATCH SIMPLE
ON UPDATE NO ACTION ON DELETE NO ACTION,
CONSTRAINT cat_id_fkey FOREIGN KEY (category_id)
REFERENCES categories (uuid) MATCH SIMPLE
ON UPDATE NO ACTION ON DELETE NO ACTION
);
