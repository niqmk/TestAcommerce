package acommerce.test;

import acommerce.test.model.Response;
import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Query;

public interface PlaceApiService {
	@GET("/maps/api/place/search/json")
	Call<Response> requestPlaces(@Query("types") String types,
	                             @Query("location") String location,
	                             @Query("radius") String radius,
	                             @Query("sensor") String sensor,
	                             @Query("key") String key);
}