package util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class FineCalculator {
    private static final double FINE_PER_DAY = 5.0;

    public static double finePerDay() {
        return FINE_PER_DAY;
    }

    public static double calculateFine(LocalDate dueDate, LocalDate returnDate) {
        long daysLate = ChronoUnit.DAYS.between(dueDate, returnDate);
        if (daysLate > 0) {
            return daysLate * FINE_PER_DAY;
        }
        return 0;
    }
}
