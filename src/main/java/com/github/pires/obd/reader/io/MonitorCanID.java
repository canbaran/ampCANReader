package com.github.pires.obd.reader.io;

import com.github.pires.obd.commands.protocol.ObdProtocolCommand;

/**
 * Created by canbaran on 12/6/17.
 */

public class MonitorCanID extends ObdProtocolCommand {    /**
 * <p>Constructor for CANStatus.</p>
 */
    public MonitorCanID() {
    super("AT CRA 0b0"); //burgundy jag 179 ford 0b0
}

    /**
     * <p>Constructor for EchoOffCommand.</p>
     *
     * @param other a {@link com.github.pires.obd.commands.protocol.EchoOffCommand} object.
     */
    public MonitorCanID(MonitorAllCommand other) {
        super(other);
    }

    /** {@inheritDoc} */
    @Override
    public String getFormattedResult() {
        return getResult();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "Monitoring CAN ID XX";
    }
}
