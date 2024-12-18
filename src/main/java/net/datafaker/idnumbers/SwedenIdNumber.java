package net.datafaker.idnumbers;

import net.datafaker.providers.base.BaseProviders;
import net.datafaker.providers.base.IdNumber;
import net.datafaker.providers.base.PersonIdNumber;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

import static net.datafaker.idnumbers.Utils.gender;
import static net.datafaker.idnumbers.Utils.birthday;

/**
 * Implementation based on the definition at
 * <a href="https://www.skatteverket.se/privat/folkbokforing/personnummer.4.3810a01c150939e893f18c29.html">https://www.skatteverket.se/privat/folkbokforing/personnummer.4.3810a01c150939e893f18c29.html</a>
 * and the description at
 * <a href="https://en.wikipedia.org/wiki/Personal_identity_number_">https://en.wikipedia.org/wiki/Personal_identity_number_</a>(Sweden)
 */
public class SwedenIdNumber implements IdNumberGenerator {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    @Override
    public String countryCode() {
        return "SE";
    }

    private static final String[] VALID_PATTERNS = {"######-####", "######+####"};
    private static final String[] PLUS_MINUS = {"+", "-"};

    @Deprecated
    public String getValidSsn(BaseProviders f) {
        return generateValid(f);
    }

    @Override
    public PersonIdNumber generateValid(BaseProviders f, IdNumber.IdNumberRequest request) {
        LocalDate birthday = birthday(f, request);
        String end = generateEndPart(f);
        String basePart = DATE_TIME_FORMATTER.format(birthday)
            + f.options().option(PLUS_MINUS)
            + end;
        String idNumber = basePart + calculateChecksum(basePart);
        return new PersonIdNumber(idNumber, birthday, gender(f, request));
    }

    public static String generateEndPart(BaseProviders f) {
        return "%03d".formatted(f.number().numberBetween(1, 1000));
    }

    @Deprecated
    public String getInvalidSsn(BaseProviders f) {
        return generateInvalid(f);
    }

    @Override
    public String generateInvalid(BaseProviders f) {
        String candidate = "121212-1212"; // Seed with a valid number
        while (isValidSwedishSsn(candidate)) {
            String pattern = getPattern(f);
            candidate = f.numerify(pattern);
        }

        return candidate;
    }

    private String getPattern(BaseProviders faker) {
        return faker.options().option(VALID_PATTERNS);
    }

    public static boolean isValidSwedishSsn(String ssn) {
        if (ssn.length() != 11) {
            return false;
        }

        try {
            if (parseDate(ssn)) {
                return false;
            }
        } catch (DateTimeParseException | NumberFormatException ignore) {
            return false;
        }

        if (ssn.startsWith("000", 7)) {
            return false;
        }

        int calculatedChecksum = calculateChecksum(ssn);
        int checksum = Integer.parseInt(ssn.substring(10, 11));
        return checksum == calculatedChecksum;
    }

    private static boolean parseDate(String ssn) {
        String dateString = ssn.substring(0, 6);
        if (ChronoField.YEAR.range().isValidIntValue(Integer.parseInt(dateString.substring(0, 2)))) {
            if (ChronoField.MONTH_OF_YEAR.range().isValidIntValue(Integer.parseInt(dateString.substring(2, 4)))) {
                if (ChronoField.DAY_OF_MONTH.range().isValidIntValue(Integer.parseInt(dateString.substring(4)))) {
                    LocalDate date = LocalDate.parse(dateString, DATE_TIME_FORMATTER);
                    // want to check that the parsed date is equal to the supplied data, most of the attempts will fail
                    String reversed = date.format(DATE_TIME_FORMATTER);
                    return !reversed.equals(dateString);
                }
            }
        }
        return true;
    }

    private static int calculateChecksum(String number) {
        String dateString = number.substring(0, 6);
        String birthNumber = number.substring(7, 10);

        String calculatedNumber = calculateDigits(dateString + birthNumber);
        int sum = calculateDigitSum(calculatedNumber);

        int lastDigit = (sum % 10);
        int difference = 10 - lastDigit;

        return (difference % 10);
    }

    private static String calculateDigits(String numbers) {
        StringBuilder calculatedNumbers = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            int res;
            int n = numbers.charAt(i) - '0';
            if (i % 2 == 0) {
                res = n << 1;
            } else {
                res = n;
            }

            calculatedNumbers.append(res);
        }
        return calculatedNumbers.toString();
    }

    private static int calculateDigitSum(String numbers) {
        int sum = 0;
        final int length = numbers.length();
        for (int i = 0; i < length; i++) {
            int n = numbers.charAt(i) - '0';
            sum += n;
        }
        return sum;
    }
}
