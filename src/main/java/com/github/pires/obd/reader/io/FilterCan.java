package com.github.pires.obd.reader.io;

import com.github.pires.obd.commands.protocol.ObdProtocolCommand;

/**
 * Created by canbaran on 12/13/17.
 */

public class FilterCan extends ObdProtocolCommand {
    public String canID;
    public String atCommand;
    public FilterCan(String IncomingAtCommand, String hexVal) {
        super("AT " + IncomingAtCommand + " " + hexVal);
        canID = hexVal;
        atCommand = IncomingAtCommand;
    }
    public FilterCan(FilterCan other) {
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
        return "Filter Can with" + atCommand + " " + canID;
    }

}

