package imerir.licence.ar.model;

import android.location.Location;

/**
 * Created by Erwann on 01/06/17.
 */

/**
 * Classe pour implémenter un point de réalité augmenté
 */
public class ARPoint {
    Location location;
    String name;
    float xScreen;
    float yScreen;
    String url;

    /**
     * Constructeur
     *
     * @param name le nom du point
     * @param lat la latitude du monument
     * @param lon la longitude du monument
     * @param altitude l'altitude du monument
     * @param url l'url de la page web
     */
    public ARPoint(String name, double lat, double lon, double altitude, String url) {
        this.name = name;
        location = new Location("ARPoint");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setAltitude(altitude);
        this.url = url;
    }

    //GETTERS ET SETTERS

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public String geturl() {
        return url;
    }

    public float getxScreen() {
        return xScreen;
    }

    public void setxScreen(float xScreen) {
        this.xScreen = xScreen;
    }

    public float getyScreen() {
        return yScreen;
    }

    public void setyScreen(float yScreen) {
        this.yScreen = yScreen;
    }

}
