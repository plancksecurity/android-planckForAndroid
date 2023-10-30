package security.planck.common

abstract class LiveDataTest<T> : LiveDataBaseTest<T, T>() {
    override fun observeLiveData() {
        testLivedata.observeForever { observedValues.add(it) }
    }
}