package com.vibecheck.vibecheckparticipante;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class FullScreenQRCode extends AppCompatActivity {

    private ImageView fullScreenQRCodeImageView;
    private static final String TAG = "FullScreenQRCode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Habilita o modo EdgeToEdge para a interface
        setContentView(R.layout.activity_full_screen_qrcode); // Define o layout da Activity

        // Aplica os insets do sistema (barras de status e navegação) para evitar que o conteúdo fique por baixo delas
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fullScreenQRCodeImageView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fullScreenQRCodeImageView = findViewById(R.id.fullScreenQRCodeImageView);

        // Obter a string Base64 do Intent
        String qrCodeBase64 = getIntent().getStringExtra("qr_code_base64");

        if (qrCodeBase64 != null && !qrCodeBase64.isEmpty()) {
            // Remover o prefixo "data:image/png;base64," se ele existir
            // Isso é crucial, pois Base64.decode() espera apenas os dados codificados
            if (qrCodeBase64.startsWith("data:image/png;base64,")) {
                qrCodeBase64 = qrCodeBase64.substring("data:image/png;base64,".length());
            }

            try {
                // Decodificar a string Base64 para um array de bytes
                byte[] decodedString = Base64.decode(qrCodeBase64, Base64.DEFAULT);
                // Converter o array de bytes em um Bitmap
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                // Definir o Bitmap no ImageView
                fullScreenQRCodeImageView.setImageBitmap(decodedByte);
            } catch (IllegalArgumentException e) {
                // Capturar erros se a string Base64 for inválida
                Log.e(TAG, "Invalid Base64 string: " + e.getMessage());
                Toast.makeText(this, "Erro ao decodificar QR Code. O formato da imagem pode estar incorreto.", Toast.LENGTH_LONG).show();
            }
        } else {
            // Se a string Base64 for nula ou vazia, informar o usuário e fechar a Activity
            Toast.makeText(this, "Nenhum QR Code para exibir.", Toast.LENGTH_LONG).show();
            finish(); // Fecha a Activity
        }
    }
}