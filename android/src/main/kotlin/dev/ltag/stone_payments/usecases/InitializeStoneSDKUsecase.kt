package dev.ltag.stone_payments.usecases

import android.content.Context
import stone.utils.keys.StoneKeyType
import stone.application.StoneStart
import io.flutter.plugin.common.MethodChannel
import dev.ltag.stone_payments.Result

class InitializeStoneSDKUsecase(private val context: Context) {
    fun initialize(key1: String, key2: String, callback: (Result<Boolean>) -> Unit) {
        try {
            val stoneKeys = mapOf(
                StoneKeyType.QRCODE_AUTHORIZATION to key1,
                StoneKeyType.QRCODE_PROVIDERID to key2
            )
            StoneStart.init(context, stoneKeys)
            callback(Result.Success(true))
        } catch (e: Exception) {
            callback(Result.Error(Exception("Erro ao Inicializar")))
        }
    }
}