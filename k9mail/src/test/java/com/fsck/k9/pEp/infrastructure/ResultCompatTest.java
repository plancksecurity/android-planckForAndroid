package com.fsck.k9.pEp.infrastructure;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import static org.junit.Assert.assertThrows;

import org.junit.Test;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class ResultCompatTest {

    @Test
    public void resultCompat_of_returns_Success_if_all_goes_well() {
        ResultCompat<Boolean> result = ResultCompat.of(() -> true);


        assertTrue(result.isSuccess());
        assertTrue(result.getOrThrow());
    }

    @Test
    public void resultCompat_onSuccess_is_called_on_success() {
        boolean[] called = new boolean[1];
        ResultCompat.success(true).onSuccess(bool -> {
            called[0] = bool;
            return null;
        });


        assertTrue(called[0]);
    }

    @Test
    public void resultCompat_of_returns_Failure_if_it_goes_wrong() {
        ResultCompat<Boolean> result = ResultCompat.of(
                () -> booleanThrow(new RuntimeException("test")));


        assertTrue(result.isFailure());
        assertTrue(result.exceptionOrNull() instanceof RuntimeException);
        assertEquals("test", result.exceptionOrThrow().getMessage());
    }

    @Test
    public void resultCompat_onFailure_is_called_on_failure() {
        Throwable[] called = new Throwable[1];
        ResultCompat.failure(new RuntimeException("test")).onFailure(throwable -> {
            called[0] = throwable;
            return null;
        });


        assertTrue(called[0] instanceof RuntimeException);
        assertEquals("test", called[0].getMessage());
    }

    @Test
    public void resultCompat_map_maps_successful_result_to_required_type() {
        ResultCompat<Boolean> booleanResult = ResultCompat.success(true);
        ResultCompat<String> stringResult =
                booleanResult.map(this::booleanToString);


        assertTrue(stringResult.isSuccess());
        assertEquals("true", stringResult.getOrThrow());
    }

    @Test
    public void resultCompat_map_carries_input_failure_across() {
        ResultCompat<Boolean> booleanResult = ResultCompat.failure(new RuntimeException("test"));
        ResultCompat<String> stringResult =
                booleanResult.map(this::booleanToString);


        assertTrue(stringResult.isFailure());
        assertEquals("test", stringResult.exceptionOrThrow().getMessage());
    }

    @Test
    public void resultCompat_map_throws_new_failure() {
        ResultCompat<Boolean> booleanResult = ResultCompat.success(true);
        RuntimeException throwable = assertThrows(
                RuntimeException.class,
                () -> booleanResult.map(
                        (bool) -> booleanToStringThrow(new RuntimeException("test"))
                )
        );


        assertEquals("test", throwable.getMessage());
    }

    @Test
    public void resultCompat_mapCatching_maps_successful_result_to_required_type() {
        ResultCompat<Boolean> booleanResult = ResultCompat.success(true);
        ResultCompat<String> stringResult =
                booleanResult.mapCatching(this::booleanToString);


        assertTrue(stringResult.isSuccess());
        assertEquals("true", stringResult.getOrThrow());
    }

    @Test
    public void resultCompat_mapCatching_carries_input_failure_across() {
        ResultCompat<Boolean> booleanResult = ResultCompat.failure(new RuntimeException("test"));
        ResultCompat<String> stringResult =
                booleanResult.mapCatching(this::booleanToString);


        assertTrue(stringResult.isFailure());
        assertEquals("test", stringResult.exceptionOrThrow().getMessage());
    }

    @Test
    public void resultCompat_mapCatching_returns_new_failure() {
        ResultCompat<Boolean> booleanResult = ResultCompat.success(true);
        ResultCompat<String> stringResult =
                booleanResult.mapCatching(
                        (bool) -> booleanToStringThrow(new RuntimeException("test")));


        assertTrue(stringResult.isFailure());
        assertEquals("test", stringResult.exceptionOrThrow().getMessage());
    }

    @Test
    public void resultCompat_flatMap_maps_successful_result_to_ResultCompat_of_required_type() {
        ResultCompat<Boolean> booleanResult = ResultCompat.success(true);
        ResultCompat<String> stringResult = booleanResult.flatMap(this::booleanToStringResult);


        assertTrue(stringResult.isSuccess());
        assertEquals("true", stringResult.getOrThrow());
    }

    @Test
    public void resultCompat_flatMap_carries_input_failure_across() {
        ResultCompat<Boolean> booleanResult = ResultCompat.failure(new RuntimeException("test"));
        ResultCompat<String> stringResult = booleanResult.flatMap(this::booleanToStringResult);


        assertTrue(stringResult.isFailure());
        assertEquals("test", stringResult.exceptionOrThrow().getMessage());
    }

    @Test
    public void resultCompat_flatMap_throws_new_failure() {
        ResultCompat<Boolean> booleanResult = ResultCompat.success(true);
        RuntimeException throwable = assertThrows(
                RuntimeException.class,
                () -> booleanResult.flatMap(
                        (bool) -> booleanToStringResultThrow(new RuntimeException("test"))
                )
        );


        assertEquals("test", throwable.getMessage());
    }


    private boolean booleanThrow(RuntimeException throwable) {
        throw throwable;
    }

    private String booleanToString(boolean input) {
        return "" + input;
    }

    private ResultCompat<String> booleanToStringResult(boolean input) {
        return ResultCompat.success("" + input);
    }

    private ResultCompat<String> booleanToStringResultThrow(RuntimeException throwable) {
        throw throwable;
    }

    private String booleanToStringThrow(RuntimeException throwable) {
        throw throwable;
    }
}
