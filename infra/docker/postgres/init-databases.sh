#!/bin/sh
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE auth_db;
    CREATE DATABASE patient_db;
    CREATE DATABASE clinic_db;
    CREATE DATABASE followup_db;
    CREATE DATABASE notification_db;
EOSQL
