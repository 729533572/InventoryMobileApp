package gov.nysenate.inventory.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.*;
import gov.nysenate.inventory.adapter.NothingSelectedSpinnerAdapter;
import gov.nysenate.inventory.android.ClearableEditText;
import gov.nysenate.inventory.android.InvApplication;
import gov.nysenate.inventory.android.R;
import gov.nysenate.inventory.model.Transaction;
import gov.nysenate.inventory.util.AppProperties;
import gov.nysenate.inventory.util.HttpUtils;
import gov.nysenate.inventory.util.Serializer;
import gov.nysenate.inventory.util.Toasty;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class EditRemoteStatus extends SenateActivity
{

    private Transaction pickup;
    private CheckBox remoteCheckBox;
    private Spinner remoteShipMethod;
    private ClearableEditText remoteComments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_remote_status);
        registerBaseActivityReceiver();

        TextView oldPickupLocation = (TextView) findViewById(R.id.old_pickup_location);
        TextView oldDeliveryLocation = (TextView) findViewById(R.id.old_delivery_location);
        TextView oldPickupBy = (TextView) findViewById(R.id.pickup_by);
        TextView oldCount = (TextView) findViewById(R.id.pickup_count);
        TextView oldDate = (TextView) findViewById(R.id.pickup_date);
        remoteCheckBox = (CheckBox) findViewById(R.id.remote_checkbox);
        remoteShipMethod = (Spinner) findViewById(R.id.remote_ship_method);
        remoteComments = (ClearableEditText) findViewById(R.id.remote_comments);

        // Display a "hint" in the spinner.
        ArrayAdapter<CharSequence> spinAdapter = ArrayAdapter
                .createFromResource(this, R.array.remote_ship_types,
                        R.layout.spinner24_item);
        spinAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        remoteShipMethod.setAdapter(new NothingSelectedSpinnerAdapter(
                spinAdapter, R.layout.spinner_nothing_selected, this));

        pickup = EditPickupMenu.pickup;

        if (pickup.isRemote()) {
            oldPickupLocation.setText(Html.fromHtml(pickup.getOrigin()
                    .getLocationSummaryStringRemoteAppended()));
            oldDeliveryLocation.setText(Html.fromHtml(pickup.getDestination()
                    .getLocationSummaryStringRemoteAppended()));
        } else {
            oldPickupLocation.setText(pickup.getOrigin()
                    .getLocationSummaryString());
            oldDeliveryLocation.setText(pickup.getDestination()
                    .getLocationSummaryString());
        }
        oldPickupBy.setText(pickup.getNapickupby());
        oldCount.setText(Integer.toString(pickup.getPickupItems().size()));
        SimpleDateFormat sdf = ((InvApplication) getApplicationContext())
                .getDateTimeFormat();
        oldDate.setText(sdf.format(pickup.getPickupDate()));

        if (pickup.isRemote()) {
            remoteCheckBox.setChecked(true);
        } else {
            remoteCheckBox.setChecked(false);
        }
        remoteBoxClicked(remoteCheckBox, false);
    }

    public void remoteBoxClicked(View view) {
        remoteBoxClicked(view, true);
    }

    public void remoteBoxClicked(View view, boolean checkRemotePickupStatus) {
        if (checkRemotePickupStatus) {
            if (pickup.getOrigin().isRemote()) {
                AlertDialog.Builder errorMsg = new AlertDialog.Builder(this)
                        .setTitle("Error setting Remote.")
                        .setMessage("Pickup remote status cannot be changed.")
                        .setCancelable(false)
                        .setNeutralButton(Html.fromHtml("<b>Ok</b>"), null);

                errorMsg.show();
                remoteCheckBox.setChecked(!((CheckBox) view).isChecked());
                return;
            }
        }

        if (((CheckBox) view).isChecked()) {
            // Can't be remote if both locations are local
            if (!pickup.getOrigin().isRemote()
                    && !pickup.getDestination().isRemote()) {
                AlertDialog.Builder errorMsg = new AlertDialog.Builder(this)
                        .setTitle("Error setting Remote.")
                        .setMessage(
                                "Albany to Albany transactions should not be processed as remote.")
                        .setCancelable(false)
                        .setNeutralButton(Html.fromHtml("<b>Ok</b>"), null);

                errorMsg.show();
                remoteCheckBox.setChecked(false);
                return;
            }
            remoteShipMethod.setVisibility(View.VISIBLE);
            remoteShipMethod.setSelection(convertShipMethodToSpinnerPosition());
            remoteComments.setVisibility(View.VISIBLE);
            remoteComments.setText(pickup.getShipComments());
        } else {
            remoteShipMethod.setSelection(0);
            remoteShipMethod.setVisibility(View.INVISIBLE);
            remoteComments.setText("");
            remoteComments.setVisibility(View.INVISIBLE);
        }
    }

    public void continueButton(View view) {
        if (anythingWasEdited()) {
            if (allInfoEntered()) {
                displayConfirmationDialog();
            } else {
                Toasty.displayCenteredMessage(this,
                        "You must select a ship method.", Toast.LENGTH_SHORT);
            }
        } else {
            Toasty.displayCenteredMessage(this,
                    "You have not made any changes.", Toast.LENGTH_SHORT);
        }
    }

    public void backButton(View view) {
        if (checkServerResponse(true) == OK) {
            super.onBackPressed();
        }
    }

    private boolean anythingWasEdited() {
        // Careful, remoteShipMethod.getSelectedItem() returns null if nothing
        // (hint) is selected.
        if (!remoteCheckBox.isChecked() && !pickup.isRemote()) {
            return false;
        }
        if (remoteCheckBox.isChecked() != pickup.isRemote()
                || !remoteShipMethod.getSelectedItem().toString()
                        .equals(pickup.getShipType())
                || !remoteComments.getText().toString()
                        .equals(pickup.getShipComments())) {
            return true;
        }
        return false;
    }

    private boolean allInfoEntered() {
        // A Remote shipment must specify a ship method.
        if (remoteCheckBox.isChecked()
                && remoteShipMethod.getSelectedItem() == null) {
            return false;
        }
        return true;
    }

    private void displayConfirmationDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("Update Remote Status")
                .setMessage(Html.fromHtml(changesMadeMessage()))
                .setNegativeButton(Html.fromHtml("<b>No</b>"),
                        new DialogInterface.OnClickListener()
                        {

                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                            }
                        })

                .setPositiveButton(Html.fromHtml("<b>Yes</b>"),
                        new DialogInterface.OnClickListener()
                        {

                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                                setNewValuesInPickup();
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                                    new EditRemoteStatusTask()
                                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                } else {
                                    new EditRemoteStatusTask().execute();
                                }
                            }

                            private void setNewValuesInPickup() {
                                if (remoteShipMethod.getSelectedItem() != null) {
                                    pickup.setShipType(remoteShipMethod
                                            .getSelectedItem().toString());
                                    pickup.setShipComments(remoteComments
                                            .getText().toString());
                                } else {
                                    // pickup is not remote
                                    pickup.setShipType("");
                                    pickup.setShipComments("");
                                }
                            }
                        });

        dialog.show();
    }

    private String changesMadeMessage() {
        String message = "You are about to make the following changes to this pickup: <br/>";
        String end = "<br/><br/>Are you sure you want to continue?";
        String changes = "";

        if (remoteCheckBox.isChecked() != pickup.isRemote()) {
            changes += "<br/>";
            changes += "Remote = ";
            changes += remoteCheckBox.isChecked() ? "<b>Yes</b>" : "<b>No</b>";
        }
        if (remoteShipMethod.getSelectedItem() != null
                && !remoteShipMethod.getSelectedItem().toString()
                        .equals(pickup.getShipType())) {
            changes += "<br/>";
            changes += "Ship method = " + "<b>"
                    + remoteShipMethod.getSelectedItem().toString() + "</b>";
        }
        if (!remoteComments.getText().toString()
                .equals(pickup.getShipComments())) {
            changes += "<br/>";
            changes += "Comments = " + "<b>"
                    + remoteComments.getText().toString() + "</b>";
        }
        return message + changes + end;
    }

    private int convertShipMethodToSpinnerPosition() {
        String shipMethod = pickup.getShipType();
        if (shipMethod != null) {
            if (shipMethod.equals("UPS")) {
                return 1;
            }
            if (shipMethod.equals("FedEx")) {
                return 2;
            }
            if (shipMethod.equals("Maintenance")) {
                return 3;
            }
            if (shipMethod.equals("Other")) {
                return 4;
            }
        }
        return 0;
    }

    private class EditRemoteStatusTask extends AsyncTask<Void, Void, Integer>
    {

        ProgressBar progressBar;

        @Override
        protected void onPreExecute() {
            progressBar = (ProgressBar) findViewById(R.id.edit_remote_progress_bar);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            String url = AppProperties.getBaseUrl(EditRemoteStatus.this);
            url += "ChangeRemoteStatus";

            HttpClient httpClient = LoginActivity.getHttpClient();
            HttpResponse response;
            HttpPost post = new HttpPost(url);

            try {
                List<NameValuePair> values = new ArrayList<NameValuePair>();
                values.add(new BasicNameValuePair("trans", Serializer.serialize(pickup)));
                values.add(new BasicNameValuePair("user", LoginActivity.nauser));
                post.setEntity(new UrlEncodedFormEntity(values));
                response = httpClient.execute(post);

                return response.getStatusLine().getStatusCode();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer response) {
            progressBar.setVisibility(View.INVISIBLE);
            Intent intent = new Intent(EditRemoteStatus.this,
                    EditPickupMenu.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("nuxrpd", Integer.toString(pickup.getNuxrpd()));
            startActivity(intent);
            overridePendingTransition(R.anim.in_right, R.anim.out_left);
            HttpUtils.displayResponseResults(EditRemoteStatus.this, response);
        }
    }
}
