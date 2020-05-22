package benchmark;

import benchmark.regexconverter.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.sat4j.specs.TimeoutException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.List;

class Regex {
    String pattern;
    int atomicGroups;
    boolean hasNested;
    String translatedPattern;
    int length;
}

public class RunRegexTranslation {

    private static final Type REVIEW_TYPE = new TypeToken<List<Regex>>() { }.getType();

    private static final Gson gson = new Gson();

    private static final String REGEXES = "/home/steyn/lookahead-paper/data/translatable/atomic.json";

    public static void main(String[] args) throws IOException {
        JsonReader reader = new JsonReader(new FileReader(REGEXES));
        int total = 0;
        int couldTranslate = 0;

        List<Regex> data = gson.fromJson(reader, REVIEW_TYPE);
        for (Regex regex : data) {
            total++;
            if (regex.pattern.length() > 100) continue;

            String translated;
            try {
                translated = Utils.translateRegex(regex.pattern);
            } catch (NullPointerException | TimeoutException | UnsupportedOperationException e) {
                System.out.printf("Could not parse %s (%s)\n", regex.pattern, e);
//                e.printStackTrace();
                continue;
            }

            regex.translatedPattern = translated;
            couldTranslate++;
        }

        System.out.println("Total = " + total);
        System.out.println("Could translate = " + couldTranslate);

        try (Writer writer = new FileWriter("/home/steyn/lookahead-paper/data/translatable/translated_atomic.json")) {
            Gson gson = new GsonBuilder()
                    .disableHtmlEscaping()
                    .setPrettyPrinting()
                    .create();
            gson.toJson(data, writer);
        }

    }

}
