# Coupon Service

REST API do zarządzania kuponami rabatowymi. Zadanie rekrutacyjne Empik.

---

## Szybki start

**Wymagania:** Java 21, Maven 3.9+, Docker

```bash
# 1. Uruchom bazę danych
docker compose up -d

# 2. Zbuduj i uruchom serwis
./mvnw spring-boot:run

# 3. Serwis działa na
http://localhost:8080
```

---

## API Reference

### Tworzenie kuponu

```
POST /api/coupons
Content-Type: application/json
```

```json
{
  "code":      "WIOSNA2024",
  "maxUsages": 100,
  "country":   "PL"
}
```

| Pole        | Typ     | Ograniczenia                                             |
|-------------|---------|----------------------------------------------------------|
| `code`      | string  | 1–50 znaków, litery / cyfry / `-` / `_`, case-insensitive |
| `maxUsages` | integer | 1–1 000 000                                              |
| `country`   | string  | ISO 3166-1 alpha-2 (2 litery)                            |

**Odpowiedzi:**

| Status | Opis                                  |
|--------|---------------------------------------|
| `201`  | Kupon utworzony                       |
| `400`  | Błąd walidacji (szczegóły w `fieldErrors`) |
| `409`  | Kupon o tym kodzie już istnieje       |

```json
// 201 Created
{
  "id":            "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "code":          "WIOSNA2024",
  "createdAt":     "2024-03-01T12:00:00",
  "maxUsages":     100,
  "currentUsages": 0,
  "country":       "PL"
}
```

---

### Użycie kuponu

```
POST /api/coupons/{code}/use
Content-Type: application/json
```

```json
{
  "userId": "user-42"
}
```

Kraj użytkownika jest wyznaczany automatycznie na podstawie adresu IP requestu.

**Odpowiedzi:**

| Status | `errorCode`               | Opis                                    |
|--------|---------------------------|-----------------------------------------|
| `200`  | —                         | Kupon użyty pomyślnie                   |
| `400`  | `VALIDATION_ERROR`        | Brakujące / niepoprawne pola            |
| `403`  | `COUPON_COUNTRY_MISMATCH` | Użytkownik z niedozwolonego kraju       |
| `404`  | `COUPON_NOT_FOUND`        | Kupon o podanym kodzie nie istnieje     |
| `409`  | `COUPON_EXHAUSTED`        | Kupon wyczerpany                        |
| `409`  | `COUPON_ALREADY_USED`     | Ten użytkownik już użył tego kuponu     |
| `503`  | `GEOLOCATION_UNAVAILABLE` | Serwis geolokalizacji niedostępny       |

```json
// 200 OK
{
  "code":            "WIOSNA2024",
  "remainingUsages": 99,
  "message":         "Coupon successfully applied"
}

// błąd (przykład 409)
{
  "status":    409,
  "errorCode": "COUPON_EXHAUSTED",
  "message":   "Coupon 'WIOSNA2024' has reached its maximum usage limit",
  "timestamp": "2024-03-01T12:05:00Z"
}
```

---

## Architektura

Projekt stosuje **hexagonal architecture** (ports & adapters), jednak bez narzutu warstwy aplikacyjnej — logika domenowa żyje bezpośrednio w `CouponService`.

```
com.empik.coupon
├── api/                     ← warstwa wejściowa HTTP
│   ├── controller/          ← CouponController
│   ├── dto/                 ← request / response records
│   ├── exception/           ← GlobalExceptionHandler, ApiError
│   └── util/                ← IpExtractor
│
├── domain/                  ← czysta logika biznesowa (zero zależności od Springa*)
│   ├── model/               ← Coupon, CouponUsage (encje JPA**)
│   ├── port/                ← interfejsy CouponRepositoryPort, GeolocationPort
│   ├── service/             ← CouponService
│   └── exception/           ← wyjątki domenowe (nie HTTP, nie Spring)
│
└── infrastructure/          ← adaptery implementujące porty
    ├── persistence/         ← JpaCouponRepository, JpaCouponUsageRepository
    ├── geolocation/         ← IpApiGeolocationService
    └── config/              ← InfrastructureConfig (RestClient bean)
```

> \* `@Service` i `@Transactional` to adnotacje Spring, ale są traktowane jako pragmatyczny kompromis — usunięcie ich wymagałoby dodatkowej warstwy aplikacyjnej bez realnego zysku w tym projekcie.
>
> \*\* Encje JPA w domenie to świadoma decyzja: osobny model `CouponEntity` byłby copy-paste bez wartości.

---

## Kluczowe decyzje techniczne

### Współbieżność — pessimistic locking zamiast optimistic

Przy naiwnym odczycie + inkrementacji istnieje *race condition*: dwa wątki mogą jednocześnie odczytać `currentUsages = 99` przy `maxUsages = 100` i obydwa przejść przez warunek, przekraczając limit.

**Rozwiązanie:** `findByCodeWithLock` wykonuje `SELECT … FOR UPDATE`. Baza serializuje transakcje konkurujące o ten sam wiersz — druga transakcja czeka, aż pierwsza się zakończy i widzi aktualną wartość licznika.

```sql
-- Hibernate tłumaczy @Lock(PESSIMISTIC_WRITE) na:
SELECT * FROM coupons WHERE code = ? FOR UPDATE
```

**Dlaczego nie optimistic?** Optimistic locking (pole `@Version`) wymaga obsługi `OptimisticLockException` i logiki retry po stronie aplikacji. W scenariuszu "kto pierwszy, ten lepszy" pesymistyczny lock jest semantycznie czystszy — dokładnie jeden wątek wygrywa, reszta czeka i dostaje aktualny stan.

Pole `@Version` pozostaje w encji jako dodatkowa warstwa bezpieczeństwa poza ścieżką `useCoupon`.

### Baza danych jako ostatnia linia obrony

Schemata zawiera trzy constrainty, które zapobiegają niespójności nawet gdyby kod aplikacji zawiedzie:

```sql
CONSTRAINT chk_max_usages           CHECK (max_usages > 0)
CONSTRAINT chk_current_usages       CHECK (current_usages >= 0)
CONSTRAINT chk_usages_not_exceeded  CHECK (current_usages <= max_usages)
UNIQUE (coupon_id, user_id)         -- jeden użytkownik, jeden kupon
```

### Normalizacja kodu kuponu

Kod jest zamieniany na uppercase przy zapisie (`Coupon.create()`), nie przy każdym odczycie. Oznacza to jeden indeks, zero ambiguity i brak potrzeby `UPPER(code)` w zapytaniach. Duplikaty w stylu `wiosna` vs `WIOSNA` są wyłapywane przed zapisem.

### Geolokalizacja — ip-api.com

Serwis używa darmowego [ip-api.com](http://ip-api.com) (do 45 req/min bez klucza API). `GeolocationPort` jest portem domenowym — podmiana na MaxMind GeoLite2, ipinfo.io lub dowolny inny provider wymaga tylko nowej implementacji, bez dotykania logiki biznesowej.

**Obsługa prywatnych IP lokalnie:** flaga `geolocation.bypass-private-ips: true` (domyślnie włączona) sprawia, że lokalne adresy (loopback, RFC-1918) otrzymują kraj z `geolocation.bypass-country` zamiast trafić do zewnętrznego API.

### Wyciąganie IP klienta

`IpExtractor` sprawdza nagłówki `X-Forwarded-For`, `X-Real-IP` i inne zanim sięgnie do `RemoteAddr`. W przypadku łańcucha proxy (`5.6.7.8, 10.0.0.1`) bierze pierwszy (leftmost) adres — oryginalny adres klienta.

> ⚠️ W produkcji należy zaufać tym nagłówkom tylko jeśli reverse proxy (nginx, AWS ALB) jest jedynym podmiotem, który może je ustawiać — inaczej klient może sfabrykować dowolny IP.

---

## Technologie

| Obszar          | Wybór                                     | Uzasadnienie                                         |
|-----------------|-------------------------------------------|------------------------------------------------------|
| Framework       | Spring Boot 3.2 (Java 21)                 | Ekosystem, virtual threads ready, LTS                |
| ORM             | Spring Data JPA / Hibernate               | Natywne wsparcie dla `PESSIMISTIC_WRITE`             |
| Baza danych     | PostgreSQL 16                             | MVCC, row-level locking, produkcyjny standard        |
| Migracje        | Flyway                                    | Wersjonowanie schematu, rollback, determinizm        |
| Walidacja       | Jakarta Bean Validation (`@Valid`)        | Deklaratywna, bez boilerplate                        |
| Geolokalizacja  | ip-api.com (via `RestClient`)             | Darmowy, zero konfiguracji, łatwy do podmiany        |
| Budowanie       | Maven 3.9 (spring-boot-starter-parent)    | Szeroko znany w ekosystemie Java/Spring              |

---

## Testy

```
src/test/
├── domain/model/
│   └── CouponTest                          ← unit: logika domenowa
├── domain/service/
│   └── CouponServiceTest                   ← unit: serwis z zamockowanymi portami
├── api/
│   ├── controller/CouponControllerIntegrationTest  ← MockMvc + mocked service
│   └── util/IpExtractorTest                ← unit: wyciąganie IP
└── infrastructure/persistence/
    └── CouponRepositoryIntegrationTest     ← Testcontainers + prawdziwy PostgreSQL
```

**Uruchomienie testów:**

```bash
./mvnw test
```

Testy integracyjne startują PostgreSQL przez Testcontainers (wymaga Docker). Kontener jest współdzielony między klasami (`static` field) — uruchamiany raz na sesję JVM.

**Strategia testowania:**

- **Unit testy** (Coupon, CouponService, IpExtractor): weryfikują logikę biznesową w izolacji, bez Springa, bez bazy.
- **Controller testy** (MockMvc): weryfikują deserializację requestu, walidację, mapowanie wyjątków na kody HTTP. `CouponService` jest mockiem — testy są szybkie i nie wymagają bazy.
- **Repository testy** (Testcontainers): weryfikują, że schema Flyway jest poprawna, że zapytania JPA działają jak oczekiwano i że constrainty bazy działają.

---

## Konfiguracja

```yaml
# application.yml
spring:
  datasource:
    url:      jdbc:postgresql://localhost:5432/coupondb
    username: coupon
    password: coupon123

geolocation:
  base-url:           http://ip-api.com
  bypass-private-ips: true   # true = lokalne IP → bypass-country (dev)
  bypass-country:     PL     # kraj przypisywany do prywatnych IP
```

Zmienne środowiskowe nadpisują konfigurację zgodnie ze standardem Spring Boot (`SPRING_DATASOURCE_URL`, `GEOLOCATION_BASE_URL` itd.).

---

## Co można rozwinąć w kierunku produkcji

- **Rate limiting** na endpoincie `use` — ochrona przed brute-force kodów
- **Cache** geolokalizacji (np. Caffeine) — IP rzadko zmienia kraj, a 45 req/min to skromny limit
- **Metryki** (Micrometer + Prometheus) — `coupon.used`, `coupon.rejected.reason`, latencja geolokalizacji
- **Circuit breaker** na kliencie ip-api.com (Resilience4j) — zamiast zwracać 503, można fallback do accept/reject
- **Audit log** — kto, kiedy, z jakiego IP użył kuponu (persisted, nie tylko logi)
- **API authentication** — np. API key dla endpointu tworzenia kuponów
- **Provider geolokalizacji** — MaxMind GeoLite2 jako lokalna baza (zero latencji, brak zewnętrznej zależności)
