package helpers;


import static de.httptandooripalace.restaurantorderprinter.R.string.settings;

/**
 * Created by uizen on 07/08/2017.
 */

public class Printer {

    private final static int CHARCOUNT_BIG = 48; // Amount of characters fit on one printed line, using $big$ format
    private final static int CHARCOUNT_BIGW = 24; // Amount of characters fit on one printed line, using $bigw$ format

    private final static String INITIATE = "·27··64·"; // ESC @
    private final static String CHAR_TABLE_EURO = "·27··116··19·"; // ESC t 19 -- 19 for euro table
    private final static String EURO = "·213·";
    private final static String DOT = "·46·";
    private final static String BR = "$intro$"; // Line break
    private final static String u = "·129·";
    private final static String U = "·154·";
    private final static String HEADER_FONT = "·27··33··32·";

    private static String alignRight(String s) {
        int length = s.length();
        int paddingLeft = CHARCOUNT_BIG - length;
        String newstr = "";
        for (int i = 0; i < paddingLeft; i++) {
            newstr += " ";
        }
        newstr += s;
        return newstr;
    }

    private static String alignRight(String s, int offsetLeft) {
        int length = s.length();
        int paddingLeft = CHARCOUNT_BIG - length;
        // EURO length counts as more than 1 character and bugs alignment
        for (int i = 0; i < StringHelper.checkCount(s, EURO); i++) {
            paddingLeft += EURO.length() - 1;
        }
        for (int i = 0; i < StringHelper.checkCount(s, u); i++) {
            paddingLeft += u.length() - 1;
        }
        for (int i = 0; i < StringHelper.checkCount(s, U); i++) {
            paddingLeft += U.length() - 1;
        }
        String newstr = "";
        int j = (offsetLeft + s.length()) / CHARCOUNT_BIG;
        if ((offsetLeft + s.length()) < CHARCOUNT_BIG * (j + 1)) {
            for (int i = 0; i < (paddingLeft - offsetLeft + CHARCOUNT_BIG * j); i++) {
                newstr += " ";
            }
        }
        newstr += s;
        return newstr;
    }

    private static String alignRightBigw(String s, int offsetLeft) {
        int length = s.length();
        int paddingLeft = CHARCOUNT_BIGW - length;
        // EURO length counts as more than 1 character and bugs alignment
        for (int i = 0; i < StringHelper.checkCount(s, EURO); i++) {
            paddingLeft += EURO.length() - 1;
        }
        for (int i = 0; i < StringHelper.checkCount(s, u); i++) {
            paddingLeft += u.length() - 1;
        }
        for (int i = 0; i < StringHelper.checkCount(s, U); i++) {
            paddingLeft += U.length() - 1;
        }
        String newstr = "";
        int j = (offsetLeft + s.length()) / CHARCOUNT_BIGW;
        if ((offsetLeft + s.length()) < CHARCOUNT_BIGW * (j + 1)) {
            for (int i = 0; i < (paddingLeft - offsetLeft + CHARCOUNT_BIGW * j); i++) {
                newstr += " ";
            }
        }
        newstr += s;
        return newstr;
    }


    private static String alignRightSpecial(String s, int offsetLeft) {// because there is 2 different size of text on the line
        int length = s.length();
        int paddingLeft = CHARCOUNT_BIG - length;
        // EURO length counts as more than 1 character and bugs alignment
        for (int i = 0; i < StringHelper.checkCount(s, EURO); i++) {
            paddingLeft += EURO.length() - 1;
        }
        for (int i = 0; i < StringHelper.checkCount(s, u); i++) {
            paddingLeft += u.length() - 1;
        }
        for (int i = 0; i < StringHelper.checkCount(s, U); i++) {
            paddingLeft += U.length() - 1;
        }
        String newstr = "";
        int j = (offsetLeft * 2 + s.length()) / CHARCOUNT_BIG;
        if ((offsetLeft * 2 + s.length()) < CHARCOUNT_BIG * (j + 1)) {
            for (int i = 0; i < (paddingLeft - offsetLeft * 2 + CHARCOUNT_BIG * j); i++) {
                newstr += " ";
            }
        }
        newstr += s;
        return newstr;
    }

    private static String alignCenter(String s) {
        int length = s.length();
        int totalSpaceLeft = CHARCOUNT_BIG - length;
        int spaceOnBothSides = totalSpaceLeft / 2;
        String newstr = "";
        // EURO length counts as more than 1 character and bugs alignment
        for (int i = 0; i < StringHelper.checkCount(s, EURO); i++) {
            for (int j = 0; j < (EURO.length() - 1) / 2; j++) {
                newstr += " ";
            }
        }
        for (int i = 0; i < StringHelper.checkCount(s, u); i++) {
            for (int j = 0; j < (u.length() - 1) / 2; j++) {
                newstr += " ";
            }
        }
        for (int i = 0; i < StringHelper.checkCount(s, U); i++) {
            for (int j = 0; j < (U.length() - 1) / 2; j++) {
                newstr += " ";
            }
        }
        for (int i = 0; i < spaceOnBothSides; i++) {
            newstr += " ";
        }
        newstr += s;
        for (int i = 0; i < spaceOnBothSides; i++) {
            newstr += " ";
        }

        return newstr;
    }

    private static String alignCenterBigw(String s) {
        int length = s.length();
        int totalSpaceLeft = CHARCOUNT_BIGW - length;
        int spaceOnBothSides = totalSpaceLeft / 2;
        String newstr = "";
        // EURO length counts as more than 1 character and bugs alignment
        for (int i = 0; i < StringHelper.checkCount(s, EURO); i++) {
            for (int j = 0; j < (EURO.length() - 1) / 2; j++) {
                newstr += " ";
            }
        }
        for (int i = 0; i < StringHelper.checkCount(s, u); i++) {
            for (int j = 0; j < (u.length() - 1) / 2; j++) {
                newstr += " ";
            }
        }
        for (int i = 0; i < StringHelper.checkCount(s, U); i++) {
            for (int j = 0; j < (U.length() - 1) / 2; j++) {
                newstr += " ";
            }
        }
        for (int i = 0; i < spaceOnBothSides; i++) {
            newstr += " ";
        }
        newstr += s;
        for (int i = 0; i < spaceOnBothSides; i++) {
            newstr += " ";
        }

        return newstr;
    }

    private static String appendIfNotEmpty(String s){
        if (!s.equals("")) {
            return s + BR;
        }
        return "";
    }

    public String getAlignRight(String s){
        return alignRight(s);
    }
    public String getAlignRight(String s, int offsetLeft){
        return alignRight(s, offsetLeft);
    }
    public String getAlignRightBigw(String s,int offsetLeft){
        return alignRightBigw(s,offsetLeft);
    }
    public String getAlignRightSpecial(String s,int offsetLeft) {
        return alignRightSpecial(s, offsetLeft);
    }

    public String getAlignCenter(String s){
        return alignCenter(s);
    }
    public String getAlignCenterBigw(String s){
        return alignCenterBigw(s);
    }
    public String getAppend(String s){
        return appendIfNotEmpty(s);
    }
}
