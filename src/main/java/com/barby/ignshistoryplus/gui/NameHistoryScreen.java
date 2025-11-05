package com.barby.ignshistoryplus.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;


public class NameHistoryScreen extends Screen {
    private final String username;
    private final String uuid;
    private final List<String> names;
    private final List<String> dates;
    private final int total;
    private final PlayerSkinWidget skinWidget;

    
    private int scrollIndex = 0;

    
    private int copyX, copyY, copyW, copyH;

    
    private static final int DEFAULT_SKIN_WIDTH = 80;
    private static final int DEFAULT_SKIN_HEIGHT = 160;

    
    private static final int SKIN_OFFSET_X = -25;
    private static final int SKIN_OFFSET_Y = 20;

    
    private static final Identifier LOGO = Identifier.of("ignshistory_plus", "crafty_logo.png");

    
    private static final int LOGO_WIDTH = 102;
    private static final int LOGO_HEIGHT = 40;

    
    public NameHistoryScreen(String username, String uuid, List<String> names, List<String> dates,
                             int total, PlayerSkinWidget skinWidget) {
        super(Text.literal("Name History"));
        this.username = username;
        this.uuid = uuid;
        this.names = names;
        this.dates = dates;
        this.total = total;
        this.skinWidget = skinWidget;
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        
        this.renderBackground(drawContext, mouseX, mouseY, delta);
        super.render(drawContext, mouseX, mouseY, delta);
        
        int gap = 10;
        int rowHeight = this.textRenderer.fontHeight + 2;

        int maxNameWidth = 0;
        for (int i = 0; i < names.size(); i++) {
            String entry = names.get(i);
            String date = (dates != null && i < dates.size()) ? dates.get(i) : "";
            String text = "- " + entry + (date != null && !date.isEmpty() ? " (" + date + ")" : "");
            int width = this.textRenderer.getWidth(text);
            if (width > maxNameWidth) {
                maxNameWidth = width;
            }
        }
        int columnWidth = maxNameWidth + 20;

        
        int infoLines = 4;
        int infoHeight = infoLines * (this.textRenderer.fontHeight + 2);

        
        int topMargin = Math.max(0, this.height / 6 - 40);
        int availableHeight = this.height - topMargin - infoHeight - 20;
        int maxRows = Math.max(1, availableHeight / rowHeight);
        int rowsToShow = Math.min(maxRows, names.size());
        int columns = 1;

        
        int skinW = 0;
        int skinH = 0;
        if (skinWidget != null) {
            skinW = skinWidget.getWidth();
            skinH = skinWidget.getHeight();
            if (skinW <= 0) skinW = DEFAULT_SKIN_WIDTH;
            if (skinH <= 0) skinH = DEFAULT_SKIN_HEIGHT;
        }

        
        int logoW = LOGO_WIDTH;
        int logoH = LOGO_HEIGHT;

        
        int totalWidth = (skinWidget != null ? skinW : 0) + (skinWidget != null ? gap : 0) + (columns * columnWidth);
        int xStart = (this.width - totalWidth) / 2;
        int xSkin = xStart;
        int xTextStart = xStart + (skinWidget != null ? skinW + gap : 0);

        
        int yStart = topMargin;
        int logoXCentered = xTextStart + (columnWidth - LOGO_WIDTH) / 2;
        drawContext.drawTexture(LOGO, logoXCentered - 5, yStart - 10, 0, 0, LOGO_WIDTH, LOGO_HEIGHT, LOGO_WIDTH, LOGO_HEIGHT);
        int infoY = yStart + LOGO_HEIGHT + 4;

        
        if (skinWidget != null) {
            skinWidget.setPosition(xSkin + SKIN_OFFSET_X, infoY + SKIN_OFFSET_Y);
            skinWidget.render(drawContext, mouseX, mouseY, delta);
        }

        
        int y = infoY;
        String userLine = "Username: " + username;
        drawContext.drawText(this.textRenderer, userLine, xTextStart, y, 0xFFFFFF, false);
        y += this.textRenderer.fontHeight + 2;

        
        if (uuid != null && !uuid.isEmpty()) {
            String label = "UUID: ";
            int labelWidth = this.textRenderer.getWidth(label);
            drawContext.drawText(this.textRenderer, label, xTextStart, y, 0xFFFFFF, false);
            int uuidX = xTextStart + labelWidth;
            int uuidColor = 0x00FF88;
            drawContext.drawText(this.textRenderer, uuid, uuidX, y, uuidColor, false);
            String copyText = " [copy]";
            int uuidWidth = this.textRenderer.getWidth(uuid);
            this.copyX = uuidX + uuidWidth;
            this.copyY = y;
            this.copyW = this.textRenderer.getWidth(copyText);
            this.copyH = this.textRenderer.fontHeight;
            drawContext.drawText(this.textRenderer, copyText, copyX, copyY, 0x66CCFF, false);
        } else {
            this.copyX = this.copyY = this.copyW = this.copyH = 0;
        }
        y += this.textRenderer.fontHeight + 2;

        
        String totalLine = "Total Names: " + total;
        drawContext.drawText(this.textRenderer, totalLine, xTextStart, y, 0xFFFFFF, false);
        y += this.textRenderer.fontHeight + 4;

        
        drawContext.drawText(this.textRenderer, "Previous Names:", xTextStart, y, 0xAAAAFF, false);
        y += this.textRenderer.fontHeight + 2;

        
        int namesStartX = xTextStart;
        
        int namesStartY = y + 2;
        int namesAreaWidth = columnWidth;
        int namesAreaHeight = rowsToShow * rowHeight;

        
        int bgX1 = namesStartX - 4;
        int bgY1 = namesStartY - 2;
        int bgX2 = namesStartX + namesAreaWidth + 4;
        int bgY2 = namesStartY + namesAreaHeight + 2;
        drawContext.fill(bgX1, bgY1, bgX2, bgY2, 0x44000000);
        int borderCol = 0x55FFFFFF;
        drawContext.fill(bgX1, bgY1, bgX2, bgY1 + 1, borderCol);
        drawContext.fill(bgX1, bgY2 - 1, bgX2, bgY2, borderCol);
        drawContext.fill(bgX1, bgY1, bgX1 + 1, bgY2, borderCol);
        drawContext.fill(bgX2 - 1, bgY1, bgX2, bgY2, borderCol);

        
        int maxScroll = Math.max(0, names.size() - rowsToShow);
        if (scrollIndex > maxScroll) scrollIndex = maxScroll;
        if (scrollIndex < 0) scrollIndex = 0;

        
        int yRow = namesStartY;
        for (int row = 0; row < rowsToShow; row++) {
            int index = scrollIndex + row;
            if (index >= names.size()) {
                break;
            }
            String entry = names.get(index);
            String date = (dates != null && index < dates.size()) ? dates.get(index) : "";
            String text = "- " + entry + (date != null && !date.isEmpty() ? " (" + date + ")" : "");
            drawContext.drawText(this.textRenderer, text, namesStartX, yRow, 0xFFFFFF, false);
            yRow += rowHeight;
        }

        
        if (names.size() > rowsToShow) {
            float visibleRatio = rowsToShow / (float) names.size();
            int barHeight = Math.max(4, (int) (namesAreaHeight * visibleRatio));
            int trackHeight = namesAreaHeight - barHeight;
            int maxScrollInt = Math.max(1, names.size() - rowsToShow);
            float scrollRatio = scrollIndex / (float) maxScrollInt;
            int barY = namesStartY + (int) (trackHeight * scrollRatio);
            int barX = namesStartX + namesAreaWidth + 4;
            int trackColor = 0x80EEEEEE;
            int thumbColor = 0xFFCFCFCF;
            drawContext.fill(barX, namesStartY, barX + 4, namesStartY + namesAreaHeight, trackColor);
            drawContext.fill(barX, barY, barX + 4, barY + barHeight, thumbColor);
        }

        
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        
        if (uuid != null && !uuid.isEmpty()) {
            if (mouseX >= copyX && mouseX <= copyX + copyW &&
                mouseY >= copyY && mouseY <= copyY + copyH) {
                net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                client.keyboard.setClipboard(uuid);
                client.getSoundManager().play(
                    net.minecraft.client.sound.PositionedSoundInstance.master(
                            net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0F
                    )
                );
                client.inGameHud.getChatHud().addMessage(Text.of("§aUUID copied to clipboard"));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        
        int topMargin = Math.max(0, this.height / 6 - 40);
        int rowHeight = this.textRenderer.fontHeight + 2;
        int infoLines = 4;
        int infoHeight = infoLines * (this.textRenderer.fontHeight + 2);
        int availableHeight = this.height - topMargin - infoHeight - 20;
        int maxRows = Math.max(1, availableHeight / rowHeight);
        int rowsToShow = Math.min(maxRows, names.size());
        if (names.size() > rowsToShow) {
            int maxScroll = Math.max(0, names.size() - rowsToShow);
            if (verticalAmount < 0) {
                scrollIndex = Math.min(scrollIndex + 1, maxScroll);
            } else if (verticalAmount > 0) {
                scrollIndex = Math.max(scrollIndex - 1, 0);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}