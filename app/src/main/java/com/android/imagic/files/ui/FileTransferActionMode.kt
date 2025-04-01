package com.android.imagic.files.ui

import androidx.core.view.isVisible

class FileTransferActionMode(
    private val transferRecyclerLayout: FileTransferFloatingLayout,
    private val floatingButton: FloatingButtonView
) {
    fun show() {
        if (floatingButton.isVisible || transferRecyclerLayout.isVisible) {
            return
        }
        showTransferRecyclerLayout()
    }

    fun hide() {
        hideFloatingButton()
        hideTransferRecyclerLayout()
    }

    fun switchView(fromFloatingButton: Boolean) {
        if (fromFloatingButton) {
            hideFloatingButton()
            showTransferRecyclerLayout()
        } else {
            hideTransferRecyclerLayout()
            showFloatingButton()
        }
    }

    private fun showFloatingButton() {
        if (floatingButton.isVisible) return

        floatingButton.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .withStartAction { floatingButton.isVisible = true }
            .start()
    }

    private fun showTransferRecyclerLayout() {
        if (transferRecyclerLayout.isVisible) return

        transferRecyclerLayout.alpha = 0f
        transferRecyclerLayout.isVisible = true
        transferRecyclerLayout.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    private fun hideFloatingButton() {
        if (floatingButton.isVisible) {
            floatingButton.animate()
                .alpha(0f)
                .scaleX(0.5f)
                .scaleY(0.5f)
                .setDuration(200)
                .withEndAction { floatingButton.isVisible = false }
                .start()
        }
    }

    private fun hideTransferRecyclerLayout() {
        if (transferRecyclerLayout.isVisible) {
            transferRecyclerLayout.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction { transferRecyclerLayout.isVisible = false }
                .start()
        }
    }
}
