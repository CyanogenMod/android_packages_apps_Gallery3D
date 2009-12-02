package com.cooliris.media;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Process;

public final class ReverseGeocoder extends Thread {
    private static final int MAX_COUNTRY_NAME_LENGTH = 8;
    // If two points are within 50 miles of each other, use "Around Palo Alto, CA" or "Around Mountain View, CA".
    // instead of directly jumping to the next level and saying "California, US".
    private static final int MAX_LOCALITY_MILE_RANGE = 50;
    private static final Deque<MediaSet> sQueue = new Deque<MediaSet>();
    private static final DiskCache sGeoCache = new DiskCache("geocoder-cache");
    private static final String TAG = "ReverseGeocoder";

    private Geocoder mGeocoder;
    private final Context mContext;

    public ReverseGeocoder(Context context) {
        super(TAG);
        mContext = context;
        start();
    }

    public void enqueue(MediaSet set) {
        Deque<MediaSet> inQueue = sQueue;
        synchronized (inQueue) {
            inQueue.addLast(set);
            inQueue.notify();
        }
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        Deque<MediaSet> queue = sQueue;
        mGeocoder = new Geocoder(mContext);
        queue.clear();
        try {
            for (;;) {
                // Wait for the next request.
                MediaSet set;
                synchronized (queue) {
                    while ((set = queue.pollFirst()) == null) {
                        queue.wait();
                    }
                }
                // Process the request.
                process(set);
            }
        } catch (InterruptedException e) {
            // Terminate the thread.
        }
    }

    public void flushCache() {
        sGeoCache.flush();
    }

    public void shutdown() {
        flushCache();
        this.interrupt();
    }

    private boolean process(final MediaSet set) {
        if (!set.mLatLongDetermined) {
            // No latitude, longitude information available.
            set.mReverseGeocodedLocationComputed = true;
            return false;
        }
        set.mReverseGeocodedLocation = computeMostGranularCommonLocation(set);
        set.mReverseGeocodedLocationComputed = true;
        return true;
    }

    protected String computeMostGranularCommonLocation(final MediaSet set) {
        // The overall min and max latitudes and longitudes of the set.
        double setMinLatitude = set.mMinLatLatitude;
        double setMinLongitude = set.mMinLonLongitude;
        double setMaxLatitude = set.mMaxLatLatitude;
        double setMaxLongitude = set.mMaxLonLongitude;
        Address addr1 = lookupAddress(setMinLatitude, setMinLongitude);
        Address addr2 = lookupAddress(setMaxLatitude, setMaxLongitude);
        if (addr1 == null || addr2 == null) {
            return null;
        }

        // Look at the first line of the address.
        String closestCommonLocation = valueIfEqual(addr1.getAddressLine(0), addr2.getAddressLine(0));
        if (closestCommonLocation != null && !("null".equals(closestCommonLocation))) {
            return closestCommonLocation;
        }

        // Compare thoroughfare (street address) next.
        closestCommonLocation = valueIfEqual(addr1.getThoroughfare(), addr2.getThoroughfare());
        if (closestCommonLocation != null && !("null".equals(closestCommonLocation))) {
            return closestCommonLocation;
        }

        // Feature names can sometimes be useful like "Golden Gate Bridge" but can also be
        // degenerate for street address (like just the house number).
        closestCommonLocation = valueIfEqual(addr1.getFeatureName(), addr2.getFeatureName());
        if (closestCommonLocation != null && !("null".equals(closestCommonLocation))) {
            try {
                Integer.parseInt(closestCommonLocation);
                closestCommonLocation = null;
            } catch (final NumberFormatException nfe) {
                // The feature name is not an integer, allow and continue.
                return closestCommonLocation;
            }
        }

        // Compare the locality.
        closestCommonLocation = valueIfEqual(addr1.getLocality(), addr2.getLocality());
        if (closestCommonLocation != null && !("null".equals(closestCommonLocation))) {
            String adminArea = addr1.getAdminArea();
            if (adminArea != null && adminArea.length() > 0) {
                closestCommonLocation += ", " + adminArea;
            }
            return closestCommonLocation;
        }

        // Just choose one of the localities if within a 50 mile radius.
        int distance = (int) LocationMediaFilter.toMile(LocationMediaFilter.distanceBetween(setMinLatitude, setMinLongitude,
                setMaxLatitude, setMaxLongitude));
        if (distance < MAX_LOCALITY_MILE_RANGE) {
            // Try each of the points and just return the first one to have a valid address.
            Address minLatAddress = lookupAddress(setMinLatitude, set.mMinLatLongitude);
            closestCommonLocation = getLocalityAdminForAddress(minLatAddress, true);
            if (closestCommonLocation != null) {
                return closestCommonLocation;
            }
            Address minLonAddress = lookupAddress(set.mMinLonLatitude, setMinLongitude);
            closestCommonLocation = getLocalityAdminForAddress(minLonAddress, true);
            if (closestCommonLocation != null) {
                return closestCommonLocation;
            }
            Address maxLatAddress = lookupAddress(setMaxLatitude, set.mMaxLatLongitude);
            closestCommonLocation = getLocalityAdminForAddress(maxLatAddress, true);
            if (closestCommonLocation != null) {
                return closestCommonLocation;
            }
            Address maxLonAddress = lookupAddress(set.mMaxLonLatitude, setMaxLongitude);
            closestCommonLocation = getLocalityAdminForAddress(maxLonAddress, true);
            if (closestCommonLocation != null) {
                return closestCommonLocation;
            }
        }

        // Check the administrative area.
        closestCommonLocation = valueIfEqual(addr1.getAdminArea(), addr2.getAdminArea());
        if (closestCommonLocation != null && !("null".equals(closestCommonLocation))) {
            String countryCode = addr1.getCountryCode();
            if (countryCode != null && countryCode.length() > 0) {
                closestCommonLocation += ", " + countryCode;
            }
            return closestCommonLocation;
        }

        // Check the country codes.
        closestCommonLocation = valueIfEqual(addr1.getCountryCode(), addr2.getCountryCode());
        if (closestCommonLocation != null && !("null".equals(closestCommonLocation))) {
            return closestCommonLocation;
        }
        // There is no intersection, let's choose a nicer name.
        String addr1Country = addr1.getCountryName();
        String addr2Country = addr2.getCountryName();
        if (addr1Country.length() > MAX_COUNTRY_NAME_LENGTH || addr2Country.length() > MAX_COUNTRY_NAME_LENGTH) {
            closestCommonLocation = addr1.getCountryCode() + " - " + addr2.getCountryCode();
        } else {
            closestCommonLocation = addr1Country + " - " + addr2Country;
        }
        return closestCommonLocation;
    }

    protected String getReverseGeocodedLocation(final double latitude, final double longitude, final int desiredNumDetails) {
        String location = null;
        int numDetails = 0;
        Address addr = lookupAddress(latitude, longitude);

        if (addr != null) {
            // Look at the first line of the address, thorough fare and feature name in order and pick one.
            location = addr.getAddressLine(0);
            if (location != null && !("null".equals(location))) {
                numDetails++;
            } else {
                location = addr.getThoroughfare();
                if (location != null && !("null".equals(location))) {
                    numDetails++;
                } else {
                    location = addr.getFeatureName();
                    if (location != null && !("null".equals(location))) {
                        numDetails++;
                    }
                }
            }

            if (numDetails == desiredNumDetails) {
                return location;
            }

            String locality = addr.getLocality();
            if (locality != null && !("null".equals(locality))) {
                if (location != null && location.length() > 0) {
                    location += ", " + locality;
                } else {
                    location = locality;
                }
                numDetails++;
            }

            if (numDetails == desiredNumDetails) {
                return location;
            }

            String adminArea = addr.getAdminArea();
            if (adminArea != null && !("null".equals(adminArea))) {
                if (location != null && location.length() > 0) {
                    location += ", " + adminArea;
                } else {
                    location = adminArea;
                }
                numDetails++;
            }

            if (numDetails == desiredNumDetails) {
                return location;
            }

            String countryCode = addr.getCountryCode();
            if (countryCode != null && !("null".equals(countryCode))) {
                if (location != null && location.length() > 0) {
                    location += ", " + countryCode;
                } else {
                    location = addr.getCountryName();
                }
            }
        }

        return location;
    }

    private String getLocalityAdminForAddress(final Address addr, final boolean approxLocation) {
        if (addr == null)
            return "";
        String localityAdminStr = addr.getLocality();
        if (localityAdminStr != null && !("null".equals(localityAdminStr))) {
            if (approxLocation) {
                // TODO: Uncomment these lines as soon as we may translations for R.string.around.
                // localityAdminStr = mContext.getResources().getString(R.string.around) + " " + localityAdminStr;
            }
            String adminArea = addr.getAdminArea();
            if (adminArea != null && adminArea.length() > 0) {
                localityAdminStr += ", " + adminArea;
            }
            return localityAdminStr;
        }
        return null;
    }

    private Address lookupAddress(final double latitude, final double longitude) {
        try {
            long locationKey = (long) (((latitude + LocationMediaFilter.LAT_MAX) * 2 * LocationMediaFilter.LAT_MAX + (longitude + LocationMediaFilter.LON_MAX)) * LocationMediaFilter.EARTH_RADIUS_METERS);
            byte[] cachedLocation = sGeoCache.get(locationKey, 0);
            Address address = null;
            if (cachedLocation == null || cachedLocation.length == 0) {
                List<Address> addresses = mGeocoder.getFromLocation(latitude, longitude, 1);
                if (!addresses.isEmpty()) {
                    address = addresses.get(0);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(bos, 256));
                    Locale locale = address.getLocale();
                    Utils.writeUTF(dos, locale.getLanguage());
                    Utils.writeUTF(dos, locale.getCountry());
                    Utils.writeUTF(dos, locale.getVariant());

                    Utils.writeUTF(dos, address.getThoroughfare());
                    int numAddressLines = address.getMaxAddressLineIndex();
                    dos.writeInt(numAddressLines);
                    for (int i = 0; i < numAddressLines; ++i) {
                        Utils.writeUTF(dos, address.getAddressLine(i));
                    }
                    Utils.writeUTF(dos, address.getFeatureName());
                    Utils.writeUTF(dos, address.getLocality());
                    Utils.writeUTF(dos, address.getAdminArea());
                    Utils.writeUTF(dos, address.getSubAdminArea());

                    Utils.writeUTF(dos, address.getCountryName());
                    Utils.writeUTF(dos, address.getCountryCode());
                    Utils.writeUTF(dos, address.getPostalCode());
                    Utils.writeUTF(dos, address.getPhone());
                    Utils.writeUTF(dos, address.getUrl());

                    dos.flush();
                    sGeoCache.put(locationKey, bos.toByteArray());
                    dos.close();
                }
            } else {
                // Parsing the address from the byte stream.
                DataInputStream dis = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(cachedLocation), 256));
                String language = Utils.readUTF(dis);
                String country = Utils.readUTF(dis);
                String variant = Utils.readUTF(dis);
                Locale locale = null;
                if (language != null) {
                    if (country == null) {
                        locale = new Locale(language);
                    } else if (variant == null) {
                        locale = new Locale(language, country);
                    } else {
                        locale = new Locale(language, country, variant);
                    }
                }
                if (!locale.getLanguage().equals(Locale.getDefault().getLanguage())) {
                    sGeoCache.delete(locationKey);
                    dis.close();
                    return lookupAddress(latitude, longitude);
                }
                address = new Address(locale);

                address.setThoroughfare(Utils.readUTF(dis));
                int numAddressLines = dis.readInt();
                for (int i = 0; i < numAddressLines; ++i) {
                    address.setAddressLine(i, Utils.readUTF(dis));
                }
                address.setFeatureName(Utils.readUTF(dis));
                address.setLocality(Utils.readUTF(dis));
                address.setAdminArea(Utils.readUTF(dis));
                address.setSubAdminArea(Utils.readUTF(dis));

                address.setCountryName(Utils.readUTF(dis));
                address.setCountryCode(Utils.readUTF(dis));
                address.setPostalCode(Utils.readUTF(dis));
                address.setPhone(Utils.readUTF(dis));
                address.setUrl(Utils.readUTF(dis));
                dis.close();
            }
            return address;

        } catch (IOException e) {
            // Ignore.
        }
        return null;
    }

    private String valueIfEqual(String a, String b) {
        return (a != null && b != null && a.equalsIgnoreCase(b)) ? a : null;
    }
}
