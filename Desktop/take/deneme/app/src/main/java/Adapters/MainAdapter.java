package Adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;

import de.httptandooripalace.restaurantorderprinter.R;
import interfaces.attachListeners;

import static android.support.design.R.id.center_vertical;
import static android.support.design.R.id.info;
import static android.support.design.R.id.left;

/**
 * Created by uiz on 07/08/2017.
 */

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.myViewHolder> {

    public static final int HEADER = 0;
    public static final int CHILD = 1;

    private attachListeners myattachListeners;

    private Context context;
    private List<Item> data;


    public MainAdapter(Context c, List<Item> data, attachListeners listeners) {
        this.data= new ArrayList<>();
        this.data=data;
        this.context=c;
        myattachListeners = listeners;
    }


    public static class myViewHolder extends RecyclerView.ViewHolder{

        Button catTitle;
        Item refferalItem;

        public myViewHolder(View itemView) {
            super(itemView);

            catTitle = (Button) itemView.findViewById(R.id.cat_title);
        }
    }

    @Override
    public myViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = null;


        switch (viewType){
            case HEADER:
                LayoutInflater inflater = (LayoutInflater) this.context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.main_list_group, parent, false);
                myViewHolder holder = new myViewHolder(view);

                return holder;
            case CHILD:
                Button prodTitle = new Button(context);
                prodTitle.setTextColor(Color.BLACK);
                prodTitle.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
                prodTitle.setGravity(left & center_vertical);
                prodTitle.setBackgroundColor(Color.WHITE);

                return new myViewHolder(prodTitle);

        }

        return null;
    }



    @Override
    public void onBindViewHolder(myViewHolder holder, final int position) {

        final Item item = data.get(position);

        switch (item.type){
            case HEADER:
                final myViewHolder itemController = holder;
                itemController.refferalItem = item;
                itemController.catTitle.setText(item.text);


                itemController.catTitle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (item.invisibleChildren==null){
                            item.invisibleChildren = new ArrayList<Item>();
                            int count = 0;
                            int pos = data.indexOf(itemController.refferalItem);
                            while(data.size()>pos+1 && data.get(pos+1).type == CHILD){
                                item.invisibleChildren.add(data.remove(pos+1));
                                count++;
                            }
                            notifyItemRangeRemoved(pos+1,count);
                        }
                        else {
                            int pos = data.indexOf(itemController.refferalItem);
                            int index = pos +1;
                            for (Item i:item.invisibleChildren){
                                data.add(index,i);
                                index++;
                            }
                            notifyItemRangeInserted(pos+1,index-pos-1);
                            item.invisibleChildren = null;
                        }
                    }
                });

                break;
            case CHILD:
                Button prodTitle = (Button) holder.itemView;
                prodTitle.setText(data.get(position).text);
                prodTitle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        myattachListeners.attachListener(v,position);

                    }
                });
                break;


        }
    }

    @Override
    public int getItemViewType(int position){
        return data.get(position).type;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class Item{
        public int type;
        public String text;
        public int prodID;
        public List<Item> invisibleChildren;



        public Item(int type, int prodID, String text){
            this.type=type;
            this.text=text;
            this.prodID=prodID;

        }

    }
}
