package android.example.com.medbot;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewManager;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;

import java.io.IOException;

import io.github.controlwear.virtual.joystick.android.JoystickView;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private JoystickView joystick_right;
    private JoystickView joystick_left;
    private JoystickView temp_joystick;
    private TextView coord_1;
    private TextView coord_2;
    private TextView temp_coord;
    private FloatingActionButton connect_server;
    private FloatingActionButton reverse;
    private String direction = "s";
    private String motion = "s";
    private int left_count;
    private int right_count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        joystick_right = findViewById(R.id.joystick_right);
        joystick_left = findViewById(R.id.joystick_left);
        connect_server = findViewById(R.id.connect_button);
        reverse = findViewById(R.id.reverse);
        coord_1 = findViewById(R.id.coord_1);
        coord_2 = findViewById(R.id.coord_2);
        left_count = 0;
        right_count = 0;

        reverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                temp_joystick = joystick_left;
                joystick_left = joystick_right;
                joystick_right = temp_joystick;
                swapPositions(coord_1, coord_2);
                temp_coord = coord_1;
                coord_1 = coord_2;
                coord_2 = temp_coord;
                Toast.makeText(MainActivity.this, "Controller swapped", Toast.LENGTH_SHORT).show();
            }
        });

        connect_server.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            connectServer();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }
        });

        joystick_left.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(final int angle, final int strength) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            direction = String.valueOf(getDirection(angle));
                            left_count++;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    coord_2.setText("Strength : " + strength + "\nAngle : " + angle + "\nDirection : " + direction);
                                }
                            });
                            if (left_count > 10) {
                                sendCoordinates(direction, motion);
                                left_count = 0;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }
        });

        joystick_right.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(final int angle, final int strength) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            motion = getMotion(angle);
                            right_count++;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    coord_1.setText("Strength : " + strength + "\nAngle : " + angle + "\nMotion : " + motion);
                                }
                            });
                            if (right_count > 10) {
                                sendCoordinates(direction, motion);
                                right_count = 0;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }
        });
    }

    private void swapPositions(TextView coord_1, TextView coord_2) {
        ViewManager viewManager1 = (ViewManager) coord_1.getParent();
        ViewManager viewManager2 = (ViewManager) coord_2.getParent();

        viewManager1.removeView(coord_1);
        viewManager2.removeView(coord_2);

        viewManager1.addView(coord_2, coord_2.getLayoutParams());
        viewManager2.addView(coord_1, coord_1.getLayoutParams());
    }

    private void sendCoordinates(String direction, String motion) throws IOException {
        String body = direction + "," + motion;
        String url = "http://192.168.43.100:8000/controller/";

        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = RequestBody.create(MediaType.parse("text/plain"), body);

        Request request = new Request.Builder().url(url).post(requestBody).build();
        okhttp3.Response response = client.newCall(request).execute();
        Log.e("MainActivity", response.toString());

        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }
    }

    private void connectServer() throws IOException {
        String url = "http://192.168.43.100:8000/controller/connect/";
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        Log.e("MainActivity : connectServer", response.toString());

        if (!response.isSuccessful()) {
            throw new IOException("Unexpected response : " + response);
        }
        if (response.body().equals("ok")) {
            Toast.makeText(MainActivity.this, "Connected to MedBot", Toast.LENGTH_SHORT).show();
        }
    }

    private String getDirection(int angle) {
        if (angle > 90 && angle < 270)
            return "l";//Math.round(strength * 0.45) + 90;
        else if (angle > 270 && angle < 360)
            return "r";//Math.abs(Math.round(strength * 0.45) - 90);
        else if (angle > 0 && angle < 90)
            return "r";//Math.abs(Math.round(strength * 0.45) - 90);
        else
            return "s";
    }

    private String getMotion(int angle) {
        if (angle > 0 && angle < 180)
            return "f";
        else if (angle > 180 && angle < 360)
            return "b";
        else
            return "s";
    }
}
