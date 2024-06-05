package security.planck.ui.passphrase

import foundation.pEp.jniadapter.Pair
import junit.framework.TestCase

internal fun assertPairArrayList(
    expected: List<Pair<String, String>>,
    actual: ArrayList<Pair<String, String>>
) {
    TestCase.assertEquals(expected.size, actual.size)
    expected.forEachIndexed { index, pair ->
        TestCase.assertEquals(pair.first, actual[index].first)
        TestCase.assertEquals(pair.second, actual[index].second)
    }
}