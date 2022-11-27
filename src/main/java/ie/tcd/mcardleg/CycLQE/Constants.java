package ie.tcd.mcardleg.CycLQE;

import java.math.BigDecimal;

public class Constants {
    public static final String RAW_ACCELEROMETER_TABLE = "CycLQE_Raw_Accelerometer_Data_Table";
    public static final String RAW_GPS_TABLE = "CycLQE_Raw_GPS_Data_Table";
    public static final String PROCESSED_DATA_TABLE = "CycLQE_Processed_Data_Table";

    public static final String UUID = "uuid";
    public static final String USER_ID = "userId";
    public static final String TTL = "expiration";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String ACCELERATION = "acceleration";
    public static final String DATE_TIME = "dateTime";

    public static final String AWS_ACCESS_KEY = "";
    public static final String AWS_SECRET_ACCESS_KEY = "";

    public static final BigDecimal LAT_INTERVAL = new BigDecimal("0.0005");
    public static final BigDecimal LON_INTERVAL = new BigDecimal("0.001");
    public static final BigDecimal DUBLIN_NORTHWEST_LAT = new BigDecimal("53.3494");
    public static final BigDecimal DUBLIN_NORTHWEST_LON = new BigDecimal("-6.32");
    public static final BigDecimal DUBLIN_SOUTHEAST_LAT = new BigDecimal("53.3414");
    public static final BigDecimal DUBLIN_SOUTHEAST_LON = new BigDecimal("-6.25");

}
