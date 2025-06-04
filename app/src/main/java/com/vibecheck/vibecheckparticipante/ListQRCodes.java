package com.vibecheck.vibecheckparticipante;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibecheck.vibecheckparticipante.adapters.QRCodeAdapter;
import com.vibecheck.vibecheckparticipante.models.QRCodeDisplayItem;
import com.vibecheck.vibecheckparticipante.network.ApiService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class ListQRCodes extends AppCompatActivity { // Nome da classe com 's'

    private ListView qrCodeListView;
    private ApiService apiService;
    private static final String TAG = "ListQRCodes"; // TAG com 's'
    private static final String API_BASE_URL = "https://3e46-179-119-53-133.ngrok-free.app/api/qrcode/";
    private static final String PARTICIPANT_ID_KEY = "id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_list_qrcodes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        qrCodeListView = findViewById(R.id.listViewQRCodes);

        Executor mainThreadExecutor = new Executor() {
            private final Handler handler = new Handler(Looper.getMainLooper());
            @Override
            public void execute(Runnable command) {
                handler.post(command);
            }
        };
        apiService = new ApiService(mainThreadExecutor);

        loadQRCodes();

        qrCodeListView.setOnItemClickListener((parent, view, position, id) -> {
            QRCodeDisplayItem selectedQRCode = (QRCodeDisplayItem) parent.getItemAtPosition(position);
            if (selectedQRCode != null && selectedQRCode.getQrCodeBase64() != null) {
                Intent intent = new Intent(ListQRCodes.this, FullScreenQRCode.class);
                intent.putExtra("qr_code_base64", selectedQRCode.getQrCodeBase64());
                startActivity(intent);
            }
        });
    }

    private void loadQRCodes() {
        SharedPreferences sharedPref = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        long participantIdLong = sharedPref.getLong(PARTICIPANT_ID_KEY, -1L);
        int participantId = (int) participantIdLong;

        if (participantId != -1) {
            String url = API_BASE_URL + participantId + "/participant";
            Log.d(TAG, "Fetching QRCodes from: " + url);

            apiService.get(url, new ApiService.ApiResponseCallback() {
                @Override
                public void onSuccess(String responseBody) {
                    Log.d(TAG, "API Response: " + responseBody);
                    ObjectMapper objectMapper = new ObjectMapper();
                    List<QRCodeDisplayItem> displayItems = new ArrayList<>();

                    try {
                        JsonNode rootNode = objectMapper.readTree(responseBody);
                        if (rootNode.isArray()) {
                            for (JsonNode qrCodeNode : rootNode) {
                                String qrCodeBase64 = qrCodeNode.path("qr_code_base64").asText("");
                                JsonNode registrationNode = qrCodeNode.path("registration");
                                JsonNode eventNode = registrationNode.path("event");

                                String eventName = eventNode.path("name").asText("Nome indisponível");
                                String eventDate = eventNode.path("date").asText("Data indisponível");
                                String eventLocation = ""; // Inicialize como vazio

                                // Extrair o endereço completo do evento (opcional, se quiser detalhar mais o local)
                                JsonNode eventAddressNode = eventNode.path("event_address");
                                if (eventAddressNode.isObject()) {
                                    String street = eventAddressNode.path("street").asText("");
                                    String number = eventAddressNode.path("number").asText("");
                                    String neighborhood = eventAddressNode.path("neighborhood").asText("");
                                    String city = eventAddressNode.path("city").asText("");
                                    String state = eventAddressNode.path("state").asText("");
                                    eventLocation = street + ", " + number + " - " + neighborhood + ", " + city + " - " + state;
                                    if (eventLocation.trim().isEmpty() || eventLocation.equals(",  - ,  - ")) {
                                        eventLocation = "Local indisponível";
                                    }
                                } else {
                                    eventLocation = eventNode.path("event_address_id").asText("Local indisponível"); // Fallback
                                }


                                // EXTRAIR NOME DO ORGANIZADOR AQUI
                                String organizerName = "Organizador indisponível";
                                JsonNode organizerNode = eventNode.path("organizer");
                                if (organizerNode.isObject()) {
                                    organizerName = organizerNode.path("full_name").asText("Organizador indisponível");
                                }


                                // Passar o nome do organizador para o construtor do QRCodeDisplayItem
                                displayItems.add(new QRCodeDisplayItem(eventName, eventDate, eventLocation, organizerName, qrCodeBase64));
                            }
                        }

                        if (!displayItems.isEmpty()) {
                            QRCodeAdapter adapter = new QRCodeAdapter(ListQRCodes.this, displayItems);
                            qrCodeListView.setAdapter(adapter);
                        } else {
                            Toast.makeText(ListQRCodes.this, "Nenhum QRCode encontrado para este participante.", Toast.LENGTH_LONG).show();
                        }

                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing JSON with Jackson (JsonNode): " + e.getMessage());
                        Toast.makeText(ListQRCodes.this, "Erro ao processar dados de QR Codes.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onError(int statusCode, String errorMessage) {
                    Log.e(TAG, "API Error: " + statusCode + " - " + errorMessage);
                    Toast.makeText(ListQRCodes.this, "Erro ao carregar QRCodes: " + errorMessage, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(IOException e) {
                    Log.e(TAG, "API Failure: " + e.getMessage());
                    Toast.makeText(ListQRCodes.this, "Erro de rede ao carregar QRCodes. Verifique sua conexão.", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(ListQRCodes.this, "ID do participante não encontrado. Por favor, faça login novamente.", Toast.LENGTH_LONG).show();
        }
    }
}