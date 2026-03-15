# Java Relational Database Engine (Zero-Dependency)

##  Overview
This project is a lightweight, file-based Relational Database Management System (RDBMS) built entirely from scratch in pure Java. It features a custom SQL-like query parser, a memory-safe table management system, an independent conditional filtering engine, and persistent file storage.

Built strictly without any external database libraries (like JDBC, SQLite, or ORMs), this engine demonstrates a deep understanding of core computer science concepts: disk I/O manipulation, string tokenization, dynamic data structures, memory management, and relational algebra (Nested Loop Joins).
##  Core Architecture & Features
* **Custom Command Parser:** A robust tokenizer that parses user input into processable SQL tokens, handling edge cases like missing spaces and namespace collisions.
* **Persistent File Storage:** Databases are mapped to physical directories, and tables are serialized as `.tab` files on the disk, ensuring data persistence across sessions.
* **Conditional Evaluation Engine:** A dynamic `WHERE` clause parser capable of extracting operators (`==`, `>`, `<`, `>=`, `<=`, `!=`, `LIKE`), sanitizing data (removing quotes/spaces), and safely casting strings to numeric types for mathematical comparisons.
* **Relational Joins:** Implements the classic Nested Loop Join algorithm to merge tables based on matching column values, fully supporting qualified names (e.g., `tableName.columnName`) to prevent column ambiguity.
* **Safe State Management:** Defends against invalid commands, missing files, concurrent modification exceptions during deletions, and out-of-bounds errors, returning clear `[ERROR]` or `[OK]` protocols.
* **Automated Testing:** Comprehensive JUnit 5 test suites covering successful execution paths, edge cases, and strict error-handling scenarios.

##  Supported SQL Commands

The database engine supports a complete suite of CRUD operations and complex relational queries:

### 1. Database Management
* **Create/Use/Drop a database:** Creates a new directory.
  ```sql
  CREATE DATABASE school;
  USE school;
  DROP DATABASE school;
  
### 2. Table Management

- **Create a table (auto-generates a primary `id` column):** 

  ```sql 
  CREATE TABLE students (name, age, email);
  
- **Alter table structure (Add/Drop columns dynamically):**
  ```sql
  ALTER TABLE students ADD height;
  ALTER TABLE students DROP email;

- **Drop a table:** 

  ```sql
  DROP TABLE students;
  ```

### 3. Data Manipulation (CRUD)

- **Insert data:** 

  ```sql
  INSERT INTO students VALUES ('Alice', 20, 'alice@bristol.ac.uk');
  ```
  
- **Select data (with Conditional Filtering):** 

  ```sql
  SELECT * FROM students;
  SELECT name, email FROM students WHERE age > 18;
  ```

- **Update existing records:**

  ```sql
  UPDATE students SET age = 21 WHERE name == 'Alice';
  ```

- **Delete records:**

  ```sql
  DELETE FROM students WHERE pass == 'No';
  ```

### 4. Relational Operations

- **Inner Join:** Merges two tables based on a specificed condition, outputing a new virtual table with fully qualified column headers.

  ```sql
  JOIN students AND marks ON id AND id;
  ```

  

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

[ ] **Network Sockets:** Upgrade the Client-Server communication to use TCP/IP sockets instead of local streams.

[ ] **Transaction Management:** Implement ACID properties with `BEGIN`, `COMMIT`, and `ROLLBACK` capabilities.

[ ] **Abstract Syntax Tree (AST):** Upgrade the linear tokenizer to an AST parser for handling complex nested queries (e.g., `AND`/`OR` logic).