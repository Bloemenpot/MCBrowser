package io.github.blobanium.mcbrowser.screen;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;

import io.github.blobanium.mcbrowser.feature.BrowserUtil;
import io.github.blobanium.mcbrowser.MCBrowser;
import io.github.blobanium.mcbrowser.feature.specialbutton.SpecialButtonActions;
import io.github.blobanium.mcbrowser.feature.specialbutton.SpecialButtonHelper;
import io.github.blobanium.mcbrowser.util.BrowserScreenHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;


public class BrowserScreen extends Screen {
    private static final int BROWSER_DRAW_OFFSET = 50;

    private MCEFBrowser browser;

    //URL
    private String initURL;


    //Ui
    private TextFieldWidget urlBox;
    private ButtonWidget forwardButton;
    private ButtonWidget backButton;
    private ButtonWidget reloadButton;
    private ButtonWidget homeButton;
    private ButtonWidget[] navigationButtons;
    private ClickableWidget[] uiElements;

    private ButtonWidget specialButton;

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
            boolean transparent = false;
            browser = MCEF.createBrowser(this.initURL, transparent);
            resizeBrowser();
            initButtons();
        }
    }

    private void initButtons(){
        this.urlBox = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, BROWSER_DRAW_OFFSET + 80,BROWSER_DRAW_OFFSET-20,BrowserScreenHelper.getUrlBoxWidth(width, BROWSER_DRAW_OFFSET),15, Text.of("")){
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers){
                if(isFocused()) {
                    browser.setFocus(false);
                    if(keyCode == GLFW.GLFW_KEY_ENTER){
                        browser.loadURL(BrowserUtil.prediffyURL(getText()));
                        setFocused(false);
                    }
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        };
        urlBox.setMaxLength(2048); //Most browsers have a max length of 2048
        addSelectableChild(urlBox);

        backButton = BrowserScreenHelper.initButton(Text.of("\u25C0"), button -> browser.goBack(), BROWSER_DRAW_OFFSET, BROWSER_DRAW_OFFSET);
        forwardButton = BrowserScreenHelper.initButton(Text.of("\u25B6"), button -> browser.goForward(), BROWSER_DRAW_OFFSET + 20, BROWSER_DRAW_OFFSET);
        reloadButton = BrowserScreenHelper.initButton(Text.of("\u27F3"), button -> { if(browser.isLoading()) {browser.stopLoad();} else {browser.reload();} }, BROWSER_DRAW_OFFSET + 40, BROWSER_DRAW_OFFSET);
        homeButton = BrowserScreenHelper.initButton(Text.of("\u2302"), button -> browser.loadURL(MCBrowser.getConfig().homePage), BROWSER_DRAW_OFFSET + 60, BROWSER_DRAW_OFFSET);

        navigationButtons = new ButtonWidget[]{forwardButton, backButton, reloadButton, homeButton};
        for(ButtonWidget button : navigationButtons){
            addSelectableChild(button);
        }

        specialButton = ButtonWidget.builder(Text.of(""), button -> SpecialButtonHelper.onPress(BrowserScreenHelper.currentUrl)).dimensions(BROWSER_DRAW_OFFSET, height - BROWSER_DRAW_OFFSET + 5,  150, 15).build();
        addSelectableChild(specialButton);

        uiElements = new ClickableWidget[]{forwardButton, backButton, reloadButton, homeButton, urlBox, specialButton};
    }

    private void resizeBrowser() {
        if (width > 100 && height > 100) {
            browser.resize(BrowserScreenHelper.scaleX(width, BROWSER_DRAW_OFFSET), BrowserScreenHelper.scaleY(height, BROWSER_DRAW_OFFSET));
        }
        if(this.urlBox != null) {
            urlBox.setWidth(BrowserScreenHelper.getUrlBoxWidth(width, BROWSER_DRAW_OFFSET));
        }

        if(this.specialButton != null){
            specialButton.setPosition(BROWSER_DRAW_OFFSET, height - BROWSER_DRAW_OFFSET + 5);
        }
    }

    @Override
    public void resize(MinecraftClient minecraft, int i, int j) {
        super.resize(minecraft, i, j);
        resizeBrowser();

        if(!children().contains(urlBox)){
            addSelectableChild(urlBox);
        }
        if(!children().contains(specialButton)){
            addSelectableChild(specialButton);
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
        BrowserScreenHelper.renderBrowser(BROWSER_DRAW_OFFSET, width, height, browser.getRenderer().getTextureID());
        urlBox.renderButton(context, mouseX, mouseY, delta);
        for(ButtonWidget button : navigationButtons){
            button.render(context, mouseX, mouseY, delta);
        }
        if(SpecialButtonHelper.isOnCompatableSite(BrowserScreenHelper.currentUrl)) {
            specialButton.render(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
    browser.sendMousePress(BrowserScreenHelper.mouseX(mouseX, BROWSER_DRAW_OFFSET), BrowserScreenHelper.mouseY(mouseY, BROWSER_DRAW_OFFSET), button);
        updateMouseLocation(mouseX, mouseY);
        setFocus();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        browser.sendMouseRelease(BrowserScreenHelper.mouseX(mouseX, BROWSER_DRAW_OFFSET), BrowserScreenHelper.mouseY(mouseY, BROWSER_DRAW_OFFSET), button);
        updateMouseLocation(mouseX, mouseY);
        setFocus();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        browser.sendMouseMove(BrowserScreenHelper.mouseX(mouseX, BROWSER_DRAW_OFFSET), BrowserScreenHelper.mouseY(mouseY, BROWSER_DRAW_OFFSET));
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
        browser.sendMouseWheel(BrowserScreenHelper.mouseX(mouseX, BROWSER_DRAW_OFFSET), BrowserScreenHelper.mouseY(mouseY, BROWSER_DRAW_OFFSET), delta, 0);
        updateMouseLocation(mouseX, mouseY);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(!urlBox.isFocused()) {
            browser.sendKeyPress(keyCode, scanCode, modifiers);
        }

        //Set Focus
        setFocus();

        // Make sure screen isn't sending the enter key if the buttons aren't focused.
        if(!isButtonsFocused() && keyCode == GLFW.GLFW_KEY_ENTER){
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean isButtonsFocused(){
        if(urlBox.isFocused()){
            return true;
        }
        for(ButtonWidget button : navigationButtons){
            if(button.isFocused()){
                return true;
            }
        }
        return false;
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
        final String getURL = browser.getURL();

        if(BrowserScreenHelper.currentUrl != getURL){
            BrowserScreenHelper.currentUrl = getURL;
            if(!urlBox.isFocused()) {
                urlBox.setText(Text.of(BrowserScreenHelper.currentUrl).getString());
                urlBox.setCursorToStart();
            }
            SpecialButtonActions action = SpecialButtonActions.getFromUrlConstantValue(getURL);
            if(action != null) {
                specialButton.setMessage(action.getButtonText());
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
            for(ClickableWidget widget : uiElements){
                widget.setFocused(widget.isMouseOver(lastMouseX, lastMouseY));
            }
        }else{
            for(ClickableWidget widget : uiElements){
                widget.setFocused(false);
            }
            browser.setFocus(true);
        }
    }



    private boolean isOverWidgets(){
        if(urlBox.isMouseOver(lastMouseX, lastMouseY) || specialButton.isMouseOver(lastMouseX, lastMouseY)){
            return true;
        }
        for(ButtonWidget button : navigationButtons){
            if(button.isMouseOver(lastMouseX, lastMouseY)){
                return true;
            }
        }
        return false;
    }
}
