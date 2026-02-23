# API Mobile - Pixstop

Documentação das rotas da API para integração com aplicativo mobile.

## Índice

1. [Autenticação](#autenticação)
2. [Login](#1-login)
3. [Registro de Empresa](#2-registro-de-empresa)
4. [Registro de Usuário](#3-registro-de-usuário)
5. [Esqueci a Senha](#4-esqueci-a-senha)
6. [Perfil do Usuário (Me)](#5-perfil-do-usuário-me)
7. [Logout](#6-logout)

---

## Autenticação

Rotas protegidas requerem autenticação via token Bearer (Sanctum).

```
Authorization: Bearer {token}
```

---

## Rotas Disponíveis

### 1. Login

**Endpoint:** `POST /api/auth/login`

**Nome da Rota:** `api.auth.login`

**Descrição:** Autentica o usuário e retorna um token Bearer para uso nas demais requisições.

#### Parâmetros de Entrada

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| user | string | ✅ | Email do usuário |
| password | string | ✅ | Senha do usuário |

#### Exemplo de Requisição

```json
POST /api/auth/login
Content-Type: application/json

{
    "user": "usuario@empresa.com",
    "password": "senha123"
}
```

#### Resposta de Sucesso (200)

```json
{
    "success": true,
    "data": {
        "token": "1|abc123def456...",
        "type": "Bearer",
        "user": {
            "id": 1,
            "name": "João Silva",
            "email": "usuario@empresa.com"
        },
        "tenant": {
            "id": "550e8400-e29b-41d4-a716-446655440000",
            "name": "Minha Empresa"
        }
    }
}
```

#### Resposta de Erro - Credenciais Inválidas (401)

```json
{
    "success": false,
    "error": {
        "message": "Credenciais inválidas"
    }
}
```

#### Resposta de Erro - Validação (422)

```json
{
    "message": "The user field is required.",
    "errors": {
        "user": ["The user field is required."]
    }
}
```

---

### 2. Registro de Empresa

**Endpoint:** `POST /api/auth/register/company`

**Nome da Rota:** `api.auth.register.company`

**Descrição:** Registra uma nova empresa (tenant) junto com o usuário administrador. Cria o banco de dados do tenant, o usuário admin e retorna um token Bearer.

#### Parâmetros de Entrada

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| company_name | string | ✅ | Nome da empresa |
| company_document_type | string | ✅ | Tipo do documento: `cnpj` ou `cpf` |
| company_document | string | ✅ | Número do documento (apenas dígitos) |
| company_email | string | ✅ | Email da empresa |
| company_phone | string | ❌ | Telefone da empresa (max 15 chars) |
| admin_name | string | ✅ | Nome do administrador |
| admin_email | string | ✅ | Email do administrador (deve ser único) |
| password | string | ✅ | Senha do administrador |
| password_confirmation | string | ✅ | Confirmação da senha |

#### Validações do Documento

| Tipo | Tamanho |
|------|---------|
| CPF | 11 dígitos |
| CNPJ | 14 dígitos |

> **Nota:** Caracteres não numéricos são removidos automaticamente do documento antes da validação.

#### Exemplo de Requisição

```json
POST /api/auth/register/company
Content-Type: application/json

{
    "company_name": "Minha Empresa LTDA",
    "company_document_type": "cnpj",
    "company_document": "12345678000190",
    "company_email": "contato@minhaempresa.com",
    "company_phone": "11999999999",
    "admin_name": "João Silva",
    "admin_email": "joao@minhaempresa.com",
    "password": "MinhaS3nh4!",
    "password_confirmation": "MinhaS3nh4!"
}
```

#### Resposta de Sucesso (201)

```json
{
    "success": true,
    "data": {
        "token": "1|abc123def456...",
        "type": "Bearer",
        "user": {
            "id": 1,
            "name": "João Silva",
            "email": "joao@minhaempresa.com"
        },
        "tenant": {
            "id": "550e8400-e29b-41d4-a716-446655440000",
            "name": "Minha Empresa LTDA",
            "company_code": "A1B2C3D4"
        }
    }
}
```

> **Nota:** O `company_code` é gerado automaticamente e pode ser compartilhado para que outros usuários se vinculem à empresa.

#### Resposta de Erro - Validação (422)

```json
{
    "message": "The company document has already been taken.",
    "errors": {
        "company_document": ["Este documento já está cadastrado."]
    }
}
```

#### Resposta de Erro - Servidor (500)

```json
{
    "success": false,
    "error": {
        "message": "Erro ao cadastrar empresa. Tente novamente."
    }
}
```

---

### 3. Registro de Usuário

**Endpoint:** `POST /api/auth/register/user`

**Nome da Rota:** `api.auth.register.user`

**Descrição:** Registra um novo usuário. Opcionalmente, pode vincular o usuário a uma empresa existente usando o `company_code`.

#### Parâmetros de Entrada

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| name | string | ✅ | Nome completo do usuário |
| email | string | ✅ | Email do usuário (deve ser único) |
| password | string | ✅ | Senha do usuário |
| password_confirmation | string | ✅ | Confirmação da senha |
| company_code | string | ❌ | Código da empresa para vincular o usuário |

#### Exemplo de Requisição (sem empresa)

```json
POST /api/auth/register/user
Content-Type: application/json

{
    "name": "Maria Santos",
    "email": "maria@email.com",
    "password": "MinhaS3nh4!",
    "password_confirmation": "MinhaS3nh4!"
}
```

#### Exemplo de Requisição (com empresa)

```json
POST /api/auth/register/user
Content-Type: application/json

{
    "name": "Maria Santos",
    "email": "maria@email.com",
    "password": "MinhaS3nh4!",
    "password_confirmation": "MinhaS3nh4!",
    "company_code": "A1B2C3D4"
}
```

#### Resposta de Sucesso - Sem Empresa (201)

```json
{
    "success": true,
    "data": {
        "token": "1|xyz789...",
        "type": "Bearer",
        "user": {
            "id": 2,
            "name": "Maria Santos",
            "email": "maria@email.com"
        },
        "tenant": null
    }
}
```

#### Resposta de Sucesso - Com Empresa (201)

```json
{
    "success": true,
    "data": {
        "token": "1|xyz789...",
        "type": "Bearer",
        "user": {
            "id": 2,
            "name": "Maria Santos",
            "email": "maria@email.com"
        },
        "tenant": {
            "id": "550e8400-e29b-41d4-a716-446655440000",
            "name": "Minha Empresa LTDA",
            "company_code": "A1B2C3D4"
        }
    }
}
```

#### Resposta de Erro - Código de Empresa Inválido (422)

```json
{
    "success": false,
    "error": {
        "message": "Código de empresa inválido ou empresa inativa.",
        "company_code": "Código de empresa inválido ou empresa inativa."
    }
}
```

#### Resposta de Erro - Validação (422)

```json
{
    "message": "The email has already been taken.",
    "errors": {
        "email": ["Este e-mail já está cadastrado."]
    }
}
```

---

### 4. Esqueci a Senha

**Endpoint:** `POST /api/auth/forgot-password`

**Nome da Rota:** `api.auth.forgot-password`

**Descrição:** Envia um email com link para redefinir a senha. O link direciona para a versão web onde o usuário pode criar uma nova senha.

#### Parâmetros de Entrada

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| email | string | ✅ | Email do usuário |

#### Exemplo de Requisição

```json
POST /api/auth/forgot-password
Content-Type: application/json

{
    "email": "usuario@empresa.com"
}
```

#### Resposta de Sucesso (200)

```json
{
    "success": true,
    "message": "Enviamos seu link de redefinição de senha por e-mail."
}
```

#### Resposta de Erro - Email Não Encontrado (422)

```json
{
    "success": false,
    "message": "Não conseguimos encontrar um usuário com esse endereço de e-mail."
}
```

#### Fluxo de Recuperação de Senha

```
┌─────────────────────────────────────────────────────────────┐
│               FLUXO RECUPERAR SENHA                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Usuário toca em "Esqueci minha senha" no app           │
│     ↓                                                       │
│  2. App exibe tela para digitar email                      │
│     ↓                                                       │
│  3. App envia POST /api/auth/forgot-password               │
│     ↓                                                       │
│  4. Backend envia email com link de recuperação            │
│     ↓                                                       │
│  5. Usuário abre email e clica no link                     │
│     ↓                                                       │
│  6. Link abre no navegador (versão web)                    │
│     ↓                                                       │
│  7. Usuário define nova senha na web                       │
│     ↓                                                       │
│  8. Usuário volta ao app e faz login com nova senha        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

> **Nota:** O reset de senha é feito na versão web por segurança. O app mobile apenas solicita o envio do email.

---

### 5. Perfil do Usuário (Me)

**Endpoint:** `GET /api/me`

**Nome da Rota:** `api.me`

**Autenticação:** ✅ Requer token Bearer

**Descrição:** Retorna os dados do usuário autenticado, incluindo a lista de empresas (tenants) vinculadas e a empresa ativa.

#### Exemplo de Requisição

```
GET /api/me
Authorization: Bearer 1|abc123def456...
```

#### Resposta de Sucesso (200)

```json
{
    "success": true,
    "data": {
        "user": {
            "id": 1,
            "name": "João Silva",
            "email": "joao@empresa.com"
        },
        "tenants": [
            {
                "id": "550e8400-e29b-41d4-a716-446655440000",
                "name": "Minha Empresa LTDA",
                "company_code": "A1B2C3D4",
                "role": "admin-company",
                "balance": 150.50,
                "active": true,
                "is_active": true
            },
            {
                "id": "660e8400-e29b-41d4-a716-446655440001",
                "name": "Outra Empresa",
                "company_code": "X9Y8Z7W6",
                "role": "user",
                "balance": 30.00,
                "active": false,
                "is_active": true
            }
        ],
        "active_tenant": {
            "id": "550e8400-e29b-41d4-a716-446655440000",
            "name": "Minha Empresa LTDA",
            "company_code": "A1B2C3D4",
            "balance": 150.50
        }
    }
}
```

#### Campos do Tenant

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | string (UUID) | ID do tenant |
| name | string | Nome da empresa |
| company_code | string | Código único da empresa |
| role | string | Papel do usuário: `admin-company` ou `user` |
| balance | float | Saldo do usuário na empresa |
| active | boolean | Se é a empresa ativa no momento |
| is_active | boolean | Se o vínculo do usuário com a empresa está ativo |

#### Campos do Active Tenant

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | string (UUID) | ID do tenant ativo |
| name | string | Nome da empresa ativa |
| company_code | string | Código da empresa ativa |
| balance | float | Saldo do usuário na empresa ativa |

> **Nota:** `active_tenant` será `null` se o usuário não tiver nenhuma empresa ativa.

#### Resposta de Erro - Não Autenticado (401)

```json
{
    "message": "Unauthenticated."
}
```

---

### 6. Logout

**Endpoint:** `POST /api/auth/logout`

**Nome da Rota:** `api.auth.logout`

**Autenticação:** ✅ Requer token Bearer

**Descrição:** Revoga o token de acesso atual do usuário.

#### Exemplo de Requisição

```
POST /api/auth/logout
Authorization: Bearer 1|abc123def456...
```

#### Resposta de Sucesso (200)

```json
{
    "success": true,
    "data": {
        "message": "Logout realizado com sucesso"
    }
}
```

#### Resposta de Erro - Não Autenticado (401)

```json
{
    "message": "Unauthenticated."
}
```

---

## Resumo das Rotas

| Método | Endpoint | Auth | Descrição |
|--------|----------|------|-----------|
| POST | `/api/auth/login` | ❌ | Login do usuário |
| POST | `/api/auth/register/company` | ❌ | Registrar empresa + admin |
| POST | `/api/auth/register/user` | ❌ | Registrar usuário |
| POST | `/api/auth/forgot-password` | ❌ | Solicitar reset de senha |
| GET | `/api/me` | ✅ | Dados do usuário autenticado |
| POST | `/api/auth/logout` | ✅ | Revogar token |

## Formato Padrão de Resposta

### Sucesso

```json
{
    "success": true,
    "data": { ... }
}
```

### Erro

```json
{
    "success": false,
    "error": {
        "message": "Descrição do erro"
    }
}
```

### Erro de Validação (422)

```json
{
    "message": "The field is required.",
    "errors": {
        "campo": ["Mensagem de erro"]
    }
}
```

