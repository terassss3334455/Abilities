package net.coms.abilitysgorwmod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DancingAnimationSelectScreen extends Screen {
    private static final int ROW_HEIGHT=30;
    private int scroll;
    private String message="登録したアニメーションを選ぶと再生します。";
    public DancingAnimationSelectScreen(){super(Component.literal("ダンシング：アニメーション選択"));}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        renderBackground(g);g.drawCenteredString(font,title,width/2,12,0xFFFFFF);
        g.drawCenteredString(font,Component.literal(message),width/2,28,0xDDDDDD);
        var entries=DancingAnimationConfig.entries();int top=44;int visible=Math.max(1,(height-78)/ROW_HEIGHT);
        for(int row=0;row<visible;row++){
            int index=scroll+row;if(index>=entries.size())break;int y=top+row*ROW_HEIGHT;
            g.fill(width/2-140,y,width/2+140,y+26,mouseX>=width/2-140&&mouseX<=width/2+140&&mouseY>=y&&mouseY<y+26?0xFF626262:0xFF353535);
            var entry=entries.get(index);String label=entry.displayName+"  -  "+entry.clip;
            g.drawCenteredString(font,Component.literal(font.plainSubstrByWidth(label,270)),width/2,y+9,0xFFFFFF);
        }
        g.drawCenteredString(font,Component.literal("クリックで再生 / ホイールで一覧をスクロール / Escで閉じる"),width/2,height-18,0xAAAAAA);
        super.render(g,mouseX,mouseY,partial);
    }
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){
        if(button==0){int top=44;int visible=Math.max(1,(height-78)/ROW_HEIGHT);int row=(int)((mouseY-top)/ROW_HEIGHT);if(mouseY>=top&&mouseX>=width/2-140&&mouseX<=width/2+140&&row>=0&&row<visible){int index=scroll+row;var entries=DancingAnimationConfig.entries();if(index<entries.size()){if(minecraft.player==null||!DancingAnimationConfig.canDance(minecraft.player)){message="能力一覧でダンシングをONにしてください。";return true;}if(DancingAnimationConfig.play(entries.get(index))){minecraft.setScreen(null);}else{message="アニメーションファイルが見つからないか、読み込めません。";}return true;}}}
        return super.mouseClicked(mouseX,mouseY,button);
    }
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double delta){int visible=Math.max(1,(height-78)/ROW_HEIGHT);scroll=Math.max(0,Math.min(scroll+(delta<0?1:-1),Math.max(0,DancingAnimationConfig.entries().size()-visible)));return true;}
    @Override public boolean isPauseScreen(){return false;}
}