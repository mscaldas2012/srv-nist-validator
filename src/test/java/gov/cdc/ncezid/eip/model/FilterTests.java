package gov.cdc.ncezid.eip.model;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FilterTests {

    class MyObj {
        public String classif;

        public MyObj(String classif) {
            this.classif = classif;
        }
        public String getClassif() {
            return this.classif;
        }
    }

    private static final String STRUCT = "struct";
    private static final String CONTENT = "content";

    private static final String ERROR = "Error";
    private static final String WARN = "Warning";

    @Test
    public void testCounts() {
        Map<String, List<MyObj>> example = new HashMap<>();

        ArrayList<MyObj> structure = new ArrayList<MyObj>(); //5 Errros, 3 warnings
        structure.add(new MyObj(ERROR));
        structure.add(new MyObj(ERROR));
        structure.add(new MyObj(WARN));
        structure.add(new MyObj(ERROR));
        structure.add(new MyObj(WARN));
        structure.add(new MyObj(ERROR));
        structure.add(new MyObj(WARN));
        structure.add(new MyObj(ERROR));

        List<MyObj> content = new ArrayList<>(); //7 Errors, 0 Warning
        content.add(new MyObj(ERROR));
        content.add(new MyObj(ERROR));
        content.add(new MyObj(ERROR));
        content.add(new MyObj(ERROR));
        content.add(new MyObj(ERROR));
        content.add(new MyObj(ERROR));
        content.add(new MyObj(ERROR));

        example.put(STRUCT, structure);
        example.put(CONTENT, content);

        java.util.Map < String, Long > allErrors = new HashMap<>();

        //java.util.Map < String, Long > errorsCount = null;
        for (Map.Entry<String, List<MyObj>> e: example.entrySet()) {
            Map < String, Long > errorsCount = e.getValue().stream().collect(Collectors.groupingBy(s -> s.getClassif(), Collectors.counting()));
            allErrors.put(e.getKey(), errorsCount.get(ERROR));
        }

        allErrors.entrySet().stream().forEach(e -> System.out.println(e.getKey() + ": "+ e.getValue()));
//
    }
}
