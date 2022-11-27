package ie.tcd.mcardleg.CycLQE;

import ie.tcd.mcardleg.CycLQE.datapoints.AccelDP;
import ie.tcd.mcardleg.CycLQE.datapoints.GPSDP;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

public class DataProcessor {

    private DynamoDbClient dbClient;
    private DynamoDbEnhancedClient enhancedClient;
    private NavigableMap<BigDecimal, NavigableMap<BigDecimal, List<Long>>> coordsTimeMap;
    private NavigableMap<Long, Float> timeAccelMap;
    private NavigableMap<BigDecimal, NavigableMap<BigDecimal, List<Float>>> coordsAccelMap;


    public DataProcessor(DBManager DBManager) throws ParseException {
        this.dbClient = DBManager.getDbClient();
        this.enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dbClient).build();

        this.coordsTimeMap = new TreeMap<BigDecimal, NavigableMap<BigDecimal, List<Long>>>();
        this.timeAccelMap = new TreeMap<Long, Float>();
        this.coordsAccelMap = new TreeMap<BigDecimal, NavigableMap<BigDecimal, List<Float>>>();
    }

    public void process() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        initialiseMaps();
        fuseCoordsAndTime(format);
        fuseTimeAndAccel(format);
        putFusedCoordsAndAccelInDB();
    }

    public Iterator<AccelDP> getAccelerometerData() {

        TableSchema<AccelDP> accelerometerTableSchema = TableSchema.builder(AccelDP.class)
                        .newItemSupplier(AccelDP::new)
                        .addAttribute(String.class, a -> a.name(Constants.UUID)
                                .getter(AccelDP::getUuid)
                                .setter(AccelDP::setUuid)
                                .tags(primaryPartitionKey()))
                        .addAttribute(Integer.class, a -> a.name(Constants.USER_ID)
                                .getter(AccelDP::getUserID)
                                .setter(AccelDP::setUserID))
                        .addAttribute(String.class, a -> a.name(Constants.DATE_TIME)
                                .getter(AccelDP::getDateTime)
                                .setter(AccelDP::setDateTime))
                        .addAttribute(Float.class, a -> a.name(Constants.ACCELERATION)
                                .getter(AccelDP::getAcceleration)
                                .setter(AccelDP::setAcceleration))
                        .build();

        return enhancedClient.table(Constants.RAW_ACCELEROMETER_TABLE, accelerometerTableSchema).scan().items().iterator();
    }

    public Iterator<GPSDP> getGpsData() {

        TableSchema<GPSDP> gpsTableSchema = TableSchema.builder(GPSDP.class)
                        .newItemSupplier(GPSDP::new)
                        .addAttribute(String.class, a -> a.name(Constants.UUID)
                                .getter(GPSDP::getUuid)
                                .setter(GPSDP::setUuid)
                                .tags(primaryPartitionKey()))
                        .addAttribute(Integer.class, a -> a.name(Constants.USER_ID)
                                .getter(GPSDP::getUserID)
                                .setter(GPSDP::setUserID))
                        .addAttribute(String.class, a -> a.name(Constants.DATE_TIME)
                                .getter(GPSDP::getDateTime)
                                .setter(GPSDP::setDateTime))
                        .addAttribute(BigDecimal.class, a -> a.name(Constants.LATITUDE)
                                .getter(GPSDP::getLatitude)
                                .setter(GPSDP::setLatitude))
                        .addAttribute(BigDecimal.class, a -> a.name(Constants.LONGITUDE)
                                .getter(GPSDP::getLongitude)
                                .setter(GPSDP::setLongitude))
                        .build();

        return enhancedClient.table(Constants.RAW_GPS_TABLE, gpsTableSchema).scan().items().iterator();
    }

    private void initialiseMaps() {
        BigDecimal lat = Constants.DUBLIN_NORTHWEST_LAT;
        while(lat.compareTo(Constants.DUBLIN_SOUTHEAST_LAT) == 1) {

            NavigableMap<BigDecimal, List<Long>> lonTimeMap = new TreeMap<BigDecimal, List<Long>>();
            NavigableMap<BigDecimal, List<Float>> lonAccelMap = new TreeMap<BigDecimal, List<Float>>();
            BigDecimal lon = Constants.DUBLIN_NORTHWEST_LON;
            while (lon.compareTo(Constants.DUBLIN_SOUTHEAST_LON) == -1) {

                lonTimeMap.put(lon, new ArrayList<Long>());
                lonAccelMap.put(lon, new ArrayList<Float>());
                lon = lon.add(Constants.LON_INTERVAL);
            }

            coordsTimeMap.put(lat, lonTimeMap);
            coordsAccelMap.put(lat, lonAccelMap);
            lat = lat.subtract(Constants.LAT_INTERVAL);
        }
    }

    private void fuseCoordsAndTime(SimpleDateFormat format) throws ParseException {
        Iterator<GPSDP> gpsIterator = getGpsData();
        GPSDP dp;

        Long epoch;
        while(gpsIterator.hasNext()) {
            try {
                dp = gpsIterator.next();
                epoch = format.parse(dp.getDateTime()).getTime();

                BigDecimal roundedLat = coordsTimeMap.floorKey(dp.getLatitude());
                NavigableMap<BigDecimal, List<Long>> lonMap = coordsTimeMap.get(roundedLat);
                BigDecimal roundedLon = lonMap.floorKey(dp.getLongitude());
                List<Long> times = lonMap.get(roundedLon);
                times.add(epoch);
                lonMap.put(roundedLon, times);
                coordsTimeMap.put(roundedLat, lonMap);
                timeAccelMap.put(epoch, null);
            }
            catch (ParseException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }
    }

    private void fuseTimeAndAccel(SimpleDateFormat format) {
        Iterator<AccelDP> accelIterator = getAccelerometerData();
        AccelDP dp;

        Long epoch;
        while(accelIterator.hasNext()) {
            try {
                dp = accelIterator.next();
                epoch = format.parse(dp.getDateTime()).getTime();
                timeAccelMap.put(timeAccelMap.floorKey(epoch), dp.getAcceleration());
            }
            catch (ParseException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }
    }

    private void putFusedCoordsAndAccelInDB() {
        for(BigDecimal lat : coordsTimeMap.keySet()) {

            NavigableMap<BigDecimal, List<Long>> latMap = coordsTimeMap.get(lat);
            for(BigDecimal lon : latMap.keySet()) {

                Float totalAccel = 0f;
                Float count = 0f;
                List<Long> times = latMap.get(lon);
                for(Long time : times) {
                    Float accel = timeAccelMap.get(time);
                    if(accel != null) {
                        totalAccel += accel;
                        count++;
                    }
                }

                Float averageAccel = totalAccel / count;
                if(averageAccel != 0f && !averageAccel.isNaN()) {
                    HashMap<String, AttributeValue> itemValues = new HashMap<String,AttributeValue>();
                    itemValues.put(Constants.UUID, AttributeValue.builder().s(UUID.randomUUID().toString()).build());
                    itemValues.put(Constants.LATITUDE, AttributeValue.builder().n(String.valueOf(lat)).build());
                    itemValues.put(Constants.LONGITUDE, AttributeValue.builder().n(String.valueOf(lon)).build());
                    itemValues.put(Constants.ACCELERATION, AttributeValue.builder().n(String.valueOf(averageAccel)).build());

                    PutItemRequest request = PutItemRequest.builder().tableName(Constants.PROCESSED_DATA_TABLE).item(itemValues).build();
                    try {
                        dbClient.putItem(request);
                    }
                    catch (Exception e) {
                        System.err.println(e.getMessage());
                        System.exit(1);
                    }
                }
            }
        }
    }

}
 