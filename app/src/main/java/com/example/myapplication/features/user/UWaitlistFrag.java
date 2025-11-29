package com.example.myapplication.features.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class UWaitlistFrag extends Fragment implements UWaitlistAdapter.OnItemClickListener{

    private RecyclerView recyclerView;
    private UWaitlistAdapter adapter;
    private FirebaseEventRepository repo;
    private String curentUid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_u_waitlist, container, false);

        recyclerView = view.findViewById(R.id.waitlistRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UWaitlistAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

         repo = new FirebaseEventRepository();
         curentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

         loadWaitlistEvents();

         return view;
    }

}
