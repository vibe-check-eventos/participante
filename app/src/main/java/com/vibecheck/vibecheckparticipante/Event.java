package com.vibecheck.vibecheckparticipante;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.vibecheck.vibecheckparticipante.network.ApiService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Event extends AppCompatActivity {

    private EditText nomeEditText, dataEditText, localEditText, descricaoEditText;
    private Button participarButton;
    private int eventId;

    private Executor executor;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        executor = Executors.newSingleThreadExecutor();
        apiService = new ApiService(command -> runOnUiThread(command));

        nomeEditText = findViewById(R.id.nomeEvento);
        dataEditText = findViewById(R.id.dataEvento);
        localEditText = findViewById(R.id.localEvento);
        descricaoEditText = findViewById(R.id.descricaoEvento);
        participarButton = findViewById(R.id.participarButton);

        // Pegando os dados do Intent
        eventId = Integer.parseInt(getIntent().getStringExtra("id"));

        Log.d("EVENTO ID", Integer.toString(eventId));

        carregarDadosDoEvento(eventId);

        participarButton.setOnClickListener(view -> realizarInscricao());
    }

    private void carregarDadosDoEvento(int id) {
        String url = "https://3e46-179-119-53-133.ngrok-free.app/api/events/" + id;

        apiService.get(url, new ApiService.ApiResponseCallback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONObject json = new JSONObject(responseBody);

                    String nome = json.getString("name");
                    String descricao = json.getString("description");
                    String data = json.getString("date"); // ou outro campo de data, se tiver um mais adequado

                    JSONObject endereco = json.getJSONObject("event_address");
                    String enderecoCompleto = endereco.getString("street") ;

                    runOnUiThread(() -> {
                        nomeEditText.setText(nome);
                        dataEditText.setText(convertYyyyMmDdHhSsToDdMmYyyyHhMm(data));
                        localEditText.setText(enderecoCompleto);
                        descricaoEditText.setText(descricao);
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(Event.this, "Erro ao ler dados do evento!", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onError(int statusCode, String errorMessage) {
                runOnUiThread(() -> Toast.makeText(Event.this, "Erro: " + errorMessage, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onFailure(IOException e) {
                runOnUiThread(() -> Toast.makeText(Event.this, "Falha de rede!", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void realizarInscricao() {
        // Pegando o participant_id do SharedPreferences
        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
        long participantId = prefs.getLong("id", -1);

        Log.d("SUBSCRIPTION", Long.toString(participantId));

        if (participantId == -1 || eventId == -1) {
            Toast.makeText(this, "Erro ao obter IDs!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("participant_id", participantId);
            json.put("event_id", eventId);

            apiService.post("https://3e46-179-119-53-133.ngrok-free.app/api/registrations",
                    json.toString(),
                    new ApiService.ApiResponseCallback() {
                        @Override
                        public void onSuccess(String responseBody) {
                            try {
                                JSONObject responseJson = new JSONObject(responseBody);
                                int registrationId = responseJson.getInt("id");
                                gerarQRCodeEEnviar(registrationId, responseJson.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(int statusCode, String errorMessage) {
                            runOnUiThread(() -> {
                                Toast.makeText(Event.this, "Erro: " + errorMessage, Toast.LENGTH_SHORT).show();
                                Log.e("ERRO ON LOADING EVENT DATA", "Erro: " + errorMessage);
                            });
                        }

                        @Override
                        public void onFailure(IOException e) {
                            runOnUiThread(() ->
                                    Toast.makeText(Event.this, "Falha de rede!", Toast.LENGTH_SHORT).show()
                            );
                        }
                    });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void gerarQRCodeEEnviar(int registrationId, String payloadRegistration) {
        executor.execute(() -> {
            try {
                QRCodeWriter writer = new QRCodeWriter();
                Bitmap bitmap = toBitmap(writer.encode(payloadRegistration, BarcodeFormat.QR_CODE, 300, 300));

                String base64 = bitmapToBase64(bitmap);

                Log.d("BASE64", base64);

                JSONObject json = new JSONObject();
                json.put("registration_id", registrationId);
                json.put("qr_code_base64", "data:image/png;base64," + base64);

                apiService.post("https://3e46-179-119-53-133.ngrok-free.app/api/qrcode",
                        json.toString(),
                        new ApiService.ApiResponseCallback() {
                            @Override
                            public void onSuccess(String responseBody) {
                                Toast.makeText(Event.this, "Incrição realizada com sucesso!", Toast.LENGTH_SHORT).show();
                                Log.d("NEW QR CODE REGISTERED", responseBody);
                                finish();
                            }

                            @Override
                            public void onError(int statusCode, String errorMessage) {
                                Toast.makeText(Event.this, "Incrição falhou.", Toast.LENGTH_SHORT).show();
                                Log.d("ERROR QR CODE", errorMessage);
                                finish();
                            }

                            @Override
                            public void onFailure(IOException e) {
                                Toast.makeText(Event.this, "Rede falhou.", Toast.LENGTH_SHORT).show();
                                Log.d("ERROR NETWORK", e.getMessage());
                                finish();
                            }
                        });

            } catch (WriterException | JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private Bitmap toBitmap(com.google.zxing.common.BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bmp.setPixel(x, y, matrix.get(x, y) ? android.graphics.Color.BLACK : android.graphics.Color.WHITE);
            }
        }
        return bmp;
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    /**
     * Converte uma string de data e hora do formato "yyyy-MM-dd HH:mm:ss"
     * para o formato "dd/MM/yyyy HH:mm".
     *
     * @param yyyyMmDdHhSs A string de data e hora no formato "yyyy-MM-dd HH:mm:ss".
     * @return A string de data e hora formatada como "dd/MM/yyyy HH:mm", ou null se o formato de entrada for inválido.
     */
    public static String convertYyyyMmDdHhSsToDdMmYyyyHhMm(String yyyyMmDdHhSs) {
        // 1. Defina o formato de entrada
        // Use "HH" para formato de 24 horas, "hh" para 12 horas com AM/PM
        SimpleDateFormat inputFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date date;
        try {
            // 2. Parse a String de entrada para um objeto Date
            date = inputFormatter.parse(yyyyMmDdHhSs);
        } catch (ParseException e) {
            // Lida com o erro se a string de entrada não corresponder ao formato esperado
            System.err.println("Erro ao parsear a data: " + yyyyMmDdHhSs + ". Formato esperado: yyyy-MM-dd HH:mm:ss");
            return null; // Retorna null ou lança uma exceção, dependendo da sua necessidade de tratamento de erro
        }

        // 3. Defina o formato de saída
        SimpleDateFormat outputFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        // 4. Formate o objeto Date para o String desejado
        return outputFormatter.format(date);
    }

}