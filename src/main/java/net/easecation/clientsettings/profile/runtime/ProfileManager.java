package net.easecation.clientsettings.profile.runtime;

import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.model.ProfileFeatures;
import net.easecation.clientsettings.profile.model.ProfileIndex;
import net.easecation.clientsettings.profile.store.ProfileCatalog;
import net.easecation.clientsettings.profile.store.ProfileLoadResult;
import net.easecation.clientsettings.profile.store.ProfileStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

public final class ProfileManager {

    private final ProfileStore store;
    private final String availabilityError;
    private final List<ProfileParticipant> participants = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private volatile ActiveProfileSnapshot activeSnapshot;
    private ProfileCatalog catalog;

    private ProfileManager(ProfileStore store, ProfileCatalog catalog, List<String> warnings, String availabilityError) {
        this.store = store;
        this.catalog = catalog;
        this.warnings.addAll(warnings);
        this.availabilityError = availabilityError;
        this.activeSnapshot = ActiveProfileSnapshot.from(catalog.activeProfile());
    }

    public static ProfileManager load(ProfileStore store, ProfileDefinition missingDefault) throws IOException {
        ProfileLoadResult result = store.load(missingDefault);
        return new ProfileManager(store, result.catalog(), result.warnings(), null);
    }

    public static ProfileManager unavailable(ProfileDefinition fallback, String error) {
        ProfileIndex index = ProfileIndex.defaults();
        ProfileCatalog catalog = new ProfileCatalog(index, Map.of(fallback.id(), fallback));
        return new ProfileManager(null, catalog, List.of(error), error);
    }

    public ActiveProfileSnapshot activeSnapshot() {
        return activeSnapshot;
    }

    public synchronized List<ProfileDefinition> profiles() {
        return catalog.profilesInOrder();
    }

    public synchronized List<String> warnings() {
        return List.copyOf(warnings);
    }

    public synchronized boolean available() {
        return availabilityError == null;
    }

    public synchronized void registerParticipant(ProfileParticipant participant) throws IOException {
        Objects.requireNonNull(participant, "participant");
        ActiveProfileSnapshot current = activeSnapshot;
        try {
            participant.resetTransientState();
            participant.apply(current, current);
            participants.add(participant);
        } catch (Exception exception) {
            throw new IOException("Could not initialize Profile participant", exception);
        }
    }

    public synchronized ProfileDefinition create(String name) throws IOException {
        ensureAvailable();
        requireUniqueName(name, null);
        ProfileDefinition profile = ProfileDefinition.create(name, ProfileFeatures.DEFAULT);
        List<String> order = new ArrayList<>(catalog.index().profileOrder());
        order.add(profile.id());
        ProfileIndex updatedIndex = new ProfileIndex(
                ProfileDefinition.CURRENT_SCHEMA_VERSION,
                catalog.index().activeProfileId(),
                order
        );
        store.create(profile, updatedIndex);
        catalog = catalogWith(updatedIndex, added(profile));
        return profile;
    }

    public synchronized ProfileDefinition duplicate(String sourceId, String name) throws IOException {
        ensureAvailable();
        requireUniqueName(name, null);
        ProfileDefinition source = catalog.profile(sourceId);
        ProfileDefinition duplicate = ProfileDefinition.create(name, source.features());
        List<String> order = new ArrayList<>(catalog.index().profileOrder());
        order.add(duplicate.id());
        ProfileIndex updatedIndex = new ProfileIndex(
                ProfileDefinition.CURRENT_SCHEMA_VERSION,
                catalog.index().activeProfileId(),
                order
        );
        store.create(duplicate, updatedIndex);
        catalog = catalogWith(updatedIndex, added(duplicate));
        return duplicate;
    }

    public synchronized ProfileDefinition rename(String profileId, String newName) throws IOException {
        ensureAvailable();
        requireUniqueName(newName, profileId);
        ProfileDefinition previous = catalog.profile(profileId);
        ProfileDefinition renamed = previous.withName(newName);
        store.saveProfile(renamed);
        catalog = catalogWith(catalog.index(), replaced(renamed));
        if (profileId.equals(activeSnapshot.id())) {
            activeSnapshot = ActiveProfileSnapshot.from(renamed);
        }
        return renamed;
    }

    public synchronized ProfileDefinition updateActiveFeatures(UnaryOperator<ProfileFeatures> update) throws IOException {
        ensureAvailable();
        ProfileDefinition previous = catalog.activeProfile();
        ProfileFeatures updatedFeatures = Objects.requireNonNull(update.apply(previous.features()), "updated features");
        ProfileDefinition updated = previous.withFeatures(updatedFeatures);
        if (updated.equals(previous)) {
            return previous;
        }
        store.saveProfile(updated);
        try {
            applyTransition(ActiveProfileSnapshot.from(updated));
        } catch (IOException exception) {
            try {
                store.saveProfile(previous);
            } catch (IOException rollbackFailure) {
                exception.addSuppressed(rollbackFailure);
            }
            throw exception;
        }
        catalog = catalogWith(catalog.index(), replaced(updated));
        return updated;
    }

    public synchronized void switchTo(String profileId) throws IOException {
        ensureAvailable();
        ProfileDefinition target = catalog.profile(profileId);
        if (profileId.equals(activeSnapshot.id())) {
            return;
        }
        ActiveProfileSnapshot previous = activeSnapshot;
        applyTransition(ActiveProfileSnapshot.from(target));
        ProfileIndex updatedIndex = catalog.index().withActiveProfile(profileId);
        try {
            store.saveIndex(updatedIndex);
        } catch (IOException exception) {
            rollbackTransition(previous, exception);
            throw exception;
        }
        catalog = catalogWith(updatedIndex, catalog.profiles());
    }

    public synchronized void delete(String profileId) throws IOException {
        ensureAvailable();
        ProfileDefinition removed = catalog.profile(profileId);
        if (removed.isDefault()) {
            throw new IllegalArgumentException("Default Profile cannot be deleted");
        }

        ProfileIndex previousIndex = catalog.index();
        List<String> order = new ArrayList<>(previousIndex.profileOrder());
        order.remove(profileId);
        boolean deletingActive = profileId.equals(previousIndex.activeProfileId());
        ProfileIndex updatedIndex = new ProfileIndex(
                ProfileDefinition.CURRENT_SCHEMA_VERSION,
                deletingActive ? ProfileDefinition.DEFAULT_ID : previousIndex.activeProfileId(),
                order
        );

        ActiveProfileSnapshot previousSnapshot = activeSnapshot;
        if (deletingActive) {
            applyTransition(ActiveProfileSnapshot.from(catalog.profile(ProfileDefinition.DEFAULT_ID)));
        }
        try {
            store.delete(removed, previousIndex, updatedIndex);
        } catch (IOException exception) {
            if (deletingActive) {
                rollbackTransition(previousSnapshot, exception);
            }
            throw exception;
        }
        Map<String, ProfileDefinition> remaining = new LinkedHashMap<>(catalog.profiles());
        remaining.remove(profileId);
        catalog = catalogWith(updatedIndex, remaining);
    }

    private void applyTransition(ActiveProfileSnapshot target) throws IOException {
        ActiveProfileSnapshot previous = activeSnapshot;
        try {
            resetParticipants();
            for (ProfileParticipant participant : participants) {
                participant.apply(previous, target);
            }
            activeSnapshot = target;
        } catch (Exception exception) {
            IOException failure = new IOException("Could not apply Profile " + target.id(), exception);
            rollbackTransition(previous, failure);
            throw failure;
        }
    }

    private void rollbackTransition(ActiveProfileSnapshot target, Exception primaryFailure) {
        ActiveProfileSnapshot failed = activeSnapshot;
        try {
            resetParticipants();
            for (ProfileParticipant participant : participants) {
                participant.apply(failed, target);
            }
        } catch (Exception rollbackFailure) {
            primaryFailure.addSuppressed(rollbackFailure);
        } finally {
            activeSnapshot = target;
        }
    }

    private void resetParticipants() throws Exception {
        for (ProfileParticipant participant : participants) {
            participant.resetTransientState();
        }
    }

    private void ensureAvailable() throws IOException {
        if (availabilityError != null) {
            throw new IOException("Profile storage is unavailable: " + availabilityError);
        }
    }

    private void requireUniqueName(String name, String ignoredProfileId) {
        String normalized = ProfileDefinition.normalizeName(name).toLowerCase(Locale.ROOT);
        boolean duplicate = catalog.profiles().values().stream()
                .filter(profile -> !profile.id().equals(ignoredProfileId))
                .anyMatch(profile -> profile.name().toLowerCase(Locale.ROOT).equals(normalized));
        if (duplicate) {
            throw new IllegalArgumentException("Profile name already exists: " + name);
        }
    }

    private Map<String, ProfileDefinition> added(ProfileDefinition profile) {
        Map<String, ProfileDefinition> updated = new LinkedHashMap<>(catalog.profiles());
        updated.put(profile.id(), profile);
        return updated;
    }

    private Map<String, ProfileDefinition> replaced(ProfileDefinition profile) {
        Map<String, ProfileDefinition> updated = new LinkedHashMap<>(catalog.profiles());
        updated.put(profile.id(), profile);
        return updated;
    }

    private static ProfileCatalog catalogWith(ProfileIndex index, Map<String, ProfileDefinition> profiles) {
        return new ProfileCatalog(index, profiles);
    }
}
