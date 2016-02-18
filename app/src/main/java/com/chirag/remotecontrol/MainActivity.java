package com.chirag.remotecontrol;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,SensorEventListener {

    Context context;
    Button playPauseButton;
    Button nextButton;
    Button previousButton;
    TextView mousePad;
    TextView sensorDataX;
    TextView sensorDataY;

    private SensorManager mSensorManager;

    private boolean isConnected=false;
    private boolean mouseMoved=false;
    private boolean setValueX=true;
    private boolean setValueY=true;
    private Socket socket;
    private PrintWriter out;

    private float initX =0;
    private float initY =0;
    private float disX =0;
    private float disY =0;
    private float threshold=5;
    private float multiplier=100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        context = this; //save the context to show Toast messages

        //Get references of all buttons
        playPauseButton = (Button)findViewById(R.id.playPauseButton);
        nextButton = (Button)findViewById(R.id.nextButton);
        previousButton = (Button)findViewById(R.id.previousButton);

        //this activity extends View.OnClickListener, set this as onClickListener
        //for all buttons
        playPauseButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);
        previousButton.setOnClickListener(this);
        sensorDataX = (TextView)findViewById(R.id.sensorDatax);
        sensorDataY = (TextView)findViewById(R.id.sensorDatay);
        //Get reference to the TextView acting as mousepad
        mousePad = (TextView)findViewById(R.id.mousePad);

        //capture finger taps and movement on the textview
        mousePad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(isConnected && out!=null){
                    switch(event.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            //save X and Y positions when user touches the TextView
                            initX =event.getX();
                            initY =event.getY();
                            mouseMoved=false;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            disX = event.getX()- initX; //Mouse movement in x direction
                            disY = event.getY()- initY; //Mouse movement in y direction
                            /*set init to new position so that continuous mouse movement
                            is captured*/
                            initX = event.getX();
                            initY = event.getY();
                            if(disX !=0|| disY !=0){
                                out.println(disX +","+ disY); //send mouse movement to server
                            }
                            mouseMoved=true;
                            break;
                        case MotionEvent.ACTION_UP:
                            //consider a tap only if usr did not move mouse after ACTION_DOWN
                            if(!mouseMoved){
                                out.println("left_click");
                            }
                    }
                }
                return true;
            }
        });
    }
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    //TODO
                    out.println("left_click");
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    //TODO
                    out.println("right_click");
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }
    @Override

    protected void onResume() {

        super.onResume();

        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_GAME);


    }

    @Override

    protected void onPause() {

        super.onPause();

        // to stop the listener and save battery

        mSensorManager.unregisterListener(this);

    }

    @Override

    public void onSensorChanged(SensorEvent event) {

        sensorDataX.setText("X: " + Float.toString(Math.round(event.values[0])) + " degrees");
        sensorDataY.setText("Y: " + Float.toString(Math.round(event.values[1])) + " degrees");
        // get the angle around the z-axis rotated

        //float degree = Math.round(event.values[0]);
        if(setValueX==true)
        {
            initX = (event.values[0]);

            setValueX=false;
        }
        if(setValueY==true)
        {
            initY = (event.values[1]);
            setValueY=false;
        }


        //initY =event.getY();
        if (isConnected && out != null) {
            {



                    disX = (event.values[0]) - initX; //Mouse movement in x direction
                    disY = (event.values[1]) - initY; //Mouse movement in y direction

                    disX *= multiplier;
                    disX = Math.round(disX);

                    disY *= multiplier;
                    disY = Math.round(disY);
                    //disY = event.getY() - initY; //Mouse movement in y direction
                            /*set init to new position so that continuous mouse movement
                            is captured*/
                    //initX = Math.round(event.values[0]);
                    //initY = event.getY();
                    if ((disX > threshold || disY > threshold) || ((disX < -threshold || disY < -threshold)) ) {
                        out.println(disX + "," + disY); //send mouse movement to server
                        if(disX!=0)
                        {
                            setValueX=true;
                        }
                        if(disY!=0)
                        {
                            setValueY=true;
                        }

                    }

            }
        }
    }


    @Override

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

        // not in use

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if(id == R.id.action_connect) {
            ConnectPhoneTask connectPhoneTask = new ConnectPhoneTask();
            connectPhoneTask.execute("192.168.137.1"); //try to connect to server in another thread
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //OnClick method is called when any of the buttons are pressed
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.playPauseButton:
                if (isConnected && out!=null) {
                    out.println("play");//send "play" to server
                }
                break;
            case R.id.nextButton:
                if (isConnected && out!=null) {
                    out.println("next"); //send "next" to server
                }
                break;
            case R.id.previousButton:
                if (isConnected && out!=null) {
                    out.println("previous"); //send "previous" to server
                }
                break;
        }

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(isConnected && out!=null) {
            try {
                out.println("exit"); //tell server to exit
                socket.close(); //close socket
            } catch (IOException e) {
                Log.e("remotedroid", "Error in closing socket", e);
            }
        }
    }

    public class ConnectPhoneTask extends AsyncTask<String,Void,Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            boolean result = true;
            try {
                InetAddress serverAddr = InetAddress.getByName(params[0]);
                socket = new Socket(serverAddr,8998 );//Open socket on server IP and port
            } catch (IOException e) {
                Log.e("remotedroid", "Error while connecting:"+e.toString() ,e);
                result = false;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            isConnected = result;
            Toast.makeText(context,isConnected?"Connected to server!":"Error while connecting",Toast.LENGTH_LONG).show();
            try {
                if(isConnected) {
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
                            .getOutputStream())), true); //create output stream to send data to server

                }
            }catch (IOException e){
                Log.e("remotedroid", "Error while creating OutWriter", e);
                Toast.makeText(context,"Error while connecting",Toast.LENGTH_LONG).show();
            }
        }
    }
}
