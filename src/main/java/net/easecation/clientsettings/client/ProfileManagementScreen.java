package net.easecation.clientsettings.client;

import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class ProfileManagementScreen extends Screen {

    private final Screen settingsParent;
    private final ProfileManagementController controller;
    private ProfileList profileList;
    private String selectedProfileId;
    private Component error;
    private Button selectButton;
    private Button renameButton;
    private Button duplicateButton;
    private Button deleteButton;

    public ProfileManagementScreen(Screen settingsParent) {
        super(Component.translatable("screen.ecclientsettings.profiles.title"));
        this.settingsParent = settingsParent;
        this.controller = new ProfileManagementController(ProfileServices.manager());
        this.selectedProfileId = controller.activeProfileId();
    }

    @Override
    protected void init() {
        this.profileList = this.addRenderableWidget(new ProfileList(
                this.minecraft,
                this.width,
                Math.max(40, this.height - 104),
                30,
                24
        ));
        reloadList();

        int totalWidth = Math.min(360, this.width - 20);
        int gap = 4;
        int quarter = (totalWidth - gap * 3) / 4;
        int left = (this.width - totalWidth) / 2;
        int firstRowY = this.height - 64;
        this.selectButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("button.ecclientsettings.profile.select"),
                        button -> selectProfile()
                ).bounds(left, firstRowY, quarter, 20).build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("button.ecclientsettings.profile.create"),
                        button -> openCreate()
                ).bounds(left + quarter + gap, firstRowY, quarter, 20).build());
        this.duplicateButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("button.ecclientsettings.profile.duplicate"),
                        button -> openDuplicate()
                ).bounds(left + (quarter + gap) * 2, firstRowY, quarter, 20).build());
        this.renameButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("button.ecclientsettings.profile.rename"),
                        button -> openRename()
                ).bounds(left + (quarter + gap) * 3, firstRowY, quarter, 20).build());

        int half = (totalWidth - gap) / 2;
        int secondRowY = this.height - 38;
        this.deleteButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("button.ecclientsettings.profile.delete"),
                        button -> confirmDelete()
                ).bounds(left, secondRowY, half, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> returnToSettings())
                .bounds(left + half + gap, secondRowY, half, 20)
                .build());
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
        if (error != null) {
            graphics.drawCenteredString(this.font, error, this.width / 2, this.height - 78, 0xFFFF5555);
        }
    }

    @Override
    public void onClose() {
        returnToSettings();
    }

    private void reloadList() {
        profileList.replaceProfiles(controller.profiles());
        profileList.select(selectedProfileId);
        updateButtons();
    }

    private void selectProfile() {
        if (selectedProfileId == null) {
            return;
        }
        try {
            controller.select(selectedProfileId);
            returnToSettings();
        } catch (IOException exception) {
            showFailure("select", exception);
        }
    }

    private void openCreate() {
        this.minecraft.setScreen(new ProfileNameScreen(
                this,
                Component.translatable("screen.ecclientsettings.profiles.create"),
                "",
                name -> runNameOperation(() -> {
                    ProfileDefinition created = controller.create(name);
                    selectedProfileId = created.id();
                })
        ));
    }

    private void openDuplicate() {
        ProfileDefinition selected = selectedProfile();
        if (selected == null) {
            return;
        }
        this.minecraft.setScreen(new ProfileNameScreen(
                this,
                Component.translatable("screen.ecclientsettings.profiles.duplicate"),
                Component.translatable("format.ecclientsettings.profile.copy", selected.name()).getString(),
                name -> runNameOperation(() -> {
                    ProfileDefinition duplicate = controller.duplicate(selected.id(), name);
                    selectedProfileId = duplicate.id();
                })
        ));
    }

    private void openRename() {
        ProfileDefinition selected = selectedProfile();
        if (selected == null) {
            return;
        }
        this.minecraft.setScreen(new ProfileNameScreen(
                this,
                Component.translatable("screen.ecclientsettings.profiles.rename"),
                selected.name(),
                name -> runNameOperation(() -> controller.rename(selected.id(), name))
        ));
    }

    private void confirmDelete() {
        ProfileDefinition selected = selectedProfile();
        if (selected == null || selected.isDefault()) {
            return;
        }
        this.minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        try {
                            controller.delete(selected.id());
                            selectedProfileId = controller.activeProfileId();
                            error = null;
                        } catch (IOException exception) {
                            showFailure("delete", exception);
                        }
                    }
                    this.minecraft.setScreen(this);
                },
                Component.translatable("screen.ecclientsettings.profiles.delete.title"),
                Component.translatable("screen.ecclientsettings.profiles.delete.message", selected.name())
        ));
    }

    private Optional<Component> runNameOperation(ProfileOperation operation) {
        try {
            operation.run();
            error = null;
            return Optional.empty();
        } catch (IOException | IllegalArgumentException exception) {
            ECClientSettings.LOGGER.warn("Profile operation failed", exception);
            return Optional.of(Component.translatable(
                    "message.ecclientsettings.profile.operation_failed",
                    exception.getMessage()
            ));
        }
    }

    private void showFailure(String operation, Exception exception) {
        ECClientSettings.LOGGER.warn("Could not {} Profile", operation, exception);
        error = Component.translatable("message.ecclientsettings.profile.operation_failed", exception.getMessage());
    }

    private void returnToSettings() {
        this.minecraft.setScreen(ClientSettingsScreen.create(settingsParent));
    }

    private ProfileDefinition selectedProfile() {
        return controller.profiles().stream()
                .filter(profile -> profile.id().equals(selectedProfileId))
                .findFirst()
                .orElse(null);
    }

    private void updateButtons() {
        if (selectButton == null) {
            return;
        }
        ProfileDefinition selected = selectedProfile();
        boolean hasSelection = selected != null;
        selectButton.active = hasSelection && !selected.id().equals(controller.activeProfileId());
        renameButton.active = hasSelection;
        duplicateButton.active = hasSelection;
        deleteButton.active = hasSelection && !selected.isDefault();
    }

    @FunctionalInterface
    private interface ProfileOperation {
        void run() throws IOException;
    }

    private final class ProfileList extends ObjectSelectionList<ProfileEntry> {

        private ProfileList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        private void replaceProfiles(List<ProfileDefinition> profiles) {
            this.clearEntries();
            profiles.forEach(profile -> this.addEntry(new ProfileEntry(profile)));
        }

        private void select(String profileId) {
            ProfileEntry selected = this.children().stream()
                    .filter(entry -> entry.profile.id().equals(profileId))
                    .findFirst()
                    .orElse(null);
            this.setSelected(selected);
        }

        @Override
        public int getRowWidth() {
            return Math.min(320, ProfileManagementScreen.this.width - 24);
        }
    }

    private final class ProfileEntry extends ObjectSelectionList.Entry<ProfileEntry> {

        private final ProfileDefinition profile;

        private ProfileEntry(ProfileDefinition profile) {
            this.profile = profile;
        }

        @Override
        public void render(
                GuiGraphics graphics,
                int index,
                int top,
                int left,
                int width,
                int height,
                int mouseX,
                int mouseY,
                boolean hovered,
                float partialTick
        ) {
            Component name = Component.literal(profile.name());
            if (profile.id().equals(controller.activeProfileId())) {
                name = name.copy().append(Component.translatable("format.ecclientsettings.profile.active")
                        .withStyle(ChatFormatting.GREEN));
            }
            graphics.drawString(ProfileManagementScreen.this.font, name, left + 6, top + 7, 0xFFFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                selectedProfileId = profile.id();
                profileList.setSelected(this);
                updateButtons();
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return profile.id().equals(controller.activeProfileId())
                    ? Component.translatable("narration.ecclientsettings.profile.active", profile.name())
                    : Component.literal(profile.name());
        }
    }
}
