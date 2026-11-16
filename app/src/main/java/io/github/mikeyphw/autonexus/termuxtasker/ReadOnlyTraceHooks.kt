package io.github.mikeyphw.autonexus.termuxtasker

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * TT-F0: pinned, read-only plugin-process observations.
 * The hooks NEVER call setResult/setThrowable or change Intent/Bundle/arguments.
 * Absence of a hook event is not proof that Tasker failed to dispatch an action.
 */
object ReadOnlyTraceHooks {
    private val armed = AtomicBoolean(false)
    private val installed = AtomicBoolean(false)
    private val count = AtomicInteger(0)
    private val sequence = AtomicLong(0)
    private const val START = "an_tt_start"
    private const val SEQUENCE = "an_tt_seq"

    fun arm(load: XC_LoadPackage.LoadPackageParam) {
        if (load.packageName != TraceProtocol.PLUGIN || !armed.compareAndSet(false, true)) return
        val process = when (load.processName) {
            TraceProtocol.PLUGIN -> TraceProtocol.Process.EDITOR
            "com.termux.tasker:background" -> TraceProtocol.Process.BACKGROUND
            else -> return
        }
        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java, "attach", Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = param.args.getOrNull(0) as? Context ?: return
                        if (!TrustedTarget.inspect(context).supported) {
                            emit(process, TraceProtocol.Phase.INCOMPATIBLE, TraceProtocol.Outcome.UNAVAILABLE)
                            return
                        }
                        if (!installed.compareAndSet(false, true)) return
                        emit(process, TraceProtocol.Phase.ATTACHED, TraceProtocol.Outcome.OBSERVED)
                        installPinnedHooks(load.classLoader, process)
                    }
                },
            )
        } catch (_: Throwable) {
            emit(process, TraceProtocol.Phase.HOOK_UNAVAILABLE, TraceProtocol.Outcome.UNAVAILABLE)
        }
    }

    private fun installPinnedHooks(loader: ClassLoader, process: TraceProtocol.Process) {
        if (process == TraceProtocol.Process.BACKGROUND) {
            safeInstall(process) {
                XposedHelpers.findAndHookMethod(
                    "com.termux.tasker.FireReceiver", loader,
                    "onReceive", Context::class.java, Intent::class.java,
                    trace(process, TraceProtocol.Phase.RECEIVER_ENTER, TraceProtocol.Phase.RECEIVER_RETURN) { param ->
                        if (param.hasThrowable()) TraceProtocol.exceptionKind(param.throwable)
                        else {
                            val receiver = param.thisObject as? BroadcastReceiver
                            if (receiver == null) TraceProtocol.Outcome.UNAVAILABLE
                            else try {
                                TraceProtocol.classifyOrderedResult(receiver.isOrderedBroadcast, receiver.resultCode)
                            } catch (_: RuntimeException) { TraceProtocol.Outcome.UNAVAILABLE }
                        }
                    },
                )
            }
            safeInstall(process) {
                XposedHelpers.findAndHookMethod(
                    "com.termux.tasker.utils.PluginUtils", loader,
                    "sendExecuteIntentToExecuteService", Context::class.java,
                    BroadcastReceiver::class.java, Intent::class.java, Intent::class.java,
                    Boolean::class.javaPrimitiveType,
                    trace(process, TraceProtocol.Phase.DISPATCH_ENTER, TraceProtocol.Phase.DISPATCH_RETURN),
                )
            }
        } else {
            safeInstall(process) {
                XposedHelpers.findAndHookMethod(
                    "com.termux.tasker.PluginResultsService", loader,
                    "onHandleIntent", Intent::class.java,
                    trace(process, TraceProtocol.Phase.RESULT_SERVICE_ENTER, TraceProtocol.Phase.RESULT_SERVICE_RETURN),
                )
            }
            safeInstall(process) {
                XposedHelpers.findAndHookMethod(
                    "com.termux.tasker.utils.PluginUtils", loader,
                    "sendPendingResultToPluginHostApp", Context::class.java, Intent::class.java,
                    trace(process, TraceProtocol.Phase.RESULT_HELPER_ENTER, TraceProtocol.Phase.RESULT_HELPER_RETURN),
                )
            }
            safeInstall(process) {
                XposedHelpers.findAndHookMethod(
                    "com.termux.tasker.utils.TaskerPlugin\$Setting", loader,
                    "signalFinish", Context::class.java, Intent::class.java, Int::class.javaPrimitiveType,
                    Bundle::class.java,
                    trace(process, TraceProtocol.Phase.CALLBACK_ENTER, TraceProtocol.Phase.CALLBACK_RETURN) { param ->
                        if (param.hasThrowable()) TraceProtocol.exceptionKind(param.throwable)
                        else if (param.result == true) TraceProtocol.Outcome.CALLBACK_SENT_LOCAL
                        else TraceProtocol.Outcome.CALLBACK_FALSE
                    },
                )
            }
            // The enhanced editor is isolated to the already signer/version-gated
            // normal plugin process. Its failure never blocks the stock editor.
            safeInstall(process) { EnhancedEditorHooks.install(loader) }
        }
    }

    private fun safeInstall(process: TraceProtocol.Process, operation: () -> Unit) {
        try {
            operation()
        } catch (_: Throwable) {
            emit(process, TraceProtocol.Phase.HOOK_UNAVAILABLE, TraceProtocol.Outcome.UNAVAILABLE)
        }
    }

    private fun trace(
        process: TraceProtocol.Process,
        before: TraceProtocol.Phase,
        after: TraceProtocol.Phase,
        result: (XC_MethodHook.MethodHookParam) -> TraceProtocol.Outcome = {
            TraceProtocol.exceptionKind(if (it.hasThrowable()) it.throwable else null)
        },
    ): XC_MethodHook = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val number = sequence.incrementAndGet()
            param.setObjectExtra(START, SystemClock.elapsedRealtime())
            param.setObjectExtra(SEQUENCE, number)
            emit(process, before, TraceProtocol.Outcome.OBSERVED, number)
        }

        override fun afterHookedMethod(param: MethodHookParam) {
            val number = (param.getObjectExtra(SEQUENCE) as? Long) ?: 0L
            val start = (param.getObjectExtra(START) as? Long) ?: SystemClock.elapsedRealtime()
            emit(process, after, result(param), number, SystemClock.elapsedRealtime() - start)
        }
    }

    private fun emit(process: TraceProtocol.Process, phase: TraceProtocol.Phase,
                     outcome: TraceProtocol.Outcome, id: Long = 0, elapsed: Long = 0) {
        if (count.incrementAndGet() > TraceProtocol.MAX_EVENTS_PER_PROCESS) return
        XposedBridge.log(TraceProtocol.event(process, phase, outcome, id, elapsed))
    }
}
