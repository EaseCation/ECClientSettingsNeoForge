package net.easecation.clientsettings.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

final class ProfileNameScreen extends Screen {

    @FunctionalInterface
    interface Submitter {
        Optional<Component> submit(String name);
    }

    private final Screen parent;
    private final String initialName;
    private final Submitter submitter;
    private EditBox nameField;
    private Component error;

    ProfileNameScreen(Screen parent, Component title, String initialName, Submitter submitter) {
        super(title);
        this.parent = parent;
        this.initialName = initialName;
        this.submitter = submitter;
    }

    @Override
    protected void init() {
        int fieldWidth = Math.min(300, this.width - 40);
        int fieldX = (this.width - fieldWidth) / 2;
        int fieldY = this.height / 2 - 18;
        this.nameField = new EditBox(
                this.font,
                fieldX,
                fieldY,
                fieldWidth,
                20,
                Component.translatable("option.ecclientsettings.profile.name")
        );
        this.nameField.setMaxLength(128);
        this.nameField.setValue(initialName);
        this.addRenderableWidget(this.nameField);
        this.setInitialFocus(this.nameField);

        int buttonWidth = Math.min(145, (fieldWidth - 6) / 2);
        int buttonsX = (this.width - (buttonWidth * 2 + 6)) / 2;
        int buttonsY = fieldY + 34;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> submit())
                .bounds(buttonsX, buttonsY, buttonWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(buttonsX + buttonWidth + 6, buttonsY, buttonWidth, 20)
                .build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 52, 0xFFFFFFFF);
        if (error != null) {
            graphics.drawCenteredString(this.font, error, this.width / 2, this.height / 2 + 42, 0xFFFF5555);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private void submit() {
        Optional<Component> validationError = submitter.submit(nameField.getValue());
        if (validationError.isPresent()) {
            error = validationError.get();
            return;
        }
        this.minecraft.setScreen(parent);
    }
}
