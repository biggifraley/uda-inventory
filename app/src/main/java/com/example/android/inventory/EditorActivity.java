package com.example.android.inventory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventory.data.ProductContract.ProductEntry;

import java.io.FileDescriptor;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Allows user to create a new product or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = EditorActivity.class.getSimpleName();

    /**
     * Identifier for the image picker
     */
    private static final int PICK_IMAGE_REQUEST = 0;

    /**
     * Identifier for the product data loader
     */
    private static final int EXISTING_PRODUCT_LOADER = 0;

    /**
     * Content URI for the existing product (null if it's a new product)
     */
    private Uri mCurrentProductUri;

    /**
     * URI for the product image (null if it's a new image)
     */
    private Uri mProductImageUri;

    /**
     * Defines a variable to contain the number of updated rows
     */
    private int mRowsUpdated = 0;

    /**
     * Defines a variable to contain the number of updated rows
     */
    private int mRowsDeleted = 0;

    /**
     * EditText field to enter the product's name
     */
    private EditText mNameEditText;

    /**
     * EditText field to enter the product's price
     */
    private EditText mPriceEditText;

    /**
     * EditText field to enter the product's quantity
     */
    private EditText mQuantityEditText;

    /**
     * EditText field to enter the product's supplier
     */
    private EditText mSupplierEditText;

    /**
     * Button to select the product's image
     */
    private Button mImageButton;

    /**
     * ImageView field for the product's image
     */
    private ImageView mPictureImageView;

    /**
     * TextView field for the product image's uri
     */
    private TextView mPictureUriTextView;

    /**
     * Boolean flag that keeps track of whether the product has been edited (true) or not (false)
     */
    private boolean mProductHasChanged = false;

    /**
    * Defines a variable to contain the quantity of product sold, received, or to reorder
    */
    private int updateQuantity = 0;

    /**
     * Defines constants to signal whether quantity change is sale, reorder, or shipment received
     */
    private static final int SALE = 0;
    private static final int SHIPMENT = 1;
    private static final int REORDER = 2;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mProductHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new product or editing and existing one.
        // Use getIntent() and getData() to get the associated URI
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        // Set title of EditorActivity depending on which situation we have.
        // If the EditorActivity was opened using the ListView item, then we will
        // have uri of product, so change app bar to say "Edit Product."
        // Otherwise, if this is a new product, uri is null, so change app bar to say "Add a Product."

        // If the intent DOES NOT contain a product content URI, then we know we are
        // creating new product.
        if (mCurrentProductUri == null) {
            // change app bar to say "Add a Product"
            setTitle(getString(R.string.editor_activity_title_new_product));
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // change app bar to say "Edit Product"
            setTitle(getString(R.string.editor_activity_title_edit_product));
            // Initialize a loader to read the product data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mSupplierEditText = (EditText) findViewById(R.id.edit_product_supplier);
        mQuantityEditText = (EditText) findViewById(R.id.edit_product_quantity);
        mPriceEditText = (EditText) findViewById(R.id.edit_product_price);
        mPictureUriTextView = (TextView) findViewById(R.id.image_uri);
        mImageButton = (Button) findViewById(R.id.select_image);
        mPictureImageView = (ImageView) findViewById(R.id.image_bitmap);

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mImageButton.setOnTouchListener(mTouchListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        // Since the editor shows all product attributes, define a projection that contains
        // all columns from the product table
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER,
                ProductEntry.COLUMN_PRODUCT_IMAGE
        };

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this, mCurrentProductUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // When the data from the product is loaded into a cursor, onLoadFinished() is called.
        // Here, I’ll first move the cursor to it’s first item position. Even though it only
        // has one item, it starts at position -1.
        // Proceed with moving to the first row of the cursor and reading data from it.
        // (This should be the only row in the cursor.)
        // Then I’ll get the data out of the cursor by getting the index of each data item,
        // and then using the indices and the get methods to grab the actual integers and strings.
        if (cursor.moveToFirst()) {
            // Find the columns of pet attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int supplierColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SUPPLIER);
            int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            String supplier = cursor.getString(supplierColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            double price = cursor.getDouble(priceColumnIndex);
            String image = cursor.getString(imageColumnIndex);

            // Format the price to show 2 decimal places
            String formattedPrice = formatPrice(price);

            // For each of the textViews I’ll set the proper text.
            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mSupplierEditText.setText(supplier);
            mQuantityEditText.setText(String.format (Locale.getDefault(), "%1$d", quantity));
            // mQuantityEditText.setText(Integer.toString(quantity));
            mPriceEditText.setText(formattedPrice);
            // mPriceEditText.setText(String.format("%1$.2f", price));
            mPictureUriTextView.setText(image);

            // If there's an image uri in the database,
            // parse the image uri string and set mProductImageUri, and
            // update the image view
            if (image != null) {
                mProductImageUri = Uri.parse(image);
                mPictureImageView.setImageBitmap(getBitmapFromUri(mProductImageUri));
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mSupplierEditText.setText("");
        mQuantityEditText.setText("");
        mPriceEditText.setText("");
        mPictureUriTextView.setText("");
    }

    /**
     * Get user input from editor and save new product into database
     */
    private void saveProduct() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String imageString = mPictureUriTextView.getText().toString().trim();

        // Check if all the fields are empty, and save button has been pressed accidentally.
        // If so, do nothing and exit.
        // (In the lessons, the following is being checked only for insert operations
        // in mCurrentProduct == null below, however, I leave it up here, because a user
        // may mistakenly try to delete a product in the editor by emptying all fields.)
        if (TextUtils.isEmpty(nameString) && TextUtils.isEmpty(supplierString) && TextUtils.isEmpty(quantityString) && TextUtils.isEmpty(priceString) && TextUtils.isEmpty(imageString)) return;

        // Validate the product name and supplier. If empty, alert user that information is required.
        if (TextUtils.isEmpty(nameString) || TextUtils.isEmpty(supplierString)) {

            Toast.makeText(this, getString(R.string.editor_insert_required_info),
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Default quantity to 0.
        int quantity = 0;
        // If a quantity has been entered, convert to int.
        if (!TextUtils.isEmpty(quantityString)) {
            quantity = Integer.parseInt(quantityString);
        }

        // Default price to 0.00.
        double price = 0.00;
        // If a price has been entered, convert to double.
        if (!TextUtils.isEmpty(priceString)) {
            price = Double.parseDouble(priceString);
        }

        // Create a ContentValues object where column names are the keys,
        // and product attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER, supplierString);
        values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, imageString);

        // Check if we are updating an existing product or inserting a new product
        if (mCurrentProductUri == null) {
            // Insert a new product into the provider, returning the content URI for the new product.
            Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Update the existing product and return the number of rows returned
            mRowsUpdated = getContentResolver().update(mCurrentProductUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful
            if (mRowsUpdated == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        // Once the save operation is done, then the activity can be closed
        // by calling the finish() method.
        finish();
    }

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {

        // Only perform the delete if this is an existing product.
        if (mCurrentProductUri != null) {
            // Delete the existing pet and return the number of rows returned
            mRowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

            // Show a toast message depending on whether or not the delete was successful
            if (mRowsDeleted == 0) {
                // If no rows were affected, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Once the delete operation is done, then the activity can be closed
        // by calling the finish() method.
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the unnecessary menu items.
        if (mCurrentProductUri == null) {
            MenuItem deleteMenuItem = menu.findItem(R.id.action_delete);
            deleteMenuItem.setVisible(false);
            MenuItem saleMenuItem = menu.findItem(R.id.action_sale);
            saleMenuItem.setVisible(false);
            MenuItem receiveMenuItem = menu.findItem(R.id.action_receive);
            receiveMenuItem.setVisible(false);
            MenuItem reorderMenuItem = menu.findItem(R.id.action_reorder);
            reorderMenuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save product to database
                saveProduct();
                // Exit activity
                // finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Show a dialog that confirms the user wants to delete the pet
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Sale" menu option
            case R.id.action_sale:
                showQuantityDialog(SALE);
                return true;
            // Respond to a click on the "Shipment" menu option
            case R.id.action_receive:
                showQuantityDialog(SHIPMENT);
                return true;
            // Respond to a click on the "Reorder" menu option
            case R.id.action_reorder:
                showQuantityDialog(REORDER);
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the product hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the pet hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    /**
     * This method is called when the delete menu option is pressed.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet and close editor.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * This method is called when the sale, receive, or reorder menu option is pressed.
     */
    private void showQuantityDialog(final int updateType) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.quantity_dialog, null);
        dialogBuilder.setView(dialogView);

        final EditText quantityUpdateText = (EditText) dialogView.findViewById(R.id.edit_quantity);

        if (updateType == SALE) {
            dialogBuilder.setMessage(R.string.quantity_dialog_sale_msg);
        } else if (updateType == SHIPMENT) {
            dialogBuilder.setMessage(R.string.quantity_dialog_shipment_msg);
        } else if (updateType == REORDER) {
            dialogBuilder.setMessage(R.string.quantity_dialog_reorder_msg);
        }

        dialogBuilder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                updateQuantity = Integer.parseInt(quantityUpdateText.getText().toString().trim());
                String quantityString = mQuantityEditText.getText().toString().trim();
                Integer newQuantity;
                if (updateType == SALE) {
                    newQuantity = Math.max((Integer.parseInt(quantityString) - updateQuantity), 0);
                    mQuantityEditText.setText(String.format (Locale.getDefault(), "%1$d", newQuantity));
                    saveProduct();
                } else if (updateType == SHIPMENT) {
                    newQuantity = Integer.parseInt(quantityString) + updateQuantity;
                    mQuantityEditText.setText(String.format (Locale.getDefault(), "%1$d", newQuantity));
                    saveProduct();
                } else if (updateType == REORDER) {
                    sendOrderEmail(Integer.parseInt(quantityString));
                }

                // Log.v(LOG_TAG, "Update type: " + updateType + "\nOriginal Quantity is: " + quantityString + "\nQuantity entered is: " + updateQuantity + "\nNew Quantity is: " + newQuantity);
            }
        });
        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    /**
     * Return the formatted price string showing 2 decimal places (i.e. "3.22")
     * from a decimal price value.
     */
    private String formatPrice(double price) {
        DecimalFormat priceFormat = new DecimalFormat("0.00");
        return priceFormat.format(price);
    }

    public void sendOrderEmail(int quantity) {
        String nameString = mNameEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();

        String emailSubject = "Order of " + nameString;
        String orderSummary = supplierString + ",\n\nI would like to order: \n\n" + quantity + " " + nameString + " at a unit price of $" + priceString + ".\n\nThank you!";

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
        intent.putExtra(Intent.EXTRA_TEXT, orderSummary);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    public void openImageSelector(View view) {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE.
        // If the request code seen here doesn't match, it's the response to some other intent,
        // and the below code shouldn't run at all.

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"

            if (resultData != null) {
                mProductImageUri = resultData.getData();
                Log.i(LOG_TAG, "Uri: " + mProductImageUri.toString());

                mPictureUriTextView.setText(mProductImageUri.toString());
                mPictureImageView.setImageBitmap(getBitmapFromUri(mProductImageUri));
            }
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return image;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Error closing ParcelFile Descriptor");
            }
        }
    }
}