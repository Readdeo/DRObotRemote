package com.robotlient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.StringTokenizer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.R.string;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.Toast;

import com.javacodegeeks.android.androidsocketclient.R;

public class Client extends Activity {
	LocationManager lm;
	LocationListener mListener;
	private ServerSocket serverSocket;

	Handler updateConversationHandler;
	LocationManager locationmanager;
	Thread serverThread = null;

	final String uploadLink = "http://readdeo.uw.hu/uploadData.php";
	String serverIpAddress;
	final String IPLink = "http://readdeo.uw.hu/uploads/IP.txt";
	final String IP2Link = "http://readdeo.uw.hu/uploads/IP2.txt";

	String PLatitude;
	String PLongitude;
	String PBearing;
	String PAltitude;

	float heading, Girány, nGirány;

	private Socket socket;
	string stringText;
	Button startGPS, stopGPS, startClient, stopClient;
	TextView txv, tv2, tv3, tv4, tv5, tv6;
	private static final int SERVERPORT = 1598;
	public static final int LSERVERPORT = 1599;
	ImageView imgvw;

	// IP feltöltése
	private void doFileUpload() {

		HttpURLConnection conn = null;
		DataOutputStream dos = null;
		DataInputStream inStream = null;
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";
		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1 * 1024 * 1024;
		String responseFromServer = "";

		try {

			// ------------------ CLIENT REQUEST
			FileInputStream fileInputStream = new FileInputStream(new File(
					String.valueOf(getFilesDir() + "/IP2.txt")));
			// open a URL connection to the Servlet
			URL url = new URL(uploadLink);
			// Open a HTTP connection to the URL
			conn = (HttpURLConnection) url.openConnection();
			// Allow Inputs
			conn.setDoInput(true);
			// Allow Outputs
			conn.setDoOutput(true);
			// Don't use a cached copy.
			conn.setUseCaches(false);
			// Use a post method.
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Content-Type",
					"multipart/form-data;boundary=" + boundary);
			dos = new DataOutputStream(conn.getOutputStream());
			dos.writeBytes(twoHyphens + boundary + lineEnd);
			dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\""
					+ getFilesDir() + "/IP2.txt" + "\"" + lineEnd);
			dos.writeBytes(lineEnd);
			// create a buffer of maximum size
			bytesAvailable = fileInputStream.available();
			bufferSize = Math.min(bytesAvailable, maxBufferSize);
			buffer = new byte[bufferSize];
			// read file and write it into form...
			bytesRead = fileInputStream.read(buffer, 0, bufferSize);

			while (bytesRead > 0) {

				dos.write(buffer, 0, bufferSize);
				bytesAvailable = fileInputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);

			}

			// send multipart form data necesssary after file data...
			dos.writeBytes(lineEnd);
			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
			// close streams

			fileInputStream.close();
			dos.flush();
			dos.close();

		} catch (MalformedURLException ex) {

		} catch (IOException ioe) {

			;
		}

		// ------------------ read the SERVER RESPONSE
		try {

			inStream = new DataInputStream(conn.getInputStream());
			String str;

			while ((str = inStream.readLine()) != null) {
			}

			inStream.close();

		} catch (IOException ioex) {

		}

	}

	// ip
	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {

		}
		return null;
	}

	public static boolean available(int port) {
		port = SERVERPORT;
		ServerSocket ss = null;
		DatagramSocket ds = null;
		try {
			ss = new ServerSocket(port);
			ss.setReuseAddress(true);
			ds = new DatagramSocket(port);
			ds.setReuseAddress(true);
			return true;
		} catch (IOException e) {
		} finally {
			if (ds != null) {
				ds.close();
			}

			if (ss != null) {
				try {
					ss.close();
				} catch (IOException e) {
					/* should not be thrown */
				}
			}
		}

		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		startGPS = (Button) findViewById(R.id.button1);
		stopGPS = (Button) findViewById(R.id.button2);
		startClient = (Button) findViewById(R.id.button3);
		stopClient = (Button) findViewById(R.id.button4);
		txv = (TextView) findViewById(R.id.textView1);
		tv2 = (TextView) findViewById(R.id.textView2);
		tv3 = (TextView) findViewById(R.id.textView3);
		tv4 = (TextView) findViewById(R.id.textView4);
		tv5 = (TextView) findViewById(R.id.textView5);
		tv6 = (TextView) findViewById(R.id.textView6);

		imgvw = (ImageView) findViewById(R.id.imageView1);

		updateConversationHandler = new Handler();
		this.serverThread = new Thread(new ServerThread());
		this.serverThread.start();

		SensorManager sensorService;
		Sensor sensor;
		sensorService = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensor = sensorService.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		sensorService.registerListener(mySensorEventListener, sensor,
				SensorManager.SENSOR_DELAY_UI);
//NMEA

		locationmanager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationmanager.addNmeaListener(new NmeaListener() {
		                public void onNmeaReceived(long timestamp, String nmea) {
		                tv3.setText(timestamp+" "+nmea);
		              //      Log.d(TAG,"Nmea Received :");
		              //      Log.d("","Timestamp is :" +timestamp+"   nmea is :"+nmea);


		                }});
		// IP txt-be írása
		FileWriter fw;
		try {
			fw = new FileWriter(getFilesDir() + "/IP2.txt", false);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.append(getLocalIpAddress());
			bw.close();
			fw.close();

			MediaScannerConnection.scanFile(Client.this,
					new String[] { getFilesDir() + "/IP2.txt" }, null,
					new MediaScannerConnection.OnScanCompletedListener() {
						@Override
						public void onScanCompleted(String path, Uri uri) {
						}
					});
		} catch (IOException e) {
			Toast t = Toast.makeText(Client.this, "IOException",
					Toast.LENGTH_SHORT);
			t.show();

		}

		// IP beolvasás
		DefaultHttpClient httpclient = new DefaultHttpClient();
		try {
			HttpGet httppost = new HttpGet(IPLink);
			HttpResponse response;

			response = httpclient.execute(httppost);

			HttpEntity ht = response.getEntity();

			BufferedHttpEntity buf = new BufferedHttpEntity(ht);

			InputStream is = buf.getContent();

			BufferedReader r = new BufferedReader(new InputStreamReader(is));

			StringBuilder total = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				total.append(line);
				serverIpAddress = String.valueOf(total);

			}
		} catch (ClientProtocolException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}

		// IP olvasás vége
		new Thread(new ClientThread()).start();
		startGPS.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {

					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
					out.println("GPSstart");
				} catch (UnknownHostException e) {
					e.printStackTrace();
					Toast t = Toast.makeText(Client.this,
							"UnknownHostException", Toast.LENGTH_SHORT);
					t.show();
				} catch (IOException e) {
					e.printStackTrace();
					Toast t = Toast.makeText(Client.this, "IOException ",
							Toast.LENGTH_SHORT);
					t.show();
				} catch (Exception e) {
					e.printStackTrace();
					Toast t = Toast.makeText(Client.this, "Exception",
							Toast.LENGTH_SHORT);
					t.show();
				}

			}
		});
		stopGPS.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {

					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
					out.println("GPSstop");
				} catch (UnknownHostException e) {
					e.printStackTrace();
					Toast t = Toast.makeText(Client.this,
							"UnknownHostException", Toast.LENGTH_SHORT);
					t.show();
				} catch (IOException e) {
					e.printStackTrace();
					Toast t = Toast.makeText(Client.this, "IOException ",
							Toast.LENGTH_SHORT);
					t.show();
				} catch (Exception e) {
					e.printStackTrace();
					Toast t = Toast.makeText(Client.this, "Exception",
							Toast.LENGTH_SHORT);
					t.show();
				}

			}
		});
		startClient.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {

					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
					out.println("stClient");
				} catch (UnknownHostException e) {
					e.printStackTrace();
					Toast t = Toast.makeText(Client.this,
							"UnknownHostException", Toast.LENGTH_SHORT);
					t.show();
				} catch (IOException e) {
					e.printStackTrace();
					Toast t = Toast.makeText(Client.this, "IOException ",
							Toast.LENGTH_SHORT);
					t.show();
				} catch (Exception e) {
					e.printStackTrace();
					Toast t = Toast.makeText(Client.this, "Exception",
							Toast.LENGTH_SHORT);
					t.show();
				}

			}
		});
		stopClient.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {

					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
					out.println("spClient");
				} catch (UnknownHostException e) {
					e.printStackTrace();
					Toast t = Toast.makeText(Client.this,
							"UnknownHostException", Toast.LENGTH_SHORT);
					t.show();
				} catch (IOException e) {
					e.printStackTrace();
					Toast t = Toast.makeText(Client.this, "IOException ",
							Toast.LENGTH_SHORT);
					t.show();
				} catch (Exception e) {
					e.printStackTrace();
					Toast t = Toast.makeText(Client.this, "Exception",
							Toast.LENGTH_SHORT);
					t.show();
				}

			}
		});

		doFileUpload();
		// GPS jelek olvasása
		lm = (LocationManager) this.getSystemService(LOCATION_SERVICE);
		mListener = new LocationListener() {

			@Override
			public void onLocationChanged(Location loc) {

				if (PLatitude != null) {
					double destLat = Double.parseDouble(PLatitude);
					double destLng = Double.parseDouble(PLongitude);
					double currentLat = loc.getLatitude();
					double currentLng = loc.getLongitude();

					final float[] results = new float[3];
					Location.distanceBetween(currentLat, currentLng, destLat,
							destLng, results);
					float r0 = results[0];
					float bearing = results[1];
					float r2 = results[2];

					heading = (bearing - nGirány);

					 rotateArrow(heading);

					tv2.setText("Távolság: " + String.valueOf(r0) + " m");
					tv3.setText(String.valueOf(bearing));
					tv4.setText(String.valueOf(r2));
					txv.setText(heading + " °");

				}// TODO
			}

			@Override
			public void onProviderDisabled(String provider) {

			}

			@Override
			public void onProviderEnabled(String provider) {

			}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {

			}

		};
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0, mListener);
	}

	class ClientThread implements Runnable {

		@Override
		public void run() {

			try {
				InetAddress serverAddr = InetAddress.getByName(serverIpAddress);

				socket = new Socket(serverAddr, SERVERPORT);

			} catch (UnknownHostException e1) {
				e1.printStackTrace();

			} catch (IOException e1) {
				e1.printStackTrace();

			}

		}

	}

	@Override
	protected void onDestroy() {
		try {
			serverSocket.close();
		} catch (IOException e) {
	
			e.printStackTrace();
		}
		super.onDestroy();
	}

	protected void onStop() {

		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public IBinder onBind(Intent intent) {
	
		return null;
	}

	class ServerThread implements Runnable {

		public void run() {
			Socket socket = null;
			try {
				serverSocket = new ServerSocket(LSERVERPORT);
			} catch (IOException e) {
				e.printStackTrace();
			}
			while (!Thread.currentThread().isInterrupted()) {

				try {

					socket = serverSocket.accept();

					CommunicationThread commThread = new CommunicationThread(
							socket);
					new Thread(commThread).start();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	class CommunicationThread implements Runnable {

		private Socket clientSocket;

		private BufferedReader input;

		public CommunicationThread(Socket clientSocket) {

			this.clientSocket = clientSocket;

			try {

				this.input = new BufferedReader(new InputStreamReader(
						this.clientSocket.getInputStream()));

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {

			while (!Thread.currentThread().isInterrupted()) {

				try {

					String read = input.readLine();

					updateConversationHandler.post(new updateUIThread(read));

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	class updateUIThread implements Runnable {
		private String msg;

		public updateUIThread(String str) {
			this.msg = str;
		}

		@Override
		public void run() {
			if (msg != null) {
				StringTokenizer tokens = new StringTokenizer(msg, ";");
				String Latitude = tokens.nextToken();
				String Longitude = tokens.nextToken();
				// String Bearing = tokens.nextToken();
				// String Altitude = tokens.nextToken();

				PLatitude = Latitude;
				PLongitude = Longitude;
				// PBearing = Bearing;
				// PAltitude = Altitude;
				tv5.setText(msg);
			}
		}

	}

	private SensorEventListener mySensorEventListener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(final SensorEvent event) {
			final float azimuth = event.values[0];
		    Girány = azimuth;

		}
	};
	
	  private void rotateArrow(float angle){
	  
	  Matrix matrix = new Matrix(); imgvw.setScaleType(ScaleType.MATRIX);
	  matrix.postRotate(angle, 27f, 27f); imgvw.setImageMatrix(matrix); }
	  
      private float normalizeDegree(float nGirány){
          if(Girány >= 0.0f && Girány <= 180.0f){
              return Girány;
          }else{
        	  //TODO talán ez a szar
         //     return 180 + (180 + Girány);
              return Girány;
          }
      }
}