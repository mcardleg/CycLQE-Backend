package ie.tcd.mcardleg.CycLQE.datapoints;


public class AccelDP {
    private String uuid;
    private String dateTime;
    private Float acceleration;

    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDateTime() {
        return dateTime;
    }
    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public Float getAcceleration() {
        return acceleration;
    }
    public void setAcceleration(Float acceleration) {
        this.acceleration = acceleration;
    }

}
