package tanks.minigames;

import tanks.*;
import tanks.bullet.Bullet;
import tanks.gui.screen.IDarkScreen;
import tanks.gui.screen.ScreenArcadeBonuses;
import tanks.gui.screen.ScreenGame;
import tanks.gui.screen.ScreenInterlevel;
import tanks.network.event.EventAirdropTank;
import tanks.obstacle.Obstacle;
import tanks.registry.RegistryTank;
import tanks.tank.*;
import tanks.translation.Translation;

import java.util.ArrayList;
import java.util.HashMap;

public class Arcade extends Minigame
{
    public double age = 0;
    
    public double lastHit = -1000;
    public int chain = 0;
    public double lastRampage = 0;
    public boolean frenzy = false;

    public static final int rampage_duration = 500;
    public static final int rampage_exit_duration = 200;
    public static final int max_power = 8;
    public static String[] rampage_titles = new String[]{"Rampage!", "Extra rampage!", "Super rampage!", "Mega rampage!!", "Giga rampage!!", "Insane rampage!!", "Ultimate rampage!!!", "Godlike rampage!!!"};

    public ArrayList<Tank> spawnedTanks = new ArrayList<>();
    public double timer = 12000;
    public double frenzyTime = -1000;

    public int score = 0;

    public int maxChain = 0;
    public int deathCount = 0;
    public boolean survivedFrenzy = true;
    public int bulletsFired = 0;
    public int kills = 0;
    public HashMap<String, Integer> destroyedTanks = new HashMap<>();
    public HashMap<String, Integer> destroyedTanksValue = new HashMap<>();

    public Arcade()
    {
        super("{28,18,235,207,166,20,20,20,0,100,50|" +
                "10-13-normal-2.5,11-13-normal-2.0,12-13-normal-2.5,15-4-normal-2.5,16-4-normal-2.0,17-4-normal-2.5,4-5-hard-2.5,4-6-hard-2.0,4-7-hard-2.5,4-8-hard-2.0,4-9-hard-2.5,4-10-hard-2.0,7-13-hard-2.0,8-13-hard-2.5,9-4-hard-2.5,9-13-hard-2.0,10-4-hard-2.0,11-4-hard-2.5,12-4-hard-2.0,13-4-hard-2.5,13-13-hard-2.0,14-4-hard-2.0,14-13-hard-2.5,15-13-hard-2.0,16-13-hard-2.5,17-13-hard-2.0,18-4-hard-2.0,18-13-hard-2.5,19-4-hard-2.5,20-4-hard-2.0,23-7-hard-2.0,23-8-hard-2.5,23-9-hard-2.0,23-10-hard-2.5,5...7-10-hole,7-11...12-hole,20-5...7-hole,21...22-7-hole" +
                "|10-10-player-0,12-10-player-0,14-10-player-0,16-10-player-0,18-10-player-0,17-7-player-2,15-7-player-2,13-7-player-2,11-7-player-2,9-7-player-2}");
        this.customLevelEnd = true;
        this.hideSpeedrunTimer = true;
        this.noLose = true;
        this.disableEndMusic = true;
    }

    @Override
    public void loadLevel()
    {
        super.loadLevel();
        Game.playerTank.team = Game.playerTeamNoFF;
    }

    @Override
    public boolean levelEnded()
    {
        return false;
    }

    @Override
    public void onLevelEnd(boolean levelWon)
    {

    }

    @Override
    public void onBulletFire(Bullet b)
    {
        if (b.tank instanceof TankPlayer || b.tank instanceof TankPlayerRemote)
        {
            bulletsFired++;
        }
    }

    @Override
    public void onKill(Tank attacker, Tank target)
    {
        if (target instanceof TankPlayer && frenzy)
            survivedFrenzy = false;

        if ((attacker instanceof TankPlayer || attacker instanceof TankPlayerRemote) && !(target instanceof IPlayerTank))
        {
            if (!destroyedTanks.containsKey(target.name))
                destroyedTanks.put(target.name, 0);

            if (!destroyedTanksValue.containsKey(target.name))
                destroyedTanksValue.put(target.name, 0);

            destroyedTanks.put(target.name, destroyedTanks.get(target.name) + 1);
            destroyedTanksValue.put(target.name, destroyedTanksValue.get(target.name) + target.coinValue);
            kills++;

            score += target.coinValue;
            Drawing.drawing.playSound("hit_chain.ogg", (float) Math.pow(2, Math.min(max_power * 3 - 1, chain) / 12.0), 0.5f);
            chain++;
            maxChain = Math.max(chain, maxChain);

            int power = Math.min(max_power, chain / 3);

            Effect e = Effect.createNewEffect(target.posX, target.posY, target.size / 2, Effect.EffectType.chain);
            e.radius = chain;
            Game.effects.add(e);

            if (chain % 3 == 0)
            {
                lastRampage = age;
                Drawing.drawing.playSound("rampage.ogg", (float) Math.pow(2, (power - 1) / 12.0));
                score += power * 5;
                if (chain / 3 <= 5)
                {
                    Drawing.drawing.addSyncedMusic("arcade/rampage" + power + ".ogg", Game.musicVolume, true, 500);
                }

                //Game.game.window.soundPlayer.setMusicSpeed((float) (1 + power * 0.2));
            }

            lastHit = age;

            for (Movable m : Game.movables)
            {
                if (m instanceof Tank)
                {
                    Tank t = (Tank) m;

                    if (chain / 3 > 0)
                    {
                        AttributeModifier c = new AttributeModifier("rampage_speed", "velocity", AttributeModifier.Operation.multiply, power / 5.0);
                        c.duration = rampage_duration + rampage_exit_duration;
                        c.deteriorationAge = rampage_exit_duration;
                        t.addUnduplicateAttribute(c);

                        AttributeModifier a = new AttributeModifier("rampage_glow", "glow", AttributeModifier.Operation.multiply, power / 5.0);
                        a.duration = rampage_duration + rampage_exit_duration;
                        a.deteriorationAge = rampage_exit_duration;
                        t.addUnduplicateAttribute(a);

                        AttributeModifier b = new AttributeModifier("rampage_reload", "reload", AttributeModifier.Operation.multiply, power / 5.0);
                        b.duration = rampage_duration + rampage_exit_duration;
                        b.deteriorationAge = rampage_exit_duration;
                        t.addUnduplicateAttribute(b);

                        AttributeModifier d = new AttributeModifier("rampage_bullet_speed", "bullet_speed", AttributeModifier.Operation.multiply, power / 5.0);
                        d.duration = rampage_duration + rampage_exit_duration;
                        d.deteriorationAge = rampage_exit_duration;
                        t.addUnduplicateAttribute(d);
                    }
                }
            }
        }
    }

    public void stopRampage()
    {
        chain = 0;
        for (int i = 1; i <= 5; i++)
        {
            Drawing.drawing.removeSyncedMusic("arcade/rampage" + i + ".ogg", 2000);
        }

        //Game.game.window.soundPlayer.setMusicSpeed((float) (1));
    }

    @Override
    public void update()
    {
        age += Panel.frameFrequency;

        if (age - lastHit > rampage_duration)
        {
            stopRampage();
        }

        if (frenzy)
            this.customLevelEnd = false;

        if (this.spawnedTanks.size() <= (Math.min(this.chain, max_power * 6) / 2) + 3 && Game.movables.contains(Game.playerTank) && timer > 0)
        {
            int count = (int) ((Math.random() * 2 + 2));
            for (int i = 0; i < count; i++)
            {
                spawnTank();
            }
        }

        for (int i = 0; i < this.spawnedTanks.size(); i++)
        {
            if (this.spawnedTanks.get(i).destroy)
            {
                this.spawnedTanks.remove(i);
                i--;
            }
        }

        if (!Game.movables.contains(Game.playerTank))
        {
            boolean foundPlayer = false;
            for (Movable m: Game.movables)
            {
                if (m instanceof Crate && ((Crate) m).tank == Game.playerTank || m == Game.playerTank)
                {
                    foundPlayer = true;
                    continue;
                }

                stopRampage();
                m.destroy = true;
                spawnedTanks.clear();
            }

            if (!foundPlayer && !frenzy)
            {
                deathCount++;
                Game.playerTank = new TankPlayer(this.sizeX / 2.0 * Game.tile_size, this.sizeY / 2.0 * Game.tile_size, 0);
                Game.playerTank.team = Game.playerTeamNoFF;
                Game.movables.add(new Crate(Game.playerTank));
            }
        }

        if (timer > 0)
        {
            int seconds = (int) (timer / 100 + 0.5);
            int secondHalves = (int) (timer / 50);
            timer -= Panel.frameFrequency;

            int newSeconds = (int) (timer / 100 + 0.5);
            int newSecondHalves = (int) (timer / 50);

            if (seconds <= 5)
            {
                if (newSecondHalves < secondHalves)
                    Drawing.drawing.playSound("tick.ogg", 2f, 0.5f);
            }
            else if (newSeconds < seconds && seconds <= 10)
                Drawing.drawing.playSound("tick.ogg", 2f, 0.5f);

            if (seconds > newSeconds && (newSeconds == 10 || newSeconds == 30 || newSeconds == 60))
                Drawing.drawing.playSound("timer.ogg");
        }
        else if (!frenzy && Game.movables.contains(Game.playerTank))
        {
            frenzy = true;
            frenzyTime = age;
            Drawing.drawing.playSound("rampage.ogg");

            for (int i = 0; i < 30; i++)
            {
                spawnTank();
            }
        }
    }

    @Override
    public void draw()
    {
        if (age - lastRampage < 200 && chain >= 3)
        {
            int power = Math.min(max_power, chain / 3);

            double c = 0.5 - (power) * 3.0 / 30;
            if (c < 0)
                c += (int) (-c) + 1;

            double[] col = Game.getRainbowColor(c);
            double frac = (1 - Math.max(0, age - lastRampage - 100) / 100.0);
            double mul = (1 + (1 - Math.min(1, (age - lastRampage) / 25.0)));

            Drawing.drawing.setInterfaceFontSize(50 * mul);
            Drawing.drawing.setColor(col[0] / 2, col[1] / 2, col[2] / 2, frac * 255);
            Drawing.drawing.displayInterfaceText(Drawing.drawing.interfaceSizeX / 2 + 5, Drawing.drawing.interfaceSizeY / 2 + 5, rampage_titles[Math.min(chain / 3 - 1, rampage_titles.length - 1)]);
            Drawing.drawing.setColor(col[0], col[1], col[2], frac * 255);
            Drawing.drawing.displayInterfaceText(Drawing.drawing.interfaceSizeX / 2, Drawing.drawing.interfaceSizeY / 2, rampage_titles[Math.min(chain / 3 - 1, rampage_titles.length - 1)]);

            Drawing.drawing.setInterfaceFontSize(20 * mul);
            Drawing.drawing.setColor(col[0] / 2, col[1] / 2, col[2] / 2, frac * 255);
            Drawing.drawing.displayInterfaceText(Drawing.drawing.interfaceSizeX / 2 + 2.5, Drawing.drawing.interfaceSizeY / 2 + 33 * mul + 2.5, "+%d points!", power * 5);
            Drawing.drawing.setColor(col[0], col[1], col[2], frac * 255);
            Drawing.drawing.displayInterfaceText(Drawing.drawing.interfaceSizeX / 2, Drawing.drawing.interfaceSizeY / 2 + 33 * mul, "+%d points!", power * 5);
        }

        if (age - frenzyTime < 400)
        {
            double frac = (1 - Math.max(0, age - frenzyTime - 100) / 100.0);
            double mul = (1 + (1 - Math.min(1, (age - frenzyTime) / 25.0)));
            double[] col = new double[]{255, 180, 0};

            Drawing.drawing.setInterfaceFontSize(75 * mul);
            Drawing.drawing.setColor(col[0] / 2, col[1] / 2, col[2] / 2, frac * 255);
            Drawing.drawing.displayInterfaceText(Drawing.drawing.interfaceSizeX / 2 + 5, Drawing.drawing.interfaceSizeY / 2 + 5, "Tank frenzy!");
            Drawing.drawing.setColor(col[0], col[1], col[2], frac * 255);
            Drawing.drawing.displayInterfaceText(Drawing.drawing.interfaceSizeX / 2, Drawing.drawing.interfaceSizeY / 2, "Tank frenzy!");

            col[1] = 100;
            Drawing.drawing.setInterfaceFontSize(30 * mul);
            Drawing.drawing.setColor(col[0] / 2, col[1] / 2, col[2] / 2, frac * 255);
            Drawing.drawing.displayInterfaceText(Drawing.drawing.interfaceSizeX / 2 + 2.5, Drawing.drawing.interfaceSizeY / 2 + 40 * mul + 2.5, "Destroy as many tanks as you can!");
            Drawing.drawing.setColor(col[0], col[1], col[2], frac * 255);
            Drawing.drawing.displayInterfaceText(Drawing.drawing.interfaceSizeX / 2, Drawing.drawing.interfaceSizeY / 2 + 40 * mul, "Destroy as many tanks as you can!");
        }

        this.drawTimer();
        this.drawPoints();
    }

    @Override
    public void loadInterlevelScreen()
    {
        Game.screen = new ScreenArcadeBonuses(this);
    }

    public void spawnTank()
    {
        double destX = 0;
        double destY = 0;

        boolean found = false;

        boolean horizontal = Math.random() < 0.5;
        boolean other = Math.random() < 0.5;

        int xBound = Game.currentSizeX;
        int yBound = Game.currentSizeY;

        if (horizontal)
            yBound = 4;
        else
            xBound = 4;

        for (int i = 0; i < 5; i++)
        {
            int x = (int) (Math.random() * xBound);
            int y = (int) (Math.random() * yBound);

            if (other && !horizontal)
                x += Game.currentSizeX - 4;
            else if (other)
                y += Game.currentSizeY - 4;

            if (!Game.game.solidGrid[x][y] && (Game.playerTank == null || (Math.pow(Game.playerTank.posX - (x * Game.tile_size), 2) + Math.pow(Game.playerTank.posY - (y * Game.tile_size), 2) > Math.pow(Game.tile_size * 5, 2))))
            {
                found = true;
                destX = (x + 0.5) * Game.tile_size;
                destY = (y + 0.5) * Game.tile_size;

                Drawing.drawing.playGlobalSound("flame.ogg", 0.75f);
                break;
            }
        }

        if (!found)
            return;

        RegistryTank.TankEntry e = Game.registryTank.getRandomTank();

        while (e.name.equals("blue") || e.name.equals("red"))
            e = Game.registryTank.getRandomTank();

        while (!frenzy && e.weight < 1.0 / Math.max(1, chain - 2))
            e = Game.registryTank.getRandomTank();

        Tank t = e.getTank(destX, destY, (int)(Math.random() * 4));
        t.team = Game.enemyTeam;
        Game.eventsOut.add(new EventAirdropTank(t));
        this.spawnedTanks.add(t);
        Game.movables.add(new Crate(t));
    }

    public void drawTimer()
    {
        int secondsTotal = (int) (timer / 100 + 0.5);
        double secondsFrac = (timer / 100 + 0.5) - secondsTotal;

        int seconds60 = secondsTotal % 60;
        int minutes = secondsTotal / 60;

        double sizeMul = 1;
        double alpha = 127;
        double red = 0;

        if (((ScreenGame) Game.screen).playing)
        {
            if (secondsTotal == 60 || secondsTotal == 30 || secondsTotal <= 10)
            {
                sizeMul = 1.5;

                if (secondsFrac > 0.4 && secondsFrac <= 0.8 && secondsTotal > 9)
                    alpha = 0;

                if (secondsTotal <= 9)
                    red = Math.max(0, secondsFrac * 2 - 1) * 255;

                if (secondsTotal <= 5 && red == 0)
                    red = Math.max(0, secondsFrac * 2) * 255;
            }
            else if (secondsTotal == 59 || secondsTotal == 29)
                sizeMul = 1.0 + Math.max((timer / 100 - secondsTotal), 0);
        }

        String st = Translation.translate("Time: ");
        String s = st + minutes + ":" + seconds60;
        if (seconds60 < 10)
            s = st + minutes + ":0" + seconds60;

        Drawing.drawing.setInterfaceFontSize(32 * sizeMul);
        Drawing.drawing.setColor(red, 0, 0, (alpha + red / 2) * Obstacle.draw_size / Game.tile_size);

        if (Level.isDark())
            Drawing.drawing.setColor(255, 255 - red, 255 - red, (alpha + red / 2) * Obstacle.draw_size / Game.tile_size);

        double posX = Drawing.drawing.interfaceSizeX / 2;
        double posY = 50;

        if (ScreenGame.finishedQuick)
        {
            Drawing.drawing.setInterfaceFontSize(32);
            Drawing.drawing.setColor(0, 0, 0, 127 * Obstacle.draw_size / Game.tile_size);

            if (Level.isDark())
                Drawing.drawing.setColor(255, 255, 255, 127);
        }

        if (timer > 0)
            Drawing.drawing.displayInterfaceText(posX, posY, s);
    }

    public void drawPoints()
    {
        double frac = Math.max(0, (25 - (age - lastHit)) / 25);
        double alpha = (127 + 128 * frac) * Obstacle.draw_size / Game.tile_size;

        if (Level.isDark() || (Game.screen instanceof IDarkScreen && Panel.win && Game.effectsEnabled))
            Drawing.drawing.setColor(255, 255, 255, alpha);
        else
            Drawing.drawing.setColor(0, 0, 0, alpha);

        double posX = -(Game.game.window.absoluteWidth / Drawing.drawing.interfaceScale - Drawing.drawing.interfaceSizeX) / 2 + Game.game.window.getEdgeBounds() / Drawing.drawing.interfaceScale + 175;
        double posY = -((Game.game.window.absoluteHeight - Drawing.drawing.statsHeight) / Drawing.drawing.interfaceScale - Drawing.drawing.interfaceSizeY) / 2 + 50;

        Drawing.drawing.setInterfaceFontSize(36 * (1 + 0.25 * frac));
        String s = Translation.translate("Points: %d", score);
        double size = Game.game.window.fontRenderer.getStringSizeX(Drawing.drawing.fontSize, s) / Drawing.drawing.interfaceScale;
        Drawing.drawing.displayInterfaceText(posX - size / 2, posY, false, s);
    }
}
