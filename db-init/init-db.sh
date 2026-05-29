#!/bin/bash
set -e

# Start SQL Server in the background
/opt/mssql/bin/sqlservr &

# Wait for SQL Server to start
echo "Waiting for SQL Server to start..."
for i in {1..60}; do
  if /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -Q "SELECT 1" &> /dev/null; then
    echo "SQL Server is ready."
    break
  fi
  sleep 2
done

# Create the database if it doesn't exist
echo "Creating database ledger_mock..."
/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -Q "
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'ledger_mock')
  CREATE DATABASE ledger_mock;
"
echo "Database initialization complete."

# Keep the container running
wait
