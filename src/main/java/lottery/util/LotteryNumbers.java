package lottery.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class LotteryNumbers {
    private LotteryNumbers() {}

    public static List<Integer> generateWinningCombo() {
        Random random = new Random();
        Set<Integer> set = new LinkedHashSet<>();
        while (set.size() < 5) {
            set.add(random.nextInt(50) + 1);
        }
        return new ArrayList<>(set).stream().sorted().toList();
    }

    public static String joinNumbers(List<Integer> numbers) {
        return numbers.stream().sorted().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
    }

    public static List<Integer> parseNumbers(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        String[] parts = csv.split(",");
        List<Integer> nums = new ArrayList<>();
        for (String part : parts) {
            nums.add(Integer.parseInt(part));
        }
        return nums;
    }

    public static boolean isMatch(String left, String right) {
        return normalizeNumbers(left).equals(normalizeNumbers(right));
    }

    private static String normalizeNumbers(String numbers) {
        List<Integer> list = parseNumbers(numbers).stream().sorted().toList();
        return joinNumbers(list);
    }
}
