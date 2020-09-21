package com.fsck.k9.fabmenu

import com.fsck.k9.pEp.ui.infrastructure.MessageAction
import com.fsck.k9.pEp.ui.listeners.OnMessageOptionsListener
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import security.pEp.ui.message_compose.PEpFabMenuPresenter
import security.pEp.ui.message_compose.PEpFabMenuView

class PEpFabMenuPresenterIntegrationTest {

    @Mock
    private lateinit var view: PEpFabMenuView
    @Mock
    private lateinit var listener: OnMessageOptionsListener

    private lateinit var presenter: PEpFabMenuPresenter

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        presenter = PEpFabMenuPresenter(view)
        presenter.listener = listener
    }

    @Test
    fun presenterStartShouldShowInitialState() {
        presenter.init()
        verify(view).showInitialState()
        verify(view, never()).closeMenu()
        verify(view, never()).openMenu()
    }

    @Test
    fun onLongClickShouldOpenMenu() {
        presenter.onLongClicked()

        verify(view, times(1)).openMenu()
        verify(view, never()).closeMenu()
    }

    @Test
    fun onDoubleLongClickShouldCloseMenu() {
        presenter.onLongClicked()

        verify(view, times(1)).openMenu()
        verify(view, never()).closeMenu()

        presenter.onLongClicked()

        verify(view, times(1)).closeMenu()
        verify(view, times(1)).openMenu()
    }

    @Test
    fun onClickMainActionShouldCloseMenu() {
        presenter.onLongClicked()
        presenter.onMainActionClicked()

        verify(view, times(1)).openMenu()
        verify(view, times(1)).closeMenu()
    }

    @Test
    fun onClickMainActionShouldReplyAction() {
        presenter.onMainActionClicked()

        verify(listener, times(1)).OnMessageOptionsListener(MessageAction.REPLY)
        verify(view, never()).closeMenu()
        verify(view, never()).openMenu()
    }

    @Test
    fun onReplyActionShouldReplyAction() {
        presenter.onReplyClicked()

        verify(listener, times(1)).OnMessageOptionsListener(MessageAction.REPLY)
        verify(view, never()).closeMenu()
        verify(view, never()).openMenu()
    }

    @Test
    fun onReplyAllActionShouldReplyAllAction() {
        presenter.onReplyAllClicked()

        verify(listener, times(1)).OnMessageOptionsListener(MessageAction.REPLY_ALL)
        verify(view, never()).closeMenu()
        verify(view, never()).openMenu()
    }

    @Test
    fun onForwardActionShouldForwardAction() {
        presenter.onForwardClicked()

        verify(listener, times(1)).OnMessageOptionsListener(MessageAction.FORWARD)
        verify(view, never()).closeMenu()
        verify(view, never()).openMenu()
    }


}