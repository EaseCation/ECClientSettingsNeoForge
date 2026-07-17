package net.easecation.clientsettings.profile.runtime;

public interface ProfileParticipant {

    void apply(ActiveProfileSnapshot previous, ActiveProfileSnapshot current) throws Exception;

    void resetTransientState() throws Exception;
}
