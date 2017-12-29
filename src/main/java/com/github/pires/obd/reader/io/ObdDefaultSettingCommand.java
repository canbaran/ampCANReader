package com.github.pires.obd.reader.io;

import com.github.pires.obd.commands.protocol.ObdProtocolCommand;

/**
 * Created by canbaran on 11/17/17.
 */

public class ObdDefaultSettingCommand  extends ObdProtocolCommand {
    /**
     * <p>Constructor for CANStatus.</p>
     */
    public ObdDefaultSettingCommand() {
        super("AT D");
    }

    /**
     * <p>Constructor for EchoOffCommand.</p>
     *
     * @param other a {@link com.github.pires.obd.commands.protocol.EchoOffCommand} object.
     */
    public ObdDefaultSettingCommand(ObdDefaultSettingCommand other) {
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
        return "Default Settings";
    }
}
