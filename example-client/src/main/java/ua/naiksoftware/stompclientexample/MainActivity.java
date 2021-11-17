package ua.naiksoftware.stompclientexample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.CompletableTransformer;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.internal.fuseable.ScalarSupplier;
import io.reactivex.rxjava3.schedulers.Schedulers;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;
import ua.naiksoftware.stomp.dto.StompMessage;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String WS_URI = "ws://10.0.2.2:8080/ws";
    private static final String AUTH_HEADER = "Authorization";
    private static final String AUTH_TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMCIsImlhdCI6MTYzNjkwOTcyMCwiZXhwIjoxNjM3NTE0NTIwfQ.YnxTB8F6spESHWdLBeSMnft9t6bqB3z8YS9BqW_OmlOWXJint-vprwSYVFBVajKli3J8RgiD_ROib9NaAC3MDA";
    private static final String ROOM_CHANNEL = "/chat/20/messages";
    private static final String USER_CHANNEL = "/user/10/queue/messages";
    private static final String SEND_CHANNEL = "/app/chat/20/send";

    private final Gson mGson = new GsonBuilder().create();
    private final List<String> mDataSet = new ArrayList<>();

    private SimpleAdapter mAdapter;
    private StompClient mStompClient;
    private RecyclerView mRecyclerView;

    private EditText editHost;
    private EditText editToken;
    private EditText editRoomChannel;
    private EditText editMsgChannel;
    private EditText editMsgInput;

    private CompositeDisposable compositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = findViewById(R.id.recycler_view);
        editHost = findViewById(R.id.edit_host);
        editToken = findViewById(R.id.edit_authToken);
        editRoomChannel = findViewById(R.id.edit_send_channel);
        editMsgChannel = findViewById(R.id.edit_msg_channel);
        editMsgInput = findViewById(R.id.edit_msg);

        editHost.setText(WS_URI);
        editToken.setText(AUTH_TOKEN);
        editMsgChannel.setText(ROOM_CHANNEL);
        editRoomChannel.setText(SEND_CHANNEL);

        mAdapter = new SimpleAdapter(mDataSet);
        mAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true));

        resetSubscriptions();
    }

    public void disconnectStomp(View view) {
        mStompClient.disconnect();
    }

    public void connectStomp(View view) {

        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, editHost.getText().toString());
        mStompClient.withClientHeartbeat(0).withServerHeartbeat(0);

        resetSubscriptions();

        Disposable dispLifecycle = mStompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lifecycleEvent -> {
                    switch (lifecycleEvent.getType()) {
                        case OPENED:
                            toast("Stomp connection opened");
                            break;
                        case ERROR:
                            Log.e(TAG, "Stomp connection error", lifecycleEvent.getException());
                            toast("Stomp connection error");
                            break;
                        case CLOSED:
                            toast("Stomp connection closed");
                            resetSubscriptions();
                            break;
                        case FAILED_SERVER_HEARTBEAT:
                            toast("Stomp failed server heartbeat");
                            break;
                    }
                });

        compositeDisposable.add(dispLifecycle);

        Consumer<StompMessage> messageReceivedConsumer = topicMessage -> {
            Log.d(TAG, "Received " + topicMessage.getPayload());
            topicMessage.getStompHeaders().forEach(stompHeader -> toast("Header[" + stompHeader.getKey()+"]=" + stompHeader.getValue()));
            if (Boolean.parseBoolean(topicMessage.findHeader("success"))) {
                toast("Success Message payload = " + topicMessage.getPayload());
            }

        };
//        // Receive greetings
//        Disposable dispTopic = mStompClient.topic(editMsgChannel.getText().toString())
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(messageReceivedConsumer, throwable -> Log.e(TAG, "Error on subscribe topic", throwable));

        // Receive greetings
        Disposable userChannel = mStompClient.topic(USER_CHANNEL)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(messageReceivedConsumer, throwable -> Log.e(TAG, "Error on subscribe topic", throwable));



//        compositeDisposable.add(dispTopic);
        compositeDisposable.add(userChannel);
        mStompClient.connect(Collections.singletonList(new StompHeader(AUTH_HEADER, AUTH_TOKEN)));
    }

    public void sendEchoViaStomp(View v) {

        if (mStompClient == null || !mStompClient.isConnected()) return;

        ChatMessageReq msg = new ChatMessageReq();
        msg.setMessage(editMsgInput.getText().toString());


        compositeDisposable.add(mStompClient.send(editRoomChannel.getText().toString(), mGson.toJson(msg))
                .compose(applySchedulers())
                .subscribe(() -> Log.d(TAG, "STOMP echo send successfully"), throwable -> {
                    Log.e(TAG, "Error send STOMP echo", throwable);
                    toast(throwable.getMessage());
                }));
    }

    private void addItem(ChatMessageRes chatMessageRes) {
        mDataSet.add(chatMessageRes.getFullName() + " - " + chatMessageRes.getMsg() + " - " + chatMessageRes.getTimeSend());
        mAdapter.notifyDataSetChanged();
        mRecyclerView.smoothScrollToPosition(mDataSet.size() - 1);
    }

    private void toast(String text) {
        Log.i(TAG, text);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    protected CompletableTransformer applySchedulers() {
        return upstream -> upstream
                .unsubscribeOn(Schedulers.newThread())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void resetSubscriptions() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
        compositeDisposable = new CompositeDisposable();
    }

    private <T> WsResponse<T> convertWsResponse(String json, Class<T> clazz) {

        Type typeOfT = TypeToken.getParameterized(WsResponse.class, clazz).getType();
        return mGson.fromJson(json, typeOfT);
    }

    @Override
    protected void onDestroy() {
        mStompClient.disconnect();

        if (compositeDisposable != null) compositeDisposable.dispose();
        super.onDestroy();
    }
}
