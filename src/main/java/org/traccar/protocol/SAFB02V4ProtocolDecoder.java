package org.traccar.protocol;

import io.netty.channel.Channel;
import java.net.SocketAddress;
import java.util.regex.Pattern;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

public class SAFB02V4ProtocolDecoder extends BaseProtocolDecoder {
    public SAFB02V4ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("imei:")
            .expression("(\\d{15}),")           // IMEI Number
            .text("\\$PSB01,")                  // Safseer protocol BO1
            .number("(dd)(dd)(dd.dd),")         // Time
            .expression("([AV]),")              // Status
            .number("(-?d+.?d*),")              // Latitude
            .expression("([NS]),")              // NORTH/SOUTH
            .number("(-?d+.?d*),")              // Longitude
            .expression("([EW]),")              // EAST/WEST
            .number("(-?d+.?d*),")              // Altitude
            .number("(d+.?d*),")                // Speed
            .number("(d+.?d*),")                // Course
            .number("(d+.?d*),")                // HDOP
            .number("(d+.?d*),")                // PDOP
            .number("(d+.?d*),")                // VDOP
            .number("(d+),")                    // GNSS in View
            .number("(d+),")                    // GPS used
            .number("(d+),")                    // GLONASS used
            .number("(d+),")                    // GSM Signal strength
            .number("(dd)(dd)(dd),")            // Date
            .number("(d+.?d*),")                // AIN 1
            .number("(d+.?d*),")                // AIN 2
            .number("(d),")                     // DIN 1
            .number("(d),")                     // DIN 2
            .number("(d),")                     // DOUT 1
            .number("(d),")                     // DOUT 2
            .number("(d+),")                    // Accelerometer X
            .number("(d+),")                    // Accelerometer Y
            .number("(d+),")                    // Accelerometer Z
            .number("(d+),")                    // Gyro X
            .number("(d+),")                    // Gyro Y
            .number("(d+),")                    // Gyro Z
            .number("(d+.?d*),")                // Car Battery Voltage
            .number("(d+),")                    // Li-Battery Voltage
            .number("(d+),")                    // Li-Poly Battery Temperature
            .number("(d+),")                    // Li Battery Health
            .number("(d+),")                    // Li Battery Charging
            .expression("(.),")                 // Positioning and Check Sum
            .number("(d+),")                    // Number of DBS nodes
            .number("(d+),")                    // Number of CBS nodes
            .number("(d+)")                     // Total number of nodes
            .groupBegin()
            .expression(",(DS\\d+),")           // Message ID
            .expression("(.),")                 // Sensor Name
            .expression("(.),")                 // Sensor MAC
            .number("(d+.?d*),")                // Temperature
            .number("(d+.?d*),")                // Humidity
            .number("(d+),")                    // Move Counter
            .number("(d+),")                    // Angle
            .number("(d+),")                    // Fuel Level
            .number("(d+),")                    // Luminosity
            .number("(d+)")                     // Battery Voltage
            .groupEnd("*")
            .groupBegin()
            .expression(",(CS\\d+),")           // Message ID
            .expression("(.),")                 // Sensor Name
            .expression("(.),")                 // Sensor MAC
            .number("(d+),")                    // Field 1
            .number("(d+),")                    // Field 2
            .number("(d+),")                    // Field 3
            .number("(d+),")                    // Field 4
            .number("(d+),")                    // Field 5
            .number("(d+),")                    // Field 6
            .number("(d+),")                    // Battery Voltage
            .groupEnd("*")
            .any()
            .compile();

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        String imei = parser.next();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(),parser.nextInt(),parser.nextInt());

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN_HEM));
        position.set("north/south", parser.next());
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN_HEM));
        position.set("east/west", parser.next());
        position.setAltitude(parser.nextDouble(0));
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));
        position.set(Position.KEY_HDOP, parser.nextDouble());
        position.set(Position.KEY_PDOP, parser.nextDouble());
        position.set(Position.KEY_VDOP, parser.nextDouble());
        position.set("gnss", parser.nextInt());
        position.set(Position.KEY_GPS, parser.nextInt());
        position.set("glonass", parser.nextInt());
        position.set("gsmSignalStrength", parser.nextInt());

        dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.set("ain1", parser.nextDouble());
        position.set("ain2", parser.nextDouble());
        position.set("din1", parser.nextInt());
        position.set("din2", parser.nextInt());
        position.set("dout1", parser.nextInt());
        position.set("dout2", parser.nextInt());
        position.set("accelerometerX", parser.nextInt());
        position.set("accelerometerY", parser.nextInt());
        position.set("accelerometerZ", parser.nextInt());
        position.set("gyroX", parser.nextInt());
        position.set("gyroY", parser.nextInt());
        position.set("gyroZ", parser.nextInt());
        position.set("carBatteryVoltage", parser.nextDouble());
        position.set("liBatteryVoltage", parser.nextInt());
        position.set("liPolyBatteryTemperature", parser.nextInt());
        position.set("liBatteryHealth", parser.nextInt());
        position.set("liBatteryCharging", parser.nextInt());
        position.set("positioningModeAndCheckSum", parser.next());

        int DbsNodesNumber = parser.nextInt();
        int CbsNodesNumber = parser.nextInt();
        parser.skip(1);

        for (int i = 1; i <= DbsNodesNumber; i++) {
            position.set("dbs" + i + "_messageId", parser.next());
            position.set("dbs" + i + "_sensorName", parser.next());
            position.set("dbs" + i + "_sensorMAC", parser.next());
            position.set("dbs" + i + "_temperature", parser.nextDouble());
            position.set("dbs" + i + "_humidity", parser.nextDouble());
            position.set("dbs" + i + "_moveCounter", parser.nextInt());
            position.set("dbs" + i + "_angle", parser.nextInt());
            position.set("dbs" + i + "_fuelLevel", parser.nextInt());
            position.set("dbs" + i + "_luminosity", parser.nextInt());
            position.set("dbs" + i + "_batteryVoltage", parser.nextInt());
        }


        for (int i = 1; i <= CbsNodesNumber; i++) {
            position.set("cbs" + i + "_messageId", parser.next());
            position.set("cbs" + i + "_sensorName", parser.next());
            position.set("dbs" + i + "_sensorMAC", parser.next());
            position.set("cbs" + i + "_field1", parser.nextInt());
            position.set("cbs" + i + "_field2", parser.nextInt());
            position.set("cbs" + i + "_field3", parser.nextInt());
            position.set("cbs" + i + "_field4", parser.nextInt());
            position.set("cbs" + i + "_field5", parser.nextInt());
            position.set("cbs" + i + "_field6", parser.nextInt());
            position.set("cbs" + i + "_batteryVoltage", parser.nextInt());
        }

        return position;
    }
}
