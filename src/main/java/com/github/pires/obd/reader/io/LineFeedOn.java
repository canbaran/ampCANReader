package com.github.pires.obd.reader.io;

import com.github.pires.obd.commands.protocol.ObdProtocolCommand;

/**
 * Created by canbaran on 12/11/17.
 */

public class LineFeedOn extends ObdProtocolCommand{
    /**
     * <p>Constructor for LineFeedOffCommand.</p>
     */
    public LineFeedOn() {
        super("AT L1");
    }

    /**
     * <p>Constructor for LineFeedOffCommand.</p>
     *
     * @param other a {@link com.github.pires.obd.commands.protocol.LineFeedOffCommand} object.
     */
    public LineFeedOn(LineFeedOn other) {
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
        return "Line Feed On";
    }

}
