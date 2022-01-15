package org.surveytools.flightlogger;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.surveytools.flightlogger.SummaryRowItem;

//CUSTOM_LIST_IN_DIALOG
// custom ArrayAdapter necessary for customized layout using an adapter (using the type specified)

public class SummaryArrayAdapter extends ArrayAdapter<SummaryRowItem> {

    Context context; 
    int layoutResourceId;    
    SummaryRowItem data[] = null;
    
    public SummaryArrayAdapter(Context context, int layoutResourceId, SummaryRowItem[] data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        SummaryHolder holder = null;
        
        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            
            holder = new SummaryHolder();
            holder.txtLabel = (TextView)row.findViewById(R.id.transect_summary_item_label_id);
            holder.txtValue = (TextView)row.findViewById(R.id.transect_summary_item_value_id);
            
            row.setTag(holder);
        }
        else
        {
            holder = (SummaryHolder)row.getTag();
        }
        
        SummaryRowItem summary = data[position];
        holder.txtLabel.setText(summary.mLabelText);
        holder.txtValue.setText(summary.mDetailsText);
        
        // SUMMARY_HEADER_LAYOUT
        // special formatting for the header item
        holder.txtValue.setTextSize(summary.mIsHeader ? 26 : 32);
        // alt holder.txtValue.setTextAlignment(...);
        
        return row;
    }
    
    static class SummaryHolder
    {
        TextView txtLabel;
        TextView txtValue;
        boolean isHeader;
    }
}
