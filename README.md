# Java Relational Database Engine (Zero-Dependency)

##  Overview
This project is a lightweight, fully functional Relational Database Management System (RDBMS) built entirely from scratch in pure Java. It operates strictly without external database libraries (like JDBC or SQLite), serving as a deep-dive exploration into database internals, file I/O operations, custom AST parsing, and state management.

Built with a strong emphasis on **system robustness and defensive programming**, this engine doesn't just execute "happy path" queries; it proactively defends against malformed inputs, namespace collisions, and corrupted disk states.

##  Core Architecture & Engineering Features
* **Recursive Descent Parser (AST):** upgraded from a liner tokenizer to a recursive Abstract Syntax Tree parser. It dynamically evaluates complex, nested boolean logic in `WHERE` clauses (e.g., handling nested parentheses and multiple `AND`/`OR` operators).
* **Defensive Pre-processing Shield (Tokenizer):** Engineered a custom tokenizer that survives hostile user inputs. It automaticaaly resolves "glued tokens" (e.g., `WHERE (age>20) AND (pass=='YES')`), safely protects string literals containing punctuation, and strictly enforces mandatory trailling semicolons(`;`) at the server gate.
* **Safe Schema Evolution**: Implemented strict memory-safe mutations for `ALTER TABLE ` commands. The engine proactively pads legacy data rows with `NULL` values during column additions, effectively preventing `IndexOutOfBounds` data corruption when loading from disk.
* **Relational Algebra (Inner Joins):** Implements a customised Nested Loop Join algorithm that automatically discards redundant matching keys and generates fully qualified column headers (e.g., `tableName.columnName`) to prevent namespace pollution.
* **File-Based Persistence:** Maps databases to physical directories and serialises tables into TSV (Tab-Separated Values) `.tab` files, mimicking real-world flat-file database structures.

##  Supported SQL Commands

### 1. **Database & Table Management (DDL)**

  ```sql
  CREATE DATABASE university;
  USE university;
  CREATE TABLE students (name, age, email);
  ALTER TABLE students ADD graduation_year;
  ALTER TABLE students DROP email;
  DROP TABLE students;
  ```



### 2. Data Manipulation & Querying (DML & DQL)

```sql
-- Insert data
INSERT INTO students VALUES ('Alice', 22, 'alice@bristol.ac.uk');

-- Complex Conditional Selects (AST Evaluated)
SELECT name, email FROM students WHERE (age>20) AND (pass == 'TRUE');

-- Safe Updates and Deletions
UPDATE students SET age = 23 WHERE name == 'Alice';
DELETE FROM students WHERE pass == 'FALSE';
```

### 3. Relational Operations

- **Inner Join:** Merges two tables based on a specificed condition, outputing a new virtual table with fully qualified column headers.

  ```sql
  JOIN students AND marks ON id AND id;
  ```


## Testing Engineering (TDD)

The project is fortified by a comprehensive JUnit 5 test suite, simulating both unit-level edge cases and complete End-to-End (E2E) transcript scenarios:

- **E2E Scenario Testing:** Automates real-world "playbooks" (Database Creation -> Inserting -> Schema Mutation -> Complex Queries) to ensure system stability across the entire transaction lifecycle.
- **Destructive Testing:** Actively tests system resilience against querying non-existent tables, mutating primary `id` columns, and dropping missing databases;

##  Getting Started

### Prerequisites

- Java 17 or higher
- Maven

### Running the Project

The project uses a Client-Server architecture. Run them in separate terminal instances:

1. **Start the Server:**

   ```bash
   mvnw exec:java@server
   ```
   
2. **Start the Client:**

   ```bash
   mvnw exec:java@client
   ```
   
3. **Send Commands:** Type your SQL commands into the Client terminal. Ensure every command ends with a semicolon (`;`).

##  Testing

The project is fortified with JUnit 5. The test suite aggressively targets edge cases, such as deleting non-existent data, parsing malformed queries, and preventing ID column mutations.

```bash
mvn test
```

##  Future Roadmap (TODO)

[ ] **B-Tree Indexing:** Implement tree-based data structures to optimize `SELECT` query performance from *O(N)* to *O(\log N)*.

[ ] **ACID Transactions & Concurrency:** Implement read/Write Locks and Write-Ahead Logging (WAL) to support `COMMIT` and `ROLLBACK` features for current clients.

