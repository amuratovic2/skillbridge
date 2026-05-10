# SkillBridge

Platforma za digitalne usluge koja povezuje klijente i freelancere. Projekat iz predmeta "Praktikum - Napredne web tehnologije", Elektrotehnicki fakultet Sarajevo.

## Tech Stack

- **Frontend:** React + TypeScript + Vite + Tailwind CSS
- **Backend:** Spring Boot 3.4 + Java 21 (mikroservisna arhitektura)
- **API Gateway:** Spring Cloud Gateway
- **Baza:** PostgreSQL 16 (Docker)
- **ORM:** Spring Data JPA (Hibernate)

## Arhitektura

```
apps/
  web/                            -> React frontend (port 4200)
services/
  discovery-service/              -> Eureka service discovery (port 8761)
  api-gateway/                    -> Spring Cloud Gateway (port 3000)
  user-service/                   -> Auth, korisnici, profili, portfolio (port 3001)
  gig-service/                    -> Usluge, kategorije, tagovi (port 3002)
  order-service/                  -> Narudzbe, isporuke, ponude (port 3003)
  communication-service/          -> Poruke, recenzije, sporovi (port 3004)
docker/
  init-schemas.sql                -> Inicijalne database sheme
```

## Pokretanje projekta

### 1. Preduvjeti

- **Java 21** - [download](https://adoptium.net/) ili `brew install openjdk@21` (Mac)
- **Maven** - [download](https://maven.apache.org/download.cgi) ili `brew install maven` (Mac)
- **Node.js 20+** - [download](https://nodejs.org/)
- **pnpm** - `npm install -g pnpm`
- **Docker** - [download](https://www.docker.com/products/docker-desktop/)

### 2. Pokreni bazu podataka

```
docker compose up -d
```

PostgreSQL je na portu **5433** i ne dira lokalni Postgres na 5432.

### 3. Instaliraj frontend dependencies

```
pnpm install
```

### 4. Pokreni backend servise

Svaki servis u zasebnom terminalu:

```
cd services/discovery-service
mvn spring-boot:run
```

```
cd services/user-service
mvn spring-boot:run
```

```
cd services/gig-service
mvn spring-boot:run
```

```
cd services/order-service
mvn spring-boot:run
```

```
cd services/communication-service
mvn spring-boot:run
```

```
cd services/api-gateway
mvn spring-boot:run
```

Ili koristeci npm skripte iz root direktorija:

```
npm run dev:discovery
npm run dev:user
npm run dev:gig
npm run dev:order
npm run dev:comm
npm run dev:gateway
```

### 5. Pokreni frontend

```
npm run dev:web
```

### Ili pokreni sve odjednom

```
npm run dev
```

Ovo pokrece svih 5 backend servisa i frontend paralelno, sa oznacenim logovima po boji.

### 6. Otvori aplikaciju

Frontend: [http://localhost:4200](http://localhost:4200)

## Demo nalozi

Podaci se automatski seeduju pri prvom pokretanju svakog servisa.

Svi nalozi koriste lozinku: `password123`

| Uloga      | Email                |
|------------|----------------------|
| Admin      | admin@skillbridge.ba |
| Freelancer | marija@example.com   |
| Freelancer | stefan@example.com   |
| Freelancer | ana@example.com      |
| Freelancer | emir@example.com     |
| Freelancer | lejla@example.com    |
| Klijent    | ahmed@example.com    |
| Klijent    | nina@example.com     |

## Korisne komande

```
# Zaustavi bazu
docker compose down

# Resetuj bazu (brise sve podatke - seeduju se ponovo pri pokretanju)
docker compose down -v && docker compose up -d
```
