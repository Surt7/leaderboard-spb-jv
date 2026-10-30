package com.worldpinball.leaderboard.testdata;

import net.datafaker.Faker;
import org.instancio.Instancio;

import java.util.Locale;
import java.util.UUID;

/**
 * Equivalent to C#'s Bogus + AutoFixture in Java.
 * - Datafaker: generates realistic pseudo-random domain data (serials, names, emails, timestamps).
 * - Instancio: automatically populates entire POJO, Entity, or Record object graphs with randomized values.
 */
public final class TestDataFactory {

    public static final Faker FAKER = new Faker(Locale.US);

    private TestDataFactory() {}

    public static String randomSerialNumber() {
        return "WP-" + FAKER.number().digits(8);
    }

    public static String randomDisplayName() {
        return FAKER.videoGame().title() + " Pinball";
    }

    public static String randomInitials() {
        return FAKER.regexify("[A-Z]{3}");
    }

    public static String randomIpSlug() {
        return FAKER.internet().slug();
    }

    public static long randomScore() {
        return FAKER.number().numberBetween(100_000L, 50_000_000L);
    }

    public static UUID randomUuid() {
        return UUID.randomUUID();
    }

    public static <T> T create(Class<T> type) {
        return Instancio.create(type);
    }
}
