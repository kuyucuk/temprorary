package helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import entities.Bill;
import entities.Product;
import entities.Settings;

/**
 * Created by Rohail on 7/10/2017.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    // Logcat tag
    private static final String LOG = "DatabaseHelper";

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "orderandorint";

    // Table Names
    private static final String TABLE_BILL = "bill";
    private static final String TABLE_PRODUCT = "product";
    private static final String TABLE_WAITER = "waiter";
    private static final String TABLE_ORDER = "order_table";

    // BILL column names
    private static final String KEY_BILL_ID = "bill_id";
    private static final String KEY_bill_date = "bill_date";
    private static final String KEY_TABLE_NR = "table_nr";
    private static final String KEY_TOTAL_PRICE_EXCL = "total_price_excl";
    private static final String KEY_IS_OPEN = "is_open";

    // PRODUCT Table - column nmaes
    private static final String KEY_PRODUCT_NAME = "product_name";
    private static final String KEY_PRICE_EXCL = "price_excl";
    private static final String KEY_PRICE_INCL = "price_incl";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_PRODUCT_ID = "product_id";
    private static final String KEY_PRODUCT_COUNT = "count";
    private static final String KEY_PRODUCT_REFERENCE = "reference";

    // User Table - column names
    private static final String KEY_WAITER_NAME = "waiter_name";
    private static final String KEY_WAITER_ID = "waiter_id";
    private static final String KEY_WAITER_ADD_LINE1 = "addr_line1";
    private static final String KEY_WAITER_ADD_LINE2 = "addr_line2";
    private static final String KEY_WAITER_TELEPHONE = "telephone";
    private static final String KEY_REST_NAME1 = "rest_name1";
    private static final String KEY_REST_NAME2 = "rest_name2";
    private static final String KEY_TAX_NR = "TAX_NR";
    private static final String KEY_EXTRA_LINE = "extra_line";

    //Order Table
    private static final String CREATE_TABLE_ORDER = "CREATE TABLE "
            + TABLE_ORDER + " (" + KEY_BILL_ID + " INTEGER, " + KEY_PRODUCT_ID
            + " INTEGER," + KEY_PRODUCT_COUNT + " INTEGER DEFAULT 1" + ")";

    // Table Create Statements
    // Bill table create statement
    private static final String CREATE_TABLE_BILL = "CREATE TABLE "
            + TABLE_BILL + "(" + KEY_BILL_ID + " INTEGER PRIMARY KEY autoincrement," + KEY_bill_date
            + " DATETIME DEFAULT CURRENT_TIMESTAMP," + KEY_TABLE_NR + " TEXT," + KEY_TOTAL_PRICE_EXCL
            + " DOUBLE," + KEY_WAITER_NAME + " TEXT," + KEY_IS_OPEN + " INTEGER DEFAULT 1" + ")";

    // Product table create statement
    private static final String CREATE_TABLE_PRODUCT = "CREATE TABLE " + TABLE_PRODUCT
            + "(" + KEY_PRODUCT_ID + " INTEGER PRIMARY KEY," + KEY_PRODUCT_NAME + " TEXT,"
            + KEY_CATEGORY + " TEXT," + KEY_PRODUCT_REFERENCE + " TEXT," + KEY_PRICE_EXCL
            + " DOUBLE," + KEY_PRODUCT_COUNT
            + " INTEGER," + KEY_PRICE_INCL + " DOUBLE" + ")";

    // waiter table create statement
    private static final String CREATE_TABLE_WAITER = "CREATE TABLE "
            + TABLE_WAITER + "(" + KEY_WAITER_ID + " INTEGER PRIMARY KEY autoincrement,"
            + KEY_WAITER_NAME + " TEXT," + KEY_REST_NAME1 + " TEXT," + KEY_REST_NAME2 + " TEXT,"
            + KEY_WAITER_TELEPHONE + " TEXT," + KEY_WAITER_ADD_LINE1 + " TEXT," +
            KEY_WAITER_ADD_LINE2 + " TEXT," + KEY_TAX_NR + " TEXT," +
            KEY_EXTRA_LINE + " TEXT" + ")";

    private final Context context;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // creating required tables
        db.execSQL(CREATE_TABLE_BILL);
        db.execSQL(CREATE_TABLE_PRODUCT);
        db.execSQL(CREATE_TABLE_WAITER);
        db.execSQL(CREATE_TABLE_ORDER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        Log.w(DatabaseHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BILL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WAITER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDER);

        // create new tables
        onCreate(db);
    }

    /**
     * INSERT*******************************************************************
     * *
     */
    public void insertBill(Bill trx) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

//        values.put(KEY_BILL_ID, trx.getId());
        values.put(KEY_WAITER_NAME, trx.getWaiter() + "");
        values.put(KEY_TABLE_NR, trx.getTableNr() + "");
//        values.put(KEY_bill_date, trx.getDate().toString() + "");
        values.put(KEY_TOTAL_PRICE_EXCL, trx.getTotal_price_excl() + "");
        values.put(KEY_IS_OPEN, trx.isOpen() == true ? 1 : 0);

        // Inserting Row
        db.insert(TABLE_BILL, null, values);
    }

    public void deleteBill(int billNr) {
        SQLiteDatabase db = this.getWritableDatabase();

//
        db.delete(TABLE_BILL, KEY_BILL_ID + " = " + billNr, null);
    }

    public void insertProduct(Product trx) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();


        values.put(KEY_PRODUCT_ID, trx.getId());
        values.put(KEY_PRODUCT_NAME, trx.getName() + "");
        values.put(KEY_PRICE_EXCL, trx.getPrice_excl());
        values.put(KEY_PRICE_INCL, trx.getPrice_incl());
        values.put(KEY_CATEGORY, trx.getCategory() + "");
        values.put(KEY_PRODUCT_COUNT, trx.getCount());

        // Inserting Row
        db.insert(TABLE_PRODUCT, null, values);
    }

    public void insertProduct(List<Product> trx) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PRODUCT, null, null);
        ContentValues values;

        for (int i = 0; i < trx.size(); i++) {
            values = new ContentValues();
            values.put(KEY_PRODUCT_ID, trx.get(i).getId());
            values.put(KEY_PRODUCT_NAME, trx.get(i).getName());
            values.put(KEY_PRICE_EXCL, trx.get(i).getPrice_excl());
            values.put(KEY_PRICE_INCL, trx.get(i).getPrice_incl());
            values.put(KEY_CATEGORY, trx.get(i).getCategory());
            values.put(KEY_PRODUCT_COUNT, trx.get(i).getCount());
            values.put(KEY_PRODUCT_REFERENCE, trx.get(i).getReference());

            // Inserting Row
            db.insert(TABLE_PRODUCT, null, values);
        }
    }

    public void insertOrder(int billId, int productId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_BILL_ID, billId);
        values.put(KEY_PRODUCT_ID, productId);

        // Inserting Row
        db.insert(TABLE_ORDER, null, values);
    }

    public void insertWaiter(Settings trx) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WAITER, null, null);
        ContentValues values = new ContentValues();

//        values.put(KEY_WAITER_ID, trx.getWaiter_id());
        values.put(KEY_WAITER_NAME, trx.getWaiter());
        values.put(KEY_REST_NAME1, trx.getNameLine1());
        values.put(KEY_REST_NAME2, trx.getNameLine2());
        values.put(KEY_WAITER_ADD_LINE1, trx.getAddrLine1());
        values.put(KEY_WAITER_ADD_LINE2, trx.getAddrLine2());
        values.put(KEY_WAITER_TELEPHONE, trx.getTelLine());
        values.put(KEY_EXTRA_LINE, trx.getExtraLine());
        values.put(KEY_TAX_NR, trx.getTaxLine());

        // Inserting Row
        db.insert(TABLE_WAITER, null, values);
    }

    /**
     * Select*******************************************************************
     * ****
     */
    public List<Bill> selectAllBills() throws ParseException {
        List<Bill> transactionsList = new ArrayList<Bill>();

        String selectQuery = "SELECT * FROM " + TABLE_BILL + " WHERE " + KEY_IS_OPEN + " = '1'";
//				+ " WHERE " + COLUMN_STATUS + " = " + status + " ORDER BY "
//				+ COLUMN_ID + " DESC";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Bill trx = new Bill();

                trx.setId(cursor.getInt(0));
                trx.setDateText(cursor.getString(1));
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                trx.setDate(sdf.parse(trx.getDateText()));
                trx.setTableNr(cursor.getString(2));
                trx.setTotal_price_excl(cursor.getDouble(3));
                trx.setWaiter(cursor.getString(4));
                trx.setOpen(cursor.getInt(5) == 0 ? false : true);
                // Adding Transaction
                transactionsList.add(trx);
            } while (cursor.moveToNext());
        }
        return transactionsList;
    }

    public List<Product> selectAllProducts() throws ParseException {

        List<Product> transactionsList = new ArrayList<Product>();

        String selectQuery = "SELECT * FROM " + TABLE_PRODUCT;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Product trx = new Product();

                trx.setId(cursor.getInt(0));
                trx.setName(cursor.getString(1));
                trx.setCategory(cursor.getString(2));
                trx.setReference(cursor.getString(3));
                trx.setPrice_excl(cursor.getDouble(4));
                trx.setCount(cursor.getInt(5));
                trx.setPrice_incl(cursor.getDouble(6));
                // Adding Transaction
                transactionsList.add(trx);
            } while (cursor.moveToNext());
        }
        return transactionsList;
    }

    public int selectOrderCount(int billID, int productId) {

        String selectQuery = "SELECT * FROM " + TABLE_ORDER +
                " WHERE " + KEY_BILL_ID + " = " + billID + " AND " +
                KEY_PRODUCT_ID + " = " + productId;
//				+ " WHERE " + COLUMN_STATUS + " = " + status + " ORDER BY "
//				+ COLUMN_ID + " DESC";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        return cursor.getCount() > 0 ? cursor.getInt(2) : 0;

    }

    public Settings selectWaiter() {
//        List<Settings> transactionsList = new ArrayList<Settings>();
        Settings trx = new Settings();
        String selectQuery = "SELECT * FROM " + TABLE_WAITER;
//				+ " WHERE " + COLUMN_STATUS + " = " + status + " ORDER BY "
//				+ COLUMN_ID + " DESC";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
//            do {


            trx.setWaiter_id(cursor.getInt(0));
            trx.setWaiter(cursor.getString(1));
            trx.setNameLine1(cursor.getString(2));
            trx.setNameLine2(cursor.getString(3));
            trx.setTelLine(cursor.getString(4));
            trx.setAddrLine1(cursor.getString(5));
            trx.setAddrLine2(cursor.getString(6));
            trx.setTaxLine(cursor.getString(7));
            trx.setExtraLine(cursor.getString(8));
            // Adding Transaction
//                transactionsList.add(trx);
//            } while (cursor.moveToNext());
        }
        return trx;
    }

    public void closeBill(String id) {
        //Open the database
        SQLiteDatabase database = this.getWritableDatabase();

        //Execute sql query to remove from database
        //NOTE: When removing by String in SQL, value must be enclosed with ''
        database.execSQL("UPDATE " + TABLE_BILL + " SET " + KEY_IS_OPEN + " = '0'"
                + " WHERE " + KEY_BILL_ID + " = " + id);

        database.delete(TABLE_ORDER, KEY_BILL_ID + " = " + id, null);

        //Close the database
        database.close();
    }

    public List<Bill> selectClosedBill() throws ParseException {
        List<Bill> transactionsList = new ArrayList<Bill>();

        String selectQuery = "SELECT * FROM " + TABLE_BILL + " WHERE " + KEY_IS_OPEN + " = '0'";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Bill trx = new Bill();

                trx.setId(cursor.getInt(0));
                trx.setDateText(cursor.getString(1));
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                trx.setDate(sdf.parse(trx.getDateText()));
                trx.setTableNr(cursor.getString(2));
                trx.setTotal_price_excl(cursor.getDouble(3));
                trx.setWaiter(cursor.getString(4));
                trx.setOpen(cursor.getInt(5) == 0 ? false : true);
                // Adding Transaction
                transactionsList.add(trx);
            } while (cursor.moveToNext());
        }
        return transactionsList;

    }

    public void addTableNr(String billId, String tableNr) {
        //Open the database
        SQLiteDatabase database = this.getWritableDatabase();

        //Execute sql query to remove from database
        //NOTE: When removing by String in SQL, value must be enclosed with ''
        //"UPDATE " + TABLE_BILL + " SET " + KEY_TABLE_NR + " = '" + tableNr + "'" +
        //" WHERE " + KEY_BILL_ID + " = '" + billId + "'"
        if (tableNr.isEmpty()) {
            database.execSQL("UPDATE " + TABLE_BILL + " SET " + KEY_TABLE_NR + " = " + null +
                    " WHERE " + KEY_BILL_ID + " = " + billId);
        } else {
            database.execSQL("UPDATE " + TABLE_BILL + " SET " + KEY_TABLE_NR + " = " + tableNr +
                    " WHERE " + KEY_BILL_ID + " = " + billId);
        }
        //Close the database
        database.close();
    }

    public void addAmountToBill(String billId, String amount) {
        //Open the database
        SQLiteDatabase database = this.getWritableDatabase();

        //Execute sql query to remove from database
        //NOTE: When removing by String in SQL, value must be enclosed with ''
        //"UPDATE " + TABLE_BILL + " SET " + KEY_TABLE_NR + " = '" + tableNr + "'" +
        //" WHERE " + KEY_BILL_ID + " = '" + billId + "'"
        database.execSQL("UPDATE " + TABLE_BILL + " SET " + KEY_TOTAL_PRICE_EXCL + " = " + amount +
                " WHERE " + KEY_BILL_ID + " = " + billId);

        //Close the database
        database.close();
    }

    public void addProductToBill(int billId, int productId) {
        //Open the database
        SQLiteDatabase database = this.getWritableDatabase();

        //Execute sql query to remove from database
        //NOTE: When removing by String in SQL, value must be enclosed with ''
        if (selectOrderCount(billId, productId) > 0) {
            database.execSQL("UPDATE " + TABLE_ORDER + " SET " + KEY_PRODUCT_COUNT + " = " + KEY_PRODUCT_COUNT + "+1" +
                    " WHERE " + KEY_BILL_ID + " = " + billId + " AND " + KEY_PRODUCT_ID + " = " + productId);
        } else {
            insertOrder(billId, productId);
        }

        //Close the database
        database.close();
    }

    public void decreaseProductToBill(int billId, int productId) {
        //Open the database
        SQLiteDatabase database = this.getWritableDatabase();

        //Execute sql query to remove from database
        //NOTE: When removing by String in SQL, value must be enclosed with ''
        if (selectOrderCount(billId, productId) > 1) {
            database.execSQL("UPDATE " + TABLE_ORDER + " SET " + KEY_PRODUCT_COUNT + " = " + KEY_PRODUCT_COUNT + "-1" +
                    " WHERE " + KEY_BILL_ID + " = " + billId + " AND " + KEY_PRODUCT_ID + " = " + productId);
        } else {
            database.delete(TABLE_ORDER, KEY_BILL_ID + " = " + billId + " AND " + KEY_PRODUCT_ID + " = " + productId, null);
        }

        //Close the database
        database.close();
    }

    public int getLatestBillNr() {
        String selectQuery = "SELECT * FROM " + TABLE_BILL
                + " ORDER BY " + KEY_BILL_ID + " DESC LIMIT 1";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        int val = -1;
        if (cursor.moveToFirst()) {
            val = cursor.getInt(cursor.getColumnIndex(KEY_BILL_ID));
        }
        cursor.close();
        return val;
    }

    public List<Product> getSelectedBillProducts(int billNr) {
        List<Product> transactionsList = new ArrayList<Product>();
        String selectQuery = "SELECT * FROM " + TABLE_PRODUCT + " P, " + TABLE_ORDER + " O WHERE O." + KEY_BILL_ID
                + " = " + billNr + " AND O." + KEY_PRODUCT_ID + " = P." + KEY_PRODUCT_ID;

        String orderQuery = "SELECT * FROM " + TABLE_ORDER + " WHERE " + KEY_BILL_ID
                + " = " + billNr;
//        String selectQuery = "SELECT * FROM " + TABLE_PRODUCT + " INNER JOIN " + TABLE_ORDER
//                + " ON " + TABLE_PRODUCT + "." + KEY_PRODUCT_ID
//                + " = " + TABLE_ORDER + "." + KEY_PRODUCT_ID;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        SQLiteDatabase orderDb = this.getWritableDatabase();
        Cursor orderCursor = orderDb.rawQuery(orderQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst() && orderCursor.moveToFirst()) {
            do {
                Product trx = new Product();

                trx.setId(cursor.getInt(0));
                trx.setName(cursor.getString(1));
                trx.setCategory(cursor.getString(2));
                trx.setReference(cursor.getString(3));
                trx.setPrice_excl(cursor.getDouble(4));
                trx.setPrice_excl(cursor.getDouble(4));
                trx.setCount(getCount(trx, orderCursor) == -1 ?
                        cursor.getInt(5) :
                        getCount(trx, orderCursor));
                trx.setPrice_incl(cursor.getDouble(6));
                // Adding Transaction
                transactionsList.add(trx);
            } while (cursor.moveToNext());
        }
        return transactionsList;
    }

    private int getCount(Product trx, Cursor cursor) {
        int count = -1;
        do {
            if (cursor.getInt(1) == trx.getId()) {
                count = cursor.getInt(2);
                break;
            }
        } while (cursor.moveToNext());

        return count;

    }
}
