package edu.uob;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/** This class implements the DB server. */
public class DBServer {

    private static final char END_OF_TRANSMISSION = 4;
    private String storageFolderPath;
    private String currentDatabase;

    public static void main(String args[]) throws IOException {
        DBServer server = new DBServer();
        server.blockingListenOn(8888);
    }

    /**
    * KEEP this signature otherwise we won't be able to mark your submission correctly.
    */
    public DBServer() {
        storageFolderPath = Paths.get("databases").toAbsolutePath().toString();
        try {
            // Create the database storage folder if it doesn't already exist !
            Files.createDirectories(Paths.get(storageFolderPath));
        } catch(IOException ioe) {
            System.out.println("Can't seem to create database storage folder " + storageFolderPath);
        }
        this.currentDatabase = "";
    }

    /**
    * KEEP this signature (i.e. {@code edu.uob.DBServer.handleCommand(String)}) otherwise we won't be
    * able to mark your submission correctly.
    *
    * <p>This method handles all incoming DB commands and carries out the required actions.
    */
    public String handleCommand(String command) {
        // TODO implement your server logic here
        String trimmedCommand = command.trim();
        if (trimmedCommand.isEmpty()) {
            return "[ERROR] Empty command";
        }
        if (!trimmedCommand.endsWith(";")) {
            return "[ERROR] Invalid syntax: Command must end with a semicolon (;)";
        }
        try {
            Tokenizer tokenizer = new Tokenizer();
            List<String> tokens = tokenizer.parseTokens(trimmedCommand);

            if (tokens.isEmpty()) {
                return "[ERROR] Empty command";
            }


            String firstWord = tokens.get(0).toUpperCase();

            switch (firstWord) {
                case "USE": return handleUse(tokens);
                case "CREATE": return handleCreate(tokens);
                case "INSERT": return handleInsert(tokens);
                case "SELECT": return handleSelect(tokens);
                case "DROP"  : return handleDrop(tokens);
                case "ALTER" : return handleAlter(tokens);
                case "DELETE": return handleDelete(tokens);
                case "UPDATE": return handleUpdate(tokens);
                case "JOIN"  : return handleJoin(tokens);
                default:
                    return "[ERROR] Unknown command: " + firstWord;
            }
        } catch (Exception e){
            return "[ERROR] " + e.getMessage();
        }
    }

    private String handleUse(List<String> tokens) {
        if (tokens.size() < 2) {
            return "[ERROR] Invalid USE command";
        }

        String dbName = tokens.get(1);
        File dbFolder = new File(storageFolderPath + File.separator + dbName);

        if (dbFolder.exists() && dbFolder.isDirectory()) {
            this.currentDatabase = dbName;
            return "[OK]";
        } else {
            return "[ERROR] Database " + dbName + " does not exist or is not a directory";
        }
    }
    private String handleCreate(List<String> tokens) {
        if (tokens.size() < 3) {
            return "[ERROR] Invalid CREATE command";
        }

        String secondWord = tokens.get(1).toUpperCase();
        if (secondWord.equals("DATABASE")) {
            String dbName = tokens.get(2);
            java.io.File newDbFolder = new java.io.File(storageFolderPath + File.separator + dbName);

            if (newDbFolder.exists()) {
                return "[ERROR] Database already exists";
            } else {
                newDbFolder.mkdirs();
                return "[OK]";
            }
        }
        else if (secondWord.equals("TABLE")) {
            if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
                return "[ERROR] You must USE a database first";
            }
            String tableName = tokens.get(2);

            String tablePath = this.storageFolderPath + File.separator + this.currentDatabase + File.separator + tableName + ".tab";
            File tableFile = new File(tablePath);
            if (tableFile.exists()) {
                return "[ERROR] Table " + tableName + "already exists";
            }
            Table newTable = new Table(tableName);
            newTable.addColumnName("id");

            boolean insideBrackets = false;
            for (int i = 3; i < tokens.size(); i++) {
                String token = tokens.get(i);
                if (token.equals("(")) {
                    insideBrackets = true;
                    continue;
                }
                if (token.equals(")")) {
                    break;
                }
                if (insideBrackets) {
                    if (!token.equals(",")) {
                        newTable.addColumnName(token);
                    }
                }
            }
            try {
                String dbFolderPath = this.storageFolderPath + File.separator + this.currentDatabase;
                newTable.saveToFile(dbFolderPath);
                return "[OK]";
            } catch (IOException e) {
                return "[ERROR] Failed to create table file: " + e.getMessage();
            }
        }
        else {
            return "[ERROR] Invalid CREATE command";
        }

    }
    private String handleInsert(List<String> tokens) {
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            return "[ERROR] You must USE a database first";
        }

        if (tokens.size() < 6 ||
                !tokens.get(0).equals("INSERT") ||
                !tokens.get(1).equals("INTO") ||
                !tokens.get(3).equals("VALUES")
        ) {
            return "[ERROR] Invalid INSERT command";
        }


        String tableName = tokens.get(2);
        String tablePath = this.storageFolderPath + File.separator + this.currentDatabase + File.separator + tableName + ".tab";
        File tableFile = new File(tablePath);
        if (!tableFile.exists()) {
            return "[ERROR] Table " + tableName + " does not exist";
        }

        try {
            Table myTable = loadTableFromFile(tableName);
            Row row = new Row();

            int id = myTable.getNextId();
            row.addValue(String.valueOf(id));

            boolean insideBrackets = false;
            for(int i = 4; i < tokens.size(); i++) {
                String token = tokens.get(i);
                if (token.equals("(")) {
                    insideBrackets = true;
                    continue;
                }
                if (token.equals(")")) {
                    break;
                }
                if (insideBrackets) {
                    if (!token.equals(",")) {
                        row.addValue(token);
                    }
                }
            }
            myTable.addRow(row);

            String dbFolderPath = this.storageFolderPath + File.separator + this.currentDatabase;
            myTable.saveToFile(dbFolderPath);
            return "[OK]";
        } catch (Exception e) {
            return "[ERROR] Failed to insert table: " + e.getMessage();
        }

    }

    // Handles the SELECT command to query and filter data from a specific table.
    private String handleSelect(List<String> tokens) {
        if (tokens.size() < 4) return "[ERROR] Invalid SELECT command";
        if (this.currentDatabase == null || this.currentDatabase.isEmpty())
            return "[ERROR] You must USE a database first";

        // Locate the FROM keyword using native list methods to avoid manual iteration
        int fromIndex = tokens.indexOf("FROM");
        if (fromIndex == -1 || fromIndex + 1 >= tokens.size()) return "[ERROR] Invalid SELECT command";

        // Extract target columns for the projection phase (fetching requested fields)
        List<String> targetColumns = new ArrayList<>();
        for (int i = 1; i < fromIndex; i++) {
            if (!tokens.get(i).equals(",")) targetColumns.add(tokens.get(i));
        }

        // Extract condition tokens for the WHERE clause (selection phase)
        boolean hasWhere = tokens.contains("WHERE");
        List<String> conditionTokens = new ArrayList<>();
        if (hasWhere) {
            int whereIndex = tokens.indexOf("WHERE");
            for (int i = whereIndex + 1; i < tokens.size(); i++) {
                String t = tokens.get(i).replace(";","");
                if (!t.isEmpty())  conditionTokens.add(t);
            }
        }

        String tableName = tokens.get(fromIndex + 1);
        try {
            // Load the table object
            Table myTable = loadTableFromFile(tableName);

            // Handle wildcard character for selecting all columns
            if (targetColumns.contains("*")) {
                targetColumns = new ArrayList<>(myTable.getColumnNames());
            }

            StringBuilder result = new StringBuilder();
            result.append("[OK]").append("\n");

            // Validate and append header columns
            for (String column : targetColumns) {
                if (!myTable.getColumnNames().contains(column)) {
                    return "[ERROR] Column " + column + " does not exist";
                }
                result.append(column).append("\t");
            }
            result.append("\n");

            List<String> columnNames = myTable.getColumnNames();

            // Iterate through rows and evaluate conditions
            for (Row row : myTable.getRows()) {
                boolean shouldPrint = true;

                if (hasWhere) {
                    try {
                        // Delegate logical evaluation to the AST parser
                        shouldPrint = evaluateCondition(row, myTable, conditionTokens);
                    } catch (RuntimeException e) {
                        return e.getMessage();
                    }
                }

                // Append data only if it satisfies the WHERE clause
                if (shouldPrint) {
                    for (String col : targetColumns) {
                        int index = columnNames.indexOf(col);
                        result.append(row.getValueAt(index).replace("'","")).append("\t");
                    }
                    result.append("\n");
                }
            }
            return result.toString();

        } catch (Exception e) {
            return "[ERROR] Failed to insert table: " + e.getMessage();
        }
    }

    private String handleDrop(List<String> tokens) {
        if (tokens.size() < 3) {
            return "[ERROR] Invalid drop command length";
        }
        String targetObjectName = tokens.get(1).toUpperCase();

        if (targetObjectName.equals("TABLE")) {
            if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
                return "[ERROR] You must USE a database first";
            }
            String tableName = tokens.get(2);
            String tablePath = this.storageFolderPath + File.separator + this.currentDatabase + File.separator + tableName + ".tab";
            File tableFile = new File(tablePath);
            if (!tableFile.exists()) {
                return "[ERROR] Table " + tableName + " does not exist";
            } else {
                tableFile.delete();
                return "[OK]";
            }
        } else if (targetObjectName.equals("DATABASE")) {
            String databaseName = tokens.get(2);
            String databasePath = this.storageFolderPath + File.separator + databaseName;
            File databaseFolder = new File(databasePath);
            if (!databaseFolder.exists()) {
                return "[ERROR] Database " + databaseName + " does not exist";
            }

            File[] allFiles = databaseFolder.listFiles();
            if (allFiles != null) {
                for (File file : allFiles) {
                    file.delete();
                }
            }
            databaseFolder.delete();
            if (databaseName.equals(this.currentDatabase)) {
                this.currentDatabase = "";
            }
            return "[OK]";
        }
        return "[ERROR] Invalid DROP target: " + tokens.get(1);

    }

    private String handleDelete(List<String> tokens) throws IOException {
        if (tokens.size() < 7) {
            return "[ERROR] Invalid DELETE command format or missing WHERE clause";
        }
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            return "[ERROR] You must USE a database first";
        }

        String tableName = tokens.get(2);

        try {
            Table myTable = loadTableFromFile(tableName);

            if (!tokens.get(3).equalsIgnoreCase("WHERE")) {
                return "[ERROR] DELETE command must contain a WHERE clause";
            }
            String targetDeleteColumn = tokens.get(4);
            String operator = tokens.get(5);

            String targetDeleteValue = tokens.get(6).replace("'", "").replace(";", "").trim();

            for(int i = myTable.getRows().size() -1; i >= 0; i--) {
                Row row = myTable.getRows().get(i);
                try {
                    if (checkCondition(row, myTable, targetDeleteColumn, operator, targetDeleteValue)) {
                        myTable.getRows().remove(i);
                    }
                } catch (RuntimeException e) {
                    return e.getMessage();
                }
            }
            myTable.saveToFile(this.storageFolderPath + File.separator + this.currentDatabase);

            return "[OK]";
        } catch (Exception e) {
            return "[ERROR] Failed to delete: " + e.getMessage();
        }
    }

    private String handleAlter(List<String> tokens) {
        if (tokens.size() < 5) {
            return "[ERROR] Invalid ALTER command length";
        }
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            return "[ERROR] You must USE a database first";
        }

        String tableName = tokens.get(2);

        try {
            Table myTable = loadTableFromFile(tableName);
            String action = tokens.get(3).toUpperCase();
            String columnName = tokens.get(4);

            if (action.equals("ADD")) {
                if (myTable.getColumnNames().contains(columnName)) {
                    return "[ERROR] Column " + columnName + " already exists";
                }
                myTable.addColumnName(columnName);
                for (Row row : myTable.getRows()) {
                    row.addValue("NULL");
                }

            }else if (action.equals("DROP")) {
                if (!myTable.getColumnNames().contains(columnName)) {
                    return "[ERROR] Column " + columnName + " does not exist";
                } else if (columnName.equalsIgnoreCase("id")) {
                    return "[ERROR] Id cannot be deleted";
                }
                int index = myTable.getColumnNames().indexOf(columnName);
                myTable.getColumnNames().remove(index);
                for (Row row : myTable.getRows()) {
                    row.getValues().remove(index);
                }
            }else {
                return "[ERROR] Invalid ALTER command (ONLY ADD or DROP)";
            }
            String dbFolderPath = this.storageFolderPath + File.separator + this.currentDatabase;
            myTable.saveToFile(dbFolderPath);
            return "[OK]";
        } catch (IOException e) {
            return "[ERROR] Failed to alter table: " + e.getMessage();
        }
    }

    // UPDATE student SET age = 26 WHERE name == 'Alice' ;
    private String handleUpdate(List<String> tokens) throws IOException {
        if (tokens.size() < 6 || !tokens.contains("SET")) return "[ERROR] Invalid UPDATE syntax";
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) return "[ERROR] You must USE a database first";

        try {
            String tableName = tokens.get(1);
            Table myTable = loadTableFromFile(tableName);

            // Locate boundaries for SET and WHERE clauses
            int setIndex = tokens.indexOf("SET");
            int whereIndex = tokens.indexOf("WHERE");
            boolean hasWhere = (whereIndex != -1);
            int endOfSet = hasWhere ? whereIndex : tokens.size();

            // Extract the target column and new value (Assuming standard format: SET column = 'value')
            String targetCol = tokens.get(setIndex + 1);
            String newValue = tokens.get(setIndex + 3).replace("'", "").trim();

            // Delegate index resolution to the Table class
            int targetColIndex = myTable.getColumnIndexOrThrow(targetCol);

            // Extract condition tokens if a WHERE clause exists
            List<String> conditionTokens = new ArrayList<>();
            if (hasWhere) {
                for (int i = whereIndex + 1; i < tokens.size(); i++) {
                    String t = tokens.get(i).replace(";","");
                    if (!t.isEmpty()) conditionTokens.add(t);
                }
            }

            // Iterate through rows and apply the update
            for (Row row : myTable.getRows()) {
                // If no WHERE clause, update all. Otherwise, ask the AST parser.
                boolean shouldUpdate = !hasWhere || evaluateCondition(row, myTable, conditionTokens);

                if (shouldUpdate) {
                    // Delegate the data mutation to the Row class
                    row.updateValueAt(targetColIndex, newValue);
                }
            }
            saveTableToFile(myTable);
            return "[OK]\n";
        } catch (Exception e) {
            return "[ERROR] Failed to update table: " + e.getMessage();
        }

    }

    private  String handleJoin(List<String> tokens)  {
        if (tokens.size() < 8) {
            return "[ERROR] Invalid JOIN command length";
        }
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            return "[ERROR] You must USE a database first";
        }
        String table1Name = tokens.get(1);
        String table2Name = tokens.get(3);
        String col1Name = tokens.get(5);
        String col2Name = tokens.get(7);
        try {
            Table table1 = loadTableFromFile(table1Name);
            Table table2 = loadTableFromFile(table2Name);
            int index1 = table1.getColumnNames().indexOf(col1Name);
            int index2 = table2.getColumnNames().indexOf(col2Name);
            if (index1 == -1 ) {
                return "[ERROR] Column " + col1Name + " does not exist";
            }
            if (index2 == -1 ) {
                return "[ERROR] Column " + col2Name + " does not exist";
            }
            StringBuilder result = new StringBuilder();
            result.append("[OK]");
            result.append("\n");
            result.append("id\t");
            mergeJoinHeaders(table1Name, col1Name, table1, result);
            mergeJoinHeaders(table2Name, col2Name, table2, result);
            result.append("\n");
            int newIdCounter = 1;

            for (Row row1 : table1.getRows()) {
                String val1 = row1.getValues().get(index1).replace("'","").trim();

                for (Row row2 : table2.getRows()) {
                    String val2 = row2.getValues().get(index2).replace("'","").trim();

                    if (val1.equals(val2)) {
                        result.append(newIdCounter).append("\t");
                        newIdCounter++;
                        mergeJoinData(col1Name, table1, result, row1);
                        mergeJoinData(col2Name, table2, result, row2);
                        result.append("\n");
                    }
                }
            }
            return result.toString();

        } catch (Exception e) {
            return "[ERROR] " + e.getMessage();
        }

    }

    private void mergeJoinData(String colName, Table table, StringBuilder result, Row row) {
        for (int j = 0; j < row.getValues().size(); j++) {
            String currentColName = table.getColumnNames().get(j);
            if (currentColName.equals(colName) ||
                currentColName.equalsIgnoreCase("id")) {
                continue;
            }
            result.append(row.getValues().get(j).replace("'","")).append("\t");
        }
    }

    private void mergeJoinHeaders(String tableName, String colName, Table table, StringBuilder result) {
        for (int i = 0; i < table.getColumnNames().size(); i++) {
            if (table.getColumnNames().get(i).equalsIgnoreCase("id") ||
                table.getColumnNames().get(i).equals(colName)) {
                continue;
            } else {
                result.append(tableName + "." + table.getColumnNames().get(i)).append("\t");
            }
        }
    }

    //  === Methods below handle networking aspects of the project - you will not need to change these ! ===

    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
                } catch (IOException e) {
                    System.err.println("Server encountered a non-fatal IO error:");
                    e.printStackTrace();
                    System.err.println("Continuing...");
                }
            }
        }
    }

    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {

            System.out.println("Connection established: " + serverSocket.getInetAddress());
            while (!Thread.interrupted()) {
                String incomingCommand = reader.readLine();
                System.out.println("Received message: " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }

    private boolean checkCondition(Row row, Table table, String columnName, String operator, String targetValue) {
        int colIndex = table.getColumnNames().indexOf(columnName);
        if (colIndex == -1) {
            throw new RuntimeException("[ERROR] Column " + columnName + " does not exist");
        }
        String cellValue = row.getValues().get(colIndex).replace("'","").trim();
        if (operator.equals("==")) {
            return cellValue.equals(targetValue);
        } else if (operator.equals("!=")) {
            return !cellValue.equals(targetValue);
        } else if (operator.equals(">") ||
                operator.equals(">=") ||
                operator.equals("<") ||
                operator.equals("<=")) {
            try {
                float cellNum = Float.parseFloat(cellValue);
                float targetNum = Float.parseFloat(targetValue);
                switch (operator) {
                    case ">": return cellNum > targetNum;
                    case "<": return cellNum < targetNum;
                    case ">=": return cellNum >= targetNum;
                    case "<=": return cellNum <= targetNum;
                }
            }catch (NumberFormatException e) {
                throw new RuntimeException("[ERROR] Cannot use math operators on non-number values");
            }
        } else if (operator.equalsIgnoreCase("LIKE")) {
            return cellValue.contains(targetValue);
        }

        throw new RuntimeException("[ERROR] Unknown operator: " + operator);
    }

    private boolean evaluateCondition(Row row, Table table, List<String> condTokens) {
        if (condTokens.isEmpty() || condTokens == null) {
            throw new RuntimeException("Empty condition");
        }
        int bracketDepth = 0;
        int mainOpIndex = -1;
        String mainOp = "";

        for (int i = 0; i < condTokens.size(); i++) {
            String token = condTokens.get(i);
            if (token.equals("(")) {
                bracketDepth++;
            } else if (token.equals(")")) {
                bracketDepth--;
            } else if (bracketDepth == 0 &&
                    (token.equalsIgnoreCase("AND") || token.equalsIgnoreCase("OR"))) {
                mainOpIndex = i;
                mainOp = token.toUpperCase();
                break;
            }
        }

        if  (mainOpIndex != -1) {
            List<String> leftTokens = condTokens.subList(0, mainOpIndex);
            List<String> rightTokens = condTokens.subList(mainOpIndex + 1, condTokens.size());

            boolean leftResult = evaluateCondition(row, table, leftTokens);
            boolean rightResult = evaluateCondition(row, table, rightTokens);

            if (mainOp.equals("AND")) {
                return leftResult && rightResult;
            } else  {
                return leftResult || rightResult;
            }
        }

        if (condTokens.get(0).equals("(") &&
                condTokens.get(condTokens.size() - 1).equals(")")
        ) {
            return evaluateCondition(row, table, condTokens.subList(1, condTokens.size() - 1));
        }

        if (condTokens.size() == 3) {
            String col = condTokens.get(0);
            String op =  condTokens.get(1);
            String val = condTokens.get(2).replace("'","").trim();

            return checkCondition(row, table, col, op, val);
        }
        throw new RuntimeException("Invalid condition syntax: " + String.join(" ",condTokens));
    }

    private void saveTableToFile(Table tableToSave) {
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            throw new RuntimeException("[ERROR] No database selected. Cannot save table.");
        }

        try {
            // Construct the database directory path
            String dbFolderPath = this.storageFolderPath + File.separator + this.currentDatabase;
            // Delegate the actual file writing to the Table object
            tableToSave.saveToFile(dbFolderPath);
        } catch (IOException e) {
            throw new RuntimeException("[ERROR] Failed to save table to disk: " + e.getMessage());
        }
    }

    private Table loadTableFromFile(String tableName) {
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            throw new RuntimeException("[ERROR] No database selected. Cannot load table.");
        }

        String tablePath = this.storageFolderPath + File.separator + this.currentDatabase + File.separator + tableName + ".tab";
        File file = new File(tablePath);

        if (!file.exists()) {
            throw new RuntimeException("[ERROR] Table " + tableName + " does not exist.");
        }

        try {
            Table loadedTable = new Table(tableName);
            Scanner scanner = new Scanner(file);

            // Phase 1: Parse Header
            if (scanner.hasNextLine()) {
                String headerLine = scanner.nextLine();
                String[] headers = headerLine.split("\t", -1);
                loadedTable.setColumnNames(new ArrayList<>(Arrays.asList(headers)));
            }

            int maxIdFound = 0;
            // Phase 2: Parse Data Rows
            while (scanner.hasNextLine()) {
                String dataLine = scanner.nextLine();
                if (dataLine.trim().isEmpty()) continue; // Skip empty lines

                String[] values = dataLine.split("\t", -1);
                Row newRow = new Row();
                for (String val : values) {
                    newRow.addValue(val);
                }
                loadedTable.addRow(newRow);

                // Phase 3 : ID Recalibration tracking
                if (values.length > 0) {
                    try {
                        int currentId = Integer.parseInt(values[0]);
                         if (currentId > maxIdFound) {
                             maxIdFound = currentId;
                         }
                    } catch (NumberFormatException e) {
                        // Ignore if the first column is not a numeric ID
                    }
                }
                scanner.close();

                // Calibrate the ID generator to avoid collisions with existing data
                loadedTable.updateNextAvailableId(maxIdFound);
            }
            return loadedTable;
        }catch (IOException e) {
            throw new RuntimeException("[ERROR] Failed to read table file: " + e.getMessage());
        }
    }
}


