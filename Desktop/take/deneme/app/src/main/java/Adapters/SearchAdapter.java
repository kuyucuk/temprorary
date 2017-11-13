package Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.httptandooripalace.restaurantorderprinter.R;
import entities.Product;
import interfaces.attachListeners;

/**
 * Created by uiz on 11/08/2017.
 */

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.myViewHolder> {

    private Context context;
    private List<Product> selectedProds;
    private attachListeners mysearchListener;

    public SearchAdapter(Context c, List<Product> selectedProds, attachListeners listener ){

        this.context=c;
        this.selectedProds= new ArrayList<>();
        this.selectedProds = selectedProds;
        mysearchListener= listener;

    }

    public class myViewHolder extends RecyclerView.ViewHolder{

        Button prod_title;

        public myViewHolder(View itemView) {
            super(itemView);

            prod_title = (Button) itemView.findViewById(R.id.prod_title);
        }
    }


    @Override
    public SearchAdapter.myViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) this.context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.main_list_item, parent, false);
        myViewHolder holder = new myViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(final SearchAdapter.myViewHolder holder, final int position) {

        final Product product = selectedProds.get(position);

        holder.prod_title.setText(product.getReference() + " - " + product.getName());

        holder.prod_title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                mysearchListener.searchListener(v,product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return selectedProds.size();
    }
}
