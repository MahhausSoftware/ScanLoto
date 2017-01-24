package com.mahhaus.scanloto;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import java.util.List;

import at.nineyards.anyline.camera.AnylineViewConfig;
import at.nineyards.anyline.modules.ocr.AnylineOcrConfig;
import at.nineyards.anyline.modules.ocr.AnylineOcrError;
import at.nineyards.anyline.modules.ocr.AnylineOcrListener;
import at.nineyards.anyline.modules.ocr.AnylineOcrResult;
import at.nineyards.anyline.modules.ocr.AnylineOcrScanView;

public class ScanActivity extends AppCompatActivity {

    private static final String LANG_TRAINNING = "eng_no_dict";
    private static final String NUM1_TRAINNING = "monnum";
    private static final String NUM2_TRAINNING = "incnum";
    private AnylineOcrScanView scanView;
    private String lic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_scan);

        lic = getString(R.string.anyline_license_key);
        scanView  =  (AnylineOcrScanView)  findViewById( R.id.scan_view );

        defineTrainingData();
        defineScanParameters();
        defineCameraParameters();
        initScanOCR();
    }

    private void defineTrainingData() {
        // Cópias dadas trainingdata-file para um local onde o núcleo pode acessá-lo.
        // Isso DEVE ser chamado para cada arquivo de dados trainingd que é usado (antes de startScanning () é chamado).
        // O arquivo deve estar localizado diretamente no diretório de "assets" (ou em tessdata / mas há outras pastas são permitidos)

        scanView.copyTrainedData( LANG_TRAINNING +".traineddata" , "855d8088928ee058257f64ccac2837eb" );
        scanView.copyTrainedData( NUM1_TRAINNING +".traineddata" , "855d8088928ee058257f64ccac2837eb" );
        scanView.copyTrainedData( NUM2_TRAINNING +".traineddata" , "855d8088928ee058257f64ccac2837eb" );
    }

    private void initScanOCR() {
        scanView.initAnyline(lic, new AnylineOcrListener() {
            @Override
            public void onReport(String identifier, Object value) {
                // Chamado com valores interessantes, que surgem durante o processamento.
                // Alguns valores eventualmente relatados:
                //
                // $ brilho - o brilho da região do centro do recorte como um valor flutuante
                // $ confiança - a confiança, um valor inteiro entre 0 e 100
                // $ thresholdedImage - a imagem atual transformada em preto e branco

            }

            @Override
            public boolean onTextOutlineDetected(List<PointF> list) {
                // Chamado quando o esboço de um possível texto é detectado.
                // Se for retornado false, o contorno é desenhado automaticamente.
                return false;
            }

            @Override
            public void onResult(AnylineOcrResult result) {
                // Chamado quando um resultado válido é encontrado
                // (confiança mínima é ultrapassado e validação com regex foi ok)
                if (result.getText() != null && !result.getText().isEmpty()) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("resultText", result.getText().trim() );

                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                } else {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("resultText", "" );
                    setResult(Activity.RESULT_CANCELED, resultIntent);
                    finish();
                }
            }

            @Override
            public void onAbortRun(AnylineOcrError code, String message) {
                // É chamado quando nenhum resultado foi encontrado para a imagem actual.
                // Por exemplo, se nenhum texto foi encontrado ou o resultado não é válido.
            }
        });
    }


    private void defineCameraParameters() {
        // Configurar o ponto de vista (entalhe, a resolução da câmera, etc.)
        // através de JSON (também pode ser feito em xml no layout)
        scanView.setConfig ( new AnylineViewConfig( this ,  "scan_view_config.json" ));
    }

    private void defineScanParameters() {
        AnylineOcrConfig anylineOcrConfig  =  new  AnylineOcrConfig();

        anylineOcrConfig.setScanMode ( AnylineOcrConfig.ScanMode.LINE );

        anylineOcrConfig.setTesseractLanguages (LANG_TRAINNING, NUM1_TRAINNING, NUM2_TRAINNING);

        //anylineOcrConfig.setCharWhitelist ( "[]0123456789" );

        //anylineOcrConfig.setMinCharHeight ( 20 );
        //anylineOcrConfig.setMaxCharHeight ( 60 );

        anylineOcrConfig.setMinConfidence ( 15 );

        //anylineOcrConfig.setValidationRegex ( "^[0-9]{2,17}$" );
        anylineOcrConfig.setValidationRegex ( "([0-9\\[\\]])" );

        //anylineOcrConfig.setCharCountX ( 29 );
        //anylineOcrConfig.setCharCountY ( 9 );

        //anylineOcrConfig.setCharPaddingXFactor ( 2 );

        //anylineOcrConfig.setCharPaddingYFactor ( 0.2 );

        anylineOcrConfig.setIsBrightTextOnDark ( false );

        scanView.setAnylineOcrConfig ( anylineOcrConfig );
    }

    @Override
    protected void onResume() {
        super.onResume();

        // usar um postdelay para "iniciar a digitalização" para melhorar a experiência do usuário:
        // caso contrário o resultado seria mostrado antes de posicionar a varredura
        scanView .postDelayed ( new  Runnable ()  {
            @Override
            public  void  run()  {
                if(! isFinishing ())  {
                    scanView.startScanning ();
                }
            }
        },  1000 );
    }

    @Override
    protected void onPause() {
        super.onPause();
        //stop the scanning
        scanView.cancelScanning();
        //release the camera (must be called in onPause, because there are situations where
        // it cannot be auto-detected that the camera should be released)
        scanView.releaseCameraInBackground();
    }


}
