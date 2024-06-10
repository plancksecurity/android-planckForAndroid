package security.planck.common

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestRule

abstract class LiveDataBaseTest<BaseType, LiveDataType> {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()
    protected val observedValues = mutableListOf<BaseType>()
    protected abstract val testLivedata: LiveData<LiveDataType>

    @Before
    open fun liveDataTestSetup() {
        observedValues.clear()
        initialize()
        observeLiveData()
    }

    protected open fun initialize() {}

    protected fun assertObservedValues(vararg values: BaseType) {
        println("########################################")
        println("observed values: \n${
            observedValues
                .mapIndexed { index, value -> "$index: $value" }
                .joinToString("\n\n")
        }")
        println("########################################")
        assertEquals(
            "expected ${values.size} values but got ${observedValues.size} values instead",
            values.size, observedValues.size
        )
        values.forEachIndexed { index, value ->
            assertEquals(
                "FAILURE AT POSITION $index:",
                value, observedValues[index]
            )
        }
    }

    protected fun customAssertObservedValues(vararg assertions: (BaseType) -> Unit) {
        println("observed values: \n${observedValues.joinToString("\n")}")
        assertEquals(assertions.size, observedValues.size)
        assertions.forEachIndexed { index, assertion ->
            assertion(observedValues[index])
        }
    }

    protected fun assertFirstObservedValues(vararg values: BaseType) {
        values.forEachIndexed { index, value ->
            assertEquals(value, observedValues[index])
        }
    }

    abstract fun observeLiveData()

}