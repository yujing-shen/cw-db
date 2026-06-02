package edu.uob;

/**
 * Placeholder for a database-level domain object.
 *
 * <p>In the current implementation, a "database" is represented by:
 * <ul>
 *   <li>a folder under {@code databases/<dbName>/} on disk, and</li>
 *   <li>the {@code currentDatabase} field in {@link DatabaseEngine} for the active session.</li>
 * </ul>
 *
 * <p>Table data lives in {@link Table} objects persisted as {@code .tab} files via
 * {@link StorageManager}. This class is reserved for future refactoring if database-level
 * metadata or behaviour is moved out of {@link DatabaseEngine}.
 */
public class Database {
}
