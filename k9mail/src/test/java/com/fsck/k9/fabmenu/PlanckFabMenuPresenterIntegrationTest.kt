package com.fsck.k9.fabmenu

import com.fsck.k9.planck.ui.infrastructure.MessageAction
import com.fsck.k9.planck.ui.listeners.OnMessageOptionsListener
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import security.planck.ui.message_compose.PlanckFabMenuPresenter
import security.planck.ui.message_compose.PlanckFabMenuView

class PlanckFabMenuPresenterIntegrationTest {

    @Mock
    private lateinit var view: PlanckFabMenuView
    @Mock
    private lateinit var listener: OnMessageOptionsListener

    private lateinit var presenter: PlanckFabMenuPresenter

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        presenter = PlanckFabMenuPresenter(view)
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