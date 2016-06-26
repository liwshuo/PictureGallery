package com.bupt.picturegallery;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by lishuo on 16/5/5.
 */
public abstract class RecyclerGalleryAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    @Override
    public void onBindViewHolder(VH holder, int position) {
        View itemView = holder.itemView;
        ViewGroup.LayoutParams lp = null;
        if(itemView.getLayoutParams() != null) {
            lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }else {
            lp = itemView.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;

            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        }
        itemView.setLayoutParams(lp);
    }
}
