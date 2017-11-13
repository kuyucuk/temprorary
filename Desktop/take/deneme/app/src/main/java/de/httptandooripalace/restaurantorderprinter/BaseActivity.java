package de.httptandooripalace.restaurantorderprinter;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import helpers.SharedPrefHelper;
import com.smartdevice.aidl.IZKCService;


public class BaseActivity extends AppCompatActivity {

    public ProgressDialog loadingDialog;
    public Toast currentToast;
    public static String MODULE_FLAG = "module_flag";
    public static int module_flag = 0;
    public static int DEVICE_MODEL = 0;
    ScreenOnOffReceiver mReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        module_flag = getIntent().getIntExtra(MODULE_FLAG, 8);
        bindService();
        mReceiver = new ScreenOnOffReceiver();
        IntentFilter screenStatusIF = new IntentFilter();
        screenStatusIF.addAction(Intent.ACTION_SCREEN_ON);
        screenStatusIF.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, screenStatusIF);
    }
    public boolean bindSuccessFlag = false;
    public static IZKCService mIzkcService;
    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("client", "onServiceDisconnected");
            mIzkcService = null;
            bindSuccessFlag = false;
            create_toast("fail!");
        }


        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("client", "onServiceConnected");
            mIzkcService = IZKCService.Stub.asInterface(service);
            if(mIzkcService!=null){
                try {
                    create_toast("connected");
                    DEVICE_MODEL = mIzkcService.getDeviceModel();
                    mIzkcService.setModuleFlag(module_flag);
                    if(module_flag==3){
                        mIzkcService.openBackLight(1);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                bindSuccessFlag = true;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        bindService();
    }
    public void bindService() {
        //com.zkc.aidl.all为远程服务的名称，不可更改
        //com.smartdevice.aidl为远程服务声明所在的包名，不可更改，
        // 对应的项目所导入的AIDL文件也应该在该包名下
        Intent intent = new Intent("com.zkc.aidl.all");
        intent.setPackage("com.smartdevice.aidl");
        bindService(intent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    public void unbindService() {
        unbindService(mServiceConn);
    }

    protected void showLoading(String title, String message) {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(this, R.style.DialogTheme);
            loadingDialog.setTitle(title);
            if (Build.VERSION.SDK_INT < 21) {
                loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
            loadingDialog.setMessage(message);
            loadingDialog.setCancelable(false);
            loadingDialog.setIndeterminate(false);
        } else {
            loadingDialog.setMessage(message);
        }

        if (!loadingDialog.isShowing())
            loadingDialog.show();
    }

    protected void showLoading() {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(this, R.style.DialogTheme);
            loadingDialog.setTitle(getString(R.string.title_please_wait));
            if (Build.VERSION.SDK_INT < 21) {
                loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
            loadingDialog.setMessage(getString(R.string.title_processing));
            loadingDialog.setCancelable(false);
            loadingDialog.setIndeterminate(false);
        }
        if (!loadingDialog.isShowing())
            loadingDialog.show();
    }

    protected void hideLoading() {

        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog.cancel();
        }
    }


    protected void processRequest() {
        showLoading();
    }

    //creates a toast message
    public void create_toast(String msg) {
        if (currentToast != null) currentToast.cancel();
        currentToast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
        currentToast.show();
    }

    public class ScreenOnOffReceiver extends BroadcastReceiver {

        private static final String TAG = "ScreenOnOffReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.SCREEN_ON")) {
//				SCREEN_ON = true;
                try {
                    //打开电源
                    mIzkcService.setModuleFlag(8);
//					SystemClock.sleep(1000);
                    mIzkcService.setModuleFlag(module_flag);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else if (action.equals("android.intent.action.SCREEN_OFF")) {
//				SCREEN_ON = false;
//				try {
//					//关闭电源
//					mIzkcService.setModuleFlag(9);
//				} catch (RemoteException e) {
//					e.printStackTrace();
//				}
            }
        }
    }
}
