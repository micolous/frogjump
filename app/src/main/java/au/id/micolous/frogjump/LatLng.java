package au.id.micolous.frogjump;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by michael on 28/11/15.
 */
public class LatLng implements Parcelable, Parcelable.Creator<LatLng> {
    protected static final Pattern LL_PATTERN = Pattern.compile("([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)");

    private double latitude;
    private double longitude;

    public LatLng(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Parses a String with location specified as latitude,longitude in decimal degrees
     * @param input Input string
     * @return LatLng representing the location, or null if it was not parsaeble.
     */
    public static LatLng parseFromString(String input) {
        Matcher llMatcher = LL_PATTERN.matcher(input);
        try {
            if (llMatcher.find()) {
                double lat = Double.valueOf(llMatcher.group(1));
                double lng = Double.valueOf(llMatcher.group(2));

                return new LatLng(lat, lng);
            }
        } catch (NumberFormatException ex) {
            // Formatting error.
            return null;
        }

        return null;
    }

    /**
     * Parses a String array with location specified in the first two elements of the array, as
     * decimal degrees, of latitude followed by longitude.
     * @param input Input string array.
     * @return LatLng representing the location, or null if it was not parseable.
     */
    public static LatLng parseFromStringArray(String[] input) {
        if (input == null || input.length < 2) {
            return null;
        }

        try {
            double lat = Double.valueOf(input[0]);
            double lng = Double.valueOf(input[1]);
            return new LatLng(lat, lng);
        } catch (NumberFormatException ex) {
            // Formatting error.
            return null;
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeDouble(latitude);
        parcel.writeDouble(longitude);
    }

    @Override
    public LatLng createFromParcel(Parcel parcel) {
        double latitude = parcel.readDouble();
        double longitude = parcel.readDouble();
        return new LatLng(latitude, longitude);
    }

    @Override
    public LatLng[] newArray(int size) {
        return new LatLng[size];
    }
}
