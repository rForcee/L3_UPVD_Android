package imerir.licence.ar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import imerir.licence.ar.model.ARPoint;

/**
 * Classe pour l'implémentation des GoogleMaps concernant l'activité activity_maps
 */
public class ARMaps extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String origin;
    private String destination;

    private Location destinationLocation;
    private Location originLocation;
    private String destinationName;
    private String destinationUrl;


    /**
     * Lors de la création de l'activité : récupère les informations passées par l'AROverlayView et créé une map
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        destinationLocation = getIntent().getParcelableExtra("destinationLocation");
        destinationName = getIntent().getStringExtra("destinationName");
        destinationUrl = getIntent().getStringExtra("destinationUrl");
        originLocation = getIntent().getParcelableExtra("originLocation");

        origin = String.valueOf(originLocation.getLatitude()) + "," + String.valueOf(originLocation.getLongitude());
        destination = String.valueOf(destinationLocation.getLatitude()) + "," + String.valueOf(destinationLocation.getLongitude());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    /**
     * Ajoute les différents marqueurs : position + monument (ARPoint)
     * Le monument est cliquable et affiche un titre + si ce titre est cliqué, renvoie vers la page wikipédia du monument
     * + possibilité de créer l'itinéraire sur maps avec un bouton en bas à droite de la vue
     *
     * Utilise getDirection pour l'affichage de l'itinéraire
     * @param googleMap la map
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

            @Override
            public void onInfoWindowClick(Marker marker) {

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(destinationUrl));
                startActivity(browserIntent);
            }
        });

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(originLocation.getLatitude(), originLocation.getLongitude()), 15));
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(originLocation.getLatitude(), originLocation.getLongitude()))
                .title("Votre position"));
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(destinationLocation.getLatitude(), destinationLocation.getLongitude()))
                .title("" + destinationName).snippet("Cliquer pour en savoir plus...").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        new GetDirection().execute();
    }


    /**
     * Classe permettant de créer un itinéraire
      */
    class GetDirection extends AsyncTask<String, String, String> {

        private ProgressDialog dialog;
        private List<LatLng> pontos;

        /**
         * Affiche un message de création d'itinéraire
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(ARMaps.this);
            dialog.setMessage("Itinéraire en cours de création.");
            dialog.setIndeterminate(false);
            dialog.setCancelable(false);
            dialog.show();
        }

        /**
         * Tâche executée en fond
         * Permet de récupérer le JSON nécessaire grâce à l'API Google en fonction de l'origine et la destination
         * Utilise la méthode decodePoly pour utiliser le JSON récupérer afin de créer un itinéraire
         * @return null
         */
        protected String doInBackground(String... args) {
            String stringUrl = "http://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination + "&sensor=false";
            StringBuilder response = new StringBuilder();
            try {
                URL url = new URL(stringUrl);
                HttpURLConnection httpconn = (HttpURLConnection) url
                        .openConnection();
                if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader input = new BufferedReader(
                            new InputStreamReader(httpconn.getInputStream()),
                            8192);
                    String strLine = null;

                    while ((strLine = input.readLine()) != null) {
                        response.append(strLine);
                    }
                    input.close();
                }

                String jsonOutput = response.toString();

                JSONObject jsonObject = new JSONObject(jsonOutput);

                // routesArray contains ALL routes
                JSONArray routesArray = jsonObject.getJSONArray("routes");
                // Grab the first route
                JSONObject route = routesArray.getJSONObject(0);

                JSONObject poly = route.getJSONObject("overview_polyline");
                String polyline = poly.getString("points");
                pontos = decodePoly(polyline);

            } catch (Exception e) {

            }

            return null;

        }

        /**
         * Trace l'itinéraire en bleu entre l'origine et la destination
         */
        protected void onPostExecute(String file_url) {
            for (int i = 0; i < pontos.size() - 1; i++) {
                LatLng src = pontos.get(i);
                LatLng dest = pontos.get(i + 1);
                try {
                    Polyline line = mMap.addPolyline(new PolylineOptions()
                            .add(new LatLng(src.latitude, src.longitude),
                                    new LatLng(dest.latitude, dest.longitude))
                            .width(10).color(Color.BLUE).geodesic(true));
                } catch (NullPointerException e) {
                    Log.e("Error", "NullPointerException onPostExecute: " + e.toString());
                } catch (Exception e2) {
                    Log.e("Error", "Exception onPostExecute: " + e2.toString());
                }

            }
            dialog.dismiss();

        }
    }

    /**
     * Décrypte le JSON pour l'ajouter dans une liste de coordonées
     * @param encoded les points dans un string
     * @return la liste des points avec les coordonnées
     */
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
}


