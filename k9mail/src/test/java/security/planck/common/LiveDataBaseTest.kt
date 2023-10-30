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

    protected abstract fun initialize()

    protected fun assertObservedValues(vararg values: BaseType) {
        assertEquals(values.toList(), observedValues)
    }

    protected fun assertFirstObservedValues(vararg values: BaseType) {
        values.forEachIndexed { index, value ->
            assertEquals(value, observedValues[index])
        }
    }

    abstract fun observeLiveData()

}