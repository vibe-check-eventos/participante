package com.vibecheck.vibecheckparticipante;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibecheck.vibecheckparticipante.network.ApiService;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ListEvents extends AppCompatActivity {

    private ApiService apiService;
    private ListView lvEvents;
    // Chaves para o SimpleAdapter
    private ArrayList<String> dadosFormatados; // Lista para as strings formatadas
    private ArrayAdapter<String> meuAdapter;
    private TextView txtLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_list_events);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        this.apiService = new ApiService(ContextCompat.getMainExecutor(this));
        this.lvEvents = findViewById(R.id.lvEvents);
        this.txtLoading = findViewById(R.id.txtLoadingParticipants);

        loadEventsData();

        //ao clicar em um evento
        this.lvEvents.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                String eventoSelecionado = (String) adapterView.getItemAtPosition(i);

                String id = "";
                String nome = "";
                String capacidade = "";
                String endereco = "";
                String data = "";

                for (String linha : eventoSelecionado.split("\n")) {
                    if (linha.startsWith("ID:")) {
                        id = linha.substring(linha.indexOf(":") + 1).trim();
                    } else if (linha.startsWith("Nome:")) {
                        nome = linha.substring(linha.indexOf(":") + 1).trim();
                    } else if (linha.startsWith("Capacidade:")) {
                        capacidade = linha.substring(linha.indexOf(":") + 1).trim();
                    } else if (linha.startsWith("Endereço:")) {
                        endereco = linha.substring(linha.indexOf(":") + 1).trim();
                    } else if (linha.startsWith("Data:")) {
                        data = linha.substring(linha.indexOf(":") + 1).trim();
                    }
                }

                Intent in = new Intent(getApplicationContext(), Event.class);

                in.putExtra("id", id);
                in.putExtra("nome", nome);
                in.putExtra("capacidade", capacidade);
                in.putExtra("endereco", endereco);
                in.putExtra("data", data);

                startActivity(in);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEventsData();
    }

    private void loadEventsData() {

        String eventsUrl = "https://3e46-179-119-53-133.ngrok-free.app/api/events";

        ListView list = this.lvEvents;

        apiService.get(eventsUrl, new ApiService.ApiResponseCallback() {

            @Override
            public void onSuccess(String responseBody) {

                txtLoading.setVisibility(View.GONE);

                Log.d("List Events Body:", responseBody);

                if (dadosFormatados == null) {
                    dadosFormatados = new ArrayList<>();
                } else {
                    dadosFormatados.clear();
                }

                if (responseBody.isEmpty() || responseBody.equals("{\"message\":\"Nenhum evento encontrado.\"}")) {
                    ArrayList<String> noDataListView = new ArrayList<>();
                    noDataListView.add("Nenhum evento encontrado no momento.");
                    ArrayAdapter<String> listEvents = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, noDataListView);
                    list.setAdapter(listEvents);
                    return;
                }

                ObjectMapper objectMapper = new ObjectMapper();
                List<Map<String, Object>> rawEventsList = new ArrayList<>();

                try {
                    rawEventsList = objectMapper.readValue(responseBody, new TypeReference<List<Map<String, Object>>>() {});

                    Log.d("ListEvents", "Dados brutos carregados: " + rawEventsList.size());

                    for (Map<String, Object> eventMap : rawEventsList) {

                        Integer id = (Integer) eventMap.get("id");
                        String name = (String) eventMap.get("name");
                        Object capacityObj = eventMap.get("capacity");
                        String capacity = (capacityObj != null) ? String.valueOf(capacityObj) : "N/A";

                        String address = "Endereço Indisponível";
                        Object eventAddressObj = eventMap.get("event_address");
                        if (eventAddressObj instanceof Map) {
                            Map<String, Object> addressMap = (Map<String, Object>) eventAddressObj;
                            String street = (String) addressMap.get("street");
                            String number = (String) addressMap.get("number");
                            String city = (String) addressMap.get("city");
                            String state = (String) addressMap.get("state");

                            StringBuilder addressBuilder = new StringBuilder();
                            if (street != null && !street.isEmpty()) addressBuilder.append(street);
                            if (number != null && !number.isEmpty()) addressBuilder.append(", ").append(number);
                            if (city != null && !city.isEmpty()) addressBuilder.append(" - ").append(city);
                            if (state != null && !state.isEmpty()) addressBuilder.append("/").append(state);
                            address = addressBuilder.toString();
                            if (address.isEmpty()) address = "Endereço Indisponível";
                        }

                        String createdAt = (String) eventMap.get("created_at");

                        String itemText =
                                "ID: " + (id != null ? String.valueOf(id) : "N/A") + "\n" +
                                        "Nome: " + (name != null ? name : "N/A") + "\n" +
                                        //"Capacidade: " + capacity + "\n" +
                                        "Endereço: " + address + "\n" +
                                        "Data: " + convertIsoToDdMmYyyyHhSs(createdAt);

                        dadosFormatados.add(itemText);
                    }

                    if (dadosFormatados.isEmpty()) {
                        dadosFormatados.add("Nenhum evento encontrado no momento.");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("ListEvents", "Erro ao desserializar/processar JSON com Jackson: " + e.getMessage());
                    Toast.makeText(ListEvents.this, "Erro ao processar dados de eventos.", Toast.LENGTH_LONG).show();

                    dadosFormatados.clear();
                    dadosFormatados.add("Ocorreu um erro ao carregar os eventos.");
                }

                meuAdapter = new ArrayAdapter<>(ListEvents.this, android.R.layout.simple_list_item_1, dadosFormatados);
                list.setAdapter(meuAdapter);
            }

            @Override
            public void onError(int statusCode, String errorMessage) {
                Log.e("Load Events", "Erro ao carregar eventos: Status " + statusCode + ", Mensagem: " + errorMessage);

                if (errorMessage != null && !errorMessage.isEmpty()) {
                    ObjectMapper errorMapper = new ObjectMapper();
                    try {
                        Map<String, Object> errorMap = errorMapper.readValue(errorMessage, new TypeReference<Map<String, Object>>() {});
                        txtLoading.setText(errorMap.get("message").toString());
                        Toast.makeText(ListEvents.this, errorMap.get("message").toString(), Toast.LENGTH_SHORT).show();
                    } catch (JsonProcessingException e) {
                        Log.e("ListEvents", "Erro ao processar mensagem de erro: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(IOException e) {
                Log.e("Load Events", "Erro ao carregar eventos: " + e.getMessage());
            }

        });
    }

    public void signOut(View view){

        clearAllUserData(ListEvents.this);

        Intent intent = new Intent(this, MainActivity.class);
        // Use FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK to prevent going back
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

    }

    public void linkToProfile(View view){
        Intent intent = new Intent(this, Profile.class);
        startActivity(intent);
    }

    public void linkToQrCodes(View view){
        Intent intent = new Intent(this, Qrcodes.class);
        startActivity(intent);
    }

    public static void clearAllUserData(Context context) {
        // Define o nome do arquivo de SharedPreferences
        final String PREF_NAME = "user_data";

        // Obtém o SharedPreferences. O Context é necessário para isso.
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Obtém um editor para modificar os dados.
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Limpa todos os dados.
        editor.clear();

        // Aplica as mudanças de forma assíncrona (não bloqueia a UI).
        editor.apply();
    }

    public static String convertIsoToDdMmYyyyHhSs(String isoDateTime) {
        // 1. Parse o String ISO 8601 para um Instant
        Instant instant = Instant.parse(isoDateTime);

        // 2. Converta o Instant para LocalDateTime no fuso horário desejado.
        //    Se você quiser o horário local do dispositivo, use ZoneId.systemDefault().
        //    Se você quiser uma representação no fuso horário de São Paulo, use "America/Sao_Paulo".
        ZoneId zoneId = ZoneId.of("America/Sao_Paulo"); // Ou ZoneId.systemDefault();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, zoneId);

        // 3. Defina o formato de saída
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"); // Note HH para 24h, hh para 12h AM/PM

        // 4. Formate o LocalDateTime para o String desejado
        return localDateTime.format(formatter);
    }
}