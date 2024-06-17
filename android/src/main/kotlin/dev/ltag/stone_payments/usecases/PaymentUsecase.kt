package dev.ltag.stone_payments.usecases

import android.util.Base64
import android.app.AlertDialog
import android.graphics.Bitmap
import android.widget.ImageView
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import br.com.stone.posandroid.providers.PosPrintReceiptProvider
import br.com.stone.posandroid.providers.PosTransactionProvider
import dev.ltag.stone_payments.Result
import dev.ltag.stone_payments.StonePaymentsPlugin
import io.flutter.plugin.common.MethodChannel
import io.flutter.embedding.engine.FlutterEngine
import stone.application.enums.*
import stone.application.interfaces.StoneActionCallback
import stone.application.interfaces.StoneCallbackInterface
import stone.providers.BaseTransactionProvider;
import stone.database.transaction.TransactionObject
import stone.utils.Stone
import java.io.ByteArrayOutputStream

class PaymentUsecase(
    private val stonePayments: StonePaymentsPlugin,
    private val flutterEngine: FlutterEngine
) {
    private val context = stonePayments.context;


    fun doPayment(
        value: Double,
        type: Int,
        installment: Int,
        print: Boolean?,
        callback: (Result<Boolean>) -> Unit,
    ) {
        try {
            stonePayments.transactionObject = TransactionObject()

            val transactionObject = stonePayments.transactionObject

            transactionObject.instalmentTransaction =
                InstalmentTransactionEnum.getAt(installment - 1);
            transactionObject.typeOfTransaction =
                if (type == 1) TypeOfTransactionEnum.CREDIT 
                else if (type == 2) TypeOfTransactionEnum.PIX 
                else TypeOfTransactionEnum.DEBIT;
            
            transactionObject.isCapture = true;
            val newValue: Int = (value * 100).toInt();
            transactionObject.amount = newValue.toString();

            val provider = PosTransactionProvider(
                context,
                transactionObject,
                Stone.getUserModel(0),
            )

            provider.setConnectionCallback(object : StoneActionCallback {

                override fun onSuccess() {

                    when (val status = provider.transactionStatus) {
                        TransactionStatusEnum.APPROVED -> {


                            Log.d("RESULT", "SUCESSO")
                            if (print == true) {
                                val posPrintReceiptProvider =
                                    PosPrintReceiptProvider(
                                        context, transactionObject,
                                        ReceiptType.MERCHANT,
                                    );

                                posPrintReceiptProvider.connectionCallback = object :
                                    StoneCallbackInterface {

                                    override fun onSuccess() {

                                        Log.d("SUCCESS", transactionObject.toString())
                                    }

                                    override fun onError() {
                                        val e = "Erro ao imprimir"
                                        Log.d("ERRORPRINT", transactionObject.toString())

                                    }
                                }

                                posPrintReceiptProvider.execute()

                            }
                            sendAMessage("APPROVED")

                            callback(Result.Success(true))
                        }
                        TransactionStatusEnum.DECLINED -> {
                            val message = provider.messageFromAuthorize
                            sendAMessage(message ?: "DECLINED")
                            callback(Result.Success(false))
                        }
                        TransactionStatusEnum.REJECTED -> {
                            val message = provider.messageFromAuthorize
                            sendAMessage(message ?: "REJECTED")
                            callback(Result.Success(false))
                        }
                        else -> {
                            val message = provider.messageFromAuthorize
                            sendAMessage(message ?: status.name)
                        }
                    }

                }

                override fun onError() {

                    Log.d("RESULT", "ERROR");

                    sendAMessage(provider.transactionStatus?.name ?: "ERROR")

                    callback(Result.Error(Exception("ERROR")));
                }

                override fun onStatusChanged(p0: Action?) {
                    p0?.let { action -> 
                        if (action.name == "TRANSACTION_WAITING_QRCODE_SCAN") {
                            val qrCodeBitmap = transactionObject.getQRCode()
                            if(qrCodeBitmap != null){

                                sendQRCode(qrCodeBitmap)
                                sendAMessage(action.name)
                            }
                            else {
                                sendAMessage("TECHNICAL_ERROR")
                            }
                        } else {
                           sendAMessage(action.name)
                       }
                    }
                }
            })

            provider.execute()


        } catch (e: Exception) {
            Log.d("ERROR", e.toString())
            callback(Result.Error(e));
        }

    }

    private fun sendAMessage(message: String) {
        Handler(Looper.getMainLooper()).post {
            val channel = MethodChannel(
                StonePaymentsPlugin.flutterBinaryMessenger!!,
                "stone_payments",
            )
            channel.invokeMethod("message", message)
        }
    }

    private fun sendQRCode(qrCodeBitmap: Bitmap) {
        Handler(Looper.getMainLooper()).post {
        val channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "stone_payments");
        val byteArrayOutputStream = ByteArrayOutputStream()
        qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()

        channel.invokeMethod("sendQRCode", byteArray)
        }
    }

}