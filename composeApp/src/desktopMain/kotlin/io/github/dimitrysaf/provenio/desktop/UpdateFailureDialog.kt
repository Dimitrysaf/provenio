package io.github.dimitrysaf.provenio.desktop

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.WString
import com.sun.jna.ptr.IntByReference
import io.github.dimitrysaf.provenio.core.build.AppVersionConfig
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.update_failed_details_hide
import provenio.composeapp.generated.resources.update_failed_details_show
import provenio.composeapp.generated.resources.update_failed_message
import provenio.composeapp.generated.resources.update_failed_title

internal const val UpdateFailedArgument = "--update-failed="
internal const val UpdateFileArgument = "--update-file="

internal object UpdateFailureDialog {
    fun showIfRequested(args: Array<String>) {
        val code = args.firstOrNull { it.startsWith(UpdateFailedArgument) }
            ?.removePrefix(UpdateFailedArgument)
            ?.takeIf { it.isNotBlank() }
            ?: return
        val installer = args.firstOrNull { it.startsWith(UpdateFileArgument) }?.removePrefix(UpdateFileArgument)
        runCatching { show(code, installer) }
    }

    private fun show(code: String, installer: String?) {
        val title = runBlocking { getString(Res.string.update_failed_title) }
        val message = runBlocking { getString(Res.string.update_failed_message, AppVersionConfig.VERSION_NAME) }
        val details = buildString {
            appendLine("Installer result: ${describe(code)}")
            appendLine("Installed version: ${AppVersionConfig.VERSION_NAME}")
            installer?.let { appendLine("Installer: $it") }
            System.getProperty("jpackage.app-path")?.let { appendLine("App: $it") }
            append("Windows: ${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})")
        }
        val shown = runCatching {
            val config = TaskDialogConfig().apply {
                cbSize = size()
                dwFlags = TdfAllowDialogCancellation or TdfSizeToContent
                dwCommonButtons = TdcbfOkButton
                pszWindowTitle = WString("Provenio")
                pszMainIcon = TdErrorIcon
                pszMainInstruction = WString(title)
                pszContent = WString(message)
                pszExpandedInformation = WString(details)
                pszCollapsedControlText = WString(runBlocking { getString(Res.string.update_failed_details_show) })
                pszExpandedControlText = WString(runBlocking { getString(Res.string.update_failed_details_hide) })
            }
            comctl32.TaskDialogIndirect(config, IntByReference(), null, null) == 0
        }.getOrDefault(false)
        if (!shown) {
            user32.MessageBoxW(null, WString("$title\n\n$message\n\n$details"), WString("Provenio"), MbOk or MbIconError)
        }
    }

    private fun describe(code: String): String = when (code) {
        "setup" -> "setup stopped before it finished"
        else -> "exit code $code"
    }

    private val comctl32: Comctl32 by lazy { Native.load("comctl32", Comctl32::class.java) }

    private val user32: User32 by lazy { Native.load("user32", User32::class.java) }

    @Suppress("FunctionName")
    private interface Comctl32 : Library {
        fun TaskDialogIndirect(
            config: TaskDialogConfig,
            button: IntByReference?,
            radioButton: IntByReference?,
            verificationChecked: IntByReference?,
        ): Int
    }

    @Suppress("FunctionName")
    private interface User32 : Library {
        fun MessageBoxW(hwnd: Pointer?, text: WString, caption: WString, type: Int): Int
    }

    @Suppress("PropertyName")
    @Structure.FieldOrder(
        "cbSize", "hwndParent", "hInstance", "dwFlags", "dwCommonButtons", "pszWindowTitle",
        "pszMainIcon", "pszMainInstruction", "pszContent", "cButtons", "pButtons", "nDefaultButton",
        "cRadioButtons", "pRadioButtons", "nDefaultRadioButton", "pszVerificationText",
        "pszExpandedInformation", "pszExpandedControlText", "pszCollapsedControlText", "pszFooterIcon",
        "pszFooter", "pfCallback", "lpCallbackData", "cxWidth",
    )
    class TaskDialogConfig : Structure(ALIGN_NONE) {
        @JvmField var cbSize: Int = 0
        @JvmField var hwndParent: Pointer? = null
        @JvmField var hInstance: Pointer? = null
        @JvmField var dwFlags: Int = 0
        @JvmField var dwCommonButtons: Int = 0
        @JvmField var pszWindowTitle: WString? = null
        @JvmField var pszMainIcon: Pointer? = null
        @JvmField var pszMainInstruction: WString? = null
        @JvmField var pszContent: WString? = null
        @JvmField var cButtons: Int = 0
        @JvmField var pButtons: Pointer? = null
        @JvmField var nDefaultButton: Int = 0
        @JvmField var cRadioButtons: Int = 0
        @JvmField var pRadioButtons: Pointer? = null
        @JvmField var nDefaultRadioButton: Int = 0
        @JvmField var pszVerificationText: WString? = null
        @JvmField var pszExpandedInformation: WString? = null
        @JvmField var pszExpandedControlText: WString? = null
        @JvmField var pszCollapsedControlText: WString? = null
        @JvmField var pszFooterIcon: Pointer? = null
        @JvmField var pszFooter: WString? = null
        @JvmField var pfCallback: Pointer? = null
        @JvmField var lpCallbackData: Pointer? = null
        @JvmField var cxWidth: Int = 0
    }

    private const val TdfAllowDialogCancellation = 0x0008
    private const val TdfSizeToContent = 0x01000000
    private const val TdcbfOkButton = 0x0001
    private val TdErrorIcon = Pointer(0xFFFEL)
    private const val MbOk = 0x0000
    private const val MbIconError = 0x0010
}
