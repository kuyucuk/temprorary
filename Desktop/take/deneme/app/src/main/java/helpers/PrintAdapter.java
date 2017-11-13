package helpers;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.httptandooripalace.restaurantorderprinter.PrintActivity;
import de.httptandooripalace.restaurantorderprinter.R;
import entities.Product;
import interfaces.OnItemClickedListener;

import static de.httptandooripalace.restaurantorderprinter.R.string.tax;
import static helpers.Rounder.round;
import static helpers.Tax_calculator.calculate_tax;


/**
 * Created by uizen on 3/27/2017.
 */

public class PrintAdapter extends RecyclerView.Adapter<PrintAdapter.ViewHolder> {

    private Context context;
    private List<Product> products;
    private OnItemClickedListener onItemClickedListener;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tv;
        Button btnPlus, btnMinus;

        public ViewHolder(View itemView) {
            super(itemView);

            tv = (TextView) itemView.findViewById(R.id.product_description);
            btnPlus = (Button) itemView.findViewById(R.id.btnplus);
            btnMinus = (Button) itemView.findViewById(R.id.btnminus);

        }
    }

    public PrintAdapter(Context c, List<Product> products, OnItemClickedListener listener) {
        context = c;
        this.products = new ArrayList<>();
        this.products = products;
        onItemClickedListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater inflater = (LayoutInflater) this.context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.print_list_item, parent, false);
        ViewHolder holder = new ViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        final Product product = products.get(position);

        holder.tv.setText(productText(product));

        final ViewHolder myHolder = holder;
        final int pos = position;

        holder.btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickedListener.onItemClicked(v, position);
                products.set(pos, product);
                myHolder.tv.setText(productText(product));

                //redraw the view ?
            }
        });

        holder.btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickedListener.onItemClicked(v, position);
                products.set(pos, product);
                myHolder.tv.setText(productText(product));
                //redraw the viex ?
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        if (products == null) return 0;
        return products.size();
    }

    private String productText(Product p) {

        double tax_incl = calculate_tax(p.getPrice_excl(),PrintActivity.tableNr);
        String rounded_tax = round(tax_incl* p.getCount());


        return (p.getName() + " x " + p.getCount()
                + "\n" + rounded_tax
                + "\n" + context.getString(R.string.price_incl_euro) + " " + rounded_tax
                + "\nRef: " + p.getReference()
                + "\n\n");
    }

    private float getTotalPrice(List<Product> prodlist) {
        float total = 0;
        for (int i = 0; i < prodlist.size(); i++) {
            total += prodlist.get(i).getPrice_incl();
        }
        return total;
    }
}
