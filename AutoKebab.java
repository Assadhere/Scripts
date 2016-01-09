package us.scriptwith.rsbot.scripts.rs3.autokebab;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.concurrent.Callable;

import org.powerbot.script.Condition;
import org.powerbot.script.MessageEvent;
import org.powerbot.script.MessageListener;
import org.powerbot.script.PaintListener;
import org.powerbot.script.PollingScript;
import org.powerbot.script.Random;
import org.powerbot.script.Script;
import org.powerbot.script.Tile;
import org.powerbot.script.rt6.ChatOption;
import org.powerbot.script.rt6.ClientContext;
import org.powerbot.script.rt6.GeItem;
import org.powerbot.script.rt6.Npc;
import org.powerbot.script.rt6.TilePath;
@Script.Manifest(Name = "Auto Kebab", Description = "Buys kebab for no requirements money making.", Properties = "author=Assad1992; topic=1297784; client=6;")

//implements MessageListener so we can react based on incoming game messages
//PaintListener to draw a paint
public class AutoKebab extends PollingScript<ClientContext> implements MessageListener, PaintListener {

    public static final int KEBAB_COST = 20;
    public static final int KEBAB = 1971;
    public static final int KARIM = 543;
    public static final String[] CHAT_OPTIONS = {"Would you like to buy a nice kebab? Only one gold.", "Yes please."};
    public static final Tile[] PATH = {
            new Tile(3271, 3168, 0),
            new Tile(3276, 3176, 0),
            new Tile(3275, 3181, 0),
            new Tile(3271, 3184, 0),
    };

    private TilePath pathToBank, pathToTrader;
    private int kebabsBought = 0, kebabGePrice = 450;

    @Override
    public void start() {
        //creates the TilePath instances to walk from bank to trader, and from trader to bank
        //the arguments are the context instance, and a Tile[] of the tiles that make up the path
        pathToTrader = new TilePath(ctx, PATH);
        pathToBank = new TilePath(ctx, PATH).reverse();
        kebabGePrice = new GeItem(KEBAB).price;
    }

    @Override
    public void poll() {
        //if the user does not have enough gold to buy a fur, stop the script
        if (ctx.backpack.moneyPouchCount() < KEBAB_COST) {
            ctx.controller.stop();
        }

        final State state = getState();
        if (state == null) {
            return;
        }
        switch (state) {
            case WALK_TO_TRADER:
                //traverse() finds the next tile in the path, and returns if it successfully stepped towards that tile
                pathToTrader.traverse();
                Condition.sleep(Random.nextInt(2000, 2500));
                break;
            case BUY_KEBAB:
                //chatting() returns if the player is currently chatting with something/someone
                //if the user is not chatting...
                if (!ctx.chat.chatting()) {
                    //... talk to the trader
                    final Npc trader = ctx.npcs.select().id(KARIM).nearest().poll();
                    if (trader.interact("Talk-to")) {
                        //wait until the chat interface comes up (prevents spam clicking) for a maximum of 2500ms (250*10)
                        //Condition.wait(condition to wait for, how long to sleep for each iteration, how many iterations to go through)
                        //sleeps until call() returns true, then wait() returns true;
                        //or times out and returns false
                        Condition.wait(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return ctx.chat.chatting();
                            }
                        }, 250, 10);
                    }
                }
                //queryContinue() returns if there is a "continue" button available in the chatting interface
                //if there is a continue button...
                else if (ctx.chat.queryContinue()) {
                    //...continue through it
                    //clickContinue() accepts a boolean argument
                    //pass 'true' to use a key (press Enter), pass 'false' to use the mouse
                    ctx.chat.clickContinue(true);
                    //sleep for 350-500ms
                    Condition.sleep(Random.nextInt(350, 500));
                }
                //Chat query works like any other query, except for the chat options
                //finds all chat options that match the CHAT_OPTIONS argument
                //if there is an option that matches...
                else if (!ctx.chat.select().text(CHAT_OPTIONS).isEmpty()) {
                    final ChatOption option = ctx.chat.poll();
                    //... select that option (true to use key instead of mouse)
                    if (option.select(true)) {
                        //sleep until the interface is completed
                        Condition.wait(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return !option.valid();
                            }
                        }, 250, 10);
                    }
                }
                break;
            case WALK_TO_BANK:
                pathToBank.traverse();
                break;
            case BANK:
                //opened() returns if the bank is currently open
                //if the bank is not open...
                if (!ctx.bank.opened()) {
                    //... open it
                    ctx.bank.open();
                }
                //if the user has bear fur in their inventory
                else if (!ctx.backpack.select().id(KEBAB).isEmpty()) {
                    //click the "deposit inventory" button in the bank
                    ctx.bank.depositInventory();
                } else {
                    //close the bank when we're done
                    ctx.bank.close();
                }
                break;
        }
    }

    private State getState() {
        if (ctx.bank.opened()) {
            return State.BANK;
        } else if (ctx.backpack.select().count() < 28) {
            if (!ctx.npcs.select().id(KARIM).within(10).isEmpty()) {
                return State.BUY_KEBAB;
            } else {
                return State.WALK_TO_TRADER;
            }
        } else if (!ctx.bank.inViewport()) {
            return State.WALK_TO_BANK;
        } else if (ctx.bank.nearest().tile().distanceTo(ctx.players.local()) < 10) {
            return State.BANK;
        }
        return null;
    }

    private enum State {
        WALK_TO_TRADER, BUY_KEBAB, WALK_TO_BANK, BANK
    }

    @Override
    public void messaged(MessageEvent e) {
        final String msg = e.text().toLowerCase();
        //when we receive a message that says "20 coins have been removed..."
        if (msg.equals("one coin has been removed from your money pouch.")) {
            //that means we bought a kebab; increment the count.
            kebabsBought++;
        }
    }

    public static final Font TAHOMA = new Font("Tahoma", Font.PLAIN, 12);

    @Override
    public void repaint(Graphics graphics) {
        final Graphics2D g = (Graphics2D) graphics;
        g.setFont(TAHOMA);

        final int profit = kebabGePrice * kebabsBought;
        final int profitHr = (int) ((profit * 3600000D) / getRuntime());
        final int kebabHr = (int) ((kebabsBought * 3600000D) / getRuntime());

        g.setColor(Color.WHITE);
        g.fillRect(5, 5, 220, 80);

        g.setColor(Color.BLACK);

        g.drawString(String.format("Kebabs: %,d (%,d)", kebabsBought, kebabHr), 10, 20);
        g.drawString(String.format("Profit: %,d (%,d)", profit, profitHr), 10, 40);
        long seconds = getRuntime() / 1000 %60;
        long minutes =getRuntime() /60000 %60;
        long hours =getRuntime() /3600000;
        g.drawString("Time : " + seconds + "s " + minutes +"m " + hours + "h" , 10, 60 );
        
    
    }
}
