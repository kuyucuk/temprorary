package Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.httptandooripalace.restaurantorderprinter.R;
import entities.Product;
import helpers.Rounder;
import helpers.Tax_calculator;
import interfaces.OnItemClickedListener;

import static de.httptandooripalace.restaurantorderprinter.PrintActivity.tableNr;

/**
 * Created by uiz on 17/07/2017.
 */

public class SplitAdapter extends RecyclerView.Adapter<SplitAdapter.ViewHolder> {


    private Context context;
    private List<Product> products;
    private OnItemClickedListener onItemClickedListener;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tv;
        Button button;

        public ViewHolder(View itemView) {
            super(itemView);

            tv = (TextView) itemView.findViewById(R.id.productDescription);
            button = (Button) itemView.findViewById(R.id.add_cart);

        }
    }


    public SplitAdapter(Context c, List<Product> products, OnItemClickedListener listener) {
        context = c;
        this.products = new ArrayList<>();
        this.products = products;
        onItemClickedListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) this.context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.split_list_item, parent, false);
        ViewHolder holder = new ViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        final Product product = products.get(position);

        holder.tv.setText(productText(product));

        final int pos = position;
        final ViewHolder myHolder = holder;

        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickedListener.onItemClicked(v, position);
                product.setCount(1);
                products.set(pos, product);
                myHolder.tv.setText(productText(product));

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
        return (p.getName() + " x " + p.getCount()
                + "\n" + R.string.price_incl_euro + Rounder.round(Tax_calculator.calculate_tax(p.getPrice_excl() * p.getCount(),tableNr))
                + "\nRef: " + p.getReference()
                + "\n\n");
    }
}
