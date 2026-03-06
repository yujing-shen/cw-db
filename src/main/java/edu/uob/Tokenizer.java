package edu.uob;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    public Tokenizer() {
    }

    public List<String> tokenize(String query) {
        List<String> tokens = new ArrayList<>();
        query = query.trim(); // delete the space in the start and end

        String[] fragments = query.split("'");
        for (int i = 0; i < fragments.length; i++) {
            if (i % 2 != 0) {
                tokens.add("'" + fragments[i] + "'");
            } else {
                String outside = fragments[i];
                outside = outside.replace("(", " ( ");
                outside = outside.replace(")", " ) ");
                outside = outside.replace(",", " , ");
                outside = outside.replace(";", " ; ");

                outside = outside.replace("==", " == ");
                outside = outside.replace(">=", " >= ");
                outside = outside.replace("<=", " <= ");
                outside = outside.replace("!=", " != ");

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
