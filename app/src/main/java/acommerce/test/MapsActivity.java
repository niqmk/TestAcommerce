package acommerce.test;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import acommerce.test.model.Response;
import acommerce.test.model.Result;
import retrofit.Callback;
import retrofit.Retrofit;

public class MapsActivity extends FragmentActivity implements
		GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener,
		OnMapReadyCallback,
		GoogleMap.OnMapClickListener,
		GoogleMap.OnMarkerClickListener,
		LocationListener {

	private final int UPDATE_INTERVAL = 10000;
	private final int FASTEST_INTERVAL = 9000;
	private final int RADIUS = 10000;
	private final String PLACE_SEARCH = "food";

	private GoogleMap mMap;
	private GoogleApiClient googleApiClient;
	private Location lastLocation;
	private LocationRequest locationRequest;
	private Marker locationMarker;
	private List<Marker> placesMarker;
	private Circle geoFenceLimits;
	private PlacesApiHelper mHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		mHelper = new PlacesApiHelper(this);
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);
		setInitial();
	}

	@Override
	protected void onStart() {
		super.onStart();
		googleApiClient.connect();
	}

	@Override
	protected void onStop() {
		super.onStop();
		googleApiClient.disconnect();
	}

	@Override
	public void onConnected(@Nullable Bundle bundle) {
		getLastKnownLocation();
	}

	@Override
	public void onConnectionSuspended(int i) {
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;
		mMap.setOnMapClickListener(this);
		mMap.setOnMarkerClickListener(this);
	}

	public void onMapClick(LatLng latLng) {
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		return false;
	}

	@Override
	public void onLocationChanged(Location location) {
		lastLocation = location;
		writeActualLocation(location);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			case 1001: {
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					getLastKnownLocation();

				} else {
					permissionsDenied();
				}
				break;
			}
		}
	}

	private void setInitial() {
		if (googleApiClient == null) {
			googleApiClient = new GoogleApiClient.Builder(this)
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this)
					.addApi(LocationServices.API)
					.addApi(Places.GEO_DATA_API)
					.addApi(Places.PLACE_DETECTION_API)
					.build();
		}
	}

	private void getLastKnownLocation() {
		if (checkPermission()) {
			lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
			if (lastLocation != null) {
				writeLastLocation();
				startLocationUpdates();
			} else {
				startLocationUpdates();
			}
		} else {
			askPermission();
		}
	}

	private void startLocationUpdates() {
		locationRequest = LocationRequest.create()
				.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
				.setInterval(UPDATE_INTERVAL)
				.setFastestInterval(FASTEST_INTERVAL);

		if (checkPermission())
			LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
	}

	private void writeActualLocation(Location location) {
		markerLocation(new LatLng(location.getLatitude(), location.getLongitude()));
	}

	private void writeLastLocation() {
		writeActualLocation(lastLocation);
	}

	private boolean checkPermission() {
		return (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
				== PackageManager.PERMISSION_GRANTED);
	}

	private void askPermission() {
		ActivityCompat.requestPermissions(
				this,
				new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
				1001
		);
	}

	private void permissionsDenied() {
	}

	private void markerLocation(LatLng latLng) {
		String title = latLng.latitude + ", " + latLng.longitude;
		MarkerOptions markerOptions = new MarkerOptions()
				.position(latLng)
				.title(title);
		if (mMap != null) {
			if (locationMarker != null)
				locationMarker.remove();
			locationMarker = mMap.addMarker(markerOptions);
			float zoom = 10f;
			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
			mMap.animateCamera(cameraUpdate);
			searchPlaces();
			drawGeofence();
		}
	}

	private void drawGeofence() {
		if (geoFenceLimits != null)
			geoFenceLimits.remove();
		CircleOptions circleOptions = new CircleOptions()
				.center(locationMarker.getPosition())
				.strokeColor(Color.argb(50, 70, 70, 70))
				.fillColor(Color.argb(100, 150, 150, 150))
				.radius(RADIUS);
		geoFenceLimits = mMap.addCircle(circleOptions);
	}

	private void searchPlaces() {
		mHelper.requestPlaces(PLACE_SEARCH, new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), RADIUS, mResultCallback);
	}

	private Callback<Response> mResultCallback = new Callback<Response>() {
		@Override
		public void onResponse(retrofit.Response<Response> response, Retrofit retrofit) {
			List<Result> results = response.body().getResults();
			if (placesMarker != null) {
				for (Marker marker : placesMarker) {
					marker.remove();
				}
			}
			placesMarker = new ArrayList<>();
			for (Result r : results) {
				acommerce.test.model.Location location = r.getGeometry().getLocation();
				LatLng latLng = new LatLng(location.getLat(), location.getLng());
				String name = r.getName();
				placesMarker.add(mMap.addMarker(new MarkerOptions().position(latLng).title(name)));
			}
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 15));
		}

		@Override
		public void onFailure(Throwable t) {
			t.printStackTrace();
		}
	};
}