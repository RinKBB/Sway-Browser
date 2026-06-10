package com.example.update.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log

class UpdateInstallReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "UpdateInstallReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Unknown error"

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                Log.d(TAG, "Installation pending user action")
                // User action required - installation intent will be shown
                val userAction = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                userAction?.let { context.startActivity(it) }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Log.d(TAG, "Installation successful")
                // Installation completed successfully
                showNotification(context, "Обновление установлено", "Приложение успешно обновлено")
            }
            PackageInstaller.STATUS_FAILURE -> {
                Log.e(TAG, "Installation failed: $message")
                showNotification(context, "Ошибка обновления", message)
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                Log.e(TAG, "Installation aborted")
                showNotification(context, "Обновление отменено", "Пользователь отменил установку")
            }
            PackageInstaller.STATUS_FAILURE_BLOCKED -> {
                Log.e(TAG, "Installation blocked")
                showNotification(context, "Ошибка обновления", "Установка заблокирована системой")
            }
            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                Log.e(TAG, "Installation conflict")
                showNotification(context, "Ошибка обновления", "Конфликт с существующей версией")
            }
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> {
                Log.e(TAG, "Installation incompatible")
                showNotification(context, "Ошибка обновления", "Несовместимое обновление")
            }
            PackageInstaller.STATUS_FAILURE_INVALID -> {
                Log.e(TAG, "Installation invalid")
                showNotification(context, "Ошибка обновления", "Некорректный файл обновления")
            }
            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                Log.e(TAG, "Installation storage error")
                showNotification(context, "Ошибка обновления", "Недостаточно места на диске")
            }
            else -> {
                Log.w(TAG, "Unknown installation status: $status")
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        // Implement notification display here
        Log.d(TAG, "Notification: $title - $message")
    }
}
