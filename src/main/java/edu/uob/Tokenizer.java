package edu.uob;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a SQL-style command into a list of tokens (words and symbols).
 * String literals in single quotes stay as one token, e.g., 'Steve Jobs'
 */
public class Tokenizer {
    public Tokenizer() {
    }

    public List<String> parseTokens(String query) {
        List<String> tokens = new ArrayList<>();
        
        // delete the space in the start and end
        query = query.trim(); 

        // Split on every single quote: even index = outside quotes, add = inside a literal
        String[] fragments = query.split("'");
        for (int i = 0; i < fragments.length; i++) {
            if (i % 2 != 0) {
                // Odd fragment: content was between quotes — keep as one token including quotes
                tokens.add("'" + fragments[i] + "'");
            } else {
                // Even fragment: normal SQL — split operators/symbols, then split on whitespace
                String outside = fragments[i];

                // Placeholders stop "==" from becoming two "=" tokens when we space out "="
                outside = outside.replace("==", " [EQ] ");
                outside = outside.replace(">=", " [GE] ");
                outside = outside.replace("<=", " [LE] ");
                outside = outside.replace("!=", " [NE] ");

                outside = outside.replace("=", " = ");
                outside = outside.replace(">", " > ");
                outside = outside.replace("<", " < ");
                outside = outside.replace("(", " ( ");
                outside = outside.replace(")", " ) ");
                outside = outside.replace(",", " , ");
                outside = outside.replace(";", " ; ");

                // Restore compound operators (now surrounded by spaces for splitting)
                outside = outside.replace("[EQ]", "==");
                outside = outside.replace("[GE]", ">=");
                outside = outside.replace("[LE]", "<=");
                outside = outside.replace("[NE]", "!=");

                // One or more whitespace characters — treats "a   b" like "a b"
                String[] words = outside.split("\\s+");
                for (String word : words) {
                    if (!(word.isEmpty())) {
                        tokens.add(word);
                    }
                }
            }
        }
        return tokens;
    }

}
