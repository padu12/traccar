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

public class SAFL01ProtocolDecoder extends BaseProtocolDecoder {
    public SAFL01ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("imei:")
            .expression("(\\d{15}),")           // IMEI Number
            .text("\\$PSB01,")                  // Safseer protocol BO1
            .number("(dd)(dd)(dd.dd),")         // Time
            .expression("([AV]),")              // Status
            .number("(-?d+.?d*),")              // Latitude
            .expression("(0),")                 // Not Implemented
            .number("(-?d+.?d*),")              // Longitude
            .expression("(0),")                 // Not Implemented
            .expression("(0),")                 // Not Implemented
            .expression("(0),")                 // Not Implemented
            .expression("(0),")                 // Not Implemented
            .expression("(0),")                 // Not Implemented
            .expression("(0),")                 // Not Implemented
            .expression("(0),")                 // Not Implemented
            .expression("(0),")                 // Not Implemented
            .expression("(0),")                 // Not Implemented
            .expression("(0),")                 // Not Implemented
            .number("(d+),")                    // GSM Signal strength
            .number("(dd)(dd)(dd),")            // Date
            .number("(d+.?d*),")                // AIN 1
            .number("(d+.?d*),")                // AIN 2
            .number("(d),")                     // DIN 1
            .number("(d),")                     // DIN 2
            .number("(d),")                     // DOUT 1
            .number("(d),")                     // DOUT 2
            .expression("(0),")                 // Not Implemented
            .expression("(0),")                 // Not Implemented
            .expression("(0),")                 // Not Implemented
            .expression("(0),")                 // Not Implemented
            .expression("(0),")                 // Not Implemented
            .expression("(0),")                 // Not Implemented
            .number("(d+.?d*),")                // Main Supply Voltage
            .number("(d+),")                    // Li-Battery Voltage
            .number("(d+),")                    // Li-Poly Battery Temperature
            .expression("(.),")                 // Positioning and Check Sum
            .groupBegin()
            .text("[")
            .expression("(.),")                 // Sensor Name
            .expression("(.),")                 // Sensor ID
            .number("(d+.?d*),")                // Temperature
            .number("(d+.?d*),")                // Humidity
            .expression("(0),")                 // Not Implemented
            .expression("(0),")                 // Not Implemented
            .expression("(0),")                 // Not Implemented
            .number("(d+)")                     // Sensor Battery
            .text("],?")
            .groupEnd("{4}")
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
        parser.skip(1);
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN_HEM));
        parser.skip(10);
        position.set("gsmSignalStrength", parser.nextInt());

        dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.set("ain1", parser.nextDouble());
        position.set("ain2", parser.nextDouble());
        position.set("din1", parser.nextInt());
        position.set("din2", parser.nextInt());
        position.set("dout1", parser.nextInt());
        position.set("dout2", parser.nextInt());
        parser.skip(6);
        position.set("mainSupplyVoltage", parser.nextDouble());
        position.set("liBatteryVoltage", parser.nextInt());
        position.set("liPolyBatteryTemperature", parser.nextInt());
        position.set("positioningModeAndCheckSum", parser.next());
        setAllNodes(position, parser);

        return position;
    }

    private void setAllNodes(Position position, Parser parser) {
        for (int i = 1; i <= 4; i++) {
            position.set(i + "_sensorName", parser.next());
            position.set(i + "_sensorId", parser.next());
            position.set(i + "_temperature", parser.nextDouble());
            position.set(i + "_humidity", parser.nextDouble());
            parser.skip(3);
            position.set(i + "_sensorBattery", parser.nextInt());
        }
    }
}
