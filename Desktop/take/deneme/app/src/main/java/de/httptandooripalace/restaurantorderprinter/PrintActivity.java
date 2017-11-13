package de.httptandooripalace.restaurantorderprinter;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.smartdevice.aidl.IZKCService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;
import entities.Bill;
import entities.Product;
import entities.Settings;
import helpers.DatabaseHelper;
import helpers.PrintAdapter;
import helpers.RequestClient;
import helpers.Rounder;
import helpers.SharedPrefHelper;
import helpers.StringHelper;
import helpers.Printer;
import interfaces.OnItemClickedListener;


import helpers.Tax_calculator;
import util.ExecutorFactory;

import static android.R.attr.name;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;
import static de.httptandooripalace.restaurantorderprinter.R.string.bill;
import static de.httptandooripalace.restaurantorderprinter.R.string.total;

public class PrintActivity extends BaseActivity implements OnItemClickedListener,View.OnClickListener {

    private static final int GOODS_PRICE_LENGTH = 6;
    private List<Product> products = new ArrayList<Product>();
    public static List<Product> added_products = new ArrayList<Product>();
    public static List<Integer> integer = new ArrayList<>();


    private final int CHARCOUNT_BIG = 48; // Amount of characters fit on one printed line, using $big$ format
    private final int CHARCOUNT_BIGW = 24; // Amount of characters fit on one printed line, using $bigw$ format

    private final String INITIATE = "·27··64·"; // ESC @
    private final String CHAR_TABLE_EURO = "·27··116··19·"; // ESC t 19 -- 19 for euro table
    private final String EURO = "·213·";
    private final String DOT = "·46·";
    private final String BR = "$intro$"; // Line break
    private final String u = "·129·";
    private final String U = "·154·";
    private final String HEADER_FONT = "·27··33··32·";
    Context context;
    static int bill_nr = 0;
    public static String tableNr = "";
    public static String waiter_name = "";

    private static Activity activity = null;
    private Button new_b, new_b2, new_b3;


    private static DatabaseHelper db;
    private static Printer pr;

    //comment
    private Settings settings;
    private boolean runFlag = true;
    Button button;
    private static final int REQUEST_EX = 1;

    private Toast currentToast;

    private static String MODULE_FLAG = "module_flag";
    private static int module_flag = 0;
    private static int DEVICE_MODEL = 0;

    private boolean bindSuccessFlag = false;
    private static IZKCService mIzkcService;
    ScreenOnOffReceiver mReceiver = null;

    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("client", "onServiceDisconnected");
            mIzkcService = null;
            bindSuccessFlag = false;
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

    public void bindService() {
        Intent intent = new Intent("com.zkc.aidl.all");
        intent.setPackage("com.smartdevice.aidl");
        bindService(intent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    public void unbindService() {
        unbindService(mServiceConn);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.print_activity);
        hideLoading();
        settings = SharedPrefHelper.loadSettings(getApplicationContext());
        context = this;
        activity = this;
        db = new DatabaseHelper(this);
        pr = new Printer();
        bindService();
        new_b = (Button) findViewById(R.id.add_products);
        new_b.setOnClickListener(add_products(new_b));//setting listener for button

        new_b2 = (Button) findViewById(R.id.split_bills);
        new_b2.setOnClickListener(split_bills(new_b2));


        findViewById(R.id.print_kitchen_bar).setOnClickListener(this);


//        printKitchenBill();
//        printDrinkBill();
//        added_products.clear();


        // Get products
        // products = SharedPrefHelper.getPrintItems(getApplicationContext());


        if (settings == null) {
            settings = new Settings();
            SharedPrefHelper.saveSettings(getApplicationContext(), settings);
        }


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            bill_nr = extras.getInt("bill_nr");
            tableNr = extras.getString("tableNr");
            Log.d("RESPONSE", "BILL NUMBER : " + bill_nr);
        } else {
            Log.d("RESPONSE", "NO BILL NR : " + bill_nr);
        }

        try {
                showLoading();
                StringEntity entity;
                JSONObject jsonParams = new JSONObject();
                Log.d("RESPONSE", "GETTING BILL PRODUCTS");
                RequestParams params = new RequestParams();
                jsonParams.put("bill_id", bill_nr);
                entity = new StringEntity(jsonParams.toString());
                RequestClient.post(context, "products/getforbill/", entity, "application/json", new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        // If the response is JSONObject instead of expected JSONArray


                        try {
                            Log.d("RESPONSE", response.getJSONArray("products").toString()); // RESPONSE: {"success":"true","products":[{"id_cat":"18","name_cat":" Dienstag","id_prod":"371","name_prod":"Chicken Curry","reference_prod":"512,","price_prod_excl":"4.03","price_prod_incl":"4.32","description_prod":"","bill_id":"1"},
                            JSONArray jsonarray = response.getJSONArray("products");
                            for (int i = 0; i < jsonarray.length(); i++) {
                                JSONObject jsonobject = jsonarray.getJSONObject(i);
                                String name = jsonobject.getString("name_prod");
                                int id = jsonobject.getInt("id_prod");
                                double price_excl = jsonobject.getDouble("price_prod_excl");
                                double price_incl = jsonobject.getDouble("price_prod_incl");
                                String reference = jsonobject.getString("reference_prod");
                                String category = jsonobject.getString("name_cat");
                                int count = jsonobject.getInt("count");
                                Product p = new Product(id, name, price_excl, price_incl, reference, category, count);
                                products.add(p);
                            }
                        } catch (Exception e) {
                            Log.d("Exception HTTP", e.getMessage());
                        }
                        // Bind products to print overview
                        loadData();
                    }

                    @Override
                    public void onFailure(int c, Header[] h, String r, Throwable t) {
                        try {
                            Log.d("RESPONSE", r.toString());
                        } catch (Exception e) {
                            Log.d("Exception HTTP", e.getMessage());
                        }
                        hideLoading();
                    }
                });

        } catch (Exception e) {
            Log.d("Ex", e.getMessage());

        }

        button=(Button)findViewById(R.id.button_1);

        ExecutorFactory.executeThread(new Runnable() {
            @Override
            public void run() {
                while(runFlag){
                    if(bindSuccessFlag){
                        //检测打印是否正常
                        try {
                            String printerSoftVersion = mIzkcService.getFirmwareVersion1();
                            if(TextUtils.isEmpty(printerSoftVersion)){
                                printerSoftVersion = mIzkcService.getFirmwareVersion2();
                            }
                            if(TextUtils.isEmpty(printerSoftVersion)){
                                mIzkcService.setModuleFlag(module_flag);
                                mHandler.obtainMessage(1).sendToTarget();
                            }else{
                                mHandler.obtainMessage(0, printerSoftVersion).sendToTarget();
                                runFlag = false;
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    String status;
                    String aidlServiceVersion;
                    try {
//					mIzkcService.sendRAWData("printer", new byte[] {0x1b, 0x40});
                        status = mIzkcService.getPrinterStatus();

                        aidlServiceVersion = mIzkcService.getServiceVersion();
                        Log.d("",(msg.obj + "AIDL Service Version:" + aidlServiceVersion));
                        create_toast(aidlServiceVersion);
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                case 1:
                    create_toast("waiting");
                    break;
                case 8:
//				showProgressDialog("waiting...");
//				new Timer().schedule(new TimerTask() {
//					@Override
//					public void run() {
//						dismissLoadDialog();
//					}
//				}, 8000);
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_1:
                printVamplify();
                break;
            case R.id.print_kitchen_bar:
                printKitchenBill();
                //printDrinkBill();
                break;
        }
    }
    private void printVamplify() {
        try {
            Log.d("status",mIzkcService.getPrinterStatus());
            if (mIzkcService.checkPrinterAvailable()==true){
                //mIzkcService.printTextWithFont("Sample text", 0, 2);
                display_customer_info();
                printPurchaseBillModelOne(mIzkcService);
            }
        } catch (RemoteException e) {
            Log.e("", "远程服务未连接...");
            e.printStackTrace();
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EX && resultCode == RESULT_OK
                && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();;
            cursor.close();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    protected void onDestroy() {
        unbindService();
        super.onDestroy();
    }

    synchronized public void printPurchaseBillModelOne(
            IZKCService mIzkcService) {

        try {
            if (mIzkcService!=null&&mIzkcService.checkPrinterAvailable()) {
                mIzkcService.printGBKText(settings.getNameLine1()+"\n\n");

                SystemClock.sleep(50);
//				mIzkcService.printGBKText("\n");
                mIzkcService.printGBKText(getString(R.string.table_nr).toUpperCase() + tableNr + "\n\n");
                double totalPriceExcl = 0;
                double totalPriceIncl = 0;
                String count;
                mIzkcService.printTextWithFont("\n"+getLineZKC("=",16),0,2);
                for (int i = 0; i < products.size(); i++) {
                    if (products.get(i).getCount() < 1) continue;

                    double priceEx = products.get(i).getPrice_excl();
                    double priceInc = Tax_calculator.calculate_tax(products.get(i).getPrice_excl(),tableNr);

                    totalPriceExcl += (priceEx * products.get(i).getCount());
                    totalPriceIncl += (priceInc * products.get(i).getCount());

                    String space="";
                    int line_length=24+6;
                    int name_space=22;

                    String name = products.get(i).getName();
                    String price = Rounder.round(products.get(i).getCount() * Tax_calculator.calculate_tax(products.get(i).getPrice_excl(),tableNr));

                    int name_length = name.length();

                    String name_p1;
                    String name_p2="";
                    String newline="\n";
                    String[] splitted;
                    if (name_length>name_space){
                        splitted = name.split("\\s+");
                            name_p1=splitted[0];
                            name_p2=name.substring(name_p1.length());
                            for (int j =1;j<splitted.length;j++){
                                if (name_p1.length()+splitted[j].length()<name_space){
                                    name_p1+=" "+splitted[j];
                                    name_p2=name.substring(name_p1.length()+1);
                                }
                                else{
                                    break;
                                }
                            }
                    }else{
                        newline="";
                        name_p1=name;
                    }

                    int space_length= line_length
                            -name_p1.length()
                            -GOODS_PRICE_LENGTH
                            -1;
                    for (int j = 0; j < space_length; j++) {
                        space+= " ";
                    }
                    mIzkcService.printTextWithFont(name_p1 + space + price+newline+name_p2 ,0,0);
                    mIzkcService.printTextWithFont("\n"+getLineZKC("-",16),0,2);
                    }
                mIzkcService.printTextWithFont("\n"+getLineZKC("=",16),0,2);
                }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public String getLineZKC(String s,int length){
        String symbols="";
        for (int i=0;i<length;i++){
            symbols+=s;
        }
        return symbols;
    }

    private void loadData() {
        RecyclerView view = (RecyclerView) findViewById(R.id.listingLayout);
        view.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        PrintAdapter adapter = new PrintAdapter(this, products, this);
        view.setAdapter(adapter);
        hideLoading();
    }

    View.OnClickListener add_products(final Button button) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(context, MainActivity.class);
                i.putExtra("bill_nr", bill_nr);
                i.putExtra("tableNr", tableNr);
                i.putExtra("waiter_name", waiter_name);
                startActivity(i);
                finish();
            }
        };
    }

    View.OnClickListener split_bills(final Button button) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                //Spliting Bill Function
                Intent i = new Intent(context, SplitActivity.class);
                i.putExtra("bill_nr", bill_nr);
                i.putExtra("tableNr", tableNr);
                i.putExtra("waiter_name", waiter_name);
                startActivity(i);
                finish();
            }
        };
    }

    View.OnClickListener printKitchen_Bar_Bills(final Button button) {
        return new View.OnClickListener() {
            public void onClick(View v) {

                printKitchenBill();
                printDrinkBill();
                added_products.clear();
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void display_customer_info() {
        Bitmap bm = getBitmap();
        try {
            if (mIzkcService.showDotImage(Color.YELLOW, Color.BLACK,
                    bm)) {

            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private Bitmap getBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(480, 272, Bitmap.Config.RGB_565);
        TextPaint mTextPaint=new TextPaint();
        Canvas canvas = new Canvas(bitmap);
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize(21);

        int line_length=50;

        String sum="";
        String ss="";
        int total=0;
        for (int i = 0; i < products.size(); i++) {
            String space="";
            int name_length = products.get(i).getName().length();

            int space_length= line_length
                    -name_length
                    -GOODS_PRICE_LENGTH
                    -1;
            for (int j = 0; j < space_length; j++) {
                space+= " ";
            }
            if (products.get(i).getCount() < 1) continue;
            ss+=products.get(i).getName()+"\n";
            sum += Rounder.round(products.get(i).getCount() * Tax_calculator.calculate_tax(products.get(i).getPrice_excl(),tableNr))+"\n";
            total+=products.get(i).getCount() * Tax_calculator.calculate_tax(products.get(i).getPrice_excl(),tableNr);
        }
        String total_sum = String.valueOf(total);

        int x = 20, y = 30;
        canvas.drawColor(Color.WHITE);
        for (String line: ss.split("\n")) {
            canvas.drawText(line, x, y, mTextPaint);
            y += mTextPaint.descent() - mTextPaint.ascent();
        }
        x = 380;
        y = 30;
        for (String line: sum.split("\n")) {
            canvas.drawText(line, x,y , mTextPaint);
            y += mTextPaint.descent() - mTextPaint.ascent();
        }
        mTextPaint.setTextSize(30);
        canvas.drawText(total_sum, x,y+30 , mTextPaint);

        // imageView1.setImageBitmap(bitmap);
        return bitmap;
    }

    @Override
    // Add settings menu icon to toolbar
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.print, menu);
        return true;
    }

    //@Override
    // Open settings menu
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                onBackPressed();
                return true;
//            case R.id.print_bill:
//                printBill(item);
//                return true;
//            case R.id.print_tax:
//                printTaxBill(item);
//                return true;
//            case R.id.print_drink:
//                printDrinkBill(item);
//                return true;
//            case R.id.print_kitchen:
//                printKitchenBill(item);
//                return true;
            case R.id.print_bills:
                printBill(item);
                printTaxBill(item);


                //Print Drink and Kitchen Bills are moved to the main function to print them when the bill created or a product added
                //These printDrinkBill and printKitchenBill function are old ones please see the ones in Main Activity (with the same name)
                //They only used when to print the added product with + and - signes

                /*printDrinkBill(item);
                printKitchenBill(item);
                 */

                return true;
//            case R.id.print_kitchen:
//                Log.d("DOT TEST SHOULD BE ONE", checkCount("One time euro " + DOT, DOT) + "");
//                Log.d("DOT TEST SHOULD BE TWO", checkCount("One time " + DOT + "euro " + DOT, DOT) + "");
//                Log.d("SHOULD BE FIVE", checkCount(DOT + DOT + "One t " + DOT + "ime euro " + DOT + DOT, DOT) + "");
//                return true;
            case R.id.bills_overview:
                Intent i2 = new Intent(this, OverviewActivity.class);
                startActivity(i2);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Delete the print overview and refresh activity
    public void deletePrintOverview(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.delete_overview);
        builder.setMessage(R.string.overview_will_deleted + "\n" + R.string.are_you_sure);

        builder.setPositiveButton(getText(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SharedPrefHelper.deleteSharedPrefs(getApplicationContext());
                finish();
                startActivity(getIntent());
                dialog.dismiss();

            }
        });

        builder.setNegativeButton(getText(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();

    }

    public void test(MenuItem item) {
        // Do print job button clicked

        //doWebViewPrint(ids, names, prices);
        //String dataToPrint="$big$This is a printer test$intro$posprinterdriver.com$intro$$intro$$cut$$intro$";

//
//
//        //String textToSend="$intro$$big$Test test 123 test$intro$$intro$$intro$";
//        String textToSend="$intro$$intro$$intro$$big$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA$intro$$intro$$intro$$intro$$intro$$small$BBBBBBBBBBBBBBBBBBBB$intro$$intro$$intro$";
//        Intent intentPrint = new Intent();
//        intentPrint.setAction(Intent.ACTION_SEND);
//        intentPrint.putExtra(Intent.EXTRA_TEXT, textToSend);
//        intentPrint.putExtra("printer_type_id", "1");// For IP
//        intentPrint.putExtra("printer_ip", "192.168.178.105");
//        intentPrint.putExtra("printer_port", "9100");
//
//        intentPrint.setType("text/plain");
//        Log.i("Print job log: ", "sendDataToIPPrinter Start Intent");
//
//        this.startActivity(intentPrint);
//

        StringBuilder strb = new StringBuilder();

        strb.append("<BIG>Bill<BR><BR>");


        strb.append("testestestestestestse");
        strb.append("<BR><BR>");

        Intent intent = new Intent("pe.diegoveloper.printing");
        //intent.setAction(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, strb.toString());
        startActivity(intent);


    }

    public void printTaxBill(MenuItem item) {
        if (products == null) return;
        if (products.size() <= 0) return;

        sendPrintJob(getBillContent() + getTaxFooter());
    }

    public void printBill(MenuItem item) {
        if (products == null) return;
        if (products.size() <= 0) return;

        sendPrintJob(getBillHeader() + getBillContent() + getBillFooter());
    }

    public String getBillHeader() {
        StringBuilder strb = new StringBuilder("");
        //strb.append("$bighw$");
        strb.append(HEADER_FONT);
        strb.append(pr.getAppend(settings.getNameLine1()));
        strb.append(pr.getAppend(settings.getNameLine2()));
        strb.append(pr.getAppend(settings.getAddrLine1()));
        strb.append(pr.getAppend(settings.getNameLine2()));
        strb.append(pr.getAppend(settings.getTelLine()));
        strb.append(pr.getAppend(settings.getTaxLine()));
        return strb.toString();
    }

    public void printDrinkBill() {
        if (added_products == null) return;
        if (added_products.size() <= 0) return;

        //String tableNr = SharedPrefHelper.getString(getApplicationContext(), "tableNr");
        String s;
        StringBuilder strb = new StringBuilder("");
        int number_drinks = 0;

        strb.append(INITIATE);
        strb.append(CHAR_TABLE_EURO);
        strb.append(BR);

        // tableNr = "randomThingToTestIfThePrintWorksWithATableNr";
        if (!tableNr.equals("")) {
            strb.append("$bighw$");
            strb.append(getString(R.string.table_nr).toUpperCase() + tableNr);
            strb.append(BR);
        }

        strb.append("$big$");
        strb.append(BR);
        strb.append(pr.getAlignCenter(getString(R.string.drink).toUpperCase()));


        strb.append(BR + "$big$" + BR);
        strb.append(getLineOf('=', CHARCOUNT_BIG));

        double totalPriceExcl = 0;
        double totalPriceIncl = 0;

        for (int i = 0; i < added_products.size(); i++) {
            if (added_products.get(i).getCount() < 1) continue;

            if (added_products.get(i).getDrink()) {
                // Don't print it when it is the first time and number_drinks == 0
                if (!(number_drinks == 0))
                    strb.append(getLineOf('-', CHARCOUNT_BIG));

                number_drinks++;
                double priceEx = added_products.get(i).getPrice_excl();
                double priceInc = added_products.get(i).getPrice_incl();

                strb.append(BR);

                if (added_products.get(i).getCount() != 1) {
                    // 2 x 2.15
                    strb.append("$bighw$");
                    s = added_products.get(i).getCount() + " x ";
                    strb.append(s);
                    strb.append("$big$");
                    s = Rounder.round(added_products.get(i).getPrice_incl());
                    strb.append(s);
                    strb.append(BR);
                }
                s = "#" + added_products.get(i).getReference() + " ";
                strb.append(s);
                strb.append(BR);
                // All Star Product                 4.30
                strb.append("$bighw$");
                s = StringHelper.swapU(added_products.get(i).getName().toUpperCase());
                strb.append(s);
                strb.append("$big$");
                String totalPriceForThisProduct = Rounder.round(added_products.get(i).getCount() * added_products.get(i).getPrice_incl());
                s = pr.getAlignRightSpecial((totalPriceForThisProduct), added_products.get(i).getName().length());
                strb.append(s);
                strb.append(BR);


                totalPriceExcl += (priceEx * added_products.get(i).getCount());
                totalPriceIncl += (priceInc * added_products.get(i).getCount());
            }
        }
        strb.append(getLineOf('=', CHARCOUNT_BIG));
        strb.append(BR);

        // Date
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

        strb.append("$big$" + BR + BR);
        strb.append(currentDateTimeString);
        strb.append(" " + getString(R.string.waiter) + " " + settings.getWaiter());
        strb.append(BR);
        //strb.append("Bondrucker1"); //Don't need that :O

        for (int i = 0; i < 8; i++) {
            strb.append(BR);
        }
        strb.append("$cut$");

        if (number_drinks > 0) sendPrintJob(strb.toString());
    }

    public void printKitchenBill() {
        if (added_products == null) return;
        if (added_products.size() <= 0) return;

        // String tableNr = SharedPrefHelper.getString(getApplicationContext(), "tableNr");
        String s;
        StringBuilder strb = new StringBuilder("");
        int number_kitchen = 0;

        strb.append(INITIATE);
        strb.append(CHAR_TABLE_EURO);
        strb.append(BR);

        //tableNr = "randomThingToTestIfThePrintWorksWithATableNr";
        if (!tableNr.equals("")) {
            strb.append("$bighw$");
            strb.append(getString(R.string.table_nr).toUpperCase() + tableNr);
            strb.append(BR);
        }

        strb.append("$big$");
        strb.append(BR);
        strb.append(pr.getAlignCenter(StringHelper.swapU(getString(R.string.kitchen).toUpperCase())));


        strb.append(BR + "$big$" + BR);
        strb.append(getLineOf('=', CHARCOUNT_BIG));

        double totalPriceExcl = 0;
        double totalPriceIncl = 0;

        for (int i = 0; i < added_products.size(); i++) {
            if (added_products.get(i).getCount() < 1) continue;

            if (!added_products.get(i).getDrink()) {
                if (!(number_kitchen == 0))
                    strb.append(getLineOf('-', CHARCOUNT_BIG));

                number_kitchen++;

                double priceEx = added_products.get(i).getPrice_excl();
                double priceInc = added_products.get(i).getPrice_incl();

                strb.append(BR);

                if (added_products.get(i).getCount() != 1) {
                    // 2 x 2.15
                    strb.append("$bighw$");
                    s = added_products.get(i).getCount() + " x ";
                    strb.append(s);
                    strb.append("$big$");
                    s = Rounder.round(added_products.get(i).getPrice_incl());
                    strb.append(s);
                    strb.append(BR);
                }
                s = "#" + added_products.get(i).getReference() + " ";
                strb.append(s);
                strb.append(BR);
                // All Star Product                 4.30
                strb.append("$bighw$");
                s = StringHelper.swapU(added_products.get(i).getName().toUpperCase());
                strb.append(s);
                strb.append("$big$");
                String totalPriceForThisProduct = Rounder.round(added_products.get(i).getCount() * added_products.get(i).getPrice_incl());
                s = pr.getAlignRightSpecial((totalPriceForThisProduct), added_products.get(i).getName().length());
                strb.append(s);
                strb.append(BR);


                totalPriceExcl += (priceEx * added_products.get(i).getCount());
                totalPriceIncl += (priceInc * added_products.get(i).getCount());
            }
        }
        strb.append(getLineOf('=', CHARCOUNT_BIG));
        strb.append(BR);

        // Date
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

        strb.append("$big$" + BR + BR);
        strb.append(currentDateTimeString);
        strb.append(" " + getString(R.string.waiter) + " " + settings.getWaiter());
        strb.append(BR);
        //strb.append("Bondrucker1"); //Don't need that :O

        for (int i = 0; i < 8; i++) {
            strb.append(BR);
        }
        strb.append("$cut$");

        if (number_kitchen > 0) sendPrintJob(strb.toString());
    }

    // sendPrintJob bill layout
    public String getBillContent() {
        //String tableNr = SharedPrefHelper.getString(getApplicationContext(), "tableNr");
        String s;
        StringBuilder strb = new StringBuilder("");

        strb.append(INITIATE);
        strb.append(CHAR_TABLE_EURO);
        strb.append(BR);


        s = "$bighw$" + BR + pr.getAlignCenterBigw(getString(bill)) + BR;
        strb.append(s);

        // tableNr = "randomThingToTestIfThePrintWorksWithATableNr";
        if (!tableNr.equals("")) {
            strb.append("$bighw$");
            strb.append(pr.getAlignCenterBigw(getString(R.string.table_nr).toUpperCase() + tableNr));
            strb.append(BR);
        }

        strb.append(BR + "$big$" + BR);
        strb.append(getLineOf('=', CHARCOUNT_BIG));

        double totalPriceExcl = 0;
        double totalPriceIncl = 0;

        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getCount() < 1) continue;

            double priceEx = products.get(i).getPrice_excl();
            double priceInc = Tax_calculator.calculate_tax(products.get(i).getPrice_excl(),tableNr);

            strb.append(BR);

            if (products.get(i).getCount() != 1) {
                // 2 x 2.15
                s = products.get(i).getCount() + " x " + Rounder.round(Tax_calculator.calculate_tax(products.get(i).getPrice_excl(),tableNr));
                strb.append(s);
                strb.append(BR);
            }
            // All Star Product                 4.30
            String totalPriceForThisProduct = Rounder.round(products.get(i).getCount() * Tax_calculator.calculate_tax(products.get(i).getPrice_excl(),tableNr));
            s = StringHelper.swapU(products.get(i).getName().toUpperCase())
                    + pr.getAlignRight((totalPriceForThisProduct), products.get(i).getName().length());
            strb.append(s);
            strb.append(BR);

            // Not on last line
            if (i != products.size() - 1)
                strb.append(getLineOf('-', CHARCOUNT_BIG));

            totalPriceExcl += (priceEx * products.get(i).getCount());
            totalPriceIncl += (priceInc * products.get(i).getCount());

        }

        strb.append(getLineOf('=', CHARCOUNT_BIG));
        strb.append(BR);

        // Total excl
        s = getString(R.string.price_excl) +
                pr.getAlignRight((EURO + Rounder.round(totalPriceExcl)), (getString(R.string.price_excl)).length());
        strb.append(s);
        strb.append(BR);

        // Tax
        String tax = Rounder.round(totalPriceIncl - totalPriceExcl);
        s = getString(R.string.tax) +
                pr.getAlignRight((EURO + tax), (getString(R.string.tax)).length());
        strb.append(s);

        // Total incl
        strb.append(BR);
        strb.append("$bigw$");
        String totalPriceInc = Rounder.round(totalPriceIncl);

        s = getString(total) +
                pr.getAlignRightBigw((EURO + totalPriceInc), (getString(total)).length());
        strb.append(s);

        // Date
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

        strb.append("$big$" + BR + BR);
        strb.append(currentDateTimeString);
        s = " " + getString(R.string.waiter) + " " + settings.getWaiter();
        strb.append(s);
        strb.append(BR + BR);
        return strb.toString();
    }


    private String getBillFooter() {
        StringBuilder strb = new StringBuilder("");
        strb.append("$big$");

        // Served by waiter
        if (!settings.getWaiter().equals(""))
            strb.append(pr.getAlignCenter(getString(R.string.served_by) + " " + settings.getWaiter()) + "$intro$");

        strb.append(pr.getAlignCenter(getString(R.string.tyvm)) + "$intro$");
        strb.append(pr.getAlignCenter(getString(R.string.visit_again)));

        for (int i = 0; i < 8; i++) {
            strb.append(BR);
        }
        strb.append("$cut$");
        return strb.toString();
    }

    private String getTaxFooter() {
        StringBuilder strb = new StringBuilder("");
        strb.append("$big$");

        strb.append(getLineOf('*', CHARCOUNT_BIG));
        strb.append(BR);
        strb.append(pr.getAlignCenter("Bewirtungsaufwand-Angaben"));
        strb.append(BR);
        strb.append(pr.getAlignCenter("(Par.4 Abs.5 Ziff.2 EstG)"));
        strb.append(BR);
        strb.append(getLineOf('*', CHARCOUNT_BIG));
        strb.append(BR);
        strb.append(BR);

        strb.append("Bewirtete Person(en):");
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf('-', CHARCOUNT_BIG));
        strb.append(BR);

        strb.append("Anlass der Bewirtung:");
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf('-', CHARCOUNT_BIG));
        strb.append(BR);

        strb.append("Höhe der Aufwendungen:");
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append(BR);
        strb.append("bei Bewirtung im Restaurant");
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append(BR);
        strb.append("in anderen Fällen");
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf('-', CHARCOUNT_BIG));
        strb.append(BR);
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append(BR);
        strb.append("Ort      Datum");
        strb.append(BR);
        strb.append(BR);
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append("Unterschrift");
        strb.append(BR);

        for (int i = 0; i < 8; i++) {
            strb.append(BR);
        }
        strb.append("$cut$");
        return strb.toString();
    }

    private String getLineOf(char c, int lineSize) {
        StringBuilder strb = new StringBuilder("");
        for (int i = 0; i < lineSize; i++) {
            strb.append(c);
        }
        return strb.toString();
    }

    private String getLineOf(String s, int lineSize) {
        StringBuilder strb = new StringBuilder("");
        for (int i = 0; i < lineSize; i++) {
            strb.append(s);
        }
        return strb.toString();
    }

    private void sendPrintJob(String dataToPrint) {
        Intent intentPrint = new Intent();
        intentPrint.setAction(Intent.ACTION_SEND);
        intentPrint.putExtra(Intent.EXTRA_TEXT, dataToPrint);
//        intentPrint.putExtra("printer_type_id", "1");// For IP
//        intentPrint.putExtra("printer_ip", settings.getPrinterIp());
//        intentPrint.putExtra("printer_port", "9100");
        intentPrint.setType("text/plain");
        /*this.*/
        startActivity(intentPrint);
    }

    private void addProduct(Product p) {
        List<Bill> open_bills = OverviewActivity.bills;
        p.setCount(1);
        added_products.add(p);

        //int id_product = Integer.parseInt(view.getTag(R.string.id_tag).toString());
        //double price_tag = Double.parseDouble(view.getTag(R.string.price_tag).toString());

        int id_product = p.getId();
        double price_tag = p.getPrice_excl();

        try {
                StringEntity entity;

                JSONObject jsonParams = new JSONObject();
                jsonParams.put("bill_id", bill_nr);
                jsonParams.put("product_id", id_product);
                entity = new StringEntity(jsonParams.toString());
                RequestClient.put(context, "bills/product/", entity, "application/json", new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        // If the response is JSONObject instead of expected JSONArray
                        try {
                            System.out.println("Adding product to the bill : ");
                            Log.d("RESPONSE", response.toString());
                        } catch (Exception e) {
                            System.out.println("Adding product to the bill : ");
                            Log.d("Exception HTTP", e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        try {
                            Log.d("RESPONSE", errorResponse.toString());
                        } catch (Exception e) {
                            Log.d("Exception HTTP", e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(int c, Header[] h, String r, Throwable t) {
                        try {
                            Log.d("RESPONSE", r.toString());
                        } catch (Exception e) {
                            Log.d("Exception HTTP", e.getMessage());
                        }
                    }
                });

        } catch (Exception e) {
            Log.d("Ex", e.getMessage());

        }

        // adding the prod.getPrice() to the total bill price

        try {
                StringEntity entity;
                JSONObject jsonParams = new JSONObject();
                for (int y = 0; y <= open_bills.size(); y++) {
                    if (open_bills.get(y).getId() == bill_nr) {
                        open_bills.get(y).setTotal_price_excl(open_bills.get(y).getTotal_price_excl() + price_tag);
                        open_bills.get(y).setTableNr(tableNr);
                        open_bills.get(y).setWaiter(waiter_name);
                        OverviewActivity.bills = open_bills;
                        jsonParams.put("bill_id", bill_nr);
                        jsonParams.put("total_price_excl", open_bills.get(y).getTotal_price_excl());
                        break;
                    }
                }
                //b.setTotal_price_excl(b.getTotal_price_excl()+price_tag);

                entity = new StringEntity(jsonParams.toString());
                RequestClient.put(context, "bills/price/", entity, "application/json", new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        // If the response is JSONObject instead of expected JSONArray
                        try {
                            System.out.println("Updating price on the bill : ");
                            Log.d("RESPONSE", response.toString());
                        } catch (Exception e) {
                            System.out.println("Updating price on the bill : ");
                            Log.d("Exception HTTP", e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        try {
                            Log.d("RESPONSE", errorResponse.toString());
                        } catch (Exception e) {
                            Log.d("Exception HTTP", e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(int c, Header[] h, String r, Throwable t) {
                        try {
                            Log.d("RESPONSE", r.toString());
                        } catch (Exception e) {
                            Log.d("Exception HTTP", e.getMessage());
                        }
                    }
                });
        } catch (Exception e) {
            String err = (e.getMessage() == null) ? "SD Card failed" : e.getMessage();
            Log.e("sdcard-err2:", err);
            //Log.d("Ex", e.getMessage());

        }
        activity.finish();
        Intent login = new Intent(context, PrintActivity.class);
        context.startActivity(login);

    }


    public void removeProduct(Product p) {

        int id_product = p.getId();

        try {
            StringEntity entity;

            JSONObject jsonParams = new JSONObject();
            jsonParams.put("bill_id", bill_nr);
            jsonParams.put("product_id", id_product);
            entity = new StringEntity(jsonParams.toString());
            RequestClient.delete(context, "bills/product/", entity, "application/json", new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    // If the response is JSONObject instead of expected JSONArray
                    try {
                        System.out.println("deleting a product from the bill : ");
                        Log.d("RESPONSE", response.toString());
                    } catch (Exception e) {
                        System.out.println("deleting a product from the bill : ");
                        Log.d("Exception HTTP", e.getMessage());
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    try {
                        Log.d("RESPONSE", errorResponse.toString());
                    } catch (Exception e) {
                        Log.d("Exception HTTP", e.getMessage());
                    }
                }

                @Override
                public void onFailure(int c, Header[] h, String r, Throwable t) {
                    try {
                        Log.d("RESPONSE", r.toString());
                    } catch (Exception e) {
                        Log.d("Exception HTTP", e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            Log.d("Ex", e.getMessage());

        }

        activity.finish();
        Intent login = new Intent(context, PrintActivity.class);
        context.startActivity(login);
    }


    private void decreaseProduct(Product p) {


        for (int i = 0; i < added_products.size(); i++) {
            if (p.getId() == added_products.get(i).getId()) {
                added_products.remove(i);
                break;
            }
        }

        int id_product = p.getId();
        List<Bill> open_bills = OverviewActivity.bills;
        double price_tag = p.getPrice_excl();
        try {
                StringEntity entity;

                JSONObject jsonParams = new JSONObject();
                jsonParams.put("bill_id", bill_nr);
                jsonParams.put("product_id", id_product);
                entity = new StringEntity(jsonParams.toString());
                RequestClient.delete(context, "bills/decreaseProduct/", entity, "application/json", new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        // If the response is JSONObject instead of expected JSONArray
                        try {
                            System.out.println("Decreasing count of product on the bill : ");
                            Log.d("RESPONSE", response.toString());
                        } catch (Exception e) {
                            System.out.println("Decreasing count of product on the bill : ");
                            Log.d("Exception HTTP", e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        try {
                            Log.d("RESPONSE", errorResponse.toString());
                        } catch (Exception e) {
                            Log.d("Exception HTTP", e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(int c, Header[] h, String r, Throwable t) {
                        try {
                            Log.d("RESPONSE", r.toString());
                        } catch (Exception e) {
                            Log.d("Exception HTTP", e.getMessage());
                        }
                    }
                });
        } catch (Exception e) {
            Log.d("Ex", e.getMessage());

        }
        try {
                StringEntity entity;
                JSONObject jsonParams = new JSONObject();
                for (int y = 0; y <= open_bills.size(); y++) {
                    if (open_bills.get(y).getId() == bill_nr) {
                        open_bills.get(y).setTotal_price_excl(open_bills.get(y).getTotal_price_excl() - price_tag);
                        open_bills.get(y).setTableNr(tableNr);
                        open_bills.get(y).setWaiter(waiter_name);
                        OverviewActivity.bills = open_bills;
                        jsonParams.put("bill_id", bill_nr);
                        jsonParams.put("total_price_excl", open_bills.get(y).getTotal_price_excl());
                        break;
                    }
                }
                //b.setTotal_price_excl(b.getTotal_price_excl()+price_tag);

                entity = new StringEntity(jsonParams.toString());
                RequestClient.put(context, "bills/price/", entity, "application/json", new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        // If the response is JSONObject instead of expected JSONArray
                        try {
                            System.out.println("Updating price on the bill : ");
                            Log.d("RESPONSE", response.toString());
                        } catch (Exception e) {
                            System.out.println("Updating price on the bill : ");
                            Log.d("Exception HTTP", e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        try {
                            Log.d("RESPONSE", errorResponse.toString());
                        } catch (Exception e) {
                            Log.d("Exception HTTP", e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(int c, Header[] h, String r, Throwable t) {
                        try {
                            Log.d("RESPONSE", r.toString());
                        } catch (Exception e) {
                            Log.d("Exception HTTP", e.getMessage());
                        }
                    }
                });
        } catch (Exception e) {
            String err = (e.getMessage() == null) ? "SD Card failed" : e.getMessage();
            Log.e("sdcard-err2:", err);
            //Log.d("Ex", e.getMessage());

        }

        activity.finish();
        Intent login = new Intent(context, PrintActivity.class);
        context.startActivity(login);
    }

    @Override
    public void onItemClicked(View v, int position) {
        if (v.getId() == R.id.btnplus) {
            addProduct(products.get(position));
        } else if (v.getId() == R.id.btnminus) {
            decreaseProduct(products.get(position));
        }
    }
}

