package io.github.blobanium.mcbrowser.screen;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.blobanium.mcbrowser.feature.BrowserUtil;
import io.github.blobanium.mcbrowser.MCBrowser;
import io.github.blobanium.mcbrowser.util.BrowserMatrixHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;


public class BrowserScreen extends Screen {
    private static final int BROWSER_DRAW_OFFSET = 50;
    private static final int Z_SHIFT = -1;
    private static final int ENTER_KEY_CODE = 257;

    private MCEFBrowser browser;

    private MinecraftClient minecraft = MinecraftClient.getInstance();

    //URL
    private String initURL;
    private String currentUrl;


    //Ui
    private TextFieldWidget urlBox;
    private ButtonWidget forwardButton;
    private ButtonWidget backButton;
    private ButtonWidget reloadButton;
    private ButtonWidget homeButton;
    private ButtonWidget[] navigationButtons;

    //Mouse Position
    private double lastMouseX;
    private double lastMouseY;

    public BrowserScreen(Text title, String url) {
        super(title);
        this.initURL = url;
    }

    @Override
    protected void init() {
        super.init();
        if (browser == null) {
            boolean transparent = true;
            browser = MCEF.createBrowser(this.initURL, transparent);
            resizeBrowser();
            initButtons();
        }
    }

    private void initButtons(){
        this.urlBox = new TextFieldWidget(minecraft.textRenderer, BROWSER_DRAW_OFFSET + 80,BROWSER_DRAW_OFFSET-20,getUrlBoxWidth(),15, Text.of("TEST1234")){
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers){
                if(isFocused()) {
                    browser.setFocus(false);
                    if(keyCode == ENTER_KEY_CODE){
                        browser.loadURL(BrowserUtil.prediffyURL(getText()));
                        setFocused(false);
                    }
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        };
        urlBox.setMaxLength(2048); //Most browsers have a max length of 2048
        urlBox.setText(Text.of("").getString());
        addSelectableChild(urlBox);

        backButton = initButton(Text.of("\u25C0"), button -> browser.goBack(), BROWSER_DRAW_OFFSET);
        addSelectableChild(backButton);

        forwardButton = initButton(Text.of("\u25B6"), button -> browser.goForward(), BROWSER_DRAW_OFFSET + 20);
        addSelectableChild(forwardButton);

        reloadButton = initButton(Text.of("\u27F3"), button -> {
                    if(browser.isLoading()){
                        browser.stopLoad();
                    }else{
                        browser.reload();
                    }
                },
                BROWSER_DRAW_OFFSET + 40);
        addSelectableChild(reloadButton);

        homeButton = initButton(Text.of("\u2302"), button -> browser.loadURL(MCBrowser.getConfig().homePage), BROWSER_DRAW_OFFSET + 60);
        addSelectableChild(homeButton);

        navigationButtons = new ButtonWidget[]{forwardButton, backButton, reloadButton, homeButton};
    }

    private ButtonWidget initButton(Text message, ButtonWidget.PressAction onPress, int positionX){
        return ButtonWidget.builder(message, onPress)
                .dimensions(positionX, BROWSER_DRAW_OFFSET-20, 15, 15)
                .build();
    }

    private void resizeBrowser() {
        if (width > 100 && height > 100) {
            browser.resize(BrowserMatrixHelper.scaleX(width, BROWSER_DRAW_OFFSET), BrowserMatrixHelper.scaleY(height, BROWSER_DRAW_OFFSET));
        }
        if(this.urlBox != null) {
            urlBox.setWidth(getUrlBoxWidth());
        }
    }

    @Override
    public void resize(MinecraftClient minecraft, int i, int j) {
        super.resize(minecraft, i, j);
        resizeBrowser();

        if(!children().contains(urlBox)){
            addSelectableChild(urlBox);
        }
        for(ButtonWidget button : navigationButtons){
            if(!children().contains(button)){
                addSelectableChild(button);
            }
        }
    }

    @Override
    public void close() {
        browser.close();
        MCBrowser.requestOpen = false;
        super.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, browser.getRenderer().getTextureID());
        Tessellator t = Tessellator.getInstance();
        BufferBuilder buffer = t.getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(BROWSER_DRAW_OFFSET, height - BROWSER_DRAW_OFFSET, Z_SHIFT).texture(0.0f, 1.0f).color(255, 255, 255, 255).next();
        buffer.vertex(width - BROWSER_DRAW_OFFSET, height - BROWSER_DRAW_OFFSET, Z_SHIFT).texture(1.0f, 1.0f).color(255, 255, 255, 255).next();
        buffer.vertex(width - BROWSER_DRAW_OFFSET, BROWSER_DRAW_OFFSET, Z_SHIFT).texture(1.0f, 0.0f).color(255, 255, 255, 255).next();
        buffer.vertex(BROWSER_DRAW_OFFSET, BROWSER_DRAW_OFFSET, Z_SHIFT).texture(0.0f, 0.0f).color(255, 255, 255, 255).next();
        t.draw();
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableDepthTest();
        urlBox.renderButton(context, mouseX, mouseY, delta);
        for(ButtonWidget button : navigationButtons){
            button.render(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
    browser.sendMousePress(BrowserMatrixHelper.mouseX(mouseX, BROWSER_DRAW_OFFSET), BrowserMatrixHelper.mouseY(mouseY, BROWSER_DRAW_OFFSET), button);
        updateMouseLocation(mouseX, mouseY);
        setFocus();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        browser.sendMouseRelease(BrowserMatrixHelper.mouseX(mouseX, BROWSER_DRAW_OFFSET), BrowserMatrixHelper.mouseY(mouseY, BROWSER_DRAW_OFFSET), button);
        updateMouseLocation(mouseX, mouseY);
        setFocus();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        browser.sendMouseMove(BrowserMatrixHelper.mouseX(mouseX, BROWSER_DRAW_OFFSET), BrowserMatrixHelper.mouseY(mouseY, BROWSER_DRAW_OFFSET));
        updateMouseLocation(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        updateMouseLocation(mouseX, mouseY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        browser.sendMouseWheel(BrowserMatrixHelper.mouseX(mouseX, BROWSER_DRAW_OFFSET), BrowserMatrixHelper.mouseY(mouseY, BROWSER_DRAW_OFFSET), delta, 0);
        updateMouseLocation(mouseX, mouseY);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        System.out.println("mod=" + modifiers + " keycode="+ keyCode);
        if(!(keyCode == GLFW.GLFW_KEY_A && modifiers == GLFW.GLFW_MOD_CONTROL && urlBox.isFocused())) {
            browser.sendKeyPress(keyCode, scanCode, modifiers);
        }
        setFocus();
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        browser.sendKeyRelease(keyCode, scanCode, modifiers);
        setFocus();
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (codePoint == (char) 0) return false;
        browser.sendKeyTyped(codePoint, modifiers);
        setFocus();
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void tick(){
        String getURL = browser.getURL();

        if(currentUrl != getURL){
            currentUrl = getURL;
            if(!urlBox.isFocused()) {
                urlBox.setText(Text.of(currentUrl).getString());
                urlBox.setCursorToStart();
            }
        }

        forwardButton.active = browser.canGoForward();
        backButton.active = browser.canGoBack();

        if(browser.isLoading()){
            reloadButton.setMessage(Text.of("\u274C"));
        } else {
            reloadButton.setMessage(Text.of("\u27F3"));
        }
    }

    private void updateMouseLocation(double mouseX, double mouseY){
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    public void setFocus(){
        if(isOverWidgets()){
            browser.setFocus(false);
            urlBox.setFocused(urlBox.isMouseOver(lastMouseX, lastMouseY));
            for(ButtonWidget button : navigationButtons){
                button.setFocused(button.isMouseOver(lastMouseX, lastMouseY));
            }
        }else{
            urlBox.setFocused(false);
            for(ButtonWidget button : navigationButtons){
                button.setFocused(false);
            }
            browser.setFocus(true);
        }
    }
    private boolean isOverWidgets(){
        if(urlBox.isMouseOver(lastMouseX, lastMouseY)){
            return true;
        }
        for(ButtonWidget button : navigationButtons){
            if(button.isMouseOver(lastMouseX, lastMouseY)){
                return true;
            }
        }
        return false;
    }

    private int getUrlBoxWidth(){
        return width - (BROWSER_DRAW_OFFSET * 2) - 80;
    }
}
