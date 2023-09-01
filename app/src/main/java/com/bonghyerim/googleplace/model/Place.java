package com.bonghyerim.googleplace.model;

import android.location.Location;

import java.io.Serializable;

public class Place implements Serializable {

    public String name;
    public String vicinity;

    public Geometry geometry;

    public OpeningHours opening_hours;
    public class Geometry implements Serializable {

        public Location location;

        public class Location implements Serializable {
            public double lat;
            public double lng;
        }
    }

    public  class OpeningHours implements Serializable{

        public boolean open_now;
    }

}