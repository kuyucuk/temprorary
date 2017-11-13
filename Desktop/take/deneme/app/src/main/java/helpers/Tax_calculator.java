package helpers;

import android.util.Log;
import static helpers.Rounder.round;

/**
 * Created by uizen on 07/08/2017.
 */

public class Tax_calculator {
    public static double calculate_tax(double price, String tableNr){
        double totalPriceIncl=0;
        int tablenr=0;
        double tax=1.19;
        try {
            tablenr = Integer.parseInt(tableNr);
        } catch(NumberFormatException nfe) {
            System.out.println("Could not parse " + nfe);
        }
        if(tablenr>= 100){
            tax= 1.07;
        }
        double totalPrice_withTax= tax*price;
        //rounding operation
        int decimal =(int) totalPrice_withTax;
        int fractional = (int) ( 100*(totalPrice_withTax-decimal));
        int rounded_fractional = (int) (Math.round(fractional/10.0)*10);
        totalPriceIncl = decimal + 0.01*rounded_fractional;
        return totalPriceIncl;
    }

}
