package com.osspl.gpstracker;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements LocationListener {

	private GoogleMap m_googleMap;
	Button m_actionButton;
	TextView txt_speed,txt_waitingtime;
	GPSDatabase m_GPSTracker;
	LocationManager m_locationManager;
	ArrayList<String> m_lengthOfTime;
	ArrayList<String> m_speedList;
	ArrayList<String> m_waitingList;
	float m_totalSpeed;
	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	private int permission = 1;
	private Location currentLocation;
	boolean track=false;
	long starttime = 0L;
	long timeInMilliseconds = 0L;
	long timeSwapBuff = 0L;
	long updatedtime = 0L;
	int t = 1;
	int secs = 0;
	int mins = 0;
	int milliseconds = 0;
	Handler handler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.txt_waitingtime=(TextView)findViewById(R.id.txt_waitingtime);
		this.txt_speed=(TextView)findViewById(R.id.txt_speed);
		this.m_actionButton = (Button) findViewById(R.id.actionButton);
		this.m_actionButton.setText("Start");
		this.m_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		this.m_GPSTracker = new GPSDatabase(this);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

			if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, permission);
			}
			if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, permission);
			}
			if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, permission);
			}
			if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, permission);
			}

		}
		if (!m_locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			buildAlertMessageNoGps();
		}
		else{
			try {
				initilizeMap();
			}    // Loading map
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public Runnable updateTimer = new Runnable() {
		public void run() {
			try {
				timeInMilliseconds = SystemClock.uptimeMillis() - starttime;
				updatedtime = timeSwapBuff + timeInMilliseconds;
				secs = (int) (updatedtime / 1000);
				mins = secs / 60;
				secs = secs % 60;
				milliseconds = (int) (updatedtime % 1000);
				txt_waitingtime.setText("" + mins + ":" + String.format("%02d", secs) + ":"
						+ String.format("%03d", milliseconds));
				txt_waitingtime.setTextColor(Color.RED);
				handler.postDelayed(this, 0);
			}catch (Exception e)
			{

			}
		}
	};

	//	Initializes map and some variables
	private void initilizeMap() {
		if (this.m_googleMap == null) {
			this.m_googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
			if (this.m_googleMap == null)    // Checks if map is created successfully or not
				Toast.makeText(getApplicationContext(), "Sorry! unable to create maps", Toast.LENGTH_SHORT).show();
			else {
				CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(20.5937, 78.9629)).zoom(10).build();    //	Centers map to O'Connell Street with a zoom of 10
				this.m_googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));    //	Makes the animation of the camera's move
				this.m_googleMap.setMyLocationEnabled(true);    //	Continuously displays the user's location
				this.m_speedList = new ArrayList<String>();
				this.m_waitingList = new ArrayList<String>();
				this.m_totalSpeed = 0;
				this.m_lengthOfTime = new ArrayList<String>();
				this.m_googleMap.clear();    //	Removes the previous drawings
				this.m_GPSTracker.clear();    //	Empties the database
				this.m_speedList.clear();
				this.m_waitingList.clear();

			}
		}
	}

	public void results(View v) {
		if (this.m_actionButton.getText().toString() == "Stop")
			Toast.makeText(this, "Tracking must be stopped!", Toast.LENGTH_SHORT).show();
		else {
			ArrayList<LatLng> directionPoints = this.m_GPSTracker.getAllTracks();
			if (directionPoints.isEmpty())    //	If there are not tracks, leaves
				Toast.makeText(getApplicationContext(), "No data available!", Toast.LENGTH_SHORT).show();
			else {
				ArrayList<String> distanceList = new ArrayList<String>();
				Location locationA = new Location("A");
				Location locationB = new Location("B");
				float totalDistance = 0;
				for (int i = 0; i < directionPoints.size(); i++) {
					if (i != directionPoints.size() - 1) {
						locationA.setLatitude(directionPoints.get(i).latitude);
						locationA.setLongitude(directionPoints.get(i).longitude);
						locationB.setLatitude(directionPoints.get(i + 1).latitude);
						locationB.setLongitude(directionPoints.get(i + 1).longitude);
						distanceList.add(Float.toString(locationA.distanceTo(locationB)));
						totalDistance += locationA.distanceTo(locationB);
					}
				}
				Intent myIntent = new Intent(this, ResultsActivity.class);
				Bundle bundle = new Bundle();
				bundle.putStringArrayList("speedList", this.m_speedList);    //	Adds average speed to the extra content
				bundle.putFloat("totalSpeed", this.m_totalSpeed);  //	Adds total speed
				bundle.putStringArrayList("waitinglist", this.m_waitingList);
				bundle.putString("waitingtime",txt_waitingtime.getText().toString());
				bundle.putStringArrayList("distanceList", distanceList);    //	Adds distance list to the extra content
				bundle.putFloat("totalDistance", totalDistance);    //	Adds total distance
				bundle.putStringArrayList("time", this.m_lengthOfTime);    //	Adds length of time to the extra content
				myIntent.putExtras(bundle);
				this.startActivity(myIntent);//	Starts a new activity with a stored content
				txt_waitingtime.setText("00:00:00");
			}
		}
	}

	public void actionButton(View v) {
		if (this.m_actionButton.getText().toString() == "Start") {
			this.m_actionButton.setText("Stop");
			track=true;
			this.m_totalSpeed = 0;
			this.m_lengthOfTime.clear();
			//	Here I offer two options: either you are using satellites or the Wi-Fi services to get user's location
			this.m_locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 0, this);    //	User's location is retrieve every 3 seconds
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				// TODO: Consider calling

				return;
			}
			this.m_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, this);

		} else {
			this.m_actionButton.setText("Start");
			starttime = 0L;
			timeInMilliseconds = 0L;
			timeSwapBuff = 0L;
			updatedtime = 0L;
			t = 1;
			secs = 0;
			mins = 0;
			milliseconds = 0;
			handler.removeCallbacks(updateTimer);
			this.m_locationManager.removeUpdates(this);    //	Stops the tracking
		}
	}

	private void drawPath() {
		ArrayList<LatLng> directionPoints = this.m_GPSTracker.getAllTracks();
		if (directionPoints.isEmpty())    //	If there are not tracks, leaves
			return;
		PolylineOptions rectLine = new PolylineOptions().width(5).color(Color.RED);    //	Customizes the line in red with a width of 5
		rectLine.addAll(directionPoints);    //	Adds all the tracks in the line that is going to be drawn
		this.m_googleMap.addPolyline(rectLine);    //	Adds the new line and draws it
		CameraPosition cameraPosition = new CameraPosition.Builder().target(directionPoints.get(0)).zoom(50).build();    //	Centers the camera on the first track
		this.m_googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!m_locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			buildAlertMessageNoGps();
		}
		//	Puts the location updates back
//		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//				// TODO: Consider calling
//
//				return;
//			}
//			this.m_locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 0, this);
//			this.m_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, this);

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions

			return;
		}
		this.m_locationManager.removeUpdates(this);    //	Removes the updates when user is using another application
	}
    
    @Override
    protected void 	onStop()
    {
        super.onStop();
        this.m_GPSTracker.close();	//	Closes the database
    }
    
    //	Automatically called when location changed
    public 	void onLocationChanged(Location loc) {
		if (track) {
			if (loc == null)    //	Filtering out null values
				return;
			this.m_speedList.add(Float.toString((float) (loc.getSpeed() * 3.6)));    //	Adds location speed
			this.m_totalSpeed += loc.getSpeed() * 3.6;
			txt_speed.setText(String.valueOf(m_totalSpeed));
			this.m_lengthOfTime.add(sdf.format(new Date(loc.getTime())));//	Getting	location time
			this.m_waitingList.add(txt_waitingtime.getText().toString());  //	Inserting waiting time
			this.m_GPSTracker.insertRow(loc.getLatitude(), loc.getLongitude());    //	Inserting in database the coordinates
			this.drawPath();

			if (m_totalSpeed < 2.0) {
				if (t == 1) {
					starttime = SystemClock.uptimeMillis();
					handler.postDelayed(updateTimer, 0);
					t = 0;
				}
			}
			else
			{
				timeSwapBuff += timeInMilliseconds;
				handler.removeCallbacks(updateTimer);
				t = 1;
			}
		}
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode == permission) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

			}else if (grantResults[0] == PackageManager.PERMISSION_DENIED){
			finish();
			}
		}

	}

	private void buildAlertMessageNoGps() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
				.setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
						startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
						try {
							initilizeMap();
						}    // Loading map
						catch (Exception e) {
							e.printStackTrace();
						}
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
						finish();
					}
				});
		final AlertDialog alert = builder.create();
		alert.show();
	}
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		return super.onOptionsItemSelected(item);
	}

	public void onProviderDisabled(String provider)
	{
		// TODO Auto-generated method stub
	}
	
	public void onProviderEnabled (String provider)
	{
		// TODO Auto-generated method stub
	}
	
	public void onStatusChanged(String provider, int status, Bundle extras) 
	{
		// TODO Auto-generated method stub
	}
}
