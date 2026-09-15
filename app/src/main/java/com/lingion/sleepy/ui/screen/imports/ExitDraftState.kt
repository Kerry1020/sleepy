package com.lingion.sleepy.ui.screen.imports

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lingion.sleepy.R

/** The user's choice when leaving an import that may contain recoverable work. */
enum class ExitDraftChoice {
    Continue,
    KeepDraft,
    DeleteDraft,
}

/** Minimal state needed by the exit confirmation flow; persistence is deliberately outside it. */
data class ExitDraftState(
    val activeImport: Boolean = false,
    val confirmationVisible: Boolean = false,
)

sealed interface ExitDraftEvent {
    data object RequestExit : ExitDraftEvent
    data class Choose(val choice: ExitDraftChoice) : ExitDraftEvent
}

enum class ExitDraftOutcome {
    None,
    FinishDirectly,
    KeepDraft,
    DeleteDraft,
}

data class ExitDraftReduction(
    val state: ExitDraftState,
    val outcome: ExitDraftOutcome,
)

/**
 * Pure exit reducer. It does not save or delete anything; callers own those side effects.
 */
fun reduceExitDraftState(
    state: ExitDraftState,
    event: ExitDraftEvent,
): ExitDraftReduction = when (event) {
    ExitDraftEvent.RequestExit -> if (state.activeImport) {
        ExitDraftReduction(state.copy(confirmationVisible = true), ExitDraftOutcome.None)
    } else {
        ExitDraftReduction(state.copy(confirmationVisible = false), ExitDraftOutcome.FinishDirectly)
    }

    is ExitDraftEvent.Choose -> when (event.choice) {
        ExitDraftChoice.Continue ->
            ExitDraftReduction(state.copy(confirmationVisible = false), ExitDraftOutcome.None)
        ExitDraftChoice.KeepDraft ->
            ExitDraftReduction(state.copy(confirmationVisible = false), ExitDraftOutcome.KeepDraft)
        ExitDraftChoice.DeleteDraft ->
            ExitDraftReduction(state.copy(confirmationVisible = false), ExitDraftOutcome.DeleteDraft)
    }
}

/**
 * UI-only confirmation surface. Draft persistence is supplied by the caller for later integration.
 */
@Composable
fun ExitDraftConfirmationDialog(
    onContinue: () -> Unit,
    onKeepDraft: () -> Unit,
    onDeleteDraft: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onContinue,
        title = { Text(stringResource(R.string.jw_import_exit_title)) },
        text = { Text(stringResource(R.string.jw_import_exit_message)) },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(stringResource(R.string.jw_import_exit_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepDraft) {
                Text(stringResource(R.string.jw_import_exit_keep_draft))
            }
            TextButton(onClick = onDeleteDraft) {
                Text(stringResource(R.string.jw_import_exit_delete_draft))
            }
        },
    )
}
