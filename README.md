# SkillBridge

SkillBridge je platforma za digitalne usluge koja povezuje klijente i freelancere. Aplikacija podrzava registraciju i prijavu korisnika, upravljanje profilima i gigovima, narudzbe, prilagodene ponude, isporuke, poruke, recenzije i notifikacije kroz mikroservisnu arhitekturu.

## Tim

- Ajla Hodžić
- Esma Dizdarević
- Ensar Hodžić
- Alem Muratović

## Demo video

[Video](https://youtu.be/G9B8GTyNxO8)

## Tehnologije

- Frontend: React, TypeScript, Vite, Tailwind CSS
- Backend: Java 21, Spring Boot, Spring Cloud Gateway, Eureka
- Baza i broker: PostgreSQL 16, RabbitMQ
- Build i workspace: pnpm, Nx, Maven

## Struktura

```text
apps/web                     React frontend
services/api-gateway         API Gateway i JWT provjere
services/discovery-service   Eureka discovery
services/user-service        Auth, korisnici, profili, portfolio
services/gig-service         Gigovi, kategorije i tagovi
services/order-service       Narudzbe, isporuke i prilagodene ponude
services/communication-service Poruke, recenzije, sporovi i notifikacije
docker                       Inicijalizacija PostgreSQL shema
```

## Pokretanje preko Dockera

Preduvjet je instaliran Docker Desktop ili Docker Engine sa Docker Compose podrskom.

1. Kopirajte repozitorij i udite u root direktorij.

```bash
cd skillbridge
```

2. Pokrenite sve servise.

```bash
docker compose up --build
```

3. Otvorite aplikaciju.

```text
Frontend:   http://localhost:4200
Gateway:    http://localhost:3000
Eureka:     http://localhost:8761
RabbitMQ:   http://localhost:15672
PostgreSQL: localhost:5433
```

RabbitMQ login je `skillbridge` / `skillbridge`. PostgreSQL koristi bazu `skillbridge`, korisnika `skillbridge` i lozinku `skillbridge123`.

Korisne komande:

```bash
docker compose ps
docker compose logs -f api-gateway
docker compose down
docker compose down -v
```

Komanda `docker compose down -v` brise Docker volume sa podacima baze. Nakon sljedeceg pokretanja sheme i seed podaci se kreiraju ponovo.

## Lokalno pokretanje bez Dockera

Za lokalni razvoj su potrebni Java 21, Maven, Node.js 20+ i pnpm. Infrastrukturu mozete pokrenuti preko Dockera, a servise lokalno:

```bash
docker compose up -d skillbridge-db skillbridge-rabbitmq
pnpm install
pnpm dev
```

Pojedinacni frontend build i testovi:

```bash
pnpm build
pnpm test
```

## Demo nalozi

Seed podaci se dodaju pri pokretanju servisa. Svi demo nalozi koriste lozinku `password123`.

| Uloga | Email |
| --- | --- |
| Admin | admin@skillbridge.ba |
| Freelancer | marija@example.com |
| Freelancer | stefan@example.com |
| Freelancer | ana@example.com |
| Freelancer | emir@example.com |
| Freelancer | lejla@example.com |
| Klijent | ahmed@example.com |
| Klijent | nina@example.com |
