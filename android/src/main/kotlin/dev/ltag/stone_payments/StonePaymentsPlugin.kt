package dev.ltag.stone_payments

import android.app.Activity
import android.content.Context
import androidx.annotation.NonNull
import dev.ltag.stone_payments.usecases.ActivateUsecase
import dev.ltag.stone_payments.usecases.PaymentUsecase
import dev.ltag.stone_payments.usecases.PrinterUsecase
import stone.utils.keys.StoneKeyType
import stone.application.StoneStart

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import stone.database.transaction.TransactionObject
import io.flutter.plugin.common.MethodChannel.Result as Res

/** StonePaymentsPlugin */
class StonePaymentsPlugin : FlutterPlugin, MethodCallHandler, Activity() {
    private lateinit var channel: MethodChannel
    var transactionObject = TransactionObject()
    var context: Context = this;
    private lateinit var flutterEngine: FlutterEngine

    companion object {
        var flutterBinaryMessenger: BinaryMessenger? = null
    }


    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        flutterEngine = flutterPluginBinding.flutterEngine
        flutterBinaryMessenger = flutterPluginBinding.binaryMessenger;
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "stone_payments")
        channel.setMethodCallHandler(this)

    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Res) {
        val activateUsecase: ActivateUsecase? = ActivateUsecase(context)
        val paymentUsecase: PaymentUsecase? = PaymentUsecase(this,flutterEngine)
        val printerUsecase: PrinterUsecase? = PrinterUsecase(this,)

        when (call.method) {
            "activateStone" -> {
                try {
                    activateUsecase!!.doActivate(
                        call.argument("appName")!!,
                        call.argument("stoneCode")!!,
                        call.argument("qrCode_Auth")!!,
                        call.argument("qrCode_ProviderId")!!,
                    ) { resp ->
                        when (resp) {
                            is Result.Success<Boolean> -> result.success(
                                "Ativado"
                            )
                            else -> result.error("Error", resp.toString(), resp.toString())
                        }
                    }
                } catch (e: Exception) {
                    result.error("UNAVAILABLE", "Cannot Activate", e.toString())
                }
            }
            "payment" -> {
                try {
                    paymentUsecase!!.doPayment(
                        call.argument("value")!!,
                        call.argument("typeTransaction")!!,
                        call.argument("installment")!!,
                        call.argument("printReceipt"),
                    ) { resp ->
                        when (resp) {
                            is Result.Success<Boolean> -> result.success(
                                "Pagamento Finalizado"
                            )
                            is Result.Error -> result.error("Error", resp.toString(), null)
                        else -> result.error("UNKNOWN_ERROR", "Erro desconhecido", null)
                        }
                    }
                } catch (e: Exception) {
                    result.error("UNAVAILABLE", "Cannot Activate", e.toString())
                }
            }
            "printFile" -> {
                try {
                    printerUsecase!!.printFile(
                        call.argument("imgBase64")!!,
                    ) { resp ->
                        when (resp) {
                            is Result.Success<Boolean> -> result.success(
                                "Impresso"
                            )
                            else -> result.error("Error", resp.toString(), resp.toString())
                        }
                    }
                } catch (e: Exception) {
                    result.error("UNAVAILABLE", "Cannot Activate", e.toString())
                }
            }
            "print" -> {
                try {
                    printerUsecase!!.print(
                        call.argument("items")!!,
                    ) { resp ->
                        when (resp) {
                            is Result.Success<Boolean> -> result.success(
                                "Impresso"
                            )
                            else -> result.error("Error", resp.toString(), resp.toString())
                        }
                    }
                } catch (e: Exception) {
                    result.error("UNAVAILABLE", "Cannot Activate", e.toString())
                }
            }
            "printReceipt" -> {
                try {
                    printerUsecase!!.printReceipt(
                        call.argument("type")!!,
                    ) { resp ->
                        when (resp) {
                            is Result.Success<Boolean> -> result.success(
                                "Via Impressa"
                            )
                            else -> result.error("Error", resp.toString(), resp.toString())
                        }
                    }
                } catch (e: Exception) {
                    result.error("UNAVAILABLE", "Cannot Activate", e.toString())
                }
            }
//          "cancel-payment" -> {
//              try {
//                  payment!!.cancel { resp ->
//                      when (resp) {
//                          is Result.Success<*> -> result.success(resp.data.toString())
//                          else -> result.error("Error", resp.toString(), resp.toString())
//                      }
//                  }
//              } catch (e: Exception) {
//                  result.error("UNAVAILABLE", "Cannot cancel", e.toString())
//              }
//          }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}