package com.example.myapplication.features.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.data.firebase.FirebaseEventRepository;

public class UWaitlistFrag extends Fragment implements UWaitlistAdapter.OnItemClickListener{

    private RecyclerView recyclerView;
    private UWaitlistAdapter adapter;
    private FirebaseEventRepository repo;
    private String curentUid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

    }
}
