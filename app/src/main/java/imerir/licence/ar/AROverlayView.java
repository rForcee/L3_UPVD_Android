package imerir.licence.ar;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.opengl.Matrix;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import imerir.licence.ar.helper.LocationHelper;
import imerir.licence.ar.model.ARPoint;

/**
 * Created by Erwann on 01/06/17.
 */

/**
 * L'overlay avec les points AR
 */
public class AROverlayView extends View {

    Context context;
    private float[] rotatedProjectionMatrix = new float[16];
    private Location currentLocation;
    private List<ARPoint> arPoints;

    /**
     * Création des points et déclaration du contexte
     * @param context le contexte
     */
    public AROverlayView(Context context) {
        super(context);

        this.context = context;

        //Demo points
        arPoints = new ArrayList<ARPoint>() {{
            add(new ARPoint("Basilique-Cathédrale de Saint Jean Baptiste", 42.700566, 2.896977, 50, "https://fr.wikipedia.org/wiki/Cath%C3%A9drale_Saint-Jean-Baptiste_de_Perpignan"));
            add(new ARPoint("Castillet", 42.701134, 2.893961, 29, "https://fr.wikipedia.org/wiki/Castillet"));
            add(new ARPoint("Palais des Rois de Majorque", 42.6939902, 2.8961102, 44, "https://fr.wikipedia.org/wiki/Palais_des_Rois_de_Majorque"));
            add(new ARPoint("Théâtre", 42.701269, 2.888696, 35, "http://www.theatredelarchipel.org/fr-agenda.html"));
            add(new ARPoint("FabLab", 42.688966, 2.850283, 31, "http://www.squaregolab.com/Home/ComingSoon"));
            add(new ARPoint("École internationale de pâtisserie Olivier Bajar", 42.663962, 2.903424, 24, "https://www.olivier-bajard.com/lecole-internationale-de-patisserie/"));
        }};
    }



    /**
     * Mise à jour de la matrix de projection
     */
    public void updateRotatedProjectionMatrix(float[] rotatedProjectionMatrix) {
        this.rotatedProjectionMatrix = rotatedProjectionMatrix;
        this.invalidate();
    }

    /**
     * Mise à jour de la localisation
     */
    public void updateCurrentLocation(Location currentLocation){
        this.currentLocation = currentLocation;
        this.invalidate();
    }

    /**
     * Permet d'afficher les points sur l'écran en fonction de leur position
     * Change la taille suivant la distance
     * @param canvas le canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (currentLocation == null) {
            return;
        }

        final int radius = 50;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));


        for (int i = 0; i < arPoints.size(); i ++) {
            float[] currentLocationInECEF = LocationHelper.WSG84toECEF(currentLocation);
            float[] pointInECEF = LocationHelper.WSG84toECEF(arPoints.get(i).getLocation());
            float[] pointInENU = LocationHelper.ECEFtoENU(currentLocation, currentLocationInECEF, pointInECEF);

            float[] cameraCoordinateVector = new float[4];
            Matrix.multiplyMV(cameraCoordinateVector, 0, rotatedProjectionMatrix, 0, pointInENU, 0);

            if (cameraCoordinateVector[2] < 0) {
                float x  = (0.5f + cameraCoordinateVector[0]/cameraCoordinateVector[3]) * canvas.getWidth();
                float y = (0.5f - cameraCoordinateVector[1]/cameraCoordinateVector[3]) * canvas.getHeight();

                arPoints.get(i).setxScreen(x);
                arPoints.get(i).setyScreen(y);

                int distance =  (int) currentLocation.distanceTo(arPoints.get(i).getLocation());

                paint.setTextSize(60 - distance / 100);
                canvas.drawCircle(x, y, radius - distance / 100, paint);
                canvas.drawText(arPoints.get(i).getName(), x - ((30 - distance / 200) * arPoints.get(i).getName().length() / 2), y - 80, paint);
                canvas.drawText(distance + "m", x - ((30 - distance / 200) * 5 / 2), y - 40, paint);
            }
        }
    }

    /**
     * Si on touche un point
     * Déclenche l'activité de map et passe en paramètre l'origine et la destination
     * @param event l'événement
     * @return true si tout s'est bien passé
     */
    @Override
    public boolean onTouchEvent (MotionEvent event) {

        float xPos, yPos;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN :
                xPos = event.getX();
                yPos = event.getY();

                for (int i = 0; i < arPoints.size(); i ++)
                {

                    if((arPoints.get(i).getxScreen() < xPos + 50 && arPoints.get(i).getxScreen() > xPos - 50) &&
                            (arPoints.get(i).getyScreen() < yPos + 50 && arPoints.get(i).getyScreen() > yPos - 50))
                    {
                        Intent intent = new Intent(context, ARMaps.class);
                        intent.putExtra("originLocation", currentLocation);
                        intent.putExtra("destinationLocation", arPoints.get(i).getLocation());
                        intent.putExtra("destinationName", arPoints.get(i).getName());
                        intent.putExtra("destinationUrl", arPoints.get(i).geturl());
                        context.startActivity(intent);
                    }
                }

                invalidate(); // add it here
                break;
        }

        return true;

    }
}
