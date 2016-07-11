package com.emc.nautilus.testcommon;

import org.junit.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Additional Assert Methods that are useful during testing.
 */
public class AssertExtensions {
    /**
     * Asserts that a function throws an expected exception.
     *
     * @param message  The message to include in the Assert calls.
     * @param runnable The function to test.
     * @param tester   A predicate that indicates whether the exception (if thrown) is as expected.
     */
    public static void assertThrows(String message, RunnableWithException runnable, Predicate<Throwable> tester) {
        try {
            runnable.run();
            Assert.fail(message + " No exception has been thrown.");
        } catch (CompletionException ex) {
            if (!tester.test(ex.getCause())) {
                Assert.fail(message + " Exception thrown was of unexpected type: " + ex.getCause());
            }
        } catch (Exception ex) {
            if (!tester.test(ex)) {
                Assert.fail(message + " Exception thrown was of unexpected type: " + ex);
            }
        }
    }

    /**
     * Asserts that a future (that has not yet been invoked) throws an expected exception.
     *
     * @param message        The message to include in the Assert calls.
     * @param futureSupplier A Supplier that returns a new CompletableFuture, to test.
     * @param tester         A predicate that indicates whether the exception (if thrown) is as expected.
     * @param <T>
     */
    public static <T> void assertThrows(String message, Supplier<CompletableFuture<T>> futureSupplier, Predicate<Throwable> tester) {
        try {
            futureSupplier.get().join();
            Assert.fail(message + " No exception has been thrown.");
        } catch (CompletionException ex) {
            if (!tester.test(getRealException(ex))) {
                Assert.fail(message + " Exception thrown was of unexpected type: " + getRealException(ex));
            }
        } catch (Exception ex) {
            if (!tester.test(ex)) {
                Assert.fail(message + " Exception thrown was of unexpected type: " + ex);
            }
        }
    }

    /**
     * Asserts that a future throws an expected exception.
     *
     * @param message The message to include in the Assert calls.
     * @param future  A the CompletableFuture to test.
     * @param tester  A predicate that indicates whether the exception (if thrown) is as expected.
     * @param <T>
     */
    public static <T> void assertThrows(String message, CompletableFuture<T> future, Predicate<Throwable> tester) {
        try {
            future.join();
            Assert.fail(message + " No exception has been thrown.");
        } catch (CompletionException ex) {
            if (!tester.test(getRealException(ex))) {
                Assert.fail(message + " Exception thrown was of unexpected type: " + getRealException(ex));
            }
        } catch (Exception ex) {
            if (!tester.test(ex)) {
                Assert.fail(message + " Exception thrown was of unexpected type: " + ex);
            }
        }
    }

    /**
     * Asserts that the contents of the given arrays are the same.
     *
     * @param message The message to include in the Assert calls.
     * @param array1  The first array to check.
     * @param offset1 The offset within the first array to start checking at.
     * @param array2  The second array to check.
     * @param offset2 The offset within the second array to start checking at.
     * @param length  The number of elements to check.
     */
    public static void assertArrayEquals(String message, byte[] array1, int offset1, byte[] array2, int offset2, int length) {
        // We could do argument checks here, but the array access below will throw the appropriate exceptions if any of these args are out of bounds.
        for (int i = 0; i < length; i++) {
            if (array1[i + offset1] != array2[i + offset2]) {
                Assert.fail(String.format(
                        "%s Arrays differ at check-offset %d. Array1[%d]=%d, Array2[%d]=%d.",
                        message,
                        i,
                        offset1 + i,
                        array1[i + offset1],
                        offset2 + i,
                        array2[i + offset2]));
            }
        }
    }

    /**
     * Asserts that the contents of the given InputStream are the same.
     *
     * @param message   The message to include in the Assert calls.
     * @param s1        The first InputStream to check.
     * @param s2        The second InputStream to check.
     * @param maxLength The maximum number of bytes to check.
     */
    public static void assertStreamEquals(String message, InputStream s1, InputStream s2, int maxLength) throws IOException {
        int readSoFar = 0;
        while (readSoFar < maxLength) {
            int b1 = s1.read();
            int b2 = s2.read();
            if (b1 != b2) {
                // This also includes the case when one stream ends prematurely.
                Assert.fail(String.format("%s InputStreams differ at index %d. Expected %d, actual %d.", message, readSoFar, b1, b2));
            }

            readSoFar++;
            if (b1 < 0) {
                break;// We have reached the end of both streams.
            }
        }
    }

    /**
     * Asserts that the contents of the given collections are the same (in any order).
     *
     * @param message  The message to include in the Assert calls.
     * @param expected A collection to check against.
     * @param actual   The collection to check.
     * @param <T>
     */
    public static <T extends Comparable<? super T>> void assertContainsSameElements(String message, Collection<T> expected, Collection<T> actual) {
        Assert.assertEquals(String.format("%s Collections differ in size.", message), expected.size(), actual.size());
        for (T e : expected) {
            if (!actual.contains(e)) {
                Assert.fail(String.format("%s Element %s does not exist.", message, e));
            }
        }
    }

    /**
     * Asserts that the contents of the given collections are the same (in any order).
     *
     * @param message  The message to include in the Assert calls.
     * @param expected A collection to check against.
     * @param actual   The collection to check.
     * @param <T>
     */
    public static <T> void assertContainsSameElements(String message, Collection<T> expected, Collection<T> actual, Comparator<T> comparator) {
        Assert.assertEquals(String.format("%s Collections differ in size.", message), expected.size(), actual.size());
        for (T e : expected) {
            boolean contains = false;
            for (T a : actual) {
                if (comparator.compare(e, a) == 0) {
                    contains = true;
                    break;
                }
            }

            if (!contains) {
                Assert.fail(String.format("%s Element %s does not exist.", message, e));
            }
        }
    }

    /**
     * Asserts that the given lists contain equivalent elements (at the same indices), based on the given tester function.
     *
     * @param message  The message to include in the Assert calls.
     * @param expected The list to check against.
     * @param actual   The list to check.
     * @param tester   A BiConsumerWithMessage that will be used to assert the elements at the same indices in each list are equivalent.
     * @param <T>
     */
    public static <T> void assertListEquals(String message, List<T> expected, List<T> actual, BiConsumerWithMessage<T, T> tester) {
        Assert.assertEquals(String.format("%s Collections differ in size.", message), expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            T expectedItem = expected.get(i);
            T actualItem = actual.get(i);
            tester.accept(String.format("%s Elements at index %d differ. Expected '%s', found '%s'.", message, i, expectedItem, actualItem), expectedItem, actualItem);
        }
    }

    /**
     * Asserts that expected > actual.
     *
     * @param message  The message to include in the Assert calls.
     * @param expected The first value (smaller).
     * @param actual   The second value (larger).
     */

    public static void assertLessThan(String message, long expected, long actual) {
        Assert.assertTrue(String.format("%s Expected: less than %d. Actual: %d.", message, expected, actual), expected > actual);
    }

    /**
     * Asserts that expected >= actual.
     *
     * @param message  The message to include in the Assert calls.
     * @param expected The first value (smaller).
     * @param actual   The second value (larger).
     */
    public static void assertLessThanOrEqual(String message, long expected, long actual) {
        Assert.assertTrue(String.format("%s Expected: less than or equal to %d. Actual: %d.", message, expected, actual), expected >= actual);
    }

    /**
     * Asserts that expected < actual.
     *
     * @param message  The message to include in the Assert calls.
     * @param expected The first value (larger).
     * @param actual   The second value (smaller).
     */
    public static void assertGreaterThan(String message, long expected, long actual) {
        Assert.assertTrue(String.format("%s Expected: greater than %d. Actual: %d.", message, expected, actual), expected < actual);
    }

    /**
     * Asserts that expected <= actual.
     *
     * @param message  The message to include in the Assert calls.
     * @param expected The first value (larger).
     * @param actual   The second value (smaller).
     */
    public static void assertGreaterThanOrEqual(String message, long expected, long actual) {
        Assert.assertTrue(String.format("%s Expected: greater than or equal to %d. Actual: %d.", message, expected, actual), expected <= actual);
    }

    /**
     * Asserts that string is null or equal to the empty string.
     *
     * @param message The message to include in the Assert calls.
     * @param string  The String to test.
     */
    public static void assertNullOrEmpty(String message, String string) {
        Assert.assertTrue(message, string == null || string.length() == 0);
    }

    /**
     * Asserts that string is not null or an empty string.
     *
     * @param message The message to include in the Assert calls.
     * @param string  The String to test.
     */
    public static void assertNotNullOrEmpty(String message, String string) {
        Assert.assertFalse(message, string == null || string.length() == 0);
    }

    private static Throwable getRealException(Throwable ex) {
        if (ex instanceof CompletionException) {
            return getRealException(ex.getCause());
        }

        if (ex instanceof ExecutionException) {
            return getRealException(ex.getCause());
        }

        return ex;
    }

    public interface RunnableWithException {
        void run() throws Exception;
    }

    public interface BiConsumerWithMessage<T, U> {
        void accept(String message, T var1, U var2);
    }
}
