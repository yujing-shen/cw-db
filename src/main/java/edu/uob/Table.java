package edu.uob;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.io.File.separator;

/**
 * Represents a database table containing a name, column definitions, and row data.
 *
 * <p>Each table maintains an auto-incrementing primary key ID ({@code nextAvailableId}),
 * which is assigned to new rows via {@link #getNextId()}.</p>
 *
 * <p>Table data can be persisted to a tab-separated .tab file via {@link #saveToFile(String)}.</p>
 */
public class Table {
    /** The name of this table */
    private String tableName;

    /** Ordered list of column names; each row's values correspond by index */
    private List<String> columnNames;

    /** All rows contained in this table */
    private List<Row> rows;

    /** The next auto-increment ID to assign; starts at 1 */
    private int nextAvailableId;

    /**
     * Constructs an empty table with the given name.
     *
     * @param tableName the name of the table, must not be null
     */
    public Table(String tableName) {
        this.tableName = tableName;
        this.columnNames = new ArrayList<>();
        this.rows = new ArrayList<>();
        this.nextAvailableId = 1;
    }

    /**
     * Returns the name of this table.
     *
     * @return the table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Returns the list of column names.
     *
     * @return the column name list
     */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /**
     * Replaces the current column definitions with the given list.
     *
     * @param columnNames the new column name list
     */
    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    /**
     * Returns all rows in this table.
     *
     * @return the row list
     */
    public List<Row> getRows() {
        return rows;
    }

    /**
     * Appends a column name to the end of the column list.
     *
     * @param columnName the column name to add
     */
    public void addColumnName(String columnName) {
        this.columnNames.add(columnName);
    }

    /**
     * Appends a row to the table.
     *
     * @param row the row to add
     */
    public void addRow(Row row) {
        this.rows.add(row);
    }

    /**
     * Returns the next available auto-increment ID and advances the counter.
     *
     * @return the unique ID assigned to the caller
     */
    public int getNextId() {
        int idToGive = nextAvailableId;
        nextAvailableId++;
        return idToGive;
    }

    /**
     * Adjusts the auto-increment counter based on the maximum ID found in the file,
     * ensuring future IDs returned by {@link #getNextId()} do not collide with existing data.
     *
     * @param maxIdInFile the largest ID value found in the persisted file
     */
    public void updateNextAvailableId(int maxIdInFile) {
        if (maxIdInFile >= this.nextAvailableId) {
            nextAvailableId = maxIdInFile + 1;
        }
    }

    /**
     * Returns the zero-based index of the given column name.
     *
     * @param columnName the column name to look up
     * @return the column index
     * @throws IllegalArgumentException if the column does not exist in this table
     */
    public int getColumnIndexOrThrow(String columnName) {
        int index = this.columnNames.indexOf(columnName);
        if (index == -1) {
            throw new IllegalArgumentException("[ERROR] Column " + columnName + " does not exist in table " + this.tableName);
        }
        return index;
    }

    /**
     * Persists the table to a tab-separated .tab file.
     *
     * <p>The file format: the first line contains column names, each subsequent line
     * contains one row's values, all separated by tab characters.</p>
     *
     * @param storageFolderPath the directory in which the .tab file will be created
     * @throws IOException if an I/O error occurs during writing
     */
    public void saveToFile(String storageFolderPath) throws IOException {
        java.io.File file = new java.io.File(storageFolderPath + separator + this.tableName + ".tab");

        BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(file));

        String headerLine = String.join("\t", this.columnNames);
        writer.write(headerLine);
        writer.write("\n");
        for (Row row : this.rows) {
            String dataLine = String.join("\t", row.getValues());
            writer.write(dataLine);
            writer.write("\n");
        }
        writer.flush();
        writer.close();
    }

    /**
     * Adds a new column and backfills all existing rows with "NULL"
     * to prevent {@link IndexOutOfBoundsException} in future operations.
     *
     * @param newColumnName the name of the new column
     */
    public void addColumn(String newColumnName) {
        this.columnNames.add(newColumnName);
        for (Row row : this.rows) {
            row.addValue("NULL");
        }
    }
}
