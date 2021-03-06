package org.jbei.wors.lib.dto.sample;

import org.jbei.wors.lib.dto.IDataTransferModel;

/**
 * Available forms that the samples can be requested in
 *
 * @author Hector Plahar
 */
public enum SampleRequestType implements IDataTransferModel {

    LIQUID_CULTURE("Liquid Culture"),

    STREAK_ON_AGAR_PLATE("Streak on Agar plate");

    private String display;

    SampleRequestType(String display) {
        this.display = display;
    }

    @Override
    public String toString() {
        return this.display;
    }
}
