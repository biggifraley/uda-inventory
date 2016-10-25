package com.example.android.inventory;

/**
 * Created by Biggi on 10/16/2016.
 */

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.inventory.data.ProductContract;

/**
 * {@link ProductCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of product data as its data source. This adapter knows
 * how to create list items for each row of product data in the {@link Cursor}.
 */
public class ProductCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link ProductCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the pet data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current pet can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        // Find fields to populate in inflated template
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView quantityTextView = (TextView) view.findViewById(R.id.quantity);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);

        // Find the columns of product attributes that we're interested in and
        // extract properties from cursor.
        String productName = cursor.getString(cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_PRODUCT_NAME));
        String productQuantity = "Quantity: " + cursor.getString(cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_PRODUCT_QUANTITY));
        String productPrice = "Price: $" + cursor.getString(cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_PRODUCT_PRICE));

//        Integer productQuantity = cursor.getInt(cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_PRODUCT_QUANTITY));
//        Long productPrice = cursor.getLong(cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_PRODUCT_PRICE));

        // Populate fields with extracted properties
        nameTextView.setText(productName);
        quantityTextView.setText(productQuantity);
        priceTextView.setText(productPrice);
    }
}
