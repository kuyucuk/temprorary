package de.httptandooripalace.restaurantorderprinter;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import Adapters.MainAdapter;
import Adapters.SearchAdapter;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;
import entities.Bill;
import entities.Product;
import entities.Settings;
import helpers.DatabaseHelper;
import helpers.RequestClient;
import helpers.Rounder;
import helpers.SharedPrefHelper;
import helpers.StringHelper;
import helpers.Tax_calculator;
import helpers.Printer;

import interfaces.attachListeners;

import static java.lang.Integer.parseInt;

public class MainActivity extends BaseActivity implements attachListeners {
    int x = 1;

    public void add_1(int number){
        number++;
    }

    private RecyclerView recyclerView;
    private Toast currentToast;
    private Context context;
    LinkedHashMap<String, List<Product>> prodlist2 = new LinkedHashMap<>();
    static int bill_nr = 0;
    static String table_nr = "#";
    static String waiter_name = "#";
    private entities.Settings settings;
    Bill b = null;
    Intent intent;
    List<Bill> open_bills = OverviewActivity.bills;
    List<Product> all_prods;
    Product selected;
    EditText search;
    Button searched_product;
    Product prod,prod2;
    public static List<Product> products = new ArrayList<Product>();
    private int added_product_id;
    EditText table_text;

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

    private static Printer pr;
    private attachListeners attachListeners;

    private JSONArray data = new JSONArray();
    private MainAdapter adapter;
    private ArrayList<String> catlist = new ArrayList<>();
    private LinkedHashMap<String, List<Product>> prodlist = new LinkedHashMap<>();
    private Bundle extras;
    private DatabaseHelper db;
    TextView lblTable_number;
    private List<MainAdapter.Item> dataToBind = new ArrayList<>();
    private List<Product> prods;
    private MainActivity myMainActivity;
    //ToggleButton tax_btn;


    @Override
    protected void onResume() {
        super.onResume();
    }

    private void add_table_nr(String s) {
        table_nr = s;
        b.setTableNr(table_nr);

            try {
                StringEntity entity;
                JSONObject jsonParams = new JSONObject();
                jsonParams.put("bill_id", bill_nr);
                jsonParams.put("table_nr", table_nr);
                entity = new StringEntity(jsonParams.toString());

                RequestClient.post(context, "bills/addtable/", entity, "application/json", new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        // If the response is JSONObject instead of expected JSONArray
                        try {
                            System.out.println("Adding table number on the bill");
                            Log.d("RESPONSE", response.toString());

                        } catch (Exception e) {
                            System.out.println("Adding table number on the bill");
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
    }

    private void convertData() {

        for (int i = 0; i < data.length(); i++) {
            JSONObject obj = new JSONObject();

            try {
                obj = data.getJSONObject(i);
            } catch (JSONException ex) {
                Log.d("JSONEX", ex.getMessage());
            }

            String catname = "";
                /*Commented out by Sergejs 12.07, since disabled big Category view
                If you try to add big categories again, you should also check search function
                String cat_id_json;
                int cat_id =0;
                int selected_cat_id = CategoryActivity.group;
                */
            try {
                catname = obj.getString("name_cat");
                //cat_id_json = obj.getString("id_group");
                //cat_id = parseInt(cat_id_json);
            } catch (JSONException ex) {
                Log.d("JSONEX", ex.getMessage());
            }
                /*Disabled big categories
                if (cat_id == selected_cat_id) {
                    TextView t = (TextView) findViewById(R.id.textView_01);
                    t.setText(cat_id);
                */
            // Actual list view data
            if (!catlist.contains(catname)) {
                catlist.add(catname);
            }

            List<Product> prods = prodlist.get(catname);
            if (prods == null) {
                prods = new ArrayList<>();
            }

            try {
                prods.add(new Product(
                        parseInt(obj.getString("id_prod")),
                        obj.getString("name_prod"),
                        Float.parseFloat(obj.getString("price_prod_excl")),
                        Float.parseFloat(obj.getString("price_prod_incl")),
                        stripCommaAtEnd(obj.getString("reference_prod")),
                        catname,
                        1
                ));
                all_prods.add(prods.get(prods.size() - 1));
            } catch (JSONException ex) {
                Log.d("JSONEX", "Couldn't get product: " + ex.getMessage());
            }

            prodlist.put(catname, prods);
            //} end of if statement
        }

        // Get the grid view and bind array adapter
        recyclerView = (RecyclerView) findViewById(R.id.overview_main);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        //Creating one List for the adapter
        // PS ----> 0 for HEADER & 1 for CHILD
        for (int i = 0; i < catlist.size(); i++) {

            String catagoryName = catlist.get(i);

            MainAdapter.Item header_item = new MainAdapter.Item(0, 0, catagoryName);
            header_item.invisibleChildren = new ArrayList<MainAdapter.Item>();
            prods = prodlist.get(catagoryName);

            for (int j = 0; j < prods.size(); j++) {
                Product myProd = prods.get(j);

                //Displaying products invisible when the list is created
                MainAdapter.Item child_item = new MainAdapter.Item(1,myProd.getId(), myProd.getReference()+" - "+myProd.getName());
                header_item.invisibleChildren.add(child_item);
            }

            dataToBind.add(header_item);


        }

        adapter = new MainAdapter(this,dataToBind,this);
        recyclerView.setAdapter(adapter);

        prodlist2 = prodlist;
        Collections.sort(all_prods, new Comparator<Product>() {
            @Override
            public int compare(Product prod1, Product prod2) {
                return prod1.getReference().compareTo(prod2.getReference());
            }
        });

        //Add to db
        db.insertProduct(all_prods);
        try {
            db.selectAllProducts();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Listview on child click listener

        hideLoading();
    }

    private JSONArray fetchMainCategory() throws JSONException {
        JSONObject obj;

        if (SharedPrefHelper.getString(MainActivity.this, SharedPrefHelper.category) == null ||
                SharedPrefHelper.getString(MainActivity.this, SharedPrefHelper.category).isEmpty()) {

            SharedPrefHelper.putString(MainActivity.this, SharedPrefHelper.category, loadJSONFromAsset());

        }

        obj = new JSONObject(SharedPrefHelper.getString(MainActivity.this, SharedPrefHelper.category));
        JSONArray jsonArray = obj.getJSONArray("Category");

        return jsonArray;
    }

    /*
    Load JSON from assets
     */
    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("main_category.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    //inserts added product to bill in database
    public void add_product(int prodId) {
            try {
                //showLoading();
                StringEntity entity;

                JSONObject jsonParams = new JSONObject();
                jsonParams.put("bill_id", bill_nr);
                jsonParams.put("product_id", prodId);
                entity = new StringEntity(jsonParams.toString());
                RequestClient.put(getApplicationContext(), "bills/product/", entity, "application/json", new JsonHttpResponseHandler() {
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
                        hideLoading();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        try {
                            Log.d("RESPONSE", errorResponse.toString());
                        } catch (Exception e) {
                            Log.d("Exception HTTP", e.getMessage());
                        }
                        hideLoading();
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
    }

    //calculate total price of the bill and add it to the bill
    public void calculate(Bundle extras, Product prod) {
        try {
            if (extras != null) {
                for (int y = 0; y <= open_bills.size(); y++) {
                    if (open_bills.get(y).getId() == bill_nr) {
                        b.setTotal_price_excl(open_bills.get(y).getTotal_price_excl());
                        open_bills.get(y).setTotal_price_excl(open_bills.get(y).getTotal_price_excl() + prod.getPrice_excl());
                        OverviewActivity.bills = open_bills;
                        break;
                    }
                }
            }

            b.setTotal_price_excl(b.getTotal_price_excl() + prod.getPrice_excl());
            StringEntity entity;

                //showLoading();
                JSONObject jsonParams = new JSONObject();
                jsonParams.put("bill_id", bill_nr);
                jsonParams.put("total_price_excl", b.getTotal_price_excl());
                entity = new StringEntity(jsonParams.toString());
                RequestClient.put(getApplicationContext(), "bills/price/", entity, "application/json", new JsonHttpResponseHandler() {
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
                        hideLoading();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        try {
                            Log.d("RESPONSE", errorResponse.toString());
                        } catch (Exception e) {
                            Log.d("Exception HTTP", e.getMessage());
                        }
                        hideLoading();
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
    }

    //creates a toast message
    public void create_toast(String msg) {
        if (currentToast != null) currentToast.cancel();
        currentToast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
        currentToast.show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);;
        setContentView(R.layout.main_activity);
        db = new DatabaseHelper(this);
        intent = getIntent();
        extras = intent.getExtras();
        table_text = (EditText) findViewById(R.id.table_number);
        context = this;
        myMainActivity = this;
        pr = new Printer();

        b = new Bill(null, true, null, null, null, 0, 0);

        settings = SharedPrefHelper.loadSettings(getApplicationContext());

        if (settings == null) {
            settings = new Settings();
            SharedPrefHelper.saveSettings(getApplicationContext(), settings);
        }

        createNewBill();


        //tax_btn =(ToggleButton)findViewById(R.id.tax_button);
        context = this;
        search = (EditText) findViewById(R.id.search);
        all_prods = new ArrayList<>();
        if (extras != null) {
            bill_nr = intent.getIntExtra("bill_nr", 1);
            table_nr = intent.getStringExtra("tableNr");
            waiter_name = intent.getStringExtra("waiter_name");
            lblTable_number = (TextView) findViewById(R.id.table_number);
            if (table_nr != null) {
                System.out.println("TABLE " + table_nr + "");
                lblTable_number.setHint(table_nr);
            } else {
                lblTable_number.setHint("#");
            }
            try {
                if (Integer.parseInt(table_nr) == 100) {
                    //tax_btn.setChecked(true);
                    //table_text.setEnabled(false);
                }
            } catch (Exception ex) {
                Log.d("Exception", ex.getMessage());
            }
            System.out.println("EDITING BILL NR : " + bill_nr);
            // and get whatever type user account id is
        } else {
            System.out.println("NO EDITING BILL NR");
        }
            String apiData = SharedPrefHelper.getString(getApplicationContext(), "apiData");

            if (apiData == null || apiData.equals("")) return;
        /*if (bill_nr!=0){
            get_added_prods();
        }
        */
            if (apiData == null || apiData.equals("")) return;

            if (apiData.equals("ERROR")) { // No internet or other connection problem with db
                // Todo: This code is so ugly, replace asap
                ArrayList<String> err = new ArrayList<>();
                err.add("Error");
                LinkedHashMap<String, List<Product>> msg = new LinkedHashMap<>();
                List<Product> durrr = new ArrayList<Product>();
                durrr.add(new Product(-1, getString(R.string.could_not_get_db_info), 0, 0, null, null, 1));
                msg.put("Error", durrr);
                /*
                recyclerView = (RecyclerView) findViewById(R.id.overview_main);
                recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                adapter = new MainAdapter(this, err, msg);
                recyclerView.setAdapter(adapter);
*/
            } else {
                showLoading();

                try {
                    data = new JSONArray(apiData); // Array of JSONObjects from API
                    convertData();
                } catch (JSONException ex) {
                    Log.d("JSONEX", ex.getMessage());
                }
            }
            /* Text change listener to filter on reference number
            EditText refnr = (EditText) findViewById(R.id.ref_nr);
            refnr.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                    String text = s.toString();
                    Log.d("TEXT CHANGED", "onTextChanged: " + text);
                    view.expandGroup(0);
                    //view.destroyDrawingCache();
                    view.invalidateViews();

                    List<Product> filteredList = new ArrayList<>();
                    ArrayList<String> catlist2 = new ArrayList<>();

                    if(!text.equals(""))
                    {
                        String searchres = "Search result";
                        if(!catlist2.contains(searchres)) {
                            catlist2.add(searchres);
                        }
                        Iterator<List<Product>> it = prodlist.values().iterator();
                        while(it.hasNext())
                        {
                            List<Product> prod = it.next();
                            for(int i = 0; i < prod.size(); i++) {
                                if(prod.get(i).getReference().contains(text)){
                                    filteredList.add(prod.get(i));
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }
                        prodlist2.put(searchres,filteredList);
                    }
                    adapter.filter(text);
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
            */
        // Text change listener to filter on reference number
        final EditText table = (EditText) findViewById(R.id.table_number);
        table.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                add_table_nr(s.toString());


            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //SEARCH Functionality
        search.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

                List<Product> selected_prods = new ArrayList<>();

                String input = search.getText().toString();

                //comparing if product reference from all_prods starts with number in search field
                for (int i = 0; i < all_prods.size(); i++) {
                    try {
                        int prod_ref = parseInt(all_prods.get(i).getReference());
                        String string_prod = String.valueOf(prod_ref);
                        //if product have same reference it is added to view
                        if ((Integer.parseInt(input) >= 0) && (string_prod.startsWith(input))) {

                            selected_prods.add(all_prods.get(i));

                        } else {
                            selected = null;
                        }
                    } catch (Exception e) {
                        Log.d("Ex", e.getMessage());
                    }
                }


                SearchAdapter mysearchadapter = new SearchAdapter(getApplicationContext(), selected_prods, myMainActivity);
                recyclerView.setAdapter(mysearchadapter);


                if (input.length() <= 0) {
                    //If the edit text is empty it will return the display all products/categories
                    recyclerView.setAdapter(adapter);
                }
            }
        });
    }


    private void createNewBill() {
        if (extras == null) {
            System.out.println("CREATING A NEW BILL");
            try {
                    //showLoading();
                    StringEntity entity;
                    JSONObject bill = new JSONObject();
                    bill.put("table_nr", table_nr);
                    bill.put("total_price_excl", b.getTotal_price_excl());
                    entity = new StringEntity(bill.toString());
                    RequestClient.put(context, "bills/", entity, "application/json", new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            // If the response is JSONObject instead of expected JSONArray
                            try {
                                Log.d("RESPONSE", response.toString());//RESPONSE: {"id":"3","success":true}
                                bill_nr = parseInt(response.get("id").toString());

                            } catch (Exception e) {
                                Log.d("Exception HTTP", e.getMessage());
                            }
                            b.setId(bill_nr);

                            //START OF SECOND QUERRY
                            try {
                                StringEntity entity;
                                JSONObject jsonParams = new JSONObject();
                                Log.d("RESPONSE", "ADDING THE WAITER ON THE BILL");
                                jsonParams.put("bill_id", bill_nr);
                                jsonParams.put("waiter_id", settings.getWaiter_id());
                                entity = new StringEntity(jsonParams.toString());

                                RequestClient.post(context, "bills/addwaiter/", entity, "application/json", new JsonHttpResponseHandler() {
                                    @Override
                                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                        // If the response is JSONObject instead of expected JSONArray
                                        try {
                                            Log.d("RESPONSE", response.toString());

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
                            //END of second querry
                            hideLoading();

                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                            try {
                                Log.d("RESPONSE", errorResponse.toString());
                            } catch (Exception e) {
                                Log.d("Exception HTTP", e.getMessage());
                            }
                            hideLoading();
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


        } else {

            //id_edit = extras.getString("bill_nr");
            //bill_nr = Integer.parseInt(id_edit);
            bill_nr = intent.getIntExtra("bill_nr", 1);
            table_nr = intent.getStringExtra("tableNr");
            waiter_name = intent.getStringExtra("waiter_name");
            System.out.println("BILL NR :" + bill_nr);
        }

        hideLoading();
    }


    // something
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                clearData();
                startActivity(i);
                onBackPressed();
                return true;
            case R.id.bills_overview:
                Intent i2 = new Intent(this, OverviewActivity.class);
                clearData();
                startActivity(i2);
                return true;
            case android.R.id.home:
                Intent resultIntent = new Intent(this, OverviewActivity.class);
                //ArrayList<String> bill_info = new ArrayList<String>();
                //bill_info.add(Integer.toString(bill_nr));
                //bill_info.add(table_nr);
                resultIntent.putExtra("bill_nr", bill_nr);
                resultIntent.putExtra("tableNr", table_nr);
                setResult(RESULT_OK, resultIntent);
                clearData();
                finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshContent() {
        finish();
        startActivity(getIntent());
    }

    public void goOverview() {
        Intent goOverview = new Intent(context, OverviewActivity.class);
        startActivity(goOverview);
    }

    public String stripCommaAtEnd(String s) {
        if (s.equals("") || s == null || s.length() < 1) return "";
        if (s.charAt(s.length() - 1) == ',') {
            return s.substring(0, s.length() - 1);
        } else return s;
    }

    public void gotoOverview(View view) {

        EditText e = (EditText) findViewById(R.id.table_number);
        String val = e.getText().toString();
        table_nr = val;
        Intent intent = new Intent(this, PrintActivity.class);
        intent.putExtra("bill_nr", bill_nr);
        intent.putExtra("tableNr", table_nr);
        intent.putExtra("waiter_name", waiter_name);
        clearData();
        startActivity(intent);
        onBackPressed();


    }

    private void clearData() {
        all_prods.clear();
        products.clear();
        prod = null;
        data = null;
        prodlist.clear();
        prodlist2.clear();
        dataToBind.clear();
    }

    public void setFocusToTableNumber(View view) {
        EditText e = (EditText) findViewById(R.id.table_number);
        e.requestFocus();
        // Show keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(e, InputMethodManager.SHOW_IMPLICIT);
    }

    public void hideSoftKeyboard(View view) {
        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void printKitchenBill() {
        if (products == null) return;
        if (products.size() <= 0) return;

        // String tableNr = SharedPrefHelper.getString(getApplicationContext(), "tableNr");
        String s;
        StringBuilder strb = new StringBuilder("");
        int number_kitchen = 0;

        strb.append(INITIATE);
        strb.append(CHAR_TABLE_EURO);
        strb.append(BR);

        //tableNr = "randomThingToTestIfThePrintWorksWithATableNr";
        if (!table_nr.equals("")) {
            strb.append("$bighw$");
            strb.append(getString(R.string.table_nr).toUpperCase() + table_nr);
            strb.append(BR);
        }

        strb.append("$big$");
        strb.append(BR);
        strb.append(pr.getAlignCenter(StringHelper.swapU(getString(R.string.kitchen).toUpperCase())));


        strb.append(BR + "$big$" + BR);
        strb.append(getLineOf("=", CHARCOUNT_BIG));

        double totalPriceExcl = 0;
        double totalPriceIncl = 0;

        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getCount() < 1) continue;
            if (!products.get(i).getDrink()) {
                if (!(number_kitchen == 0))
                    strb.append(getLineOf("-", CHARCOUNT_BIG));

                number_kitchen++;

                double priceEx = products.get(i).getPrice_excl();
                double priceInc = Tax_calculator.calculate_tax(products.get(i).getPrice_excl(),table_nr);

                strb.append(BR);

                if (products.get(i).getCount() != 1) {
                    // 2 x 2.15
                    strb.append("$bighw$");
                    s = products.get(i).getCount() + " x ";
                    strb.append(s);
                    strb.append("$big$");
                    s = Rounder.round(Tax_calculator.calculate_tax(products.get(i).getPrice_excl(),table_nr));
                    strb.append(s);
                    strb.append(BR);
                }
                s = "#" + products.get(i).getReference() + " ";
                strb.append(s);
                strb.append(BR);
                // All Star Product                 4.30
                strb.append("$bighw$");
                s = StringHelper.swapU(products.get(i).getName().toUpperCase());
                strb.append(s);
                strb.append("$big$");
                String totalPriceForThisProduct = Rounder.round(products.get(i).getCount() * Tax_calculator.calculate_tax(products.get(i).getPrice_excl(),table_nr));
                s = pr.getAlignRightSpecial((totalPriceForThisProduct), products.get(i).getName().length());
                strb.append(s);
                strb.append(BR);


                totalPriceExcl += (priceEx * products.get(i).getCount());
                totalPriceIncl += (priceInc * products.get(i).getCount());
            }

        }
        strb.append(getLineOf("=", CHARCOUNT_BIG));
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

    private String getLineOf(String s, int lineSize) {
        StringBuilder strb = new StringBuilder("");
        for (int i = 0; i < lineSize; i++) {
            strb.append(s);
        }
        return strb.toString();
    }

    private String getBillFooter() {
        StringBuilder strb = new StringBuilder("");
        strb.append("$big$");

        // Served by waiter
        if (!settings.getWaiter().equals(""))
            strb.append(pr.getAlignCenter(getString(R.string.served_by) + " " + waiter_name) + "$intro$");

        strb.append(pr.getAlignCenter(getString(R.string.tyvm)) + "$intro$");
        strb.append(pr.getAlignCenter(getString(R.string.visit_again)));

        for (int i = 0; i < 8; i++) {
            strb.append(BR);
        }
        strb.append("$cut$");
        return strb.toString();
    }

    public void printDrinkBill() {
        if (products == null) return;
        if (products.size() <= 0) return;

        //String tableNr = SharedPrefHelper.getString(getApplicationContext(), "tableNr");
        String s;
        StringBuilder strb = new StringBuilder("");
        int number_drinks = 0;

        strb.append(INITIATE);
        strb.append(CHAR_TABLE_EURO);
        strb.append(BR);

        // tableNr = "randomThingToTestIfThePrintWorksWithATableNr";
        if (!table_nr.equals("")) {
            strb.append("$bighw$");
            strb.append(getString(R.string.table_nr).toUpperCase() + table_nr);
            strb.append(BR);
        }

        strb.append("$big$");
        strb.append(BR);
        strb.append(pr.getAlignCenter(getString(R.string.drink).toUpperCase()));


        strb.append(BR + "$big$" + BR);
        strb.append(getLineOf("=", CHARCOUNT_BIG));

        double totalPriceExcl = 0;
        double totalPriceIncl = 0;

        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getCount() < 1) continue;
            if (products.get(i).getDrink()) {
                // Don't print it when it is the first time and number_drinks == 0
                if (!(number_drinks == 0))
                    strb.append(getLineOf("-", CHARCOUNT_BIG));

                number_drinks++;
                double priceEx = products.get(i).getPrice_excl();
                double priceInc = products.get(i).getPrice_incl();

                strb.append(BR);

                if (products.get(i).getCount() != 1) {
                    // 2 x 2.15
                    strb.append("$bighw$");
                    s = products.get(i).getCount() + " x ";
                    strb.append(s);
                    strb.append("$big$");
                    s = Rounder.round(products.get(i).getPrice_incl());
                    strb.append(s);
                    strb.append(BR);
                }
                s = "#" + products.get(i).getReference() + " ";
                strb.append(s);
                strb.append(BR);
                // All Star Product                 4.30
                strb.append("$bighw$");
                s = StringHelper.swapU(products.get(i).getName().toUpperCase());
                strb.append(s);
                strb.append("$big$");
                String totalPriceForThisProduct = Rounder.round(products.get(i).getCount() * products.get(i).getPrice_incl());
                s = pr.getAlignRightSpecial((totalPriceForThisProduct), products.get(i).getName().length());
                strb.append(s);
                strb.append(BR);


                totalPriceExcl += (priceEx * products.get(i).getCount());
                totalPriceIncl += (priceInc * products.get(i).getCount());
            }

        }


        strb.append(getLineOf("=", CHARCOUNT_BIG));
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


    @Override
    public void attachListener(View v, int position) {


        MainAdapter.Item myItem = dataToBind.get(position);

        for (int i = 0; i < all_prods.size(); i++) {
            if (all_prods.get(i).getId() == myItem.prodID) {
                prod2 = all_prods.get(i);
                added_product_id=prod2.getId();
                add_product(prod2.getId());
                products.add(prod2);
                calculate(extras, prod2);
                create_toast( getString(R.string.added)  + " " + prod2.getName());

            }
        }

    }

    @Override
    public void searchListener(View v, Product myProduct) {

        add_product(myProduct.getId());
        products.add(myProduct);
        calculate(extras,myProduct);
        search.setText("");
        create_toast(getString(R.string.added)+" "+myProduct.getName());

    }

}
