import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:stone_payments/enums/item_print_type_enum.dart';
import 'package:stone_payments/enums/type_owner_print_enum.dart';
import 'package:stone_payments/enums/type_transaction_enum.dart';
import 'package:stone_payments/models/item_print_model.dart';
import 'package:stone_payments/stone_payments.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final stonePaymentsPlugin = StonePayments();

  await stonePaymentsPlugin.activateStone(
    appName: 'stone_payments_example',
    stoneCode: '123456789',
    qrCode_Auth: "xxxxxxxxxxxxxxxxxxxxxxxxx",
    qrCode_ProviderId: "xxxxxxxxxxxxxxxxxxxxxx",
  );

  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final stonePaymentsPlugin = StonePayments();
  String text = 'Running';
  late StreamSubscription<String> messageSubscription;
  late StreamSubscription<Uint8List> qrCodeSubscription;
  Uint8List? qrCodeBytes;

  @override
  void initState() {
    super.initState();
    messageSubscription = stonePaymentsPlugin.onMessageListener((message) {
      if (mounted) {
        setState(() {
          text = message;
        });
      }
    });

    qrCodeSubscription = stonePaymentsPlugin.onQRCodeReceivedListener((bytes) {
      if (mounted) {
        setState(() {
          qrCodeBytes = bytes;
        });
      }
    });
  }

  @override
  void dispose() {
    messageSubscription.cancel();
    qrCodeSubscription.cancel();
    super.dispose();
  }

  Future<void> _makePayment() async {
    try {
      await stonePaymentsPlugin.payment(
        value: 5,
        typeTransaction: TypeTransactionEnum.pix,
      );
    } catch (e) {
      setState(() {
        text = "Falha no pagamento";
      });
    }
  }

  Future<void> _printItems() async {
    try {
      var byteData = await rootBundle.load('assets/flutter5786.png');
      var imgBase64 = base64Encode(byteData.buffer.asUint8List());

      var items = [
        const ItemPrintModel(
          type: ItemPrintTypeEnum.text,
          data: 'Teste Título',
        ),
        const ItemPrintModel(
          type: ItemPrintTypeEnum.text,
          data: 'Teste Subtítulo',
        ),
        ItemPrintModel(
          type: ItemPrintTypeEnum.image,
          data: imgBase64,
        ),
      ];

      await stonePaymentsPlugin.print(items);
    } catch (e) {
      setState(() {
        text = "Falha na impressão";
      });
    }
  }

  Future<void> _printReceipt(TypeOwnerPrintEnum type) async {
    try {
      await stonePaymentsPlugin.printReceipt(type);
    } catch (e) {
      setState(() {
        text = "Falha na impressão";
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: SizedBox(
          width: double.infinity,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Text(text),
              ElevatedButton(
                onPressed: _makePayment,
                child: const Text('Comprar R\$5,00'),
              ),
              if(text == "Aguardando a leitura do QRCode." && qrCodeBytes != null)
                Image.memory(qrCodeBytes!),
              ElevatedButton(
                onPressed: _printItems,
                child: const Text('Imprimir'),
              ),
              ElevatedButton(
                onPressed: () => _printReceipt(TypeOwnerPrintEnum.merchant),
                child: const Text('Imprimir Via Loja'),
              ),
              ElevatedButton(
                onPressed: () => _printReceipt(TypeOwnerPrintEnum.client),
                child: const Text('Imprimir Via Cliente'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
