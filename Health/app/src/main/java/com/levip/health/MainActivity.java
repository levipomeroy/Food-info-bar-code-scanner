package com.levip.health;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    TextView barcodeResult;
    TextView barcodeVal;
    Button scanBarcode;
    JSONObject product_info;
    String List_Ingr;
    String[] Peanut_Avoid = {"peanuts", "arachis oil","artificial nuts","beer nuts", "peanut oil", "goobers","ground nuts",
            "lupin","mandelonas", "mixed nuts", "monkey nuts", "nut meat", "nut pieces", "peanut butter", "peanut flour",
            "peanut protein hydrolysate", "fenugreek", "satay"};
    String ndbNum;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        barcodeResult = (TextView) findViewById(R.id.barcode_result);
        barcodeVal = (TextView) findViewById(R.id.barcodeVal);
        scanBarcode = (Button) findViewById(R.id.scan_barcode);


        /*******************************************************************************************
         This function starts the ScanBarcodeActivity when the button is clicked.
         ******************************************************************************************/
        scanBarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ScanBarcodeActivity.class);
                startActivityForResult(intent, 0);
            }
        });
    }


    public void scanBarcode(View v){
        Intent intent = new Intent(this, ScanBarcodeActivity.class);
        startActivityForResult(intent, 0);
    }

    /**********************************************************************************************
     This function gets the barcode number from the ScanBarcodeActivity and displays it. It then calls
     the api to see if the code is contained in it.
     **********************************************************************************************/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == 0){
            if(requestCode == CommonStatusCodes.SUCCESS){
                if(data!=null){
                    final Barcode barcode = data.getParcelableExtra("barcode");
                    barcodeVal.setText(barcode.displayValue);

                    new Thread() {
                        public void run() {
                            USDA_API_Func(barcode.displayValue.toString());
                        }
                    }.start();
//                    new Thread() {
//                        public void run() {
//                            Edamum_API_Func(barcode.displayValue.toString());
//                        }
//                    }.start();

//                    barcodeResult.setText("Barcode value : "+barcode.displayValue);
                }
                else{
                    barcodeResult.setText("No barcode found");
                }
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**********************************************************************************************
     This function searches the api for the barcode number and returns with resulting json object
     if it is found.
     **********************************************************************************************/
    public void USDA_API_Func(String barcode_num) {
        String USDA_API_KEY = "7bWVq01c024uZbzxlRx9VyQTah78IABt8ErbXHlM";

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {

            URL url = new URL("https://api.nal.usda.gov/ndb/search/?format=json&q=" + barcode_num +"&max=25&offset=0&api_key=" + USDA_API_KEY);

            conn = (HttpURLConnection) url.openConnection();

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            if (reader == null)
                System.out.println("Error with reader");
            StringBuilder json = new StringBuilder(1024);
            String tmp = "";
            while ((tmp = reader.readLine()) != null)
                json.append(tmp).append("\n");
            reader.close();
            JSONObject data = new JSONObject(json.toString());

            Message msg = new Message();
            msg.obj = (JSONObject) data;
            msg.setTarget(ProductHandler);
            msg.sendToTarget();
        }
        catch (MalformedURLException e) {e.printStackTrace();}
        catch (IOException e) {e.printStackTrace();}
        catch (JSONException e) {e.printStackTrace();}
    }

//    public void Edamum_API_Func(String barcode_num) {
//        String Edamum_API_KEY = "dc6f82554f9677c66a1c6a1287521194";
//        String App_Id = "b5c4f19b";
//
//        HttpURLConnection conn = null;
//        StringBuilder jsonResults = new StringBuilder();
//        try {
//            URL url = new URL("https://api.edamam.com/api/food-database/parser?upc=" + barcode_num +"&app_id=" +App_Id +"&app_key=" + Edamum_API_KEY);
//
//            conn = (HttpURLConnection) url.openConnection();
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//            if (reader == null)
//                System.out.println("Error with reader");
//            StringBuilder json = new StringBuilder(1024);
//            String tmp = "";
//            while ((tmp = reader.readLine()) != null)
//                json.append(tmp).append("\n");
//            reader.close();
//            JSONObject data = new JSONObject(json.toString());
//
//            Message msg = new Message();
//            msg.obj = (JSONObject) data;
//            msg.setTarget(ProductHandler);
//            msg.sendToTarget();
//        }
//        catch (MalformedURLException e) {e.printStackTrace();}
//        catch (IOException e) {e.printStackTrace();}
//        catch (JSONException e) {e.printStackTrace();}
//    }

//    public Handler EdumumHandler = new Handler() {
//        public void handleMessage(Message msg) {
//        }
//    };

    /**********************************************************************************************
     This function handles the json object retrieved from the api in another thread and gets the item
     name and ndbno out of it. It then displays the name and calls GetIngredients to gather ingredients
     and nutritional information.
     **********************************************************************************************/
    @SuppressLint("HandlerLeak")
    public Handler ProductHandler = new Handler() {
        public void handleMessage(Message msg) {
            List_Ingr = null;
            product_info = (JSONObject) msg.obj;
            String name = null;
            try {
                ndbNum = product_info.getJSONObject("list").getJSONArray("item").getJSONObject(0).getString("ndbno");
                name = product_info.getJSONObject("list").getJSONArray("item").getJSONObject(0).getString("name");

                barcodeResult.setText(name);
                new Thread() {
                    public void run() {
                        GetIngredients();
                    }
                }.start();
            } catch (JSONException e) {
                barcodeResult.setText("barcode not found :(");
            }
        }
    };

    /**********************************************************************************************
     This function goes back to the api with the ndbno to get the ingredients and nutrients lists
     **********************************************************************************************/
    public void GetIngredients() {
        String USDA_API_KEY = "7bWVq01c024uZbzxlRx9VyQTah78IABt8ErbXHlM";

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {

            URL url = new URL("https://api.nal.usda.gov/ndb/reports/?ndbno=" + ndbNum + "&type=f&format=json&api_key=" + USDA_API_KEY);

            conn = (HttpURLConnection) url.openConnection();

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            if (reader == null)
                System.out.println("Error with reader");
            StringBuilder json = new StringBuilder(1024);
            String tmp = "";
            while ((tmp = reader.readLine()) != null)
                json.append(tmp).append("\n");
            reader.close();
            JSONObject data = new JSONObject(json.toString());

            Message msg = new Message();
            msg.obj = (JSONObject) data;
            msg.setTarget(IngredientsHandler);
            msg.sendToTarget();
        }
        catch (IOException | JSONException e) {e.printStackTrace();}
    }

    /**********************************************************************************************
     This function handles the json object retrieved from the api in another thread and passes it
     the ParseIngredientsList to get parsed and displayed.
     **********************************************************************************************/
    @SuppressLint("HandlerLeak")
    public Handler IngredientsHandler = new Handler() {
        public void handleMessage(Message msg) {
            ParseIngredientsList((JSONObject) msg.obj);
            ParseNutrientsList((JSONObject) msg.obj);
        }
    };

    /**********************************************************************************************
    This function parses the json object for the list of ingredients and displays them.
     **********************************************************************************************/
    public void ParseIngredientsList(JSONObject Full_Info){

        List_Ingr = null;
        try {
            List_Ingr = Full_Info.getJSONObject("report").getJSONObject("food").getJSONObject("ing").getString("desc");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        boolean eat = true;
        for(int i=0; i<Peanut_Avoid.length && eat == true; i++){
            if(List_Ingr.toLowerCase().contains(Peanut_Avoid[i])){
                eat = false;
            }
        }
        if(eat == true){
            barcodeResult.setText(barcodeResult.getText() + "\n\n" + "Ingredients:\n" + List_Ingr.toLowerCase() + "\n\n"
            + "Peanut free!\n");
        }
        else{
            barcodeResult.setText(barcodeResult.getText() + "\n\n" + "Ingredients:\n" + List_Ingr.toLowerCase() + "\n\n"
                    + "You will die if you eat this!\n");
        }
        //barcodeResult.setText(barcodeResult.getText() + "\n\n" + "Ingredients:\n" + List_Ingr.toLowerCase());
    }

    /**********************************************************************************************
     This function parses the json object for the list of ingredients and displays them.
     **********************************************************************************************/
    public void ParseNutrientsList(JSONObject Full_Info){
        String[] Nutrients_Info = new String[30];
        String[] Types = {"Calories","Protein","Fats","Carbohydrates","Fiber","Sugars","Calcium","Iron","Sodium",
                "Vitamin C", "Niacan","Vitamin A","Saturated Fats","Trans Fat", "Cholesterol"};

        try {
            //calories
            Nutrients_Info[0] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(0).getString("value");
            Nutrients_Info[1] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(0).getString("unit");
            //Protein
            Nutrients_Info[2] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(1).getString("value");
            Nutrients_Info[3] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(1).getString("unit");
            //Fats(lipids)
            Nutrients_Info[4] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(2).getString("value");
            Nutrients_Info[5] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(2).getString("unit");
            //carbs
            Nutrients_Info[6] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(3).getString("value");
            Nutrients_Info[7] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(3).getString("unit");
            //fiber
            Nutrients_Info[8] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(4).getString("value");
            Nutrients_Info[9] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(4).getString("unit");
            //sugars -total
            Nutrients_Info[10] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(5).getString("value");
            Nutrients_Info[11] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(5).getString("unit");
            //Calcium
            Nutrients_Info[12] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(6).getString("value");
            Nutrients_Info[13] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(6).getString("unit");
            //Iron
            Nutrients_Info[14] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(7).getString("value");
            Nutrients_Info[15] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(7).getString("unit");
            //Sodium
            Nutrients_Info[16] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(8).getString("value");
            Nutrients_Info[17] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(8).getString("unit");
            //Vitamin C -9
            Nutrients_Info[18] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(9).getString("value");
            Nutrients_Info[19] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(9).getString("unit");
            //Niacin
            Nutrients_Info[20] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(10).getString("value");
            Nutrients_Info[21] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(10).getString("unit");
            //Vitamin A
            Nutrients_Info[22] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(11).getString("value");
            Nutrients_Info[23] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(11).getString("unit");
            //Fatty Acids/total Saturated
            Nutrients_Info[24] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(12).getString("value");
            Nutrients_Info[25] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(12).getString("unit");
            //Fatty Acids/total trans
            Nutrients_Info[26] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(13).getString("value");
            Nutrients_Info[27] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(13).getString("unit");
            //Cholesterol
            Nutrients_Info[28] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(14).getString("value");
            Nutrients_Info[29] = Full_Info.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients").getJSONObject(14).getString("unit");

            barcodeResult.setText(barcodeResult.getText() + "\n\n" + "Nutrients:\n");
            int j =0;
            for(int i =0; i <30; i++){
                barcodeResult.setText(barcodeResult.getText() +Types[j] +"\t\t\t" + Nutrients_Info[i] + " " + Nutrients_Info[++i] +"\n");
                j++;
            }



        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
