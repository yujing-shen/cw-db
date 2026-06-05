package edu.uob;

import java.util.List;

/**
 * Recursive descent evaluator for SQL WHERE clause conditions.
 *
 * <p>Parses a flat token list into an implicit AST by locating the top-level
 * logical operator (AND/OR) outside any parentheses, then recursively evaluates
 * the left and right sub-expressions. Parenthesized groups are peeled off layer
 * by layer until a base condition triplet (column, operator, value) is reached.</p>
 *
 * <p>Supported operators:</p>
 * <ul>
 *   <li>Comparison: {@code ==}, {@code !=}, {@code >}, {@code >=}, {@code <}, {@code <=}</li>
 *   <li>Pattern: {@code LIKE} (substring match)</li>
 *   <li>Logical: {@code AND}, {@code OR} (with implicit left-to-right precedence)</li>
 * </ul>
 *
 * <p>Design note: column index resolution is delegated to {@link Table#getColumnIndexOrThrow},
 * and value extraction to {@link Row#getCleanValueAt}, keeping this evaluator decoupled
 * from storage concerns.</p>
 */
public class ConditionEvaluator {

    /**
     * Evaluates a WHERE condition against a given row.
     *
     * <p>Evaluation strategy (recursive descent):</p>
     * <ol>
     *   <li>Scan for a top-level AND/OR operator outside parentheses.</li>
     *   <li>If found, split into left/right sub-expressions and recurse.</li>
     *   <li>If not found but the expression is wrapped in parentheses, strip them and recurse.</li>
     *   <li>Otherwise, evaluate the base condition triplet (column, operator, value).</li>
     * </ol>
     *
     * @param row        the row to evaluate against
     * @param table      the table providing schema information
     * @param condTokens the flat token list of the WHERE clause,
     *                   e.g. ["age", ">", "20", "AND", "name", "==", "'Alice'"]
     * @return {@code true} if the row satisfies the condition, {@code false} otherwise
     * @throws RuntimeException if the condition syntax is invalid or the token list is empty
     */
    public boolean evaluate(Row row, Table table, List<String> condTokens) {
        if (condTokens == null || condTokens.isEmpty()) {
            throw new RuntimeException("[ERROR] Empty condition provided.");
        }

        int bracketDepth = 0;
        int mainOpIndex = -1;
        String mainOp = "";

        // Scan for the top-level logical operator (AND/OR) at bracket depth 0
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

        // Recursive splitting: evaluate left and right sub-expressions independently
        if (mainOpIndex != -1) {
            List<String> leftTokens = condTokens.subList(0, mainOpIndex);
            List<String> rightTokens = condTokens.subList(mainOpIndex + 1, condTokens.size());

            boolean leftResult = evaluate(row, table, leftTokens);
            boolean rightResult = evaluate(row, table, rightTokens);

            return mainOp.equals("AND") ? (leftResult && rightResult) : (leftResult || rightResult);
        }

        // Strip wrapping parentheses and recurse into the inner expression
        if (condTokens.get(0).equals("(") && condTokens.get(condTokens.size() - 1).equals(")")) {
            return evaluate(row, table, condTokens.subList(1, condTokens.size() - 1));
        }

        // Base case: evaluate a simple condition triplet (column, operator, value)
        if (condTokens.size() == 3) {
            String col = condTokens.get(0);
            String op = condTokens.get(1);
            String val = condTokens.get(2); // String literal cleaning is delegated to checkCondition
            return checkCondition(row, table, col, op, val);
        }

        throw new RuntimeException("[ERROR] Invalid condition syntax: " + String.join(" ", condTokens));
    }

    /**
     * Evaluates a base condition (e.g., {@code age > 20}) against a specific row.
     *
     * <p>Delegates column index resolution to {@link Table#getColumnIndexOrThrow}
     * and value extraction to {@link Row#getCleanValueAt} to maintain decoupling
     * from storage internals.</p>
     *
     * <p>Type handling:</p>
     * <ul>
     *   <li>Boolean literals ({@code TRUE}/{@code FALSE}) use case-insensitive comparison with {@code ==}.</li>
     *   <li>Ordering operators ({@code >}, {@code >=}, {@code <}, {@code <=}) parse values as
     *       {@code float}; returns {@code false} if either operand is non-numeric.</li>
     *   <li>{@code LIKE} performs a substring containment check.</li>
     * </ul>
     *
     * @param row         the row to check
     * @param table       the table providing schema reference
     * @param columnName  the column name to compare
     * @param operator    the comparison operator ({@code ==}, {@code !=}, {@code >},
     *                    {@code >=}, {@code <}, {@code <=}, {@code LIKE})
     * @param targetValue the value to compare against (quoted strings are cleaned internally)
     * @return {@code true} if the condition holds, {@code false} otherwise
     * @throws RuntimeException if the column does not exist or the operator is unknown
     */
    private boolean checkCondition(Row row, Table table, String columnName, String operator, String targetValue) {
        // Delegate index resolution to the Table class (decoupling)
        int colIndex = table.getColumnIndexOrThrow(columnName);

        // Delegate data extraction to the Row class; clean the target value locally
        String cellValue = row.getCleanValueAt(colIndex);
        String cleanTarget = targetValue.replace("'", "").trim();

        switch (operator.toUpperCase()) {
            case "==":
                if (cleanTarget.equalsIgnoreCase("TRUE") || cleanTarget.equalsIgnoreCase("FALSE")) {
                    return cellValue.equalsIgnoreCase(cleanTarget);
                }
                return cellValue.equals(cleanTarget);
            case "!=":
                return !cellValue.equals(cleanTarget);
            case "LIKE":
                return cellValue.contains(cleanTarget);
            case ">":
            case ">=":
            case "<":
            case "<=":
                try {
                    float cellNum = Float.parseFloat(cellValue);
                    float targetNum = Float.parseFloat(targetValue);
                    switch (operator) {
                        case ">": return cellNum > targetNum;
                        case "<": return cellNum < targetNum;
                        case "<=": return cellNum <= targetNum;
                        case ">=": return cellNum >= targetNum;
                        default: return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            default:
                throw new RuntimeException("[ERROR] Unknown operator: " + operator);
        }
    }

}
