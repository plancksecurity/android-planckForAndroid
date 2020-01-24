package com.fsck.k9;


import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;


public class MockHelper {
    public static <T> T mockBuilder(Class<T> classToMock) {
        return mock(classToMock, invocation -> {
            Object mock = invocation.getMock();
            if (invocation.getMethod().getReturnType().isInstance(mock)) {
                return mock;
            } else {
                return RETURNS_DEFAULTS.answer(invocation);
            }
        });
    }
}
