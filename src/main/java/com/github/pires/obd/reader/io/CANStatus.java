package com.github.pires.obd.reader.io;
import com.github.pires.obd.commands.protocol.ObdProtocolCommand;

/**
 * Created by canbaran on 11/16/17.
 */

public class CANStatus extends ObdProtocolCommand {

    /**
     * <p>Constructor for CANStatus.</p>
     */
    public CANStatus() {
        super("AT CS");
    }

    /**
     * <p>Constructor for EchoOffCommand.</p>
     *
     * @param other a {@link com.github.pires.obd.commands.protocol.EchoOffCommand} object.
     */
    public CANStatus(CANStatus other) {
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
        return "Read CAN STATUS";
    }

}
