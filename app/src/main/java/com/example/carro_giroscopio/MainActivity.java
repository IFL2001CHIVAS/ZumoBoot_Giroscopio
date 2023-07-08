package com.example.carro_giroscopio;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String DEVICE_ADDRESS = "98:DA:20:05:39:CC"; // Dirección MAC del módulo Bluetooth HC-04
    private static final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

    private Button connectButton,btnactive;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private float accelerometerValueX, accelerometerValueY, accelerometerValueZ;

    private Handler handler;
    private Runnable sendRunnable;

    private static final int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        connectButton = findViewById(R.id.connectButton);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter == null) {
                    Toast.makeText(MainActivity.this, "El dispositivo no admite Bluetooth", Toast.LENGTH_SHORT).show();
                } else if (!bluetoothAdapter.isEnabled()) {
                    // Habilitar Bluetooth si no está habilitado
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    Intent bten = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    lanzarinteten.launch(bten);
                    Toast.makeText(MainActivity.this, "Bluetooth habilitado", Toast.LENGTH_SHORT).show();
                } else {
                    // Conectar con el dispositivo Bluetooth
                    bluetoothDevice = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
                    try {
                        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(PORT_UUID);
                        bluetoothSocket.connect();
                        outputStream = bluetoothSocket.getOutputStream();
                        Toast.makeText(MainActivity.this, "Conexión Bluetooth establecida", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d("errorbt",e.toString());
                    }
                }
            }
        });


        // Inicializar el sensor del acelerómetro
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // Configurar el envío constante de la coordenada X del acelerómetro
        handler = new Handler();
        sendRunnable = new Runnable() {
            @Override
            public void run() {
                sendAccelerometerValueX();
                handler.postDelayed(this, 2000); //inicial 1 miliseguo// Intervalo de envío (en milisegundos)
            }
        };


        btnactive = findViewById(R.id.Btnactivarblue);
        btnactive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothPermison();
            }
        });

    }


    public void BluetoothPermison(){
        // Verificar si los permisos están concedidos
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            // Solicitar el permiso
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_ENABLE_BT);
            }
        } else {
            // El permiso ya está concedido, puedes realizar las operaciones necesarias
            if (!bluetoothAdapter.isEnabled()) {
                Intent bten = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                lanzarinteten.launch(bten);
            }

        }

    }

    ActivityResultLauncher<Intent> lanzarinteten = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // El usuario ha activado el Bluetooth
                        // Realiza las operaciones necesarias aquí
                        Toast.makeText(getApplicationContext(), "bluetooth on", Toast.LENGTH_SHORT).show();
                    } else {
                        // El usuario no ha activado el Bluetooth
                        // Realiza las acciones correspondientes aquí

                    }
                }
            });

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "El dispositivo no admite el sensor de acelerómetro", Toast.LENGTH_SHORT).show();
        }

        // Iniciar el envío constante cuando se reanuda la actividad
        handler.post(sendRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        // Detener el envío constante cuando se pausa la actividad
        handler.removeCallbacks(sendRunnable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Actualizar los valores del acelerómetro
            accelerometerValueX = event.values[0];
            accelerometerValueY = event.values[1];
            accelerometerValueZ = event.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No es necesario implementar esto aquí
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothSocket != null) {
            try {
                outputStream.close();
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendAccelerometerValueX() {
        // Verificar si la conexión Bluetooth está establecida y el outputStream no es nulo
        if (bluetoothSocket != null && outputStream != null) {
            // Enviar solo el valor de la coordenada X del acelerómetro a través del Bluetooth
            String message = "";
            if(accelerometerValueX >= 0 && accelerometerValueX <2){
                message ="forward";
            } else if (accelerometerValueX <= 0 && accelerometerValueX > -2) {
                message ="backward";
            } else if (accelerometerValueX >= 5) {
                message ="left";
            }
            else if (accelerometerValueX <= -5) {
                message ="right";
            }
            else{
                message = "stop";
            }
            try {
                outputStream.write(message.getBytes());
                //Toast.makeText(MainActivity.this, "Mensaje enviado", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainActivity.this, "Error en la conexión Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }
}
