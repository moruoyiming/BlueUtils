package com.calypso.buetools;

import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;

import com.calypso.bluelib.bean.SearchResult;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class DeviceListAdapter extends BaseQuickAdapter<SearchResult, BaseViewHolder> {


    public DeviceListAdapter(@LayoutRes int layoutResId, @Nullable List<SearchResult> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, SearchResult item) {
        helper.setText(R.id.name, item.getName());
        helper.setText(R.id.mac, item.getAddress());
        helper.setText(R.id.rssi, String.format("Rssi: %d", item.getRssi()));
    }


}
