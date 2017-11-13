package interfaces;

import android.view.View;

import entities.Product;

/**
 * Created by uiz on 09/08/2017.
 */

public interface attachListeners {
    public void attachListener(View v, int position);
    public void searchListener(View v, Product myProduct);

}
