package Adapters;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.httptandooripalace.restaurantorderprinter.R;
import entities.Bill;
import helpers.Tax_calculator;


import java.lang.Math;




import helpers.Rounder;

/**
 * Created by uiz on 27/04/2017.
 */

public class OverviewAdapter extends RecyclerView.Adapter<OverviewAdapter.OverViewHolder> {
    private  LayoutInflater inflater;
    private Context context;
    private List<Bill> bills;

    public OverviewAdapter(Context c, List<Bill> bills) {
        context = c;
        this.bills = new ArrayList<>();
        this.bills = bills;

    }

    @Override
    public OverViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.overview_list_item,parent,false);
        OverViewHolder holder = new OverViewHolder(view);
        return holder;
    }


    @Override
    public void onBindViewHolder(OverViewHolder holder, int position) {
        final Bill bill = bills.get(position);
        holder.t1.setText(bill.getTableNr() == null ? "#" : bill.getTableNr());
        holder.t2.setText(bill.getWaiter());
        holder.t3.setText("" + bill.getId());
        holder.t4.setText(bill.getDate().toString().substring(10, 16));
        //Rounding with helpers.Rounder
        holder.t6.setText(Rounder.round(Tax_calculator.calculate_tax(bill.getTotal_price_excl(),bill.getTableNr())) + " €");

        holder.b1.setTag(R.string.id_tag, bill.getId());
        holder.b1.setTag(R.string.table_tag, bill.getTableNr());

        holder.b2.setTag(R.string.id_tag, bill.getId());
        holder.b2.setTag(R.string.table_tag, bill.getTableNr());

        holder.b3.setTag(R.string.id_tag, bill.getId());
        holder.b3.setTag(R.string.table_tag, bill.getTableNr());
        holder.b3.setTag(R.string.waiter_tag, bill.getWaiter());
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        if (bills == null) {
            return 0;
        }
        return bills.size();
    }

    class OverViewHolder extends RecyclerView.ViewHolder{
        TextView t1;
        TextView t2;
        TextView t3;
        TextView t4;
        TextView t6;
        FloatingActionButton b1;
        FloatingActionButton b2;
        FloatingActionButton b3;
        public OverViewHolder(View itemView) {
            super(itemView);
            t1= (TextView) itemView.findViewById(R.id.table_number);
            t2 = (TextView) itemView.findViewById(R.id.waiter_name);
            t3 = (TextView) itemView.findViewById(R.id.bill_nr);
            t4 = (TextView) itemView.findViewById(R.id.hour);
            t6 = (TextView) itemView.findViewById(R.id.total_price);
            b1 = (FloatingActionButton) itemView.findViewById(R.id.close_bill);
            b2 = (FloatingActionButton) itemView.findViewById(R.id.edit_bill);
            b3 = (FloatingActionButton) itemView.findViewById(R.id.print_bill);
        }
    }
}
