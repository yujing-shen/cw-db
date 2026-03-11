# Java Relational Database Engine

##  Overview
This project is a lightweight, file-based Relational Database Management System (RDBMS) built entirely from scratch in Java. It features a custom SQL-like query parser, an object-oriented table management system, and persistent file storage. 

This engine operates strictly without the use of external database libraries (like JDBC, SQLite, etc.), demonstrating core concepts of file I/O, string tokenization, data structures, and state management.

##  Features Implemented
* **Custom Command Parser:** A tokenizer that parses user input into processable SQL tokens.
* **Persistent Storage:** Databases are stored as directories, and tables are stored as physical `.tab` files on the disk.
* **State Management:** Tracks the currently selected database context during the session.
* **Robust Error Handling:** Defends against invalid commands, missing files, and duplicate entries, returning clear `[ERROR]` or `[OK]` responses.
* **Automated Testing:** Comprehensive JUnit 5 test suites to ensure command reliability and prevent regressions.

##  Supported SQL Commands

Currently, the database engine supports the following core SQL operations:

### 1. Database Management
* **Create a database:** Creates a new directory.
  ```sql
  CREATE DATABASE school;

- **Switch database context:** Selects a database for subsequent operations.

  ```sql
  USE school;
  ```
  
- **Drop a database:** Safely deletes the database directory and all associated table files.

  ```sql
  DROP DATABASE school;
  ```

### 2. Table Management

- **Create a table:** Initializes a `.tab` file with a default `id` column and user-defined columns.

  ```sql 
  CREATE TABLE students (name, age, email);

- **Drop a table:** Deletes the specific table file from the disk.

  ```sql
  DROP TABLE students;
  ```

### 3. Data Manipulation (CRUD)

- **Insert data:** Auto-generates an incrementing `id` and appends a new row to the table.

  ```sql
  INSERT INTO students VALUES ('Alice', 20, 'alice@bristol.ac.uk');
  ```
  
- **Select data (Basic):** Retrieves all columns or specific columns from a table.

  ```sql
  SELECT * FROM students;
  SELECT name, email FROM students;
  ```

##  Getting Started

### Prerequisites

- Java 17 or higher
- Maven

### Running the Project

The project is split into a Server and a Client. You need to run them in separate terminal windows.

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

The project uses JUnit 5 for automated testing. Tests cover both successful execution paths and error-handling scenarios (e.g., trying to drop a non-existent table). To run the tests:

```bash
mvn test
```

##  Future Roadmap (TODO)

- [ ] Implement `WHERE` clause parsing for conditional `SELECT` queries.
- [ ] Add `DELETE FROM` to remove specific rows based on conditions.
- [ ] Add `UPDATE` to modify existing data.
- [ ] Implement `ALTER TABLE` to add or drop columns.