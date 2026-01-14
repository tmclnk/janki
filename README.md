# Janki - Web-Based Spaced Repetition Flashcard System

A modern, web-based clone of the popular Anki flashcard application, built with Spring Boot WebFlux and implementing the SuperMemo 2 (SM-2) spaced repetition algorithm.

## Project Overview

Janki helps you learn and memorize information effectively using spaced repetition - a learning technique that shows flashcards at increasing intervals based on how well you know them. The better you know a card, the less frequently you'll see it.

## Tech Stack

- **Backend**: Spring Boot 3.5.9 + WebFlux (reactive)
- **Language**: Java 25 (GraalVM CE)
- **Database**: SQLite (via JDBC)
- **Template Engine**: Thymeleaf
- **Build Tool**: Maven
- **Dependencies**: Spring Data JDBC, Lombok, Validation

## Features

### Current (MVP)
- **Basic Flashcards**: Front/back text cards
- **Deck Management**: Create, organize, and manage decks
- **SM-2 Algorithm**: Intelligent spaced repetition scheduling
- **Study Sessions**: Review cards with immediate feedback
- **Rating System**: Four-button interface (Again, Hard, Good, Easy)
- **Single User**: No authentication required


## Quick Start

### Prerequisites
- Java 25 (GraalVM recommended)
- Maven 3.6+

### Build & Run

```bash
# Clone the repository
git clone <repository-url>
cd janki

# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

The application will start on **http://localhost:8080**

### Database

Janki uses SQLite with a file-based database (`janki.db`) created automatically in the project root directory. No external database setup required!

## How to Use

### 1. Create a Deck
- Navigate to the home page
- Click "New Deck"
- Enter a name and description (e.g., "Spanish Vocabulary")

### 2. Add Cards
- Open a deck
- Click "Add Card"
- Enter front (question) and back (answer)
- Save the card

### 3. Study!
- Click "Study" on a deck
- Review the front of the card
- Click "Show Answer" to reveal the back
- Rate your recall:
  - **Again**: Complete failure → review immediately
  - **Hard**: Difficult recall → review sooner
  - **Good**: Normal recall → standard interval
  - **Easy**: Trivial recall → longer interval

## SM-2 Spaced Repetition Algorithm

Janki implements the SuperMemo 2 algorithm:

### Card States
- **NEW**: Never studied before
- **LEARNING**: Being learned (intervals < 6 days)
- **REVIEW**: Graduated cards (intervals ≥ 6 days)

### Scheduling Logic
1. **First review**: Pass → review in 1 day
2. **Second review**: Pass → review in 6 days (graduate to REVIEW)
3. **Subsequent reviews**: Interval = previous interval × ease factor
4. **Failed review** (rating < 3): Reset to LEARNING state

### Ease Factor
- Starts at 2.5 for all cards
- Adjusts based on your performance
- Minimum value: 1.3
- Formula: `EF' = EF + (0.1 - (5-q) × (0.08 + (5-q) × 0.02))`

## Architecture

### Hybrid Reactive/Blocking Design

```
┌─────────────────────────────────────┐
│   Web Layer (Reactive - WebFlux)   │
│   Controllers return Mono/Flux      │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   Service Layer (Reactive Wrappers) │
│   Mono.fromCallable() + boundedElastic │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  Repository Layer (Blocking - JDBC) │
│  Spring Data JDBC + SQLite          │
└─────────────────────────────────────┘
```

### Project Structure

```
src/main/java/com/prairiegrade/janki/
├── JankiApplication.java          # Main application entry point
├── domain/                        # Entity classes
│   ├── Deck.java
│   ├── Card.java
│   └── ReviewRecord.java
├── repository/                    # Data access layer
│   ├── DeckRepository.java
│   ├── CardRepository.java
│   └── ReviewRecordRepository.java
├── service/                       # Business logic
│   ├── SM2Service.java           # Spaced repetition algorithm
│   ├── DeckService.java
│   ├── CardService.java
│   └── StudyService.java
├── controller/
│   ├── api/                      # REST API endpoints
│   └── web/                      # Thymeleaf controllers
├── dto/                          # Data transfer objects
└── exception/                    # Custom exceptions

src/main/resources/
├── application.properties         # Configuration
├── schema.sql                    # Database schema
└── templates/                    # Thymeleaf templates
```

### Database Schema

**Decks** → **Cards** → **ReviewRecords**

- One deck contains many cards
- Each card has one review record (one-to-one)
- Cascade delete: removing a deck removes all cards and review records

## API Endpoints

### Decks
- `GET /api/decks` - List all decks
- `POST /api/decks` - Create a new deck
- `GET /api/decks/{id}` - Get deck details
- `PUT /api/decks/{id}` - Update a deck
- `DELETE /api/decks/{id}` - Delete a deck
- `GET /api/decks/{id}/stats` - Get deck statistics

### Cards
- `GET /api/decks/{deckId}/cards` - List cards in a deck
- `POST /api/decks/{deckId}/cards` - Add a card to a deck
- `PUT /api/cards/{id}` - Update a card
- `DELETE /api/cards/{id}` - Delete a card

### Study
- `GET /api/study/decks/{deckId}/due` - Get cards due for review
- `POST /api/study/cards/{cardId}/rate` - Rate a card after review

## Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=SM2ServiceTest

# Run with coverage
./mvnw test jacoco:report
```

## Development

### Hot Reload
The project includes Spring Boot DevTools for automatic restart during development.

### Database Inspection
```bash
# Open SQLite database
sqlite3 janki.db

# View schema
.schema

# Query tables
SELECT * FROM decks;
SELECT * FROM cards WHERE deck_id = 1;
SELECT * FROM review_records WHERE next_review_date <= datetime('now');
```

### Adding New Features
1. Update domain models if needed
2. Add repository methods
3. Implement service layer logic
4. Create controllers (REST and/or web)
5. Add templates for web UI
6. Write tests

## Resources

- [SuperMemo SM-2 Algorithm](https://super-memory.com/english/ol/sm2.htm)
- [Anki Manual](https://docs.ankiweb.net/)
- [Spring Boot WebFlux Documentation](https://docs.spring.io/spring-boot/reference/web/reactive.html)
- [Spring Data JDBC](https://spring.io/projects/spring-data-jdbc)

## License

This project is for educational purposes.

## Acknowledgments

- Inspired by [Anki](https://apps.ankiweb.net/) by Damien Elmes
- SM-2 algorithm by Piotr Woźniak (SuperMemo)

---

**Status**: Active Development | Last Updated: 2026-01-14
