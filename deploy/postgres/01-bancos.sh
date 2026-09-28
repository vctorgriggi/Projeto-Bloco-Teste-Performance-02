#!/bin/sh
# cria os dois bancos de negocio, cada um com o seu usuario, na primeira subida do
# postgres (a imagem oficial roda os scripts de /docker-entrypoint-initdb.d uma vez so,
# quando o volume de dados esta vazio).
#
# um servidor, dois bancos, dois usuarios: o usuario "blog" nao tem acesso ao banco
# "engagementdb" e vice-versa. e o "um banco por servico" da terceira entrega mantido
# no ambiente de conteiner, sem pagar por duas instancias do postgres. o mesmo script
# e usado pelo docker compose e pelo kubernetes (montado de um ConfigMap).
set -eu

cria_banco() {
    banco="$1"; usuario="$2"; senha="$3"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<SQL
CREATE USER $usuario WITH PASSWORD '$senha';
CREATE DATABASE $banco OWNER $usuario;
REVOKE ALL ON DATABASE $banco FROM PUBLIC;
SQL
}

cria_banco blogdb       "${BLOG_DB_USER:-blog}"             "${BLOG_DB_PASSWORD:-blog}"
cria_banco engagementdb "${ENGAGEMENT_DB_USER:-engagement}" "${ENGAGEMENT_DB_PASSWORD:-engagement}"
