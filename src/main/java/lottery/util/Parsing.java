package lottery.util;

import java.util.ArrayList;
import java.util.List;

public final class Parsing {
    private Parsing() {}

    public static String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    public static long asLong(Object value) {
        if (value == null) {
            return -1;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return -1;
        }
    }

    public static long queryParamAsLong(String query, String key) {
        if (query == null || query.isBlank()) {
            return -1;
        }
        String[] parts = query.split("&");
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                try {
                    return Long.parseLong(kv[1]);
                } catch (Exception ignored) {
                    return -1;
                }
            }
        }
        return -1;
    }

    public static List<Integer> toIntList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Integer> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Number number) {
                out.add(number.intValue());
            } else {
                try {
                    out.add(Integer.parseInt(String.valueOf(item)));
                } catch (Exception e) {
                    return List.of();
                }
            }
        }
        return out;
    }
}
