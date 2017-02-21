package acommerce.test;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.TimeUnit;

import acommerce.test.model.Response;
import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

public class PlacesApiHelper {
	private Context mContext;

	public PlacesApiHelper(Context context) {
		mContext = context;
	}

	public void requestPlaces(String types, LatLng latLng, int radius, Callback<Response> callback) {
		final OkHttpClient okHttpClient = new OkHttpClient();
		okHttpClient.setReadTimeout(60, TimeUnit.SECONDS);
		okHttpClient.setConnectTimeout(60, TimeUnit.SECONDS);
		okHttpClient.setWriteTimeout(60, TimeUnit.SECONDS);
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl("https://maps.googleapis.com/")
				.addConverterFactory(GsonConverterFactory.create())
				.client(okHttpClient)
				.build();
		PlaceApiService service = retrofit.create(PlaceApiService.class);
		Call<Response> call = service.requestPlaces(types,
				String.valueOf(latLng.latitude) + "," + String.valueOf(latLng.longitude),
				String.valueOf(radius),
				"false",
				mContext.getString(R.string.google_maps_key_browser));
		call.enqueue(callback);
	}
}