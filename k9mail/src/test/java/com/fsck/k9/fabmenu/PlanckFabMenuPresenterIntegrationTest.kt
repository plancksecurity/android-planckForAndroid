package com.fsck.k9.fabmenu

import com.fsck.k9.planck.ui.infrastructure.MessageAction
import com.fsck.k9.planck.ui.listeners.OnMessageOptionsListener
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import security.planck.ui.message_compose.PlanckFabMenuPresenter
import security.planck.ui.message_compose.PlanckFabMenuView

class PlanckFabMenuPresenterIntegrationTest {
    private val view: PlanckFabMenuView = mockk(relaxed = true)
    private val listener: OnMessageOptionsListener = mockk(relaxed = true)

    private lateinit var presenter: PlanckFabMenuPresenter

    @Before
    fun setup() {
        presenter = PlanckFabMenuPresenter(view).also { it.listener = listener }
    }

    @Test
    fun presenterStartShouldShowInitialState() {
        presenter.init()
        verify { view.showInitialState() }
        verify(exactly = 0) { view.closeMenu() }
        verify(exactly = 0) { view.openMenu() }
    }

    @Test
    fun onLongClickShouldOpenMenu() {
        presenter.onLongClicked()

        verify(exactly = 1) { view.openMenu() }
        verify(exactly = 0) { view.closeMenu() }
    }

    @Test
    fun onDoubleLongClickShouldCloseMenu() {
        presenter.onLongClicked()

        verify(exactly = 1) { view.openMenu() }
        verify(exactly = 0) { view.closeMenu() }

        presenter.onLongClicked()

        verify(exactly = 1) { view.closeMenu() }
        verify(exactly = 1) { view.openMenu() }
    }

    @Test
    fun onClickMainActionShouldCloseMenu() {
        presenter.onLongClicked()
        presenter.onMainActionClicked()

        verify(exactly = 1) { view.openMenu() }
        verify(exactly = 1) { view.closeMenu() }
    }

    @Test
    fun onClickMainActionShouldReplyAction() {
        presenter.onMainActionClicked()

        verify(exactly = 1) { listener.OnMessageOptionsListener(MessageAction.REPLY) }
        verify(exactly = 0) { view.closeMenu() }
        verify(exactly = 0) { view.openMenu() }
    }

    @Test
    fun onReplyActionShouldReplyAction() {
        presenter.onReplyClicked()

        verify(exactly = 1) { listener.OnMessageOptionsListener(MessageAction.REPLY) }
        verify(exactly = 0) { view.closeMenu() }
        verify(exactly = 0) { view.openMenu() }
    }

    @Test
    fun onReplyAllActionShouldReplyAllAction() {
        presenter.onReplyAllClicked()

        verify(exactly = 1) { listener.OnMessageOptionsListener(MessageAction.REPLY_ALL) }
        verify(exactly = 0) { view.closeMenu() }
        verify(exactly = 0) { view.openMenu() }
    }

    @Test
    fun onForwardActionShouldForwardAction() {
        presenter.onForwardClicked()

        verify(exactly = 1) { listener.OnMessageOptionsListener(MessageAction.FORWARD) }
        verify(exactly = 0) { view.closeMenu() }
        verify(exactly = 0) { view.openMenu() }
    }
}