package com.vibecheck.vibecheckparticipante.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibecheck.vibecheckparticipante.R;
import com.vibecheck.vibecheckparticipante.models.QRCodeDisplayItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QRCodeAdapter extends ArrayAdapter<QRCodeDisplayItem> {

    public QRCodeAdapter(@NonNull Context context, @NonNull List<QRCodeDisplayItem> qrcodes) {
        super(context, 0, qrcodes);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_qrcode, parent, false);
        }

        QRCodeDisplayItem qrCodeItem = getItem(position);

        ImageView eventImageView = convertView.findViewById(R.id.eventImageView);
        TextView eventNameTextView = convertView.findViewById(R.id.eventNameTextView);
        TextView eventDateTextView = convertView.findViewById(R.id.eventDateTextView);
        TextView eventLocationTextView = convertView.findViewById(R.id.eventLocationTextView);
        TextView organizerNameTextView = convertView.findViewById(R.id.organizerNameTextView); // Obter referência ao NOVO TextView

        if (qrCodeItem != null) {
            eventNameTextView.setText(qrCodeItem.getEventName());

            // Formatar a data
            String dateString = qrCodeItem.getEventDate();
            if (dateString != null && !dateString.isEmpty()) {
                try {
                    SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());
                    Date date = apiFormat.parse(dateString);
                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    eventDateTextView.setText(displayFormat.format(date));
                } catch (ParseException e) {
                    e.printStackTrace();
                    eventDateTextView.setText("Data indisponível");
                }
            } else {
                eventDateTextView.setText("Data indisponível");
            }

            eventLocationTextView.setText("Local: " + qrCodeItem.getEventLocation());
            organizerNameTextView.setText("Organizador: " + qrCodeItem.getOrganizerName()); // Popular o NOVO TextView

            // Carregar a imagem Base64 no ImageView
            String base64Image = qrCodeItem.getQrCodeBase64();
            if (base64Image != null && !base64Image.isEmpty()) {
                if (base64Image.startsWith("data:image/png;base64,")) {
                    base64Image = base64Image.substring("data:image/png;base64,".length());
                }
                try {
                    byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    eventImageView.setImageBitmap(decodedByte);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    eventImageView.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else {
                eventImageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            eventNameTextView.setText("Evento desconhecido");
            eventDateTextView.setText("Data desconhecida");
            eventLocationTextView.setText("Local desconhecido");
            organizerNameTextView.setText("Organizador: Desconhecido"); // Fallback para o novo TextView
            eventImageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        return convertView;
    }
}