package security.planck.common

import com.fsck.k9.planck.infrastructure.livedata.Event

abstract class LiveDataEventTest<T> : LiveDataBaseTest<T, Event<T>>() {
    override fun observeLiveData() {
        testLivedata.observeForever { event ->
            event.getContentIfNotHandled()?.let { observedValues.add(it) }
        }
    }
}