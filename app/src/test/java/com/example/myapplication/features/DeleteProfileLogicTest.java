package com.example.myapplication.features;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.myapplication.features.organizer.OProfileFrag;
import com.example.myapplication.features.user.UProfileFrag;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit coverage for the delete-profile cascade used by both user and organizer fragments.
 */
public class DeleteProfileLogicTest {

    @Test
    public void handleEventDeletionResult_runsOnCompleteWhenSnapshotMissingOrEmpty() throws Exception {
        UProfileFrag fragment = new UProfileFrag();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicBoolean failed = new AtomicBoolean(false);
        Runnable onComplete = () -> completed.set(true);
        OnFailureListener onFailure = e -> failed.set(true);

        invokeHandleEventDeletionResult(fragment, null, onComplete, onFailure);
        assertTrue(completed.get());
        assertFalse(failed.get());

        completed.set(false);
        QuerySnapshot emptySnapshot = mock(QuerySnapshot.class);
        doReturn(true).when(emptySnapshot).isEmpty();

        invokeHandleEventDeletionResult(fragment, emptySnapshot, onComplete, onFailure);
        assertTrue(completed.get());
        assertFalse(failed.get());
    }

    @Test
    public void handleEventDeletionResult_deletesDocsAndNotifiesCompletion() throws Exception {
        UProfileFrag fragment = new UProfileFrag();
        Runnable onComplete = mock(Runnable.class);
        OnFailureListener onFailure = mock(OnFailureListener.class);

        DocumentReference ref1 = mock(DocumentReference.class);
        DocumentReference ref2 = mock(DocumentReference.class);
        doReturn(Tasks.forResult(null)).when(ref1).delete();
        doReturn(Tasks.forResult(null)).when(ref2).delete();

        DocumentSnapshot doc1 = mock(DocumentSnapshot.class);
        DocumentSnapshot doc2 = mock(DocumentSnapshot.class);
        doReturn(ref1).when(doc1).getReference();
        doReturn(ref2).when(doc2).getReference();

        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        doReturn(false).when(snapshot).isEmpty();
        doReturn(Arrays.asList(doc1, doc2)).when(snapshot).getDocuments();

        Task<java.util.List<Task<?>>> aggregateTask = mock(Task.class);

        try (MockedStatic<Tasks> tasks = mockStatic(Tasks.class)) {
            tasks.when(() -> Tasks.whenAllComplete(anyList())).thenReturn(aggregateTask);

            doAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                OnSuccessListener<java.util.List<Task<?>>> listener = invocation.getArgument(0);
                listener.onSuccess(Collections.emptyList());
                return aggregateTask;
            }).when(aggregateTask).addOnSuccessListener(any());

            doReturn(aggregateTask).when(aggregateTask).addOnFailureListener(any());

            invokeHandleEventDeletionResult(fragment, snapshot, onComplete, onFailure);
        }

        verify(ref1).delete();
        verify(ref2).delete();
        verify(onComplete).run();
        verifyNoInteractions(onFailure);
    }

    @Test
    public void handleEventDeletionResult_propagatesFailure() throws Exception {
        UProfileFrag fragment = new UProfileFrag();
        Runnable onComplete = mock(Runnable.class);
        AtomicReference<Exception> failure = new AtomicReference<>();
        OnFailureListener onFailure = failure::set;

        DocumentReference ref = mock(DocumentReference.class);
        doReturn(Tasks.forResult(null)).when(ref).delete();

        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        doReturn(ref).when(doc).getReference();

        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        doReturn(false).when(snapshot).isEmpty();
        doReturn(Collections.singletonList(doc)).when(snapshot).getDocuments();

        Task<java.util.List<Task<?>>> aggregateTask = mock(Task.class);
        Exception expected = new Exception("aggregate failed");

        try (MockedStatic<Tasks> tasks = mockStatic(Tasks.class)) {
            tasks.when(() -> Tasks.whenAllComplete(anyList())).thenReturn(aggregateTask);
            doReturn(aggregateTask).when(aggregateTask).addOnSuccessListener(any());
            doAnswer(invocation -> {
                OnFailureListener listener = invocation.getArgument(0);
                listener.onFailure(expected);
                return aggregateTask;
            }).when(aggregateTask).addOnFailureListener(any());

            invokeHandleEventDeletionResult(fragment, snapshot, onComplete, onFailure);
        }

        assertSame(expected, failure.get());
        verify(onComplete, never()).run();
    }

    @Test
    public void deleteUserEvents_queriesBothOrganizerIdFields_user() throws Exception {
        UProfileFrag fragment = new UProfileFrag();
        String uid = "abc123";
        CollectionReference events = stubSuccessfulEventQueries(fragment, uid);

        Runnable onComplete = mock(Runnable.class);
        OnFailureListener onFailure = mock(OnFailureListener.class);

        invokeDeleteUserEvents(fragment, uid, onComplete, onFailure);

        verify(events).whereEqualTo("organizerID", uid);
        verify(events).whereEqualTo("organizerId", uid);
        verify(onComplete).run();
        verifyNoInteractions(onFailure);
    }

    @Test
    public void deleteUserEvents_stopsOnFailure_user() throws Exception {
        UProfileFrag fragment = new UProfileFrag();
        String uid = "abc123";
        Exception expected = new Exception("query failed");
        CollectionReference events = stubFailingEventQuery(fragment, uid, expected);

        Runnable onComplete = mock(Runnable.class);
        AtomicReference<Exception> failure = new AtomicReference<>();
        OnFailureListener onFailure = failure::set;

        invokeDeleteUserEvents(fragment, uid, onComplete, onFailure);

        assertSame(expected, failure.get());
        verify(onComplete, never()).run();
        verify(events, never()).whereEqualTo("organizerId", uid);
    }

    @Test
    public void deleteUserEvents_queriesBothOrganizerIdFields_organizer() throws Exception {
        OProfileFrag fragment = new OProfileFrag();
        String uid = "organizer-42";
        CollectionReference events = stubSuccessfulEventQueries(fragment, uid);

        Runnable onComplete = mock(Runnable.class);
        OnFailureListener onFailure = mock(OnFailureListener.class);

        invokeDeleteUserEvents(fragment, uid, onComplete, onFailure);

        verify(events).whereEqualTo("organizerID", uid);
        verify(events).whereEqualTo("organizerId", uid);
        verify(onComplete).run();
        verifyNoInteractions(onFailure);
    }

    @Test
    public void deleteUserEvents_stopsOnFailure_organizer() throws Exception {
        OProfileFrag fragment = new OProfileFrag();
        String uid = "organizer-42";
        Exception expected = new Exception("query failed");
        CollectionReference events = stubFailingEventQuery(fragment, uid, expected);

        Runnable onComplete = mock(Runnable.class);
        AtomicReference<Exception> failure = new AtomicReference<>();
        OnFailureListener onFailure = failure::set;

        invokeDeleteUserEvents(fragment, uid, onComplete, onFailure);

        assertSame(expected, failure.get());
        verify(onComplete, never()).run();
        verify(events, never()).whereEqualTo("organizerId", uid);
    }

    @Test
    public void deleteEventsByField_runsCompletionForBothFragments() throws Exception {
        for (Object fragment : Arrays.asList(new UProfileFrag(), new OProfileFrag())) {
            FirebaseFirestore firestore = mock(FirebaseFirestore.class);
            CollectionReference events = mock(CollectionReference.class);
            Query query = mock(Query.class);
            Task<QuerySnapshot> task = mock(Task.class);
            QuerySnapshot snapshot = mock(QuerySnapshot.class);
            doReturn(true).when(snapshot).isEmpty();

            String fieldName = "organizerID";
            String uid = fragment instanceof UProfileFrag ? "user-uid" : "organizer-uid";

            doReturn(events).when(firestore).collection("events");
            doReturn(query).when(events).whereEqualTo(fieldName, uid);
            doReturn(task).when(query).get();

            doAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                OnSuccessListener<QuerySnapshot> listener = invocation.getArgument(0);
                listener.onSuccess(snapshot);
                return task;
            }).when(task).addOnSuccessListener(any());
            doReturn(task).when(task).addOnFailureListener(any());

            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicBoolean failed = new AtomicBoolean(false);

            injectFirestore(fragment, firestore);
            invokeDeleteEventsByField(fragment, fieldName, uid, () -> completed.set(true), e -> failed.set(true));

            assertTrue(completed.get());
            assertFalse(failed.get());
            verify(events).whereEqualTo(fieldName, uid);
        }
    }

    @Test
    public void deleteEventsByField_propagatesFailureForBothFragments() throws Exception {
        for (Object fragment : Arrays.asList(new UProfileFrag(), new OProfileFrag())) {
            FirebaseFirestore firestore = mock(FirebaseFirestore.class);
            CollectionReference events = mock(CollectionReference.class);
            Query query = mock(Query.class);
            Task<QuerySnapshot> task = mock(Task.class);

            String fieldName = "organizerID";
            String uid = fragment instanceof UProfileFrag ? "user-uid" : "organizer-uid";
            Exception expected = new Exception("query failed for " + uid);

            doReturn(events).when(firestore).collection("events");
            doReturn(query).when(events).whereEqualTo(fieldName, uid);
            doReturn(task).when(query).get();

            doReturn(task).when(task).addOnSuccessListener(any());
            doAnswer(invocation -> {
                OnFailureListener listener = invocation.getArgument(0);
                listener.onFailure(expected);
                return task;
            }).when(task).addOnFailureListener(any());

            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicReference<Exception> failure = new AtomicReference<>();

            injectFirestore(fragment, firestore);
            invokeDeleteEventsByField(fragment, fieldName, uid, () -> completed.set(true), failure::set);

            assertFalse(completed.get());
            assertSame(expected, failure.get());
            verify(events).whereEqualTo(fieldName, uid);
        }
    }

    @Test
    public void deleteUserDocument_notifiesCompletionForBothFragments() throws Exception {
        for (Object fragment : Arrays.asList(new UProfileFrag(), new OProfileFrag())) {
            FirebaseFirestore firestore = mock(FirebaseFirestore.class);
            CollectionReference users = mock(CollectionReference.class);
            DocumentReference doc = mock(DocumentReference.class);
            Task<Void> deleteTask = mock(Task.class);

            String uid = fragment instanceof UProfileFrag ? "user-uid" : "organizer-uid";

            doReturn(users).when(firestore).collection("users");
            doReturn(doc).when(users).document(uid);
            doReturn(deleteTask).when(doc).delete();

            doAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                OnSuccessListener<Void> listener = invocation.getArgument(0);
                listener.onSuccess(null);
                return deleteTask;
            }).when(deleteTask).addOnSuccessListener(any());
            doReturn(deleteTask).when(deleteTask).addOnFailureListener(any());

            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicReference<Exception> failure = new AtomicReference<>();

            injectFirestore(fragment, firestore);
            invokeDeleteUserDocument(fragment, uid, () -> completed.set(true), failure::set);

            assertTrue(completed.get());
            assertNull(failure.get());
            verify(users).document(uid);
            verify(doc).delete();
        }
    }

    @Test
    public void deleteUserDocument_propagatesFailureForBothFragments() throws Exception {
        for (Object fragment : Arrays.asList(new UProfileFrag(), new OProfileFrag())) {
            FirebaseFirestore firestore = mock(FirebaseFirestore.class);
            CollectionReference users = mock(CollectionReference.class);
            DocumentReference doc = mock(DocumentReference.class);
            Task<Void> deleteTask = mock(Task.class);

            String uid = fragment instanceof UProfileFrag ? "user-uid" : "organizer-uid";
            Exception expected = new Exception("delete failed for " + uid);

            doReturn(users).when(firestore).collection("users");
            doReturn(doc).when(users).document(uid);
            doReturn(deleteTask).when(doc).delete();

            doReturn(deleteTask).when(deleteTask).addOnSuccessListener(any());
            doAnswer(invocation -> {
                OnFailureListener listener = invocation.getArgument(0);
                listener.onFailure(expected);
                return deleteTask;
            }).when(deleteTask).addOnFailureListener(any());

            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicReference<Exception> failure = new AtomicReference<>();

            injectFirestore(fragment, firestore);
            invokeDeleteUserDocument(fragment, uid, () -> completed.set(true), failure::set);

            assertFalse(completed.get());
            assertSame(expected, failure.get());
            verify(users).document(uid);
            verify(doc).delete();
        }
    }

    private CollectionReference stubSuccessfulEventQueries(Object fragment, String uid) throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference events = mock(CollectionReference.class);
        Query legacyQuery = mock(Query.class);
        Query modernQuery = mock(Query.class);
        QuerySnapshot emptySnapshot = mock(QuerySnapshot.class);
        doReturn(true).when(emptySnapshot).isEmpty();

        Task<QuerySnapshot> legacyTask = mock(Task.class);
        Task<QuerySnapshot> modernTask = mock(Task.class);

        doReturn(events).when(firestore).collection("events");
        doReturn(legacyQuery).when(events).whereEqualTo("organizerID", uid);
        doReturn(modernQuery).when(events).whereEqualTo("organizerId", uid);
        doReturn(legacyTask).when(legacyQuery).get();
        doReturn(modernTask).when(modernQuery).get();

        stubSuccessTask(legacyTask, emptySnapshot);
        stubSuccessTask(modernTask, emptySnapshot);
        injectFirestore(fragment, firestore);
        return events;
    }

    private CollectionReference stubFailingEventQuery(Object fragment, String uid, Exception error) throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference events = mock(CollectionReference.class);
        Query legacyQuery = mock(Query.class);
        Task<QuerySnapshot> legacyTask = mock(Task.class);

        doReturn(events).when(firestore).collection("events");
        doReturn(legacyQuery).when(events).whereEqualTo("organizerID", uid);
        doReturn(legacyTask).when(legacyQuery).get();

        doReturn(legacyTask).when(legacyTask).addOnSuccessListener(any());
        doAnswer(invocation -> {
            OnFailureListener listener = invocation.getArgument(0);
            listener.onFailure(error);
            return legacyTask;
        }).when(legacyTask).addOnFailureListener(any());

        injectFirestore(fragment, firestore);
        return events;
    }

    private void stubSuccessTask(Task<QuerySnapshot> task, QuerySnapshot snapshot) {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            OnSuccessListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onSuccess(snapshot);
            return task;
        }).when(task).addOnSuccessListener(any());

        doReturn(task).when(task).addOnFailureListener(any());
    }

    private void injectFirestore(Object fragment, FirebaseFirestore firestore) throws Exception {
        Field firestoreField = fragment.getClass().getDeclaredField("firestore");
        firestoreField.setAccessible(true);
        firestoreField.set(fragment, firestore);
    }

    private void invokeDeleteEventsByField(Object fragment, String fieldName, String uid, Runnable onComplete, OnFailureListener onFailure) throws Exception {
        Method method = fragment.getClass().getDeclaredMethod(
                "deleteEventsByField", String.class, String.class, Runnable.class, OnFailureListener.class);
        method.setAccessible(true);
        method.invoke(fragment, fieldName, uid, onComplete, onFailure);
    }

    private void invokeDeleteUserDocument(Object fragment, String uid, Runnable onComplete, OnFailureListener onFailure) throws Exception {
        Method method = fragment.getClass().getDeclaredMethod(
                "deleteUserDocument", String.class, Runnable.class, OnFailureListener.class);
        method.setAccessible(true);
        method.invoke(fragment, uid, onComplete, onFailure);
    }

    private void invokeHandleEventDeletionResult(Object fragment, QuerySnapshot snapshot, Runnable onComplete, OnFailureListener onFailure) throws Exception {
        Method method = fragment.getClass().getDeclaredMethod(
                "handleEventDeletionResult", QuerySnapshot.class, Runnable.class, OnFailureListener.class);
        method.setAccessible(true);
        method.invoke(fragment, snapshot, onComplete, onFailure);
    }

    private void invokeDeleteUserEvents(Object fragment, String uid, Runnable onComplete, OnFailureListener onFailure) throws Exception {
        Method method = fragment.getClass().getDeclaredMethod(
                "deleteUserEvents", String.class, Runnable.class, OnFailureListener.class);
        method.setAccessible(true);
        method.invoke(fragment, uid, onComplete, onFailure);
    }
}
