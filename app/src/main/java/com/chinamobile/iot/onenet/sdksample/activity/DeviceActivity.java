package com.chinamobile.iot.onenet.sdksample.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.chinamobile.iot.onenet.OneNetApi;
import com.chinamobile.iot.onenet.OneNetApiCallback;
import com.chinamobile.iot.onenet.sdksample.R;
import com.chinamobile.iot.onenet.sdksample.model.DSItem;
import com.chinamobile.iot.onenet.sdksample.model.DeviceItem;
import com.chinamobile.iot.onenet.sdksample.utils.DeviceItemDeserializer;
import com.chinamobile.iot.onenet.sdksample.utils.IntentActions;
import com.github.clans.fab.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;

public class DeviceActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {

    public static final String EXTRA_DEVICE_ITEM = "extra_device_item";
    public static final String TAG = "DeviceActivity";
    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private DSListAdapter mAdapter;
    private FloatingActionButton mFabAddDataStream;
    private DeviceItem mDeviceItem;
    private EditText mDeviceIMEI;

    public static void actionDevice(Context context, DeviceItem deviceItem) {
        Intent intent = new Intent(context, DeviceActivity.class);
        intent.putExtra(EXTRA_DEVICE_ITEM, deviceItem);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        LocalBroadcastManager.getInstance(this).registerReceiver(mUpdateDeviceReceiver, new IntentFilter(IntentActions.ACTION_UPDATE_DEVICE));
        LocalBroadcastManager.getInstance(this).registerReceiver(mUpdateDsList, new IntentFilter(IntentActions.ACTION_UPDATE_DS_LIST));
        initViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mUpdateDeviceReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mUpdateDsList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        switch (mDeviceItem.getProtocol().toUpperCase()) {
            case "HTTP":
            case "JTEXT":
                getMenuInflater().inflate(R.menu.menu_device_activity_http, menu);
                break;
            case "EDP":
            case "MQTT":
            case "MODBUS":
                getMenuInflater().inflate(R.menu.menu_device_activity_edp, menu);
                break;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;

            case R.id.menu_edit_device:
                EditDeviceActivity.actionEditDevice(this, mDeviceItem);
                break;

            case R.id.menu_delete_device:
                showDeleteDeviceDialog();
                break;

            case R.id.menu_send_command:
                SendCommandActivity.actionSendCommand(this, mDeviceItem);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    private void EcdisplayLog(String response) {
        if ((response.startsWith("{") && response.endsWith("}")) || (response.startsWith("[") && response.endsWith("]"))) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser jsonParser = new JsonParser();
            response = gson.toJson(jsonParser.parse(response));
        }
        DisplayApiRespActivity.actionDisplayApiResp(DeviceActivity.this, response);
    }
    private class EcCallback implements OneNetApiCallback {
        @Override
        public void onSuccess(String response) {
            //displayLog(response);
            //Values are passing to activity & to fragment as well
           // Toast.makeText(DeviceActivity.this, response,
             //       Toast.LENGTH_SHORT).show();
            EcdisplayLog(response);
           // mProgressDialog.dismiss();
        }

        @Override
        public void onFailed(Exception e) {
            //DisplayApiRespActivity.actionDisplayApiResp(getActivity(), e.toString());
            //mProgressDialog.dismiss();
            //Toast.makeText(DeviceActivity.this, e.toString(),
            //        Toast.LENGTH_SHORT).show();
            Log.e(TAG, e.toString());
        }
    }
    private void initViews() {
        mDeviceItem = (DeviceItem) getIntent().getSerializableExtra(EXTRA_DEVICE_ITEM);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(mDeviceItem.getTitle());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(android.support.v7.appcompat.R.drawable.abc_ic_ab_back_material);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyler_view);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        //mFabAddDataStream = (FloatingActionButton) findViewById(R.id.fab_add_ds);
        mDeviceIMEI = (EditText) findViewById(R.id.editTextIMEI);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new DSListAdapter();
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                Log.d(TAG, mDeviceIMEI.getText().toString());//866818030021444
                OneNetApi.get("http://api.heclouds.com/nbiot/offline?imei="+mDeviceIMEI.getText().toString() +"&obj_id=3306&expired_time=2019-04-08T17:30:00&trigger_msg=7",new EcCallback());

            }

            @Override
            public void onLongClick(View view, int position) {
                Toast.makeText(DeviceActivity.this, "Long press on position :"+position,
                        Toast.LENGTH_LONG).show();
            }
        }));
        mSwipeRefreshLayout.setColorSchemeColors(0xFFDA4336);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        getDataStreams();
        mSwipeRefreshLayout.setRefreshing(true);

//        mFabAddDataStream.setOnClickListener(this);
    }

    @Override
    public void onRefresh() {
        getDataStreams();
    }

    @Override
    public void onClick(View v) {
        AddDataStreamActivity.actionAddDataStream(this, mDeviceItem.getId());
        Log.d(TAG, "click actionAddDataStream....." + mDeviceItem.getId());
    }
    public static interface ClickListener{
        public void onClick(View view,int position);
        public void onLongClick(View view,int position);
    }
    class RecyclerTouchListener implements RecyclerView.OnItemTouchListener{

        private ClickListener clicklistener;
        private GestureDetector gestureDetector;

        public RecyclerTouchListener(Context context, final RecyclerView recycleView, final ClickListener clicklistener){

            this.clicklistener=clicklistener;
            gestureDetector=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child=recycleView.findChildViewUnder(e.getX(),e.getY());
                    if(child!=null && clicklistener!=null){
                        clicklistener.onLongClick(child,recycleView.getChildAdapterPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            View child=rv.findChildViewUnder(e.getX(),e.getY());
            if(child!=null && clicklistener!=null && gestureDetector.onTouchEvent(e)){
                clicklistener.onClick(child,rv.getChildAdapterPosition(child));
            }

            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }
    class DSListAdapter extends BaseQuickAdapter<DSItem, BaseViewHolder> {
        public DSListAdapter() {
            super(R.layout.ds_list_item);
        }

        @Override
        protected void convert(BaseViewHolder helper, DSItem item) {
            helper.setText(R.id.id, item.getId());
            if (item.getCurrentValue() != null) {
                helper.setText(R.id.current_value, getResources().getString(R.string.latest_data) + ": " + item.getCurrentValue().toString());
            } else {
                helper.setText(R.id.current_value, getResources().getString(R.string.latest_data) + ": " + getResources().getString(R.string.no_data));
            }
            if (!TextUtils.isEmpty(item.getUpdateTime())) {
                helper.setText(R.id.update_time, getResources().getString(R.string.update_time) + ": " + item.getUpdateTime());
            } else {
                helper.setText(R.id.update_time, getResources().getString(R.string.update_time) + ": " + getResources().getString(R.string.no_data));
            }
        }
    }

    private void getDataStreams() {
        OneNetApi.queryMultiDataStreams(mDeviceItem.getId(), new OneNetApiCallback() {
            @Override
            public void onSuccess(String response) {
                mSwipeRefreshLayout.setRefreshing(false);
                JsonObject resp = new JsonParser().parse(response).getAsJsonObject();
                int errno = resp.get("errno").getAsInt();
                if (0 == errno) {
                    JsonElement dataElement = resp.get("data");
                    if (dataElement != null) {
                        JsonArray jsonArray = dataElement.getAsJsonArray();
                        ArrayList<DSItem> dsItems = new ArrayList<>();
                        Gson gson = new Gson();
                        for (JsonElement element : jsonArray) {
                            dsItems.add(gson.fromJson(element, DSItem.class));
                        }
                        mAdapter.setNewData(dsItems);
                    }
                } else {
                    String error = resp.get("error").getAsString();
                    Toast.makeText(getApplicationContext(), error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailed(Exception e) {
                mSwipeRefreshLayout.setRefreshing(false);
                e.printStackTrace();
            }
        });
    }

    private void showDeleteDeviceDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_device)
                .setMessage(R.string.delete_device_dialog_content)
                .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteDevice();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void deleteDevice() {
        OneNetApi.deleteDevice(mDeviceItem.getId(), new OneNetApiCallback() {
            @Override
            public void onSuccess(String response) {
                JsonObject resp = new JsonParser().parse(response).getAsJsonObject();
                int errno = resp.get("errno").getAsInt();
                if (0 == errno) {
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(IntentActions.ACTION_UPDATE_DEVICE_LIST));
                    finish();
                } else {
                    String error = resp.get("error").getAsString();
                    Toast.makeText(getApplicationContext(), error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailed(Exception e) {
                e.printStackTrace();
            }
        });
    }

    private BroadcastReceiver mUpdateDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            OneNetApi.querySingleDevice(mDeviceItem.getId(), new OneNetApiCallback() {
                @Override
                public void onSuccess(String response) {
                    JsonObject resp = new JsonParser().parse(response).getAsJsonObject();
                    int errno = resp.get("errno").getAsInt();
                    if (0 == errno) {
                        GsonBuilder gsonBuilder = new GsonBuilder();
                        gsonBuilder.registerTypeAdapter(DeviceItem.class, new DeviceItemDeserializer());
                        Gson gson = gsonBuilder.create();
                        mDeviceItem = gson.fromJson(resp.get("data"), DeviceItem.class);
                        getSupportActionBar().setTitle(mDeviceItem.getTitle());
                    } else {
                        String error = resp.get("error").getAsString();
                        Toast.makeText(getApplicationContext(), error, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailed(Exception e) {
                    e.printStackTrace();
                }
            });
        }
    };

    private BroadcastReceiver mUpdateDsList = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getDataStreams();
        }
    };
}
