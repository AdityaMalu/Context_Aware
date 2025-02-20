package com.example.reader;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class IDFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_id, container, false);

        TextView textView = view.findViewById(R.id.text_fragment);
        textView.setText("ID Fragment");

        Button createIdButton = view.findViewById(R.id.btn_create_id);
        createIdButton.setOnClickListener(v -> {
            // Action to create an ID
            Toast.makeText(getActivity(), "ID Created!", Toast.LENGTH_SHORT).show();
        });

        return view;
    }
}

