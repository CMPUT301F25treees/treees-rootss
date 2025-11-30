package com.example.myapplication.data.firebase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public class FirebaseUserRepositoryTest {

    private FirebaseUserRepository repository;

    @Mock
    private FirebaseFirestore firestore;

    @Mock
    private CollectionReference collectionReference;

    @Mock
    private DocumentReference documentReference;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        try (MockedStatic<FirebaseFirestore> mockedFirestore = mockStatic(FirebaseFirestore.class)) {
            mockedFirestore.when(FirebaseFirestore::getInstance).thenReturn(firestore);
            repository = new FirebaseUserRepository();
        }
        injectFirestore(repository, firestore);
    }

    private void injectFirestore(Object target, FirebaseFirestore firestore) throws Exception {
        Field field = target.getClass().getDeclaredField("firestore");
        field.setAccessible(true);
        field.set(target, firestore);
    }

    @Test
    public void deleteEventsForUser_deletesBothFields() {
        String uid = "test-uid";

        when(firestore.collection("events")).thenReturn(collectionReference);
        Query query = mock(Query.class);
        when(collectionReference.whereEqualTo(anyString(), eq(uid))).thenReturn(query);
        
        Task<QuerySnapshot> queryTask = mock(Task.class);
        when(query.get()).thenReturn(queryTask);

        QuerySnapshot emptySnapshot = mock(QuerySnapshot.class);
        when(emptySnapshot.isEmpty()).thenReturn(true);
        
        doAnswer(invocation -> {
            OnSuccessListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onSuccess(emptySnapshot);
            return queryTask;
        }).when(queryTask).addOnSuccessListener(any());
        
        doReturn(queryTask).when(queryTask).addOnFailureListener(any());

        AtomicBoolean completed = new AtomicBoolean(false);
        
        repository.deleteEventsForUser(uid, () -> completed.set(true), e -> {});

        verify(collectionReference).whereEqualTo("organizerID", uid);
        verify(collectionReference).whereEqualTo("organizerId", uid);
        assertTrue(completed.get());
    }

    @Test
    public void deleteUserDocument_success() {
        String uid = "test-uid";
        when(firestore.collection("users")).thenReturn(collectionReference);
        when(collectionReference.document(uid)).thenReturn(documentReference);
        
        Task<Void> deleteTask = mock(Task.class);
        when(documentReference.delete()).thenReturn(deleteTask);

        doAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return deleteTask;
        }).when(deleteTask).addOnSuccessListener(any());
        doReturn(deleteTask).when(deleteTask).addOnFailureListener(any());

        AtomicBoolean completed = new AtomicBoolean(false);
        repository.deleteUserDocument(uid, () -> completed.set(true), e -> {});

        assertTrue(completed.get());
        verify(documentReference).delete();
    }
    
    @Test
    public void deleteAuthUser_success() {
        FirebaseUser user = mock(FirebaseUser.class);
        Task<Void> task = mock(Task.class);
        when(user.delete()).thenReturn(task);
        
        doAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return task;
        }).when(task).addOnSuccessListener(any());
        doReturn(task).when(task).addOnFailureListener(any());

        AtomicBoolean completed = new AtomicBoolean(false);
        repository.deleteAuthUser(user, () -> completed.set(true), e -> {});

        assertTrue(completed.get());
        verify(user).delete();
    }

    @Test
    public void disableEventsForUser_updatesDocuments() {
        String uid = "test-uid";
        when(firestore.collection("events")).thenReturn(collectionReference);
        Query query = mock(Query.class);
        when(collectionReference.whereEqualTo("organizerID", uid)).thenReturn(query);
        
        Task<QuerySnapshot> queryTask = mock(Task.class);
        when(query.get()).thenReturn(queryTask);

        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        when(snapshot.isEmpty()).thenReturn(false);
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.getReference()).thenReturn(documentReference);
        when(snapshot.getDocuments()).thenReturn(Collections.singletonList(doc));
        
        doAnswer(invocation -> {
            OnSuccessListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onSuccess(snapshot);
            return queryTask;
        }).when(queryTask).addOnSuccessListener(any());
        doReturn(queryTask).when(queryTask).addOnFailureListener(any());

        WriteBatch batch = mock(WriteBatch.class);
        when(firestore.batch()).thenReturn(batch);
        Task<Void> batchTask = mock(Task.class);
        when(batch.commit()).thenReturn(batchTask);
        
        doAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            listener.onSuccess(null);
            return batchTask;
        }).when(batchTask).addOnSuccessListener(any());
        doReturn(batchTask).when(batchTask).addOnFailureListener(any());

        AtomicBoolean completed = new AtomicBoolean(false);
        repository.disableEventsForUser(uid, () -> completed.set(true), e -> {});
        
        verify(batch).update(documentReference, "disabled", true);
        verify(batch).commit();
        assertTrue(completed.get());
    }
}
