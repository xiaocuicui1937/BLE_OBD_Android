package com.zhongxunkeji.app.ble_obd_android;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.UiThread;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.zhongxunkeji.app.ble_obd_android.dialog.BLEDevicesDialog;
import com.zhongxunkeji.app.ble_obd_android.utils.LogUtils;
import com.zhongxunkeji.app.ble_obd_android.utils.Tools;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements BLEDevicesDialog.OnFragmentInteractionListener {
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 5000;
    private static final String LOG_SCAN = "BLEScan";
    private static final String LOG_GATT = "BLE_Gatt";

    //Bluetooth UUID
    private static final UUID mServiceUUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID mWriteUUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID mNotifyUUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    //开启notify才能接收数据
    private static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private DialogFragment mDevicesDialog;
    private ProgressDialog mWaitingDialog;
    private String mCurrentCommand;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning, mIsConnected, mWaitingForResult;
    private Handler mHandler;
    private BluetoothGatt mBluetoothGatt;
    private View mRootView;
    private TextView mTvContent;
    private Spinner mSpType;
    private Button mBtWrite;
    private String mCurrentCharacter;
    private Set<BluetoothDevice> devices;

    /**
     * @author cui
     * @function 简化findViewById
     * @param id
     * @param <T>
     * @return
     */
    private <T extends View> T $(View view,int id){
        return (T)view.findViewById(id);
    }
    private <T extends View> T $(int id){
        return (T)findViewById(id);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        setContentView(R.layout.activity_main);

        mBtWrite = $(R.id.bt_write);
        initData();
    }

    /**
     * @author cui
     * @function 初始化数据
     */
    private void initData() {
        findViewById(R.id.btn_find_devices).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(true);
            }
        });

        devices = new HashSet<>();
        mHandler = new Handler();
        mWaitingDialog = new ProgressDialog(this);
        mWaitingDialog.setMessage("Scanning");
        mWaitingDialog.setIndeterminate(true);
        mWaitingDialog.setCancelable(false);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * @deprecated
     * @author cui
     * @function 展示连接设备成功后的可以发送指令的界面，通过ViewStub来展示内容
     * @function 需要将ui更新放在ui线程中
     */
    private void showVsCharacter() {
        /*new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                    }

                });
            }
        }).start();*/

        if (mRootView == null) {
            ViewStub vs = (ViewStub) findViewById(R.id.vs_send_characters);
            mRootView = vs.inflate();
        }
        mSpType = $(mRootView, R.id.sp_type);
        mTvContent = $(mRootView, R.id.tv_content);
        mBtWrite = $(mRootView, R.id.bt_write);

        selectCharacter();

    }

    /**
     * @deprecated
     * @author cui
     * @function 选择发送的指令
     * */
    private void selectCharacter() {
        mSpType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        //查询vin码的指令
                        mCurrentCharacter = Tools.qiParam();
                        break;
                    case 1:
                        mCurrentCharacter = Tools.resetDefalutParam();//恢复出厂设置的指令
                        break;
                    case 2:
                        mCurrentCharacter = Tools.resetParam();//重启设备的指令
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    /**
     * @author cui
     * @function 扫描设备
     * @param enable
     */
    public void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    Log.d(LOG_SCAN, "******Stopping BLEScan*****");
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mWaitingDialog.dismiss();
                    showDevicesDialog();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            Log.d(LOG_SCAN, "*****Starting BLEScan*****");
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            mWaitingDialog.show();
        } else {
            mScanning = false;
            Log.d(LOG_SCAN, "******Stopping BLEScan*****");
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mWaitingDialog.dismiss();
        }
    }


    /**
     * @author cui
     * @function Device scan callback.
      */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(LOG_SCAN, "Found a device");
            devices.add(device);
        }
    };

    /**
     * @author cui
     * @function 显示加载数据的动画
     */
    @UiThread
    private void showDevicesDialog() {
        Log.d(LOG_SCAN, "Showing devices dialog");
        mDevicesDialog = BLEDevicesDialog.newInstance(devices);
        mDevicesDialog.show(getSupportFragmentManager(), "dialogdevices");
    }

    /**
     * @author cui
     * @function 连接到ble设备
     * @param p_device
     */

    @Override
    public void connectToBLEDevice(BluetoothDevice p_device) {
        mDevicesDialog.dismiss();
        mWaitingDialog.setMessage("Connecting to Device");
        mWaitingDialog.show();
        mBluetoothGatt = p_device.connectGatt(this, false, mGattCallback);
    }

    /**
     * @author cui
     * @function 连接到指定的设备
     */
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(LOG_GATT, "Characteristic " + characteristic.getUuid().toString() + " value : " + Arrays.toString(characteristic.getValue()));
                LogUtils.logInfo("Read characteristic value " + Arrays.toString(characteristic.getValue()) + " in FFF1");
            } else {
                LogUtils.logInfo("Couldnt read characteristic " + mNotifyUUID.toString());
            }

            mWaitingForResult = false;
        }

        //调用mBluetoothGatt.writeCharacteristic(writeChar);会主动回调这个方法
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                LogUtils.logInfo("Command wroten");
                Log.d(LOG_GATT, "Characteristic " + characteristic.getUuid().toString() + " write : " + Arrays.toString(characteristic.getValue()));
            }
        }
        //数据返回的回调（此处接收BLE设备返回数据）
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            LogUtils.logInfo(characteristic.getUuid().toString() + " value have been updated :\nValue = " + Arrays.toString(characteristic.getValue()));
        }
        //连接状态的变化，连接成功或者连接失败
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {

                mIsConnected = true;
                Log.i(LOG_GATT, "******Connected to GATT server*****");
                mWaitingDialog.dismiss();
                Log.i(LOG_GATT, "Attempting to start service discovery");
                mBluetoothGatt.discoverServices();
                mBtWrite.setVisibility(View.VISIBLE);
                //showVsCharacter();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                disconnectFromBLEDevice();
                mIsConnected = false;
                Log.i(LOG_GATT, "******Disconnected from GATT server******");
            }
        }

        //发现服务
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(LOG_GATT, "******Services discovered*****");
                for (BluetoothGattService service : gatt.getServices()) {
                    Log.i(LOG_GATT, "UUID : " + service.getUuid().toString());
                    LogUtils.logInfo(service.getUuid().toString());
                }
                suscribeToNotification();

            }
        }

        //调用writeDescriptor方法后会主动回调下面这个方法
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (descriptor.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) {
                    LogUtils.logInfo("Subscribed");
                    mBtWrite.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            writeOBDCommand(Tools.resetParam());
                        }
                    });
                }
            } else {
                LogUtils.logInfo("Failed Subscribed");
            }
        }
    };

    /**
     * @author cui
     * @function 连接设备失败
     */
    public void disconnectFromBLEDevice() {
        mWaitingDialog.dismiss();
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }


    /**
     * @author cui
     * @function 开启notify才能接收到ble设备返回的信息
     * 1、通过服务端和通知指定的uuid获取到描述符（说明命令是用来干嘛的，这里是用来开启notify）的特征
     * 2、传入指定的特征开启接收通知的功能
     * 3、通过通知的uuid获取到描述，调用setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
     * 4、写入到ble服务端使之生效
     */
    private void suscribeToNotification() {
        //subscribe to notification
        BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(mServiceUUID).
                getCharacteristic(mNotifyUUID);
        mBluetoothGatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    /**
     * @author cui
     * @function 写入命令
     * @param p_command 需要发送的指令
     */
    private void writeOBDCommand(String p_command) {
        byte[] command = null;
        try{
            command = p_command.getBytes("UTF-8");
        }catch (Exception ignored){

        }
        if (mIsConnected) {
//            while (mWaitingForResult) {
//                try {
//                    this.wait(50);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
            BluetoothGattCharacteristic writeChar = mBluetoothGatt.getService(mServiceUUID)
                    .getCharacteristic(mWriteUUID);

            LogUtils.logInfo("About to write " + p_command + " in FFF1");

            writeChar.setValue(command);
            mBluetoothGatt.writeCharacteristic(writeChar);
            mWaitingForResult = true;

            //mCurrentCommand = p_command;
        } else {
            Toast.makeText(this, "Can't write OBD Command\nNot Connected", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //清除回调和所有的信息，防止内存泄漏
        mHandler.removeCallbacksAndMessages(null);
    }
}
